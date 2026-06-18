# Development Guide

## Local Setup

### Prerequisites

- **Android Studio** Ladybug (2024.2.1+), IntelliJ IDEA or VSCode with proper Kotlin and Android plugins
- **JDK 17**
- **Android SDK** 35 (compile) + 29 (min)

### Opening the Project

1. Clone the repository
2. Open Android Studio → Open → select `mywifipass_android/mywifipass/`
3. Wait for Gradle sync to complete

### Environment

Create `mywifipass/.env` for release signing:

```properties
STORE_PASSWORD=your_password
```

For debug builds, Android Studio uses the default debug keystore automatically.

## Building

### Debug

```bash
cd mywifipass
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release

```bash
./gradlew assembleRelease
```

Requires `mi-release-key.jks` in `app/` and `.env` with `STORE_PASSWORD`.

### Clean Build

```bash
./gradlew clean assembleDebug
```

## Testing

There are no unit or instrumented tests yet. Run lint before committing:

### Lint

```bash
./gradlew lint
```

## Debugging

### Logcat Filtering

```bash
adb logcat -s MainController:* Fido2Service:* ApiPetitions:* NetworkRepository:*
```

Key log tags:
- `MainController` - network operations, CSR signing
- `Fido2Service` - FIDO2 flow (challenge, credential, verification)
- `ApiPetitions` - HTTP requests/responses
- `NetworkRepository` - Room DB + SSE streaming

### ADB over Wi-Fi

```bash
adb tcpip 5555
adb connect <device-ip>:5555
```

### Inspecting Room Database

```bash
adb shell run-as app.mywifipass cat /data/data/app.mywifipass/databases/app-database
```

Or use Android Studio's **App Inspection** → Database Inspector.

## Room Database Migrations

When changing the `Network` entity:

1. Increment `@Database(version = X)` in `Database.kt`
2. Add a top-level `MIGRATION_X_Y` val in `Database.kt` following the existing pattern
3. Register it in the `Room.databaseBuilder(...).addMigrations(...)` call

## Code Style

- **Kotlin** coding conventions (camelCase, 4-space indent)
- **Compose** state hoisting - state flows down, events flow up
- **Coroutines** - `rememberCoroutineScope()` in Compose, `lifecycleScope` in Activities; `Dispatchers.IO` for network/DB
- **Logging** - `android.util.Log` with tag = class name
- **Strings** - all user-facing strings in `res/values/strings.xml` (en) + `values-es/strings.xml`

## CI/CD

No CI/CD pipeline is configured yet. Lint, build, and tests must be run locally before merging.

## Release Process

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Update `docs/changelog.md`
3. Tag: `git tag v1.X.Y`
4. Build: `./gradlew assembleRelease`
5. Sign and upload APK to GitHub Releases
