package com.liadpaz.music.utils

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.recyclerview.widget.DiffUtil

object C {

    val diffCallback = object : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaBrowserCompat.MediaItem, newItem: MediaBrowserCompat.MediaItem): Boolean =
            oldItem.mediaId == newItem.mediaId

        override fun areContentsTheSame(oldItem: MediaBrowserCompat.MediaItem, newItem: MediaBrowserCompat.MediaItem): Boolean =
            false //oldItem.description == newItem.description // TODO: implement
    }

    val queueDiffCallback = object : DiffUtil.ItemCallback<MediaSessionCompat.QueueItem>() {
        override fun areItemsTheSame(oldItem: MediaSessionCompat.QueueItem, newItem: MediaSessionCompat.QueueItem): Boolean =
            oldItem.queueId == newItem.queueId

        override fun areContentsTheSame(oldItem: MediaSessionCompat.QueueItem, newItem: MediaSessionCompat.QueueItem): Boolean =
            false //oldItem.description == newItem.description // TODO: implement
    }
}