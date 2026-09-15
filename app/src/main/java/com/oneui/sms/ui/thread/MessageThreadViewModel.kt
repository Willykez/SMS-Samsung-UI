package com.oneui.sms.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.sms.data.SettingsRepository
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.data.local.MessageEntity
import com.oneui.sms.data.local.QuickResponseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessageThreadViewModel(
    private val repository: SmsRepository,
    private val settingsRepository: SettingsRepository,
    private val threadId: Long,
    private val address: String,
) : ViewModel() {

    // #12 in-thread search: empty query falls back to the normal thread view.
    private val searchQuery = MutableStateFlow("")
    val isSearching: StateFlow<Boolean> = searchQuery
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val messages: StateFlow<List<MessageEntity>> = searchQuery
        .flatMapLatest { q ->
            if (q.isBlank()) repository.observeThread(threadId) else repository.searchInThread(threadId, q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fontScale: StateFlow<Float> = settingsRepository.observe()
        .map { it.fontScale }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val quickResponses: StateFlow<List<QuickResponseEntity>> = settingsRepository.observeQuickResponses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // #6/#7 — per-conversation notification sound & chat color live on the ConversationEntity.
    val conversation: StateFlow<com.oneui.sms.data.local.ConversationEntity?> = repository.observeConversations()
        .map { list -> list.find { it.threadId == threadId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setChatColor(hex: String?) = viewModelScope.launch { repository.setChatColor(threadId, hex) }
    fun setMuted(muted: Boolean) = viewModelScope.launch { repository.setMuted(listOf(threadId), muted) }

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft

    // #1 schedule-message composer state
    private val _pendingScheduleTime = MutableStateFlow<Long?>(null)
    val pendingScheduleTime: StateFlow<Long?> = _pendingScheduleTime

    init {
        viewModelScope.launch { repository.refreshThread(threadId) }
        viewModelScope.launch { repository.markRead(listOf(threadId)) }
    }

    fun onDraftChange(text: String) {
        _draft.value = text
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun send() {
        val body = _draft.value.trim()
        if (body.isEmpty()) return
        val scheduleAt = _pendingScheduleTime.value
        _draft.value = ""
        _pendingScheduleTime.value = null
        viewModelScope.launch {
            if (scheduleAt != null) {
                repository.scheduleMessage(threadId, address, body, scheduleAt) // #1
            } else {
                repository.sendMessage(threadId, address, body)
            }
        }
    }

    fun setScheduleTime(timeMillis: Long?) {
        _pendingScheduleTime.value = timeMillis
    }

    fun toggleStar(message: MessageEntity) = viewModelScope.launch { // #2
        repository.setStarred(message.id, !message.isStarred)
    }

    fun deleteMessage(messageId: Long) = viewModelScope.launch { // #9
        repository.softDeleteMessage(messageId)
    }

    fun pickQuickResponse(response: QuickResponseEntity) { // #14
        _draft.value = response.text
    }

    fun setReminder(message: MessageEntity, remindAt: Long, note: String?) = viewModelScope.launch { // #3
        repository.setReminder(message.id, threadId, remindAt, note)
    }
}
