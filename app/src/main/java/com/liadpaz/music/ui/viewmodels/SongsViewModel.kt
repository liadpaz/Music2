package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.EXTRA_FROM
import com.liadpaz.music.service.EXTRA_FROM_ALL
import com.liadpaz.music.service.ServiceConnection
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

    fun play(mediaItem: MediaBrowserCompat.MediaItem) =
        serviceConnection.transportControls?.playFromMediaId(mediaItem.mediaId, bundleOf(Pair(EXTRA_FROM, EXTRA_FROM_ALL)))

    override fun onCleared() {
        serviceConnection.unsubscribe(ALL_SONGS_ROOT, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            SongsViewModel(serviceConnection) as T
    }
}