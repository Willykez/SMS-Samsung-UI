package com.oneui.sms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    // Pinned threads first, then most recent — matches One UI's conversation list ordering.
    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun observeArchived(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("UPDATE conversations SET isPinned = :pinned WHERE threadId = :threadId")
    suspend fun setPinned(threadId: Long, pinned: Boolean)

    @Query("UPDATE conversations SET isArchived = :archived WHERE threadId = :threadId")
    suspend fun setArchived(threadId: Long, archived: Boolean)

    @Query("UPDATE conversations SET isMuted = :muted WHERE threadId = :threadId")
    suspend fun setMuted(threadId: Long, muted: Boolean)

    @Query("DELETE FROM conversations WHERE threadId = :threadId")
    suspend fun delete(threadId: Long)

    @Query("""
        SELECT * FROM conversations
        WHERE isArchived = 0 AND (displayName LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun search(query: String): Flow<List<ConversationEntity>>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun observeThread(threadId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DeliveryStatus)
}
