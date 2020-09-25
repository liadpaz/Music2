package com.liadpaz.music.utils.extensions

import android.graphics.Color
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
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

fun MediaDescriptionCompat.toMediaMetadataBuilder() = MediaMetadataCompat.Builder().also { builder ->
    builder.id = mediaId!!
    builder.mediaUri = mediaUri.toString()
    builder.title = title.toString()
    builder.displayTitle = title.toString()
    builder.artist = subtitle.toString()
    builder.displaySubtitle = subtitle.toString()
    builder.album = description.toString()
    builder.displayDescription = description.toString()
    builder.albumArtUri = iconUri.toString()
    builder.displayIconUri = iconUri.toString()
}

fun MediaDescriptionCompat.toMediaMetadata(): MediaMetadataCompat = toMediaMetadataBuilder().build()

@ColorInt
fun Palette.findColor(): Int =
    listOf(getDarkMutedColor(Color.BLACK), getDarkVibrantColor(Color.BLACK), getLightMutedColor(Color.BLACK), getLightVibrantColor(Color.BLACK)).firstOrNull { it != Color.BLACK }
        ?: Color.BLACK

fun Int?.isNullOrZero(): Boolean = this == null || this == 0

val ViewGroup.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this.context)