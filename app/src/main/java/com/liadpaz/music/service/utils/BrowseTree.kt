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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.HashMap

class BrowseTree(private val musicSource: FileMusicSource, private val onUpdate: (String) -> Unit) : Iterable<MediaMetadataCompat> {

    private val musicSourceObserver = Observer { sourceChange: FileMusicSource.SourceChange ->
        sortSongs(sourceChange)
    }

    private var songs = listOf<MediaMetadataCompat>()
    private var _playlists: Map<MediaMetadataCompat, MutableList<MediaMetadataCompat>> = mapOf()
    private var _recentlyAdded = listOf<MediaMetadataCompat>()
    private var _albums: Map<MediaMetadataCompat, List<MediaMetadataCompat>> = mapOf()
    private var _artists: Map<MediaMetadataCompat, List<MediaMetadataCompat>> = mapOf()

    val playlists: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = HashMap(_playlists)
    val recentlyAdded: List<MediaMetadataCompat>
        get() = _recentlyAdded
    val albums: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = _albums
    val artists: Map<MediaMetadataCompat, List<MediaMetadataCompat>>
        get() = _artists
    val queue: List<MediaMetadataCompat>?
        get() = musicSource.queue

    private val recentlyAddedMetadata: MediaMetadataCompat = buildRecentlyAddedMetadata()

    init {
        musicSource.observeForever(musicSourceObserver)
    }

