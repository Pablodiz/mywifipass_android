# Security Model

## Overview

The MyWifiPass Android app handles cryptographic material (private keys, certificates, auth tokens). Security measures are applied at multiple layers.

## Token Storage

Auth tokens are stored using **EncryptedSharedPreferences** (AndroidX Security Crypto):

- **Algorithm:** AES-256-GCM
- **Key storage:** Android Keystore (hardware-backed when available)
- **Master key:** `_androidx_security_master_key_` in Android Keystore
- **Fallback:** If decryption fails, the token is cleared and treated as missing

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
val prefs = EncryptedSharedPreferences.create(context, "auth", masterKey, ...)
```

## Certificate Validation (`CertificateValidator`)

All X.509 certificates are validated at install time:

| Check | Description |
|---|---|
| Expiry | Certificate must not be expired |
| Future validity | Certificate `notBefore` must not be in the future |
| Chain validation | CA → client certificate chain verified |
| Expiry warning | Warning logged if cert expires in < 30 days |

## URL Validation (`UrlValidator`)

Server URLs are validated to prevent malformed or malicious inputs:

| Check | Policy |
|---|---|
| **Scheme** | Only `http` and `https` accepted (both allowed for any host) |
| **Host** | Must be non-empty and pass Android `Patterns.WEB_URL` check |
| **Suspicious patterns** | Rejected: `%`-encoded characters in host, non-ASCII Unicode chars, `..` (traversal) |
| **Port** | Any valid port (1-65535) allowed |

## Screen Security

Sensitive activities use `FLAG_SECURE` to prevent screenshots:

```kotlin
window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE)
```

Applied to: `LoginActivity`, `AdminActivity`, `NetworkDetailActivity`.

## Private Key Protection

CSR private keys are generated in-process using the standard JCE provider:

```kotlin
fun generateKeyPair(): KeyPair {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(2048)
    return keyGen.generateKeyPair()
}
```

- **RSA-2048**, signed with SHA-256 via Bouncy Castle
- The private key is converted to PEM and stored in the local Room database alongside the certificate
- Keys are not hardware-backed and are not stored in Android Keystore

> **Known limitation:** moving key generation into Android Keystore would make private keys hardware-backed, user-auth-gated, and non-exportable, but the current architecture stores them in Room to allow certificate re-use across sessions.

## Error Sanitization

API error responses are sanitized before being shown to users:

- Full error details logged to Logcat (server-side)
- UI shows generic messages without stack traces
- `showTrace = false` for all user-facing error dialogs

## Permissions

The app requests minimal permissions:

| Permission | Justification |
|---|---|
| `INTERNET` | API communication |
| `CAMERA` | QR code scanning |
| `CHANGE_WIFI_STATE` | Wi-Fi connection |
| `ACCESS_WIFI_STATE` | Status checks |
| `ACCESS_NETWORK_STATE` | Network connectivity checks |

No location, contacts, storage, or background permissions.

## Known Limitations

- **No certificate pinning** - in the self-hosted model this is inherent: every deployment generates its own unique certificates that are not known at APK build time, so it is not possible to ship a pinned certificate in a generic APK. Pinning would only be applicable for a fixed-server enterprise deployment where the server certificate is known in advance and the APK is built or configured per-organisation.
- **`usesCleartextTraffic="true"` is enabled for development convenience** - this allows anyone to try the system without needing to set up TLS certificates for the backend server. In production, HTTPS should always be used; this flag should be disabled or scoped to debug builds before any real deployment.
- **No biometric re-auth when EAP-TLS authenticates** - this is a characteristic of how Android's `WifiEnterpriseConfig` works: once a certificate is installed, the OS uses it automatically for Wi-Fi authentication without prompting the user. This is not specific to this app.
- **No remote wipe** - if a device is lost, revoke the certificate from the admin panel. The system regenerates the CRL and reloads FreeRADIUS within minutes.
