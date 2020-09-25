package com.liadpaz.music.utils

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.recyclerview.widget.DiffUtil

object C {
    val diffCallback = object : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaBrowserCompat.MediaItem, newItem: MediaBrowserCompat.MediaItem): Boolean =
            oldItem.mediaId == newItem.mediaId

        override fun areContentsTheSame(oldItem: MediaBrowserCompat.MediaItem, newItem: MediaBrowserCompat.MediaItem): Boolean =
            equals(oldItem.description, newItem.description)
    }
}

private fun equals(media1: MediaDescriptionCompat, media2: MediaDescriptionCompat): Boolean =
    media1.title == media2.title && media1.description == media2.description && media1.subtitle == media2.subtitle && media1.mediaId == media2.mediaId && media1.extras == media2.extras