package com.oneui.sms.receiver

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

/**
 * MMS is explicitly out of scope for this app. Android still requires a
 * WAP_PUSH_DELIVER receiver to exist for the app to be offered as a default
 * SMS app candidate, so this simply no-ops (any incoming MMS is silently
 * dropped rather than parsed/rendered).
 */
class MmsStubReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Intentionally no-op — MMS is not supported in this SMS-only build.
    }
}

/**
 * Handles RESPOND_VIA_MESSAGE (e.g. "quick decline text" from the incoming
 * call screen). Required for full default-SMS-app compliance.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val uri = intent?.data
        if (body != null && uri != null) {
            val address = uri.schemeSpecificPart
            android.telephony.SmsManager.getDefault().sendTextMessage(address, null, body, null, null)
        } else {
            Toast.makeText(this, "Couldn't send quick reply", Toast.LENGTH_SHORT).show()
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
