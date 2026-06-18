# Configuration

## Server URL

The admin login screen accepts a server URL field each time a validator logs in. The URL is not persisted - validators re-enter it on each login session.

| Setting | Default | Notes |
|---|---|---|
| Server URL | (prompted) | `https://domain.example.com` or `http://192.168.x.x:10000` |

The URL is validated by `UrlValidator`:
- Both **HTTP** and **HTTPS** accepted for any host
- Rejects empty URLs, non-http/https schemes, and suspicious host patterns (`%`-encoding, non-ASCII, `..`)

## Release Signing

The release build reads signing credentials from `mywifipass/.env`:

```properties
STORE_PASSWORD=your_keystore_password
```

Keystore: `mywifipass/app/mi-release-key.jks` (not in repo)
- Alias: `pablo`
- V1 + V2 signing enabled

## Network Security

The app declares `usesCleartextTraffic="true"` in `AndroidManifest.xml` to allow HTTP connections to self-hosted servers on private networks. For production deployments with public domains, HTTPS is enforced at the URL validation layer.

> **⚠️ Security note:** `usesCleartextTraffic="true"` is currently applied globally (debug and release). Before a public release this should be restricted to the debug build variant, with a network security config that whitelists cleartext only for known LAN hostnames in release builds.

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | API communication |
| `CAMERA` | QR code scanning |
| `CHANGE_WIFI_STATE` | Wi-Fi connection management |
| `ACCESS_WIFI_STATE` | Wi-Fi status detection |
| `ACCESS_NETWORK_STATE` | Network connectivity detection |

## FIDO2 / Passkey Support

FIDO2 requires:
- **Google Play Services** (provides Credential Manager)
- **Screen lock** (PIN, pattern, or biometric) configured on the device
- The server domain must serve a valid `/.well-known/assetlinks.json`

FIDO2 is enabled per-network via the `requires_fido2_validation: true` flag in the Wi-Fi pass metadata.

## Room Database

The local Room database persists:
- Networks (WiFi passes added by the user)
- Connection state (transient - not persisted between sessions)

Database version is managed via Room migrations.
