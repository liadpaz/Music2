package com.liadpaz.music.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class Repository private constructor(private val context: Context) {

    private val _granted = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val granted: LiveData<Boolean> = _granted

    fun setPermissionGranted(granted: Boolean) = _granted.postValue(granted)

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: Repository(context).also { instance = it }
        }
    }
}