    private fun sortSongs(sourceChange: FileMusicSource.SourceChange) {
        CoroutineScope(Dispatchers.Main).launch {
            sourceChange.also { change ->
                change.allSongs?.let { list ->
                    if (!(songs equals list)) {
                        buildSongs(list).also {
                            songs = it.first
                            _albums = it.second
                            _artists = it.third
                        }
                        onUpdate(ALL_SONGS_ROOT)
                        onUpdate(ARTISTS_ROOT)
                        onUpdate(ALBUMS_ROOT)
                    }
                }
                change.recentlyAdded?.let { list ->
                    if (!(_recentlyAdded equals list)) {
                        _recentlyAdded = list
                        recentlyAddedMetadata.description?.extras?.let { extras ->
                            extras.putInt(EXTRA_SONGS_NUM, list.size)
                            extras.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, list.getOrNull(0)?.displayIconUri.toString())
                        }
                        onUpdate(PLAYLISTS_ROOT)
                        onUpdate("${PLAYLISTS_ROOT}${PLAYLIST_RECENTLY_ADDED}")
                    }
                }
                change.playlists?.let { list ->
                    _playlists = buildPlaylists(list)
                    onUpdate(PLAYLISTS_ROOT)
                }
            }
        }
    }

    fun release() = musicSource.removeObserver(musicSourceObserver)

    operator fun get(mediaId: String): List<MediaMetadataCompat>? = when {
        mediaId.startsWith(ALL_SONGS_ROOT) -> songs
        mediaId.startsWith(PLAYLISTS_ROOT) -> {
            if (mediaId == PLAYLISTS_ROOT) {
                playlists.keys.toMutableList().apply { add(0, recentlyAddedMetadata) }
            } else {
                val playlistId = mediaId.substring(PLAYLISTS_ROOT.length)
                if (playlistId == PLAYLIST_RECENTLY_ADDED) {
                    recentlyAdded
                } else {
                    playlists.findValueByKey { it.title == playlistId }?.toList()
                }
            }
        }
        mediaId.startsWith(ALBUMS_ROOT) -> {
            if (mediaId == ALBUMS_ROOT) {
                albums.keys.toList()
            } else {
                albums.findValueByKey { it.album == mediaId.substring(ALBUMS_ROOT.length) }?.toList()
            }
        }
        mediaId.startsWith(ARTISTS_ROOT) -> {
            if (mediaId == ARTISTS_ROOT) {
                artists.keys.toList()
            } else {
                artists.findValueByKey { it.artist == mediaId.substring(ARTISTS_ROOT.length) }?.toList()
            }
        }
        else -> throw IllegalArgumentException("Invalid id entered: $mediaId")
    }

    fun search(query: String, extras: Bundle?): List<MediaMetadataCompat> = musicSource.search(query, extras)

    private suspend fun buildSongs(list: List<MediaMetadataCompat>): Triple<List<MediaMetadataCompat>, Map<MediaMetadataCompat, List<MediaMetadataCompat>>, Map<MediaMetadataCompat, List<MediaMetadataCompat>>> =
        withContext(Dispatchers.Default) {
            val songs = arrayListOf<MediaMetadataCompat>()
            val albums = sortedMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>(compareBy {
                it.album?.toLowerCase(Locale.getDefault())
            })
            val artists = sortedMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>(compareBy {
                it.artist?.toLowerCase(Locale.getDefault())
            })
            list.forEach { mediaItem ->
                songs.add(mediaItem)

                val album = albums.findValueByKey { it.album == mediaItem.displayDescription } ?: buildAlbumRoot(albums, mediaItem)
                album += mediaItem

                mediaItem.findArtists().forEach { artist ->
                    val artistRoot = artists.findValueByKey { it.artist == artist } ?: buildArtistRoot(artists, artist)
                    artistRoot += mediaItem
                    artists.find { it.artist == artist }?.apply {
                        description?.extras?.let { extras ->
                            extras.putInt(EXTRA_SONGS_NUM, extras.getInt(EXTRA_SONGS_NUM) + 1)
                        }
                    }
                }
            }
            Triple(songs, albums, artists)
        }

    private suspend fun buildPlaylists(list: List<Pair<String, List<MediaMetadataCompat>>>) = withContext(Dispatchers.Default) {
        val playlists = mutableMapOf<MediaMetadataCompat, MutableList<MediaMetadataCompat>>()
        list.forEach { (playlistName, playlistSongs) ->
            val playlistRoot = playlists.findValueByKey { it.title == playlistName } ?: buildPlaylistRoot(playlists, playlistName)
            playlistRoot += playlistSongs
            playlists.find { it.title == playlistName }?.apply {
                description?.extras?.let { extras ->
                    extras.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, playlistSongs.getOrNull(0)?.displayIconUri.toString())
                    extras.putInt(EXTRA_SONGS_NUM, playlistSongs.size)
                }
            }
        }
        playlists
    }

    private fun buildRecentlyAddedMetadata(): MediaMetadataCompat = MediaMetadataCompat.Builder().apply {
        id = "${PLAYLISTS_ROOT}${PLAYLIST_RECENTLY_ADDED}"
        title = musicSource.recentlyAddedName
        flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
    }.build()

    private fun buildPlaylistRoot(playlists: MutableMap<MediaMetadataCompat, MutableList<MediaMetadataCompat>>, name: String): MutableList<MediaMetadataCompat> {
        val playlistMetadata = MediaMetadataCompat.Builder().apply {
            id = "${PLAYLISTS_ROOT}${name}"
            title = name
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        }.build()

        playlists[playlistMetadata] = mutableListOf()

        return playlists[playlistMetadata]!!
    }

    private fun buildAlbumRoot(albums: SortedMap<MediaMetadataCompat, MutableList<MediaMetadataCompat>>, mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = "${ALBUMS_ROOT}${mediaItem.displayDescription.toString()}"
            title = mediaItem.displayDescription
            artist = mediaItem.findFirstArtist()
            album = mediaItem.displayDescription
            albumArtUri = mediaItem.displayIconUri.toString()
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        }.build()

        albums[albumMetadata] = mutableListOf()

        return albums[albumMetadata]!!
    }

    private fun buildArtistRoot(artists: SortedMap<MediaMetadataCompat, MutableList<MediaMetadataCompat>>, artist: String): MutableList<MediaMetadataCompat> {
        val artistMetadata = MediaMetadataCompat.Builder().apply {
            id = "${ARTISTS_ROOT}${artist}"
            title = artist
            this.artist = artist
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        }.build()

        artists[artistMetadata] = mutableListOf()

        return artists[artistMetadata]!!
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

private infix fun List<MediaMetadataCompat>?.equals(other: List<MediaMetadataCompat>?): Boolean {
    if (this == null || other == null) return this == other
    if (this.size != other.size) return false

    forEachIndexed { index, song ->
        if (!(song equals other[index])) return false
    }
    return true
}

private infix fun MediaMetadataCompat.equals(other: MediaMetadataCompat): Boolean =
    this.id == other.id && this.displayTitle == other.displayTitle && this.displaySubtitle == other.displayTitle && this.displayDescription == other.displayDescription && this.displayIconUri == other.displayIconUri

const val ROOT = "/"
const val ALL_SONGS_ROOT = "_ALL_SONGS_"
const val PLAYLISTS_ROOT = "_PLAYLISTS_"
const val ALBUMS_ROOT = "_ALBUMS_"
const val ARTISTS_ROOT = "_ARTISTS_"
const val QUEUE_ROOT = "_QUEUE_"

const val EXTRA_SONGS_NUM = "songs_num"

private const val TAG = "BrowseTree"