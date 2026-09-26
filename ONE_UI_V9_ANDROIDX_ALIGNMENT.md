# OneMessages v0.9.0 — AndroidX / One UI picker alignment

## Why the SESL dependency was removed

The supplied SESL artifacts fork AndroidX classes while retaining `androidx.*` package names. In this Compose app, `androidx.activity:activity-compose:1.9.1` requires the modern `androidx.core` APIs including `OnConfigurationChangedProvider`, `OnTrimMemoryProvider`, `OnNewIntentProvider`, `OnMultiWindowModeChangedProvider`, `OnPictureInPictureModeChangedProvider`, `OnUserLeaveHintProvider`, and `MenuHost`.

Using SESL `core:1.3.0` as a replacement for official `androidx.core` therefore makes `ComponentActivity` impossible to compile. Keeping both SESL and official copies creates duplicate classes.

The v0.9 architecture keeps one coherent official AndroidX classpath:

- `androidx.core:core-ktx:1.13.1`
- `androidx.activity:activity-compose:1.9.1`
- Compose / Material 3 / Navigation / Lifecycle / Room / WorkManager official AndroidX artifacts

No SESL forked AndroidX artifacts are placed on the application classpath.

## One UI picker requirement

The supplied catalog's date/time picker interaction is ported into `OneUiDateTimePickerDialog` as a Compose-native component. It retains the catalog's two-stage date → time flow, prominent header, large touch targets, rounded dialog surface, explicit primary action, Back/Cancel behavior, and future-time validation.

This avoids importing a legacy View/XML fork that conflicts with the application's modern AndroidX/Compose runtime.
