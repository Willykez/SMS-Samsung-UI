# Feature Spec — Samsung Messages Settings Parity (SMS-only scope)

Source: guidingtech.com "15 Samsung Text Message Settings" — mapped against the
existing scaffold (`SmsRepository`, Room `ConversationEntity`/`MessageEntity`).
None of these require RCS/MMS.

| # | Feature | Data model change needed | Notes |
|---|---------|--------------------------|-------|
| 1 | Schedule messages | New `scheduledAt: Long?` + status `SCHEDULED` on outbox row | WorkManager one-time job fires `SmsManager.sendTextMessage` at the target time; needs a "Scheduled messages" list screen |
| 2 | Star messages | `isStarred: Boolean` on `MessageEntity` | Long-press context menu action; "Starred messages" filtered list screen |
| 3 | Reminder on a message | New `ReminderEntity(messageId, remindAt)` table | AlarmManager/WorkManager to fire a notification at `remindAt` |
| 4 | Pin messages (within a thread) | `isPinned: Boolean` + `pinOrder: Int` on `MessageEntity` | **Distinct from conversation pinning** (already have `isPinned` on `ConversationEntity`) — this pins individual *messages* to the top of a thread, with manual reordering |
| 5 | Mute conversations | Already have `isMuted` on `ConversationEntity` | ✅ no schema change — just needs the UI toggle wired |
| 6 | Custom notification sound per contact | `notificationSoundUri: String?` on `ConversationEntity` | Android notification channels are per-app in 8+, so this needs per-conversation `NotificationChannel` creation |
| 7 | Change message background/theme per chat | `chatColorHex: String?` on `ConversationEntity` | Purely a bubble-tint override read in `MessageThreadScreen` |
| 8 | Increase font size (pinch zoom) | None | UI-only: a `fontScale` state read by `MessageBubble`/composer text |
| 9 | Recycle bin (30-day soft delete) | `deletedAt: Long?` on both entities | Delete = set timestamp, not row removal; list queries filter `WHERE deletedAt IS NULL`; daily WorkManager job hard-deletes rows older than 30 days |
| 10 | View unread messages only | None (uses existing `unreadCount`) | UI filter toggle in `ConversationListViewModel` |
| 11 | Auto-delete old messages | New `SettingsEntity` (or DataStore) with `retentionDays: Int?` | Periodic WorkManager job purges messages older than the threshold |
| 12 | Search within a chat thread | New DAO query: `messages WHERE threadId = :id AND body LIKE '%query%'` | Search icon in the thread's top bar, not just the conversation list (list-level search already scaffolded) |
| 13 | Add thread to home screen | No data model change | Android `ShortcutManager` pinned shortcut, or an App Widget (`GlanceAppWidget` for Compose) |
| 14 | Quick responses (canned replies) | New `QuickResponseEntity(id, text)` table | Settings screen to manage the list + a picker sheet above the composer |
| 15 | Disable web link preview | Part of the same `SettingsEntity` — `showLinkPreviews: Boolean` | When on, `MessageBubble` detects URLs and renders an `OG`-scrape preview card (needs a lightweight URL-metadata fetch) |

## New shared pieces this implies
- **`SettingsEntity`/DataStore** — one place for `retentionDays`, `showLinkPreviews`, and any other global toggle, read via a `SettingsRepository`.
- **Two WorkManager jobs**: scheduled-send (per-message, one-shot) and retention-purge (periodic, daily).
- **Notification channel-per-conversation** — needed for #6, and useful groundwork for #3's reminder notifications too.
- Room schema bumps to v2+ with migrations, since #1, #2, #4, #6, #7, #9 all touch existing entities.

## Suggested build order
Roughly cheapest → most involved, and grouping shared infra together:
1. **No-schema-change UI wins**: #5 (mute — already modeled), #8 (font size), #10 (unread filter)
2. **Single-field additions**: #2 (star), #7 (chat background), #6 (notification sound)
3. **Soft-delete infra**: #9 (recycle bin) — unlocks #11 (auto-delete) cheaply once the `deletedAt` pattern exists
4. **New small tables**: #14 (quick responses), #12 (in-thread search)
5. **Scheduling infra**: #1 (schedule messages), #3 (reminders) — share the WorkManager one-shot pattern
6. **More involved**: #4 (pinned messages w/ reordering), #15 (link previews — needs network fetch + caching), #13 (widget/shortcut — separate Android surface)

Want me to start on group 1–2 now, or do you want to review/adjust this list first?
