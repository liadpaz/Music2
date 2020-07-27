package com.liadpaz.music.utils

import android.support.v4.media.MediaBrowserCompat
import androidx.recyclerview.widget.DiffUtil

object C {

    val diffCallback = object : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaBrowserCompat.MediaItem, newItem: MediaBrowserCompat.MediaItem): Boolean =
            oldItem.mediaId == newItem.mediaId

        override fun areContentsTheSame(oldItem: MediaBrowserCompat.MediaItem, newItem: MediaBrowserCompat.MediaItem): Boolean =
            false //oldItem.description == newItem.description // TODO: implement
    }
}