package com.oneui.sms.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object NotificationChannels {
    const val REMINDERS = "reminders"
    const val DEFAULT_MESSAGES = "messages_default"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(DEFAULT_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    /**
     * #6 — per-contact notification sound. One channel per thread, since Android
     * only lets the user override sound at the channel level from Android 8+.
     */
    fun ensureConversationChannel(context: Context, threadId: Long, soundUri: String?): String {
        val channelId = "thread_$threadId"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
            val existing = nm?.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = NotificationChannel(channelId, "Conversation", NotificationManager.IMPORTANCE_HIGH)
                if (soundUri != null) {
                    channel.setSound(
                        android.net.Uri.parse(soundUri),
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                            .build(),
                    )
                }
                nm?.createNotificationChannel(channel)
            }
            // Note: once a channel is created, its sound can't be changed in code —
            // only the user can change it via system settings. If soundUri changes,
            // deep-link the user to the channel's settings screen instead.
        }
        return channelId
    }
}

/** #3 — schedules/cancels the AlarmManager wake-up for a single reminder. */
object ReminderScheduler {
    private const val EXTRA_MESSAGE_ID = "messageId"
    private const val EXTRA_THREAD_ID = "threadId"
    private const val EXTRA_NOTE = "note"

    fun schedule(context: Context, reminderId: Long, messageId: Long, threadId: Long, remindAt: Long, note: String?) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_MESSAGE_ID, messageId)
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_NOTE, note)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pendingIntent)
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra("threadId", -1)
        val note = intent.getStringExtra("note")
        NotificationChannels.ensureCreated(context)

        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Reminder")
            .setContentText(note?.takeIf { it.isNotBlank() } ?: "Follow up on this conversation")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
        nm?.notify(("reminder_$threadId").hashCode(), notification)
    }
}
