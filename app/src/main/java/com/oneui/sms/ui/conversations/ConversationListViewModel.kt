package com.oneui.sms.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.data.local.ConversationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ListFilter { ALL, UNREAD_ONLY }

class ConversationListViewModel(private val repository: SmsRepository) : ViewModel() {

    private val filter = MutableStateFlow(ListFilter.ALL)
    private val allConversations = repository.observeConversations()
    private val unreadConversations = repository.observeUnreadOnly()

    val conversations: StateFlow<List<ConversationEntity>> =
        combine(filter, allConversations, unreadConversations) { f, all, unread ->
            if (f == ListFilter.UNREAD_ONLY) unread else all
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFilter: StateFlow<ListFilter> = filter

    // ---- multi-select mode (matches the Pin-to-top / Delete / Notifications bottom bar) ----
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    fun toggleSelected(threadId: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(threadId)) remove(threadId)
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun setFilter(f: ListFilter) {
        filter.value = f
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshConversations() }
    }

    // ---- overflow menu actions ----
    fun markAllAsRead() = viewModelScope.launch { repository.markAllRead() }

    // ---- selection bottom-bar actions ----
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
}
