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

## Modern UI pass

The current 0.2.0 design pass adds:

- Samsung One UI-inspired hierarchy, rounded surfaces and thumb-friendly controls.
- Rich inbox filters: All, Unread, Pinned, OTP and Transactions.
- SMS-native OTP detection with one-tap copy.
- SMS character and segment feedback in the composer.
- Modern conversation header with avatar, SMS status and mute controls.
- Chat appearance controls and per-thread accent color.
- Rich long-press actions: copy, star, reminder, delete and message info.
- Quick-response/template workflow and scheduled SMS composer.
- Richer settings surfaces for appearance, notifications, spam/blocking and SMS delivery.
- SMS-only product language throughout the UI; no RCS/MMS composer or fake chat features.


The latest visual pass keeps the One UI/Samsung Messages ergonomic philosophy but gives the app its own more modern identity:

- Large, compact inbox hierarchy with a clearer "Messages / Your conversations" header
- Dedicated in-app search for names, phone numbers and message snippets
- Capsule filters for All / Unread / Pinned conversations
- Cleaner category tabs with stronger selected-state treatment
- Richer conversation rows with avatar initials, time, unread count, pinned/muted indicators and better typography
- Improved empty/search states instead of blank lists
- More expressive rounded surfaces, primary/secondary containers and a consistent SMS blue accent
- Refined thread bubbles with asymmetrical corners, timestamps and delivery state indicators
- Long-press message actions now include Copy text alongside Star, Reminder and Delete
- Thumb-friendly composer with quick responses, scheduling actions and a long-SMS character hint
- Edge-to-edge window support for modern Android devices
- No fake RCS/MMS UI: the product remains explicitly SMS-first

The Gradle wrapper executable bit is also restored in the project archive. Build verification is limited by this environment because the Gradle distribution cannot be downloaded without network access.

## 0.4.0 Samsung Messages-style redesign + local SMS/contact sync

This pass changes the inbox visual language to match the supplied Samsung Messages references: large unread hero, compact action row, All/Personal/Shipping/OTP category tabs, flat conversation rows, circular contact photos, unread count badges, and the Conversations/Contacts bottom navigation.

The inbox now performs a real read-through sync from the device Telephony SMS provider and resolves each sender through the local Contacts provider using `PhoneLookup.DISPLAY_NAME` and `PHOTO_URI`. The user is asked for SMS and Contacts runtime permissions before entering the inbox. Existing local conversation state (pin, archive, mute, category, color) is preserved during refresh.

## 0.3.0 Deep Functionality Pass

This release turns the modern UI into a more complete SMS product while preserving the explicit SMS-only scope.

### Implemented product areas
- Modern One UI/Google Messages-inspired inbox hierarchy and filters
- Smart inbox classification for OTP and transaction-like SMS
- Global message search over cached SMS bodies, plus conversation search
- Persistent drafts and a dedicated Drafts screen
- Swipe archive/delete with undo-ready architecture and an Archived screen
- Conversation multi-select for pin/mute/delete/category actions
- Persistent blocked-number list with incoming-SMS suppression
- Incoming SMS notifications with optional inline quick reply
- Sent and delivered SMS status callbacks through SmsManager PendingIntents
- OTP extraction and one-tap code copying
- Scheduled SMS with cancel, send-now, and reschedule actions
- Starred messages and message reminders
- Conversation contact/profile surface and call action
- Message information surface with status, timestamp, characters, and SMS segments
- Quick replies/templates
- Per-conversation mute and accent color customization
- Light, dark, system, and AMOLED appearance modes
- Notification, SMS delivery, spam, appearance, and messaging settings persisted in Room
- SMS character/segment feedback in the composer
- SMS-native long-message controls and no RCS/MMS feature UI

### Platform notes
OneMessages remains a default-SMS-app candidate and retains the minimal MMS/WAP-PUSH stub required by Android role eligibility; MMS content is not parsed, stored, displayed, or composed by the app.

## 0.5.0 Advanced One UI 4 Compose pass

The supplied open-source One UI design catalog was reviewed and translated into
Compose-native primitives. The app remains a single Compose UI stack instead
of mixing the legacy View/XML library with Compose.

The conversation screen now has grouped asymmetric bubbles, date separators,
explicit delivery failure state, an adaptive keyboard-aware composer, rounded
search, contextual quick-response chips, schedule state, and IME-aware send
actions. See `ONE_UI_ADVANCED_PASS.md` for the component-by-component mapping.
