package com.liadpaz.music.service

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.extractor.Extractor
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.flac.FlacExtractor
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor
import com.google.android.exoplayer2.extractor.wav.WavExtractor
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.liadpaz.music.service.utils.BrowseTree
import com.liadpaz.music.service.utils.PLAYLISTS_ROOT
import com.liadpaz.music.service.utils.findValueByKey
import com.liadpaz.music.utils.extensions.*

class PlaybackPreparer(private val browseTree: BrowseTree, private val exoPlayer: SimpleExoPlayer, private val dataSourceFactory: DataSource.Factory) : MediaSessionConnector.PlaybackPreparer {

    private var mediaSource = ConcatenatingMediaSource()
    private val extractorsFactory = MediaExtractorsFactory()

    override fun getSupportedPrepareActions(): Long =
        PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

    override fun onPrepare(playWhenReady: Boolean) = Unit

    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        val shuffle = extras?.getBoolean(EXTRA_SHUFFLE) ?: false

        val metadataList = when (extras?.getString(EXTRA_FROM)) {
            EXTRA_FROM_PLAYLISTS -> buildPlaylistWithPlaylist((extras[EXTRA_PLAYLIST] as String).substring(PLAYLISTS_ROOT.length)).apply { if (shuffle) shuffle() }
            EXTRA_FROM_ALBUMS -> buildPlaylistWithAlbum(extras[EXTRA_ALBUM] as String).apply { if (shuffle) shuffle() }
            EXTRA_FROM_ARTISTS -> buildPlaylistWithArtist(extras[EXTRA_ARTIST] as String).apply { if (shuffle) shuffle() }
            else -> browseTree.toMutableList().apply { if (shuffle) shuffle() }
        }

        mediaSource = ConcatenatingMediaSource(*metadataList.toMediaSources(dataSourceFactory, extractorsFactory).toTypedArray())

        val initialWindowIndex = extras!!.getInt(EXTRA_POSITION, 0)

        exoPlayer.prepare(mediaSource)
        exoPlayer.seekTo(initialWindowIndex, 0)
    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
        val metadataList = browseTree.search(query, extras ?: Bundle.EMPTY)
        if (metadataList.isNotEmpty()) {
            mediaSource = ConcatenatingMediaSource(*metadataList.toMediaSources(dataSourceFactory, extractorsFactory).toTypedArray())
            exoPlayer.prepare(mediaSource)
        }
    }

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

    override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean =
        false

    private fun buildPlaylistWithPlaylist(playlistName: String): MutableList<MediaMetadataCompat> = when (playlistName) {
        PLAYLIST_RECENTLY_ADDED -> browseTree.recentlyAdded.toMutableList()
        else -> browseTree.playlists.findValueByKey { it.description.title == playlistName }!!.toMutableList()
    }

    private fun buildPlaylistWithAlbum(album: String): MutableList<MediaMetadataCompat> =
        browseTree.albums.findValueByKey { it.album == album }!!.toMutableList()

    private fun buildPlaylistWithArtist(artist: String): MutableList<MediaMetadataCompat> =
        browseTree.artists.findValueByKey { it.artist.containsCaseInsensitive(artist) }!!.toMutableList()

    fun addQueueItem(item: MediaDescriptionCompat, position: Int) =
        mediaSource.addMediaSource(position, item.toMediaSource(dataSourceFactory, extractorsFactory))


    fun addQueueItem(item: MediaDescriptionCompat) =
        mediaSource.addMediaSource(item.toMediaSource(dataSourceFactory, extractorsFactory))

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = mediaSource.moveMediaSource(fromIndex, toIndex)

    fun removeQueueItem(position: Int): MediaSource = mediaSource.removeMediaSource(position)

    private class MediaExtractorsFactory : ExtractorsFactory {
        override fun createExtractors(): Array<Extractor> =
            arrayOf(Mp3Extractor(), Mp4Extractor(), FlacExtractor(), WavExtractor())
    }
}

const val PLAYLIST_RECENTLY_ADDED = "playlist_recently_added"

const val EXTRA_FROM = "from"
const val EXTRA_FROM_ALL = "all"
const val EXTRA_FROM_PLAYLISTS = "playlists"
const val EXTRA_FROM_ALBUMS = "albums"
const val EXTRA_FROM_ARTISTS = "artists"

const val EXTRA_PLAYLIST = "playlist"
const val EXTRA_ARTIST = "artist"
const val EXTRA_ALBUM = "album"
const val EXTRA_SHUFFLE = "shuffle"
const val EXTRA_POSITION = "position"

private const val TAG = "PlaybackPreparer"