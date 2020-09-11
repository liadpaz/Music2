package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.service.utils.ALBUMS_ROOT
import com.liadpaz.music.service.utils.ALL_SONGS_ROOT
import com.liadpaz.music.service.utils.ARTISTS_ROOT
import com.liadpaz.music.service.utils.PLAYLISTS_ROOT

class MainViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModel() {

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
//            Log.d(TAG, "onChildrenLoaded: $parentId")
            when (parentId) {
                ALL_SONGS_ROOT -> _songs.postValue(children)
                PLAYLISTS_ROOT -> _playlists.postValue(children)
                ALBUMS_ROOT -> _albums.postValue(children)
                ARTISTS_ROOT -> _artists.postValue(children)
            }
        }
    }

    private val _songs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val songs: LiveData<List<MediaBrowserCompat.MediaItem>> = _songs
    private val _playlists = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val playlists: LiveData<List<MediaBrowserCompat.MediaItem>> = _playlists
    private val _albums = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val albums: LiveData<List<MediaBrowserCompat.MediaItem>> = _albums
    private val _artists = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val artists: LiveData<List<MediaBrowserCompat.MediaItem>> = _artists

    init {
        serviceConnection.subscribe(ALL_SONGS_ROOT, subscriptionCallback)
        serviceConnection.subscribe(PLAYLISTS_ROOT, subscriptionCallback)
        serviceConnection.subscribe(ALBUMS_ROOT, subscriptionCallback)
        serviceConnection.subscribe(ARTISTS_ROOT, subscriptionCallback)
    }


    fun onPermissionGranted() {
        repository.setPermissionGranted(true)
    }

    override fun onCleared() {
        serviceConnection.unsubscribe(ALL_SONGS_ROOT, subscriptionCallback)
        serviceConnection.unsubscribe(PLAYLISTS_ROOT, subscriptionCallback)
        serviceConnection.unsubscribe(ALBUMS_ROOT, subscriptionCallback)
        serviceConnection.unsubscribe(ARTISTS_ROOT, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            MainViewModel(serviceConnection, repository) as T
    }
}

private const val TAG = "MainViewModel"