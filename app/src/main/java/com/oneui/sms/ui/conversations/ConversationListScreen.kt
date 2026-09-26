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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneui.sms.data.local.CategoryEntity
import com.oneui.sms.data.local.ConversationEntity
import com.oneui.sms.data.local.MessageEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class InboxFilter { ALL, UNREAD, PINNED, OTP, TRANSACTIONS }

enum class BottomTab { CONVERSATIONS, CONTACTS }

@Composable
fun ConversationListScreen(
    conversations: List<ConversationEntity>,
    categories: List<CategoryEntity>,
    categoriesEnabled: Boolean,
    currentCategory: String,
    selectedIds: Set<Long>,
    totalUnreadCount: Int,
    searchResults: List<MessageEntity>,
    onSearchQueryChange: (String) -> Unit,
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
    onOpenArchive: () -> Unit,
    onOpenDrafts: () -> Unit,
    onOpenEditCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenContacts: () -> Unit,
    onMuteSelected: (Boolean) -> Unit,
    onDeleteSelected: () -> Unit,
    onPinSelected: (Boolean) -> Unit,
    onArchiveConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inSelectionMode = selectedIds.isNotEmpty()
    var bottomTab by remember { mutableStateOf(BottomTab.CONVERSATIONS) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(InboxFilter.ALL) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filtered = remember(conversations, query, filter) {
        conversations.filter { conversation ->
            val text = "${conversation.displayName ?: ""} ${conversation.address} ${conversation.snippet}".lowercase()
            val matchesQuery = query.isBlank() || text.contains(query.trim().lowercase())
            val matchesFilter = when (filter) {
                InboxFilter.ALL -> true
                InboxFilter.UNREAD -> conversation.unreadCount > 0
                InboxFilter.PINNED -> conversation.isPinned
                InboxFilter.OTP -> looksLikeOtp(conversation.snippet)
                InboxFilter.TRANSACTIONS -> looksLikeTransaction(conversation.snippet)
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (inSelectionMode) {
                SelectionTopBar(count = selectedIds.size, onClose = onClearSelection)
            } else {
                InboxHeader(
                    searchOpen = searchOpen,
                    query = query,
                    onQueryChange = { query = it; onSearchQueryChange(it) },
                    onOpenSearch = { searchOpen = true },
                    onCloseSearch = { searchOpen = false; query = "" },
                    onOpenUnread = onOpenUnread,
                    onOpenSettings = onOpenSettings,
                    onOpenEditCategories = onOpenEditCategories,
                    onOpenStarred = onOpenStarred,
                    onOpenScheduled = onOpenScheduled,
                    onOpenRecycleBin = onOpenRecycleBin,
                    onOpenArchive = onOpenArchive,
                    onOpenDrafts = onOpenDrafts,
                    onMarkAllRead = onMarkAllRead,
                    onReorderPinned = { scope.launch { snackbarHostState.showSnackbar("Pinned conversations are already grouped at the top") } },
                )
            }
        },
        floatingActionButton = {
            if (!inSelectionMode && bottomTab == BottomTab.CONVERSATIONS) {
                FloatingActionButton(
                    onClick = onCompose,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.ChatBubble, contentDescription = "New message")
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
            } else {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = bottomTab == BottomTab.CONVERSATIONS,
                        onClick = { bottomTab = BottomTab.CONVERSATIONS },
                        icon = {
                            BadgedBox(badge = { if (totalUnreadCount > 0) Badge { Text(totalUnreadCount.toString()) } }) {
                                Icon(Icons.Filled.Forum, contentDescription = null)
                            }
                        },
                        label = { Text("Chats") },
                    )
                    NavigationBarItem(
                        selected = bottomTab == BottomTab.CONTACTS,
                        onClick = { bottomTab = BottomTab.CONTACTS; onOpenContacts() },
                        icon = { Icon(Icons.Filled.Contacts, contentDescription = null) },
                        label = { Text("Contacts") },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!inSelectionMode) {
                InboxFilterRow(selected = filter, onSelect = { filter = it })
                if (categoriesEnabled) {
                    CategoryTabRow(categories, currentCategory, onSelectCategory, onOpenEditCategories)
                }
            }

            if (query.isNotBlank() && searchResults.isNotEmpty()) {
                SearchResultSection(searchResults = searchResults, onOpen = onOpenConversation)
            } else if (filtered.isEmpty()) {
                EmptyState(searching = query.isNotBlank() || filter != InboxFilter.ALL)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.threadId }, contentType = { "conversation" }) { convo ->
                        val isSelected = convo.threadId in selectedIds
                        if (inSelectionMode) {
                            ConversationRow(convo, { onToggleSelected(convo.threadId) }, selectable = true, isSelected = isSelected)
                        } else {
                            SwipeableConversationRow(
                                conversation = convo,
                                onOpen = { onOpenConversation(convo.threadId) },
                                onLongPress = { onToggleSelected(convo.threadId) },
                                onArchive = { onArchiveConversation(convo.threadId) },
                                onDelete = { onDeleteConversation(convo.threadId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxHeader(
    searchOpen: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onOpenUnread: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditCategories: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenDrafts: () -> Unit,
    onMarkAllRead: () -> Unit,
    onReorderPinned: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    if (searchOpen) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search messages, people, numbers") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = { IconButton(onClick = onCloseSearch) { Icon(Icons.Filled.Close, contentDescription = "Close search") } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {}
    } else {
        TopAppBar(
            title = {
                Column {
                    Text("Messages", fontWeight = FontWeight.Bold)
                    Text("Your conversations", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            actions = {
                IconButton(onClick = onOpenSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
                IconButton(onClick = onOpenUnread) { Icon(Icons.Filled.MarkChatUnread, contentDescription = "Unread messages") }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Mark all as read") }, onClick = { menuOpen = false; onMarkAllRead() })
                        DropdownMenuItem(text = { Text("Edit categories") }, onClick = { menuOpen = false; onOpenEditCategories() })
                        DropdownMenuItem(text = { Text("Reorder pinned") }, onClick = { menuOpen = false; onReorderPinned() })
                        DropdownMenuItem(text = { Text("Starred messages") }, onClick = { menuOpen = false; onOpenStarred() })
                        DropdownMenuItem(text = { Text("Scheduled messages") }, onClick = { menuOpen = false; onOpenScheduled() })
                        DropdownMenuItem(text = { Text("Recycle bin") }, onClick = { menuOpen = false; onOpenRecycleBin() })
                        DropdownMenuItem(text = { Text("Archived") }, onClick = { menuOpen = false; onOpenArchive() })
                        DropdownMenuItem(text = { Text("Drafts") }, onClick = { menuOpen = false; onOpenDrafts() })
                        DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; onOpenSettings() })
                    }
                }
            },
        )
    }
}


@Composable
private fun SearchResultSection(searchResults: List<MessageEntity>, onOpen: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
        Text("Messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
        searchResults.take(12).forEach { message ->
            Surface(Modifier.fillMaxWidth().padding(vertical = 3.dp).combinedClickable(onClick = { onOpen(message.threadId) }, onLongClick = {}), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(12.dp)) { Text(message.address, style = MaterialTheme.typography.labelMedium); Text(message.body, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(formatTime(message.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

private fun looksLikeOtp(text: String): Boolean = Regex("\\b(?:code|otp|verification|passcode|pin)\\b.*\\d{4,8}|\\d{4,8}.*\\b(?:code|otp|verification)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
private fun looksLikeTransaction(text: String): Boolean = Regex("\\b(?:paid|payment|transaction|debited|credited|balance|airtime|deposit|withdraw|invoice|receipt)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)

@Composable
private fun InboxFilterRow(selected: InboxFilter, onSelect: (InboxFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { FilterPill("All", selected == InboxFilter.ALL) { onSelect(InboxFilter.ALL) } }
        item { FilterPill("Unread", selected == InboxFilter.UNREAD) { onSelect(InboxFilter.UNREAD) } }
        item { FilterPill("Pinned", selected == InboxFilter.PINNED) { onSelect(InboxFilter.PINNED) } }
        item { FilterPill("OTP", selected == InboxFilter.OTP) { onSelect(InboxFilter.OTP) } }
        item { FilterPill("Transactions", selected == InboxFilter.TRANSACTIONS) { onSelect(InboxFilter.TRANSACTIONS) } }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = {}),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CategoryTabRow(categories: List<CategoryEntity>, current: String, onSelect: (String) -> Unit, onAddClick: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { CategoryTab("All", current == CATEGORY_ALL) { onSelect(CATEGORY_ALL) } }
        items(categories, key = { it.id }) { category -> CategoryTab(category.name, current == category.name) { onSelect(category.name) } }
        item { IconButton(onClick = onAddClick, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, contentDescription = "Add category", modifier = Modifier.size(18.dp)) } }
    }
}

@Composable
private fun CategoryTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = {}), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(width = if (selected) 24.dp else 0.dp, height = 3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit) {
    TopAppBar(title = { Text("$count selected", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Cancel selection") } })
}

@Composable
private fun SelectionBottomBar(onMute: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit) {
    Surface(tonalElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BottomBarAction(Icons.Filled.NotificationsOff, "Mute", onMute)
            BottomBarAction(Icons.Filled.Delete, "Delete", onDelete)
            BottomBarAction(Icons.Filled.PushPin, "Pin", onPin)
        }
    }
}

@Composable
private fun BottomBarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { Icon(icon, contentDescription = label) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SwipeableConversationRow(conversation: ConversationEntity, onOpen: () -> Unit, onLongPress: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        when (value) {
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
            SwipeToDismissBoxValue.StartToEnd -> { onArchive(); true }
            else -> false
        }
    })
    SwipeToDismissBox(state = dismissState, backgroundContent = { SwipeBackground(dismissState.dismissDirection) }) {
        ConversationRow(conversation, onOpen, onLongPress)
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val start = direction == SwipeToDismissBoxValue.StartToEnd
    Row(Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(if (start) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (start) Arrangement.Start else Arrangement.End) {
        Text(if (start) "Archive" else "Delete", fontWeight = FontWeight.SemiBold, color = if (start) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun ConversationRow(conversation: ConversationEntity, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, selectable: Boolean = false, isSelected: Boolean = false) {
    val title = conversation.displayName ?: conversation.address
    val rowModifier = if (onLongClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier.combinedClickable(onClick = onClick)
    val avatarColor = MaterialTheme.colorScheme.primaryContainer
    Surface(modifier = rowModifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
        ListItem(
            leadingContent = {
                if (selectable) {
                    Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                } else {
                    Surface(shape = CircleShape, color = avatarColor, modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(title.trim().take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(formatTime(conversation.timestamp), style = MaterialTheme.typography.labelSmall, color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            supportingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(conversation.snippet, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (conversation.isPinned) Icon(Icons.Filled.PushPin, contentDescription = "Pinned", modifier = Modifier.padding(start = 6.dp).size(15.dp))
                    if (conversation.isMuted) Icon(Icons.Filled.NotificationsOff, contentDescription = "Muted", modifier = Modifier.padding(start = 6.dp).size(15.dp))
                    if (conversation.unreadCount > 0) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp)) {
                            Text(conversation.unreadCount.coerceAtMost(99).toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (searching) Icons.Filled.Search else Icons.Filled.ChatBubble, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        Text(if (searching) "No conversations found" else "No messages yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(if (searching) "Try another name, number, or keyword." else "Start a conversation with the + button.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val sameDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date) == SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now)
    return if (sameDay) SimpleDateFormat("h:mm a", Locale.getDefault()).format(date) else SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
}
