# OneMessages v0.6.0 — build and UI fixes

## CI failure fixed

`ScheduledMessagesScreen.kt` previously embedded the SESL date/time picker construction directly inside the `AlertDialog` button lambda. The block was malformed in the generated Kotlin source and caused KSP to fail for both debug and release.

The picker flow is now isolated in `openOneUiDateTimePicker(...)`, which keeps the Compose dialog readable and makes the SESL integration reusable.

## Release configuration

- Version code: 6
- Version name: 0.6.0
- R8 minification remains enabled for release.
- Resource shrinking remains enabled.
- Release CI runs tests and `assembleRelease` and uploads the APK plus R8 mapping/seeds/usage reports.

## One UI picker behavior

Scheduling and rescheduling use the supplied `io.github.oneuiproject.sesl:picker-basic` date/time dialogs. The selected date and time are combined into one timestamp and rejected when it is not in the future.

## UI pass

- Scheduled-message rows now use the shared One UI rounded section-card language.
- Date/time has stronger hierarchy and One UI accent treatment.
- Actions are text-forward and touch-friendly instead of three dense icon buttons.
- Shared One UI shape tokens include list-item and dialog shapes for further screens.
