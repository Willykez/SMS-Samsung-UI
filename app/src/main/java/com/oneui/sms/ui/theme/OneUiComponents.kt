package com.oneui.sms.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Compose translation of the supplied One UI Design catalog.
 *
 * The reference library is a legacy View/XML component set. These components
 * preserve its interaction language without adding a second UI toolkit to
 * the Compose application.
 */
object OneUiTokens {
    val PageHorizontal = 14.dp
    val GroupGap = 12.dp
    val RowMinHeight = 56.dp
    val ControlSize = 48.dp
    val PillShape = RoundedCornerShape(20.dp)
    val CardShape = RoundedCornerShape(24.dp)
    val ComposerShape = RoundedCornerShape(26.dp)
}

@Composable
fun OneUiSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = OneUiTokens.CardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        content = content,
    )
}

@Composable
fun OneUiSettingRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(24.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (summary != null) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (checked != null && onCheckedChange != null) {
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        }
    }
}

@Composable
fun OneUiPill(
    text: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val content = @Composable {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = OneUiTokens.PillShape,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            border = if (selected) null
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = OneUiTokens.PillShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}

@Composable
fun OneUiChipRow(
    values: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = OneUiTokens.PageHorizontal),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(values) { value ->
            OneUiPill(
                text = value,
                selected = value == selected,
                onClick = { onSelected(value) },
            )
        }
    }
}
