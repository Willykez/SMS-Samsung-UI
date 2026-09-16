package com.oneui.sms.ui.navigation

import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oneui.sms.data.SettingsRepository
import com.oneui.sms.data.SmsRepository
import com.oneui.sms.ui.contacts.ContactsScreen
import com.oneui.sms.ui.contacts.ContactsViewModel
import com.oneui.sms.ui.conversations.ConversationListScreen
import com.oneui.sms.ui.conversations.ConversationListViewModel
import com.oneui.sms.ui.conversations.EditCategoriesScreen
import com.oneui.sms.ui.conversations.UnreadMessagesScreen
import com.oneui.sms.ui.conversations.UnreadMessagesViewModel
import com.oneui.sms.ui.recyclebin.RecycleBinScreen
import com.oneui.sms.ui.recyclebin.RecycleBinViewModel
import com.oneui.sms.ui.scheduled.ScheduledMessagesScreen
import com.oneui.sms.ui.scheduled.ScheduledMessagesViewModel
import com.oneui.sms.ui.settings.MoreSettingsScreen
import com.oneui.sms.ui.settings.QuickResponsesScreen
import com.oneui.sms.ui.settings.QuickResponsesViewModel
import com.oneui.sms.ui.settings.SettingsScreen
import com.oneui.sms.ui.settings.SettingsViewModel
import com.oneui.sms.ui.settings.StubScreen
import com.oneui.sms.ui.starred.StarredMessagesScreen
import com.oneui.sms.ui.starred.StarredMessagesViewModel
import com.oneui.sms.ui.thread.MessageThreadScreen
import com.oneui.sms.ui.thread.MessageThreadViewModel
import kotlinx.coroutines.launch

private object Routes {
    const val LIST = "list"
    const val THREAD = "thread/{threadId}/{address}"
    const val STARRED = "starred"
    const val SCHEDULED = "scheduled"
    const val RECYCLE_BIN = "recycle_bin"
    const val SETTINGS = "settings"
    const val MORE_SETTINGS = "more_settings"
    const val QUICK_RESPONSES = "quick_responses"
    const val UNREAD = "unread"
    const val EDIT_CATEGORIES = "edit_categories"
    const val CONTACTS = "contacts"
    const val STUB = "stub/{title}"
    fun thread(threadId: Long, address: String) = "thread/$threadId/$address"
    fun stub(title: String) = "stub/$title"
}

