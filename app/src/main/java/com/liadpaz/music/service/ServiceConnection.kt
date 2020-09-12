package com.liadpaz.music.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.liadpaz.music.service.MusicService.QueueEditor.Companion.ACTION_MOVE_ITEM
import com.liadpaz.music.service.MusicService.QueueEditor.Companion.ACTION_REMOVE_ITEM
import com.liadpaz.music.service.MusicService.QueueEditor.Companion.EXTRA_FROM_POSITION
import com.liadpaz.music.service.MusicService.QueueEditor.Companion.EXTRA_TO_POSITION
import com.liadpaz.music.utils.extensions.id

class ServiceConnection private constructor(context: Context, serviceComponent: ComponentName) {
    val isConnected = MutableLiveData<Boolean>()
        .apply { postValue(false) }

    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }

    private val _repeatMode = MutableLiveData<@PlaybackStateCompat.RepeatMode Int>().apply {
        postValue(PlaybackStateCompat.REPEAT_MODE_ALL)
    }
    val repeatMode: LiveData<Int> = _repeatMode

    val transportControls: MediaControllerCompat.TransportControls?
        get() = mediaController?.transportControls

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser =
        MediaBrowserCompat(context, serviceComponent, mediaBrowserConnectionCallback, null).apply {
            connect()
        }
    private var mediaController: MediaControllerCompat? = null

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) = mediaBrowser.subscribe(parentId, callback)

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) = mediaBrowser.unsubscribe(parentId, callback)

    fun addQueueItem(item: MediaDescriptionCompat) = mediaController?.addQueueItem(item)

    fun addQueueItem(item: MediaDescriptionCompat, position: Int) = mediaController?.addQueueItem(item, position)

    fun removeQueueItemAt(position: Int) = sendCommand(ACTION_REMOVE_ITEM, bundleOf(EXTRA_POSITION to position))

    fun moveQueueItem(fromPosition: Int, toPosition: Int) =
        sendCommand(ACTION_MOVE_ITEM, bundleOf(EXTRA_FROM_POSITION to fromPosition, EXTRA_TO_POSITION to toPosition))

    private fun sendCommand(command: String, parameters: Bundle?, resultCallback: ((Int, Bundle?) -> Unit) = { _, _ -> }) =
        if (mediaBrowser.isConnected) {
            mediaController?.sendCommand(command, parameters, object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    resultCallback(resultCode, resultData)
                }
            })
            true
        } else {
            false
        }

    private inner class MediaBrowserConnectionCallback(private val context: Context) : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }

            isConnected.postValue(true)
        }

        override fun onConnectionSuspended() = isConnected.postValue(false)

        override fun onConnectionFailed() = isConnected.postValue(false)
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) =
            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) =
            nowPlaying.postValue(
                if (metadata?.id == null) {
                    NOTHING_PLAYING
                } else {
                    metadata
                }
            )

        override fun onRepeatModeChanged(repeatMode: Int) = _repeatMode.postValue(repeatMode)

        override fun onSessionDestroyed() =
            mediaBrowserConnectionCallback.onConnectionSuspended()
    }

    companion object {
        @Volatile
        private var instance: ServiceConnection? = null

        fun getInstance(context: Context): ServiceConnection =
            instance ?: synchronized(this) {
                instance ?: ServiceConnection(context, ComponentName(context, MusicService::class.java)).also { instance = it }
            }
    }
}

val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
    .build()

val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, null)
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()

private const val TAG = "ServiceConnection"