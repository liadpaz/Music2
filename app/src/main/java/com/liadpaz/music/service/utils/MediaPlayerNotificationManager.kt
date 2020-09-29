package com.liadpaz.music.service.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.media.session.MediaControllerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.liadpaz.music.R
import com.liadpaz.music.utils.GlideApp
import com.liadpaz.music.utils.GlideRequests
import java.util.concurrent.Executors

class MediaPlayerNotificationManager(private val context: Context, private val mediaController: MediaControllerCompat, private val controlDispatcher: ControlDispatcher, private val notificationListener: PlayerNotificationManager.NotificationListener) {
	private val instanceId = instanceCounter++
	private val handler = Handler(Looper.getMainLooper(), this::handleMessage)
	private val actions = mapOf<String, NotificationCompat.Action>(
		ACTION_PLAY to NotificationCompat.Action.Builder(R.drawable.ic_notification_play, context.getString(R.string.exo_controls_play_description), createBroadcastIntent(ACTION_PLAY, context, instanceId)).build(),
		ACTION_PAUSE to NotificationCompat.Action.Builder(R.drawable.exo_notification_pause, context.getString(R.string.exo_controls_pause_description), createBroadcastIntent(ACTION_PAUSE, context, instanceId)).build(),
		ACTION_NEXT to NotificationCompat.Action.Builder(R.drawable.ic_skip_next, context.getString(R.string.exo_controls_next_description), createBroadcastIntent(ACTION_NEXT, context, instanceId)).build(),
		ACTION_PREVIOUS to NotificationCompat.Action.Builder(R.drawable.ic_skip_previous, context.getString(R.string.exo_controls_previous_description), createBroadcastIntent(ACTION_PREVIOUS, context, instanceId)).build(),
		ACTION_REPEAT_MODE_ALL to NotificationCompat.Action.Builder(R.drawable.ic_repeat_one, null, createBroadcastIntent(ACTION_REPEAT_MODE_ALL, context, instanceId)).build(),
		ACTION_REPEAT_MODE_ONE to NotificationCompat.Action.Builder(R.drawable.ic_repeat, null, createBroadcastIntent(ACTION_REPEAT_MODE_ONE, context, instanceId)).build(),
	)
	private val intentFilter = IntentFilter().apply {
		for (action in actions) {
			addAction(action.key)
		}
	}
	private val notificationManager = NotificationManagerCompat.from(context)
	private val playerListener: Player.EventListener = object : Player.EventListener {
		override fun onPlaybackStateChanged(state: Int) = postStartOrUpdateNotification()
		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = postStartOrUpdateNotification()
		override fun onIsPlayingChanged(isPlaying: Boolean) = postStartOrUpdateNotification()
		override fun onTimelineChanged(timeline: Timeline, reason: Int) = postStartOrUpdateNotification()
		override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = postStartOrUpdateNotification()
		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = postStartOrUpdateNotification()
		override fun onPositionDiscontinuity(reason: Int) = postStartOrUpdateNotification()
		override fun onRepeatModeChanged(repeatMode: Int) = postStartOrUpdateNotification()
	}
	private val notificationBroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent) {
			if (player == null || !isNotificationStarted || intent.getIntExtra(EXTRA_INSTANCE_ID, instanceId) != instanceId) return
			when (intent.action) {
				ACTION_PLAY -> controlDispatcher.dispatchSetPlayWhenReady(player!!, true)
				ACTION_PAUSE -> controlDispatcher.dispatchSetPlayWhenReady(player!!, false)
				ACTION_PREVIOUS -> controlDispatcher.dispatchPrevious(player!!)
				ACTION_NEXT -> controlDispatcher.dispatchNext(player!!)
				ACTION_REPEAT_MODE_ALL -> controlDispatcher.dispatchSetRepeatMode(player!!, Player.REPEAT_MODE_ALL)
				ACTION_REPEAT_MODE_ONE -> controlDispatcher.dispatchSetRepeatMode(player!!, Player.REPEAT_MODE_ONE)
			}
		}
	}
	private val mediaDescriptionProvider = DescriptionProvider(mediaController, GlideApp.with(context))

	private var builder: NotificationCompat.Builder? = null
	private var builderActions: List<NotificationCompat.Action>? = null
	private var player: Player? = null
	private var isNotificationStarted = false

	init {
		notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
			description = context.getString(R.string.notification_channel_description)
		})
	}

	private fun getActions(player: Player): MutableList<String> =
		mutableListOf(if (player.repeatMode == Player.REPEAT_MODE_ALL) ACTION_REPEAT_MODE_ONE else ACTION_REPEAT_MODE_ALL, ACTION_PREVIOUS, if (player.isPlaying) ACTION_PAUSE else ACTION_PLAY, ACTION_NEXT)

	private fun startOrUpdateNotification(player: Player, bitmap: Bitmap?) {
		val onGoing = getOnGoing(player)
		builder = createNotification(player, builder, onGoing, bitmap) ?: let {
			stopNotification()
			return
		}
		val notification = builder!!.build()
		notificationManager.notify(NOTIFICATION_ID, notification)
		if (!isNotificationStarted) {
			isNotificationStarted = true
			context.registerReceiver(notificationBroadcastReceiver, intentFilter)
		}
		notificationListener.onNotificationPosted(NOTIFICATION_ID, notification, onGoing)
	}

	private fun stopNotification() {
		if (isNotificationStarted) {
			isNotificationStarted = false
			handler.removeMessages(MSG_START_OR_UPDATE_NOTIFICATION)
			handler.removeMessages(MSG_UPDATE_NOTIFICATION_BITMAP)
			notificationManager.cancel(NOTIFICATION_ID)
			context.unregisterReceiver(notificationBroadcastReceiver)
			notificationListener.onNotificationCancelled(NOTIFICATION_ID, false)
		}
	}

	private fun createNotification(player: Player, builder: NotificationCompat.Builder?, ongoing: Boolean, largeIcon: Bitmap?): NotificationCompat.Builder? {
		if (player.playbackState == Player.STATE_IDLE || player.currentTimeline.isEmpty) {
			builderActions = null
			return null
		}

		if (builder == null || actions != builderActions) {
			this.builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
				for (action in getActions(player).map { actions.getValue(it) }) {
					addAction(action)
				}
			}
		}

		return this.builder?.setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaController.sessionToken).setShowActionsInCompactView(1, 2, 3))
			?.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
			?.setOngoing(ongoing)
			?.setSmallIcon(R.drawable.ic_music_note)
			?.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			?.setShowWhen(false)

			// set media specific properties
			?.setContentTitle(mediaDescriptionProvider.currentContentTitle)
			?.setContentText(mediaDescriptionProvider.currentContentText)
			?.setSubText(mediaDescriptionProvider.currentSubText)
			?.setLargeIcon(largeIcon ?: mediaDescriptionProvider.getCurrentLargeIcon(::postUpdateNotificationBitmap))
			?.setContentIntent(mediaController.sessionActivity)
	}

	fun setPlayer(player: Player?) {
		if (player == this.player) return
		if (this.player != null) {
			this.player!!.removeListener(playerListener)
			if (player == null) {
				stopNotification()
			}
		}
		this.player = player
		if (player != null) {
			player.addListener(playerListener)
			postStartOrUpdateNotification()
		}
	}

	private fun postStartOrUpdateNotification() {
		handler.removeMessages(MSG_START_OR_UPDATE_NOTIFICATION)
		handler.removeMessages(MSG_UPDATE_NOTIFICATION_BITMAP)
		handler.sendEmptyMessage(MSG_START_OR_UPDATE_NOTIFICATION)
	}

	private fun postUpdateNotificationBitmap(bitmap: Bitmap?) =
		handler.obtainMessage(MSG_UPDATE_NOTIFICATION_BITMAP, -1, -1, bitmap).sendToTarget()

	private fun handleMessage(message: Message): Boolean = when (message.what) {
		MSG_START_OR_UPDATE_NOTIFICATION -> {
			if (player != null) {
				startOrUpdateNotification(player!!, null)
			}
			true
		}
		MSG_UPDATE_NOTIFICATION_BITMAP -> {
			if (player != null && isNotificationStarted) {
				startOrUpdateNotification(player!!, message.obj as? Bitmap)
			}
			true
		}
		else -> false
	}

	private fun getOnGoing(player: Player) =
		(player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY) && player.playWhenReady

	private fun createBroadcastIntent(action: String, context: Context, instanceId: Int) =
		PendingIntent.getBroadcast(context, instanceId, Intent(action).setPackage(context.packageName).putExtra(EXTRA_INSTANCE_ID, instanceId), PendingIntent.FLAG_UPDATE_CURRENT)

	/**
	 * This class is providing the media description to the notification.
	 */
	private class DescriptionProvider(private val controller: MediaControllerCompat, private val glide: GlideRequests) {
		private val executor = Executors.newSingleThreadExecutor()

		@Volatile
		var currentIconUri: Uri? = null

		@Volatile
		var currentBitmap: Bitmap? = null

		val currentContentText: CharSequence
			get() = controller.metadata.description.subtitle.toString()

		val currentContentTitle: CharSequence
			get() = controller.metadata.description.title.toString()

		val currentSubText: CharSequence
			get() = controller.metadata.description.description.toString()

		fun getCurrentLargeIcon(callback: (Bitmap) -> Unit): Bitmap? {
			val iconUri = controller.metadata.description.iconUri
			return if (currentIconUri != iconUri || currentBitmap == null) {
				currentIconUri = iconUri
				iconUri?.let { uri ->
					executor.execute {
						currentBitmap = try {
							glide.asBitmap().load(uri).submit().get()
						} catch (e: Exception) {
							glide.asBitmap().load(R.drawable.ic_album_color).submit().get()
						}
						callback(currentBitmap!!)
					}
				}
				currentBitmap
			} else {
				currentBitmap
			}
		}
	}

	companion object {
		/**
		 * This action is to play media.
		 */
		private const val ACTION_PLAY = "com.liadpaz.music.play"

		/**
		 * This action is to pause media.
		 */
		private const val ACTION_PAUSE = "com.liadpaz.music.pause"

		/**
		 * This action is to skip to previous media item.
		 */
		private const val ACTION_PREVIOUS = "com.liadpaz.music.prev"

		/**
		 * This action is to skip to next media item.
		 */
		private const val ACTION_NEXT = "com.liadpaz.music.next"

		/**
		 * This action is to repeat all media items (icon is repeat one).
		 */
		private const val ACTION_REPEAT_MODE_ALL = "com.liadpaz.music.repeat_mode_all"

		/**
		 * This action is to repeat one media item (icon is repeat all).
		 */
		private const val ACTION_REPEAT_MODE_ONE = "com.liadpaz.music.repeat_mode_one"
		private const val EXTRA_INSTANCE_ID = "INSTANCE_ID"

		private const val CHANNEL_ID = "com.liadpaz.music.service.NOTIFICATION_CHANNEL"
		private const val NOTIFICATION_ID = 123

		private const val MSG_START_OR_UPDATE_NOTIFICATION = 0
		private const val MSG_UPDATE_NOTIFICATION_BITMAP = 1

		private var instanceCounter = 0
	}
}