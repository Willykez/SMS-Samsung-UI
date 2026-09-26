# OneMessages v0.7.0 — CI/compiler fixes

## Fixed

- Added `androidx.appcompat:appcompat:1.7.0`, required because SESL `SeslDatePickerDialog` and `SeslTimePickerDialog` inherit from `androidx.appcompat.app.AlertDialog`.
- Added the missing Compose `LazyListScope.items` import for quick responses.
- Removed the custom `onKeyEvent` Enter handler from the composer; IME `ImeAction.Send` / `KeyboardActions` remains the canonical send path.
- Kept the supplied SESL/One UI date and time picker implementation.

## Release

- versionCode: 7
- versionName: 0.7.0
- R8/resource shrinking remains enabled for release.
- GitHub Actions release CI remains enabled.

The SESL `androidx.core` duplicate-namespace message is a Gradle/manifest warning from the supplied SESL core artifact; it is not the Kotlin compilation failure reported by CI.
