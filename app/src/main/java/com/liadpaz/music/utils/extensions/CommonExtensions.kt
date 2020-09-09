package com.liadpaz.music.utils.extensions

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
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
        return it
    }
    getDarkVibrantColor(Color.BLACK).takeIf { it != Color.BLACK && isDarkColor(it) }?.let {
        return it
    }
    getLightMutedColor(Color.BLACK).takeIf { it != Color.BLACK && isDarkColor(it) }?.let {
        return it
    }
    getLightVibrantColor(Color.BLACK).takeIf { it != Color.BLACK && isDarkColor(it) }?.let {
        return it
    }
    return Color.BLACK
}

private fun isDarkColor(@ColorInt colorInt: Int): Boolean =
    (0.2126 * Color.red(colorInt) + 0.7152 * Color.green(colorInt) + 0.0722 * Color.blue(colorInt)) > 0.5

fun Int?.isNullOrZero(): Boolean = this == null || this == 0

val ViewGroup.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this.context)