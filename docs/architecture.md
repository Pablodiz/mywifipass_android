# Architecture

## High-Level Architecture

```mermaid
graph TD
    subgraph App["Android App"]
        ACT["Activities (5)"]
        UI["Compose UI"]
        CTR["Controllers (4)"]
        REPO["Repositories (2)"]
        DB[("Room DB (SQLite)")]
        API["API Client (HttpURLConnection)"]
        SVC["Services\n(FIDO2, CSR, EAP-TLS)"]
        SEC["Security Layer\n(Keystore, EncryptedPrefs,\nCertValidator, UrlValidator)"]

        ACT --> CTR
        UI --> CTR
        CTR --> REPO
        CTR --> SVC
        REPO --> DB
        REPO --> API
        REPO --> SVC
        CTR --> SEC
    end

    API -->|"HTTPS / SSE"| Server["MyWifiPass Server\n(Django + FreeRADIUS)"]
```

## Component Overview

### Activities (5)

| Activity | Purpose |
|---|---|
| `MainActivity` | Entry point - network list, QR scanner trigger |
| `LoginActivity` | Validator login via credentials or QR token from server |
| `AdminActivity` | Validator panel - scan user QR, verify identity, authorize |
| `NetworkDetailActivity` | Per-network detail: SSE authorization stream, certificate installation, Wi-Fi connection status |
| `DeepLinkActivity` | Handles `mywifipass://` URI scheme for Wi-Fi pass URLs |

### Controllers (4)

Controllers bridge UI events and business logic. They coordinate repositories, services, and state updates.

| Controller | Scope |
|---|---|
| `MainController` | Network CRUD, FIDO2 validation, CSR signing, SSE auth |
| `LoginController` | Validator login - credentials or QR token parsing |
| `AdminController` | Admin QR validation and attendee authorization |
| `RoleController` | Role/auth state - token presence, redirect decisions |

### Repositories (2)

| Repository | Responsibility |
|---|---|
| `AuthRepository` | Token persistence via EncryptedSharedPreferences, login state |
| `NetworkRepository` | Network CRUD against Room DB + remote API, SSE streaming |

### Services

| Service | Purpose |
|---|---|
| `Fido2Service` | FIDO2/WebAuthn flow: get challenge → Credential Manager → verify |
| `CredentialHelper` | Wraps `androidx.credentials` API (requires Activity context) |
| `CSR` | RSA-2048 key pair generation (JCE) + CSR signing + PEM encoding via Bouncy Castle |
| `EapTLS_Certificate` | Certificate parsing and installation into `WifiEnterpriseConfig` |
| `EapTLSConnection` | Wi-Fi connection via `WifiEnterpriseConfig` (EAP-TLS) |

### Data Layer

| Component | Technology |
|---|---|
| Local DB | Room (SQLite) - networks, connection state |
| API Client | `HttpURLConnection` via `httpPetition()` wrapper (single-shot, no retry) |
| SSE Client | `HttpURLConnection` + `BufferedReader` loop returning `SseOutcome` enum |
| Serialization | Gson + kotlinx.serialization for JSON parsing |

### Security Layer

| Component | Purpose |
|---|---|
| `EncryptedSharedPreferences` | AES-256-GCM encrypted token storage |
| `CertificateValidator` | Expiry, chain, and format validation for X.509 certs |
| `UrlValidator` | URL validation - both HTTP and HTTPS accepted; rejects `%`-encoding, non-ASCII, `..` |
| `Android Keystore` | Token master key for EncryptedSharedPreferences (AES-256-GCM) |

---

## Data Flow: Adding a Wi-Fi Pass

Two entry points, same outcome:

```mermaid
flowchart TD
    A1["User scans QR in-app\n(MainScreen QR scanner)"] --> C
    A2["User opens mywifipass:// link\n(external app / browser)"] --> B["DeepLinkActivity"]
    B -->|"intent extra: wifi_pass_url"| MA["MainActivity"]
    MA --> C["MainController.addNetworkFromUrl()"]
    C --> D["NetworkRepository.addNetworkFromUrl()\nGET wifi-pass-url"]
    D --> E["Parse JSON response"]
    E --> F["Insert into Room DB"]
    F --> G["Update UI (MainScreen)"]
```

## Data Flow: FIDO2 Validation + Certificate Installation

```mermaid
sequenceDiagram
    participant User
    participant App
    participant Server

    User->>App: Tap network card (MainScreen)
    App->>Server: POST /fido2/authenticate/start/
    Server-->>App: challenge + sessionId

    App->>User: Biometric prompt (Credential Manager)
    User-->>App: Passkey assertion

    App->>Server: POST /fido2/authenticate/finish/ (assertion + sessionId)
    Server-->>App: Authorization confirmed

    Note over App: Navigate to NetworkDetailScreen
    App->>Server: SSE /check_user_authorized/ (blocking stream)
    Server-->>App: "authorized" event

    App->>Server: POST /sign_certificate/ (CSR)
    Server-->>App: Signed X.509 certificate

    App->>App: EapTLS_Certificate.install() → WifiEnterpriseConfig
    App->>App: Device connects via EAP-TLS
```

## Activity Lifecycle

All sensitive activities (`LoginActivity`, `AdminActivity`, `NetworkDetailActivity`) use `FLAG_SECURE` to prevent screenshots and screen recording. The app uses Jetpack Compose with `LaunchedEffect` for side effects such as SSE connections and authorization checks.
