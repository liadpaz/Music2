package com.liadpaz.music.service.utils

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.liadpaz.music.data.findArtists
import com.liadpaz.music.data.firstFirstArtist
import com.liadpaz.music.utils.extensions.*

class BrowseTree(musicSource: MusicSource): Iterable<MediaMetadataCompat> {

    private val _songs = mutableListOf<MediaMetadataCompat>()
    private val _playlists = mutableMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>()
    private val _albums =
        sortedMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>({ album1, album2 ->
            album1.album?.compareTo(album2.album!!, true) ?: 0
        })
    private val _artists =
        sortedMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>({ artist1, artist2 ->
            artist1.artist?.compareTo(artist2.artist!!, true) ?: 0
        })

    val songs: List<MediaMetadataCompat>
        get() = _songs
    val playlists: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = _playlists
    val albums: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = _albums
    val artists: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = _artists

    init {
        musicSource.whenReady {
            musicSource.forEach { mediaItem ->
                _songs.add(mediaItem)

                // TODO: playlists

                // adds the 'mediaItem' to the albums list
                val album = _albums.findValueByKey { it.album == mediaItem.album }
                    ?: buildAlbumRoot(mediaItem)
                album += mediaItem

                // adds the 'mediaItem' to the artists list
                mediaItem.findArtists().forEach { artist ->
                    val artistRoot =
                        _artists.findValueByKey { it.artist == artist } ?: buildArtistRoot(artist)
                    artistRoot += mediaItem
                    _artists.find { it.artist == artist }?.apply {
                        description?.extras?.let { extras ->
                            extras.putInt(EXTRA_SONGS_NUM, extras.getInt(EXTRA_SONGS_NUM) + 1)
                        }
                    }
                }
            }
        }
    }

    operator fun get(mediaId: String): List<MediaMetadataCompat>? = when {
        mediaId.startsWith(ALL_SONGS_ROOT) -> _songs
        mediaId.startsWith(PLAYLISTS_ROOT) -> TODO("not implemented yet")
        mediaId.startsWith(ALBUMS_ROOT) -> {
            if (mediaId == ALBUMS_ROOT) {
                _albums.keys.toList()
            } else {
                _albums.findValueByKey { it.album == mediaId.substring(ALBUMS_ROOT.length) }?.toList()
            }
        }
        mediaId.startsWith(ARTISTS_ROOT) -> {
            if (mediaId == ARTISTS_ROOT) {
                _artists.keys.toList()
            } else {
                _artists.findValueByKey { it.artist == mediaId.substring(ARTISTS_ROOT.length) }?.toList()
            }
        }
        else -> throw UnsupportedOperationException("Invalid id entered: $mediaId")
    }

    fun search(query: String, extras: Bundle?): List<MediaMetadataCompat> {
        return listOf()
    }

    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = "${ALBUMS_ROOT}${mediaItem.album.toString()}"
            title = mediaItem.album
            artist = mediaItem.firstFirstArtist()
            album = mediaItem.album
            albumArtUri = mediaItem.albumArtUri.toString()
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        }.build()

        _albums[albumMetadata] = mutableListOf()

        return _albums[albumMetadata]!!
    }

    private fun buildArtistRoot(artist: String): MutableList<MediaMetadataCompat> {
        val artistMetadata = MediaMetadataCompat.Builder().apply {
            id = "${ARTISTS_ROOT}${artist}"
            title = artist
            this.artist = artist
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        }.build()

        _artists[artistMetadata] = mutableListOf()

        return _artists[artistMetadata]!!
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = songs.iterator()
}

inline fun Map<MediaMetadataCompat, MutableList<MediaMetadataCompat>>.findValueByKey(crossinline predicate: (MediaMetadataCompat) -> Boolean): MutableList<MediaMetadataCompat>? {
    for ((key, value) in this) {
        if (predicate(key)) {
            return value
        }
    }
    return null
}

inline fun Map<MediaMetadataCompat, MutableList<MediaMetadataCompat>>.find(crossinline predicate: (MediaMetadataCompat) -> Boolean): MediaMetadataCompat? {
    for ((key, _) in this) {
        if (predicate(key)) {
            return key
        }
    }
    return null
}

const val ROOT = "/"
const val ALL_SONGS_ROOT = "_ALL_SONGS_"
const val PLAYLISTS_ROOT = "_PLAYLISTS_"
const val ALBUMS_ROOT = "_ALBUMS_"
const val ARTISTS_ROOT = "_ARTISTS_"

const val EXTRA_SONGS_NUM = "songs_num"

private const val TAG = "BrowseTree"