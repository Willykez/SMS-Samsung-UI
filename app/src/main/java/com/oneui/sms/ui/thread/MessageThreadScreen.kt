@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.oneui.sms.ui.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.sms.data.local.DeliveryStatus
import com.oneui.sms.data.local.MessageEntity
import com.oneui.sms.data.local.QuickResponseEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Splits the screen per the One UI pattern: an upper Viewing Area (message
 * stream) and a lower Interaction Area within thumb reach (composer row,
 * quick-response chips, min 48dp touch targets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageThreadScreen(
    contactName: String,
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
    var showScheduleMenu by remember { mutableStateOf(false) }
    var showConversationMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val bubbleTint = chatColorHex?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isSearching) {
                SearchTopBar(onQueryChange = onSearchQueryChange, onClose = onToggleSearch)
            } else {
                TopAppBar(
                    title = { Text(contactName, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleSearch) { // #12
                            Icon(Icons.Filled.Search, contentDescription = "Search in conversation")
                        }
                        Box {
                            IconButton(onClick = { showConversationMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Conversation options")
                            }
                            DropdownMenu(expanded = showConversationMenu, onDismissRequest = { showConversationMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (isMuted) "Unmute notifications" else "Mute notifications") }, // #5
                                    onClick = { showConversationMenu = false; onSetMuted(!isMuted) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Chat background color") }, // #7
                                    onClick = { showConversationMenu = false; showColorPicker = true },
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            Column {
                if (showQuickResponses) {
                    QuickResponseRow(quickResponses) { response ->
                        onPickQuickResponse(response)
                        showQuickResponses = false
                    }
                }
                if (pendingScheduleTime != null) {
                    ScheduleChip(timeMillis = pendingScheduleTime, onClear = { onSetScheduleTime(null) })
                }
                InteractionBar(
                    draft = draft,
                    onDraftChange = onDraftChange,
                    onSend = onSend,
                    onToggleQuickResponses = { showQuickResponses = !showQuickResponses },
                    onOpenScheduleMenu = { showScheduleMenu = true },
                    scheduleMenuExpanded = showScheduleMenu,
                    onDismissScheduleMenu = { showScheduleMenu = false },
                    onPickSchedulePreset = { minutesFromNow ->
                        showScheduleMenu = false
                        onSetScheduleTime(System.currentTimeMillis() + minutesFromNow * 60_000L)
                    },
                    isScheduled = pendingScheduleTime != null,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            reverseLayout = true, // newest at bottom, matches chat convention
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(
                items = messages.asReversed(),
                key = { it.id },
                // contentType groups incoming/outgoing bubbles so Compose can reuse
                // layout slots during fast scroll, per the report's recomposition guidance.
                contentType = { if (it.isOutgoing) "outgoing" else "incoming" },
            ) { message ->
                MessageBubble(
                    message,
                    fontScale,
                    outgoingTint = bubbleTint,
                    onToggleStar = { onToggleStar(message) },
                    onDelete = { onDeleteMessage(message.id) },
                    onSetReminder = { minutesFromNow ->
                        onSetReminder(message, System.currentTimeMillis() + minutesFromNow * 60_000L, null)
                    },
                )
            }
        }
    }

    if (showColorPicker) {
        ChatColorPickerDialog(
            current = bubbleTint,
            onPick = { hex -> onSetChatColor(hex); showColorPicker = false },
            onDismiss = { showColorPicker = false },
        )
    }
}

private val CHAT_COLOR_SWATCHES = listOf(
    null to "Default",
    "#0381FE" to "Blue",
    "#34C759" to "Green",
    "#FF9500" to "Orange",
    "#AF52DE" to "Purple",
    "#FF3B30" to "Red",
)

@Composable
private fun ChatColorPickerDialog(current: Color?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat background color") },
        text = {
            Column {
                CHAT_COLOR_SWATCHES.forEach { (hex, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .combinedClickableSimple { onPick(hex) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = hex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        ) {}
                        Text(label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSimple(onClick: () -> Unit): Modifier =
    this.then(Modifier.combinedClickable(onClick = onClick, onLongClick = {}))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = { query = it; onQueryChange(it) },
                placeholder = { Text("Search in conversation") },
                colors = TextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        navigationIcon = {
            IconButton(onClick = { onQueryChange(""); onClose() }) {
                Icon(Icons.Filled.Close, contentDescription = "Close search")
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageEntity,
    fontScale: Float,
    outgoingTint: Color?,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    onSetReminder: (Long) -> Unit,
) {
    val bubbleColor = if (message.isOutgoing) (outgoingTint ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isOutgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    var showActions by remember { mutableStateOf(false) }

    if (message.status == DeliveryStatus.SCHEDULED) {
        // Matches the "Message scheduled." system notice bubble from the reference design.
        Box(Modifier.fillMaxWidth()) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    "Message scheduled",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = (12 * fontScale).sp,
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.align(alignment).widthIn(max = 280.dp)) {
            Surface(
                color = bubbleColor,
                shape = MaterialTheme.shapes.large, // fully rounded, One UI-style
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { showActions = true },
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message.body,
                        color = textColor,
                        fontSize = (16 * fontScale).sp, // #8 font scale
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                    if (message.isStarred) { // #2
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Starred",
                            tint = textColor,
                            modifier = Modifier.size(14.dp).padding(end = 8.dp),
                        )
                    }
                }
            }
            if (message.isOutgoing && message.status == DeliveryStatus.FAILED) {
                Text(
                    "Not sent — tap to retry",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp, end = 4.dp),
                )
            }
            DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                DropdownMenuItem(
                    text = { Text(if (message.isStarred) "Unstar" else "Star") },
                    leadingIcon = { Icon(if (message.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder, null) },
                    onClick = { showActions = false; onToggleStar() },
                )
                DropdownMenuItem(
                    text = { Text("Remind me in 1 hour") }, // #3
                    onClick = { showActions = false; onSetReminder(60) },
                )
                DropdownMenuItem(
                    text = { Text("Remind me tomorrow, 9am") },
                    onClick = { showActions = false; onSetReminder(minutesUntilTomorrow9am()) },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { showActions = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun QuickResponseRow(responses: List<QuickResponseEntity>, onPick: (QuickResponseEntity) -> Unit) {
    if (responses.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(responses, key = { it.id }) { response ->
            SuggestionChip(onClick = { onPick(response) }, label = { Text(response.text) })
        }
    }
}

@Composable
private fun ScheduleChip(timeMillis: Long, onClear: () -> Unit) {
    val formatter = remember { SimpleDateFormat("EEE, d MMM h:mm a", Locale.getDefault()) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                "Will be sent: ${formatter.format(Date(timeMillis))}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Cancel scheduling")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractionBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleQuickResponses: () -> Unit,
    onOpenScheduleMenu: () -> Unit,
    scheduleMenuExpanded: Boolean,
    onDismissScheduleMenu: () -> Unit,
    onPickSchedulePreset: (Long) -> Unit,
    isScheduled: Boolean,
) {
    // Everything here sits in the lower thumb-sweep zone with >=48dp targets,
    // matching the report's "Interact Naturally" ergonomic requirement.
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(onClick = onOpenScheduleMenu) {
                    Icon(Icons.Filled.Add, contentDescription = "More options")
                }
                DropdownMenu(expanded = scheduleMenuExpanded, onDismissRequest = onDismissScheduleMenu) {
                    DropdownMenuItem(text = { Text("Quick responses") }, onClick = { onDismissScheduleMenu(); onToggleQuickResponses() })
                    DropdownMenuItem(text = { Text("Schedule: in 1 hour") }, onClick = { onPickSchedulePreset(60) }) // #1
                    DropdownMenuItem(text = { Text("Schedule: tonight 8pm") }, onClick = {
                        onPickSchedulePreset(minutesUntilTonight8pm())
                    })
                    DropdownMenuItem(text = { Text("Schedule: tomorrow 9am") }, onClick = {
                        onPickSchedulePreset(minutesUntilTomorrow9am())
                    })
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(),
            )
            IconButton(onClick = onSend, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (isScheduled) Icons.Filled.Schedule else Icons.Filled.Send,
                    contentDescription = if (isScheduled) "Schedule send" else "Send",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun minutesUntilTonight8pm(): Long {
    val cal = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        if (before(cal)) add(Calendar.DAY_OF_YEAR, 1)
    }
    return (target.timeInMillis - cal.timeInMillis) / 60_000
}

private fun minutesUntilTomorrow9am(): Long {
    val cal = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    return (target.timeInMillis - cal.timeInMillis) / 60_000
}
