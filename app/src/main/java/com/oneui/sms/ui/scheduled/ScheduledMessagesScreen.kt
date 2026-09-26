@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oneui.sms.ui.scheduled

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.ui.theme.OneUiSectionCard
import com.oneui.sms.ui.theme.OneUiDateTimePickerDialog
import com.oneui.sms.ui.theme.OneUiTokens
import com.oneui.sms.data.local.MessageEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduledMessagesViewModel(private val repository: SmsRepository) : ViewModel() {
    val scheduled: StateFlow<List<MessageEntity>> = repository.observeScheduled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancel(messageId: Long) = viewModelScope.launch { repository.cancelScheduled(messageId) }
    fun reschedule(message: MessageEntity, at: Long) = viewModelScope.launch { repository.rescheduleMessage(message, at) }
    fun sendNow(message: MessageEntity) = viewModelScope.launch { repository.sendScheduledNow(message) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessagesScreen(
    messages: List<MessageEntity>,
    onCancel: (Long) -> Unit,
    onReschedule: (MessageEntity, Long) -> Unit,
    onSendNow: (MessageEntity) -> Unit,
    onBack: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("EEE, d MMM yyyy h:mm a", Locale.getDefault()) }
    var editing by remember { mutableStateOf<MessageEntity?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled messages") },
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
            contentPadding = PaddingValues(OneUiTokens.PageHorizontal, 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                OneUiSectionCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(msg.address, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    formatter.format(Date(msg.scheduledAt ?: msg.timestamp)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Text(
                            msg.body,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { onSendNow(msg) }) {
                                Icon(Icons.Filled.Send, null, modifier = Modifier.padding(end = 6.dp))
                                Text("Send now")
                            }
                            TextButton(onClick = { editing = msg }) {
                                Icon(Icons.Filled.Edit, null, modifier = Modifier.padding(end = 6.dp))
                                Text("Reschedule")
                            }
                            IconButton(onClick = { onCancel(msg.id) }) {
                                Icon(Icons.Filled.Close, "Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { msg ->
        OneUiDateTimePickerDialog(
            initialAt = msg.scheduledAt ?: System.currentTimeMillis(),
            onDismiss = { editing = null },
            onPicked = { selectedAt ->
                onReschedule(msg, selectedAt)
                editing = null
            },
        )
    }
}

