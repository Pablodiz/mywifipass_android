# Changelog

All notable changes to the MyWifiPass Android app.

---

## [1.3] - Apr-Jun 2026 (Current)

> **Branch:** `37-feature-fido2-validator-poc`

### FIDO2 / Passkey Validation

- **Fido2Service** orchestrates WebAuthn authentication flow end-to-end
- **Discoverable mode** - empty `allowCredentials`, Android Credential Manager shows passkey picker
- **Session ID** echo-back for stateless challenge resolution on server
- **CredentialHelper** wraps `androidx.credentials.CredentialManager` API
- **Improved error logging** - `GetCredentialException` type and message logged in detail
- **ApiPetitions** - `fido2AuthenticateStart` returns `Pair<optionsJson, sessionId>`; `fido2AuthenticateFinish` accepts optional `sessionId`

### Long-Press Delete

- **MyCard** + **MyCardList** support `onItemLongClick` via `combinedClickable` (ExperimentalFoundationApi)
- **MainScreen** → **AlertDialog** for delete confirmation
- **i18n** - new strings for delete dialog in English and Spanish

### SSE Authorization Streaming

- **NetworkRepository** supports SSE over `check_user_authorized` endpoint with `Accept: text/event-stream`
- Fallback to one-shot HTTP check if SSE unavailable (server returns non-SSE response)

### Security Hardening (Phase 1)

- **EncryptedSharedPreferences** - AES-256-GCM token storage (replaces plain SharedPreferences)
- **CertificateValidator** - expiry, chain, and format validation for X.509 certificates
- **UrlValidator** - both HTTP and HTTPS accepted for any host; rejects `%`-encoding, non-ASCII chars, `..` in host
- **FLAG_SECURE** on `LoginActivity`, `AdminActivity`, `NetworkDetailActivity`
- **Error sanitization** - full traces logged, generic messages shown to users

### Auto Wi-Fi Configuration

- Removed manual "Configure Wi-Fi" button - connection configures on certificate installation
- Retry logic for auto-configuration
- UI state reflects connection status (configured icon)

### Validator & Login UI Redesign (#41)

- **Login screen** - help text in a `surfaceVariant` card at the top; fields in a scrollable column below; `adjustResize` keeps TopBar fixed when keyboard opens
- **Validator (Admin) screen** - QR icon + instructional text explaining the scan → verify identity → authorize/reject flow
- **No-internet banner** - shown on login and validator screens with context-specific messages; fixed initial state (synchronous read) and `onLost` timing bug (stale `activeNetwork`) that caused missed updates
- **Notification dialogs** - cleared on `ON_RESUME` so dialogs from a background Activity no longer reappear when navigating back
- **QR codes** (network detail screen) - transparent background; tap to open full-size in a dialog

---

## [1.2] - 2026-02

> **Branch:** `refactor/code-quality-improvements`

- Code quality improvements: replace `!!` assertions, extract magic numbers
- Error handling: specific exception types, better logging
- Room DB migration consolidation

---

## [1.1] - 2025-09/10

> CSR architecture support

- **CSR generation** via JCE `KeyPairGenerator` (RSA-2048); CSR signing via Bouncy Castle (SHA-256WithRSAEncryption)
- **Private keys generated on-device**, never transmitted
- **Certificate download and installation** via `WifiEnterpriseConfig`
- **QR code scanning** for Wi-Fi pass URLs
- **Deep link support** (`mywifipass://` URI scheme)
- **Admin panel** - QR login, user validation
- **Material 3 Compose UI** - MainScreen, NetworkDetailScreen
- **Room database** for offline network cache
- **i18n** - English + Spanish

---

## [1.0] - 2025-07 (TFG)

> Original degree thesis version.

- Basic network list and QR scanning
- Server-side certificate download
- Manual Wi-Fi configuration
- Admin login via QR token
