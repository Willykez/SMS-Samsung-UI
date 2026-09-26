@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.oneui.sms.ui.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.sms.data.local.DeliveryStatus
import com.oneui.sms.data.local.MessageEntity
import com.oneui.sms.data.local.QuickResponseEntity
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.net.Uri

@Composable
fun MessageThreadScreen(
    contactName: String,
    contactAddress: String,
    messages: List<MessageEntity>,
    draft: String,
    isSearching: Boolean,
    fontScale: Float,
    quickResponses: List<QuickResponseEntity>,
    pendingScheduleTime: Long?,
    chatColorHex: String?,
    onSetChatColor: (String?) -> Unit,
    isMuted: Boolean,
    onSetMuted: (Boolean) -> Unit,
    onBlockNumber: () -> Unit,
    onSetReminder: (MessageEntity, Long, String?) -> Unit,
    onDraftChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSend: () -> Unit,
    onToggleStar: (MessageEntity) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onPickQuickResponse: (QuickResponseEntity) -> Unit,
    onSetScheduleTime: (Long?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showQuickResponses by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<MessageEntity?>(null) }
    val context = LocalContext.current
    val bubbleTint = chatColorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    val otp = remember(messages) { messages.asReversed().firstNotNullOfOrNull { SmartSms.extractOtp(it.body) } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSearching) {
                SearchTopBar(onQueryChange = onSearchQueryChange, onClose = onToggleSearch)
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(initials = contactName.take(1).uppercase())
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(contactName, fontWeight = FontWeight.SemiBold)
                                Text(if (isMuted) "SMS · Muted" else "SMS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    actions = {
                        IconButton(onClick = onToggleSearch) { Icon(Icons.Filled.Search, "Search") }
                        Box {
                            IconButton(onClick = { showTools = true }) { Icon(Icons.Filled.MoreVert, "Conversation options") }
                            DropdownMenu(showTools, { showTools = false }) {
                                DropdownMenuItem(text = { Text(if (isMuted) "Unmute notifications" else "Mute notifications") }, leadingIcon = { Icon(Icons.Filled.NotificationsOff, null) }, onClick = { showTools = false; onSetMuted(!isMuted) })
                                DropdownMenuItem(text = { Text("Chat appearance") }, leadingIcon = { Icon(Icons.Filled.Palette, null) }, onClick = { showTools = false; showColorPicker = true })
                                DropdownMenuItem(text = { Text("Contact profile") }, leadingIcon = { Icon(Icons.Filled.Person, null) }, onClick = { showTools = false; showProfile = true })
                                DropdownMenuItem(text = { Text("Block number") }, leadingIcon = { Icon(Icons.Filled.Block, null) }, onClick = { showTools = false; onBlockNumber() })
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            Column {
                if (showQuickResponses) QuickResponseRow(quickResponses) { onPickQuickResponse(it); showQuickResponses = false }
                if (pendingScheduleTime != null) ScheduleChip(pendingScheduleTime) { onSetScheduleTime(null) }
                InteractionBar(draft, onDraftChange, onSend, { showQuickResponses = !showQuickResponses }, { /* schedule menu */ }, pendingScheduleTime != null, onSetScheduleTime)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (otp != null) {
                item(key = "otp") { OtpCard(otp) }
            }
            items(messages.asReversed(), key = { it.id }) { message ->
                MessageBubble(message, fontScale, bubbleTint, { onToggleStar(message) }, { onDeleteMessage(message.id) }, { mins -> onSetReminder(message, System.currentTimeMillis() + mins * 60_000L, null) }, { infoMessage = message })
            }
        }
    }
    if (showColorPicker) ChatColorPickerDialog(bubbleTint, { onSetChatColor(it); showColorPicker = false }) { showColorPicker = false }
    if (showProfile) {
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Row(verticalAlignment = Alignment.CenterVertically) { Avatar(contactName.take(1).uppercase()); Column(Modifier.padding(start = 12.dp)) { Text(contactName, fontWeight = FontWeight.SemiBold); Text("SMS contact", style = MaterialTheme.typography.labelSmall) } } },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(contactAddress, style = MaterialTheme.typography.bodyMedium); Text("Messages are sent as standard SMS.", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            confirmButton = { TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contactAddress}"))); showProfile = false }) { Text("Call") } },
            dismissButton = { TextButton(onClick = { showProfile = false }) { Text("Close") } },
        )
    }
    infoMessage?.let { message ->
        AlertDialog(onDismissRequest = { infoMessage = null }, title = { Text("Message info") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(message.body); Text("${if (message.isOutgoing) "Sent" else "Received"} · ${SimpleDateFormat("EEE, d MMM yyyy h:mm a", Locale.getDefault()).format(Date(message.timestamp))}"); Text("Status · ${message.status.name.lowercase().replace('_',' ')}"); Text("${message.body.length} characters · ${SmartSms.smsSegments(message.body)} SMS ${if (SmartSms.smsSegments(message.body) == 1) "segment" else "segments"}") } }, confirmButton = { TextButton(onClick = { infoMessage = null }) { Text("Done") } })
    }
}

@Composable private fun Avatar(initials: String) {
    Surface(Modifier.size(40.dp), CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Text(initials, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
}

@Composable private fun OtpCard(code: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text("Verification code", fontWeight = FontWeight.SemiBold); Text(code.chunked(3).joinToString(" "), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            val clipboard = LocalClipboardManager.current
            TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) { Text("Copy") }
        }
    }
}

@Composable private fun MessageBubble(message: MessageEntity, fontScale: Float, outgoingTint: Color?, onStar: () -> Unit, onDelete: () -> Unit, onReminder: (Long) -> Unit, onInfo: () -> Unit) {
    val outgoing = message.isOutgoing
    val color = if (outgoing) (outgoingTint ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    var menu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start) {
        Surface(color = color, shape = if (outgoing) RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp) else RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp), modifier = Modifier.widthIn(max = 330.dp).combinedClickable(onClick = {}, onLongClick = { menu = true })) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.Bottom) {
                Text(message.body, color = textColor, fontSize = (16 * fontScale).sp, lineHeight = (22 * fontScale).sp)
                if (message.isStarred) Icon(Icons.Filled.Star, null, tint = textColor, modifier = Modifier.padding(start = 7.dp).size(14.dp))
            }
        }
        Row(Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (outgoing) Icon(if (message.status == DeliveryStatus.FAILED) Icons.Filled.ErrorOutline else if (message.status == DeliveryStatus.PENDING) Icons.Filled.Schedule else Icons.Filled.Done, null, modifier = Modifier.padding(start = 4.dp).size(13.dp), tint = if (message.status == DeliveryStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem({ Text("Copy text") }, { clipboard.setText(AnnotatedString(message.body)); menu = false }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) })
            DropdownMenuItem({ Text(if (message.isStarred) "Unstar" else "Star") }, { onStar(); menu = false }, leadingIcon = { Icon(if (message.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder, null) })
            DropdownMenuItem({ Text("Remind me in 1 hour") }, { onReminder(60); menu = false }, leadingIcon = { Icon(Icons.Filled.Alarm, null) })
            DropdownMenuItem({ Text("Delete") }, { onDelete(); menu = false }, leadingIcon = { Icon(Icons.Filled.DeleteOutline, null) })
            DropdownMenuItem({ Text("Message info") }, { onInfo(); menu = false }, leadingIcon = { Icon(Icons.Filled.Info, null) })
        }
    }
}

