package com.liadpaz.music.repository

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.liadpaz.music.R
import com.liadpaz.music.utils.contentprovider.SongProvider

class Repository private constructor(context: Context) {

    val recentlyAddedName = context.getString(R.string.playlist_recently_added)

    private val _granted = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val granted: LiveData<Boolean> = _granted

    private val _folder = MutableLiveData<String>().apply {
        postValue(SongProvider.FOLDER_DEFAULT)
    }
    val folder: LiveData<String> = _folder

    private val _queue = MutableLiveData<List<MediaMetadataCompat>>()
    val queue: LiveData<List<MediaMetadataCompat>> = _queue

    private val _mediaPosition = MutableLiveData<Int>()
    val mediaPosition: LiveData<Int> = _mediaPosition

    private val _queuePosition = MutableLiveData<Int>()
    val queuePosition: LiveData<Int> = _queuePosition

    private val _playlists = MutableLiveData<MutableList<Pair<String, MutableList<Int>>>>()
    val playlists: LiveData<MutableList<Pair<String, MutableList<Int>>>> = _playlists

    fun setQueuePosition(position: Int): Unit = _queuePosition.postValue(position)

    fun setQueue(queue: List<MediaMetadataCompat>) = _queue.postValue(queue)

    /**
     * This function creates a new playlist.
     *
     * @param name The playlist's name to modify.
     * @param songs The songs ids to initialize the playlist with.
     */
    fun createNewPlaylist(name: String, songs: IntArray) {
        val playlists = _playlists.value ?: mutableListOf()
        if (playlists.find { pair -> pair.first == name } == null) {
            _playlists.postValue(playlists.apply { add(0, Pair(name, songs.toMutableList())) })
        }
    }

    /**
     * This function adds songs to a playlist
     *
     * @param name The name of the playlist.
     * @param songs The songs ids to add.
     */
    fun addSongsToPlaylist(name: String, songs: IntArray) {
        val playlists = _playlists.value!!
        val playlist = playlists.removeAt(playlists.indexOfFirst { it.first == name }).apply {
            second.addAll(songs.toList())
        }
        playlists.add(0, playlist)
        _playlists.postValue(playlists)
    }

    /**
     * This function moves song position inside a playlist.
     *
     * @param name The name of the playlist.
     * @param fromPosition The original index.
     * @param toPosition The desired index.
     */
    fun moveSongInPlaylist(name: String, fromPosition: Int, toPosition: Int) {
        val playlists = _playlists.value!!
        val playlist = playlists.removeAt(playlists.indexOfFirst { it.first == name }).apply {
            second.add(toPosition, second.removeAt(fromPosition))
        }
        playlists.add(0, playlist)
        _playlists.postValue(playlists)
    }

    /**
     * This function deletes a song inside a playlist.
     *
     * @param name The name of the playlist.
     * @param position The index of the song to delete.
     */
    fun deletePlaylistSong(name: String, position: Int) {
        val playlists = _playlists.value!!
        val playlist = playlists.removeAt(playlists.indexOfFirst { it.first == name }).apply {
            second.removeAt(position)
        }
        playlists.add(0, playlist)
        _playlists.postValue(playlists)
    }

    /**
     * This function deletes a playlist.
     *
     * @param name The name of the playlist to delete.
     */
    fun deletePlaylist(name: String) {
        val playlists = _playlists.value!!
        playlists.removeIf { (mName, _) -> mName == name }
        _playlists.postValue(playlists)
    }

    /**
     * This function changes a playlist name.
     *
     * @param oldName The original playlist's name.
     * @param newName The new playlist's name.
     */
    fun changePlaylistName(oldName: String, newName: String) {
        val playlists = _playlists.value!!
        val playlist = Pair(newName, playlists.removeAt(playlists.indexOfFirst { (mName, _) -> mName == oldName }).second)
        playlists.add(0, playlist)
        _playlists.postValue(playlists)
    }

    fun setPlaylists(playlists: MutableList<Pair<String, MutableList<Int>>>) = _playlists.postValue(playlists)

    fun setPermissionGranted(granted: Boolean) = _granted.postValue(granted)

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: Repository(context).also { instance = it }
        }
    }
}

private const val TAG = "Repository"