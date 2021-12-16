package com.dreamers.exoplayercore

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.*
import com.google.appinventor.components.runtime.util.JsonUtil
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import org.json.JSONException
import org.json.JSONObject
import java.util.*

@Suppress("FunctionName")
class ExoplayerCore(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), Component,
    OnPauseListener, OnStopListener, OnResumeListener, OnDestroyListener, Player.Listener {

    private val context: Context = container.`$context`()
    private var exoplayer: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        // Need to register extension for activity changes
        Log.v(LOG_TAG, "Registering for Activity Changes")
        form.registerForOnPause(this)
        form.registerForOnStop(this)
        form.registerForOnResume(this)
        form.registerForOnDestroy(this)
    }

    private var currentWindow: Int = 0
    private var playbackPosition: Long = 0L
    private var shouldPlayWhenReady = false

    private var isPlayerInitialized = false
    private val mediaItems: ArrayList<Any> = arrayListOf()

    private var playbackSpeed: Float = 1f


    companion object {
        private const val LOG_TAG = "DreamersExoPlayer"
    }

    // On Pause
    override fun onPause() {
        Log.v(LOG_TAG, "onPause")
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    // On Stop
    override fun onStop() {
        Log.v(LOG_TAG, "onStop")
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    // On Resume
    override fun onResume() {
        Log.v(LOG_TAG, "onResume")
        if ((Util.SDK_INT < 24 || exoplayer == null)) {
            resumePlayer()
        }
    }

    override fun onDestroy() {
        Log.v(LOG_TAG, "onDestroy")
        releasePlayer()
    }

    // Check if should resume player.
    private fun resumePlayer() {
        if (exoplayer == null && isPlayerInitialized) {
            setupPlayer()
            // Call app resume here
            Log.v(LOG_TAG, "OnAppResume")
            OnAppResume()
        }
    }

    /** Release player from memory. It is necessary to release the resources when player is no longer visible. */
    private fun releasePlayer() {
        exoplayer?.run {
            playbackPosition = this.currentPosition
            currentWindow = this.currentWindowIndex
            shouldPlayWhenReady = this.playWhenReady
            removeListener(this@ExoplayerCore)
            release()
        }
        exoplayer = null
        trackSelector = null
        Log.v(LOG_TAG, "releasePlayer : Released = ${exoplayer == null}")
    }

    /** Convert `MediaMetadata` to `YailDictionary` so it can be used easily in blocks. */
    private fun MediaMetadata.toJson(): YailDictionary {
        val data = JSONObject().also { obj ->
            obj.put("title", this.title.toString())
            obj.put("artist", this.artist.toString())
            obj.put("albumTitle", this.albumTitle.toString())
            obj.put("albumArtist", this.albumArtist.toString())
            obj.put("displayTitle", this.displayTitle.toString())
            obj.put("subtitle", this.subtitle.toString())
            obj.put("description", this.description.toString())
            obj.put("media_uri", this.mediaUri.toString())
            obj.put("artwork_uri", this.artworkUri.toString())
            obj.put("track_number", this.trackNumber)
            obj.put("total_tracks", this.totalTrackCount)
            obj.put("year", this.year)
            obj.put("playable", this.isPlayable)
        }
        return JsonUtil.getDictionaryFromJsonObject(data)
    }

    /** Returns `String` if it is present in JSONObject else `null`. */
    private fun JSONObject.getStringOrNull(key: String): String? {
        return try {
            getString(key)
        } catch (e: java.lang.Exception) {
            null
        }
    }

    /** Returns `Integer` if it is present in JSONObject else `null`. */
    private fun JSONObject.getIntOrNull(key: String): Int? {
        return try {
            getInt(key)
        } catch (e: java.lang.Exception) {
            null
        }
    }

    /** Converts JSON String into `MediaItem.Subtitle`. */
    private fun parseSubtitleData(data: String): MediaItem.Subtitle? {
        try {
            val jsonObject = JSONObject(data)
            val uri: String = jsonObject.getString("path")
            val mimeType: String = jsonObject.getString("mime_type")
            val label = jsonObject.getStringOrNull("label")
            val language = jsonObject.getStringOrNull("language")
            val selectionFlags = jsonObject.getIntOrNull("selection_flags") ?: 0
            return MediaItem.Subtitle(Uri.parse(uri), mimeType, language, selectionFlags, 0, label)
        } catch (e: JSONException) {
            Log.e(LOG_TAG, "parseSubtitleData | Failed to parse data : $data with error : ${e.message}")
            OnError("Failed to parse data : $data with error : ${e.message}")
        }
        return null
    }

    /** Initialize player. Here all the necessary setup should be done. */
    private fun setupPlayer() {
        Log.v(LOG_TAG, "Setting up player")
        trackSelector = DefaultTrackSelector(context)
        exoplayer = SimpleExoPlayer.Builder(context)
            .setTrackSelector(trackSelector!!)
            .build()
            .also { exoplayer ->

                exoplayer.seekTo(currentWindow, playbackPosition)
                exoplayer.playWhenReady = shouldPlayWhenReady
                exoplayer.prepare()

                // Set playback speed
                exoplayer.setPlaybackSpeed(playbackSpeed)

                /** If the player is created again when app is resumed,
                 *  we need to make sure that we use previously added media items
                 *  rather than initializing player with empty list. */
                if (mediaItems.isNotEmpty()) {
                    Log.v(LOG_TAG, "setupPlayer : Using previously added media items.")
                    mediaItems.forEach { item ->
                        if (item is MediaItem) {
                            exoplayer.addMediaItem(item)
                        } else if (item is MediaSource) {
                            exoplayer.addMediaSource(item)
                        }
                    }
                }

                // Add Listeners to player
                exoplayer.addListener(this)
                // Add Analytics Logger to player
                exoplayer.addAnalyticsListener(EventLogger(trackSelector!!))
            }

        // Change value of in player instantiated variable
        isPlayerInitialized = exoplayer != null
    }

    override fun onPlaybackStateChanged(state: Int) {
        super.onPlaybackStateChanged(state)
        Log.v(LOG_TAG, "onPlaybackStateChanged : $state")
        OnStateChanged(state)
    }

    override fun onPlayerError(e: ExoPlaybackException) {
        super.onPlayerError(e)
        Log.e(LOG_TAG, "onPlayerError : ${e.message}")
        OnError(e.toString())
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        Log.v(LOG_TAG, "onIsLoadingChanged : $isLoading")
        OnLoadingChanged(isLoading)
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        super.onVideoSizeChanged(videoSize)
        Log.i(LOG_TAG, "onVideoSizeChanged : $videoSize")
        OnVideoSizeChanged(
            videoSize.width,
            videoSize.height,
            videoSize.pixelWidthHeightRatio,
            videoSize.unappliedRotationDegrees
        )
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        super.onDeviceVolumeChanged(volume, muted)
        Log.v(LOG_TAG, "onDeviceVolumeChanged : Volume = $volume | Muted : $muted")
        OnVolumeChanged(volume, muted)
    }

    override fun onRenderedFirstFrame() {
        super.onRenderedFirstFrame()
        Log.v(LOG_TAG, "onRenderedFirstFrame")
        OnRenderFirstFrame()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Log.v(LOG_TAG, "onIsPlayingChanged : IsPlaying = $isPlaying")
        OnIsPlayingChanged(isPlaying)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        Log.v(LOG_TAG, "onRepeatModeChanged : RepeatMode = $repeatMode")
        OnRepeatModeChanged(repeatMode)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        Log.v(LOG_TAG, "onShuffleModeEnabledChanged : ShuffleModeEnabled = $shuffleModeEnabled")
        OnShuffleModeEnabledChanged(shuffleModeEnabled)
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        val meta = mediaMetadata.toJson()
        Log.v(LOG_TAG, "onMediaMetadataChanged : MetaData = $meta")
        OnMetadataChanged(meta)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        Log.v(
            LOG_TAG,
            "onMediaItemTransition : MediaUrl = ${mediaItem?.mediaId.toString()} | Reason = $reason"
        )
        OnMediaItemTransition(mediaItem?.mediaId.toString(), reason)
    }

    override fun onCues(cues: MutableList<Cue>) {
        super.onCues(cues)
        val text = cues.map { cue -> cue.text }
        val yailList = YailList.makeList(text)
        OnCues(yailList)
    }

    /** Get exoplayer instance */
    @SimpleFunction(description = "Get Exoplayer instance to use in exoplayer ui")
    fun Player(): Any? = exoplayer


    @SimpleFunction(description = "Create Exoplayer.")
    fun CreatePlayer() {
        setupPlayer()
    }

    /** Play Video */
    @SimpleFunction(description = "Play media")
    fun Play() {
        exoplayer?.play()
    }

    /** Pause Video */
    @SimpleFunction(description = "Pause media")
    fun Pause() {
        exoplayer?.pause()
    }

    /** Stop video */
    @SimpleFunction(description = "Stop media.")
    fun Stop() {
        exoplayer?.stop()
    }

    @SimpleFunction(description = "Play next media item in the playlist.")
    fun Next() {
        exoplayer?.next()
    }

    @SimpleFunction(description = "Play previous media item in the playlist.")
    fun Previous() {
        exoplayer?.previous()
    }

    @SimpleProperty(description = "Check if playlist has next media item.")
    fun HasNext(): Boolean = exoplayer?.hasNext() ?: false

    @SimpleProperty(description = "Check if playlist has previous media item.")
    fun HasPrevious(): Boolean = exoplayer?.hasPrevious() ?: false

    @SimpleProperty(description = "Get current windows index. Returns `-1` if player is not initialized.")
    fun CurrentWindowIndex(): Int = if (exoplayer?.currentWindowIndex != null) exoplayer?.currentWindowIndex!! + 1 else -1

    /** Seek to */
    @SimpleFunction(description = "Seek media to given position.")
    fun SeekTo(position: Long) {
        exoplayer?.seekTo(position)
    }

    @SimpleFunction(description = "Seek to media item at given position.")
    fun SeekToWindow(position: Int) {
        exoplayer?.seekToDefaultPosition(position)
    }

    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT,
        defaultValue = "1"
    )
    @SimpleProperty(description = "Set playback speed.")
    fun PlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        exoplayer?.setPlaybackSpeed(speed)
    }

    /** Play when ready */
    @SimpleProperty(description = "Should play automatically when ready")
    fun PlayWhenReady(play: Boolean) {
        exoplayer?.playWhenReady = play
        shouldPlayWhenReady = play
    }

    /** Clear Media Items */
    @SimpleFunction(description = "Clear media items")
    fun ClearMediaItems() {
        exoplayer?.clearMediaItems()
        mediaItems.clear()
    }

    /** Set Repeat Modes */
    @SimpleProperty(description = "Set playback repeat modes")
    fun RepeatModes(modes: Int) {
        exoplayer?.repeatMode = modes
    }

    /** Add Media Item */
    @Deprecated("Use AddMediaItem with CreateMedia & CreateMediaExtended instead. This method will be removed in the newer version.")
    @SimpleFunction(description = "Add a new media item")
    fun AddMedia(path: String, subtitles: YailList) {
        try {
            if (path.isNotEmpty()) {
                val builder = MediaItem.Builder().setUri(path)
                val subtitleArray: ArrayList<MediaItem.Subtitle> = arrayListOf()

                subtitles.toStringArray().forEach { subtitleData ->
                    val subtitle = parseSubtitleData(subtitleData)
                    subtitle?.let { subtitleArray.add(subtitle) }
                }
                builder.setSubtitles(subtitleArray)
                builder.setMediaId(path)
                val mediaItem = builder.build()
                mediaItems.add(mediaItem)
                exoplayer?.addMediaItem(mediaItem)
            } else throw Exception("Path is null or empty")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "AddMedia : Error = ${e.message}")
            OnError("AddMedia : Error = ${e.message}")
        }
    }

    /**
     * Create a basic media item
     *
     * @param path Path to media file either offline or online.
     * @param subtitles List of Subtitles.
     *
     * @return MediaItem or null if media item is not created successfully
     */
    @SimpleFunction(description = "Create new media item")
    fun CreateMedia(path: String, subtitles: YailList): Any? {
        try {
            if (path.isNotEmpty()) {
                val builder = MediaItem.Builder().setUri(path)
                val subtitleArray: ArrayList<MediaItem.Subtitle> = arrayListOf()

                subtitles.toStringArray().forEach { subtitleData ->
                    val subtitle = parseSubtitleData(subtitleData)
                    subtitle?.let { subtitleArray.add(subtitle) }
                }
                builder.setSubtitles(subtitleArray)
                builder.setMediaId(path)
                return builder.build()
            } else throw Exception("Path is null or empty")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "AddMedia : Error = ${e.message}")
            OnError("AddMedia : Error = ${e.message}")
            return null
        }
    }

    @SimpleFunction(description = "Creates a Subtitle Object.")
    fun SubtitleTrack(path: String, mimeType: String,label: String, language: String, flags: Int): String {
        return """
            {
                path: $path,
                mime_type: $mimeType,
                selection_flags: $flags,
                ${if(label.isNotEmpty()) "label: $label," else ""}
                ${if(language.isNotEmpty()) "language: $language" else ""}
            }
        """.trimIndent()
    }

    /**
     * Create new media item with extra customizations.
     *
     * @param path Path to media file either offline or online.
     * @param subtitles List of Subtitles.
     * @param mediaId Custom media id or an empty string to use path as default id.
     * @param mimeType The MIME type may be used as a hint for inferring the type of the media item.
     * @param startPositionMs Sets the optional start position in milliseconds which must be a value larger than or equal to zero.
     * @param endPositionMs Sets the optional end position in milliseconds which must be a value larger than or equal to zero, or TimeEndOfSource.
     * @param relativeToLiveWindow Sets whether the start/end positions should move with the live window for live streams.
     * @param relativeToDefaultPosition Sets whether the start position and the end position are relative to the default position in the window.
     * @param startsAtKeyFrame Sets whether the start point is guaranteed to be a key frame.
     * @param drmScheme The drm scheme that will be used to get respective drm UUID.
     * @param drmLicenseUri Sets the optional default DRM license server URI.
     * @param drmForceDefaultLicenseUri Sets whether to force use the default DRM license server URI even if the media specifies its own DRM license server URI.
     * @param drmLicenseRequestHeaders Sets the optional request headers attached to the DRM license request.
     * @param drmMultiSession Sets whether the DRM configuration is multi session enabled.
     * @param drmPlayClearContentWithoutKey Sets whether clear samples within protected content should be played when keys for the encrypted part of the content have yet to be loaded.
     * @param drmSessionForClearContent Sets whether a DRM session should be used for clear tracks of type TrackTypeVideo and TrackTypeAudio.
     * @param liveTargetOffsetMs Sets the optional target offset from the live edge for live streams, in milliseconds.
     * @param liveMinOffsetMs Sets the optional minimum offset from the live edge for live streams, in milliseconds.
     * @param liveMaxOffsetMs Sets the optional maximum offset from the live edge for live streams, in milliseconds.
     * @param liveMinPlaybackSpeed Sets the optional minimum playback speed for live stream speed adjustment.
     * @param liveMaxPlaybackSpeed Sets the optional maximum playback speed for live stream speed adjustment.
     *
     * @return MediaItem or null if media item is not created successfully
     */
    @SimpleFunction(description = "Create new media item with extra customizations.")
    fun CreateMediaExtended(
        path: String,
        subtitles: YailList,
        mediaId: String,
        mimeType: String,
        startPositionMs: Long,
        endPositionMs: Long,
        relativeToLiveWindow: Boolean,
        relativeToDefaultPosition: Boolean,
        startsAtKeyFrame: Boolean,
        drmScheme: String,
        drmLicenseUri: String,
        drmForceDefaultLicenseUri: Boolean,
        drmLicenseRequestHeaders: YailDictionary,
        drmMultiSession: Boolean,
        drmPlayClearContentWithoutKey: Boolean,
        drmSessionForClearContent: Boolean,
        liveTargetOffsetMs: Long,
        liveMinOffsetMs: Long,
        liveMaxOffsetMs: Long,
        liveMinPlaybackSpeed: Float,
        liveMaxPlaybackSpeed: Float
    ): Any? {
        try {
            if (path.isNotEmpty()) {
                val builder = MediaItem.Builder().setUri(path)
                val subtitleArray: ArrayList<MediaItem.Subtitle> = arrayListOf()

                subtitles.toStringArray().forEach { subtitleData ->
                    val subtitle = parseSubtitleData(subtitleData)
                    subtitle?.let { subtitleArray.add(subtitle) }
                }

                builder.apply {
                    if (mediaId.isNotEmpty()) setMediaId(mediaId) else setMediaId(path)
                    if (mimeType.isNotEmpty()) setMimeType(mimeType)
                    setSubtitles(subtitleArray)

                    // Clipping properties
                    setClipStartPositionMs(startPositionMs)
                    setClipEndPositionMs(endPositionMs)
                    setClipRelativeToLiveWindow(relativeToLiveWindow)
                    setClipRelativeToDefaultPosition(relativeToDefaultPosition)
                    setClipStartsAtKeyFrame(startsAtKeyFrame)

                    // Drm properties
                    if (drmScheme.isNotEmpty()) {
                        val drmUUID = Util.getDrmUuid(drmScheme)
                        if (drmUUID != null) {
                            setDrmUuid(drmUUID)
                            if (drmLicenseUri.isNotEmpty()) setDrmLicenseUri(drmLicenseUri)
                            setDrmForceDefaultLicenseUri(drmForceDefaultLicenseUri)

                            val headers: MutableMap<String, String> = mutableMapOf()
                            drmLicenseRequestHeaders.iterator()
                                .forEach { pair -> headers[pair.getString(0)] = pair.getString(1) }
                            setDrmLicenseRequestHeaders(headers)

                            setDrmMultiSession(drmMultiSession)
                            setDrmPlayClearContentWithoutKey(drmPlayClearContentWithoutKey)
                            if (drmSessionForClearContent) setDrmSessionForClearTypes(
                                listOf(
                                    C.TRACK_TYPE_VIDEO,
                                    C.TRACK_TYPE_AUDIO
                                )
                            )
                        }
                    }

                    // Live properties
                    setLiveTargetOffsetMs(liveTargetOffsetMs)
                    setLiveMinOffsetMs(liveMinOffsetMs)
                    setLiveMaxOffsetMs(liveMaxOffsetMs)
                    setLiveMinPlaybackSpeed(liveMinPlaybackSpeed)
                    setLiveMaxPlaybackSpeed(liveMaxPlaybackSpeed)
                }

                return builder.build()

            } else throw Exception("Path is null or empty")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "AddMedia : Error = ${e.message}")
            OnError("AddMedia : Error = ${e.message}")
            return null
        }
    }

    /**
     * Create HLS media source with given media item.
     *
     * @param mediaItem MediaItem
     * @param userAgent The user agent that will be used, or empty string to use the default user agent of the underlying platform.
     * @param requestHeaders Http Request Headers.
     * @param allowCrossProtocolRedirects Whether to allow cross protocol redirects (i.e. redirects from HTTP to HTTPS or vice versa).
     * @param allowChunklessPreparation Sets whether chunkless preparation is allowed. If true, preparation without chunk downloads will be enabled for streams that provide sufficient information in their master playlist.
     *
     * @return HlsMediaSource or null if given media item is not valid.
     */
    @SimpleFunction(description = "Create HLS media source with given media item.")
    fun HlsSource(
        mediaItem: Any?,
        userAgent: String,
        requestHeaders: YailDictionary,
        allowCrossProtocolRedirects: Boolean,
        allowChunklessPreparation: Boolean,
    ): Any? {
        if (mediaItem != null && mediaItem is MediaItem) {
            // Create http data source
            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                if (userAgent.isNotEmpty()) setUserAgent(userAgent)

                val headers: MutableMap<String, String> = mutableMapOf()
                requestHeaders.iterator()
                    .forEach { pair -> headers[pair.getString(0)] = pair.getString(1) }
                setDefaultRequestProperties(headers)

                setAllowCrossProtocolRedirects(allowCrossProtocolRedirects)
            }

            // return HlsMediaSource
            return HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(allowChunklessPreparation)
                .createMediaSource(mediaItem)
        }
        return null
    }

    /**
     * Create Dash media source with given media item.
     *
     * @param mediaItem MediaItem
     * @param userAgent The user agent that will be used, or empty string to use the default user agent of the underlying platform.
     * @param requestHeaders Http Request Headers.
     * @param allowCrossProtocolRedirects Whether to allow cross protocol redirects (i.e. redirects from HTTP to HTTPS or vice versa).
     *
     * @return DashMediaSource or null if given media item is not valid.
     */
    @SimpleFunction(description = "Create Dash media source with given media item.")
    fun DashSource(
        mediaItem: Any?,
        userAgent: String,
        requestHeaders: YailDictionary,
        allowCrossProtocolRedirects: Boolean,
    ): Any? {
        if (mediaItem != null && mediaItem is MediaItem) {
            // Create http data source
            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                if (userAgent.isNotEmpty()) setUserAgent(userAgent)

                val headers: MutableMap<String, String> = mutableMapOf()
                requestHeaders.iterator()
                    .forEach { pair -> headers[pair.getString(0)] = pair.getString(1) }
                setDefaultRequestProperties(headers)

                setAllowCrossProtocolRedirects(allowCrossProtocolRedirects)
            }

            return DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
        return null
    }

    /**
     * Add a new media source
     *
     * @param source Media source that will be added to the player.
     * @see HlsSource
     */
    @SimpleFunction(description = "Add a new media source.")
    fun AddMediaSource(source: Any?) {
        if (source != null && source is MediaSource) {
            mediaItems.add(source)
            exoplayer?.addMediaSource(source)
        }
    }

    /**
     * Add a new media item to the player.
     *
     * @param mediaItem MediaItem object that will be added to the player
     * @see CreateMedia
     * @see CreateMediaExtended
     */
    @SimpleFunction(description = "Add media item")
    fun AddMediaItem(mediaItem: Any?) {
        if (mediaItem != null && mediaItem is MediaItem) {
            mediaItems.add(mediaItem)
            exoplayer?.addMediaItem(mediaItem)
        }
    }

    @SimpleProperty
    fun RateUnset() = C.RATE_UNSET

    @SimpleProperty
    fun TimeUnset() = C.TIME_UNSET

    @SimpleProperty
    fun TimeEndOfSource() = C.TIME_END_OF_SOURCE

    /** Remove Media Item at index */
    @SimpleFunction(description = "Remove media item at a specific index")
    fun RemoveMedia(index: Int) {
        exoplayer?.removeMediaItem(index)
        mediaItems.removeAt(index)
    }

    /** Format milliseconds to time */
    @SimpleFunction(description = "Convert milliseconds to hh:mm:ss time format")
    fun Format(mills: Int): String {
        val seconds: Long = mills.div(1000L)
        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            seconds / 3600L,
            seconds % 3600L / 60L,
            seconds % 60L,
        )
    }

    @SimpleFunction(description = "Increment device volume.")
    fun IncreaseVolume() {
        exoplayer?.increaseDeviceVolume()
    }

    @SimpleFunction(description = "Decrease device volume.")
    fun DecreaseVolume() {
        exoplayer?.decreaseDeviceVolume()
    }

    @SimpleProperty(description = "Check if video is playing or not.")
    fun IsPlaying() = exoplayer?.isPlaying ?: false

    @SimpleProperty(description = "Check if video is currently loading.")
    fun IsLoading() = exoplayer?.isLoading ?: false

    @SimpleProperty(description = "Buffered percentage")
    fun BufferedPercentage() = exoplayer?.bufferedPercentage ?: 0

    @SimpleProperty(description = "Buffered position")
    fun BufferedLocation() = exoplayer?.bufferedPosition ?: 0

    @SimpleProperty(description = "Get media duration in milliseconds")
    fun Duration() = exoplayer?.duration ?: 0

    @SimpleProperty(description = "Get current position in milliseconds")
    fun CurrentPosition() = exoplayer?.currentPosition ?: 0

    @SimpleProperty(description = "Get the current playback state of exoplayer")
    fun PlaybackState() = exoplayer?.playbackState ?: 0

    @SimpleProperty(description = "Get current device volume")
    fun DeviceVolume(): Int = exoplayer?.deviceVolume ?: 0

    @SimpleProperty(description = "Set current device volume")
    fun DeviceVolume(volume: Int) {
        exoplayer?.deviceVolume = volume
    }

    @SimpleProperty(description = "Check if device is muted or not")
    fun DeviceMuted(): Boolean = exoplayer?.isDeviceMuted ?: false

    @SimpleProperty(description = "Toggle device muted state")
    fun DeviceMuted(muted: Boolean) {
        exoplayer?.isDeviceMuted = muted
    }

    @SuppressLint("NewApi")
    @SimpleProperty(description = "Minimum device volume")
    fun MinVolume() = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)

    @SuppressLint("NewApi")
    @SimpleProperty(description = "Maximum device volume")
    fun MaxVolume() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    // Events
    // =============================
    @SimpleEvent(description = "Event raised when application is resumed. Here you need to reassign Exoplayer to ExoplayerUI.")
    fun OnAppResume() {
        EventDispatcher.dispatchEvent(this, "OnAppResume")
    }

    @SimpleEvent(description = "Event raised when playback state changes.")
    fun OnStateChanged(state: Int) {
        EventDispatcher.dispatchEvent(this, "OnStateChanged", state)
    }

    @SimpleEvent(description = "Event raised when error occurs.")
    fun OnError(error: String) {
        EventDispatcher.dispatchEvent(this, "OnError", error)
    }

    @SimpleEvent(description = "Event raised when loading state changes.")
    fun OnLoadingChanged(loading: Boolean) {
        EventDispatcher.dispatchEvent(this, "OnLoadingChanged", loading)
    }

    @SimpleEvent(description = "Event raised when video size changes.")
    fun OnVideoSizeChanged(width: Int, height: Int, pixelRatio: Float, unAppliedRotationDegrees: Int) {
        EventDispatcher.dispatchEvent(this, "OnVideoSizeChanged", width, height, pixelRatio, unAppliedRotationDegrees)
    }

    @SimpleEvent(description = "Event raised when device volume changes.")
    fun OnVolumeChanged(volume: Int, isMuted: Boolean) {
        EventDispatcher.dispatchEvent(this, "OnVolumeChanged", volume, isMuted)
    }

    @SimpleEvent(description = "Event raised when player renders first frame of the video.")
    fun OnRenderFirstFrame() {
        EventDispatcher.dispatchEvent(this, "OnRenderFirstFrame")
    }

    @SimpleEvent(description = "Event raised when video playing changes.")
    fun OnIsPlayingChanged(isPlaying: Boolean) {
        EventDispatcher.dispatchEvent(this, "OnIsPlayingChanged", isPlaying)
    }

    @SimpleEvent(description = "Event raised when playlist shuffle mode changes.")
    fun OnShuffleModeEnabledChanged(enabled: Boolean) {
        EventDispatcher.dispatchEvent(this, "OnShuffleModeEnabledChanged", enabled)
    }

    @SimpleEvent(description = "Event raised when repeat mode changes.")
    fun OnRepeatModeChanged(repeatMode: Int) {
        EventDispatcher.dispatchEvent(this, "OnRepeatModeChanged", repeatMode)
    }

    @SimpleEvent(description = "Event raised when media metadata changes. Provides a json data object.")
    fun OnMetadataChanged(data: Any) {
        EventDispatcher.dispatchEvent(this, "OnMetadataChanged", data)
    }

    @SimpleEvent(description = "Event raised when current media item transitions.")
    fun OnMediaItemTransition(mediaUrl: String, reason: Int) {
        EventDispatcher.dispatchEvent(this, "OnMediaItemTransition", mediaUrl, reason)
    }

    @SimpleEvent(description = "Event raised when text output changes.")
    fun OnCues(cues: YailList) {
        EventDispatcher.dispatchEvent(this, "OnCues", cues)
    }

    // Property Getters
    // =============================

    // Playback States
    @SimpleProperty(description = "Playback State : IDLE")
    fun StateIdle() = Player.STATE_IDLE

    @SimpleProperty(description = "Playback State : READY")
    fun StateReady() = Player.STATE_READY

    @SimpleProperty(description = "Playback State : ENDED")
    fun StateEnded() = Player.STATE_ENDED

    @SimpleProperty(description = "Playback State : BUFFERING")
    fun StateBuffering() = Player.STATE_BUFFERING

    // Repeat Modes
    @SimpleProperty(description = "Repeat Mode : None")
    fun RepeatModeOff() = Player.REPEAT_MODE_OFF

    @SimpleProperty(description = "Repeat Mode : One")
    fun RepeatModeOne() = Player.REPEAT_MODE_ONE

    @SimpleProperty(description = "Repeat Mode : All")
    fun RepeatModeAll() = Player.REPEAT_MODE_ALL

    // Transition Reason

    @SimpleProperty
    fun TransitionReasonAuto() = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO

    @SimpleProperty
    fun TransitionReasonRepeat() = Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT

    @SimpleProperty
    fun TransitionReasonSeek() = Player.MEDIA_ITEM_TRANSITION_REASON_SEEK

    @SimpleProperty
    fun TransitionReasonPlaylistChanged() = Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED

    @SimpleProperty
    fun SelectionFlagDefault() = C.SELECTION_FLAG_DEFAULT

    @SimpleProperty
    fun SelectionFlagForced() = C.SELECTION_FLAG_FORCED

    @SimpleProperty
    fun SelectionFlagAutoSelect() = C.SELECTION_FLAG_AUTOSELECT

}
