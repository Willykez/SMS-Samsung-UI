package com.oneui.sms.ui.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneui.sms.data.local.ConversationEntity

/**
 * Implements the One UI "viewing area" pattern: a large collapsible header
 * (title + search) that shrinks as the list scrolls. The overflow menu here
 * matches the Samsung Messages inbox menu: Delete / Mark all as read /
 * Starred messages / Scheduled messages / Recycle bin / Settings.
 * ("Edit categories" / "Reorder pinned" are left as later additions — see
 * FEATURE_SPEC.md.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    conversations: List<ConversationEntity>,
    selectedIds: Set<Long>,
    filter: ListFilter,
    onOpenConversation: (Long) -> Unit,
    onCompose: () -> Unit,
    onToggleSelected: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onSetFilter: (ListFilter) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenSettings: () -> Unit,
    onMuteSelected: (Boolean) -> Unit,
    onDeleteSelected: () -> Unit,
    onPinSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inSelectionMode = selectedIds.isNotEmpty()
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (inSelectionMode) {
                SelectionTopBar(count = selectedIds.size, onClose = onClearSelection)
            } else {
                ConversationListTopBar(
                    filter = filter,
                    onSetFilter = onSetFilter,
                    onMarkAllRead = onMarkAllRead,
                    onOpenStarred = onOpenStarred,
                    onOpenScheduled = onOpenScheduled,
                    onOpenRecycleBin = onOpenRecycleBin,
                    onOpenSettings = onOpenSettings,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        floatingActionButton = {
            if (!inSelectionMode) {
                // Anchored bottom-right, within thumb reach per One UI's "Interact Naturally" pillar.
                FloatingActionButton(onClick = onCompose) {
                    Icon(Icons.Filled.Add, contentDescription = "New message")
                }
            }
        },
        bottomBar = {
            if (inSelectionMode) {
                SelectionBottomBar(
                    onMute = { onMuteSelected(true) },
                    onDelete = onDeleteSelected,
                    onPin = { onPinSelected(true) },
                )
            }
        },
    ) { padding ->
        if (conversations.isEmpty()) {
            EmptyState(padding, filter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(conversations, key = { it.threadId }) { convo ->
                    val isSelected = convo.threadId in selectedIds
                    if (inSelectionMode) {
                        ConversationRow(
                            conversation = convo,
                            onClick = { onToggleSelected(convo.threadId) },
                            selectable = true,
                            isSelected = isSelected,
                        )
                    } else {
                        SwipeableConversationRow(
                            conversation = convo,
                            onOpen = { onOpenConversation(convo.threadId) },
                            onLongPress = { onToggleSelected(convo.threadId) },
                            onArchive = { onPinSelected(true) },
                            onDelete = { onDeleteSelected() },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListTopBar(
    filter: ListFilter,
    onSetFilter: (ListFilter) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenSettings: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    var menuOpen by remember { mutableStateOf(false) }

    LargeTopAppBar(
        title = { Text("Messages", fontWeight = FontWeight.Bold) },
        actions = {
            IconButton(onClick = {
                onSetFilter(if (filter == ListFilter.ALL) ListFilter.UNREAD_ONLY else ListFilter.ALL)
            }) {
                // #10 view unread messages only
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filter",
                    tint = if (filter == ListFilter.UNREAD_ONLY) MaterialTheme.colorScheme.primary else LocalContentColorDefault(),
                )
            }
            IconButton(onClick = { /* #12-style search across all conversations */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Mark all as read") }, onClick = { menuOpen = false; onMarkAllRead() })
                    DropdownMenuItem(text = { Text("Starred messages") }, onClick = { menuOpen = false; onOpenStarred() }) // #2
                    DropdownMenuItem(text = { Text("Scheduled messages") }, onClick = { menuOpen = false; onOpenScheduled() }) // #1
                    DropdownMenuItem(text = { Text("Recycle bin") }, onClick = { menuOpen = false; onOpenRecycleBin() }) // #9
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; onOpenSettings() })
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LocalContentColorDefault() = MaterialTheme.colorScheme.onSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit) {
    TopAppBar(
        title = { Text("$count selected") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
            }
        },
    )
}

@Composable
private fun SelectionBottomBar(onMute: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit) {
    // Matches the screenshot: Notifications / Delete / More(Pin to top) row,
    // all within the lower thumb-reach zone.
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomBarAction(Icons.Filled.NotificationsOff, "Notifications", onMute)
            BottomBarAction(Icons.Filled.Delete, "Delete", onDelete)
            BottomBarAction(Icons.Filled.PushPin, "Pin to top", onPin)
        }
    }
}

@Composable
private fun BottomBarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationRow(
    conversation: ConversationEntity,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                SwipeToDismissBoxValue.StartToEnd -> { onArchive(); true }
                else -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
    ) {
        ConversationRow(conversation = conversation, onClick = onOpen, onLongClick = onLongPress)
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val (color, label) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary to "Pin"
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error to "Delete"
        else -> MaterialTheme.colorScheme.surface to ""
    }
    Row(
        Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (direction == SwipeToDismissBoxValue.StartToEnd) Arrangement.Start else Arrangement.End,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selectable: Boolean = false,
    isSelected: Boolean = false,
) {
    val rowModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.combinedClickable(onClick = onClick)
    }

    ListItem(
        modifier = rowModifier.padding(horizontal = 4.dp).clip(MaterialTheme.shapes.medium),
        leadingContent = {
            if (selectable) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text((conversation.displayName ?: conversation.address).take(1).uppercase())
                    }
                }
            }
        },
        headlineContent = {
            Text(
                conversation.displayName ?: conversation.address,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
            )
        },
        supportingContent = {
            Text(
                conversation.snippet,
                maxLines = 1,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conversation.isPinned) {
                    Icon(Icons.Filled.PushPin, contentDescription = "Pinned", modifier = Modifier.size(16.dp))
                }
                if (conversation.isMuted) {
                    Icon(Icons.Filled.NotificationsOff, contentDescription = "Muted", modifier = Modifier.size(16.dp))
                }
                if (conversation.unreadCount > 0) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(
                            conversation.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun EmptyState(padding: PaddingValues, filter: ListFilter) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val title = if (filter == ListFilter.UNREAD_ONLY) "No unread messages" else "No messages yet"
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        if (filter == ListFilter.ALL) {
            Text("Tap + to start a conversation", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
