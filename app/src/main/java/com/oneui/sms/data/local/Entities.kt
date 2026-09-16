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
    val notificationSoundUri: String? = null, // #6 custom sound per contact
    val chatColorHex: String? = null,         // #7 custom chat background/bubble tint
    val category: String? = null,             // e.g. Personal/Shipping/OTP tab
    val deletedAt: Long? = null,              // #9 recycle bin (soft delete)
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,     // Telephony.Sms._ID, or a negative temp id while pending/scheduled
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val status: DeliveryStatus,
    val isStarred: Boolean = false,   // #2
    val scheduledAt: Long? = null,    // #1
    val deletedAt: Long? = null,      // #9 recycle bin
)

enum class DeliveryStatus { PENDING, SCHEDULED, SENT, DELIVERED, FAILED, RECEIVED }

/** #3 — a standalone reminder to follow up on a message you haven't replied to. */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val threadId: Long,
    val remindAt: Long,
    val note: String? = null,
)

/** #14 — canned replies available from the composer's quick-response picker. */
@Entity(tableName = "quick_responses")
data class QuickResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
)

/** Conversation categories (All / Personal / Shipping / OTP / custom) — the tab row on the inbox. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)

/** Single-row table (id is always 0) holding the global toggles from Settings. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val recycleBinEnabled: Boolean = true,       // #9
    val autoDeleteDays: Int? = null,             // #11 — null = never
    val showLinkPreviews: Boolean = true,         // #15
    val fontScale: Float = 1.0f,                  // #8
    val categoriesEnabled: Boolean = true,        // conversation categories on/off
    val removeLocationFromSharedImages: Boolean = false,
)
