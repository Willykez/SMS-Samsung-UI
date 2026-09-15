# About gradle-wrapper.jar

This folder is missing `gradle-wrapper.jar` on purpose — it's a compiled
binary that Gradle's own tooling generates, and it couldn't be produced in
the environment that built this project (no internet access to fetch it,
and no local Gradle install to generate it from).

**This does not block you from opening or running the app in Android
Studio** — the IDE talks to Gradle directly using the version specified in
`gradle-wrapper.properties` and doesn't need this jar to sync or build.

**It only matters if you want to run `./gradlew` from a terminal**, which
the CI workflow in `.github/workflows/android-ci.yml` avoids by installing
Gradle directly instead of relying on the wrapper.

## To generate the real jar (one-time, optional)

1. Open this project in Android Studio and let it sync once.
2. Open the **Gradle** tool window (View → Tool Windows → Gradle).
3. Navigate to **ForexTradeAnalyst → Tasks → build setup → wrapper**, and
   double-click it.
4. This creates a real `gradle-wrapper.jar` right here. Commit it, and
   `./gradlew` will work locally and in CI from then on.

Delete this file once you've done that — it's just a note for now.
