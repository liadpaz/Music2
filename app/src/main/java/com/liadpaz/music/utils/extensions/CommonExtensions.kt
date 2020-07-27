package com.liadpaz.music.utils.extensions

import android.net.Uri
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