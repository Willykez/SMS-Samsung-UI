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

## Not yet built (next steps, roughly in priority order)

1. **Permissions flow** — runtime request for `READ_SMS`/`SEND_SMS`/
   `RECEIVE_SMS`/`READ_CONTACTS` (default-SMS-app role request is wired up in
   `MainActivity`, but the runtime permission dialogs still need to be added
   before first use).
2. **Contact resolution** — `displayName` is currently null; needs a
   `ContactsContract` lookup by phone number.
3. **New conversation / contact picker** for the compose FAB.
4. **Search** — the Room DAO already has a `search()` query; needs wiring to
   a search UI in the collapsed header.
5. **Dual-pane layout** for tablets/foldables (window-size-class breakpoint
   at 600dp, per the report) — currently single-pane only.
6. **Notifications** — heads-up notification with quick-reply `RemoteInput`
   on incoming SMS (stub comment left in `SmsDeliverReceiver`).
7. **Pin/mute/archive actions** — DAO methods exist; need to be wired to
   swipe actions and a long-press context menu.

## Setup

1. Open in Android Studio (Koala+), let Gradle sync.
2. Run on a device/emulator with a SIM or SMS capability (Android emulators
   can send/receive SMS to each other via the emulator console).
3. On first launch you'll be prompted to set the app as your default SMS
   app — required before `Telephony.Sms` becomes writable/readable for send.
