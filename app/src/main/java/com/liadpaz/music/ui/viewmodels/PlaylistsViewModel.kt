package com.liadpaz.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.repository.Repository

class PlaylistsViewModel(private val repository: Repository) : ViewModel() {

    val playlists: List<String>
        get() = repository.playlists.value?.map { it.first } ?: emptyList()

    fun createNewPlaylist(name: String, songs: IntArray? = intArrayOf()) = repository.createNewPlaylist(name, songs ?: intArrayOf())

    fun deletePlaylist(name: String) = repository.deletePlaylist(name)

    fun addSongsToPlaylist(name: String, songs: IntArray? = null) = repository.addSongsToPlaylist(name, songs ?: intArrayOf())

    fun changeName(oldName: String, newName: String) = repository.changePlaylistName(oldName, newName)

    class Factory(private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = PlaylistsViewModel(repository) as T
    }
}