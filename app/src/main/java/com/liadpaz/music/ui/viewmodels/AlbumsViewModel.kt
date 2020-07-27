package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.service.utils.ALBUMS_ROOT

class AlbumsViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModel() {

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) =
            _albums.postValue(children)
    }

    init {
        serviceConnection.subscribe(ALBUMS_ROOT, subscriptionCallback)
    }

    private val _albums = MutableLiveData<List<MediaBrowserCompat.MediaItem>>().apply {
        postValue(arrayListOf())
    }
    val albums: LiveData<List<MediaBrowserCompat.MediaItem>> = _albums

    override fun onCleared() {
        serviceConnection.unsubscribe(ALBUMS_ROOT, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            AlbumsViewModel(serviceConnection, repository) as T
    }
}