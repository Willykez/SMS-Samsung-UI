@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.oneui.sms.ui.conversations

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
private const val SMART_PERSONAL = "Personal"
private const val SMART_SHIPPING = "Shipping"
private const val SMART_OTP = "OTP"

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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filtered = remember(conversations, query, filter, currentCategory) {
        conversations.filter { c ->
            val haystack = "${c.displayName.orEmpty()} ${c.address} ${c.snippet}".lowercase()
            val queryMatches = query.isBlank() || haystack.contains(query.trim().lowercase())
            val filterMatches = when (filter) {
                InboxFilter.ALL -> true
                InboxFilter.UNREAD -> c.unreadCount > 0
                InboxFilter.PINNED -> c.isPinned
                InboxFilter.OTP -> looksLikeOtp(c.snippet)
                InboxFilter.TRANSACTIONS -> looksLikeTransaction(c.snippet)
            }
            val categoryMatches = when (currentCategory) {
                "All" -> true
                SMART_PERSONAL -> !looksLikeOtp(c.snippet) && !looksLikeShipping(c.snippet) && !looksLikeTransaction(c.snippet)
                SMART_SHIPPING -> looksLikeShipping(c.snippet)
                SMART_OTP -> looksLikeOtp(c.snippet)
                else -> c.category == currentCategory
            }
            queryMatches && filterMatches && categoryMatches
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (inSelectionMode) SelectionTopBar(selectedIds.size, onClearSelection)
            else if (searchOpen) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it; onSearchQueryChange(it) },
                    onSearch = {},
                    active = true,
                    onActiveChange = { if (!it) { searchOpen = false; query = "" } },
                    placeholder = { Text("Search messages, people or numbers") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                ) {}
            }
        },
        floatingActionButton = {
            if (!inSelectionMode && bottomTab == BottomTab.CONVERSATIONS) {
                FloatingActionButton(
                    onClick = onCompose,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                ) { Icon(Icons.Filled.ChatBubbleOutline, "New message") }
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
                        icon = { BadgedBox({ if (totalUnreadCount > 0) Badge { Text(totalUnreadCount.coerceAtMost(999).toString()) } }) { Icon(Icons.Filled.Forum, null) } },
                        label = { Text("Conversations") },
                    )
                    NavigationBarItem(
                        selected = bottomTab == BottomTab.CONTACTS,
                        onClick = { bottomTab = BottomTab.CONTACTS; onOpenContacts() },
                        icon = { Icon(Icons.Filled.Contacts, null) },
                        label = { Text("Contacts") },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!inSelectionMode && !searchOpen) {
                UnreadHero(totalUnreadCount, onOpenUnread)
                ActionRow(
                    onFilter = { filter = if (filter == InboxFilter.ALL) InboxFilter.UNREAD else InboxFilter.ALL },
                    onSearch = { searchOpen = true },
                    onMore = { scope.launch { snackbar.showSnackbar("Use the menu from the three-dot action to manage messages") } },
                    onMarkAllRead = onMarkAllRead,
                    onOpenUnread = onOpenUnread,
                    onOpenSettings = onOpenSettings,
                    onOpenStarred = onOpenStarred,
                    onOpenScheduled = onOpenScheduled,
                    onOpenRecycleBin = onOpenRecycleBin,
                    onOpenArchive = onOpenArchive,
                    onOpenDrafts = onOpenDrafts,
                    onOpenEditCategories = onOpenEditCategories,
                )
                if (categoriesEnabled) {
                    CategoryTabRow(
                        categories = categories,
                        current = currentCategory,
                        onSelect = onSelectCategory,
                        onAddClick = onOpenEditCategories,
                    )
                }
            }

            if (searchOpen && query.isNotBlank() && searchResults.isNotEmpty()) {
                SearchResultSection(searchResults, onOpenConversation, conversations)
            } else if (filtered.isEmpty()) {
                EmptyState(searching = query.isNotBlank() || filter != InboxFilter.ALL)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(filtered, key = { it.threadId }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = { onOpenConversation(conversation.threadId) },
                            onLongClick = { onToggleSelected(conversation.threadId) },
                            selected = conversation.threadId in selectedIds,
                            onArchive = { onArchiveConversation(conversation.threadId) },
                            onDelete = { onDeleteConversation(conversation.threadId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnreadHero(totalUnreadCount: Int, onView: () -> Unit) {
    if (totalUnreadCount <= 0) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$totalUnreadCount unread\nmessage${if (totalUnreadCount == 1) "" else "s"}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Surface(
            onClick = onView,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        ) { Text("View", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
    }
}

@Composable
private fun ActionRow(
    onFilter: () -> Unit,
    onSearch: () -> Unit,
    onMore: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenUnread: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenDrafts: () -> Unit,
    onOpenEditCategories: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onFilter) { Icon(Icons.Filled.FilterList, "Unread filter") }
        IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Search") }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                MenuItem("Mark all as read", Icons.Filled.MarkChatUnread) { menuOpen = false; onMarkAllRead() }
                MenuItem("Unread messages", Icons.Filled.MarkChatUnread) { menuOpen = false; onOpenUnread() }
                MenuItem("Starred messages", Icons.Filled.Star) { menuOpen = false; onOpenStarred() }
                MenuItem("Scheduled messages", Icons.Filled.Schedule) { menuOpen = false; onOpenScheduled() }
                MenuItem("Archived", Icons.Filled.Forum) { menuOpen = false; onOpenArchive() }
                MenuItem("Drafts", Icons.Filled.ChatBubbleOutline) { menuOpen = false; onOpenDrafts() }
                MenuItem("Recycle bin", Icons.Filled.Delete) { menuOpen = false; onOpenRecycleBin() }
                MenuItem("Conversation categories", Icons.Filled.Add) { menuOpen = false; onOpenEditCategories() }
                MenuItem("Settings", Icons.Filled.Settings) { menuOpen = false; onOpenSettings() }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

@Composable
private fun CategoryTabRow(categories: List<CategoryEntity>, current: String, onSelect: (String) -> Unit, onAddClick: () -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item { CategoryTab("All", current == "All") { onSelect("All") } }
        item { CategoryTab(SMART_PERSONAL, current == SMART_PERSONAL) { onSelect(SMART_PERSONAL) } }
        item { CategoryTab(SMART_SHIPPING, current == SMART_SHIPPING) { onSelect(SMART_SHIPPING) } }
        item { CategoryTab(SMART_OTP, current == SMART_OTP) { onSelect(SMART_OTP) } }
        items(categories.filter { it.name !in listOf(SMART_PERSONAL, SMART_SHIPPING, SMART_OTP) }, key = { it.id }) { category ->
            CategoryTab(category.name, current == category.name) { onSelect(category.name) }
        }
        item { IconButton(onClick = onAddClick, modifier = Modifier.size(34.dp)) { Icon(Icons.Filled.Add, "Add category") } }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CategoryTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.combinedClickable(onClick = onClick, onLongClick = {}),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.height(3.dp).width(if (selected) 28.dp else 0.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    selected: Boolean,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = remember(conversation.photoUri) {
        conversation.photoUri?.let { uri ->
            runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use(BitmapFactory::decodeStream) }.getOrNull()
        }
    }
    val title = conversation.displayName?.takeIf { it.isNotBlank() } ?: conversation.address
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background,
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(title, bitmap, conversation.chatColorHex)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(formatTime(conversation.timestamp), style = MaterialTheme.typography.labelMedium, color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(conversation.snippet, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (conversation.isPinned) Icon(Icons.Filled.PushPin, "Pinned", modifier = Modifier.padding(start = 7.dp).size(16.dp))
                    if (conversation.isMuted) Icon(Icons.Filled.NotificationsOff, "Muted", modifier = Modifier.padding(start = 7.dp).size(16.dp))
                    if (conversation.unreadCount > 0) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(start = 8.dp)) {
                            Text(conversation.unreadCount.coerceAtMost(99).toString(), color = MaterialTheme.colorScheme.onTertiary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(title: String, bitmap: android.graphics.Bitmap?, colorHex: String?) {
    val background = parseColor(colorHex) ?: MaterialTheme.colorScheme.surfaceVariant
    Surface(shape = CircleShape, color = background, modifier = Modifier.size(52.dp)) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = title, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(title.trim().take(1).uppercase(Locale.getDefault()), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun parseColor(hex: String?): androidx.compose.ui.graphics.Color? = hex?.removePrefix("#")?.let { value ->
    runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#$value")) }.getOrNull()
}

@Composable
private fun SearchResultSection(results: List<MessageEntity>, onOpen: (Long) -> Unit, conversations: List<ConversationEntity>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
        Text("Message results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
        results.take(20).forEach { message ->
            val name = conversations.firstOrNull { it.threadId == message.threadId }?.displayName ?: message.address
            Row(Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(message.threadId) }, onLongClick = {} ).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(message.body, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit) {
    TopAppBar(title = { Text("$count selected", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close selection") } })
}

@Composable
private fun SelectionBottomBar(onMute: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit) {
    Surface(tonalElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SelectionAction(Icons.Filled.NotificationsOff, "Mute", onMute)
            SelectionAction(Icons.Filled.Delete, "Delete", onDelete)
            SelectionAction(Icons.Filled.PushPin, "Pin", onPin)
        }
    }
}

@Composable
private fun SelectionAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(onClick = onClick) { Icon(icon, label) }; Text(label, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun EmptyState(searching: Boolean) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(if (searching) Icons.Filled.Search else Icons.Filled.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(14.dp))
        Text(if (searching) "No conversations found" else "No messages yet", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        Text(if (searching) "Try another name, number, or keyword." else "Start a conversation with the compose button.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun looksLikeOtp(text: String): Boolean = Regex("\\b(?:code|otp|verification|passcode|pin)\\b.*\\d{4,8}|\\d{4,8}.*\\b(?:code|otp|verification)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
private fun looksLikeTransaction(text: String): Boolean = Regex("\\b(?:paid|payment|transaction|debited|credited|balance|airtime|deposit|withdraw|invoice|receipt)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
private fun looksLikeShipping(text: String): Boolean = Regex("\\b(?:delivery|delivered|shipment|shipping|parcel|package|courier|tracking|dispatch|pickup)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val fmt = if (now - timestamp < 24 * 60 * 60 * 1000L) SimpleDateFormat("h:mm a", Locale.getDefault()) else SimpleDateFormat("d MMM", Locale.getDefault())
    return fmt.format(Date(timestamp))
}
