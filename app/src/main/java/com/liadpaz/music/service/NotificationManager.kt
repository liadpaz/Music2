package com.liadpaz.music.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil
import com.liadpaz.music.R
import com.liadpaz.music.utils.GlideApp
import com.liadpaz.music.utils.getBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationManager(context: Context, private val exoPlayer: ExoPlayer, sessionToken: MediaSessionCompat.Token, notificationListener: PlayerNotificationManager.NotificationListener) {

    private val notificationManager: PlayerNotificationManager
    private val glide = GlideApp.with(context)

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)

        NotificationUtil.createNotificationChannel(context, CHANNEL_ID, R.string.notification_channel_name, R.string.notification_channel_description, NotificationUtil.IMPORTANCE_MIN)
        notificationManager =
            PlayerNotificationManager(context, CHANNEL_ID, NOTIFICATION_ID, DescriptionAdapter(mediaController), notificationListener, RepeatActionReceiver()).apply {
                setMediaSessionToken(sessionToken)
                setSmallIcon(R.drawable.ic_launcher_foreground)

                setUseChronometer(false)
                setUseNavigationActionsInCompactView(true)

                setRewindIncrementMs(0)
                setFastForwardIncrementMs(0)
            }
    }

    fun showNotification() = notificationManager.setPlayer(exoPlayer)

    fun hideNotification() = notificationManager.setPlayer(null)

    private class RepeatActionReceiver : PlayerNotificationManager.CustomActionReceiver {
        override fun getCustomActions(player: Player): MutableList<String> =
            mutableListOf(if (player.repeatMode == Player.REPEAT_MODE_ALL) ACTION_REPEAT_MODE else ACTION_REPEAT_MODE_ONE)

        override fun createCustomActions(context: Context, instanceId: Int): MutableMap<String, NotificationCompat.Action> =
            mutableMapOf(
                ACTION_REPEAT_MODE to NotificationCompat.Action.Builder(R.drawable.ic_repeat, null, PendingIntent.getBroadcast(context, instanceId, Intent(ACTION_REPEAT_MODE_ONE), PendingIntent.FLAG_UPDATE_CURRENT)).build(),
                ACTION_REPEAT_MODE_ONE to NotificationCompat.Action.Builder(R.drawable.ic_repeat_one, null, PendingIntent.getBroadcast(context, instanceId, Intent(ACTION_REPEAT_MODE), PendingIntent.FLAG_UPDATE_CURRENT)).build()
            )


        override fun onCustomAction(player: Player, action: String, intent: Intent) =
            when (action) {
                ACTION_REPEAT_MODE -> player.repeatMode = Player.REPEAT_MODE_ALL
                ACTION_REPEAT_MODE_ONE -> player.repeatMode = Player.REPEAT_MODE_ONE
                else -> throw IllegalArgumentException()
            }

    }

    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) : PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player): CharSequence? =
            controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player): CharSequence =
            controller.metadata.description.title.toString()

        override fun getCurrentSubText(player: Player): CharSequence? =
            controller.metadata.description.description.toString()

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

const val CHANNEL_ID = "com.liadpaz.music.service.NOTIFICATION_CHANNEL"
const val NOTIFICATION_ID = 123

private const val ACTION_REPEAT_MODE = "repeat_mode"
private const val ACTION_REPEAT_MODE_ONE = "repeat_mode_one"

private const val TAG = "NotificationManager"