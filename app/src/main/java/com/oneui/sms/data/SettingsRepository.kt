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
