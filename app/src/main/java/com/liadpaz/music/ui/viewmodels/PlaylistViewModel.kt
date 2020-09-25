package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.*
import com.liadpaz.music.service.utils.PLAYLISTS_ROOT

class PlaylistViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository, private val playlist: String) : ViewModel() {

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) = _songs.postValue(children)
    }

    init {
        serviceConnection.subscribe(playlist, subscriptionCallback)
    }

    private val _songs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val songs: LiveData<List<MediaBrowserCompat.MediaItem>> = _songs

    fun play(mediaItem: MediaBrowserCompat.MediaItem, position: Int) =
        serviceConnection.transportControls?.playFromMediaId(mediaItem.mediaId, bundleOf(EXTRA_FROM to EXTRA_FROM_PLAYLISTS, EXTRA_PLAYLIST to playlist, EXTRA_POSITION to position))

    fun moveSong(fromPosition: Int, toPosition: Int) =
        repository.moveSongInPlaylist(playlist.substring(PLAYLISTS_ROOT.length), fromPosition, toPosition)

    fun deleteSong(position: Int) = repository.deletePlaylistSong(playlist.substring(PLAYLISTS_ROOT.length), position)

    fun playShuffle() =
        serviceConnection.transportControls?.playFromMediaId(playlist, bundleOf(EXTRA_FROM to EXTRA_FROM_PLAYLISTS, EXTRA_PLAYLIST to playlist, EXTRA_SHUFFLE to true))

    override fun onCleared() {
        serviceConnection.unsubscribe(playlist, subscriptionCallback)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository, private val playlist: String) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = PlaylistViewModel(serviceConnection, repository, playlist) as T
    }
}