package com.dreamers.exoplayerui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.HVArrangement
import com.google.appinventor.components.runtime.ReplForm
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@Suppress("FunctionName")
class ExoplayerUi(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()) {

    private val context: Context = container.`$context`()
    private var playerView: PlayerView? = null
    private val isDebugMode = form is ReplForm

    companion object {
        private const val LOG_TAG = "ExoplayerUI"
    }

    private fun getDrawable(context: Context, fileName: String): Drawable? {
        return try {
            val inputStream: InputStream? = getAsset(context, fileName)
            val drawable: Drawable = Drawable.createFromStream(inputStream, null)
            inputStream?.close()
            drawable
        } catch (e: Exception) {
            Log.v(LOG_TAG, "getDrawable : Error = $e")
            null
        }
    }

    private fun getAsset(context: Context, file: String): InputStream? {
        return try {
            if (isDebugMode) {
                val path: String = when {
                    Build.VERSION.SDK_INT >= 29 -> context.getExternalFilesDir(null).toString() + "/assets/$file"
                    context.javaClass.name.contains("makeroid") -> "/storage/emulated/0/Kodular/assets/$file"
                    else -> "/storage/emulated/0/AppInventor/assets/$file"
                }
                Log.v(LOG_TAG, "getAsset | Filepath = $path")
                FileInputStream(File(path))
            } else {
                context.assets.open(file)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "getAsset | Debug Mode : $isDebugMode | Error : $e")
            null
        }
    }

    // Initialize player view
    private fun initialize(layout: HVArrangement, exoplayer: SimpleExoPlayer) {
        val viewGroup: ViewGroup = layout.view as ViewGroup
        playerView = PlayerView(context).also { view ->
            view.player = exoplayer
            if (form is ReplForm) {
                Log.v(LOG_TAG, "createLayout | Debug mode : true")
                view.setDebugMode(true)
            }
        }
        viewGroup.addView(
            playerView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    // Create Player View
    @SimpleFunction(description = "Create player ui.")
    fun Create(layout: HVArrangement, exoplayer: Any?) {
        if (exoplayer != null && exoplayer is SimpleExoPlayer) {
            initialize(layout, exoplayer)
        }
    }

    // Set Repeat Mode
    @SimpleProperty(description = "Show or hide repeat mode button from ui by specifying repeat modes")
    fun RepeatMode(modes: Int) {
        playerView?.setRepeatToggleModes(modes)
    }

    // Shuffle Button
    @SimpleProperty(description = "Show/Hide shuffle playlist button")
    fun ShuffleButtonVisible(show: Boolean) {
        playerView?.setShowShuffleButton(show)
    }

    // Show next button
    @SimpleProperty(description = "Show/Hide next button")
    fun NextButtonVisible(show: Boolean) {
        playerView?.setShowNextButton(show)
    }

    // Show previous button
    @SimpleProperty(description = "Show/Hide previous button")
    fun PreviousButtonVisible(show: Boolean) {
        playerView?.setShowNextButton(show)
    }

    // Show rewind button
    @SimpleProperty(description = "Show/Hide rewind button")
    fun RewindButtonVisible(show: Boolean) {
        playerView?.setShowRewindButton(show)
    }

    // Show fast forward button
    @SimpleProperty(description = "Show/Hide fast forward  button")
    fun FastForwardButtonVisible(show: Boolean) {
        playerView?.setShowFastForwardButton(show)
    }

    // Show Loading
    @SimpleProperty(description = "Set when to show loading progress indicator")
    fun ShowLoading(show: Int) {
        playerView?.setShowBuffering(show)
    }

    // Controller auto show
    @SimpleProperty(description = "Automatically show controller")
    fun AutoShowController(show: Boolean) {
        playerView?.controllerAutoShow = show
    }

    // Hide controller
    @SimpleProperty(description = "Hide controller on touch")
    fun HideOnTouch(hide: Boolean) {
        playerView?.controllerHideOnTouch = hide
    }

    // Controller Show timeout
    @SimpleProperty(description = "Milliseconds after which the controller should hide")
    fun ControllerTimeout(mills: Int) {
        playerView?.controllerShowTimeoutMs = mills
    }

    // Set resize mode
    @SimpleProperty(description = "Set video resize mode")
    fun ResizeMode(mode: Int) {
        playerView?.resizeMode = mode
    }

    // Show Controls
    @SimpleFunction(description = "Show controls")
    fun ShowControls() {
        playerView?.showController()
    }

    // Hide Controller
    @SimpleFunction(description = "Hide controls")
    fun HideControls() {
        playerView?.hideController()
    }

    // Default Artwork
    @SimpleProperty(description = "Set default thumbnail in case if there is no thumbnail in media metadata, the player will show this thumbnail. It works only for audio files.")
    fun DefaultThumbnail(path: String) {
        playerView?.defaultArtwork = getDrawable(context, path)
    }

    // Use Artwork
    @SimpleProperty(description = "Whether to show an image (thumbnail) if current media is an audio.")
    fun UseArtwork(shouldUse: Boolean) {
        playerView?.useArtwork = shouldUse
    }

    // Controls Visible
    @SimpleProperty(description = "Check if controls are currently visible")
    fun ControlsVisible() = playerView?.isControllerVisible ?: false

    // Repeat Mode None
    @SimpleProperty(description = "Repeat Mode : None")
    fun RepeatModeOff() = Player.REPEAT_MODE_OFF

    // Repeat Mode None
    @SimpleProperty(description = "Repeat Mode : One")
    fun RepeatModeOne() = Player.REPEAT_MODE_ONE

    // Repeat Mode None
    @SimpleProperty(description = "Repeat Mode : All")
    fun RepeatModeAll() = Player.REPEAT_MODE_ALL

    // Show Buffering Never
    @SimpleProperty(description = "Always keep Loading hidden.")
    fun ShowLoadingNever() = PlayerView.SHOW_BUFFERING_NEVER

    // Show Buffering when playing
    @SimpleProperty(description = "Show loading when playing.")
    fun ShowLoadingWhenPlaying() = PlayerView.SHOW_BUFFERING_WHEN_PLAYING

    // Show Buffering always
    @SimpleProperty(description = "Always show Loading.")
    fun ShowLoadingAlways() = PlayerView.SHOW_BUFFERING_ALWAYS

    // Resize Modes
    @SimpleProperty(description = "Resize mode fit")
    fun ResizeModeFit() = AspectRatioFrameLayout.RESIZE_MODE_FIT

    @SimpleProperty(description = "Resize mode fill")
    fun ResizeModeFill() = AspectRatioFrameLayout.RESIZE_MODE_FILL

    @SimpleProperty(description = "Resize mode zoom")
    fun ResizeModeZoom() = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

    @SimpleProperty(description = "Resize mode fixed width")
    fun ResizeModeFixedWidth() = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH

    @SimpleProperty(description = "Resize mode fixed height")
    fun ResizeModeFixedHeight() = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT

}
