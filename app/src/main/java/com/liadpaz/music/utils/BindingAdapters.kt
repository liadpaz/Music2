package com.liadpaz.music.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.liadpaz.music.R
import com.liadpaz.music.service.utils.EXTRA_SONGS_NUM
import com.liadpaz.music.ui.adapters.AlbumsAdapter
import com.liadpaz.music.ui.adapters.ArtistsAdapter
import com.liadpaz.music.ui.adapters.QueueAdapter
import com.liadpaz.music.ui.adapters.SongsAdapter
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.extensions.findDarkColor

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

@BindingAdapter("queue")
fun setQueue(recyclerView: RecyclerView, queue: List<MediaSessionCompat.QueueItem>?) =
    (recyclerView.adapter as? QueueAdapter)?.submitList(queue)

@BindingAdapter("max")
fun setMax(seekBar: SeekBar, max: Number?) {
    seekBar.max = max?.toInt() ?: 0
}

@BindingAdapter("time")
fun setTime(textView: TextView, time: Number?) {
    textView.text = PlayingViewModel.NowPlayingMetadata.timestampToMSS(time?.toLong() ?: 0)
}

@BindingAdapter("progress")
fun setProgress(seekBar: SeekBar, progress: Number?) {
    seekBar.progress = progress?.toInt() ?: 0
}

@BindingAdapter("colorFilter")
fun setColorFilter(imageButton: ImageButton, @ColorInt color: Int?) {
    color?.let { imageButton.setColorFilter(color) }
}

@BindingAdapter("gradientBackground")
fun setGradientBackground(view: View, palette: Palette?) {
    palette?.let {
        view.background =
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(it.getDominantColor(Color.GRAY), it.findDarkColor()))
    }
}

@BindingAdapter("repeatMode")
fun setRepeatMode(imageButton: ImageButton, @PlaybackStateCompat.RepeatMode repeatMode: Int) {
    GlideApp.with(imageButton).load(
        if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) R.drawable.ic_repeat else R.drawable.ic_repeat_one
    ).into(imageButton)
}

@BindingAdapter("artistSongs")
fun setArtistSongs(textView: MaterialTextView, artist: MediaBrowserCompat.MediaItem) {
    textView.text = textView.context.getString(
        R.string.item_artist_songs, artist.description.extras?.getInt(EXTRA_SONGS_NUM) ?: 0
    )
}