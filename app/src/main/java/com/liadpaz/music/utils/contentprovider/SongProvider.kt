package com.liadpaz.music.utils.contentprovider

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.StringDef
import androidx.core.database.getIntOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.liadpaz.music.utils.extensions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongProvider(private val context: Context, @Order var sortOrder: String = ORDER_DEFAULT, val folderLiveData: LiveData<String>? = null) : LiveData<ArrayList<MediaMetadataCompat>?>() {

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

    private suspend fun getContentProviderValue(): ArrayList<MediaMetadataCompat>? = withContext(Dispatchers.IO) {
        context.contentResolver.query(mediaStoreUri, PROJECTION, "_data like ?", arrayOf("%$privateFolder%"), sortOrder)?.use { cursor ->
            val songs = arrayListOf<MediaMetadataCompat>()
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
                        MediaMetadataCompat.Builder().create(
                            mediaId,
                            ContentUris.withAppendedId(mediaStoreUri, mediaId),
                            cursor.getString(titleIndex),
                            cursor.getString(artistIndex),
                            cursor.getString(albumIndex),
                            Uri.parse("content://media/external/audio/albumart/${cursor.getInt(albumIdIndex)}"),
                            cursor.getIntOrNull(durationIndex)
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

private fun MediaMetadataCompat.Builder.create(mediaId: Long, mediaUri: Uri, title: String, artist: String, album: String, artUri: Uri, duration: Int?): MediaMetadataCompat {
    this.id = mediaId.toString()
    this.mediaUri = mediaUri.toString()
    this.displayTitle = title
    this.displaySubtitle = artist
    this.displayDescription = album
    this.displayIconUri = artUri.toString()
    duration?.let { this.duration = it.toLong() }
    this.downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
    return build()
}

const val ORDER_DEFAULT = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
const val ORDER_LAST_ADDED = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

@StringDef(value = [ORDER_DEFAULT, ORDER_LAST_ADDED])
annotation class Order

fun MediaMetadataCompat.findArtists() =
    artistsRegex.findAll(displaySubtitle.toString()).toList().map(MatchResult::value)

fun MediaMetadataCompat.findFirstArtist() = findArtists()[0]

fun MediaDescriptionCompat.findArtists(): List<String> = artistsRegex.findAll(subtitle.toString()).toList().map(MatchResult::value)

private val artistsRegex = Regex("([^ &,]([^,&])*[^ ,&]+)")