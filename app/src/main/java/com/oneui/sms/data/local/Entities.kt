package com.oneui.sms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors a conversation thread from Telephony.Threads for fast list rendering.
 * The Telephony provider remains the source of truth; this is a read-through cache
 * so the conversation list can render instantly and stay searchable offline.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val threadId: Long,
    val address: String,          // phone number / contact address
    val displayName: String?,     // resolved contact name, if any
    val snippet: String,          // last message preview text
    val timestamp: Long,
    val unreadCount: Int,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,     // Telephony.Sms._ID
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val status: DeliveryStatus,
)

enum class DeliveryStatus { PENDING, SENT, DELIVERED, FAILED, RECEIVED }
