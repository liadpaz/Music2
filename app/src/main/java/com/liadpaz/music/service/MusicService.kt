package com.liadpaz.music.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Clock
import com.liadpaz.music.repository.PreferencesRepository
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.utils.*
import com.liadpaz.music.service.utils.ControlDispatcher
import com.liadpaz.music.utils.GlideApp
import com.liadpaz.music.utils.extensions.flag
import com.liadpaz.music.utils.extensions.id
import com.liadpaz.music.utils.extensions.mediaUri
import com.liadpaz.music.utils.extensions.toMediaMetadata

class MusicService : MediaBrowserServiceCompat() {

	private lateinit var notificationManager: MediaPlayerNotificationManager
	private lateinit var musicSource: FileMusicSource

	private lateinit var mediaSession: MediaSessionCompat

	private val browseTree: BrowseTree by lazy {
		BrowseTree(musicSource) { what ->
			notifyChildrenChanged(what)
		}
	}

	private val glide by lazy { GlideApp.with(this) }

	private var isForegroundService: Boolean = false

	private val playerListener = PlayerListener()

	private val player: SimpleExoPlayer by lazy {
		SimpleExoPlayer.Builder(applicationContext, { eventHandler, _, audioRendererEventListener, _, _ ->
			arrayOf(MediaCodecAudioRenderer(this, MediaCodecSelector.DEFAULT, eventHandler, audioRendererEventListener))
		}, DefaultTrackSelector(DefaultTrackSelector.Parameters.DEFAULT_WITHOUT_CONTEXT, AdaptiveTrackSelection.Factory()), AudioMediaSourceFactory(applicationContext), DefaultLoadControl(), DefaultBandwidthMeter.getSingletonInstance(this), AnalyticsCollector(Clock.DEFAULT)).build().apply {
			setAudioAttributes(AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true)
			setHandleAudioBecomingNoisy(true)
			addListener(playerListener)
			repeatMode = ExoPlayer.REPEAT_MODE_ALL
		}
	}

	private val repository by lazy { Repository.getInstance(this) }
	private val preferencesRepository by lazy { PreferencesRepository.getInstance(this) }

	override fun onCreate() {
		super.onCreate()
		musicSource = FileMusicSource(this) { queue, queuePosition, mediaPosition ->
			Log.d(TAG, "onCreate: ${queue.map { it.id }} $queuePosition")
			if (queuePosition != -1) {
				player.setMediaItems(queue.map { MediaItem.Builder().setUri(it.mediaUri).setMediaId(it.id).setTag(it.description).build() }, queuePosition, mediaPosition)
				player.prepare()
				player.pause()
			}
		}

		val sessionActivityPendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
			PendingIntent.getActivity(this, 0, sessionIntent.putExtra(EXTRA_TYPE, this::class.java.canonicalName), PendingIntent.FLAG_UPDATE_CURRENT)
		}

		mediaSession = MediaSessionCompat(this, "MusicService").apply {
			setSessionActivity(sessionActivityPendingIntent)
			isActive = true
		}

		sessionToken = mediaSession.sessionToken

		val controlDispatcher = ControlDispatcher()
		notificationManager = MediaPlayerNotificationManager(applicationContext, mediaSession.controller, controlDispatcher, PlayerNotificationListener())

