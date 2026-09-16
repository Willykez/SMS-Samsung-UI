package com.oneui.sms.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.sms.data.SettingsRepository
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.data.local.CategoryEntity
import com.oneui.sms.data.local.ConversationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val CATEGORY_ALL = "All" // reserved tab name — not a real CategoryEntity row

class ConversationListViewModel(
    private val repository: SmsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow(CATEGORY_ALL)
    val currentCategory: StateFlow<String> = selectedCategory

    val categories: StateFlow<List<CategoryEntity>> = settingsRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesEnabled: StateFlow<Boolean> = settingsRepository.observe()
        .map { it.categoriesEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val conversations: StateFlow<List<ConversationEntity>> = selectedCategory
        .flatMapLatest { category ->
            if (category == CATEGORY_ALL) repository.observeConversations() else repository.observeByCategory(category)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Drives the bottom-nav "Conversations" tab badge — always the full unread
    // count regardless of which category tab is selected.
    val totalUnreadCount: StateFlow<Int> = repository.observeConversations()
        .map { list -> list.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ---- multi-select mode (matches the Pin-to-top / Delete / Notifications bottom bar) ----
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    fun toggleSelected(threadId: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(threadId)) remove(threadId)
        }
    }

    fun clearSelection() { _selectedIds.value = emptySet() }
    fun selectCategory(name: String) { selectedCategory.value = name }

    fun refresh() = viewModelScope.launch { repository.refreshConversations() }
    fun markAllAsRead() = viewModelScope.launch { repository.markAllRead() }

    fun addCategory(name: String) = viewModelScope.launch { settingsRepository.addCategory(name) }
    fun deleteCategory(id: Long) = viewModelScope.launch { settingsRepository.deleteCategory(id) }
    fun setCategoriesEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setCategoriesEnabled(enabled) }

    fun muteSelected(muted: Boolean) = viewModelScope.launch {
        repository.setMuted(_selectedIds.value.toList(), muted)
        clearSelection()
    }

    fun deleteSelected() = viewModelScope.launch {
        repository.softDeleteConversations(_selectedIds.value.toList()) // #9 recycle bin
        clearSelection()
    }

    fun pinSelected(pinned: Boolean) = viewModelScope.launch {
        repository.setPinned(_selectedIds.value.toList(), pinned)
        clearSelection()
    }

    fun assignSelectedToCategory(category: String?) = viewModelScope.launch {
        repository.setCategory(_selectedIds.value.toList(), category)
        clearSelection()
    }
}
