package com.oneui.sms.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.oneui.sms.MainActivity
import com.oneui.sms.R

object MessageNotification {
    const val CHANNEL = "messages"
    const val GROUP = "onemessages_sms"
    const val REPLY_KEY = "reply_text"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Incoming SMS notifications"
            })
        }
    }

    fun show(context: Context, address: String, body: String, threadId: Long, muted: Boolean = false, withReply: Boolean = true, withSound: Boolean = true, withVibration: Boolean = true) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("openThreadId", threadId)
            putExtra("openAddress", address)
        }
        val open = PendingIntent.getActivity(context, threadId.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val reply = RemoteInput.Builder(REPLY_KEY).setLabel("Reply").build()
        val replyIntent = PendingIntent.getBroadcast(
            context, (threadId.hashCode() xor 0x44),
            Intent(context, NotificationReplyReceiver::class.java).putExtra("address", address).putExtra("threadId", threadId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val replyAction = NotificationCompat.Action.Builder(0, "Reply", replyIntent).addRemoteInput(reply).build()
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(address)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setGroup(GROUP)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .also { if (withReply) it.addAction(replyAction) }
        if (muted || !withSound) builder.setSilent(true)
        if (!withVibration) builder.setVibrate(longArrayOf(0L))
        context.getSystemService(NotificationManager::class.java).notify(threadId.hashCode(), builder.build())
    }
}

class NotificationReplyReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra("address") ?: return
        val threadId = intent.getLongExtra("threadId", -1L)
        val reply = androidx.core.app.RemoteInput.getResultsFromIntent(intent)?.getCharSequence(MessageNotification.REPLY_KEY)?.toString()?.trim() ?: return
        if (threadId < 0 || reply.isEmpty()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.oneui.sms.data.SmsRepository(context.applicationContext).sendMessage(threadId, address, reply)
        }
    }
}
