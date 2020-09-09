package com.liadpaz.music.service.utils

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.liadpaz.music.data.Song
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.utils.contentprovider.ORDER_LAST_ADDED
import com.liadpaz.music.utils.contentprovider.SongProvider
import com.liadpaz.music.utils.extensions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface MusicSource {

    fun search(query: String, extras: Bundle?): List<MediaMetadataCompat>
}

class FileMusicSource(context: Context, private val repository: Repository) : LiveData<FileMusicSource.SourceChange>(), MusicSource, Iterable<MediaMetadataCompat> {

    val recentlyAddedName = repository.recentlyAddedName

    private val dataSharedPrefs by lazy {
        DataSharedPrefs.getInstance(context)
    }

    private val allSongProvider = SongProvider(context, folder = repository.folder.value!!)
    private val recentlyAddedProvider = SongProvider(context, folder = repository.folder.value!!, sortOrder = ORDER_LAST_ADDED)
    private val playlistsProvider = SongProvider(context, folder = "")

    private var songs: List<MediaMetadataCompat> = emptyList()
    private var recentlyAdded: List<MediaMetadataCompat> = emptyList()
    private var allSongs: List<Song> = listOf()
    private var playlistsIds: MutableList<Pair<String, MutableList<Int>>> = mutableListOf()

    private val allSongsObserver = Observer { songs: ArrayList<Song>? ->
        this.songs = songs?.map { item -> MediaMetadataCompat.Builder().from(item).build() } ?: emptyList()
        postValue(SourceChange(this.songs, recentlyAdded, createPlaylists(playlistsIds)))
    }

    private val recentlyAddedObserver = Observer { songs: ArrayList<Song>? ->
        recentlyAdded = songs?.map { item -> MediaMetadataCompat.Builder().from(item).build() } ?: emptyList()
        postValue(SourceChange(this.songs, recentlyAdded, createPlaylists(playlistsIds)))
    }

    private val playlistsObserver = Observer { songs: ArrayList<Song>? ->
        allSongs = songs ?: mutableListOf()
        postValue(SourceChange(this.songs, recentlyAdded, createPlaylists(playlistsIds)))
    }

    private val repositoryObserver = Observer { playlists: MutableList<Pair<String, MutableList<Int>>>? ->
        playlistsIds = playlists ?: mutableListOf()
        CoroutineScope(Dispatchers.IO).launch {
            dataSharedPrefs.setPlaylists(playlists)
        }
        postValue(SourceChange(this.songs, recentlyAdded, createPlaylists(playlistsIds)))
    }

    private fun createPlaylists(playlists: MutableList<Pair<String, MutableList<Int>>>?): List<Pair<String, List<MediaMetadataCompat>>>? =
        playlists?.map { (name, songsIds) ->
            name to songsIds.map { id -> MediaMetadataCompat.Builder().from(allSongs.find { it.mediaId.toInt() == id }!!).build() }
        }

    override fun onActive() {
        allSongProvider.observeForever(allSongsObserver)
        recentlyAddedProvider.observeForever(recentlyAddedObserver)
        playlistsProvider.observeForever(playlistsObserver)
        repository.playlists.observeForever(repositoryObserver)
        playlistsIds = dataSharedPrefs.getPlaylists()
        repository.setPlaylists(playlistsIds)
    }

    override fun onInactive() {
        allSongProvider.removeObserver(allSongsObserver)
        recentlyAddedProvider.removeObserver(recentlyAddedObserver)
        playlistsProvider.removeObserver(playlistsObserver)
        repository.playlists.removeObserver(repositoryObserver)
    }

    /**
     * Handles searching a [MusicSource] from a focused voice search, often coming
     * from the Google Assistant.
     */
    override fun search(query: String, extras: Bundle?): List<MediaMetadataCompat> {
        // First attempt to search with the "focus" that's provided in the extras.
        val focusSearchResult = when (extras?.get(MediaStore.EXTRA_MEDIA_FOCUS)) {
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                // For an Artist focused search, only the artist is set.
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                Log.d(TAG, "Focused artist search: '$artist'")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist)
                }
            }
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                // For an Album focused search, album and artist are set.
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                Log.d(TAG, "Focused album search: album='$album' artist='$artist")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist) && song.album == album
                }
            }
            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                // For a Song (aka Media) focused search, title, album, and artist are set.
                val title = extras[MediaStore.EXTRA_MEDIA_TITLE]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                Log.d(TAG, "Focused media search: title='$title' album='$album' artist='$artist")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist) && song.album == album
                            && song.title == title
                }
            }
            else -> {
                // There isn't a focus, so no results yet.
                emptyList()
            }
        }

        // If there weren't any results from the focused search (or if there wasn't a focus
        // to begin with), try to find any matches given the 'query' provided, searching against
        // a few of the fields.
        // In this sample, we're just checking a few fields with the provided query, but in a
        // more complex app, more logic could be used to find fuzzy matches, etc...
        if (focusSearchResult.isEmpty()) {
            return if (query.isNotBlank()) {
                Log.d(TAG, "Unfocused search for '$query'")
                filter { song ->
                    song.title.containsCaseInsensitive(query)
                            || song.artist.containsCaseInsensitive(query)
                }
            } else {
                // If the user asked to "play music", or something similar, the query will also
                // be blank. Given the small catalog of songs in the sample, just return them
                // all, shuffled, as something to play.
                Log.d(TAG, "Unfocused search without keyword")
                return shuffled()
            }
        } else {
            return focusSearchResult
        }
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = songs.iterator()

    data class SourceChange(val allSongs: List<MediaMetadataCompat>? = null, val recentlyAdded: List<MediaMetadataCompat>? = null, val playlists: List<Pair<String, List<MediaMetadataCompat>>>? = null)
}

fun MediaMetadataCompat.Builder.from(song: Song): MediaMetadataCompat.Builder {
    id = song.mediaId.toString()
    title = song.title
    artist = song.artist
    album = song.album
    if (song.duration != -1) {
        duration = song.duration.toLong()
    }
    mediaUri = song.mediaUri.toString()
    albumArtUri = song.artUri.toString()
    flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

    displayTitle = song.title
    displaySubtitle = song.artist
    displayDescription = song.album
    displayIconUri = song.artUri.toString()

    downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED

    return this
}

private const val TAG = "MusicSource"
