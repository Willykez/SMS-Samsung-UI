@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.oneui.sms.ui.conversations
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.data.local.ConversationEntity
import kotlinx.coroutines.launch

@Composable fun ArchiveScreen(repo: SmsRepository, onBack: () -> Unit, onOpen: (Long) -> Unit) {
 val items by repo.observeArchived().collectAsState(initial=emptyList()); val scope=rememberCoroutineScope()
 Scaffold(topBar={TopAppBar(title={Text("Archived")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}})}){p->
  if(items.isEmpty()) Box(Modifier.fillMaxSize().padding(p), contentAlignment=androidx.compose.ui.Alignment.Center){Text("No archived conversations", color=MaterialTheme.colorScheme.onSurfaceVariant)}
  else LazyColumn(Modifier.fillMaxSize().padding(p).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){items(items,key={it.threadId}){c->ListItem(headlineContent={Text(c.displayName?:c.address)},supportingContent={Text(c.snippet,maxLines=1)},trailingContent={TextButton(onClick={scope.launch{repo.archive(c.threadId,false)}}){Text("Unarchive")}},modifier=Modifier.fillMaxWidth().clickable{onOpen(c.threadId)})}}
 }
}
