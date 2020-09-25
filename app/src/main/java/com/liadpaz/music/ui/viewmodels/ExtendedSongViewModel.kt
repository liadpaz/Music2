package com.liadpaz.music.ui.viewmodels

import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository

class ExtendedSongViewModel(private val repository: Repository) : ViewModel() {

    val queue: List<MediaMetadataCompat>
        get() = repository.queue.value ?: listOf()

    val queueSize: Int
        get() = queue.size

    class Factory(private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = ExtendedSongViewModel(repository) as T
    }
}