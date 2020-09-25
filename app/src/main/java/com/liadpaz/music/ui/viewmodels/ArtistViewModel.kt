package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.service.*
import com.liadpaz.music.service.utils.ARTISTS_ROOT

class ArtistViewModel(private val serviceConnection: ServiceConnection, private val artist: String) : ViewModel() {

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) = _songs.postValue(children)
    }

    init {
        serviceConnection.subscribe("${ARTISTS_ROOT}${artist}", subscriptionCallback)
    }

    private val _songs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val songs: LiveData<List<MediaBrowserCompat.MediaItem>> = _songs

    fun play(mediaItem: MediaBrowserCompat.MediaItem, position: Int) =
        serviceConnection.transportControls?.playFromMediaId(mediaItem.mediaId, bundleOf(EXTRA_FROM to EXTRA_FROM_ARTISTS, EXTRA_ARTIST to artist, EXTRA_POSITION to position))

    fun playShuffle() =
        serviceConnection.transportControls?.playFromMediaId(artist, bundleOf(EXTRA_FROM to EXTRA_FROM_ARTISTS, EXTRA_ARTIST to artist, EXTRA_SHUFFLE to true))

    override fun onCleared() {
        serviceConnection.unsubscribe(artist, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val artist: String) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = ArtistViewModel(serviceConnection, artist) as T
    }
}