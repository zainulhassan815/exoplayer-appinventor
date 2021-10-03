package com.dreamers.exoplayerui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build.VERSION
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.ui.*
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.*

@Suppress("FunctionName")
class ExoplayerUi(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), Component,
    OnPauseListener, OnResumeListener {

    init {
        // Need to register extension for activity changes
        form.registerForOnPause(this)
        form.registerForOnResume(this)
    }

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
    private var showFullscreenButton: Boolean = true
    private var hideOnTouch: Boolean = true
    private var autoShowController: Boolean = true
    private var useArtwork: Boolean = true
    private var animationEnabled: Boolean = true
    private var useController: Boolean = true
    private var fastForwardMs: Int = DefaultControlDispatcher.DEFAULT_FAST_FORWARD_MS
    private var rewindMs: Int = DefaultControlDispatcher.DEFAULT_REWIND_MS
    private var playerType: PlayerViewType? = null

    private var subtitleTextSizeFraction: Float = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION
    private var subtitleTextSizeAbsolute: Float = 14f
    private var ignorePadding: Boolean = false
    private var subtitleTextSizeType: Int = Cue.TEXT_SIZE_TYPE_FRACTIONAL
    private var subtitleViewType: Int = SubtitleView.VIEW_TYPE_CANVAS
    private var subtitleBottomPadding: Float = SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION

    private var subtitleForegroundColor: Int = Color.WHITE
    private var subtitleBackgroundColor: Int = Color.BLACK
    private var subtitleWindowColor: Int = Color.TRANSPARENT
    private var subtitleEdgeColor: Int = Color.WHITE
    private var subtitleEdgeType: Int = CaptionStyleCompat.EDGE_TYPE_NONE
    private var subtitleTypeface: Typeface? = null

    private var trackColor: Int = DefaultTimeBar.DEFAULT_UNPLAYED_COLOR
    private var thumbColor: Int = DefaultTimeBar.DEFAULT_SCRUBBER_COLOR
    private var progressColor: Int = DefaultTimeBar.DEFAULT_PLAYED_COLOR
    private var bufferedColor: Int = DefaultTimeBar.DEFAULT_BUFFERED_COLOR

    private var trackHeight: Int = DefaultTimeBar.DEFAULT_BAR_HEIGHT_DP
    private var thumbSize: Int = DefaultTimeBar.DEFAULT_SCRUBBER_ENABLED_SIZE_DP
    private var thumbSizeDisabled: Int = DefaultTimeBar.DEFAULT_SCRUBBER_DISABLED_SIZE_DP
    private var thumbSizeActive: Int = DefaultTimeBar.DEFAULT_SCRUBBER_DRAGGED_SIZE_DP

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
        const val VIEW_TYPE_CANVAS = "Canvas"
        const val VIEW_TYPE_WEB = "Web"
        const val EDGE_TYPE_NONE = "No Edge"
        const val EDGE_TYPE_OUTLINE = "Outline"
        const val EDGE_TYPE_DROP_SHADOW = "Drop Shadow"
        const val EDGE_TYPE_RAISED = "Raised"
        const val EDGE_TYPE_DEPRESSED = "Depressed"
        const val TEXT_SIE_TYPE_FRACTION = "Fractional Size"
        const val TEXT_SIE_TYPE_ABSOLUTE = "Absolute Size"
    }

    // On Pause
    override fun onPause() {
        Log.v(LOG_TAG, "onPause")
        if (VERSION.SDK_INT < 24) {
            if (playerType == PlayerViewType.SimplePlayerView)
                playerView?.onPause()
            else if (playerType == PlayerViewType.StyledPlayerView)
                styledPlayerView?.onPause()
        }
    }

    // On Resume
    override fun onResume() {
        Log.v(LOG_TAG, "onResume")
        if (VERSION.SDK_INT < 24) {
            if (playerType == PlayerViewType.SimplePlayerView)
                playerView?.onResume()
            else if (playerType == PlayerViewType.StyledPlayerView)
                styledPlayerView?.onResume()
        }
    }

    private val subtitleView: SubtitleView?
        get() {
            return when (playerType) {
                PlayerViewType.SimplePlayerView -> playerView?.subtitleView
                PlayerViewType.StyledPlayerView -> styledPlayerView?.subtitleView
                else -> null
            }
        }

    // Initialize player view
    private fun initialize(layout: HVArrangement, exoplayer: SimpleExoPlayer, playerType: PlayerViewType) {

        this.playerType = playerType
        val viewGroup: ViewGroup = layout.view as ViewGroup
        if (isDebugMode) Log.v(LOG_TAG, "createLayout | Debug mode : true")
        val playerAttributes = PlayerAttributes(
            /* useArtwork */useArtwork,
            /* resizeMode */getResizeMode(resizeMode),
            /* controllerTimeout */controllerTimeout,
            /* hideOnTouch */hideOnTouch,
            /* autoShowController */autoShowController,
            /* showBuffering */getBufferingMode(bufferingMode),
            /* useController */useController,
            /* hideDuringAds */true,
            /* isDebugMode */isDebugMode,
            /* rewindMs */rewindMs,
            /* fastForwardMs */fastForwardMs,
            /* repeatToggleModes */getRepeatMode(repeatMode),
            /* showRewindButton */showRewindButton,
            /* showFastForwardButton */showFastForwardButton,
            /* showPreviousButton */showPreviousButton,
            /* showNextButton */showNextButton,
            /* showShuffleButton */showShuffleButton,
            /* showSubtitleButton */showSubtitlesButton,
            /* showFullscreenButton */showFullscreenButton,
            /* animationEnabled */animationEnabled
        )

        val timeBarAttributes = TimeBarAttributes(
            null,
            trackHeight,
            DefaultTimeBar.DEFAULT_TOUCH_TARGET_HEIGHT_DP,
            Gravity.BOTTOM,
            DefaultTimeBar.DEFAULT_AD_MARKER_WIDTH_DP,
            thumbSize,
            thumbSizeDisabled,
            thumbSizeActive,
            progressColor,
            thumbColor,
            bufferedColor,
            trackColor,
            DefaultTimeBar.DEFAULT_AD_MARKER_COLOR,
            DefaultTimeBar.DEFAULT_PLAYED_AD_MARKER_COLOR
        )

        if (playerType == PlayerViewType.SimplePlayerView) {
            playerView = PlayerView(context, timeBarAttributes, playerAttributes).also { view ->
                view.player = exoplayer
                view.setKeepContentOnPlayerReset(true)
                view.setControllerVisibilityListener {
                    OnVisibilityChanged(it == View.VISIBLE)
                }
            }
        } else {
            styledPlayerView = StyledPlayerView(context, timeBarAttributes, playerAttributes).also { view ->
                view.player = exoplayer
                view.setKeepContentOnPlayerReset(true)
                view.setControllerVisibilityListener {
                    OnVisibilityChanged(it == View.VISIBLE)
                }
                if (showFullscreenButton)
                    view.setControllerOnFullScreenModeChangedListener {
                        OnFullscreenChanged(it)
                    }
            }
        }

        // Set subtitle styles
        subtitleView?.apply {
            SubtitleTextSizeFraction(subtitleTextSizeFraction)
            SubtitleTextSizeAbsolute(subtitleTextSizeAbsolute)
            setViewType(subtitleViewType)
            setBottomPaddingFraction(subtitleBottomPadding)
            setStyle(
                CaptionStyleCompat(
                    subtitleForegroundColor,
                    subtitleBackgroundColor,
                    subtitleWindowColor,
                    subtitleEdgeType,
                    subtitleEdgeColor,
                    subtitleTypeface
                )
            )
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

    @SimpleFunction(description = "Hide System UI. Use with caution. Still in testing.")
    fun HideSystemUI() {
        val window = (context as Activity).window
        window.decorView.fitsSystemWindows = true
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Hide the nav bar and status bar
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)

    }

    @SimpleFunction(description = "Show System UI. Use with caution. Still in testing.")
    fun ShowSystemUI() {
        val window = (context as Activity).window
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_VISIBLE)
    }

    // On Visibility Changed
    @SimpleEvent(description = "Event raised when controls visibility changes.")
    fun OnVisibilityChanged(visible: Boolean) {
        EventDispatcher.dispatchEvent(this, "OnVisibilityChanged", visible)
    }

    // On Fullscreen Change
    @SimpleEvent(description = "Event raised when fullscreen button is clicked.")
    fun OnFullscreenChanged(isFullScreen: Boolean) {
        EventDispatcher.dispatchEvent(this, "OnFullscreenChanged", isFullScreen)
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
            playerView?.setRepeatToggleModes(getRepeatMode(mode))
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setRepeatToggleModes(getRepeatMode(mode))

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

    // Show fullscreen button
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "True"
    )
    @SimpleProperty(description = "Show/Hide fullscreen button.")
    fun FullscreenButtonVisible(show: Boolean) {
        showFullscreenButton = show
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
            playerView?.setShowBuffering(getBufferingMode(show))
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.setShowBuffering(getBufferingMode(show))

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
            playerView?.resizeMode = getResizeMode(mode)
        else if (playerType == PlayerViewType.StyledPlayerView)
            styledPlayerView?.resizeMode = getResizeMode(mode)

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

    // Subtitle Text Size Type
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = [TEXT_SIE_TYPE_FRACTION, TEXT_SIE_TYPE_ABSOLUTE],
        defaultValue = TEXT_SIE_TYPE_FRACTION
    )
    @SimpleProperty(description = "Set subtitles text size type.")
    fun SubtitleTextSizeType(type: String) {
        subtitleTextSizeType = getSizeType(type)
    }

    @SimpleProperty
    fun TextSizeTypeFractional() = TEXT_SIE_TYPE_FRACTION

    @SimpleProperty
    fun TextSizeTypeAbsolute() = TEXT_SIE_TYPE_ABSOLUTE

    // Subtitle Text Size Fraction
    @DesignerProperty(
        defaultValue = "${SubtitleView.DEFAULT_TEXT_SIZE_FRACTION}",
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT
    )
    @SimpleProperty(description = "Set fractional text size for subtitles")
    fun SubtitleTextSizeFraction(size: Float) {
        subtitleTextSizeFraction = size
        if (subtitleTextSizeType == Cue.TEXT_SIZE_TYPE_FRACTIONAL)
            subtitleView?.setFractionalTextSize(size, ignorePadding)
    }

    // Subtitle Text Size Absolute
    @DesignerProperty(
        defaultValue = "14",
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT
    )
    @SimpleProperty(description = "Set absolute text size for subtitles")
    fun SubtitleTextSizeAbsolute(size: Float) {
        subtitleTextSizeAbsolute = size
        if (subtitleTextSizeType == Cue.TEXT_SIZE_TYPE_ABSOLUTE)
            subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleTextSizeAbsolute)
    }

    // Subtitle View Type
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = [VIEW_TYPE_CANVAS, VIEW_TYPE_WEB],
        defaultValue = VIEW_TYPE_CANVAS
    )
    @SimpleProperty(description = "Set subtitle output view type")
    fun SubtitleRenderViewType(type: String) {
        subtitleViewType = getSubtitleViewType(type)
        subtitleView?.setViewType(subtitleViewType)
    }

    @SimpleProperty
    fun SubtitleViewTypeCanvas() = VIEW_TYPE_CANVAS

    @SimpleProperty
    fun SubtitleViewTypeWeb() = VIEW_TYPE_WEB

    // Subtitle Bottom Padding
    @DesignerProperty(
        defaultValue = "14",
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT
    )
    @SimpleProperty(description = "Set subtitle bottom padding")
    fun SubtitleBottomPadding(padding: Float) {
        subtitleBottomPadding = padding
        subtitleView?.setBottomPaddingFraction(padding)
    }

    // Subtitle Ignore Padding
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
        defaultValue = "False"
    )
    @SimpleProperty(description = "Set to true if SubtitleTextSizeFraction should be interpreted as a fraction of this view's height ignoring any top and bottom padding. Set to false if SubtitleTextSizeFraction should be interpreted as a fraction of this view's remaining height after the top and bottom padding has been subtracted.")
    fun IgnorePadding(ignore: Boolean) {
        ignorePadding = ignore
    }

    // Foreground Color
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
        defaultValue = "&HFFFFFFFF"
    )
    @SimpleProperty(description = "Set foreground color for subtitles. Set before creating player.")
    fun SubtitleForegroundColor(color: Int) {
        subtitleForegroundColor = color
    }

    // Background Color
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
        defaultValue = "&HFF000000"
    )
    @SimpleProperty(description = "Set background color for subtitles. Set before creating player.")
    fun SubtitleBackgroundColor(color: Int) {
        subtitleBackgroundColor = color
    }

    // Window Color
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
        defaultValue = "&H00FFFFFF"
    )
    @SimpleProperty(description = "Set subtitles window color. Set before creating player.")
    fun SubtitleWindowColor(color: Int) {
        subtitleWindowColor = color
    }

    // Edge Color
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
        defaultValue = "&HFFFFFFFF"
    )
    @SimpleProperty(description = "Set edge color for subtitles. Set before creating player.")
    fun SubtitleEdgeColor(color: Int) {
        subtitleEdgeColor = color
    }

    // Edge Type
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = [EDGE_TYPE_NONE, EDGE_TYPE_DEPRESSED, EDGE_TYPE_DROP_SHADOW, EDGE_TYPE_OUTLINE, EDGE_TYPE_RAISED],
        defaultValue = EDGE_TYPE_NONE
    )
    @SimpleProperty(description = "Set subtitles edge type. Set before creating player.")
    fun SubtitleEdgeType(type: String) {
        subtitleEdgeType = getEdgeType(type)
    }

    @SimpleProperty
    fun EdgeTypeNone() = EDGE_TYPE_NONE

    @SimpleProperty
    fun EdgeTypeOutline() = EDGE_TYPE_OUTLINE

    @SimpleProperty
    fun EdgeTypeDropShadow() = EDGE_TYPE_DROP_SHADOW

    @SimpleProperty
    fun EdgeTypeRaised() = EDGE_TYPE_RAISED

    @SimpleProperty
    fun EdgeTypeDepressed() = EDGE_TYPE_DEPRESSED

    // Subtitle Typeface
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET,
        defaultValue = ""
    )
    @SimpleProperty(description = "Set custom font typeface for subtitles. Set before creating player.")
    fun SubtitleTypeface(asset: String) {
        subtitleTypeface = getTypeface(context, asset)
    }

    // TrackColor
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
        defaultValue = "&H33FFFFFF"
    )
    @SimpleProperty(description = "Track Color")
    fun TrackColor(color: Int) {
        trackColor = color
    }

    // ThumbColor
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
        defaultValue = "&HFFFFFFFF"
    )
    @SimpleProperty(description = "Thumb Color")
    fun ThumbColor(color: Int) {
        thumbColor = color
    }

    // ProgressColor
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
        defaultValue = "&HFFFFFFFF"
    )
    @SimpleProperty(description = "Progress Color")
    fun ProgressColor(color: Int) {
        progressColor = color
    }

    // BufferedColor
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
        defaultValue = "&HCCFFFFFF"
    )
    @SimpleProperty(description = "Buffered Color")
    fun BufferedColor(color: Int) {
        bufferedColor = color
    }

    // TrackHeight
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
        defaultValue = "${DefaultTimeBar.DEFAULT_BAR_HEIGHT_DP}"
    )
    @SimpleProperty(description = "Track Height. Set this value before creating slider.")
    fun TrackHeight(height: Int) {
        trackHeight = height
    }

    // ThumbSize
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
        defaultValue = "${DefaultTimeBar.DEFAULT_SCRUBBER_ENABLED_SIZE_DP}"
    )
    @SimpleProperty(description = "Thumb Size. Set this value before creating slider.")
    fun ThumbSize(size: Int) {
        thumbSize = size
    }

    // DisabledThumbSize
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
        defaultValue = "${DefaultTimeBar.DEFAULT_SCRUBBER_DISABLED_SIZE_DP}"
    )
    @SimpleProperty(description = "Disabled Thumb Size. Set this value before creating slider.")
    fun DisabledThumbSize(size: Int) {
        thumbSizeDisabled = size
    }

    // ActiveThumbSize
    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
        defaultValue = "${DefaultTimeBar.DEFAULT_SCRUBBER_DRAGGED_SIZE_DP}"
    )
    @SimpleProperty(description = "Active Thumb Size. Set this value before creating slider.")
    fun ActiveThumbSize(size: Int) {
        thumbSizeActive = size
    }
}
