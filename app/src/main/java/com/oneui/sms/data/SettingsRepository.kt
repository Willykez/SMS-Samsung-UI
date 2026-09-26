package com.oneui.sms.data

import android.content.Context
import com.oneui.sms.data.local.AppDatabase
import com.oneui.sms.data.local.QuickResponseEntity
import com.oneui.sms.data.local.SettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Backs #8, #9, #11, #14, #15 — the global toggles under Settings > More settings. */
class SettingsRepository(context: Context) {
    private val db = AppDatabase.get(context)

    fun observe(): Flow<SettingsEntity> =
        db.settingsDao().observe().map { it ?: SettingsEntity() }

    suspend fun update(transform: (SettingsEntity) -> SettingsEntity) = withContext(Dispatchers.IO) {
        // Settings changes are rare (user tapping a toggle), so a plain
        // read-then-write is fine — no need for a transaction here.
        val current = db.settingsDao().observe().first() ?: SettingsEntity()
        db.settingsDao().upsert(transform(current))
    }

    suspend fun setRecycleBinEnabled(enabled: Boolean) = update { it.copy(recycleBinEnabled = enabled) }
    suspend fun setAutoDeleteDays(days: Int?) = update { it.copy(autoDeleteDays = days) }
    suspend fun setShowLinkPreviews(show: Boolean) = update { it.copy(showLinkPreviews = show) }
    suspend fun setFontScale(scale: Float) = update { it.copy(fontScale = scale) }
    suspend fun setCategoriesEnabled(enabled: Boolean) = update { it.copy(categoriesEnabled = enabled) }
    suspend fun setRemoveLocationFromSharedImages(remove: Boolean) = update { it.copy(removeLocationFromSharedImages = remove) }


    suspend fun setNotificationsEnabled(v: Boolean) = update { it.copy(notificationsEnabled = v) }
    suspend fun setNotificationSoundEnabled(v: Boolean) = update { it.copy(notificationSoundEnabled = v) }
    suspend fun setNotificationVibrationEnabled(v: Boolean) = update { it.copy(notificationVibrationEnabled = v) }
    suspend fun setQuickReplyNotifications(v: Boolean) = update { it.copy(quickReplyNotifications = v) }
    suspend fun setDeliveryReports(v: Boolean) = update { it.copy(deliveryReports = v) }
    suspend fun setConfirmLongSms(v: Boolean) = update { it.copy(confirmLongSms = v) }
    suspend fun setShowSegmentCount(v: Boolean) = update { it.copy(showSegmentCount = v) }
    suspend fun setAutoDetectOtp(v: Boolean) = update { it.copy(autoDetectOtp = v) }
    suspend fun setCompactConversations(v: Boolean) = update { it.copy(compactConversations = v) }
    suspend fun setShowContactAvatars(v: Boolean) = update { it.copy(showContactAvatars = v) }
    suspend fun setAnimateMessages(v: Boolean) = update { it.copy(animateMessages = v) }
    suspend fun setThemeMode(v: String) = update { it.copy(themeMode = v) }

    // conversation categories (tab row)
    fun observeCategories(): Flow<List<com.oneui.sms.data.local.CategoryEntity>> = db.categoryDao().observeAll()
    suspend fun addCategory(name: String) = withContext(Dispatchers.IO) {
        db.categoryDao().upsert(com.oneui.sms.data.local.CategoryEntity(name = name))
    }
    suspend fun deleteCategory(id: Long) = withContext(Dispatchers.IO) { db.categoryDao().delete(id) }

    // #14 quick responses
    fun observeQuickResponses(): Flow<List<QuickResponseEntity>> = db.quickResponseDao().observeAll()
    suspend fun addQuickResponse(text: String) = withContext(Dispatchers.IO) {
        db.quickResponseDao().upsert(QuickResponseEntity(text = text))
    }
    suspend fun deleteQuickResponse(id: Long) = withContext(Dispatchers.IO) {
        db.quickResponseDao().delete(id)
    }
}
