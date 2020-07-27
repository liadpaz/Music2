package com.liadpaz.music.utils.contentprovider

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.liadpaz.music.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongProvider(private val context: Context, var sortOrder: String = DEFAULT_ORDER, var folder: String = DEFAULT_FOLDER) {

    suspend fun getContentProviderValue(): ArrayList<Song>? = withContext(Dispatchers.IO) {
        context.contentResolver.query(mediaStoreUri, PROJECTION, "_data like ?", arrayOf("%$folder%"), sortOrder)?.use { cursor ->
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
                                Log.d(TAG, "getContentProviderValue: ")
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

        const val DEFAULT_ORDER = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        const val DEFAULT_FOLDER = "Music"

        private val mediaStoreUri =
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
}

private const val TAG = "ContentProviderLiveData"