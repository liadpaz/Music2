package com.liadpaz.music.service.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@WorkerThread
class DataSharedPrefs private constructor(private val generalPrefs: SharedPreferences, private val playlistsPrefs: SharedPreferences) {

    fun getPlaylists(): MutableList<Pair<String, MutableList<Int>>> = playlistsPrefs.all.map { (name, songs) ->
        name to Gson().fromJson(songs.toString(), IntArray::class.java).toMutableList()
    }.sortedBy { getPlaylistsOrder().indexOf(it.first) }.toMutableList()

    fun setPlaylists(playlists: List<Pair<String, List<Int>>>?) = playlists?.let {
        setPlaylistsOrder(it.map { pair -> pair.first })
        playlistsPrefs.edit().clear().apply {
            for (playlist in it) {
                putString(playlist.first, Gson().toJson(playlist.second))
            }
        }.apply()
    }

    var queue: List<Int>
        get() = Gson().fromJson(generalPrefs.getString(KEY_QUEUE, "[]"), IntArray::class.java).toList()
        set(queue) = generalPrefs.edit().putString(KEY_QUEUE, Gson().toJson(queue)).apply()

    var queuePosition: Int
        get() = generalPrefs.getInt(KEY_POSITION, 0)
        set(position) = generalPrefs.edit().putInt(KEY_POSITION, position).apply()

    var mediaPosition: Long
        get() = generalPrefs.getLong(KEY_MEDIA_POSITION, 0)
        set(position) = generalPrefs.edit().putLong(KEY_MEDIA_POSITION, position).apply()

    private fun getPlaylistsOrder(): List<String> =
        Gson().fromJson(generalPrefs.getString(KEY_PLAYLISTS, "[]"), object : TypeToken<List<String>>() {}.type)

    private fun setPlaylistsOrder(order: List<String>?) =
        generalPrefs.edit().putString(KEY_PLAYLISTS, Gson().toJson(order)).apply()

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
const val KEY_MEDIA_POSITION = "key_media_position"
const val KEY_PLAYLISTS = "key_playlists"

private const val TAG = "DataSharedPrefs"