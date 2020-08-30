package com.liadpaz.music.utils.extensions

import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import java.util.*

fun String?.containsCaseInsensitive(other: String?) =
    if (this == null && other == null) {
        true
    } else if (this != null && other != null) {
        toLowerCase(Locale.getDefault()).contains(other.toLowerCase(Locale.getDefault()))
    } else {
        false
    }

fun String?.toUri(): Uri = this?.let { Uri.parse(it) } ?: Uri.EMPTY

@ColorInt
fun Palette.findDarkColor(): Int {
    getDarkMutedColor(Color.BLACK).takeIf { it != Color.BLACK && isDarkColor(it) }?.let {
        Log.d(TAG, "findDarkColor: dark muted")
        return it
    }
    getDarkVibrantColor(Color.BLACK).takeIf { it != Color.BLACK && isDarkColor(it) }?.let {
        Log.d(TAG, "findDarkColor: dark vibrant")
        return it
    }
    getLightMutedColor(Color.BLACK).takeIf { it != Color.BLACK && isDarkColor(it) }?.let {
        Log.d(TAG, "findDarkColor: light muted")
        return it
    }
    getLightVibrantColor(Color.BLACK).takeIf { it != Color.BLACK && isDarkColor(it) }?.let {
        Log.d(TAG, "findDarkColor: light vibrant")
        return it
    }
    return Color.BLACK
}

private fun isDarkColor(@ColorInt colorInt: Int): Boolean =
    (0.2126 * Color.red(colorInt) + 0.7152 * Color.green(colorInt) + 0.0722 * Color.blue(colorInt)) > 0.5

private const val TAG = "CommonExtensions"