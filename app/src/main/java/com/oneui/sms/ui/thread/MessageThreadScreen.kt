@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.oneui.sms.ui.thread

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.sms.data.local.DeliveryStatus
import com.oneui.sms.data.local.MessageEntity
import com.oneui.sms.data.local.QuickResponseEntity
import androidx.picker.app.SeslDatePickerDialog
import androidx.picker.app.SeslTimePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ComposerShape = RoundedCornerShape(26.dp)
private val BubbleShape = RoundedCornerShape(22.dp)
private val BubbleTightShape = RoundedCornerShape(22.dp, 22.dp, 7.dp, 22.dp)

@Composable
fun MessageThreadScreen(
    contactName: String,
    contactAddress: String,
    contactPhotoUri: String? = null,
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
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current
    val bubbleTint = chatColorHex?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    }
    val otp = remember(messages) {
        messages.asReversed().firstNotNullOfOrNull { SmartSms.extractOtp(it.body) }
    }
    val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val list = remember(messages) { messages.asReversed() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (isSearching) {
                SearchTopBar(onQueryChange = onSearchQueryChange, onClose = onToggleSearch)
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(contactName.take(1).uppercase(), contactPhotoUri, 40.dp)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(contactName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    Text(
                                        if (isMuted) "SMS · Muted" else "SMS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 5.dp),
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleSearch) {
                            Icon(Icons.Filled.Search, "Search conversation")
                        }
                        Box {
                            IconButton(onClick = { showTools = true }) {
                                Icon(Icons.Filled.MoreVert, "Conversation options")
                            }
                            DropdownMenu(
                                expanded = showTools,
                                onDismissRequest = { showTools = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isMuted) "Unmute notifications" else "Mute notifications") },
                                    leadingIcon = { Icon(Icons.Filled.NotificationsOff, null) },
                                    onClick = { showTools = false; onSetMuted(!isMuted) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Chat appearance") },
                                    leadingIcon = { Icon(Icons.Filled.Palette, null) },
                                    onClick = { showTools = false; showColorPicker = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Contact profile") },
                                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                                    onClick = { showTools = false; showProfile = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Block number") },
                                    leadingIcon = { Icon(Icons.Filled.Block, null) },
                                    onClick = { showTools = false; onBlockNumber() },
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            ThreadComposer(
                draft = draft,
                keyboardOpen = keyboardOpen,
                showQuickResponses = showQuickResponses,
                quickResponses = quickResponses,
                pendingScheduleTime = pendingScheduleTime,
                onDraftChange = onDraftChange,
                onSend = {
                    onSend()
                    keyboard?.show()
                },
                onQuick = { showQuickResponses = !showQuickResponses },
                onSchedule = {
                    val calendar = java.util.Calendar.getInstance().apply {
                        timeInMillis = pendingScheduleTime ?: (System.currentTimeMillis() + 60 * 60_000L)
                    }
                    val dateDialog = SeslDatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val chosenDate = java.util.Calendar.getInstance().apply {
                                timeInMillis = calendar.timeInMillis
                                set(java.util.Calendar.YEAR, year)
                                set(java.util.Calendar.MONTH, month)
                                set(java.util.Calendar.DAY_OF_MONTH, day)
                            }
                            SeslTimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    chosenDate.set(java.util.Calendar.HOUR_OF_DAY, hour)
                                    chosenDate.set(java.util.Calendar.MINUTE, minute)
                                    chosenDate.set(java.util.Calendar.SECOND, 0)
                                    chosenDate.set(java.util.Calendar.MILLISECOND, 0)
                                    val sendAt = chosenDate.timeInMillis
                                    onSetScheduleTime(
                                        if (sendAt > System.currentTimeMillis()) sendAt else null
                                    )
                                },
                                chosenDate.get(java.util.Calendar.HOUR_OF_DAY),
                                chosenDate.get(java.util.Calendar.MINUTE),
                                DateFormat.is24HourFormat(context),
                            ).show()
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH),
                    )
                    dateDialog.show()
                },
                onClearSchedule = { onSetScheduleTime(null) },
                onPickQuickResponse = {
                    onPickQuickResponse(it)
                    showQuickResponses = false
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (otp != null) {
                    item(key = "otp") {
                        OtpCard(otp)
                    }
                }

                itemsIndexed(
                    items = list,
                    key = { _, message -> message.id },
                ) { index, message ->
                    val previous = list.getOrNull(index - 1)
                    val next = list.getOrNull(index + 1)
                    val groupedWithPrevious = previous != null &&
                        previous.isOutgoing == message.isOutgoing &&
                        previous.timestamp - message.timestamp < 2 * 60_000L
                    val groupedWithNext = next != null &&
                        next.isOutgoing == message.isOutgoing &&
                        message.timestamp - next.timestamp < 2 * 60_000L

                    if (shouldShowDateHeader(message.timestamp, previous?.timestamp)) {
                        DateDivider(message.timestamp)
                    }

                    MessageBubble(
                        message = message,
                        fontScale = fontScale,
                        outgoingTint = bubbleTint,
                        groupedWithPrevious = groupedWithPrevious,
                        groupedWithNext = groupedWithNext,
                        onStar = { onToggleStar(message) },
                        onDelete = { onDeleteMessage(message.id) },
                        onReminder = { mins ->
                            onSetReminder(
                                message,
                                System.currentTimeMillis() + mins * 60_000L,
                                null,
                            )
                        },
                        onInfo = { infoMessage = message },
                    )
                }
            }

        }
    }

    if (showColorPicker) {
        ChatColorPickerDialog(
            current = bubbleTint,
            onPick = {
                onSetChatColor(it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }

    if (showProfile) {
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(contactName.take(1).uppercase(), contactPhotoUri, 48.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(contactName, fontWeight = FontWeight.SemiBold)
                        Text("SMS contact", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(contactAddress, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Messages are sent as standard SMS.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactAddress"))
                        )
                        showProfile = false
                    }
                ) { Text("Call") }
            },
            dismissButton = {
                TextButton(onClick = { showProfile = false }) { Text("Close") }
            },
        )
    }

    infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            title = { Text("Message info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(message.body)
                    Text(
                        "${if (message.isOutgoing) "Sent" else "Received"} · ${
                            SimpleDateFormat(
                                "EEE, d MMM yyyy h:mm a",
                                Locale.getDefault(),
                            ).format(Date(message.timestamp))
                        }",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Status · ${message.status.name.lowercase().replace('_', ' ')}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${message.body.length} characters · ${SmartSms.smsSegments(message.body)} SMS ${
                            if (SmartSms.smsSegments(message.body) == 1) "segment" else "segments"
                        }",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { infoMessage = null }) { Text("Done") }
            },
        )
    }
}

