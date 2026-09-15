package com.oneui.sms.ui.thread

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneui.sms.data.local.DeliveryStatus
import com.oneui.sms.data.local.MessageEntity

/**
 * Splits the screen per the One UI pattern: an upper Viewing Area (the message
 * stream, via TopAppBar + scrolling list) and a lower Interaction Area strictly
 * within thumb reach (composer row, min 48dp touch targets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageThreadScreen(
    contactName: String,
    messages: List<MessageEntity>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(contactName, fontWeight = FontWeight.SemiBold) })
        },
        bottomBar = {
            InteractionBar(draft = draft, onDraftChange = onDraftChange, onSend = onSend)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                MessageBubble(message)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val bubbleColor = if (message.isOutgoing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(alignment)
                .widthIn(max = 280.dp),
        ) {
            Surface(
                color = bubbleColor,
                shape = MaterialTheme.shapes.large, // fully rounded, One UI-style
            ) {
                Text(
                    message.body,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            if (message.isOutgoing && message.status == DeliveryStatus.FAILED) {
                Text(
                    "Not sent — tap to retry",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp, end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun InteractionBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    // Everything here sits in the lower thumb-sweep zone with >=48dp targets,
    // matching the report's "Interact Naturally" ergonomic requirement.
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { /* attach photo/file — out of scope for SMS-only build */ }) {
                Icon(Icons.Filled.Add, contentDescription = "Attach")
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(),
            )
            IconButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
