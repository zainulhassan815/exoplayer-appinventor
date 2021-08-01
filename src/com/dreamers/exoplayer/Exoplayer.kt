package com.dreamers.exoplayer

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.runtime.*
import java.util.*
import kotlin.collections.ArrayList

@Suppress("FunctionName")
class Exoplayer(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), Component, OnPauseListener, OnStopListener, OnResumeListener {

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

    private var playerView: PlayerView? = null
    private var playbackListeners: Player.Listener? = null

    private val mediaItems: ArrayList<MediaItem> = arrayListOf()

    companion object {
        private const val logTag = "DreamersExoPlayer"
    }

    // On Pause
    override fun onPause() {
        Log.v(logTag, "onPause")
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    // On Stop
    override fun onStop() {
        Log.v(logTag, "onStop")
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    // On Resume
    override fun onResume() {
        Log.v(logTag, "onResume")
        if ((Util.SDK_INT < 24 || exoplayer == null)) {
            resumePlayer()
        }
    }

    // Check if should resume player.
    private fun resumePlayer() {
        if (playerView != null) {
            setupPlayer()
        }
    }

    // Create a layout to show video using a texture view
    private fun createLayout(layout: HVArrangement): PlayerView {
        val container: ViewGroup = layout.view as ViewGroup
        val playerView = PlayerView(context)
        if (form is ReplForm) {
            Log.v(logTag,"createLayout | Debug mode : true")
            playerView.setDebugMode(true)
        }
        container.addView(playerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return playerView
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
        Log.v(logTag, "releasePlayer : Released = ${exoplayer == null}")
    }

    // Do basic setup for player
    private fun setupPlayer() {
        Log.v(logTag,"Setting up player")
        exoplayer = SimpleExoPlayer.Builder(context)
                .build()
                .also { exoplayer ->
                    playerView?.player = exoplayer
                    exoplayer.seekTo(currentWindow, playbackPosition)
                    exoplayer.playWhenReady = shouldPlayWhenReady
                    exoplayer.prepare()

                    if (mediaItems.isNotEmpty()) {
                        Log.v(logTag,"setupPlayer : Using previously added media items.")
                        exoplayer.addMediaItems(mediaItems)
                    }

                    // Assign Listeners
                    playbackListeners = object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            super.onPlaybackStateChanged(state)
                            Log.v(logTag, "onPlaybackStateChanged : $state")
                            OnStateChanged(state)
                        }

                        override fun onPlayerError(error: ExoPlaybackException) {
                            super.onPlayerError(error)
                            Log.e(logTag, "onPlayerError : $error")
                            OnError(error.toString())
                        }

                        override fun onIsLoadingChanged(isLoading: Boolean) {
                            super.onIsLoadingChanged(isLoading)
                            Log.v(logTag, "onIsLoadingChanged : $isLoading")
                            OnLoadingChanged(isLoading)
                        }

                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            super.onVideoSizeChanged(videoSize)
                            Log.i(logTag, "onVideoSizeChanged : $videoSize")
                            OnVideoSizeChanged(videoSize.width, videoSize.height, videoSize.pixelWidthHeightRatio, videoSize.unappliedRotationDegrees)
                        }

                        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
                            super.onDeviceVolumeChanged(volume, muted)
                            Log.v(logTag, "onDeviceVolumeChanged : Volume = $volume | Muted : $muted")
                            OnVolumeChanged(volume, muted)
                        }

                        override fun onRenderedFirstFrame() {
                            super.onRenderedFirstFrame()
                            Log.v(logTag, "onRenderedFirstFrame")
                            OnRenderFirstFrame()
                        }
                    }

                    // Add Listeners to player
                    playbackListeners?.let { exoplayer.addListener(it) }
                }
    }

    @SimpleFunction(description = "Create Exoplayer in an arrangement")
    fun Create(layout: HVArrangement) {
        playerView = createLayout(layout)
        setupPlayer()
    }

    // Play Video
    @SimpleFunction(description = "Play video")
    fun Play() {
        exoplayer?.play()
    }

    // Pause Video
    @SimpleFunction(description = "Pause video")
    fun Pause() {
        exoplayer?.pause()
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

    // Add Media Item
    @SimpleFunction(description = "Add a new media item")
    fun AddMedia(path: String, subtitle: String, mimeType: String, language: String) {
        try {
            if (!path.isNullOrEmpty()) {
                val builder = MediaItem.Builder().setUri(path)
                val subtitleItem: MediaItem.Subtitle?
                if (!subtitle.isNullOrEmpty()) {
                    subtitleItem = MediaItem.Subtitle(Uri.parse(subtitle), mimeType,language, C.SELECTION_FLAG_DEFAULT)
                    builder.setSubtitles(arrayListOf(subtitleItem))
                }
                val mediaItem = builder.build()
                mediaItems.add(mediaItem)
                exoplayer?.addMediaItem(mediaItem)
            } else throw Exception("Path is null or empty")
        } catch (e: Exception) {
            Log.e(logTag, "AddMedia : Error = $e")
            OnError("AddMedia : Error = $e")
        }
    }

    // Remove Media Item at index
    @SimpleFunction(description = "Remove media item at a specific index")
    fun RemoveMedia(index:Int) {
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
}
