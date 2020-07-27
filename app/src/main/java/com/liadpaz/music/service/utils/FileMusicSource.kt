package com.liadpaz.music.service.utils

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.liadpaz.music.data.Song
import com.liadpaz.music.utils.GlideApp
import com.liadpaz.music.utils.contentprovider.SongProvider
import com.liadpaz.music.utils.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileMusicSource(context: Context) : AbstractMusicSource() {

    private var catalog = emptyList<MediaMetadataCompat>()
    private val glide = GlideApp.with(context)
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
//            val bitmap = glide.asBitmap().load(song.artUri).submit().get()

            MediaMetadataCompat.Builder()
                .from(song)
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                .build()
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

    // To make things easier for *displaying* these, set the display properties as well.
    displayTitle = song.title
    displaySubtitle = song.artist
    displayDescription = song.album
    displayIconUri = song.artUri.toString()

    // Add downloadStatus to force the creation of an "extras" bundle in the resulting
    // MediaMetadataCompat object. This is needed to send accurate metadata to the
    // media session during updates.
    downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED

    // Allow it to be used in the typical builder style.
    return this
}