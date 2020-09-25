package com.liadpaz.music.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.EMPTY_PLAYBACK_STATE
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.utils.GlideApp
import com.liadpaz.music.utils.extensions.*
import com.liadpaz.music.utils.getBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.floor

class PlayingViewModel(app: Application, private val serviceConnection: ServiceConnection, repository: Repository) : AndroidViewModel(app) {

    data class NowPlayingMetadata(val id: String, val albumArtUri: Uri, val title: String?, val artist: String?, val duration: Long, val palette: Palette) {

        companion object {
            fun timestampToMSS(position: Long): String {
                val totalSeconds = floor(position / 1E3).toInt()
                val minutes = totalSeconds / 60
                val remainingSeconds = totalSeconds - (minutes * 60)
                return "%d:%02d".format(minutes, remainingSeconds)
            }
        }
    }

    private val _playbackState = MutableLiveData<PlaybackStateCompat>()
    private val _mediaMetadata = MutableLiveData<NowPlayingMetadata>()
    private val _mediaPosition = MutableLiveData<Long>()

    val playbackState: LiveData<PlaybackStateCompat> = _playbackState
    val mediaMetadata: LiveData<NowPlayingMetadata> = _mediaMetadata
    val mediaPosition: LiveData<Long> = _mediaPosition
    val queue: LiveData<List<MediaMetadataCompat>> = repository.queue
    val queuePosition: LiveData<Int> = repository.queuePosition
    val repeatMode: LiveData<Int> = serviceConnection.repeatMode

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        _playbackState.postValue(it ?: EMPTY_PLAYBACK_STATE)
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

    fun addQueueItem(item: MediaDescriptionCompat) = serviceConnection.addQueueItem(item)

    fun addNextQueueItem(item: MediaDescriptionCompat) = serviceConnection.addQueueItem(item, queuePosition.value!! + 1)

    fun moveQueueItem(fromPosition: Int, toPosition: Int) = serviceConnection.moveQueueItem(fromPosition, toPosition)

    fun removeQueueItem(position: Int) = serviceConnection.removeQueueItemAt(position)

    fun skipToQueueItem(position: Int) = serviceConnection.transportControls?.skipToQueueItem(position.toLong())

    fun toggleRepeatMode() =
        serviceConnection.transportControls?.setRepeatMode(if (serviceConnection.repeatMode.value == PlaybackStateCompat.REPEAT_MODE_ALL) PlaybackStateCompat.REPEAT_MODE_ONE else PlaybackStateCompat.REPEAT_MODE_ALL)

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
        CoroutineScope(Dispatchers.IO).launch {
            if (mediaMetadata.duration > 0 && mediaMetadata.id != null) {
                _mediaMetadata.postValue(NowPlayingMetadata(
                    mediaMetadata.id!!,
                    mediaMetadata.displayIconUri,
                    mediaMetadata.displayTitle?.trim(),
                    mediaMetadata.displaySubtitle?.trim(),
                    mediaMetadata.duration,
                    Palette.from(getBitmap(GlideApp.with(getApplication() as Context), mediaMetadata.displayIconUri)).addFilter { _, hsl ->
                        (hsl[2] > 0.6 && hsl[1] < 0.6) || hsl[2] < 0.4
                    }.generate())
                )
            }
        }
    }

    fun playPause() = serviceConnection.transportControls?.apply {
        when (_playbackState.value?.state) {
            PlaybackStateCompat.STATE_PLAYING,
            PlaybackStateCompat.STATE_BUFFERING,
            -> pause()
            else -> play()
        }
    }

    class Factory(private val app: Application, private val serviceConnection: ServiceConnection, private val repository: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = PlayingViewModel(app, serviceConnection, repository) as T
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