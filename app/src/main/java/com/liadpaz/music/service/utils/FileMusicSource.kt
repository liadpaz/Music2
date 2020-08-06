package com.liadpaz.music.service.utils

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.liadpaz.music.data.Song
import com.liadpaz.music.utils.contentprovider.SongProvider
import com.liadpaz.music.utils.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileMusicSource(context: Context) : AbstractMusicSource() {

    private var catalog = emptyList<MediaMetadataCompat>()
    private val provider = SongProvider(context)

    init {
        state = STATE_INITIALIZING
    }

    override suspend fun load() {
        state = STATE_CREATED
        updateCatalog()?.let { updatedCatalog ->
            catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    private suspend fun updateCatalog(): List<MediaMetadataCompat>? = withContext(Dispatchers.IO) {
        provider.getContentProviderValue()?.map { song ->
            MediaMetadataCompat.Builder().from(song).build()
        }
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

}

fun MediaMetadataCompat.Builder.from(song: Song): MediaMetadataCompat.Builder {

    id = song.mediaId.toString()
    title = song.title
    artist = song.artist
    album = song.album
    if (song.duration != -1) {
        duration = song.duration.toLong()
    }
    mediaUri = song.mediaUri.toString()
    albumArtUri = song.artUri.toString()
    flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

    displayTitle = song.title
    displaySubtitle = song.artist
    displayDescription = song.album
    displayIconUri = song.artUri.toString()

    downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED

    return this
}