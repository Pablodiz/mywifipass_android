# MyWifiPass Android - Technical Documentation

> **Version:** 1.3 &nbsp;|&nbsp; **License:** BSD 3-Clause &nbsp;|&nbsp; **Author:** Pablo Diz de la Cruz

## What is MyWifiPass Android?

MyWifiPass Android is the mobile client for the [MyWifiPass System](https://github.com/Pablodiz/mywifipass_system) platform. It serves two roles: end users scan a Wi-Fi pass QR code and go through a certificate provisioning flow to connect to an EAP-TLS network; validators (event staff) use the admin panel to check user identity documents and authorize certificate signing.

### Technology Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Database | Room (local SQLite) |
| HTTP | `java.net.HttpURLConnection` via `httpPetition()` wrapper |
| Cryptography | EncryptedSharedPreferences (AES-256-GCM) for token storage; Bouncy Castle for CSR signing |
| FIDO2 / WebAuthn | Android Credential Manager (androidx.credentials) |
| Security | EncryptedSharedPreferences, certificate validation, URL whitelisting |

---

## Table of Contents

1. **[Architecture](architecture.md)** - App architecture, component diagram, data flow, activity lifecycle
2. **[Installation](installation.md)** - Build from source, device requirements
3. **[Configuration](configuration.md)** - Server URL, self-hosted setup, .env for signing
4. **[User Guide](usage.md)** - Scan QR, add passes, FIDO2 validation, Wi-Fi connection
5. **[FIDO2 / Passkeys](fido2-guide.md)** - Passkey registration, discoverable mode, biometric auth flow
6. **[Project Structure](project-structure.md)** - Directory tree with explanations
7. **[Development Guide](development.md)** - Local build, debugging, testing, CI/CD
8. **[Security Model](security.md)** - Encrypted storage, certificate validation, URL whitelisting
9. **[Contributing](contributing.md)** - PR rules, code style, commit conventions
10. **[Troubleshooting](troubleshooting.md)** - Common issues: build, connection, FIDO2, certificates
11. **[Changelog](changelog.md)** - Version history

---

## Quick Links

- **Server Repository:** [github.com/Pablodiz/mywifipass_system](https://github.com/Pablodiz/mywifipass_system)
- **Main TFG Repository:** [github.com/Pablodiz/TFG_proyecto](https://github.com/Pablodiz/TFG_proyecto)

---

> **Copyright (c) 2025, Pablo Diz de la Cruz.**  
> Distributed under the [BSD 3-Clause License](../LICENSE).
