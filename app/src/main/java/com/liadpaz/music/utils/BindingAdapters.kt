package com.liadpaz.music.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.liadpaz.music.R
import com.liadpaz.music.service.utils.EXTRA_SONGS_NUM
import com.liadpaz.music.ui.adapters.*
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.extensions.findColor

@BindingAdapter("songs")
fun setSongs(recyclerView: RecyclerView, songs: List<MediaBrowserCompat.MediaItem>?) = (recyclerView.adapter as? SongsAdapter)?.submitList(songs)

@BindingAdapter("playlistSongs")
fun setPlaylistSongs(recyclerView: RecyclerView, songs: List<MediaBrowserCompat.MediaItem>?) =
	(recyclerView.adapter as? PlaylistAdapter)?.submitList(songs)

@BindingAdapter("playlists")
fun setPlaylists(recyclerView: RecyclerView, playlists: List<MediaBrowserCompat.MediaItem>?) =
	(recyclerView.adapter as? PlaylistsAdapter)?.submitList(playlists)

@BindingAdapter("albums")
fun setAlbums(recyclerView: RecyclerView, albums: List<MediaBrowserCompat.MediaItem>?) = (recyclerView.adapter as? AlbumsAdapter)?.submitList(albums)

@BindingAdapter("artists")
fun setArtists(recyclerView: RecyclerView, artists: List<MediaBrowserCompat.MediaItem>?) =
	(recyclerView.adapter as? ArtistsAdapter)?.submitList(artists)

@BindingAdapter("uri")
fun setUri(imageView: ImageView, uri: Uri?) = GlideApp.with(imageView).load(uri).into(imageView)

@BindingAdapter("playlistUri")
fun setPlaylistUri(imageView: ImageView, bundle: Bundle?) =
	bundle?.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI).takeUnless { uri -> uri.isNullOrEmpty() }?.let { uri ->
		GlideApp.with(imageView).load(Uri.parse(uri)).into(imageView)
	}

@BindingAdapter("playPause")
fun setPlayPause(imageButton: ImageButton, playbackState: PlaybackStateCompat?) = GlideApp.with(imageButton).load(
	when (playbackState?.state) {
		PlaybackStateCompat.STATE_BUFFERING,
		PlaybackStateCompat.STATE_PLAYING,
		-> R.drawable.ic_notification_pause
		else -> R.drawable.ic_notification_play
	}).into(imageButton)

@BindingAdapter("resource")
fun setResource(imageButton: ImageButton, @DrawableRes res: Int) =
	GlideApp.with(imageButton).load(res).into(imageButton)

@BindingAdapter("queue")
fun setQueue(recyclerView: RecyclerView, queue: List<MediaMetadataCompat>?) = (recyclerView.adapter as? QueueAdapter)?.submitList(queue)

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

@BindingAdapter("background")
fun setBackground(view: View, palette: Palette?) = palette?.let {
	view.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(it.getDominantColor(Color.GRAY), it.findColor()))
}

@BindingAdapter("repeatMode")
fun setRepeatMode(imageButton: ImageButton, @PlaybackStateCompat.RepeatMode repeatMode: Int) =
	GlideApp.with(imageButton).load(if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) R.drawable.ic_repeat else R.drawable.ic_repeat_one).into(imageButton)

@BindingAdapter("artistSongs")
fun setArtistSongs(textView: TextView, artist: MediaBrowserCompat.MediaItem) {
	textView.text = textView.context.getString(
		R.string.item_artist_songs, artist.description.extras?.getInt(EXTRA_SONGS_NUM) ?: 0
	)
}

@BindingAdapter("playlistSongs")
fun setPlaylistSongs(textView: TextView, bundle: Bundle?) {
	textView.text = textView.context.getString(R.string.item_artist_songs, bundle?.getInt(EXTRA_SONGS_NUM))
}

@BindingAdapter("queuePair")
fun setQueuePair(viewPager2: ViewPager2, queuePair: Pair<List<MediaMetadataCompat>, Int>?) = queuePair?.takeIf { it.first.isNotEmpty() }?.let {
	(viewPager2.adapter as? AlbumArtViewPagerAdapter)?.submitList(queuePair.first)
	viewPager2.setCurrentItem(it.second, false)
}

@BindingAdapter("smallPlaylists")
fun setSmallPlaylists(recyclerView: RecyclerView, playlists: List<MediaBrowserCompat.MediaItem>?) =
	(recyclerView.adapter as? SmallPlaylistsAdapter)?.setList(playlists?.map { it.description.title.toString() }?.subList(1, playlists.size))

@BindingAdapter("smallArtists")
fun setSmallArtists(recyclerView: RecyclerView, artists: List<String>?) = (recyclerView.adapter as? SmallArtistsAdapter)?.setList(artists)

private const val TAG = "BindingAdapters"