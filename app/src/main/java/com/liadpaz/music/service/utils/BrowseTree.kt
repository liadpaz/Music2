package com.liadpaz.music.service.utils

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.liadpaz.music.data.findArtists
import com.liadpaz.music.data.firstArtist
import com.liadpaz.music.utils.extensions.*
import java.util.*

class BrowseTree(context: Context, musicSource: MusicSource) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

    init {
        val rootList = mediaIdToChildren[ROOT] ?: mutableListOf()

        val allSongsMetadata = MediaMetadataCompat.Builder().apply {
            id = ALL_SONGS_ROOT
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()
        val playlistsMetadata = MediaMetadataCompat.Builder().apply {
            id = PLAYLISTS_ROOT
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()
        val albumsMetadata = MediaMetadataCompat.Builder().apply {
            id = ALBUMS_ROOT
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()
        val artistsMetadata = MediaMetadataCompat.Builder().apply {
            id = ARTISTS_ROOT
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        rootList.addAll(arrayOf(allSongsMetadata, playlistsMetadata, albumsMetadata, artistsMetadata))
        mediaIdToChildren[ROOT] = rootList

        musicSource.forEach { mediaItem ->
            val allSongs = mediaIdToChildren[ALL_SONGS_ROOT] ?: mutableListOf()
            allSongs += mediaItem
            mediaIdToChildren[ALL_SONGS_ROOT] = allSongs

            // TODO: playlists

            // adds the 'mediaItem' to the albums list
            val albums =
                mediaIdToChildren["$ALBUMS_ROOT/${mediaItem.album}"] ?: buildAlbumRoot(mediaItem)
            albums += mediaItem
            mediaIdToChildren["$ALBUMS_ROOT/${mediaItem.album}"] = albums

            // adds the 'mediaItem' to the artists list
            mediaItem.findArtists().forEach { artist ->
                val artists = mediaIdToChildren["$ARTISTS_ROOT/$artist"] ?: buildArtistRoot(artist)
                artists += mediaItem
                mediaIdToChildren[ARTISTS_ROOT]!!.find { rootArtist -> rootArtist.artist == artist }?.apply {
                    description?.extras?.let { extras ->
                        extras.putInt(EXTRA_SONGS_NUM, extras.getInt(EXTRA_SONGS_NUM) + 1)
                    }
                }
                mediaIdToChildren["$ARTISTS_ROOT/$artist"] = artists
            }
        }

        mediaIdToChildren[ALBUMS_ROOT]?.sortBy { album -> album.album?.toLowerCase(Locale.getDefault()) }
        mediaIdToChildren[ARTISTS_ROOT]?.sortBy { artist -> artist.artist?.toLowerCase(Locale.getDefault()) }
    }

    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = "$ALBUMS_ROOT/${mediaItem.album}"
            title = mediaItem.album
            artist = mediaItem.firstArtist()
            album = mediaItem.album
            albumArtUri = mediaItem.albumArtUri.toString()
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        }.build()

        val rootList = mediaIdToChildren[ALBUMS_ROOT] ?: mutableListOf()
        rootList += albumMetadata
        mediaIdToChildren[ALBUMS_ROOT] = rootList

        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.id!!] = it
        }
    }

    private fun buildArtistRoot(artist: String): MutableList<MediaMetadataCompat> {
        val artistMetadata = MediaMetadataCompat.Builder().apply {
            id = artist
            title = artist
            this.artist = artist
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        }.build()

        val rootList = mediaIdToChildren[ARTISTS_ROOT] ?: mutableListOf()
        rootList += artistMetadata
        mediaIdToChildren[ARTISTS_ROOT] = rootList

        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[artistMetadata.id!!] = it
        }
    }
}


const val ROOT = "/"
const val ALL_SONGS_ROOT = "_ALL_SONGS_"
const val PLAYLISTS_ROOT = "_PLAYLISTS_"
const val ALBUMS_ROOT = "_ALBUMS_"
const val ARTISTS_ROOT = "_ARTISTS_"

const val EXTRA_SONGS_NUM = "songs_num"