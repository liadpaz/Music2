package com.liadpaz.music.service.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.gson.Gson

@WorkerThread
class DataSharedPrefs private constructor(private val generalPrefs: SharedPreferences, private val playlistsPrefs: SharedPreferences) {

    fun getPlaylists(): MutableList<Pair<String, MutableList<Int>>> =
        playlistsPrefs.all.map { (name, songs) ->
            Log.d(TAG, "getPlaylists: $songs")
            name to Gson().fromJson(songs.toString(), IntArray::class.java).toMutableList()
        }.toMutableList()

    fun setPlaylists(playlists: List<Pair<String, List<Int>>>?) =
        playlists?.let {
            playlistsPrefs.edit().clear().apply {
                for (playlist in it) {
                    putString(playlist.first, Gson().toJson(playlist.second))
                }
            }.apply()
        }

    fun getQueue(): List<Int> =
        Gson().fromJson(generalPrefs.getString(KEY_QUEUE, "[]"), IntArray::class.java).toList()

    fun setQueue(queue: List<Int>) =
        generalPrefs.edit().putString(KEY_QUEUE, Gson().toJson(queue)).apply()

    fun setQueuePosition(position: Int) =
        generalPrefs.edit().putInt(KEY_POSITION, position).apply()

    fun getQueuePosition() =
        generalPrefs.getInt(KEY_POSITION, 0)

    companion object {
        @Volatile
        private var instance: DataSharedPrefs? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance
                ?: DataSharedPrefs(context.getSharedPreferences(SHARED_PREFS_GENERAL_NAME, 0), context.getSharedPreferences(SHARED_PREFS_PLAYLISTS_NAME, 0)).also { instance = it }
        }
    }
}

const val SHARED_PREFS_GENERAL_NAME = "name" // TODO: get real name
const val SHARED_PREFS_PLAYLISTS_NAME = "com.liadpaz.music.playlists"

const val KEY_QUEUE = "key_queue"
const val KEY_POSITION = "key_position"

private const val TAG = "DataSharedPrefs"