package com.liadpaz.music.service

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.liadpaz.music.service.utils.ARTISTS_ROOT
import com.liadpaz.music.service.utils.BrowseTree
import com.liadpaz.music.utils.extensions.*

class PlaybackPreparer(private val browseTree: BrowseTree, private val exoPlayer: ExoPlayer, private val dataSourceFactory: DefaultDataSourceFactory) : MediaSessionConnector.PlaybackPreparer {

    private var mediaSource = ConcatenatingMediaSource()

    override fun getSupportedPrepareActions(): Long =
        PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

    override fun onPrepare(playWhenReady: Boolean) = Unit

    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        val shuffle = extras?.get(EXTRA_SHUFFLE) as? Boolean ?: false

        val itemToPlay: MediaMetadataCompat = browseTree.find { item ->
            item.id == mediaId
        }!!
        val metadataList = when (extras?.getString(EXTRA_FROM)) {
            EXTRA_FROM_PLAYLISTS -> TODO("implement custom playlists")
            EXTRA_FROM_ALBUMS -> buildPlaylistWithAlbum(itemToPlay).apply { if (shuffle) shuffled() }
            EXTRA_FROM_ARTISTS -> buildPlaylistWithArtist((extras[EXTRA_ARTIST] as String).substring(ARTISTS_ROOT.length)).apply { if (shuffle) shuffled() }
            else -> browseTree.toList().apply { if (shuffle) shuffled() }
        }

        mediaSource = ConcatenatingMediaSource(*metadataList.toMediaSources(dataSourceFactory).toTypedArray())

        val initialWindowIndex =
            extras?.getInt(EXTRA_POSITION, metadataList.indexOf(itemToPlay))
                ?: metadataList.indexOf(itemToPlay)

        exoPlayer.prepare(mediaSource)
        exoPlayer.seekTo(initialWindowIndex, 0)
    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
        val metadataList = browseTree.search(query, extras ?: Bundle.EMPTY)
        if (metadataList.isNotEmpty()) {
            mediaSource = ConcatenatingMediaSource(*metadataList.toMediaSources(dataSourceFactory).toTypedArray())
            exoPlayer.prepare(mediaSource)
        }
    }

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

    override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean =
        false

    private fun buildPlaylistWithAlbum(item: MediaMetadataCompat): List<MediaMetadataCompat> =
        browseTree.filter { it.album == item.album }

    private fun buildPlaylistWithArtist(artist: String): List<MediaMetadataCompat> =
        browseTree.filter { it.artist.containsCaseInsensitive(artist) }

    fun addQueueItem(item: MediaDescriptionCompat, position: Int) =
        mediaSource.addMediaSource(position, item.toMediaSource(dataSourceFactory))

    fun addQueueItem(item: MediaDescriptionCompat) =
        mediaSource.addMediaSource(item.toMediaSource(dataSourceFactory))

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = mediaSource.moveMediaSource(fromIndex, toIndex)

    fun removeQueueItem(position: Int): MediaSource = mediaSource.removeMediaSource(position)
}


const val EXTRA_FROM = "from"
const val EXTRA_FROM_ALL = "all"
const val EXTRA_FROM_PLAYLISTS = "playlists"
const val EXTRA_FROM_ALBUMS = "albums"
const val EXTRA_FROM_ARTISTS = "artists"

const val EXTRA_ARTIST = "artist"
const val EXTRA_SHUFFLE = "shuffle"
const val EXTRA_POSITION = "position"

private const val TAG = "PlaybackPreparer"