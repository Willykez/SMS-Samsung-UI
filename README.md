# OneMessages — a One UI–styled SMS client

A native Android (Kotlin + Jetpack Compose) SMS app styled after Samsung
Messages / One UI. **Scope is intentionally narrow: plain SMS only.** No RCS,
no MMS, no cloud backend, no Signal Protocol — those were in the research
reports but are out of scope for this build.

## Why native Android, and why no backend

SMS lives in the OS. `Telephony.Sms` (the system content provider) *is* the
source of truth — there's no server to build, no message queue, no
Cassandra/ScyllaDB cluster. That whole side of the research reports doesn't
apply here. Because SMS access requires the OS-level default-SMS-app role,
this has to be a native Android app — it isn't reachable from Flutter/React
Native without a native module anyway, so plain Kotlin keeps things simplest.

## Architecture

```
ui/            Jetpack Compose screens + ViewModels (StateFlow)
data/          SmsRepository — bridges Telephony provider <-> Room cache
data/local/    Room database (fast, reactive local cache of threads/messages)
receiver/      SMS_DELIVER receiver + required MMS/quick-reply stubs
```

- **SmsRepository** reads/writes `Telephony.Sms` directly and mirrors results
  into Room so the UI binds to a `Flow` and updates reactively — no server
  round-trip.
- **Outbox pattern** (scaled down from the report): sending writes a
  `PENDING` row immediately so the bubble appears instantly, then updates to
  `SENT`/`FAILED` once `SmsManager` confirms.
- **Default SMS app role**: Android requires `SmsDeliverReceiver` (inbound),
  a stub MMS receiver (`WAP_PUSH_DELIVER` — required to even be *offered* as
  a default SMS app candidate, even though MMS itself is unsupported here),
  and `HeadlessSmsSendService` (quick-reply from the call screen). All three
  are in the manifest.

## One UI design elements implemented

- Two-zone layout: collapsible **Viewing Area** (`LargeTopAppBar` that
  shrinks on scroll) + a lower **Interaction Area** pinned to the thumb-reach
  zone with ≥48dp touch targets (composer row).
- Heavily rounded containers (custom `Shapes`) instead of hard grid lines.
- Swipe-to-archive / swipe-to-delete on the conversation list.
- `LazyColumn` with `key` + `contentType` on both the conversation list and
  message thread, per the report's Compose recomposition-optimization
  guidance, so scrolling stays smooth as history grows.
- Dynamic color (Material You) with a One UI–style blue fallback for
  pre-Android-12 devices.

## Visual design pass (matched against reference Samsung Messages screenshots)

The first build matched feature *logic* but used generic Material 3
defaults instead of the actual One UI visual language. This pass pulled
concrete details from reference screenshots:

- **Category tabs** (All / Personal / Shipping / OTP / custom, with "+" to
  add) on the inbox, backed by a real `CategoryEntity` table
- **Bottom Conversations/Contacts nav** with an unread-count badge, FAB
  restyled to a chat-bubble icon anchored above it
- **Exact overflow-menu order**: Delete / Mark all as read / Edit
  categories / Reorder pinned / Starred messages / Scheduled messages /
  Recycle bin / Settings
- **Dedicated Unread messages screen** (back arrow + list), not just an
  inline filter toggle
- **Edit categories screen** — On/Off toggle, "+ Add category" row, add
  dialog — matches the reference flow
- **Settings restructured into grouped cards** matching the reference
  copy verbatim (including "Keep deleted messages for 30 days.", the
  "More settings" row list with Push messages/Broadcast channels labels, etc.)
- **Contacts tab** — real device-contacts list, tapping one opens/creates a thread
- Light theme background switched to the lavender-tinted grey the
  screenshots use instead of pure white

**Honest scope note**: several Settings rows shown in the reference
(Chat settings, Notifications, Block numbers and spam, Emergency alert
history, About Messages, Text/Multimedia messages, Push messages,
Broadcast channels) are wired to a `StubScreen` — present and
navigable for visual/structural completeness, but with no real
functionality behind them, since they're outside this app's SMS-only
feature scope. "Reorder pinned" similarly shows a "coming soon"
snackbar rather than actual drag-to-reorder.

## Feature coverage (see FEATURE_SPEC.md for the full mapping)

All 15 Samsung Messages settings from the spec are now wired end-to-end
(Room schema, repository methods, ViewModels, and screens):

1. Schedule messages — composer "+" menu, `ScheduledSendWorker`, Scheduled messages list
2. Star messages — long-press a bubble, "Starred messages" list
3. Reminders — `ReminderEntity`, AlarmManager scheduling (`ReminderScheduler`), and a "Remind me…" long-press action on any message bubble
4. Pin conversations to top — multi-select bottom bar (matches the reference screenshots)
5. Mute conversations — multi-select bottom bar + per-thread mute in the thread's overflow menu
6. Custom notification sound per contact — per-thread notification channel created on demand (`NotificationChannels.ensureConversationChannel`); **the ringtone-picker UI itself isn't hooked up yet** (needs `RingtoneManager.ACTION_RINGTONE_PICKER`, an Activity-level intent)
7. Chat background/bubble color — thread overflow menu → color picker
8. Font size — Settings screen slider, applied live to message bubbles
9. Recycle bin (30-day soft delete) — conversation delete + message delete both route here; daily purge via `RetentionPurgeWorker`
10. View unread only — filter icon in the conversation list header
11. Auto-delete old messages — Settings screen, shares the same daily worker as #9
12. Search within a chat thread — search icon in the thread's top bar
13. Home screen widget/shortcut — **not yet built** (separate Android surface: `ShortcutManager` or a Glance widget)
14. Quick responses — composer "+" menu → picker chips; managed from Settings
15. Disable web link preview — Settings toggle; **the preview-card rendering itself isn't built yet** (the toggle just gates a feature that doesn't exist yet)

### Genuinely not yet built
- **Runtime permission dialogs** (READ_SMS/SEND_SMS/RECEIVE_SMS/READ_CONTACTS) — the default-SMS-app role request and the contact picker are both wired, but the runtime permission prompts still need to precede first use.
- **Contact resolution** — `displayName` is still null; needs a `ContactsContract` lookup by phone number (same API already used in the new-conversation picker — just needs to run for every conversation row too).
- **#6 notification-sound picker UI** — channel creation exists; needs the system ringtone picker intent + a way to change an already-created channel's sound (user must do that via system settings once a channel exists — Android limitation, not ours).
- **#13 widget/shortcut** — separate Android surface (App Widget or pinned shortcut).
- **#15 link preview cards** — needs a lightweight URL-metadata fetch + cache.
- **Dual-pane tablet/fold layout** (600dp breakpoint) — currently single-pane only.

## Setup

1. Open in Android Studio (Koala+), let Gradle sync.
2. Run on a device/emulator with a SIM or SMS capability (Android emulators
   can send/receive SMS to each other via the emulator console).
3. On first launch you'll be prompted to set the app as your default SMS
   app — required before `Telephony.Sms` becomes writable/readable for send.
