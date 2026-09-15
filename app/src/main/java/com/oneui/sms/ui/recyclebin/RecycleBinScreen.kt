package com.oneui.sms.ui.recyclebin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.data.local.ConversationEntity
import com.oneui.sms.data.local.MessageEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecycleBinViewModel(private val repository: SmsRepository) : ViewModel() {
    val deletedConversations: StateFlow<List<ConversationEntity>> = repository.observeRecycleBinConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedMessages: StateFlow<List<MessageEntity>> = repository.observeDeletedMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreConversation(threadId: Long) = viewModelScope.launch {
        repository.restoreConversations(listOf(threadId))
    }

    fun restoreMessage(messageId: Long) = viewModelScope.launch {
        repository.restoreMessage(messageId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    conversations: List<ConversationEntity>,
    messages: List<MessageEntity>,
    onRestoreConversation: (Long) -> Unit,
    onRestoreMessage: (Long) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle bin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
        ) {
            item {
                Text(
                    "Items are permanently deleted after 30 days.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(conversations, key = { "c${it.threadId}" }) { convo ->
                RecycleBinRow(
                    title = convo.displayName ?: convo.address,
                    subtitle = convo.snippet,
                    onRestore = { onRestoreConversation(convo.threadId) },
                )
            }
            items(messages, key = { "m${it.id}" }) { msg ->
                RecycleBinRow(
                    title = msg.address,
                    subtitle = msg.body,
                    onRestore = { onRestoreMessage(msg.id) },
                )
            }
        }
    }
}

@Composable
private fun RecycleBinRow(title: String, subtitle: String, onRestore: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(subtitle, maxLines = 1)
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Filled.Restore, contentDescription = "Restore")
            }
        }
    }
}
