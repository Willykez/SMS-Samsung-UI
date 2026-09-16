package com.oneui.sms.ui.contacts

import android.content.Context
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.sms.data.SmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactRow(val name: String, val phoneNumber: String)

class ContactsViewModel(private val context: Context, private val repository: SmsRepository) : ViewModel() {
    private val _contacts = MutableStateFlow<List<ContactRow>>(emptyList())
    val contacts: StateFlow<List<ContactRow>> = _contacts

    init {
        viewModelScope.launch {
            _contacts.value = withContext(Dispatchers.IO) { loadContacts() }
        }
    }

    private fun loadContacts(): List<ContactRow> {
        val result = mutableListOf<ContactRow>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        )
        cursor?.use {
            val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                result += ContactRow(it.getString(nameIdx) ?: "", it.getString(numberIdx) ?: "")
            }
        }
        return result.distinctBy { it.phoneNumber }
    }

    suspend fun getOrCreateThreadId(number: String): Long = repository.getOrCreateThreadId(number)
}

@Composable
fun ContactsScreen(
    contacts: List<ContactRow>,
    onOpenThread: (threadId: Long, address: String) -> Unit,
    resolveThreadId: suspend (String) -> Long,
) {
    val scope = rememberCoroutineScopeCompat()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(contacts, key = { it.phoneNumber }) { contact ->
            ListItem(
                leadingContent = {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(contact.name.take(1).uppercase())
                        }
                    }
                },
                headlineContent = { Text(contact.name) },
                supportingContent = { Text(contact.phoneNumber) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val threadId = resolveThreadId(contact.phoneNumber)
                        onOpenThread(threadId, contact.phoneNumber)
                    }
                },
            )
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
