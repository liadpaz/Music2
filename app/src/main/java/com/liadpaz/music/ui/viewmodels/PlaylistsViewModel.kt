package com.liadpaz.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository

class PlaylistsViewModel(private val repository: Repository) : ViewModel() {
    // TODO: Implement the ViewModel

    class Factory(private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            PlaylistsViewModel(repository) as T
    }
}