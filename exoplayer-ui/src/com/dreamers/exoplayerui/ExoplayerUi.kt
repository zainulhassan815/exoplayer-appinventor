package com.dreamers.exoplayerui

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.PlayerViewAttributes
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.HVArrangement
import com.google.appinventor.components.runtime.ReplForm

@Suppress("FunctionName")
class ExoplayerUi(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()) {

    private val context: Context = container.`$context`()
    private var playerView: PlayerView? = null
    private var styledPlayerView: StyledPlayerView? = null
    private val isDebugMode = form is ReplForm

    private var repeatMode: String = REPEAT_MODE_OFF
    private var bufferingMode: String = SHOW_BUFFERING_WHEN_PLAYING
    private var controllerTimeout: Int = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
    private var resizeMode: String = RESIZE_MODE_FIT
    private var showShuffleButton: Boolean = false
    private var showNextButton: Boolean = true
    private var showPreviousButton: Boolean = true
    private var showRewindButton: Boolean = true
    private var showFastForwardButton: Boolean = true
    private var showSubtitlesButton: Boolean = true
    private var hideOnTouch: Boolean = true
    private var autoShowController: Boolean = true
    private var useArtwork: Boolean = true
    private var animationEnabled: Boolean = true
    private var useController: Boolean = true
    private var fastForwardMs: Int = DefaultControlDispatcher.DEFAULT_FAST_FORWARD_MS
    private var rewindMs: Int = DefaultControlDispatcher.DEFAULT_REWIND_MS
    private var playerType: PlayerViewType? = null

    enum class PlayerViewType {
        SimplePlayerView,
        StyledPlayerView
    }

    companion object {
        const val LOG_TAG = "ExoplayerUI"
        const val RESIZE_MODE_FIT = "Resize Mode Fit"
        const val RESIZE_MODE_FILL = "Resize Mode Fill"
        const val RESIZE_MODE_ZOOM = "Resize Mode Zoom"
        const val RESIZE_MODE_FIXED_WIDTH = "Resize Mode Fixed Width"
        const val RESIZE_MODE_FIXED_HEIGHT = "Resize Mode Fixed Height"
        const val SHOW_BUFFERING_WHEN_PLAYING = "When Playing"
        const val SHOW_BUFFERING_ALWAYS = "Always"
        const val SHOW_BUFFERING_NEVER = "Never"
        const val REPEAT_MODE_OFF = "Off"
        const val REPEAT_MODE_ONE = "One"
        const val REPEAT_MODE_All = "All"
        const val REPEAT_MODE_ONE_ALL = "One & All"
    }

    // Initialize player view
    private fun initialize(layout: HVArrangement, exoplayer: SimpleExoPlayer, playerType: PlayerViewType) {

        val viewGroup: ViewGroup = layout.view as ViewGroup
        if (isDebugMode) Log.v(LOG_TAG, "createLayout | Debug mode : true")

        val attributes = PlayerViewAttributes(
            isDebugMode = isDebugMode,
            repeatToggleModes = getRepeatModeFromString(repeatMode),
            showBuffering = getBufferingModeFromString(bufferingMode),
            controllerTimeout = controllerTimeout,
            resizeMode = getResizeModeFromString(resizeMode),
            showFastForwardButton = showFastForwardButton,
            showNextButton = showNextButton,
            showPreviousButton = showPreviousButton,
            showRewindButton = showRewindButton,
            showShuffleButton = showShuffleButton,
            hideOnTouch = hideOnTouch,
            useArtwork = useArtwork,
            animationEnabled = animationEnabled,
            useController = useController,
            rewindMs = rewindMs,
            fastForwardMs = fastForwardMs,
            autoShowController = autoShowController,
            showSubtitleButton = showSubtitlesButton,
        )

        if (playerType == PlayerViewType.SimplePlayerView) {
            playerView = PlayerView(context, attributes).also { view ->
                view.player = exoplayer
            }
        } else {
            styledPlayerView = StyledPlayerView(context, attributes).also { view ->
                view.player = exoplayer
            }
        }
        viewGroup.addView(
            if (playerType == PlayerViewType.SimplePlayerView) playerView else styledPlayerView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    // Create Player View
    @SimpleFunction(description = "Create player ui.")
    fun CreateSimplePlayer(layout: HVArrangement, exoplayer: Any?) {
        if (exoplayer != null && exoplayer is SimpleExoPlayer) {
            initialize(layout, exoplayer, PlayerViewType.SimplePlayerView)
        }
    }

    // Create Styled Player View
    @SimpleFunction(description = "Create styled player ui. A bit more complex ui then simple player.")
    fun CreateStyledPlayer(layout: HVArrangement, exoplayer: Any?) {
        if (exoplayer != null && exoplayer is SimpleExoPlayer) {
            initialize(layout, exoplayer, PlayerViewType.StyledPlayerView)
        }
    }

    // Show Controls
    @SimpleFunction(description = "Show controls")
    fun ShowControls() {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.showController()
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.showController()
    }

    // Hide Controller
    @SimpleFunction(description = "Hide controls")
    fun HideControls() {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.hideController()
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.hideController()
    }

    // Set Repeat Mode
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = [REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_All, REPEAT_MODE_ONE_ALL],
        defaultValue = REPEAT_MODE_OFF
    )
    @SimpleProperty(description = "Show or hide repeat mode button from ui by specifying repeat modes")
    fun RepeatMode(mode: String) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.setRepeatToggleModes(getRepeatModeFromString(mode))
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setRepeatToggleModes(getRepeatModeFromString(mode))

        repeatMode = mode
    }

