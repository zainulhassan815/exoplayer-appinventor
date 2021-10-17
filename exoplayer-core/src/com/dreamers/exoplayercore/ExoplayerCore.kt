package com.dreamers.exoplayercore

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
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

    init {
        // Need to register extension for activity changes
        Log.v(LOG_TAG,"Registering for Activity Changes")
        form.registerForOnPause(this)
        form.registerForOnStop(this)
        form.registerForOnResume(this)
        form.registerForOnDestroy(this)
    }

    private var currentWindow: Int = 0
    private var playbackPosition: Long = 0L
    private var shouldPlayWhenReady = false

    private var isPlayerInitialized = false
    private val mediaItems: ArrayList<MediaItem> = arrayListOf()


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
        }
    }

    // Release player from memory
    private fun releasePlayer() {
        exoplayer?.run {
            playbackPosition = this.currentPosition
            currentWindow = this.currentWindowIndex
            shouldPlayWhenReady = this.playWhenReady
            removeListener(this@ExoplayerCore)
            release()
        }
//        playbackListeners = null
        exoplayer = null
        trackSelector = null
        Log.v(LOG_TAG, "releasePlayer : Released = ${exoplayer == null}")
    }

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

    private fun JSONObject.getStringOrNull(key: String): String? {
        return try {
            getString(key)
        } catch (e: java.lang.Exception) {
            null
        }
    }

    private fun JSONObject.getIntOrNull(key: String): Int? {
        return try {
            getInt(key)
        } catch (e: java.lang.Exception) {
            null
        }
    }

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
            Log.e(LOG_TAG, "parseSubtitleData | Failed to parse data : $data with error : $e")
            OnError("Failed to parse data : $data with error : $e")
        }
        return null
    }

    // Do basic setup for player
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

                if (mediaItems.isNotEmpty()) {
                    Log.v(LOG_TAG, "setupPlayer : Using previously added media items.")
                    exoplayer.addMediaItems(mediaItems)
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

    override fun onPlayerError(error: ExoPlaybackException) {
        super.onPlayerError(error)
        Log.e(LOG_TAG, "onPlayerError : $error")
        OnError(error.toString())
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

    // Get exoplayer instance
    @SimpleFunction(description = "Get Exoplayer instance to use in exoplayer ui")
    fun Player(): Any? = exoplayer


    @SimpleFunction(description = "Create Exoplayer.")
    fun CreatePlayer() {
        setupPlayer()
    }

    // Play Video
    @SimpleFunction(description = "Play media")
    fun Play() {
        exoplayer?.play()
    }

    // Pause Video
    @SimpleFunction(description = "Pause media")
    fun Pause() {
        exoplayer?.pause()
    }

    // Stop video
    @SimpleFunction(description = "Stop media.")
    fun Stop() {
        exoplayer?.stop()
    }

    // Seek to
    @SimpleFunction(description = "Seek media to given position.")
    fun SeekTo(position: Long) {
        exoplayer?.seekTo(position)
    }

    // Play when ready
    @SimpleProperty(description = "Should play automatically when ready")
    fun PlayWhenReady(play: Boolean) {
        exoplayer?.playWhenReady = play
        shouldPlayWhenReady = play
    }

    // Clear Media Items
    @SimpleFunction(description = "Clear media items")
    fun ClearMediaItems() {
        exoplayer?.clearMediaItems()
        mediaItems.clear()
    }

    // Set Repeat Modes
    @SimpleProperty(description = "Set playback repeat modes")
    fun RepeatModes(modes: Int) {
        exoplayer?.repeatMode = modes
    }

    // Add Media Item
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
            Log.e(LOG_TAG, "AddMedia : Error = $e")
            OnError("AddMedia : Error = $e")
        }
    }

    // Remove Media Item at index
    @SimpleFunction(description = "Remove media item at a specific index")
    fun RemoveMedia(index: Int) {
        exoplayer?.removeMediaItem(index)
        mediaItems.removeAt(index)
    }

    // Format milliseconds to time
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

    // Events
    // =============================
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
