package com.oneui.sms.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.ui.conversations.ConversationListScreen
import com.oneui.sms.ui.conversations.ConversationListViewModel
import com.oneui.sms.ui.thread.MessageThreadScreen
import com.oneui.sms.ui.thread.MessageThreadViewModel

private object Routes {
    const val LIST = "list"
    const val THREAD = "thread/{threadId}/{address}"
    fun thread(threadId: Long, address: String) = "thread/$threadId/$address"
}

@Composable
fun OneMessagesNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val repository = remember { SmsRepository(context.applicationContext) }

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            val vm = remember { ConversationListViewModel(repository) }
            val conversations by vm.conversations.collectAsState()

            ConversationListScreen(
                conversations = conversations,
                onOpenConversation = { threadId ->
                    val address = conversations.first { it.threadId == threadId }.address
                    navController.navigate(Routes.thread(threadId, address))
                },
                onCompose = { /* TODO: contact picker -> new thread */ },
                onArchive = { /* TODO: repository.archive(it) */ },
                onDelete = { /* TODO: repository.delete(it) */ },
            )
        }

        composable(Routes.THREAD) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId")?.toLongOrNull() ?: return@composable
            val address = backStackEntry.arguments?.getString("address") ?: ""
            val vm = remember(threadId) { MessageThreadViewModel(repository, threadId, address) }
            val messages by vm.messages.collectAsState()
            val draft by vm.draft.collectAsState()

            MessageThreadScreen(
                contactName = address,
                messages = messages,
                draft = draft,
                onDraftChange = vm::onDraftChange,
                onSend = vm::send,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
