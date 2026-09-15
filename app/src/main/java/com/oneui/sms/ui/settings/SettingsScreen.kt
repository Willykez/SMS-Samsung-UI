@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oneui.sms.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
}

private val AUTO_DELETE_OPTIONS = listOf(null, 30, 90, 365) // #11: Never / 30d / 90d / 1yr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsEntity,
    onBack: () -> Unit,
    onOpenQuickResponses: () -> Unit,
    onSetRecycleBinEnabled: (Boolean) -> Unit,
    onSetAutoDeleteDays: (Int?) -> Unit,
    onSetShowLinkPreviews: (Boolean) -> Unit,
    onSetFontScale: (Float) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            ListItem(
                headlineContent = { Text("Quick responses") }, // #14
                supportingContent = { Text("Manage your pre-written replies") },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = {
                    TextButton(onClick = onOpenQuickResponses) { Text("Edit") }
                },
            )

            ListItem(
                headlineContent = { Text("Recycle bin") }, // #9
                supportingContent = { Text("Keep deleted messages for 30 days before permanent removal") },
                trailingContent = {
                    Switch(checked = settings.recycleBinEnabled, onCheckedChange = onSetRecycleBinEnabled)
                },
            )

            AutoDeleteRow(currentDays = settings.autoDeleteDays, onSelect = onSetAutoDeleteDays) // #11

            ListItem(
                headlineContent = { Text("Preview web links from contacts") }, // #15
                trailingContent = {
                    Switch(checked = settings.showLinkPreviews, onCheckedChange = onSetShowLinkPreviews)
                },
            )

            FontScaleRow(currentScale = settings.fontScale, onChange = onSetFontScale) // #8
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoDeleteRow(currentDays: Int?, onSelect: (Int?) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val label = when (currentDays) {
        null -> "Never"
        30 -> "After 30 days"
        90 -> "After 90 days"
        365 -> "After 1 year"
        else -> "After $currentDays days"
    }

    ListItem(
        headlineContent = { Text("Delete old messages") },
        supportingContent = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
            androidx.compose.foundation.layout.Box {
                TextButton(onClick = { menuOpen = true }) { Text("Change") }
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

@Composable
private fun FontScaleRow(currentScale: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Message text size", style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("A", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = currentScale,
                onValueChange = onChange,
                valueRange = 0.85f..1.5f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text("A", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
