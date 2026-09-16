package com.oneui.sms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    // Pinned threads first, then most recent — matches One UI's conversation list ordering.
    @Query("SELECT * FROM conversations WHERE isArchived = 0 AND deletedAt IS NULL ORDER BY isPinned DESC, timestamp DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isArchived = 0 AND deletedAt IS NULL AND unreadCount > 0 ORDER BY isPinned DESC, timestamp DESC")
    fun observeUnreadOnly(): Flow<List<ConversationEntity>> // #10

    @Query("SELECT * FROM conversations WHERE isArchived = 0 AND deletedAt IS NULL AND category = :category ORDER BY isPinned DESC, timestamp DESC")
    fun observeByCategory(category: String): Flow<List<ConversationEntity>>

    @Query("UPDATE conversations SET category = :category WHERE threadId IN (:threadIds)")
    suspend fun setCategory(threadIds: List<Long>, category: String?)

    @Query("SELECT * FROM conversations WHERE isArchived = 1 AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeArchived(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeRecycleBin(): Flow<List<ConversationEntity>> // #9

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("UPDATE conversations SET isPinned = :pinned WHERE threadId IN (:threadIds)")
    suspend fun setPinned(threadIds: List<Long>, pinned: Boolean)

    @Query("UPDATE conversations SET isArchived = :archived WHERE threadId IN (:threadIds)")
    suspend fun setArchived(threadIds: List<Long>, archived: Boolean)

    @Query("UPDATE conversations SET isMuted = :muted WHERE threadId IN (:threadIds)")
    suspend fun setMuted(threadIds: List<Long>, muted: Boolean)

    @Query("UPDATE conversations SET notificationSoundUri = :uri WHERE threadId = :threadId")
    suspend fun setNotificationSound(threadId: Long, uri: String?)

    @Query("UPDATE conversations SET chatColorHex = :hex WHERE threadId = :threadId")
    suspend fun setChatColor(threadId: Long, hex: String?)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE threadId IN (:threadIds)")
    suspend fun markRead(threadIds: List<Long>)

    @Query("UPDATE conversations SET unreadCount = 0")
    suspend fun markAllRead()

    // #9 soft delete / restore / purge
    @Query("UPDATE conversations SET deletedAt = :at WHERE threadId IN (:threadIds)")
    suspend fun softDelete(threadIds: List<Long>, at: Long)

    @Query("UPDATE conversations SET deletedAt = NULL WHERE threadId IN (:threadIds)")
    suspend fun restore(threadIds: List<Long>)

    @Query("DELETE FROM conversations WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    @Query("DELETE FROM conversations WHERE threadId = :threadId")
    suspend fun deleteHard(threadId: Long)

    @Query("""
        SELECT * FROM conversations
        WHERE isArchived = 0 AND deletedAt IS NULL
          AND (displayName LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun search(query: String): Flow<List<ConversationEntity>>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE threadId = :threadId AND deletedAt IS NULL AND scheduledAt IS NULL ORDER BY timestamp ASC")
    fun observeThread(threadId: Long): Flow<List<MessageEntity>>

    // #12 search within a single thread
    @Query("SELECT * FROM messages WHERE threadId = :threadId AND deletedAt IS NULL AND body LIKE '%' || :query || '%' ORDER BY timestamp ASC")
    fun searchInThread(threadId: Long, query: String): Flow<List<MessageEntity>>

    // #2 starred messages, across all threads
    @Query("SELECT * FROM messages WHERE isStarred = 1 AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeStarred(): Flow<List<MessageEntity>>

    // #1 scheduled messages awaiting send
    @Query("SELECT * FROM messages WHERE scheduledAt IS NOT NULL AND status = 'SCHEDULED' ORDER BY scheduledAt ASC")
    fun observeScheduled(): Flow<List<MessageEntity>>

    // #9 recycle bin contents
    @Query("SELECT * FROM messages WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DeliveryStatus)

    @Query("UPDATE messages SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("UPDATE messages SET deletedAt = :at WHERE id = :id")
    suspend fun softDelete(id: Long, at: Long)

    @Query("UPDATE messages SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM messages WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    // #11 auto-delete old (non-deleted, non-scheduled) messages past the retention window
    @Query("UPDATE messages SET deletedAt = :now WHERE timestamp < :cutoff AND deletedAt IS NULL AND scheduledAt IS NULL")
    suspend fun softDeleteOlderThan(cutoff: Long, now: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteHard(id: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY remindAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE messageId = :messageId")
    suspend fun deleteForMessage(messageId: Long)
}

@Dao
interface QuickResponseDao {
    @Query("SELECT * FROM quick_responses ORDER BY id ASC")
    fun observeAll(): Flow<List<QuickResponseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(response: QuickResponseEntity): Long

    @Query("DELETE FROM quick_responses WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    fun observe(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)
}
