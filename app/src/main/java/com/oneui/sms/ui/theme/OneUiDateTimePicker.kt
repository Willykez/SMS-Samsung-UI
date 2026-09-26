package com.oneui.sms.ui.theme

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
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
 * Compose port of the supplied One UI picker catalog's two-stage date/time
 * interaction. It deliberately uses the current AndroidX/Material3 picker
 * primitives so it can coexist with ComponentActivity/Compose, while retaining
 * the catalog's large header, rounded surface, primary action and two-step flow.
 */
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

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { if (dateState.selectedDateMillis != null) step = 1 },
                ) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        ) {
            Surface(
                modifier = Modifier.wrapContentHeight(),
                shape = OneUiTokens.DialogShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    PickerHeader(title = "Choose date", subtitle = "When should this message be sent?")
                    DatePicker(
                        state = dateState,
                        modifier = Modifier.fillMaxWidth(),
                        showModeToggle = false,
                    )
                }
            }
        }
    } else {
        TimePickerDialog(
            onDismiss = onDismiss,
            confirmButton = {
                Button(onClick = {
                    val selected = Calendar.getInstance().apply {
                        val millis = dateState.selectedDateMillis ?: initialDateMillis
                        timeInMillis = millis
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (selected > System.currentTimeMillis()) onPicked(selected) else onDismiss()
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        ) {
            Surface(
                modifier = Modifier.wrapContentHeight(),
                shape = OneUiTokens.DialogShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PickerHeader(title = "Choose time", subtitle = "Set the send time")
                    TimePicker(state = timeState, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { step = 0 }) { Text("Back") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
