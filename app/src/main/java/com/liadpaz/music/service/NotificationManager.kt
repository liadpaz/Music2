package com.liadpaz.music.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil
import com.liadpaz.music.R
import com.liadpaz.music.utils.GlideApp
import com.liadpaz.music.utils.GlideRequests
import com.liadpaz.music.utils.getBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationManager(context: Context, private val player: ExoPlayer, sessionToken: MediaSessionCompat.Token, controlDispatcher: ControlDispatcher, notificationListener: PlayerNotificationManager.NotificationListener) {

    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)

        if (NotificationManagerCompat.from(context).getNotificationChannel(CHANNEL_ID) == null) {
            NotificationUtil.createNotificationChannel(context, CHANNEL_ID, R.string.notification_channel_name, R.string.notification_channel_description, NotificationUtil.IMPORTANCE_LOW)
        }
        notificationManager = MediaPlayerNotificationManager(context, mediaController, notificationListener, RepeatActionReceiver()).apply {
            setMediaSessionToken(sessionToken)
            setSmallIcon(R.drawable.ic_launcher_foreground)

            setUseChronometer(false)

            setControlDispatcher(controlDispatcher)
        }
    }

    fun showNotification() = notificationManager.setPlayer(player)

    fun hideNotification() = notificationManager.setPlayer(null)

    /**
     * This class is for a repeat (All or One) action in the notification.
     */
    private class RepeatActionReceiver : PlayerNotificationManager.CustomActionReceiver {
        override fun getCustomActions(player: Player): MutableList<String> =
            mutableListOf(if (player.repeatMode == Player.REPEAT_MODE_ALL) ACTION_REPEAT_MODE else ACTION_REPEAT_MODE_ONE)

        override fun createCustomActions(context: Context, instanceId: Int): MutableMap<String, NotificationCompat.Action> = mutableMapOf(
            ACTION_REPEAT_MODE to NotificationCompat.Action.Builder(R.drawable.ic_repeat, null, PendingIntent.getBroadcast(context, instanceId, Intent(ACTION_REPEAT_MODE_ONE), PendingIntent.FLAG_UPDATE_CURRENT)).build(),
            ACTION_REPEAT_MODE_ONE to NotificationCompat.Action.Builder(R.drawable.ic_repeat_one, null, PendingIntent.getBroadcast(context, instanceId, Intent(ACTION_REPEAT_MODE), PendingIntent.FLAG_UPDATE_CURRENT)).build())

        override fun onCustomAction(player: Player, action: String, intent: Intent) = when (action) {
            ACTION_REPEAT_MODE -> player.repeatMode = Player.REPEAT_MODE_ALL
            ACTION_REPEAT_MODE_ONE -> player.repeatMode = Player.REPEAT_MODE_ONE
            else -> throw IllegalArgumentException()
        }
    }

    private class MediaPlayerNotificationManager(context: Context, mediaController: MediaControllerCompat, notificationListener: NotificationListener, private val customActionReceiver: CustomActionReceiver) : PlayerNotificationManager(context, CHANNEL_ID, NOTIFICATION_ID, DescriptionAdapter(mediaController, GlideApp.with(context)), notificationListener, customActionReceiver) {
        override fun getActions(player: Player): MutableList<String> =
            mutableListOf(ACTION_PREVIOUS, if (player.isPlaying) ACTION_PAUSE else ACTION_PLAY, ACTION_NEXT, *customActionReceiver.getCustomActions(player).toTypedArray())

        override fun getActionIndicesForCompactView(actionNames: MutableList<String>, player: Player): IntArray =
            intArrayOf(actionNames.indexOf(ACTION_PREVIOUS), actionNames.indexOf(if (player.isPlaying) ACTION_PAUSE else ACTION_PLAY), actionNames.indexOf(ACTION_NEXT))

        private class DescriptionAdapter(private val controller: MediaControllerCompat, private val glide: GlideRequests) : MediaDescriptionAdapter {
            var currentIconUri: Uri? = null
            var currentBitmap: Bitmap? = null

            override fun createCurrentContentIntent(player: Player): PendingIntent? = controller.sessionActivity

            override fun getCurrentContentText(player: Player): CharSequence? = controller.metadata.description.subtitle.toString()

            override fun getCurrentContentTitle(player: Player): CharSequence = controller.metadata.description.title.toString()

            override fun getCurrentSubText(player: Player): CharSequence? = controller.metadata.description.description.toString()

            override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                val iconUri = controller.metadata.description.iconUri
                return if (currentIconUri != iconUri || currentBitmap == null) {
                    currentIconUri = iconUri
                    CoroutineScope(Dispatchers.IO).launch {
                        iconUri?.let { uri ->
                            callback.onBitmap(getBitmap(glide, uri, errorDrawable = R.drawable.ic_album_color).also { currentBitmap = it })
                        }
                    }
                    currentBitmap
                } else {
                    currentBitmap
                }
            }
        }
    }
}

const val CHANNEL_ID = "com.liadpaz.music.service.NOTIFICATION_CHANNEL"
const val NOTIFICATION_ID = 123

private const val ACTION_REPEAT_MODE = "repeat_mode"
private const val ACTION_REPEAT_MODE_ONE = "repeat_mode_one"

private const val TAG = "NotificationManager"