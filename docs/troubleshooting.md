# Troubleshooting

## Common Issues

### "Cannot connect to server"

**Check:** Server URL is correct and server is running.

```bash
curl http://<server-ip>:10000/api/
```

**Causes:**
- Server not running → `docker compose up -d`
- Firewall blocking port → allow port 10000/tcp (or custom `WEBAPP_PORT`)
- HTTPS used but server only supports HTTP (or vice versa)
- Self-signed certificate → install CA on device (Settings → Security → Install from storage)

### "Server URL not valid" in admin login

The URL validator (used in the admin login screen) rejects:
- Empty URLs or non-http/https schemes
- Suspicious host patterns: `%`-encoded characters, non-ASCII characters, `..` path traversal

Both HTTP and HTTPS are accepted for any host.

**Fix:** Use a full URL with scheme: `http://192.168.1.100:10000` or `https://mywifipass.example.com`

### QR scanner doesn't work

- Ensure camera permission is granted (Settings → Apps → MyWifiPass → Permissions)
- The QR must contain a valid HTTP/HTTPS Wi-Fi pass download URL (provided by the server or event organizer)
- Try good lighting and hold the camera steady

### "No passkey registered" / Credential Manager error

When FIDO2 validation fails with credential errors:

1. Ensure **screen lock** is set (PIN, pattern, password, or biometric)
2. Ensure **Google Play Services** is up to date
3. Register a passkey first via the server's web UI at `/fido2/register/`
4. Check that the server's `/.well-known/assetlinks.json` includes the app's fingerprint

**Debug logging:** Filter logcat for `Fido2Service`:

```bash
adb logcat -s Fido2Service:*
```

Look for `CREDENTIAL MANAGER ERROR` block with exception type and message.

### "Authentication failed" after FIDO2

- The challenge may have expired (5-minute window) → restart authentication
- The passkey may have been deleted from the server → re-register
- The server may be using a different `rp_id` than expected → check server config

### Certificate installation fails

Common causes:
- Device already has a certificate for this SSID → forget the network first (Settings → Wi-Fi → Forget)
- Wi-Fi is off → enable Wi-Fi
- Android version < 10 → EAP-TLS programmatic configuration is limited

### Build errors

**`Unresolved reference: combinedClickable`**

Update imports:
```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
```

**Gradle sync fails**

```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### "Network deleted" but still visible

Navigate away and back, or restart the app - the network list refreshes on `ON_RESUME`.

### SSE streaming not working (stuck polling)

- Server must support SSE (`Accept: text/event-stream`)
- Proxy/load balancer must not buffer SSE responses (disable buffering)
- If SSE returns UNAVAILABLE (server does not speak SSE), the app falls back to a one-shot HTTP check; check logcat for `NetworkRepository` SSE errors
