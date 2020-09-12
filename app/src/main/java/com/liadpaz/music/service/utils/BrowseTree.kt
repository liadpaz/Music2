package com.liadpaz.music.service.utils

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.Observer
import com.liadpaz.music.service.PLAYLIST_RECENTLY_ADDED
import com.liadpaz.music.utils.contentprovider.findArtists
import com.liadpaz.music.utils.contentprovider.findFirstArtist
import com.liadpaz.music.utils.extensions.*

class BrowseTree(private val musicSource: FileMusicSource, private val onUpdate: (String) -> Unit) : Iterable<MediaMetadataCompat> {

    private val musicSourceObserver = Observer { sourceChange: FileMusicSource.SourceChange ->
        sortSongs(sourceChange)
    }

    private val songs = mutableListOf<MediaMetadataCompat>()
    private val _playlists = mutableMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>()
    private val _recentlyAdded = mutableListOf<MediaMetadataCompat>()
    private val _albums = sortedMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>({ album1, album2 ->
        album1.album?.compareTo(album2.album!!, true) ?: 0
    })
    private val _artists = sortedMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>({ artist1, artist2 ->
        artist1.artist?.compareTo(artist2.artist!!, true) ?: 0
    })

    val playlists: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = _playlists
    val recentlyAdded: List<MediaMetadataCompat>
        get() = _recentlyAdded
    val albums: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = _albums
    val artists: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = _artists

    private val recentlyAddedMetadata: MediaMetadataCompat = buildRecentlyAddedMetadata()

    init {
        musicSource.observeForever(musicSourceObserver)
    }

    private fun sortSongs(sourceChange: FileMusicSource.SourceChange) {
        sourceChange.also { change ->
            change.allSongs?.let { list ->
                songs.clear()
                _albums.clear()
                _artists.clear()
                list.forEach { mediaItem ->
                    songs.add(mediaItem)

                    val album = _albums.findValueByKey { it.album == mediaItem.displayDescription } ?: buildAlbumRoot(mediaItem)
                    album += mediaItem

                    mediaItem.findArtists().forEach { artist ->
                        val artistRoot = _artists.findValueByKey { it.artist == artist } ?: buildArtistRoot(artist)
                        artistRoot += mediaItem
                        _artists.find { it.artist == artist }?.apply {
                            description?.extras?.let { extras ->
                                extras.putInt(EXTRA_SONGS_NUM, extras.getInt(EXTRA_SONGS_NUM) + 1)
                            }
                        }
                    }
                }
                onUpdate(ALL_SONGS_ROOT)
                onUpdate(ARTISTS_ROOT)
                onUpdate(ALBUMS_ROOT)
            }
            change.recentlyAdded?.let { list ->
                _recentlyAdded.clear()
                _recentlyAdded.addAll(list)
                recentlyAddedMetadata.description?.extras?.let { extras ->
                    extras.putInt(EXTRA_SONGS_NUM, list.size)
                    extras.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, list.getOrNull(0)?.displayIconUri.toString())
                }
                onUpdate(PLAYLISTS_ROOT)
                onUpdate("${PLAYLISTS_ROOT}${PLAYLIST_RECENTLY_ADDED}")
            }
            change.playlists?.let { list: List<Pair<String, List<MediaMetadataCompat>>> ->
                _playlists.clear()
                list.forEach { (playlistName, playlistSongs) ->
                    val playlistRoot = _playlists.findValueByKey { it.title == playlistName } ?: buildPlaylistRoot(playlistName)
                    playlistRoot += playlistSongs
                    _playlists.find { it.title == playlistName }?.apply {
                        description?.extras?.let { extras ->
                            extras.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, playlistSongs.getOrNull(0)?.displayIconUri.toString())
                            extras.putInt(EXTRA_SONGS_NUM, playlistSongs.size)
                        }
                    }
                }
                onUpdate(PLAYLISTS_ROOT)
            }
        }
    }

    fun release() = musicSource.removeObserver(musicSourceObserver)

    operator fun get(mediaId: String): List<MediaMetadataCompat>? = when {
        mediaId.startsWith(ALL_SONGS_ROOT) -> songs
        mediaId.startsWith(PLAYLISTS_ROOT) -> {
            if (mediaId == PLAYLISTS_ROOT) {
                _playlists.keys.toMutableList().apply { add(0, recentlyAddedMetadata) }
            } else {
                val playlistId = mediaId.substring(PLAYLISTS_ROOT.length)
                if (playlistId == PLAYLIST_RECENTLY_ADDED) {
                    _recentlyAdded
                } else {
                    _playlists.findValueByKey { it.title == playlistId }?.toList()
                }
            }
        }
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
        else -> throw IllegalArgumentException("Invalid id entered: $mediaId")
    }

    fun search(query: String, extras: Bundle?): List<MediaMetadataCompat> = musicSource.search(query, extras)

    private fun buildRecentlyAddedMetadata(): MediaMetadataCompat = MediaMetadataCompat.Builder().apply {
        id = "${PLAYLISTS_ROOT}${PLAYLIST_RECENTLY_ADDED}"
        title = musicSource.recentlyAddedName
        flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
    }.build()

    private fun buildPlaylistRoot(name: String): MutableList<MediaMetadataCompat> {
        val playlistMetadata = MediaMetadataCompat.Builder().apply {
            id = "${PLAYLISTS_ROOT}${name}"
            title = name
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        }.build()

        _playlists[playlistMetadata] = mutableListOf()

        return _playlists[playlistMetadata]!!
    }

    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = "${ALBUMS_ROOT}${mediaItem.displayDescription.toString()}"
            title = mediaItem.displayDescription
            artist = mediaItem.findFirstArtist()
            album = mediaItem.displayDescription
            albumArtUri = mediaItem.displayIconUri.toString()
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

@JvmName("findValueByKeyImmutable")
inline fun Map<MediaMetadataCompat, List<MediaMetadataCompat>>.findValueByKey(crossinline predicate: (MediaMetadataCompat) -> Boolean): List<MediaMetadataCompat>? {
    for ((key, value) in this) {
        if (predicate(key)) {
            return value
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