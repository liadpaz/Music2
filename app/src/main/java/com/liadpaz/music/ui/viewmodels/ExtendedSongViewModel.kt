package com.liadpaz.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository

class ExtendedSongViewModel(repository: Repository) : ViewModel() {

    val queue = repository.queue

    val queuePosition = repository.queuePosition

    class Factory(private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = ExtendedSongViewModel(repository) as T
    }
}