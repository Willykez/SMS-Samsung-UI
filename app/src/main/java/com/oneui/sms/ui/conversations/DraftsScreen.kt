@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.oneui.sms.ui.conversations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneui.sms.data.SmsRepository

@Composable fun DraftsScreen(repo: SmsRepository, onBack: () -> Unit, onOpen: (Long) -> Unit) {
 val drafts by repo.observeDrafts().collectAsState(initial=emptyList())
 Scaffold(topBar={TopAppBar(title={Text("Drafts")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}})}){p->
  if(drafts.isEmpty()) Box(Modifier.fillMaxSize().padding(p), contentAlignment=androidx.compose.ui.Alignment.Center){Text("No drafts", color=MaterialTheme.colorScheme.onSurfaceVariant)}
  else LazyColumn(Modifier.fillMaxSize().padding(p).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){items(drafts,key={it.threadId}){d->ListItem(leadingContent={Icon(Icons.Filled.Edit,null)},headlineContent={Text(d.address)},supportingContent={Text(d.body,maxLines=2)},modifier=Modifier.fillMaxWidth().clickable{onOpen(d.threadId)})}}
 }
}
