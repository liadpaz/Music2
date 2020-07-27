package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.service.utils.ARTISTS_ROOT

class ArtistsViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModel() {

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) =
            _artists.postValue(children)
    }

    init {
        serviceConnection.subscribe(ARTISTS_ROOT, subscriptionCallback)
    }

    private val _artists = MutableLiveData<List<MediaBrowserCompat.MediaItem>>().apply {
        postValue(emptyList())
    }
    val artists: LiveData<List<MediaBrowserCompat.MediaItem>> = _artists

    override fun onCleared() {
        serviceConnection.unsubscribe(ARTISTS_ROOT, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ArtistsViewModel(serviceConnection, repository) as T
    }
}