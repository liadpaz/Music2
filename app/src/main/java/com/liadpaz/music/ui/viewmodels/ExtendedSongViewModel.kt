package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.service.ServiceConnection

class ExtendedSongViewModel(private val serviceConnection: ServiceConnection) : ViewModel() {

    val queue: List<MediaSessionCompat.QueueItem>
        get() = serviceConnection.queue.value ?: listOf()

    val queueSize: Int
        get() = queue.size

    class Factory(private val serviceConnection: ServiceConnection) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ExtendedSongViewModel(serviceConnection) as T
    }
}