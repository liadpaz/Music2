package com.liadpaz.music.repository

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.liadpaz.music.R
import com.liadpaz.music.utils.contentprovider.SongProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    private val _queue = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
    val queue: LiveData<List<MediaSessionCompat.QueueItem>> = _queue

    private val _queuePosition = MutableLiveData<Int>()
    val queuePosition: LiveData<Int> = _queuePosition

    private val _playlists = MutableLiveData<MutableList<Pair<String, MutableList<Int>>>>()
    val playlists: LiveData<MutableList<Pair<String, MutableList<Int>>>> = _playlists

    fun setQueuePosition(position: Int): Unit = position.let {
        if (it != _queuePosition.value) {
            _queuePosition.postValue(position)
        }
    }

    fun setQueue(queue: List<MediaSessionCompat.QueueItem>) =
        CoroutineScope(Dispatchers.Default).launch {
            queue.let {
                if (!it.contentEquals(_queue.value)) {
                    _queue.postValue(queue)
                }
            }
        }

    /**
     * This function creates a new playlist.
     */
    fun createNewPlaylist(name: String, songs: IntArray) {
        val playlists = _playlists.value ?: mutableListOf()
        if (playlists.find { pair -> pair.first == name } == null) {
            _playlists.postValue(playlists.apply { add(0, Pair(name, songs.toMutableList())) })
        }
    }

    fun addSongsToPlaylist(name: String, songs: IntArray) {
        val playlists = _playlists.value!!
        val playlist = playlists.removeAt(playlists.indexOfFirst { it.first == name }).apply {
            second.addAll(songs.toList())
        }
        playlists.add(0, playlist)
        _playlists.postValue(playlists)
    }

    fun deletePlaylist(name: String) {
        val playlists = _playlists.value!!
        playlists.removeIf { (mName, _) -> mName == name }
        _playlists.postValue(playlists)
    }

    fun setPlaylists(playlists: MutableList<Pair<String, MutableList<Int>>>) {
        _playlists.postValue(playlists)
    }

    fun setPermissionGranted(granted: Boolean) = _granted.postValue(granted)

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: Repository(context).also { instance = it }
        }
    }
}

fun List<MediaSessionCompat.QueueItem>?.contentEquals(other: List<MediaSessionCompat.QueueItem>?): Boolean {
    if (this == null || other == null) {
        return this == other
    }
    if (this.size != other.size) return false

    forEachIndexed { index, any ->
        if (other[index].description != any.description || other[index].queueId != any.queueId) {
            return@contentEquals false
        }
    }
    return true
}

private const val TAG = "Repository"