package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.*
import com.liadpaz.music.service.utils.ALL_SONGS_ROOT

class SongsViewModel(private val serviceConnection: ServiceConnection) : ViewModel() {

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) =
            _songs.postValue(ArrayList(children))
    }

    init {
        serviceConnection.subscribe(ALL_SONGS_ROOT, subscriptionCallback)
    }

    private val _songs = MutableLiveData<ArrayList<MediaBrowserCompat.MediaItem>>().apply {
        postValue(arrayListOf())
    }
    val songs: LiveData<ArrayList<MediaBrowserCompat.MediaItem>> = _songs

    fun play(mediaItem: MediaBrowserCompat.MediaItem, position: Int) =
        serviceConnection.transportControls?.playFromMediaId(mediaItem.mediaId, bundleOf(EXTRA_FROM to EXTRA_FROM_ALL, EXTRA_POSITION to position))

    fun playShuffle() =
        serviceConnection.transportControls?.playFromMediaId(ALL_SONGS_ROOT, bundleOf(EXTRA_FROM to EXTRA_FROM_ALL, EXTRA_SHUFFLE to true))

    override fun onCleared() {
        serviceConnection.unsubscribe(ALL_SONGS_ROOT, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            SongsViewModel(serviceConnection) as T
    }
}