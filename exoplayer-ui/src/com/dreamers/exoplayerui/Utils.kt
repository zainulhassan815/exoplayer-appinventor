package com.dreamers.exoplayerui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.ReplForm
import java.io.FileInputStream
import java.io.InputStream

internal fun getDrawable(form: Form, fileName: String): Drawable? {
    return try {
        val inputStream: InputStream? = getAsset(form, fileName)
        val drawable: Drawable = Drawable.createFromStream(inputStream, null)
        inputStream?.close()
        drawable
    } catch (e: Exception) {
        Log.v(ExoplayerUi.LOG_TAG, "getDrawable : Error = $e")
        null
    }
}

internal fun getAsset(form: Form, file: String): InputStream? {
    val context = form.`$context`()
    val isDebugMode = form is ReplForm
    return try {
        if (isDebugMode) {
            val path: String = getAssetPath(context, file)
            Log.v(ExoplayerUi.LOG_TAG, "getAsset | Filepath = $path")
            FileInputStream(path)
        } else {
            context.assets.open(file)
        }
    } catch (e: Exception) {
        Log.e(ExoplayerUi.LOG_TAG, "getAsset | Debug Mode : $isDebugMode | Error : $e")
        null
    }
}

internal fun getTypeface(context: Context, asset: String): Typeface? {
    return try {
        val path = getAssetPath(context, asset)
        Typeface.createFromFile(path)
    } catch (e: Exception) {
        Log.e(ExoplayerUi.LOG_TAG, "getTypeface | Failed to get typeface from path : $asset with error : $e")
        null
    }
}

private fun getAssetPath(context: Context, file: String) = when {
    context.javaClass.name.contains("makeroid") -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(null).toString() + "/assets/$file"
        } else {
            "/storage/emulated/0/Kodular/assets/$file"
        }
    }
    else -> context.getExternalFilesDir(null).toString() + "/AppInventor/assets/$file"
}

internal fun getResizeMode(mode: String): Int = when (mode) {
    ExoplayerUi.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    ExoplayerUi.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    ExoplayerUi.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    ExoplayerUi.RESIZE_MODE_FIXED_HEIGHT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    ExoplayerUi.RESIZE_MODE_FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

internal fun getBufferingMode(mode: String): Int = when (mode) {
    ExoplayerUi.SHOW_BUFFERING_ALWAYS -> PlayerView.SHOW_BUFFERING_ALWAYS
    ExoplayerUi.SHOW_BUFFERING_NEVER -> PlayerView.SHOW_BUFFERING_NEVER
    ExoplayerUi.SHOW_BUFFERING_WHEN_PLAYING -> PlayerView.SHOW_BUFFERING_WHEN_PLAYING
    else -> PlayerView.SHOW_BUFFERING_WHEN_PLAYING
}

internal fun getRepeatMode(mode: String): Int = when (mode) {
    ExoplayerUi.REPEAT_MODE_OFF -> Player.REPEAT_MODE_OFF
    ExoplayerUi.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
    ExoplayerUi.REPEAT_MODE_All -> Player.REPEAT_MODE_ALL
    ExoplayerUi.REPEAT_MODE_ONE_ALL -> Player.REPEAT_MODE_ONE.or(Player.REPEAT_MODE_ALL)
    else -> Player.REPEAT_MODE_OFF
}

internal fun getSubtitleViewType(type: String): Int = when (type) {
    ExoplayerUi.VIEW_TYPE_CANVAS -> SubtitleView.VIEW_TYPE_CANVAS
    ExoplayerUi.VIEW_TYPE_WEB -> SubtitleView.VIEW_TYPE_WEB
    else -> SubtitleView.VIEW_TYPE_CANVAS
}

internal fun getEdgeType(type: String): Int = when (type) {
    ExoplayerUi.EDGE_TYPE_NONE -> CaptionStyleCompat.EDGE_TYPE_NONE
    ExoplayerUi.EDGE_TYPE_OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
    ExoplayerUi.EDGE_TYPE_DROP_SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
    ExoplayerUi.EDGE_TYPE_RAISED -> CaptionStyleCompat.EDGE_TYPE_RAISED
    ExoplayerUi.EDGE_TYPE_DEPRESSED -> CaptionStyleCompat.EDGE_TYPE_DEPRESSED
    else -> CaptionStyleCompat.EDGE_TYPE_NONE
}

internal fun getSizeType(type: String): Int = when (type) {
    ExoplayerUi.TEXT_SIE_TYPE_FRACTION -> Cue.TEXT_SIZE_TYPE_FRACTIONAL
    ExoplayerUi.TEXT_SIE_TYPE_ABSOLUTE -> Cue.TEXT_SIZE_TYPE_ABSOLUTE
    else -> Cue.TEXT_SIZE_TYPE_FRACTIONAL
}

internal fun getSurfaceType(type: String): Int = when(type) {
    ExoplayerUi.SURFACE_TYPE_NONE -> PlayerView.SURFACE_TYPE_NONE
    ExoplayerUi.SURFACE_TYPE_SURFACE_VIEW -> PlayerView.SURFACE_TYPE_SURFACE_VIEW
    ExoplayerUi.SURFACE_TYPE_TEXTURE_VIEW -> PlayerView.SURFACE_TYPE_TEXTURE_VIEW
    else -> PlayerView.SURFACE_TYPE_SURFACE_VIEW
}