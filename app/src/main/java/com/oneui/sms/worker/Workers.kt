package com.oneui.sms.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.oneui.sms.data.SettingsRepository
import com.oneui.sms.data.SmsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * #1 Schedule messages — fires SmsManager.sendTextMessage at the scheduled
 * time. One WorkRequest is enqueued per scheduled message (delayed start),
 * matching the "Will be sent: <date>" chip shown in the composer.
 */
class ScheduledSendWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_THREAD_ID = "threadId"
        const val KEY_ADDRESS = "address"
        const val KEY_BODY = "body"

        fun enqueue(context: Context, messageId: Long, threadId: Long, address: String, body: String, sendAt: Long) {
            val delay = (sendAt - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ScheduledSendWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        KEY_MESSAGE_ID to messageId,
                        KEY_THREAD_ID to threadId,
                        KEY_ADDRESS to address,
                        KEY_BODY to body,
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "scheduled_send_$messageId",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context, messageId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("scheduled_send_$messageId")
        }
    }

    override suspend fun doWork(): Result {
        val threadId = inputData.getLong(KEY_THREAD_ID, -1)
        val address = inputData.getString(KEY_ADDRESS) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        if (threadId == -1L) return Result.failure()

        SmsRepository(applicationContext).sendMessage(threadId, address, body)
        return Result.success()
    }
}

/**
 * #11 Auto-delete old messages — runs daily, soft-deletes (into the recycle
 * bin per #9) anything older than the user's configured retention window.
 */
class RetentionPurgeWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        private const val UNIQUE_NAME = "retention_purge_daily"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RetentionPurgeWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).observe().first()
        val repo = SmsRepository(applicationContext)

        settings.autoDeleteDays?.let { days -> repo.applyAutoDeleteRetention(days) }
        if (settings.recycleBinEnabled) {
            repo.purgeRecycleBin(retentionDays = 30) // fixed 30-day recycle bin window
        }
        return Result.success()
    }
}
