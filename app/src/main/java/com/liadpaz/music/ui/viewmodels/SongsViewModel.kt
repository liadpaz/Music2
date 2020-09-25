package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.service.*
import com.liadpaz.music.service.utils.ALL_SONGS_ROOT

class SongsViewModel(private val serviceConnection: ServiceConnection) : ViewModel() {

    fun play(mediaItem: MediaBrowserCompat.MediaItem, position: Int) =
        serviceConnection.transportControls?.playFromMediaId(mediaItem.mediaId, bundleOf(EXTRA_FROM to EXTRA_FROM_ALL, EXTRA_POSITION to position))

    fun playShuffle() =
        serviceConnection.transportControls?.playFromMediaId(ALL_SONGS_ROOT, bundleOf(EXTRA_FROM to EXTRA_FROM_ALL, EXTRA_SHUFFLE to true))

    class Factory(private val serviceConnection: ServiceConnection) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = SongsViewModel(serviceConnection) as T
    }
}