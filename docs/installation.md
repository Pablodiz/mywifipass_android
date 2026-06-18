# Installation

## Device Requirements

| Requirement | Minimum |
|---|---|
| Android version | 10 (API 29) |
| Camera | Required for QR scanning (optional) |
| Google Play Services | Required for FIDO2/Passkey support (Credential Manager) |

## Build from Source

### Prerequisites

- **Android Studio** Ladybug or newer
- **JDK 17**
- **Gradle 8.9** (included via wrapper)
- **Kotlin 2.0+**

### Steps

```bash
git clone https://github.com/Pablodiz/mywifipass_android.git
cd mywifipass_android/mywifipass
```

Create a `.env` file in `mywifipass/` for release signing:

```properties
STORE_PASSWORD=your_keystore_password
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Release Build

```bash
./gradlew assembleRelease
```

Requires `mi-release-key.jks` in `mywifipass/app/` (not included in the repo) and `STORE_PASSWORD` in `mywifipass/.env`.

### Install via ADB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Server Connection

The app connects to a self-hosted MyWifiPass server. Regular users get the server URL embedded in the Wi-Fi pass QR code. Validators (admins) enter the server URL manually in the login screen each session - it is not persisted.

> The app accepts both HTTP and HTTPS for any server URL. Suspicious host patterns (percent-encoding, non-ASCII chars, `..`) are rejected. See [Configuration](configuration.md).

---

## Verification

1. Open the app
2. Tap "Add Wifi Pass" and scan a Wi-Fi pass QR code
3. The network should appear in the main list

If the server uses self-signed certificates, ensure the server's CA certificate is installed on the Android device (Settings → Security → Install from storage).
