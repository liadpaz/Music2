package com.liadpaz.music.repository

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.FieldPosition

class Repository private constructor(private val context: Context) {

    private val _granted = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val granted: LiveData<Boolean> = _granted

    private val _queue = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
    val queue: LiveData<List<MediaSessionCompat.QueueItem>> = _queue

    private val _queuePosition = MutableLiveData<Int>()
    val queuePosition: LiveData<Int> = _queuePosition

    fun setQueuePosition(position: Int): Unit = position.let {
        if (it != _queuePosition.value) {
            _queuePosition.postValue(position)
        }
    }

    fun setPermissionGranted(granted: Boolean) = _granted.postValue(granted)

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: Repository(context).also { instance = it }
        }
    }
}