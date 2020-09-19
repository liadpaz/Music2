package com.liadpaz.music.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.ContentDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.utils.BrowseTree
import com.liadpaz.music.service.utils.FileMusicSource
import com.liadpaz.music.service.utils.QUEUE_ROOT
import com.liadpaz.music.service.utils.ROOT
import com.liadpaz.music.utils.extensions.flag

class MusicService : MediaBrowserServiceCompat() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var musicSource: FileMusicSource

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private val browseTree: BrowseTree by lazy {
        BrowseTree(musicSource) { what ->
            notifyChildrenChanged(what)
        }
    }

//    private val glide by lazy {
//        GlideApp.with(this)
//    }
    // TODO: use glide to load art for session metadata

    private var isForegroundService: Boolean = false

    private val mAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerListener()

    private val exoPlayer: SimpleExoPlayer by lazy {
        SimpleExoPlayer.Builder(applicationContext, AudioOnlyRenderersFactory(this)).build().apply {
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
        }
    }

    private val repository by lazy {
        Repository.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()

        repository.granted.observeForever(permissionGrantedObserver)

        musicSource = FileMusicSource(this, repository) { position ->
            mediaSession.controller.transportControls.playFromMediaId(QUEUE_ROOT, bundleOf(EXTRA_POSITION to position))
            mediaSession.controller.transportControls.pause()
        }

        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent.putExtra(EXTRA_TYPE, this::class.java.canonicalName), PendingIntent.FLAG_UPDATE_CURRENT)
            }

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setSessionActivity(sessionActivityPendingIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        notificationManager =
            NotificationManager(applicationContext, exoPlayer, mediaSession.sessionToken, PlayerNotificationListener())

        mediaSessionConnector = MediaSessionConnector(mediaSession).also { connector ->
            val dataSourceFactory = DataSource.Factory { ContentDataSource(this) }

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

        browseTree.release()

        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? =
        BrowserRoot(ROOT, bundleOf("android.media.browse.SEARCH_SUPPORTED" to true, "android.media.browse.CONTENT_STYLE_SUPPORTED" to true))

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(browseTree[parentId]?.map { item -> MediaBrowserCompat.MediaItem(item.description, item.flag) })
    }

    override fun onSearch(query: String, extras: Bundle?, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(browseTree.search(query, extras).map { item -> MediaBrowserCompat.MediaItem(item.description, item.flag) })
    }

    private inner class PlayerNotificationListener : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
            if (ongoing && !isForegroundService) {
                startForegroundService(Intent(applicationContext, this@MusicService.javaClass))
                startForeground(notificationId, notification)
                Log.d(TAG, "Foreground: true")
                isForegroundService = true
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            Log.d(TAG, "Foreground: false, notification: off")
            isForegroundService = false
            stopSelf()
        }
    }

    private inner class PlayerListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY,
                -> {
                    notificationManager.showNotification()

                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) {
                            stopForeground(false)
                            Log.d(TAG, "Foreground: false, notification: on")
                        }
                    }
                }
                else -> {
                    notificationManager.hideNotification()
                }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, @Player.TimelineChangeReason reason: Int) {
            exoPlayer.playWhenReady = true
            val window = Timeline.Window()
            repository.setQueuePosition(exoPlayer.currentWindowIndex)
            repository.setQueue(timeline.let {
                val queue = arrayListOf<MediaSessionCompat.QueueItem>()
                for (i in 0 until it.windowCount) {
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
            exoPlayer.retry()
        }
    }

    class QueueEditor(private val playbackPreparer: PlaybackPreparer) : MediaSessionConnector.QueueEditor {
        override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean =
            when (command) {
                ACTION_REMOVE_ITEM -> {
                    playbackPreparer.removeQueueItem(extras!!.getInt(EXTRA_POSITION))
                    true
                }
                ACTION_MOVE_ITEM -> {
                    playbackPreparer.moveQueueItem(extras!!.getInt(EXTRA_FROM_POSITION), extras.getInt(EXTRA_TO_POSITION))
                    true
                }
                else -> false
            }

        override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat) =
            playbackPreparer.addQueueItem(description)

        override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat, index: Int) =
            playbackPreparer.addQueueItem(description, index)

        /**
         * This function is unsupported, needs to user {@code ACTION_REMOVE_ITEM} and to use positional removal
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
     * This class is for navigating the queue
     */
    private class QueueNavigator(mediaSession: MediaSessionCompat) : TimelineQueueNavigator(mediaSession) {
        private val window = Timeline.Window()

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
            player.currentTimeline.getWindow(windowIndex, window).tag as MediaDescriptionCompat

        // TODO: get bitmap asynchronously and notify when retrieved
    }

    /**
     * This class is for the [ExoPlayer], it is a [RenderersFactory] for audio only, as this app requires only audio.
     * It is intended to save apk size.
     */
    private class AudioOnlyRenderersFactory(private val context: Context) : RenderersFactory {
        override fun createRenderers(eventHandler: Handler, videoRendererEventListener: VideoRendererEventListener, audioRendererEventListener: AudioRendererEventListener, textRendererOutput: TextOutput, metadataRendererOutput: MetadataOutput, drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?): Array<Renderer> =
            arrayOf(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, eventHandler, audioRendererEventListener))
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
    }
}

private const val TAG = "MusicService"