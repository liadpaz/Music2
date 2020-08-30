package com.liadpaz.music.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.EMPTY_PLAYBACK_STATE
import com.liadpaz.music.service.NOTHING_PLAYING
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.utils.GlideApp
import com.liadpaz.music.utils.Util
import com.liadpaz.music.utils.extensions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.floor

class PlayingViewModel(app: Application, private val serviceConnection: ServiceConnection, private val repository: Repository) : AndroidViewModel(app) {

    data class NowPlayingMetadata(val id: String, val albumArtUri: Uri, val title: String?, val artist: String?, val duration: Long, val color: Palette) {

        companion object {
            fun timestampToMSS(position: Long): String {
                val totalSeconds = floor(position / 1E3).toInt()
                val minutes = totalSeconds / 60
                val remainingSeconds = totalSeconds - (minutes * 60)
                return "%d:%02d".format(minutes, remainingSeconds)
            }
        }
    }

    private val _playbackState = MutableLiveData<PlaybackStateCompat>().apply {
        postValue(EMPTY_PLAYBACK_STATE)
    }
    private val _mediaMetadata = MutableLiveData<NowPlayingMetadata>()
    private val _mediaPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }

    val playbackState: LiveData<PlaybackStateCompat> = _playbackState
    val mediaMetadata: LiveData<NowPlayingMetadata> = _mediaMetadata
    val mediaPosition: LiveData<Long> = _mediaPosition
    val queue = serviceConnection.queue
    val queuePosition: LiveData<Int> = repository.queuePosition

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        _playbackState.postValue(it ?: EMPTY_PLAYBACK_STATE)
        val metadata = serviceConnection.nowPlaying.value ?: NOTHING_PLAYING
        updateState(metadata)
    }

    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        updateState(it)
    }

    private fun checkPlaybackPosition(): Boolean = handler.postDelayed({
        val currPosition = _playbackState.value?.currentPlayBackPosition
        if (_mediaPosition.value != currPosition) {
            _mediaPosition.postValue(currPosition)
        }
        if (updatePosition) {
            checkPlaybackPosition()
        }
    }, POSITION_UPDATE_INTERVAL_MILLIS)

    fun skipToQueueItem(position: Int) =
        serviceConnection.transportControls?.skipToQueueItem(position.toLong())


    fun skipToPrev() = serviceConnection.transportControls?.skipToPrevious()

    fun skipToNext() = serviceConnection.transportControls?.skipToNext()

    fun stop() = serviceConnection.transportControls?.stop()

    fun seekTo(position: Long) = serviceConnection.transportControls?.seekTo(position)

    init {
        serviceConnection.playbackState.observeForever(playbackStateObserver)
        serviceConnection.nowPlaying.observeForever(mediaMetadataObserver)
        checkPlaybackPosition()
    }

    override fun onCleared() {
        serviceConnection.playbackState.removeObserver(playbackStateObserver)
        serviceConnection.nowPlaying.removeObserver(mediaMetadataObserver)

        updatePosition = false
    }

    private fun updateState(mediaMetadata: MediaMetadataCompat) {
        if (mediaMetadata.duration != 0L && mediaMetadata.id != null) {
            CoroutineScope(Dispatchers.IO).launch {
                _mediaMetadata.postValue(
                    NowPlayingMetadata(
                        mediaMetadata.id!!,
                        mediaMetadata.albumArtUri,
                        mediaMetadata.title?.trim(),
                        mediaMetadata.displaySubtitle?.trim(),
                        mediaMetadata.duration,
                        Palette.from(Util.getBitmap(GlideApp.with(getApplication() as Context), mediaMetadata.albumArtUri)).generate()
                    )
                )
            }
        }
    }

    fun playPause() = serviceConnection.transportControls?.apply {
        Log.d(TAG, "playPause: ")
        when (_playbackState.value?.state) {
            PlaybackStateCompat.STATE_PLAYING,
            PlaybackStateCompat.STATE_BUFFERING -> pause()
            else -> play()
        }
    }

    class Factory(private val app: Application, private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            PlayingViewModel(app, serviceConnection, repository) as T
    }
}

inline val PlaybackStateCompat.currentPlayBackPosition: Long
    get() = if (state == PlaybackStateCompat.STATE_PLAYING) {
        val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        (position + (timeDelta * playbackSpeed)).toLong()
    } else {
        position
    }

private const val POSITION_UPDATE_INTERVAL_MILLIS = 100L

private const val TAG = "PlayingViewModel"