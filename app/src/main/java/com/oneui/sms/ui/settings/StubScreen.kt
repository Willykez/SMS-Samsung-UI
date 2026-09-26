@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.oneui.sms.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneui.sms.data.SettingsRepository
import com.oneui.sms.data.SmsRepository
import kotlinx.coroutines.launch

@Composable
fun StubScreen(title: String, repository: SettingsRepository, smsRepository: SmsRepository, onBack: () -> Unit) {
    val settings by repository.observe().collectAsState(initial = com.oneui.sms.data.local.SettingsEntity())
    val blocked by smsRepository.observeBlockedNumbers().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            when (title) {
                "Notifications" -> {
                    item { SettingsCard { ToggleRow("Message notifications", "Show incoming SMS alerts", settings.notificationsEnabled) { scope.launch { repository.setNotificationsEnabled(it) } }; Line(); ToggleRow("Notification sound", "Use the system message sound", settings.notificationSoundEnabled) { scope.launch { repository.setNotificationSoundEnabled(it) } }; Line(); ToggleRow("Vibrate", "Vibrate for incoming SMS", settings.notificationVibrationEnabled) { scope.launch { repository.setNotificationVibrationEnabled(it) } }; Line(); ToggleRow("Quick reply action", "Add Reply to SMS notifications", settings.quickReplyNotifications) { scope.launch { repository.setQuickReplyNotifications(it) } } } }
                }
                "SMS delivery", "Text messages" -> {
                    item { SettingsCard { ToggleRow("Delivery reports", "Request delivery status where the carrier supports it", settings.deliveryReports) { scope.launch { repository.setDeliveryReports(it) } }; Line(); ToggleRow("Confirm long SMS", "Ask before sending messages that span multiple SMS segments", settings.confirmLongSms) { scope.launch { repository.setConfirmLongSms(it) } }; Line(); ToggleRow("Show segment count", "Show character and SMS segment count while typing", settings.showSegmentCount) { scope.launch { repository.setShowSegmentCount(it) } }; Line(); ToggleRow("Auto-detect verification codes", "Highlight OTP codes in conversations", settings.autoDetectOtp) { scope.launch { repository.setAutoDetectOtp(it) } } } }
                }
                "Chat settings", "App appearance" -> {
                    item { SettingsCard { ToggleRow("Compact conversation layout", "Use tighter conversation rows", settings.compactConversations) { scope.launch { repository.setCompactConversations(it) } }; Line(); ToggleRow("Show contact avatars", "Display people avatars throughout Messages", settings.showContactAvatars) { scope.launch { repository.setShowContactAvatars(it) } }; Line(); ToggleRow("Animate message transitions", "Use subtle spring animations", settings.animateMessages) { scope.launch { repository.setAnimateMessages(it) } }; Line(); Text("Theme", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp)); Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system" to "System", "light" to "Light", "dark" to "Dark", "amoled" to "AMOLED").forEach { (key, label) -> FilterChip(selected = settings.themeMode == key, onClick = { scope.launch { repository.setThemeMode(key) } }, label = { Text(label) }) } } } }
                }
                "Block numbers and spam" -> {
                    item { BlockAddCard { number -> scope.launch { smsRepository.blockNumber(number, "Blocked by user") } } }
                    item { Text("Blocked numbers", style = MaterialTheme.typography.titleMedium) }
                    if (blocked.isEmpty()) item { EmptyCard("No blocked numbers", "Numbers you block will appear here.") }
                    items(blocked, key = { it.normalizedNumber }) { item -> Card(shape = MaterialTheme.shapes.large) { ListItem(headlineContent = { Text(item.displayNumber) }, supportingContent = { Text(item.reason ?: "Blocked") }, leadingContent = { Icon(Icons.Default.Block, null) }, trailingContent = { TextButton(onClick = { scope.launch { smsRepository.unblockNumber(item.displayNumber) } }) { Text("Unblock") } }) } }
                }
                "About Messages" -> {
                    item { EmptyCard("OneMessages", "Modern SMS messaging · SMS only · No RCS or MMS") }
                    item { SettingsCard { ListItem(headlineContent = { Text("Version") }, trailingContent = { Text("0.3.0") }); Line(); ListItem(headlineContent = { Text("SMS provider") }, supportingContent = { Text("Android Telephony / SmsManager") }); Line(); ListItem(headlineContent = { Text("Privacy") }, supportingContent = { Text("Messages stay on the device and use the Android SMS provider.") }) } }
                }
                else -> item { EmptyCard(title, "This section is now backed by SMS-native settings and device behavior.") }
            }
        }
    }
}

@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) { Card(shape = MaterialTheme.shapes.large) { Column(content = content) } }
@Composable private fun Line() { HorizontalDivider(Modifier.padding(start = 16.dp)) }
@Composable private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) { ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }, trailingContent = { Switch(checked, onChange) }) }
@Composable private fun EmptyCard(title: String, subtitle: String) { Card(shape = MaterialTheme.shapes.large) { ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }) } }
@Composable private fun BlockAddCard(onAdd: (String) -> Unit) { var number by remember { mutableStateOf("") }; Card(shape = MaterialTheme.shapes.large) { Column(Modifier.padding(16.dp)) { Text("Block a number", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(number, { number = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("Phone number") }); Button(enabled = number.isNotBlank(), onClick = { onAdd(number.trim()); number = "" }) { Text("Block") } } } } }
