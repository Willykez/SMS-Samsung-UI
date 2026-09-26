# OneMessages 0.8.0 — SESL dependency alignment

The SESL picker is built from forked AndroidX modules that retain `androidx.*` package names. The app must not put the stock and SESL copies of those modules on the same classpath.

## Fix

`app/build.gradle.kts` now:

- removes direct stock `androidx.core:core-ktx` and `androidx.appcompat:appcompat` dependencies;
- adds SESL `core:1.3.0` and `appcompat:1.4.0` explicitly so the picker supertypes are available at compile time;
- excludes the forked stock modules from all configurations: core, appcompat, fragment, viewpager, drawerlayout, customview, recyclerview, coordinatorlayout, swiperefreshlayout, and preference;
- leaves unrelated AndroidX libraries (Compose, lifecycle, activity, Room, WorkManager, navigation, etc.) on their normal artifacts;
- removes unused `core-splashscreen` rather than forcing a stock core dependency back into the graph;
- keeps a narrow META-INF packaging exclusion as defense-in-depth, not as the duplicate-class fix.

This follows the OneUIProject SESL guidance to remove Google's original copies of the forked modules while retaining official AndroidX modules that SESL does not replace.
