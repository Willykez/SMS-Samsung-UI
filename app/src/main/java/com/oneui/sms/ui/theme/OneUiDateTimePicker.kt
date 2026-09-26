package com.oneui.sms.ui.theme

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

/**
 * Compose-native One UI inspired date/time picker based on the supplied catalog.
 * It intentionally avoids SESL/legacy AndroidX forks so the application keeps
 * one coherent modern AndroidX dependency graph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneUiDateTimePickerDialog(
    initialAt: Long,
    onDismiss: () -> Unit,
    onPicked: (Long) -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    val initial = remember(initialAt) {
        Calendar.getInstance().apply { timeInMillis = initialAt }
    }
    val initialDateMillis = remember(initialAt) {
        val localDate = Instant.ofEpochMilli(initialAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    val timeState = rememberTimePickerState(
        initialHour = initial.get(Calendar.HOUR_OF_DAY),
        initialMinute = initial.get(Calendar.MINUTE),
        is24Hour = DateFormat.is24HourFormat(context),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OneUiTokens.DialogShape,
        title = {
            PickerHeader(
                title = if (step == 0) "Choose date" else "Choose time",
                subtitle = if (step == 0) "When should this message be sent?" else "Set the send time",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step == 0) {
                    DatePicker(
                        state = dateState,
                        modifier = Modifier.fillMaxWidth(),
                        showModeToggle = false,
                    )
                } else {
                    TimePicker(
                        state = timeState,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (step == 0) {
                    if (dateState.selectedDateMillis != null) step = 1
                } else {
                    val selected = Calendar.getInstance().apply {
                        timeInMillis = dateState.selectedDateMillis ?: initialDateMillis
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (selected > System.currentTimeMillis()) onPicked(selected) else onDismiss()
                }
            }) {
                Text(if (step == 0) "Next" else "Done")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.End) {
                if (step == 1) {
                    TextButton(onClick = { step = 0 }) { Text("Back") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun PickerHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
