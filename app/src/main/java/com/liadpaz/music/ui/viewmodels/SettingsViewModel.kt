package com.liadpaz.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.PreferencesRepository

class SettingsViewModel(private val repository: PreferencesRepository) : ViewModel() {

    val folder = repository.folder

    fun setStopTask(stopTask: Boolean) = repository.setStopTask(stopTask)

    fun resetFolder() = repository.setFolder()

    fun setFolder(folder: String) = repository.setFolder(folder)

    fun setDisplayOn(displayOn: Boolean) = repository.setDisplayOn(displayOn)

    @Suppress("UNCHECKED_CAST")
    class Factory(private val repository: PreferencesRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            SettingsViewModel(repository) as T
    }
}
