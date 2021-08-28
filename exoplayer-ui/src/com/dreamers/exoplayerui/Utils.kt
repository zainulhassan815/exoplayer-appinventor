package com.dreamers.exoplayerui

import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.appinventor.components.runtime.Form
import com.google.appinventor.components.runtime.ReplForm
import java.io.File
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
            val path: String = when {
                Build.VERSION.SDK_INT >= 29 -> context.getExternalFilesDir(null).toString() + "/assets/$file"
                context.javaClass.name.contains("makeroid") -> "/storage/emulated/0/Kodular/assets/$file"
                else -> "/storage/emulated/0/AppInventor/assets/$file"
            }
            Log.v(ExoplayerUi.LOG_TAG, "getAsset | Filepath = $path")
            FileInputStream(File(path))
        } else {
            context.assets.open(file)
        }
    } catch (e: Exception) {
        Log.e(ExoplayerUi.LOG_TAG, "getAsset | Debug Mode : $isDebugMode | Error : $e")
        null
    }
}

internal fun getResizeModeFromString(mode: String): Int = when(mode) {
    ExoplayerUi.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    ExoplayerUi.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    ExoplayerUi.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    ExoplayerUi.RESIZE_MODE_FIXED_HEIGHT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    ExoplayerUi.RESIZE_MODE_FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

internal fun getBufferingModeFromString(mode: String): Int = when(mode) {
    ExoplayerUi.SHOW_BUFFERING_ALWAYS -> PlayerView.SHOW_BUFFERING_ALWAYS
    ExoplayerUi.SHOW_BUFFERING_NEVER -> PlayerView.SHOW_BUFFERING_NEVER
    ExoplayerUi.SHOW_BUFFERING_WHEN_PLAYING -> PlayerView.SHOW_BUFFERING_WHEN_PLAYING
    else -> PlayerView.SHOW_BUFFERING_WHEN_PLAYING
}

internal fun getRepeatModeFromString(mode: String): Int = when(mode) {
    ExoplayerUi.REPEAT_MODE_OFF -> Player.REPEAT_MODE_OFF
    ExoplayerUi.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
    ExoplayerUi.REPEAT_MODE_All -> Player.REPEAT_MODE_ALL
    ExoplayerUi.REPEAT_MODE_ONE_ALL -> Player.REPEAT_MODE_ONE.or(Player.REPEAT_MODE_ALL)
    else -> Player.REPEAT_MODE_OFF
}