@Composable
private fun ThreadComposer(
    draft: String,
    keyboardOpen: Boolean,
    showQuickResponses: Boolean,
    quickResponses: List<QuickResponseEntity>,
    pendingScheduleTime: Long?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuick: () -> Unit,
    onSchedule: () -> Unit,
    onClearSchedule: () -> Unit,
    onPickQuickResponse: (QuickResponseEntity) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val segments = SmartSms.smsSegments(draft)
    val canSend = draft.isNotBlank()
    val composerBottom by animateDpAsState(if (keyboardOpen) 4.dp else 10.dp, label = "composerBottom")
    val maxLines = if (keyboardOpen) 7 else 5

    Surface(
        tonalElevation = if (keyboardOpen) 2.dp else 5.dp,
        shadowElevation = if (keyboardOpen) 2.dp else 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(bottom = composerBottom)
            .animateContentSize(),
    ) {
        Column {
            AnimatedVisibility(showQuickResponses && quickResponses.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(quickResponses, key = { it.id }) { response ->
                        Surface(
                            onClick = { onPickQuickResponse(response) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                response.text,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            pendingScheduleTime?.let { time ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp))
                    Text(
                        "Scheduled · ${
                            SimpleDateFormat(
                                "EEE, h:mm a",
                                Locale.getDefault(),
                            ).format(Date(time))
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 7.dp),
                    )
                    IconButton(onClick = onClearSchedule, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, "Cancel scheduled send")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (!keyboardOpen) {
                    ComposerAction(
                        icon = Icons.Filled.Bolt,
                        label = "Quick replies",
                        onClick = onQuick,
                    )
                }

                ComposerAction(
                    icon = if (pendingScheduleTime == null) Icons.Filled.CalendarMonth else Icons.Filled.Schedule,
                    label = if (pendingScheduleTime == null) "Schedule SMS" else "Scheduled",
                    onClick = onSchedule,
                    selected = pendingScheduleTime != null,
                )

                OneUiComposerField(
                    value = draft,
                    onValueChange = onDraftChange,
                    focusRequester = focusRequester,
                    keyboardOpen = keyboardOpen,
                    maxLines = maxLines,
                    onSend = {
                        if (canSend) {
                            onSend()
                            keyboard?.show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )

                SendButton(
                    enabled = canSend,
                    scheduled = pendingScheduleTime != null,
                    onClick = {
                        if (canSend) {
                            onSend()
                            focusRequester.requestFocus()
                        }
                    },
                )
            }

            AnimatedVisibility(draft.isNotEmpty() && keyboardOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 64.dp, end = 18.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (segments > 1) Icons.Filled.Info else Icons.Filled.Check,
                        null,
                        modifier = Modifier.size(13.dp),
                        tint = if (segments > 1) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${draft.length} characters · $segments SMS ${
                            if (segments == 1) "segment" else "segments"
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        modifier = Modifier.size(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                label,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OneUiComposerField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    keyboardOpen: Boolean,
    maxLines: Int,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .padding(horizontal = 4.dp)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                    onSend()
                    true
                } else false
            },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        singleLine = false,
        maxLines = maxLines,
        decorationBox = { innerTextField ->
            Surface(
                shape = ComposerShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (keyboardOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    else Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (value.isEmpty()) {
                        Text(
                            "Text message",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun SendButton(
    enabled: Boolean,
    scheduled: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (enabled) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = bg,
        modifier = Modifier
            .padding(start = 6.dp)
            .size(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (scheduled) Icons.Filled.Schedule else Icons.Filled.Send,
                "Send message",
                tint = fg,
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    fontScale: Float,
    outgoingTint: Color?,
    groupedWithPrevious: Boolean,
    groupedWithNext: Boolean,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onReminder: (Long) -> Unit,
    onInfo: () -> Unit,
) {
    val outgoing = message.isOutgoing
    val color = if (outgoing) {
        outgoingTint ?: MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (outgoing) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    var menu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    val shape = when {
        outgoing && groupedWithPrevious && groupedWithNext ->
            RoundedCornerShape(20.dp, 8.dp, 8.dp, 20.dp)
        outgoing && groupedWithPrevious ->
            RoundedCornerShape(20.dp, 8.dp, 8.dp, 20.dp)
        outgoing && groupedWithNext ->
            RoundedCornerShape(20.dp, 20.dp, 8.dp, 20.dp)
        outgoing -> BubbleTightShape
        groupedWithPrevious && groupedWithNext ->
            RoundedCornerShape(8.dp, 20.dp, 20.dp, 8.dp)
        groupedWithPrevious ->
            RoundedCornerShape(8.dp, 20.dp, 20.dp, 8.dp)
        groupedWithNext ->
            RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp)
        else -> RoundedCornerShape(7.dp, 22.dp, 22.dp, 22.dp)
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = color,
            shape = shape,
            modifier = Modifier
                .widthIn(max = 342.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menu = true },
                ),
        ) {
            Row(
                Modifier.padding(
                    start = 15.dp,
                    end = 13.dp,
                    top = 10.dp,
                    bottom = 10.dp,
                ),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    message.body,
                    color = textColor,
                    fontSize = (16 * fontScale).sp,
                    lineHeight = (22 * fontScale).sp,
                )
                if (message.isStarred) {
                    Icon(
                        Icons.Filled.Star,
                        null,
                        tint = textColor.copy(alpha = 0.9f),
                        modifier = Modifier
                            .padding(start = 7.dp)
                            .size(14.dp),
                    )
                }
            }
        }

        if (!groupedWithNext) {
            Row(
                Modifier.padding(
                    start = if (outgoing) 0.dp else 6.dp,
                    end = if (outgoing) 6.dp else 0.dp,
                    top = 2.dp,
                    bottom = 2.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault())
                        .format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (outgoing) {
                    Icon(
                        when (message.status) {
                            DeliveryStatus.FAILED -> Icons.Filled.ErrorOutline
                            DeliveryStatus.PENDING -> Icons.Filled.Schedule
                            DeliveryStatus.SENT -> Icons.Filled.DoneAll
                            else -> Icons.Filled.Check
                        },
                        null,
                        modifier = Modifier.padding(start = 4.dp).size(13.dp),
                        tint = if (message.status == DeliveryStatus.FAILED)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (message.status == DeliveryStatus.FAILED) {
                        Text(
                            "Not sent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Copy text") },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                onClick = {
                    clipboard.setText(AnnotatedString(message.body))
                    menu = false
                },
            )
            DropdownMenuItem(
                text = { Text(if (message.isStarred) "Unstar" else "Star") },
                leadingIcon = {
                    Icon(
                        if (message.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        null,
                    )
                },
                onClick = { onStar(); menu = false },
            )
            DropdownMenuItem(
                text = { Text("Remind me in 1 hour") },
                leadingIcon = { Icon(Icons.Filled.Alarm, null) },
                onClick = {
                    onReminder(60)
                    menu = false
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Filled.DeleteOutline, null) },
                onClick = { onDelete(); menu = false },
            )
            DropdownMenuItem(
                text = { Text("Message info") },
                leadingIcon = { Icon(Icons.Filled.Info, null) },
                onClick = { onInfo(); menu = false },
            )
        }
    }
}

@Composable
private fun DateDivider(timestamp: Long) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.padding(vertical = 9.dp).fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
            Text(
                dateLabel(timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun shouldShowDateHeader(current: Long, previous: Long?): Boolean {
    if (previous == null) return true
    return !isSameDay(current, previous)
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(Date(a)) == fmt.format(Date(b))
}

private fun dateLabel(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val day = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now)
    return when {
        day == today -> "Today"
        else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(date)
    }
}

@Composable
private fun Avatar(initials: String, photoUri: String? = null, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val context = LocalContext.current
    val bitmap = remember(photoUri) {
        photoUri?.let {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(it))
                    ?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap.asImageBitmap(),
                contentDescription = initials,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    initials,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun OtpCard(code: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    Icons.Filled.Key,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(9.dp),
                )
            }
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp),
            ) {
                Text("Verification code", fontWeight = FontWeight.SemiBold)
                Text(
                    code.chunked(3).joinToString(" "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            val clipboard = LocalClipboardManager.current
            TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Text("Copy")
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "Close search")
            }
        },
        title = {
            OneUiSearchField(
                query = query,
                onQueryChange = {
                    query = it
                    onQueryChange(it)
                },
            )
        },
    )
}

@Composable
private fun OneUiSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        decorationBox = { inner ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(19.dp))
                    Box(Modifier.weight(1f).padding(start = 8.dp)) {
                        if (query.isEmpty()) {
                            Text(
                                "Search this conversation",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                }
            }
        },
    )
}

@Composable
private fun ChatColorPickerDialog(
    current: Color?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val swatches = listOf(
        null to "Default",
        "#0381FE" to "Blue",
        "#34C759" to "Green",
        "#FF9500" to "Orange",
        "#AF52DE" to "Purple",
        "#FF3B30" to "Red",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat appearance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                swatches.forEach { (hex, label) ->
                    Surface(
                        onClick = { onPick(hex) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (hex == null) MaterialTheme.colorScheme.surfaceVariant
                        else runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrElse { MaterialTheme.colorScheme.primary },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hex == null) MaterialTheme.colorScheme.primary
                                        else Color.White.copy(alpha = 0.2f)
                                    ),
                            )
                            Text(
                                label,
                                color = if (hex == null) MaterialTheme.colorScheme.onSurface
                                else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                            if (
                                (hex == null && current == null) ||
                                (hex != null && current != null && current == runCatching {
                                    Color(android.graphics.Color.parseColor(hex))
                                }.getOrNull())
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    null,
                                    tint = if (hex == null) MaterialTheme.colorScheme.primary
                                    else Color.White,
                                    modifier = Modifier.padding(start = 7.dp).size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
