package com.liadpaz.music.service

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.liadpaz.music.data.findArtists
import com.liadpaz.music.data.firstArtist
import com.liadpaz.music.service.utils.MusicSource
import com.liadpaz.music.utils.extensions.*

class PlaybackPreparer(private val musicSource: MusicSource, private val exoPlayer: ExoPlayer, private val dataSourceFactory: DefaultDataSourceFactory) : MediaSessionConnector.PlaybackPreparer {

    override fun getSupportedPrepareActions(): Long =
        PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

    override fun onPrepare(playWhenReady: Boolean) = Unit

    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        val shuffle = extras?.get(EXTRA_SHUFFLE) as? Boolean ?: false

        musicSource.whenReady {
            val itemToPlay: MediaMetadataCompat = musicSource.find { item ->
                item.id == mediaId
            }!!
            val metadataList = when (extras?.getString(EXTRA_FROM) ?: EXTRA_FROM_ALL) {
                EXTRA_FROM_ALL -> musicSource.filter { true }.apply { if (shuffle) shuffled() }
                EXTRA_FROM_PLAYLISTS -> TODO("implement custom playlists")
                EXTRA_FROM_ALBUMS -> buildPlaylistWithAlbum(itemToPlay).apply { if (shuffle) shuffled() }
                EXTRA_FROM_ARTISTS -> buildPlaylistWithArtist(itemToPlay).apply { if (shuffle) shuffled() }
                else -> throw IllegalArgumentException()
            }
            val mediaSource = metadataList.toMediaSource(dataSourceFactory)

            val initialWindowIndex =
                extras?.getInt(EXTRA_POSITION, metadataList.indexOf(itemToPlay))
                    ?: metadataList.indexOf(itemToPlay)

            exoPlayer.prepare(mediaSource)
            exoPlayer.seekTo(initialWindowIndex, 0)
            exoPlayer.playWhenReady = playWhenReady
        }
    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
        musicSource.whenReady {
            val metadataList = musicSource.search(query, extras ?: Bundle.EMPTY)
            if (metadataList.isNotEmpty()) {
                val mediaSource = metadataList.toMediaSource(dataSourceFactory)
                exoPlayer.prepare(mediaSource)
                exoPlayer.playWhenReady = playWhenReady
            }
        }
    }

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

    override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean =
        false

    private fun buildPlaylistWithAlbum(item: MediaMetadataCompat): List<MediaMetadataCompat> =
        musicSource.filter { it.album == item.album }

    private fun buildPlaylistWithArtist(item: MediaMetadataCompat): List<MediaMetadataCompat> =
        musicSource.filter { it.artist.containsCaseInsensitive(item.artist) }
}


const val EXTRA_FROM = "from"
const val EXTRA_FROM_ALL = "all"
const val EXTRA_FROM_PLAYLISTS = "playlists"
const val EXTRA_FROM_ALBUMS = "albums"
const val EXTRA_FROM_ARTISTS = "artists"


const val EXTRA_SHUFFLE = "shuffle"
const val EXTRA_POSITION = "position"

private const val TAG = "PlaybackPreparer"