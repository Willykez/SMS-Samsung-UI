package com.oneui.sms.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.oneui.sms.data.local.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoriesScreen(
    enabled: Boolean,
    categories: List<CategoryEntity>,
    onSetEnabled: (Boolean) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversation categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("On") },
                supportingContent = { Text("Organize your conversations into categories so you can find them easily") },
                trailingContent = { Switch(checked = enabled, onCheckedChange = onSetEnabled) },
            )

            if (enabled) {
                ListItem(
                    headlineContent = { Text("Add category") },
                    leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable { showAddDialog = true },
                )
                LazyColumn {
                    items(categories, key = { it.id }) { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            trailingContent = {
                                IconButton(onClick = { onDeleteCategory(category.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete category")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            var text by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add category") },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Category name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (text.isNotBlank()) onAddCategory(text.trim())
                        showAddDialog = false
                    }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
            )
        }
    }
}
