package com.liadpaz.music.service.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.flac.FlacExtractor
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor
import com.google.android.exoplayer2.extractor.wav.WavExtractor
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.ContentDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.liadpaz.music.R
import com.liadpaz.music.service.EXTRA_POSITION
import com.liadpaz.music.utils.GlideRequests
import com.liadpaz.music.utils.extensions.displayIcon
import com.liadpaz.music.utils.extensions.duration
import com.liadpaz.music.utils.extensions.toMediaMetadataBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class ControlDispatcher : ControlDispatcher {
    private val window: Timeline.Window = Timeline.Window()

    override fun dispatchSetPlayWhenReady(player: Player, playWhenReady: Boolean): Boolean = let {
        player.playWhenReady = playWhenReady
        true
    }

    override fun dispatchSeekTo(player: Player, windowIndex: Int, positionMs: Long): Boolean = let {
        player.seekTo(windowIndex, positionMs)
        true
    }

    override fun dispatchPrevious(player: Player): Boolean = let {
        val timeline = player.currentTimeline
        if (player.mediaItemCount != 0) {
            timeline.getWindow(player.currentWindowIndex, window)
            val previousWindowIndex = player.previousWindowIndex
            if (previousWindowIndex != C.INDEX_UNSET && player.currentPosition <= 3000) {
                player.seekTo(previousWindowIndex, C.TIME_UNSET)
            } else if (previousWindowIndex != C.INDEX_UNSET && player.currentPosition > 3000) {
                player.seekTo(player.currentWindowIndex, 0)
            } else {
                player.seekTo(timeline.windowCount - 1, C.TIME_UNSET)
            }
        }
        true
    }

    override fun dispatchNext(player: Player): Boolean = let {
        val nextWindowIndex = player.nextWindowIndex
        if (player.mediaItemCount != 0) {
            player.seekTo(if (nextWindowIndex != C.INDEX_UNSET) nextWindowIndex else 0, C.TIME_UNSET)
        }
        true
    }

    override fun dispatchRewind(player: Player): Boolean = true

    override fun dispatchFastForward(player: Player): Boolean = true

    override fun dispatchSetRepeatMode(player: Player, repeatMode: Int): Boolean = let {
        player.repeatMode = repeatMode
        true
    }

    override fun dispatchSetShuffleModeEnabled(player: Player, shuffleModeEnabled: Boolean): Boolean = let {
        player.shuffleModeEnabled = shuffleModeEnabled
        true
    }

    override fun dispatchStop(player: Player, reset: Boolean): Boolean = let {
        player.stop(reset)
        true
    }

    override fun isRewindEnabled(): Boolean = false

    override fun isFastForwardEnabled(): Boolean = false
}

/**
 * This class is for retrieving the metadata about the currently playing media item.
 */
class MetadataProvider(private val connector: MediaSessionConnector, private val glide: GlideRequests) : MediaSessionConnector.MediaMetadataProvider {
    private var currentUri: Uri? = null
    private var bitmap: Bitmap? = null

    override fun getMetadata(player: Player): MediaMetadataCompat =
        (player.currentMediaItem?.playbackProperties?.tag as? MediaDescriptionCompat)?.let { description ->
            description.toMediaMetadataBuilder().also {
                it.duration = player.duration
                if (description.iconUri != currentUri || bitmap == null) {
                    currentUri = description.iconUri
                    currentUri?.let { loadedBitmap ->
                        Executors.newSingleThreadExecutor().execute {
                            bitmap = try {
                                glide.asBitmap().load(loadedBitmap).submit().get()
                            } catch (e: Exception) {
                                glide.asBitmap().load(R.drawable.ic_album_color).submit().get()
                            }
                            CoroutineScope(Dispatchers.Main).launch {
                                connector.invalidateMediaSessionMetadata()
                            }
                        }
                    }
                }
                it.displayIcon = bitmap
            }.build()
        } ?: MediaMetadataCompat.Builder().build()
}

/**
 * This class is for editing the playing queue from commands from the {@code MediaController}.
 */
class QueueEditor : MediaSessionConnector.QueueEditor {
    override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean =
        when (command) {
            ACTION_REMOVE_ITEM -> {
                player.removeMediaItem(extras!!.getInt(EXTRA_POSITION))
                true
            }
            ACTION_MOVE_ITEM -> {
                player.moveMediaItem(extras!!.getInt(EXTRA_FROM_POSITION), extras.getInt(EXTRA_TO_POSITION))
                true
            }
            else -> false
        }

    override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat) =
        player.addMediaItem(MediaItem.Builder().setUri(description.mediaUri).setMediaId(description.mediaId).setTag(description).build())

    override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat, index: Int) =
        player.addMediaItem(index, MediaItem.Builder().setUri(description.mediaUri).setMediaId(description.mediaId).setTag(description).build())

    /**
     * This function is unsupported, needs to use {@code ACTION_REMOVE_ITEM} and to use positional removal
     */
    override fun onRemoveQueueItem(player: Player, description: MediaDescriptionCompat): Nothing =
        throw UnsupportedOperationException("Needs to take position")

    companion object {
        const val ACTION_REMOVE_ITEM = "action_remove_item"
        const val ACTION_MOVE_ITEM = "action_move_item"
        const val EXTRA_FROM_POSITION = "extra_from_position"
        const val EXTRA_TO_POSITION = "extra_to_position"
    }
}

/**
 * This class is for navigating the queue. It's using the default implementation of {@code MediaSessionConnector#TimelineQueueNavigator} and
 * implementing the {code getMediaDescription} in order to get the media description from the {@code MediaItem} tag.
 */
class QueueNavigator(mediaSession: MediaSessionCompat) : TimelineQueueNavigator(mediaSession) {
    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
        player.getMediaItemAt(windowIndex).playbackProperties?.tag as MediaDescriptionCompat

    override fun getSupportedQueueNavigatorActions(player: Player): Long =
        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
}

/**
 * This class is to create audio only media sources.
 */
class AudioMediaSourceFactory(context: Context) : MediaSourceFactory {
    private val extractorsFactory = ExtractorsFactory { arrayOf(Mp3Extractor(), Mp4Extractor(), FlacExtractor(), WavExtractor()) }
    private val dataSourceFactory = DataSource.Factory { ContentDataSource(context) }

    override fun setDrmSessionManager(drmSessionManager: DrmSessionManager?): MediaSourceFactory = this

    override fun setDrmHttpDataSourceFactory(drmHttpDataSourceFactory: HttpDataSource.Factory?): MediaSourceFactory = this

    override fun setDrmUserAgent(userAgent: String?): MediaSourceFactory = this

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy?): MediaSourceFactory = this

    override fun getSupportedTypes(): IntArray = intArrayOf()

    override fun createMediaSource(mediaItem: MediaItem): MediaSource =
        ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(mediaItem)
}

private const val TAG = "ServiceUtils"