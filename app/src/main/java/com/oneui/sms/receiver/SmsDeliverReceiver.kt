package com.oneui.sms.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.oneui.sms.data.SmsRepository
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
            repo.refreshConversations()
        }

        // TODO: post a One UI-style heads-up notification here (grouped by thread,
        // with a quick-reply RemoteInput action) once notification design is in scope.
    }
}
