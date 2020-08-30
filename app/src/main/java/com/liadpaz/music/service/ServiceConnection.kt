package com.liadpaz.music.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.liadpaz.music.utils.extensions.id
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ServiceConnection private constructor(context: Context, serviceComponent: ComponentName) {
    val isConnected = MutableLiveData<Boolean>()
        .apply { postValue(false) }

    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }

    private val _queue = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
    val queue: LiveData<List<MediaSessionCompat.QueueItem>> = _queue

    val transportControls: MediaControllerCompat.TransportControls?
        get() = mediaController?.transportControls

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser =
        MediaBrowserCompat(context, serviceComponent, mediaBrowserConnectionCallback, null).apply {
            connect()
        }
    private var mediaController: MediaControllerCompat? = null

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) =
        mediaBrowser.subscribe(parentId, callback)

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) =
        mediaBrowser.unsubscribe(parentId, callback)

    fun sendCommand(command: String, parameters: Bundle?, resultCallback: ((Int, Bundle?) -> Unit) = { _, _ -> }) =
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

    suspend fun sendCommand(command: String, parameters: Bundle?): Pair<Int, Bundle?> =
        suspendCancellableCoroutine { cont ->
            sendCommand(command, parameters) { resultCode, resultData ->
                cont.resume(Pair(resultCode, resultData))
            }
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

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            if (!queue.contentEquals(_queue.value)) {
                _queue.postValue(queue)
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

    companion object {
        @Volatile
        private var instance: ServiceConnection? = null

        fun getInstance(context: Context, serviceComponent: ComponentName): ServiceConnection =
            instance ?: synchronized(this) {
                instance ?: ServiceConnection(context, serviceComponent).also { instance = it }
            }
    }
}

val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
    .build()

val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()

fun List<Any>?.contentEquals(other: List<Any>?): Boolean {
    if (this == null || other == null) {
        Log.d(TAG, "contentEquals: "); return this == other
    }
    if (this.size != other.size) return false

    forEachIndexed { index, any ->
        Log.d(TAG, "contentEquals:\n$any\n${other[index]}")
        if (other[index] != any) {
            return false
        }
    }

    return true
}

private const val TAG = "ServiceConnection"