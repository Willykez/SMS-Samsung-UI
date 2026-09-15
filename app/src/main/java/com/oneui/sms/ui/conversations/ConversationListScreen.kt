package com.oneui.sms.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneui.sms.data.local.ConversationEntity

/**
 * One UI-inspired conversation list.
 *
 * Uses a large collapsible Material 3 top app bar and a conversation list
 * beneath it. Conversations support swipe-to-delete and swipe-to-archive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    conversations: List<ConversationEntity>,
    onOpenConversation: (Long) -> Unit,
    onCompose: () -> Unit,
    onArchive: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topBarState = rememberTopAppBarState()

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Search action can be connected when search UI
                            // is implemented.
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCompose,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New message",
                )
            }
        },
    ) { padding ->
        if (conversations.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    vertical = 4.dp,
                ),
            ) {
                items(
                    items = conversations,
                    key = { it.threadId },
                ) { conversation ->
                    SwipeableConversationRow(
                        conversation = conversation,
                        onOpen = {
                            onOpenConversation(conversation.threadId)
                        },
                        onArchive = {
                            onArchive(conversation.threadId)
                        },
                        onDelete = {
                            onDelete(conversation.threadId)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationRow(
    conversation: ConversationEntity,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    onArchive()
                    true
                }

                else -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeBackground(
                direction = dismissState.dismissDirection,
            )
        },
    ) {
        ConversationRow(
            conversation = conversation,
            onClick = onOpen,
        )
    }
}

@Composable
private fun SwipeBackground(
    direction: SwipeToDismissBoxValue,
) {
    val (backgroundColor, label) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd ->
            MaterialTheme.colorScheme.primary to "Archive"

        SwipeToDismissBoxValue.EndToStart ->
            MaterialTheme.colorScheme.error to "Delete"

        else ->
            MaterialTheme.colorScheme.surface to ""
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Arrangement.Start
            } else {
                Arrangement.End
            },
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationRow(
    conversation: ConversationEntity,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (
                            conversation.displayName
                                ?: conversation.address
                            )
                            .take(1)
                            .uppercase(),
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = conversation.displayName ?: conversation.address,
                fontWeight =
                    if (conversation.unreadCount > 0) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
            )
        },
        supportingContent = {
            Text(
                text = conversation.snippet,
                maxLines = 1,
                fontWeight =
                    if (conversation.unreadCount > 0) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
            )
        },
        trailingContent = {
            if (conversation.unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 2.dp,
                        ),
                    )
                }
            }
        },
    )
}

@Composable
private fun EmptyState(
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No messages yet",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = "Tap + to start a conversation",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}