    @SimpleProperty(description = "Repeat Mode : None")
    fun RepeatModeOff() = REPEAT_MODE_OFF

    @SimpleProperty(description = "Repeat Mode : One")
    fun RepeatModeOne() = REPEAT_MODE_ONE

    @SimpleProperty(description = "Repeat Mode : All")
    fun RepeatModeAll() = REPEAT_MODE_All

    @SimpleProperty(description = "Repeat Mode : One & All")
    fun RepeatModeOneAll() = REPEAT_MODE_ONE_ALL

    // Shuffle Button
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "False"
    )
    @SimpleProperty(description = "Show/Hide shuffle playlist button")
    fun ShuffleButtonVisible(show: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.setShowShuffleButton(show)
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setShowShuffleButton(show)

        showShuffleButton = show
    }

    // Show next button
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Show/Hide next button")
    fun NextButtonVisible(show: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.setShowNextButton(show)
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setShowNextButton(show)

        showNextButton = show
    }

    // Show previous button
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Show/Hide previous button")
    fun PreviousButtonVisible(show: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.setShowPreviousButton(show)
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setShowPreviousButton(show)

        showPreviousButton = show
    }

    // Show rewind button
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Show/Hide rewind button")
    fun RewindButtonVisible(show: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.setShowRewindButton(show)
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setShowRewindButton(show)

        showRewindButton = show
    }

    // Show fast forward button
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Show/Hide fast forward  button")
    fun FastForwardButtonVisible(show: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.setShowFastForwardButton(show)
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setShowFastForwardButton(show)

        showFastForwardButton = show
    }

    // Show subtitle button
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Show/Hide fast forward  button")
    fun SubtitleButtonVisible(show: Boolean) {
        styledPlayerView?.setShowSubtitleButton(show)
        showSubtitlesButton = show
    }

    // Show Loading
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = [SHOW_BUFFERING_WHEN_PLAYING, SHOW_BUFFERING_NEVER, SHOW_BUFFERING_ALWAYS],
        defaultValue = SHOW_BUFFERING_WHEN_PLAYING
    )
    @SimpleProperty(description = "Set when to show loading progress indicator")
    fun ShowLoading(show: String) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.setShowBuffering(getBufferingModeFromString(show))
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setShowBuffering(getBufferingModeFromString(show))

        bufferingMode = show
    }

    @SimpleProperty(description = "Always keep Loading hidden.")
    fun ShowLoadingNever() = SHOW_BUFFERING_NEVER

    @SimpleProperty(description = "Show loading when playing.")
    fun ShowLoadingWhenPlaying() = SHOW_BUFFERING_WHEN_PLAYING

    @SimpleProperty(description = "Always show Loading.")
    fun ShowLoadingAlways() = SHOW_BUFFERING_ALWAYS

    // Controller auto show
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Automatically show controller")
    fun AutoShowController(show: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.controllerAutoShow = show
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.controllerAutoShow = show

        autoShowController = show
    }

    // Hide controller on touch
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Hide controller on touch")
    fun HideOnTouch(hide: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.controllerHideOnTouch = hide
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.controllerHideOnTouch = hide

        hideOnTouch = hide
    }

    // Controller Show timeout
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
        defaultValue = "${PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS}"
    )
    @SimpleProperty(description = "Milliseconds after which the controller should hide")
    fun ControllerTimeout(mills: Int) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.controllerShowTimeoutMs = mills
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.controllerShowTimeoutMs = mills

        controllerTimeout = mills
    }

    // Set resize mode
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = [RESIZE_MODE_FIT, RESIZE_MODE_FILL, RESIZE_MODE_ZOOM, RESIZE_MODE_FIXED_WIDTH, RESIZE_MODE_FIXED_HEIGHT],
        defaultValue = RESIZE_MODE_FIT
    )
    @SimpleProperty(description = "Set video resize mode")
    fun ResizeMode(mode: String) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.resizeMode = getResizeModeFromString(mode)
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.resizeMode = getResizeModeFromString(mode)

        resizeMode = mode
    }

    // Resize Modes
    @SimpleProperty(description = RESIZE_MODE_FIT)
    fun ResizeModeFit() = RESIZE_MODE_FIT

    @SimpleProperty(description = RESIZE_MODE_FILL)
    fun ResizeModeFill() = RESIZE_MODE_FILL

    @SimpleProperty(description = RESIZE_MODE_ZOOM)
    fun ResizeModeZoom() = RESIZE_MODE_ZOOM

    @SimpleProperty(description = RESIZE_MODE_FIXED_WIDTH)
    fun ResizeModeFixedWidth() = RESIZE_MODE_FIXED_WIDTH

    @SimpleProperty(description = RESIZE_MODE_FIXED_HEIGHT)
    fun ResizeModeFixedHeight() = RESIZE_MODE_FIXED_HEIGHT

    // Default Artwork
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET,
        defaultValue = ""
    )
    @SimpleProperty(description = "Set default thumbnail in case if there is no thumbnail in media metadata, the player will show this thumbnail. It works only for audio files.")
    fun DefaultThumbnail(path: String) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.defaultArtwork = getDrawable(form, path)
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.defaultArtwork = getDrawable(form, path)
    }

    // Use Artwork
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Whether to show an image (thumbnail) if current media is an audio.")
    fun UseArtwork(shouldUse: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.useArtwork = shouldUse
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.useArtwork = shouldUse

        useArtwork = shouldUse
    }

    // Use Controller
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Whether to use default controller or not.")
    fun UseController(shouldUse: Boolean) {
        if (playerType == PlayerViewType.SimplePlayerView)
            playerView?.useController = shouldUse
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.useController = shouldUse

        useController = shouldUse
    }

    // Animation Enabled
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Whether to animate show and hide of player. Set before creating player.")
    fun AnimationEnabled(enabled: Boolean) {
        animationEnabled = enabled
    }

    // Fast Forward Milliseconds
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
        defaultValue = "${DefaultControlDispatcher.DEFAULT_FAST_FORWARD_MS}"
    )
    @SimpleProperty(description = "Set fast forward milliseconds. Set before creating player.")
    fun FastForwardMs(mills: Int) {
        fastForwardMs = mills
    }

    // Rewind Milliseconds
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
        defaultValue = "${DefaultControlDispatcher.DEFAULT_REWIND_MS}"
    )
    @SimpleProperty(description = "Set fast forward milliseconds. Set before creating player.")
    fun RewindMs(mills: Int) {
        rewindMs = mills
    }

    // Controls Visible
    @SimpleProperty(description = "Check if controls are currently visible")
    fun ControlsVisible(): Boolean =
        when (playerType) {
            PlayerViewType.SimplePlayerView -> playerView?.isControllerVisible ?: false
            PlayerViewType.StyledPlayerView -> styledPlayerView?.isControllerFullyVisible ?: false
            else -> false
        }
}
