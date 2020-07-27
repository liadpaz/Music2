package com.liadpaz.music.ui.viewmodels

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.ServiceConnection

class MainViewModel(private val repository: Repository) : ViewModel() {

    private var bottomSheetBehavior: BottomSheetBehavior<ViewGroup>? = null

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) =
            _slideOffset.postValue(slideOffset)

        override fun onStateChanged(bottomSheet: View, newState: Int) =
            _bottomSheetState.postValue(newState)
    }

    private val _slideOffset = MutableLiveData<Float>().apply {
        postValue(0F)
    }

    private val _bottomSheetState = MutableLiveData<Int>().apply {
        postValue(BottomSheetBehavior.STATE_HIDDEN)
    }
    val slideOffset: LiveData<Float> = _slideOffset
    val bottomSheetState: LiveData<Int> = _bottomSheetState

    fun setBottomSheetBehavior(bottomSheetBehavior: BottomSheetBehavior<ViewGroup>): Unit =
        bottomSheetBehavior.let {
            it.addBottomSheetCallback(bottomSheetCallback)
            this.bottomSheetBehavior = it
        }

    fun getPeekHeight() = bottomSheetBehavior?.peekHeight ?: 0

    override fun onCleared() {
        bottomSheetBehavior?.removeBottomSheetCallback(bottomSheetCallback)?.let {
            bottomSheetBehavior = null
        }
    }

    fun onPermissionGranted() {
        repository.setPermissionGranted(true)
    }

    fun setState(@BottomSheetBehavior.State state: Int) =
        bottomSheetBehavior?.let { it.state = state }

    class Factory(private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            MainViewModel(repository) as T
    }
}