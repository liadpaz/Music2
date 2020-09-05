package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.service.utils.PLAYLISTS_ROOT

class PlaylistsViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModel() {

    private val _playlists = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val playlists: LiveData<List<MediaBrowserCompat.MediaItem>> = _playlists

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) =
            _playlists.postValue(children)
    }

    init {
        serviceConnection.subscribe(PLAYLISTS_ROOT, subscriptionCallback)
    }

    override fun onCleared() {
        serviceConnection.unsubscribe(PLAYLISTS_ROOT, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            PlaylistsViewModel(serviceConnection, repository) as T
    }
}

private const val TAG = "PlaylistsViewModel"