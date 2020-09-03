package com.liadpaz.music.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.liadpaz.music.R
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.utils.BrowseTree
import com.liadpaz.music.service.utils.FileMusicSource
import com.liadpaz.music.service.utils.MusicSource
import com.liadpaz.music.service.utils.ROOT
import com.liadpaz.music.utils.extensions.flag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MusicService : MediaBrowserServiceCompat() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSource: MusicSource

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private val browseTree: BrowseTree by lazy {
        BrowseTree(mediaSource)
    }

    private var isForegroundService: Boolean = false

    private val mAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerListener()

    private val exoPlayer by lazy {
        SimpleExoPlayer.Builder(applicationContext).build().apply {
            setAudioAttributes(mAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
        }
    }

    private var permissionGranted = false
    private val permissionGrantedObserver = Observer<Boolean> {
        if (it && permissionGranted != it) {
            permissionGranted = it
//            serviceScope.launch {
//                mediaSource.load()
//            }
        }
    }

    private val repository by lazy {
        Repository.getInstance(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate: ")

        repository.granted.observeForever(permissionGrantedObserver)

        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setSessionActivity(sessionActivityPendingIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        notificationManager =
            NotificationManager(applicationContext, exoPlayer, mediaSession.sessionToken, PlayerNotificationListener())

        mediaSource = FileMusicSource(applicationContext)
        serviceScope.launch {
            mediaSource.load()
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).also { connector ->
            val dataSourceFactory =
                DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)))

            val playbackPreparer = PlaybackPreparer(browseTree, exoPlayer, dataSourceFactory)
            connector.setPlayer(exoPlayer)
            connector.setPlaybackPreparer(playbackPreparer)
            connector.setQueueNavigator(QueueNavigator(mediaSession))
            connector.setQueueEditor(QueueEditor(playbackPreparer))
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // TODO: add check if user wants to stop playback
        /*if (toStop) {*/
        exoPlayer.stop(true)
        /*}*/
    }

    override fun onDestroy() {
        mediaSession.run {
            isActive = false
            release()
        }

        repository.granted.removeObserver(permissionGrantedObserver)

        Log.d(TAG, "onDestroy: ")

        serviceJob.cancel()

        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? =
        BrowserRoot(ROOT, bundleOf(Pair("android.media.browse.SEARCH_SUPPORTED", true), Pair("android.media.browse.CONTENT_STYLE_SUPPORTED", true)))

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        val resultSent = mediaSource.whenReady {
            result.sendResult(browseTree[parentId]?.map { item -> MediaBrowserCompat.MediaItem(item.description, item.flag) })
        }

        if (!resultSent) {
            result.detach()
        }
    }

    override fun onSearch(query: String, extras: Bundle?, result: Result<MutableList<MediaBrowserCompat.MediaItem>>): Nothing {
        TODO("implement search option")
    }

    private inner class PlayerNotificationListener : PlayerNotificationManager.NotificationListener {

        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, this@MusicService.javaClass))

                startForeground(notificationId, notification)
                isForegroundService = true
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    private inner class PlayerListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    notificationManager.showNotification()

                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) stopForeground(false)
                    }
                }
                else -> {
                    notificationManager.hideNotification()
                }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, @Player.TimelineChangeReason reason: Int) {
            exoPlayer.playWhenReady = true
            repository.setQueuePosition(exoPlayer.currentWindowIndex)
            repository.setQueue(timeline.let {
                val queue = arrayListOf<MediaSessionCompat.QueueItem>()
                for (i in 0 until it.windowCount) {
                    val window = Timeline.Window()
                    queue.add(MediaSessionCompat.QueueItem(it.getWindow(i, window).tag as MediaDescriptionCompat, i.toLong()))
                }
                queue
            })
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            repository.setQueuePosition(exoPlayer.currentWindowIndex)
            if (reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                exoPlayer.playWhenReady = true
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            var message = "Playback error"
            when (error.type) {
                // If the data from MediaSource object could not be loaded the Exoplayer raises
                // a type_source error.
                // An error message is printed to UI via Toast message to inform the user.
                ExoPlaybackException.TYPE_SOURCE -> {
                    message = "Media not found"
                    Log.e(TAG, "TYPE_SOURCE: " + error.sourceException.message)
                }
                // If the error occurs in a render component, Exoplayer raises a type_remote error.
                ExoPlaybackException.TYPE_RENDERER -> {
                    Log.e(TAG, "TYPE_RENDERER: " + error.rendererException.message)
                }
                // If occurs an unexpected RuntimeException Exoplayer raises a type_unexpected error.
                ExoPlaybackException.TYPE_UNEXPECTED -> {
                    Log.e(TAG, "TYPE_UNEXPECTED: " + error.unexpectedException.message)
                }
                // Occurs when there is a OutOfMemory error.
                ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
                    Log.e(TAG, "TYPE_OUT_OF_MEMORY: " + error.outOfMemoryError.message)
                }
                // If the error occurs in a remote component, Exoplayer raises a type_remote error.
                ExoPlaybackException.TYPE_REMOTE -> {
                    Log.e(TAG, "TYPE_REMOTE: " + error.message)
                }
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    class QueueEditor(private val playbackPreparer: PlaybackPreparer) : MediaSessionConnector.QueueEditor {
        override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean =
            when (command) {
                ACTION_REMOVE_ITEM -> {
                    extras?.let { playbackPreparer.removeQueueItem(it.getInt(EXTRA_QUEUE_POSITION)) }
                    true
                }
                ACTION_MOVE_ITEM -> {
                    extras?.let { playbackPreparer.moveQueueItem(it.getInt(EXTRA_FROM_POSITION), it.getInt(EXTRA_TO_POSITION)) }
                    true
                }
                else -> false
            }

        override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat) =
            playbackPreparer.addQueueItem(description)

        override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat, index: Int) =
            playbackPreparer.addQueueItem(description, index)

        override fun onRemoveQueueItem(player: Player, description: MediaDescriptionCompat): Nothing =
            throw UnsupportedOperationException()

        companion object {
            const val ACTION_REMOVE_ITEM = "action_remove_item"
            const val ACTION_MOVE_ITEM = "action_move_item"
            const val EXTRA_QUEUE_POSITION = "extra_queue_position"
            const val EXTRA_FROM_POSITION = "extra_from_position"
            const val EXTRA_TO_POSITION = "extra_to_position"
        }
    }

    private class QueueNavigator(mediaSession: MediaSessionCompat) : TimelineQueueNavigator(mediaSession) {

        private val window = Timeline.Window()

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
            player.currentTimeline.getWindow(windowIndex, window).tag as MediaDescriptionCompat
    }
}

private const val TAG = "MusicService"