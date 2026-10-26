# Aro App

Native Android client for **Aro** — an offline-first nutrition, fitness, planning, and clinical tracking companion.

This repository contains the Android application only. It has no runtime or build dependency on the Aro website.

## Requirements

- JDK 17
- Android SDK 36
- Android Studio (recommended) or the Android command-line tools

## Build

Open this folder in Android Studio and let it generate `local.properties`, or create the file manually:

```properties
sdk.dir=/path/to/Android/sdk
```

Then build from the project root:

```bash
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Tests

Unit tests:

```bash
./gradlew testDebugUnitTest
```

Instrumented tests require a connected device or emulator:

```bash
./gradlew connectedDebugAndroidTest
```

## Project details

- **Package:** `com.drnitz.aro`
- **Min SDK:** 26
- **Target / compile SDK:** 36
- **UI:** Jetpack Compose with Material 3
- **Storage:** Room, schema exports under `app/schemas/`
- **Offline-first:** food and exercise libraries are bundled in `app/src/main/assets/`
- **Network use:** Open Food Facts lookups and remote food/exercise images only

## Privacy

Health, nutrition, workout, and plan data stay in the on-device Room database. There is no account system or cloud sync.

## Attributions & Data Sources

- Exercise database powered by [Free Exercise DB](https://github.com/yuhonas/free-exercise-db) (Public Domain / The Unlicense) and [RepDB](https://repdb.co/) ("Exercise data by RepDB (repdb.co)", Free Tier with attribution).
- Nutrition database powered by Open Food Facts and curated USDA data.

