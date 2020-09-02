package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.ServiceConnection

class ExtendedSongViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModel() {

    val queue: List<MediaSessionCompat.QueueItem>
        get() = repository.queue.value ?: listOf()

    val queueSize: Int
        get() = queue.size

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ExtendedSongViewModel(serviceConnection, repository) as T
    }
}