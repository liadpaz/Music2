package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.*

class PlaylistViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository, private val playlist: String) : ViewModel() {

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
            Log.d(TAG, "onChildrenLoaded: ${children.size}")
            _songs.postValue(children)
        }
    }

    init {
        Log.d(TAG, "init: ")
        serviceConnection.subscribe(playlist, subscriptionCallback)
    }

    private val _songs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val songs: LiveData<List<MediaBrowserCompat.MediaItem>> = _songs

    fun play(mediaItem: MediaBrowserCompat.MediaItem, position: Int) =
        serviceConnection.transportControls?.playFromMediaId(mediaItem.mediaId, bundleOf(EXTRA_FROM to EXTRA_FROM_PLAYLISTS, EXTRA_PLAYLIST to playlist, EXTRA_POSITION to position))

    fun playShuffle() =
        serviceConnection.transportControls?.playFromMediaId(playlist, bundleOf(EXTRA_FROM to EXTRA_FROM_PLAYLISTS, EXTRA_PLAYLIST to playlist, EXTRA_SHUFFLE to true))

    override fun onCleared() {
        serviceConnection.unsubscribe(playlist, subscriptionCallback)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository, private val playlist: String) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            PlaylistViewModel(serviceConnection, repository, playlist) as T
    }
}

private const val TAG = "PlaylistViewModel"