@Composable private fun QuickResponseRow(responses: List<QuickResponseEntity>, onPick: (QuickResponseEntity) -> Unit) {
    if (responses.isEmpty()) return
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(responses, key = { it.id }) { SuggestionChip(onClick = { onPick(it) }, label = { Text(it.text, maxLines = 1) }) } }
}

@Composable private fun ScheduleChip(time: Long, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp)); Text("Scheduled · ${SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(time))}", style = MaterialTheme.typography.labelMedium, Modifier.padding(start = 6.dp)) }
        IconButton(onClick = onClear, Modifier.size(32.dp)) { Icon(Icons.Filled.Close, "Cancel") }
    }
}

@Composable private fun InteractionBar(draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit, onQuick: () -> Unit, onSchedule: () -> Unit, scheduled: Boolean, onSetSchedule: (Long?) -> Unit) {
    val segments = SmartSms.smsSegments(draft)
    Surface(tonalElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = onQuick, Modifier.size(48.dp)) { Icon(Icons.Filled.Bolt, "Quick replies") }
                Box {
                    IconButton(onClick = { onSetSchedule(System.currentTimeMillis() + 60 * 60_000L) }, Modifier.size(48.dp)) { Icon(if (scheduled) Icons.Filled.Schedule else Icons.Filled.CalendarMonth, "Schedule SMS") }
                }
                OutlinedTextField(value = draft, onValueChange = onDraftChange, modifier = Modifier.weight(1f), placeholder = { Text("Text message") }, shape = MaterialTheme.shapes.extraLarge, maxLines = 5, colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent))
                IconButton(onClick = onSend, enabled = draft.isNotBlank(), Modifier.size(48.dp)) { Surface(Modifier.size(40.dp), CircleShape, color = if (draft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (scheduled) Icons.Filled.Schedule else Icons.Filled.Send, null, tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) } } }
            }
            if (draft.isNotEmpty()) Text("${draft.length} characters · $segments SMS ${if (segments == 1) "segment" else "segments"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 56.dp, top = 3.dp))
        }
    }
}

@Composable private fun SearchTopBar(onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    TopAppBar(title = { TextField(value = query, onValueChange = { query = it; onQueryChange(it) }, placeholder = { Text("Search this conversation") }, singleLine = true, colors = TextFieldDefaults.colors(unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent)) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close search") } })
}

@Composable private fun ChatColorPickerDialog(current: Color?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val swatches = listOf(null to "Default", "#0381FE" to "Blue", "#34C759" to "Green", "#FF9500" to "Orange", "#AF52DE" to "Purple", "#FF3B30" to "Red")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Chat appearance") }, text = { Column { swatches.forEach { (hex, label) -> TextButton(onClick = { onPick(hex) }, Modifier.fillMaxWidth()) { Text(label) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
