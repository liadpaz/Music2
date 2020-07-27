package com.liadpaz.music.data

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.recyclerview.widget.DiffUtil
import com.liadpaz.music.utils.extensions.artist

data class Song(val mediaId: Long, val mediaUri: Uri, val title: String, val artist: String, val album: String, val artUri: Uri, val duration: Int) {

    companion object {

        val diffCallback = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
                oldItem.mediaUri == newItem.mediaUri

            override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
                oldItem == newItem
        }
    }
}

private fun MediaMetadataCompat.artistsAsList(): List<String> =
    regex.findAll(artist.toString()).toList().map { result -> result.value }

fun MediaMetadataCompat.findArtists() = artistsAsList()

fun MediaMetadataCompat.firstArtist() = findArtists()[0]

private val regex = Regex("([^ &,]([^,&])*[^ ,&]+)")