package com.liadpaz.music.data

import android.support.v4.media.MediaBrowserCompat
import androidx.recyclerview.widget.DiffUtil

data class Playlist(val name: String, val songs: List<MediaBrowserCompat.MediaItem>) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Playlist>() {
            override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean = oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean = oldItem.songs == newItem.songs
        }
    }
}