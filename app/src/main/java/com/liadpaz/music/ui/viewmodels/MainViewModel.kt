package com.liadpaz.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.ServiceConnection

class MainViewModel(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModel() {

    fun onPermissionGranted() {
        repository.setPermissionGranted(true)
    }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            MainViewModel(serviceConnection, repository) as T
    }
}