package com.oneui.sms.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Only fires when this app is the default SMS app (SMS_DELIVER, not the legacy
 * SMS_RECEIVED broadcast every app used to get). Writes the incoming message to
 * the system store, then refreshes the Room cache so the UI updates reactively.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }

        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sender)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)

        val repo = SmsRepository(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val threadId = Telephony.Threads.getOrCreateThreadId(context, setOf(sender))
            val blocked = repo.isBlocked(sender)
            if (!blocked) {
                repo.refreshConversations()
                val settings = SettingsRepository(context.applicationContext).observe().first()
                if (settings.notificationsEnabled) {
                    MessageNotification.show(
                        context, sender, body, threadId,
                        muted = false,
                        withReply = settings.quickReplyNotifications,
                        withSound = settings.notificationSoundEnabled,
                        withVibration = settings.notificationVibrationEnabled,
                    )
                }
            }
        }
    }
}


class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("messageId", Long.MIN_VALUE)
        if (id == Long.MIN_VALUE) return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            SmsRepository(context.applicationContext).setMessageStatus(id, if (resultCode == android.app.Activity.RESULT_OK) com.oneui.sms.data.local.DeliveryStatus.SENT else com.oneui.sms.data.local.DeliveryStatus.FAILED)
        }
    }
}

class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("messageId", Long.MIN_VALUE)
        if (id == Long.MIN_VALUE) return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            SmsRepository(context.applicationContext).setMessageStatus(id, com.oneui.sms.data.local.DeliveryStatus.DELIVERED)
        }
    }
}
