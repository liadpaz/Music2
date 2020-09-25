package com.liadpaz.music.service.utils

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
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

class FileMusicSource(context: Context, private val repository: Repository, private val onLoadQueue: (List<MediaMetadataCompat>, Int, Long) -> Unit) : LiveData<FileMusicSource.SourceChange>(), MusicSource, Iterable<MediaMetadataCompat> {

    val recentlyAddedName = repository.recentlyAddedName

    private val dataSharedPrefs by lazy { DataSharedPrefs.getInstance(context) }

    private val allSongProvider = SongProvider(context, folderLiveData = repository.folder)
    private val recentlyAddedProvider = SongProvider(context, folderLiveData = repository.folder, sortOrder = ORDER_LAST_ADDED)
    private val playlistsProvider = SongProvider(context)

    private var songs: List<MediaMetadataCompat>? = null
    private var recentlyAdded: List<MediaMetadataCompat>? = null
    private var allSongs: List<MediaMetadataCompat>? = null
    private var playlistsIds: MutableList<Pair<String, MutableList<Int>>>? = null

    var queue: List<MediaMetadataCompat>? = null
        private set

    private val allSongsObserver = Observer { songs: ArrayList<MediaMetadataCompat>? ->
        this.songs = songs
        postValue(SourceChange(allSongs = this.songs, recentlyAdded = recentlyAdded, playlists = createPlaylists(playlistsIds)))
    }
    private val recentlyAddedObserver = Observer { songs: ArrayList<MediaMetadataCompat>? ->
        recentlyAdded = songs
        postValue(SourceChange(allSongs = this.songs, recentlyAdded = recentlyAdded, playlists = createPlaylists(playlistsIds)))
    }
    private val playlistsObserver = Observer { songs: ArrayList<MediaMetadataCompat> ->
        allSongs = songs
        createQueue(dataSharedPrefs.queue, dataSharedPrefs.queuePosition).also {
            queue = it.first
            dataSharedPrefs.queuePosition = it.second
            onLoadQueue(it.first, it.second, dataSharedPrefs.mediaPosition)
        }
        postValue(SourceChange(allSongs = this.songs, recentlyAdded = recentlyAdded, playlists = createPlaylists(playlistsIds)))
    }
    private val repositoryPlaylistsObserver = Observer { playlists: MutableList<Pair<String, MutableList<Int>>>? ->
        playlistsIds = playlists
        CoroutineScope(Dispatchers.IO).launch {
            dataSharedPrefs.setPlaylists(playlists)
        }
        postValue(SourceChange(this.songs, recentlyAdded, createPlaylists(playlistsIds)))
    }
    private val repositoryQueueObserver = Observer { queue: List<MediaMetadataCompat> ->
        dataSharedPrefs.queue = queue.map { it.description.mediaId?.toInt()!! }
    }
    private val repositoryQueuePositionObserver = Observer { queuePosition: Int ->
        dataSharedPrefs.queuePosition = queuePosition
    }

    fun setMediaPosition(position: Long) {
        dataSharedPrefs.mediaPosition = position
    }

    private fun createPlaylists(playlists: MutableList<Pair<String, MutableList<Int>>>?): List<Pair<String, List<MediaMetadataCompat>>>? =
        allSongs?.let { all ->
            playlists?.map { (name, songsIds) ->
                val playlist = mutableListOf<MediaMetadataCompat>()
                songsIds.forEach { id ->
                    all.find { it.id == id.toString() }?.let { playlist.add(it) }
                }
                name to playlist
            }
        }

    private fun createQueue(list: List<Int>, position: Int): Pair<List<MediaMetadataCompat>, Int> {
        var newPosition = position
        val queue = mutableListOf<MediaMetadataCompat>()
        list.forEachIndexed { index, queueItem ->
            allSongs?.find { it.id == queueItem.toString() }?.let { queue.add(it) } ?: let {
                if (index <= newPosition) {
                    newPosition--
                }
            }
        }
        if (queue.isEmpty()) {
            newPosition = -1
        }
        return queue to newPosition
    }

    override fun onActive() {
        allSongProvider.observeForever(allSongsObserver)
        recentlyAddedProvider.observeForever(recentlyAddedObserver)
        playlistsProvider.observeForever(playlistsObserver)
        repository.playlists.observeForever(repositoryPlaylistsObserver)
        repository.queue.observeForever(repositoryQueueObserver)
        repository.queuePosition.observeForever(repositoryQueuePositionObserver)
        playlistsIds = dataSharedPrefs.getPlaylists()
        playlistsIds?.let { repository.setPlaylists(it) }
    }

    override fun onInactive() {
        allSongProvider.removeObserver(allSongsObserver)
        recentlyAddedProvider.removeObserver(recentlyAddedObserver)
        playlistsProvider.removeObserver(playlistsObserver)
        repository.playlists.removeObserver(repositoryPlaylistsObserver)
        repository.queue.removeObserver(repositoryQueueObserver)
        repository.queuePosition.removeObserver(repositoryQueuePositionObserver)
    }

    override fun search(query: String, extras: Bundle?): List<MediaMetadataCompat> {
        val focusSearchResult = when (extras?.get(MediaStore.EXTRA_MEDIA_FOCUS)) {
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                Log.d(TAG, "Focused artist search: '$artist'")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist)
                }
            }
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                Log.d(TAG, "Focused album search: album='$album' artist='$artist")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist) && song.album == album
                }
            }
            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
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
                emptyList()
            }
        }

        if (focusSearchResult.isEmpty()) {
            return if (query.isNotBlank()) {
                Log.d(TAG, "Unfocused search for '$query'")
                filter { song ->
                    song.title.containsCaseInsensitive(query)
                            || song.artist.containsCaseInsensitive(query)
                }
            } else {
                Log.d(TAG, "Unfocused search without keyword")
                return shuffled()
            }
        } else {
            return focusSearchResult
        }
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = songs?.iterator() ?: emptyList<MediaMetadataCompat>().iterator()

    data class SourceChange(val allSongs: List<MediaMetadataCompat>? = null, val recentlyAdded: List<MediaMetadataCompat>? = null, val playlists: List<Pair<String, List<MediaMetadataCompat>>>? = null)
}


private const val TAG = "MusicSource"