		MediaSessionConnector(mediaSession).also {
			it.setPlaybackPreparer(PlaybackPreparer(browseTree, player))
			it.setEnabledPlaybackActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SET_REPEAT_MODE)
			it.setControlDispatcher(controlDispatcher)
			it.setMediaMetadataProvider(MetadataProvider(it, glide))
			it.setQueueEditor(QueueEditor())
			it.setQueueNavigator(QueueNavigator(it.mediaSession))
			it.setPlayer(player)
		}
	}

	override fun onTaskRemoved(rootIntent: Intent?) {
		if (preferencesRepository.stopTask.value == true) {
			player.stop(true)
		}
	}

	override fun onDestroy() {
		mediaSession.run {
			isActive = false
			release()
		}

		Log.d(TAG, "onDestroy: ")

		musicSource.setMediaPosition(player.currentPosition)
		browseTree.release()
		player.removeListener(playerListener)
		player.release()
	}

	override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? =
		BrowserRoot(ROOT, bundleOf("android.media.browse.SEARCH_SUPPORTED" to true, "android.media.browse.CONTENT_STYLE_SUPPORTED" to true))

	override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
		result.sendResult(browseTree[parentId]?.map { item -> MediaBrowserCompat.MediaItem(item.description, item.flag) })
	}

	override fun onSearch(query: String, extras: Bundle?, result: Result<List<MediaBrowserCompat.MediaItem>>) {
		result.sendResult(browseTree.search(query, extras).map { item -> MediaBrowserCompat.MediaItem(item.description, item.flag) })
	}

	/**
	 * This class is listening to [PlayerNotificationManager] notification events, and it sets the service as a foreground service if needed to keep
	 * the service alive while the app is background.
	 */
	private inner class PlayerNotificationListener : PlayerNotificationManager.NotificationListener {
		override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
			if (ongoing && !isForegroundService) {
				ContextCompat.startForegroundService(this@MusicService, Intent(applicationContext, this@MusicService.javaClass))
				startForeground(notificationId, notification)
				isForegroundService = true
			}
		}

		override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
			stopForegroundInternal(true)
			stopSelf()
		}
	}

	/**
	 * This class listens to [ExoPlayer] events such as [onPlaybackStateChanged], [onPlayWhenReadyChanged], [onMediaItemTransition],
	 * [onTimelineChanged] and [onPlayerError].
	 */
	private inner class PlayerListener : Player.EventListener {
		override fun onPlaybackStateChanged(@Player.State state: Int) = onPlayerPlaybackStateChange(state, player.playWhenReady)

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, @Player.PlayWhenReadyChangeReason reason: Int) =
			onPlayerPlaybackStateChange(player.playbackState, playWhenReady)

		override fun onMediaItemTransition(mediaItem: MediaItem?, @Player.MediaItemTransitionReason reason: Int) {
			repository.setQueuePosition(player.currentWindowIndex)
			player.play()
		}

		override fun onTimelineChanged(timeline: Timeline, @Player.TimelineChangeReason reason: Int) {
			if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
				repository.setQueuePosition(player.currentWindowIndex)
				repository.setQueue(timeline.let {
					val queue = arrayListOf<MediaMetadataCompat>()
					for (i in 0 until it.windowCount) {
						queue.add((player.getMediaItemAt(i).playbackProperties?.tag as MediaDescriptionCompat).toMediaMetadata())
					}
					queue
				})
			}
		}

		override fun onPlayerError(error: ExoPlaybackException) {
			var message = "Playback error"
			when (error.type) {
				ExoPlaybackException.TYPE_SOURCE -> {
					message = "Media not found"
					Log.e(TAG, "TYPE_SOURCE: " + error.sourceException.message)
				}
				ExoPlaybackException.TYPE_RENDERER -> Log.e(TAG, "TYPE_RENDERER: " + error.rendererException.message)
				ExoPlaybackException.TYPE_UNEXPECTED -> Log.e(TAG, "TYPE_UNEXPECTED: " + error.unexpectedException.message)
				ExoPlaybackException.TYPE_OUT_OF_MEMORY -> Log.e(TAG, "TYPE_OUT_OF_MEMORY: " + error.outOfMemoryError.message)
				ExoPlaybackException.TYPE_REMOTE -> Log.e(TAG, "TYPE_REMOTE: " + error.message)
				ExoPlaybackException.TYPE_TIMEOUT -> Log.e(TAG, "TYPE_TIMEOUT: " + error.timeoutException.message)
			}
			Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
			player.prepare()
		}
	}

	private fun onPlayerPlaybackStateChange(@Player.State state: Int, playWhenReady: Boolean) {
		when (state) {
			Player.STATE_BUFFERING,
			Player.STATE_READY,
			-> {
				notificationManager.setPlayer(player)
				if (state == Player.STATE_READY) {
					if (!playWhenReady) {
						stopForegroundInternal(false)
					}
				}
			}
			else -> notificationManager.setPlayer(null)
		}
	}

	private fun stopForegroundInternal(removeNotification: Boolean) {
		stopForeground(removeNotification)
		isForegroundService = false
	}

	companion object {
		const val EXTRA_TYPE = "extra_type"
	}
}

private const val TAG = "MusicService"