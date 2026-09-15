package com.oneui.sms.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.data.local.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessageThreadViewModel(
    private val repository: SmsRepository,
    private val threadId: Long,
    private val address: String,
) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> = repository.observeThread(threadId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft

    init {
        viewModelScope.launch { repository.refreshThread(threadId) }
    }

    fun onDraftChange(text: String) {
        _draft.value = text
    }

    fun send() {
        val body = _draft.value.trim()
        if (body.isEmpty()) return
        _draft.value = ""
        viewModelScope.launch {
            repository.sendMessage(threadId, address, body)
        }
    }
}
