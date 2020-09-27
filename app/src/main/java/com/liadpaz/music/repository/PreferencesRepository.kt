package com.liadpaz.music.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.liadpaz.music.utils.contentprovider.SongProvider

class PreferencesRepository private constructor(context: Context) {

    private val _granted = MutableLiveData<Boolean>()
    val granted: LiveData<Boolean> = _granted

    private val _stopTask = MutableLiveData<Boolean>().apply {
        postValue(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_STOP_TASK, false))
    }
    val stopTask: LiveData<Boolean> = _stopTask

    private val _folder = MutableLiveData<String>().apply {
        postValue(PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_FOLDER, SongProvider.FOLDER_DEFAULT))
    }
    val folder: LiveData<String> = _folder

    private val _displayOn = MutableLiveData<Boolean>().apply {
        postValue(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_DISPLAY, false))
    }
    val displayOn: LiveData<Boolean> = _displayOn

    fun setPermissionGranted(granted: Boolean) = _granted.postValue(granted)

    fun setFolder(folder: String = SongProvider.FOLDER_DEFAULT) = _folder.postValue(folder)

    fun setStopTask(stopTask: Boolean) = _stopTask.postValue(stopTask)

    fun setDisplayOn(displayOn: Boolean) = _displayOn.postValue(displayOn)

    companion object {
        @Volatile
        private var instance: PreferencesRepository? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: PreferencesRepository(context).also { instance = it }
        }

        private const val KEY_STOP_TASK = "key_stop_task"
        private const val KEY_FOLDER = "key_folder"
        private const val KEY_DISPLAY = "key_display"
    }
}