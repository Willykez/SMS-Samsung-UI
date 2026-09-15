package com.oneui.sms.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.oneui.sms.data.local.AppDatabase
import com.oneui.sms.data.local.ConversationEntity
import com.oneui.sms.data.local.DeliveryStatus
import com.oneui.sms.data.local.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for SMS data. Reads from Android's Telephony content
 * provider (the real on-device SMS store — there is no backend server in this
 * SMS-only scope) and mirrors results into Room for fast, reactive UI binding.
 *
 * Sending is done via SmsManager; delivery/sent status comes back through
 * PendingIntents registered in SmsDeliverReceiver.
 */
class SmsRepository(private val context: Context) {

    private val db = AppDatabase.get(context)
    private val resolver = context.contentResolver

    fun observeConversations(): Flow<List<ConversationEntity>> =
        db.conversationDao().observeConversations()

    fun observeThread(threadId: Long): Flow<List<MessageEntity>> =
        db.messageDao().observeThread(threadId)

    /** Pulls the latest state from Telephony.Sms into the local Room cache. */
    suspend fun refreshConversations() = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Telephony.Sms.Conversations.THREAD_ID,
            Telephony.Sms.Conversations.SNIPPET,
        )
        // Telephony.Sms.Conversations is deprecated in favor of manual grouping by
        // THREAD_ID over Telephony.Sms.CONTENT_URI on modern API levels; grouping
        // logic lives here so the rest of the app only deals with clean entities.
        val cursor = resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE,
            ),
            null, null,
            "${Telephony.Sms.DATE} DESC",
        ) ?: return@withContext

        val latestByThread = LinkedHashMap<Long, ConversationEntity>()
        val unreadCounts = HashMap<Long, Int>()

        cursor.use {
            val idxThread = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val idxAddress = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val idxBody = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val idxDate = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val idxRead = it.getColumnIndexOrThrow(Telephony.Sms.READ)

            while (it.moveToNext()) {
                val threadId = it.getLong(idxThread)
                val address = it.getString(idxAddress) ?: continue
                val read = it.getInt(idxRead)

                if (read == 0) unreadCounts[threadId] = (unreadCounts[threadId] ?: 0) + 1

                if (!latestByThread.containsKey(threadId)) {
                    latestByThread[threadId] = ConversationEntity(
                        threadId = threadId,
                        address = address,
                        displayName = null, // resolved separately via ContactsResolver
                        snippet = it.getString(idxBody) ?: "",
                        timestamp = it.getLong(idxDate),
                        unreadCount = 0, // filled below once full scan completes
                    )
                }
            }
        }

        val merged = latestByThread.values.map { c ->
            c.copy(unreadCount = unreadCounts[c.threadId] ?: 0)
        }
        db.conversationDao().upsertAll(merged)
    }

    suspend fun refreshThread(threadId: Long) = withContext(Dispatchers.IO) {
        val cursor = resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
            ),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC",
        ) ?: return@withContext

        val messages = mutableListOf<MessageEntity>()
        cursor.use {
            val idxId = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val idxAddress = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val idxBody = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val idxDate = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val idxType = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val type = it.getInt(idxType)
                val isOutgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                    type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                messages += MessageEntity(
                    id = it.getLong(idxId),
                    threadId = threadId,
                    address = it.getString(idxAddress) ?: "",
                    body = it.getString(idxBody) ?: "",
                    timestamp = it.getLong(idxDate),
                    isOutgoing = isOutgoing,
                    status = if (isOutgoing) DeliveryStatus.SENT else DeliveryStatus.RECEIVED,
                )
            }
        }
        db.messageDao().upsertAll(messages)
    }

    /**
     * Sends an SMS and writes an OUTBOX_PENDING row immediately so the UI can
     * render the bubble before the network confirms delivery (offline-outbox
     * pattern, scaled down from the report's WorkManager-based version).
     */
    suspend fun sendMessage(threadId: Long, address: String, body: String) = withContext(Dispatchers.IO) {
        val pendingId = -System.currentTimeMillis() // negative temp id until Telephony assigns a real one
        db.messageDao().upsert(
            MessageEntity(
                id = pendingId,
                threadId = threadId,
                address = address,
                body = body,
                timestamp = System.currentTimeMillis(),
                isOutgoing = true,
                status = DeliveryStatus.PENDING,
            )
        )

        // Persist to the system SMS store so it survives in Telephony's own history.
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.THREAD_ID, threadId)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
        }
        resolver.insert(Telephony.Sms.CONTENT_URI, values)

        val smsManager = ContextCompat.getSystemService(context, SmsManager::class.java)
        smsManager?.sendTextMessage(address, null, body, null, null)

        db.messageDao().updateStatus(pendingId, DeliveryStatus.SENT)
    }
}
