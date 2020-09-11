package com.liadpaz.music.utils.contentprovider

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.StringDef
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.liadpaz.music.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongProvider(private val context: Context, @Order var sortOrder: String = ORDER_DEFAULT, val folderLiveData: LiveData<String>? = null) : LiveData<ArrayList<Song>?>() {

    private var privateFolder = ""

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = onChange(selfChange, null)

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            CoroutineScope(Dispatchers.Main).launch {
                postValue(getContentProviderValue())
            }
        }
    }
    private val folderObserver = Observer { folder: String ->
        CoroutineScope(Dispatchers.Main).launch {
            privateFolder = folder
            postValue(getContentProviderValue())
        }
    }

    override fun onActive() {
        context.contentResolver.registerContentObserver(mediaStoreUri, true, contentObserver)
        folderLiveData?.apply {
            observeForever(folderObserver)
            privateFolder = value!!
        }
        CoroutineScope(Dispatchers.Main).launch {
            postValue(getContentProviderValue())
        }
    }

    override fun onInactive() {
        context.contentResolver.unregisterContentObserver(contentObserver)
        folderLiveData?.removeObserver(folderObserver)
    }

    private suspend fun getContentProviderValue(): ArrayList<Song>? = withContext(Dispatchers.IO) {
        context.contentResolver.query(mediaStoreUri, PROJECTION, "_data like ?", arrayOf("%$privateFolder%"), sortOrder)?.use { cursor ->
            val songs = arrayListOf<Song>()
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationIndex = if (Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.Audio.Media.DURATION) else -1
                do {
                    val mediaId = cursor.getInt(idIndex).toLong()
                    songs.add(
                        Song(
                            mediaId,
                            ContentUris.withAppendedId(mediaStoreUri, mediaId),
                            cursor.getString(titleIndex),
                            cursor.getString(artistIndex),
                            cursor.getString(albumIndex),
                            Uri.parse("content://media/external/audio/albumart/${cursor.getInt(albumIdIndex)}"),
                            if (durationIndex != -1) {
                                cursor.getInt(durationIndex)
                            } else -1
                        )
                    )
                } while (cursor.moveToNext())
            }
            return@use songs
        }
    }

    companion object {
        private val PROJECTION =
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID)

        const val FOLDER_DEFAULT = "Music"

        private val mediaStoreUri =
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
}

const val ORDER_DEFAULT = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
const val ORDER_LAST_ADDED = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

@StringDef(value = [ORDER_DEFAULT, ORDER_LAST_ADDED])
annotation class Order

private const val TAG = "ContentProviderLiveData"