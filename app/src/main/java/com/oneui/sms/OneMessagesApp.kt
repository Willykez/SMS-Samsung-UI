package com.oneui.sms

import android.app.Application
import com.oneui.sms.worker.RetentionPurgeWorker

class OneMessagesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // #11 auto-delete + #9 recycle-bin purge run on a daily background cadence.
        RetentionPurgeWorker.schedulePeriodic(this)
    }
}