@Composable
fun OneMessagesNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val repository = remember { SmsRepository(context.applicationContext) }
    val settingsRepository = remember { SettingsRepository(context.applicationContext) }

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            val vm = remember { ConversationListViewModel(repository, settingsRepository) }
            val conversations by vm.conversations.collectAsState()
            val categories by vm.categories.collectAsState()
            val categoriesEnabled by vm.categoriesEnabled.collectAsState()
            val currentCategory by vm.currentCategory.collectAsState()
            val selectedIds by vm.selectedIds.collectAsState()
            val totalUnread by vm.totalUnreadCount.collectAsState()
            val scope = rememberCoroutineScope()

            val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        val hasPhone = it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                        if (hasPhone > 0) {
                            val phones = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null,
                            )
                            phones?.use { p ->
                                if (p.moveToFirst()) {
                                    val number = p.getString(p.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                    scope.launch {
                                        val threadId = repository.getOrCreateThreadId(number)
                                        navController.navigate(Routes.thread(threadId, number))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ConversationListScreen(
                conversations = conversations,
                categories = categories,
                categoriesEnabled = categoriesEnabled,
                currentCategory = currentCategory,
                selectedIds = selectedIds,
                totalUnreadCount = totalUnread,
                onSelectCategory = vm::selectCategory,
                onOpenConversation = { threadId ->
                    val address = conversations.first { it.threadId == threadId }.address
                    navController.navigate(Routes.thread(threadId, address))
                },
                onCompose = { contactPicker.launch(null) },
                onToggleSelected = vm::toggleSelected,
                onClearSelection = vm::clearSelection,
                onMarkAllRead = vm::markAllAsRead,
                onOpenUnread = { navController.navigate(Routes.UNREAD) },
                onOpenStarred = { navController.navigate(Routes.STARRED) },
                onOpenScheduled = { navController.navigate(Routes.SCHEDULED) },
                onOpenRecycleBin = { navController.navigate(Routes.RECYCLE_BIN) },
                onOpenEditCategories = { navController.navigate(Routes.EDIT_CATEGORIES) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                onMuteSelected = vm::muteSelected,
                onDeleteSelected = vm::deleteSelected,
                onPinSelected = vm::pinSelected,
            )
        }

        composable(Routes.THREAD) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId")?.toLongOrNull() ?: return@composable
            val address = backStackEntry.arguments?.getString("address") ?: ""
            val vm = remember(threadId) { MessageThreadViewModel(repository, settingsRepository, threadId, address) }
            val messages by vm.messages.collectAsState()
            val draft by vm.draft.collectAsState()
            val isSearching by vm.isSearching.collectAsState()
            val fontScale by vm.fontScale.collectAsState()
            val quickResponses by vm.quickResponses.collectAsState()
            val pendingScheduleTime by vm.pendingScheduleTime.collectAsState()
            val conversation by vm.conversation.collectAsState()

            MessageThreadScreen(
                contactName = address,
                messages = messages,
                draft = draft,
                isSearching = isSearching,
                fontScale = fontScale,
                quickResponses = quickResponses,
                pendingScheduleTime = pendingScheduleTime,
                chatColorHex = conversation?.chatColorHex,
                onSetChatColor = vm::setChatColor,
                isMuted = conversation?.isMuted ?: false,
                onSetMuted = vm::setMuted,
                onSetReminder = vm::setReminder,
                onDraftChange = vm::onDraftChange,
                onSearchQueryChange = vm::onSearchQueryChange,
                onToggleSearch = { vm.onSearchQueryChange("") },
                onSend = vm::send,
                onToggleStar = vm::toggleStar,
                onDeleteMessage = vm::deleteMessage,
                onPickQuickResponse = vm::pickQuickResponse,
                onSetScheduleTime = vm::setScheduleTime,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.STARRED) {
            val vm = remember { StarredMessagesViewModel(repository) }
            val starred by vm.starred.collectAsState()
            StarredMessagesScreen(
                messages = starred,
                onUnstar = vm::unstar,
                onBack = { navController.popBackStack() },
                onOpenThread = { threadId ->
                    val address = starred.first { it.threadId == threadId }.address
                    navController.navigate(Routes.thread(threadId, address))
                },
            )
        }

        composable(Routes.SCHEDULED) {
            val vm = remember { ScheduledMessagesViewModel(repository) }
            val scheduled by vm.scheduled.collectAsState()
            ScheduledMessagesScreen(messages = scheduled, onCancel = vm::cancel, onBack = { navController.popBackStack() })
        }

        composable(Routes.RECYCLE_BIN) {
            val vm = remember { RecycleBinViewModel(repository) }
            val deletedConversations by vm.deletedConversations.collectAsState()
            val deletedMessages by vm.deletedMessages.collectAsState()
            RecycleBinScreen(
                conversations = deletedConversations,
                messages = deletedMessages,
                onRestoreConversation = vm::restoreConversation,
                onRestoreMessage = vm::restoreMessage,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            val vm = remember { SettingsViewModel(settingsRepository) }
            val settings by vm.settings.collectAsState()
            SettingsScreen(
                settings = settings,
                onBack = { navController.popBackStack() },
                onOpenStub = { title -> navController.navigate(Routes.stub(title)) },
                onOpenMoreSettings = { navController.navigate(Routes.MORE_SETTINGS) },
                onSetRecycleBinEnabled = vm::setRecycleBinEnabled,
                onSetCategoriesEnabled = vm::setCategoriesEnabled,
            )
        }

        composable(Routes.MORE_SETTINGS) {
            val vm = remember { SettingsViewModel(settingsRepository) }
            val settings by vm.settings.collectAsState()
            MoreSettingsScreen(
                settings = settings,
                onBack = { navController.popBackStack() },
                onOpenStub = { title -> navController.navigate(Routes.stub(title)) },
                onOpenQuickResponses = { navController.navigate(Routes.QUICK_RESPONSES) },
                onSetShowLinkPreviews = vm::setShowLinkPreviews,
                onSetRemoveLocation = vm::setRemoveLocationFromSharedImages,
                onSetAutoDeleteDays = vm::setAutoDeleteDays,
            )
        }

        composable(Routes.QUICK_RESPONSES) {
            val vm = remember { QuickResponsesViewModel(settingsRepository) }
            val responses by vm.responses.collectAsState()
            QuickResponsesScreen(responses = responses, onAdd = vm::add, onDelete = vm::delete, onBack = { navController.popBackStack() })
        }

        composable(Routes.UNREAD) {
            val vm = remember { UnreadMessagesViewModel(repository) }
            val unread by vm.unread.collectAsState()
            UnreadMessagesScreen(
                conversations = unread,
                onOpenConversation = { threadId ->
                    val address = unread.first { it.threadId == threadId }.address
                    navController.navigate(Routes.thread(threadId, address))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.EDIT_CATEGORIES) {
            val vm = remember { ConversationListViewModel(repository, settingsRepository) }
            val categories by vm.categories.collectAsState()
            val categoriesEnabled by vm.categoriesEnabled.collectAsState()
            EditCategoriesScreen(
                enabled = categoriesEnabled,
                categories = categories,
                onSetEnabled = vm::setCategoriesEnabled,
                onAddCategory = vm::addCategory,
                onDeleteCategory = vm::deleteCategory,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.CONTACTS) {
            val vm = remember { ContactsViewModel(context.applicationContext, repository) }
            val contacts by vm.contacts.collectAsState()
            ContactsScreen(
                contacts = contacts,
                onOpenThread = { threadId, address -> navController.navigate(Routes.thread(threadId, address)) },
                resolveThreadId = { number -> vm.getOrCreateThreadId(number) },
            )
        }

        composable(
            Routes.STUB,
            arguments = listOf(navArgument("title") { type = NavType.StringType }),
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "Settings"
            StubScreen(title = title, onBack = { navController.popBackStack() })
        }
    }
}
