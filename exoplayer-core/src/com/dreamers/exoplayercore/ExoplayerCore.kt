package com.dreamers.exoplayercore

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.runtime.*
import java.util.*
import kotlin.collections.ArrayList

@Suppress("FunctionName")
class ExoplayerCore(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), Component,
    OnPauseListener, OnStopListener, OnResumeListener {

    private val context: Context = container.`$context`()
    private var exoplayer: SimpleExoPlayer? = null

    init {
        // Need to register extension for activity changes
        form.registerForOnPause(this)
        form.registerForOnStop(this)
        form.registerForOnResume(this)
    }

    private var currentWindow: Int = 0
    private var playbackPosition: Long = 0L
    private var shouldPlayWhenReady = false

    private var isPlayerInitialized = false
    private var playbackListeners: Player.Listener? = null
    private val mediaItems: ArrayList<MediaItem> = arrayListOf()

    private var surface: TextureView? = null

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
            playbackListeners?.let { removeListener(it) }
            release()
        }
        playbackListeners = null
        exoplayer = null
        Log.v(LOG_TAG, "releasePlayer : Released = ${exoplayer == null}")
    }

    // Do basic setup for player
    private fun setupPlayer() {
        Log.v(LOG_TAG, "Setting up player")
        exoplayer = SimpleExoPlayer.Builder(context)
            .build()
            .also { exoplayer ->

                if (surface != null) {
                    exoplayer.setVideoTextureView(surface)
                }

                exoplayer.seekTo(currentWindow, playbackPosition)
                exoplayer.playWhenReady = shouldPlayWhenReady
                exoplayer.prepare()

                if (mediaItems.isNotEmpty()) {
                    Log.v(LOG_TAG, "setupPlayer : Using previously added media items.")
                    exoplayer.addMediaItems(mediaItems)
                }

                // Assign Listeners
                playbackListeners = object : Player.Listener {
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


                }

                // Add Listeners to player
                playbackListeners?.let { exoplayer.addListener(it) }
            }

        // Change value of in player instantiated variable
        isPlayerInitialized = exoplayer != null
    }

    // Create a layout to show video using a texture view
    private fun createLayout(layout: HVArrangement): TextureView {
        val container: ViewGroup = layout.view as ViewGroup
        val textureView = TextureView(context)
        container.addView(
            textureView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        return textureView
    }

    // Get exoplayer instance
    @SimpleFunction(description = "Get Exoplayer instance to use in exoplayer ui")
    fun GetPlayer() : Any? = exoplayer

    @SimpleFunction(description = "Create Exoplayer in an arrangement and use custom controls.")
    fun CreateCustomPlayer(layout: HVArrangement) {
        surface = createLayout(layout)
        setupPlayer()
    }

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
    fun AddMedia(path: String, subtitle: String, mimeType: String, language: String) {
        try {
            if (!path.isNullOrEmpty()) {
                val builder = MediaItem.Builder().setUri(path)
                val subtitleItem: MediaItem.Subtitle?
                if (!subtitle.isNullOrEmpty()) {
                    subtitleItem = MediaItem.Subtitle(Uri.parse(subtitle), mimeType, language, C.SELECTION_FLAG_DEFAULT)
                    builder.setSubtitles(arrayListOf(subtitleItem))
                }
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

    // Property Getters
    // =============================

    // State Idle
    @SimpleProperty(description = "Playback State : IDLE")
    fun StateIdle() = Player.STATE_IDLE

    // State Idle
    @SimpleProperty(description = "Playback State : READY")
    fun StateReady() = Player.STATE_READY

    // State Idle
    @SimpleProperty(description = "Playback State : ENDED")
    fun StateEnded() = Player.STATE_ENDED

    // State Idle
    @SimpleProperty(description = "Playback State : BUFFERING")
    fun StateBuffering() = Player.STATE_BUFFERING

    // Repeat Mode None
    @SimpleProperty(description = "Repeat Mode : None")
    fun RepeatModeOff() = Player.REPEAT_MODE_OFF

    // Repeat Mode None
    @SimpleProperty(description = "Repeat Mode : One")
    fun RepeatModeOne() = Player.REPEAT_MODE_ONE

    // Repeat Mode None
    @SimpleProperty(description = "Repeat Mode : All")
    fun RepeatModeAll() = Player.REPEAT_MODE_ALL

}
