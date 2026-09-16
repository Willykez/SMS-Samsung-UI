@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oneui.sms.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.sms.data.SettingsRepository
import com.oneui.sms.data.local.SettingsEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<SettingsEntity> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    fun setRecycleBinEnabled(enabled: Boolean) = viewModelScope.launch { repository.setRecycleBinEnabled(enabled) }
    fun setAutoDeleteDays(days: Int?) = viewModelScope.launch { repository.setAutoDeleteDays(days) }
    fun setShowLinkPreviews(show: Boolean) = viewModelScope.launch { repository.setShowLinkPreviews(show) }
    fun setFontScale(scale: Float) = viewModelScope.launch { repository.setFontScale(scale) }
    fun setCategoriesEnabled(enabled: Boolean) = viewModelScope.launch { repository.setCategoriesEnabled(enabled) }
    fun setRemoveLocationFromSharedImages(remove: Boolean) = viewModelScope.launch { repository.setRemoveLocationFromSharedImages(remove) }
}

private val AUTO_DELETE_OPTIONS = listOf(null, 30, 90, 365)

/** Top-level "Messages settings" screen — grouped cards match the reference exactly. */
@Composable
fun SettingsScreen(
    settings: SettingsEntity,
    onBack: () -> Unit,
    onOpenStub: (String) -> Unit,
    onOpenMoreSettings: () -> Unit,
    onSetRecycleBinEnabled: (Boolean) -> Unit,
    onSetCategoriesEnabled: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            SettingsGroup {
                SettingsRow(title = "Chat settings", onClick = { onOpenStub("Chat settings") })
            }
            Spacer12()
            SettingsGroup {
                SettingsRow(
                    title = "Conversation categories",
                    trailing = { Switch(checked = settings.categoriesEnabled, onCheckedChange = onSetCategoriesEnabled) },
                )
                Divider()
                SettingsRow(
                    title = "Recycle bin",
                    subtitle = "Keep deleted messages for 30 days.",
                    trailing = { Switch(checked = settings.recycleBinEnabled, onCheckedChange = onSetRecycleBinEnabled) },
                )
            }
            Spacer12()
            SettingsGroup {
                SettingsRow(title = "Notifications", onClick = { onOpenStub("Notifications") })
                Divider()
                SettingsRow(title = "Block numbers and spam", onClick = { onOpenStub("Block numbers and spam") })
                Divider()
                SettingsRow(title = "More settings", onClick = onOpenMoreSettings)
                Divider()
                SettingsRow(title = "Emergency alert history", onClick = { onOpenStub("Emergency alert history") })
            }
            Spacer12()
            SettingsGroup {
                SettingsRow(title = "About Messages", onClick = { onOpenStub("About Messages") })
            }
        }
    }
}

/** Secondary "More settings" screen — copy matches the reference exactly. */
@Composable
fun MoreSettingsScreen(
    settings: SettingsEntity,
    onBack: () -> Unit,
    onOpenStub: (String) -> Unit,
    onOpenQuickResponses: () -> Unit,
    onSetShowLinkPreviews: (Boolean) -> Unit,
    onSetRemoveLocation: (Boolean) -> Unit,
    onSetAutoDeleteDays: (Int?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            SettingsGroup {
                SettingsRow(title = "Text messages", onClick = { onOpenStub("Text messages") })
                Divider()
                SettingsRow(title = "Multimedia messages", onClick = { onOpenStub("Multimedia messages") })
            }
            Spacer12()
            SettingsGroup {
                SettingsRow(title = "Quick responses", onClick = onOpenQuickResponses)
                Divider()
                SettingsRow(title = "Push messages", subtitle = "Prompt", subtitleIsLink = true, onClick = { onOpenStub("Push messages") })
                Divider()
                SettingsRow(title = "Broadcast channels", subtitle = "Off", subtitleIsLink = true, onClick = { onOpenStub("Broadcast channels") })
                Divider()
                SettingsRow(
                    title = "Preview web links from contacts",
                    trailing = { Switch(checked = settings.showLinkPreviews, onCheckedChange = onSetShowLinkPreviews) },
                )
                Divider()
                SettingsRow(
                    title = "Remove location from shared images",
                    trailing = { Switch(checked = settings.removeLocationFromSharedImages, onCheckedChange = onSetRemoveLocation) },
                )
                Divider()
                AutoDeleteRow(currentDays = settings.autoDeleteDays, onSelect = onSetAutoDeleteDays)
            }
        }
    }
}

@Composable
private fun AutoDeleteRow(currentDays: Int?, onSelect: (Int?) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Column {
        SettingsRow(
            title = "Delete old messages",
            subtitle = "Delete your oldest messages to make room for new ones — configurable retention window",
            trailing = {
                androidx.compose.foundation.layout.Box {
                    Switch(checked = currentDays != null, onCheckedChange = { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        AUTO_DELETE_OPTIONS.forEach { days ->
                            DropdownMenuItem(
                                text = { Text(if (days == null) "Never" else "After $days days") },
                                onClick = { menuOpen = false; onSelect(days) },
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    subtitleIsLink: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth(),
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let {
            {
                Text(
                    it,
                    color = if (subtitleIsLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = trailing,
    )
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun Spacer12() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 6.dp))
}
