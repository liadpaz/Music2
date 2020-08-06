package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.EXTRA_FROM
import com.liadpaz.music.service.EXTRA_FROM_ALBUMS
import com.liadpaz.music.service.EXTRA_FROM_ALL
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.service.utils.ALBUMS_ROOT

class AlbumViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository, private val album: String) : ViewModel() {

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) =
            _songs.postValue(children)
    }

    init {
        serviceConnection.subscribe(album, subscriptionCallback)
    }

    private val _songs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>().apply {
        postValue(listOf())
    }
    val songs: LiveData<List<MediaBrowserCompat.MediaItem>> = _songs

    fun play(mediaItem: MediaBrowserCompat.MediaItem) =
        serviceConnection.transportControls.playFromMediaId(mediaItem.mediaId, bundleOf(EXTRA_FROM to EXTRA_FROM_ALBUMS))

    override fun onCleared() {
        serviceConnection.unsubscribe(album, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository, private val album: String) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            AlbumViewModel(serviceConnection, repository, album) as T
    }
}