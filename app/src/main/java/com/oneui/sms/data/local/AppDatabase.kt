package com.oneui.sms.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromStatus(status: DeliveryStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DeliveryStatus = DeliveryStatus.valueOf(value)
}

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ReminderEntity::class,
        QuickResponseEntity::class,
        SettingsEntity::class,
        CategoryEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun reminderDao(): ReminderDao
    abstract fun quickResponseDao(): QuickResponseDao
    abstract fun settingsDao(): SettingsDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "onemessages.db",
                )
                    // Pre-release scaffold: destructive migration is fine until we
                    // ship v1. Replace with real Migration objects before release.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
