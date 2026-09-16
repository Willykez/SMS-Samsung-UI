@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneui.sms.data.local.CategoryEntity
import com.oneui.sms.data.local.ConversationEntity
import kotlinx.coroutines.launch

enum class BottomTab { CONVERSATIONS, CONTACTS }

/**
 * Matches the reference Samsung Messages inbox: bold "Messages" title with
 * filter/search/overflow actions, a scrollable category tab row (All +
 * custom categories + "+"), the conversation list itself, and a bottom
 * Conversations/Contacts nav with a chat-bubble FAB anchored above it.
 */
@Composable
fun ConversationListScreen(
    conversations: List<ConversationEntity>,
    categories: List<CategoryEntity>,
    categoriesEnabled: Boolean,
    currentCategory: String,
    selectedIds: Set<Long>,
    totalUnreadCount: Int,
    onSelectCategory: (String) -> Unit,
    onOpenConversation: (Long) -> Unit,
    onCompose: () -> Unit,
    onToggleSelected: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenUnread: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenEditCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenContacts: () -> Unit,
    onMuteSelected: (Boolean) -> Unit,
    onDeleteSelected: () -> Unit,
    onPinSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inSelectionMode = selectedIds.isNotEmpty()
    var bottomTab by remember { mutableStateOf(BottomTab.CONVERSATIONS) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (inSelectionMode) {
                SelectionTopBar(count = selectedIds.size, onClose = onClearSelection)
            } else {
                ConversationListTopBar(
                    onMarkAllRead = onMarkAllRead,
                    onOpenEditCategories = onOpenEditCategories,
                    onOpenStarred = onOpenStarred,
                    onOpenScheduled = onOpenScheduled,
                    onOpenRecycleBin = onOpenRecycleBin,
                    onOpenSettings = onOpenSettings,
                    onOpenUnread = onOpenUnread,
                    onDeleteStart = { scope.launch { snackbarHostState.showSnackbar("Select conversations to delete") } },
                    onReorderPinned = { scope.launch { snackbarHostState.showSnackbar("Reorder pinned: coming soon") } },
                )
            }
        },
        floatingActionButton = {
            if (!inSelectionMode && bottomTab == BottomTab.CONVERSATIONS) {
                FloatingActionButton(onClick = onCompose, shape = CircleShape) {
                    Icon(Icons.Filled.ChatBubble, contentDescription = "New message")
                }
            }
        },
        bottomBar = {
            Column {
                if (inSelectionMode) {
                    SelectionBottomBar(
                        onMute = { onMuteSelected(true) },
                        onDelete = onDeleteSelected,
                        onPin = { onPinSelected(true) },
                    )
                } else {
                    NavigationBar {
                        NavigationBarItem(
                            selected = bottomTab == BottomTab.CONVERSATIONS,
                            onClick = { bottomTab = BottomTab.CONVERSATIONS },
                            icon = {
                                BadgedBox(badge = {
                                    if (totalUnreadCount > 0) Badge { Text(totalUnreadCount.toString()) }
                                }) {
                                    Icon(Icons.Filled.Forum, contentDescription = null)
                                }
                            },
                            label = { Text("Conversations") },
                        )
                        NavigationBarItem(
                            selected = bottomTab == BottomTab.CONTACTS,
                            onClick = { bottomTab = BottomTab.CONTACTS; onOpenContacts() },
                            icon = { Icon(Icons.Filled.Contacts, contentDescription = null) },
                            label = { Text("Contacts") },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!inSelectionMode && categoriesEnabled) {
                CategoryTabRow(
                    categories = categories,
                    current = currentCategory,
                    onSelect = onSelectCategory,
                    onAddClick = onOpenEditCategories,
                )
            }
            if (conversations.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
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
}

@Composable
private fun CategoryTabRow(
    categories: List<CategoryEntity>,
    current: String,
    onSelect: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            CategoryTab(label = CATEGORY_ALL, selected = current == CATEGORY_ALL, onClick = { onSelect(CATEGORY_ALL) })
        }
        items(categories, key = { it.id }) { category ->
            CategoryTab(label = category.name, selected = current == category.name, onClick = { onSelect(category.name) })
        }
        item {
            IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Add category", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CategoryTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = {}),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (selected) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(width = 20.dp, height = 2.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun ConversationListTopBar(
    onMarkAllRead: () -> Unit,
    onOpenEditCategories: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUnread: () -> Unit,
    onDeleteStart: () -> Unit,
    onReorderPinned: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Messages", fontWeight = FontWeight.Bold) },
        actions = {
            IconButton(onClick = onOpenUnread) {
                Icon(Icons.Filled.MarkChatUnread, contentDescription = "Unread messages")
            }
            IconButton(onClick = { /* search across conversations */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                // Matches the reference overflow menu order exactly.
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDeleteStart() })
                    DropdownMenuItem(text = { Text("Mark all as read") }, onClick = { menuOpen = false; onMarkAllRead() })
                    DropdownMenuItem(text = { Text("Edit categories") }, onClick = { menuOpen = false; onOpenEditCategories() })
                    DropdownMenuItem(text = { Text("Reorder pinned") }, onClick = { menuOpen = false; onReorderPinned() })
                    DropdownMenuItem(text = { Text("Starred messages") }, onClick = { menuOpen = false; onOpenStarred() })
                    DropdownMenuItem(text = { Text("Scheduled messages") }, onClick = { menuOpen = false; onOpenScheduled() })
                    DropdownMenuItem(text = { Text("Recycle bin") }, onClick = { menuOpen = false; onOpenRecycleBin() })
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; onOpenSettings() })
                }
            }
        },
    )
}

@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit) {
    TopAppBar(
        title = { Text("$count selected") },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Cancel selection") }
        },
    )
}

@Composable
private fun SelectionBottomBar(onMute: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit) {
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { Icon(icon, contentDescription = label) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

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
    SwipeToDismissBox(state = dismissState, backgroundContent = { SwipeBackground(dismissState.dismissDirection) }) {
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
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
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
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No messages yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text("Tap the chat bubble to start a conversation", style = MaterialTheme.typography.bodyMedium)
    }
}
