package com.liadpaz.music.utils

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.ImageButton
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.liadpaz.music.R
import com.liadpaz.music.service.utils.EXTRA_SONGS_NUM
import com.liadpaz.music.ui.adapters.AlbumsAdapter
import com.liadpaz.music.ui.adapters.ArtistsAdapter
import com.liadpaz.music.ui.adapters.SongsAdapter

@BindingAdapter("songs")
fun setSongs(recyclerView: RecyclerView, songs: List<MediaBrowserCompat.MediaItem>?) =
    (recyclerView.adapter as? SongsAdapter)?.submitList(songs)

// TODO: implement playlists

@BindingAdapter("albums")
fun setAlbums(recyclerView: RecyclerView, albums: List<MediaBrowserCompat.MediaItem>?) =
    (recyclerView.adapter as? AlbumsAdapter)?.submitList(albums)

@BindingAdapter("artists")
fun setArtists(recyclerView: RecyclerView, artists: List<MediaBrowserCompat.MediaItem>?) =
    (recyclerView.adapter as? ArtistsAdapter)?.submitList(artists)

@BindingAdapter("uri")
fun setUri(imageView: ImageView, uri: Uri?) {
    GlideApp.with(imageView).load(uri).into(imageView)
}

@BindingAdapter("playPause")
fun setPlayPause(imageButton: ImageButton, playbackState: PlaybackStateCompat?) {
    GlideApp.with(imageButton).load(
        when (playbackState?.state) {
            PlaybackStateCompat.STATE_BUFFERING,
            PlaybackStateCompat.STATE_PLAYING -> R.drawable.ic_notification_pause
            else -> R.drawable.ic_notification_play
        }
    ).into(imageButton)
}

@BindingAdapter("artistSongs")
fun setArtistSongs(textView: MaterialTextView, artist: MediaBrowserCompat.MediaItem) {
    textView.text = textView.context.getString(
        R.string.item_artist_songs, artist.description.extras?.getInt(EXTRA_SONGS_NUM) ?: 0
    )
}