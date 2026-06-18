# Project Structure

```
mywifipass_android/
├── README.md
├── user_manual.md
├── LICENSE
├── docs/                              # This documentation
│   ├── index.md
│   ├── architecture.md
│   ├── installation.md
│   ├── configuration.md
│   ├── usage.md
│   ├── fido2-guide.md
│   ├── project-structure.md
│   ├── development.md
│   ├── security.md
│   ├── contributing.md
│   ├── troubleshooting.md
│   └── changelog.md
├── wip_docs/                          # Work-in-progress notes
│   ├── CODE_REVIEW.md
│   └── RESUMEN_FEATURE_FIDO2_ANDROID.md
└── mywifipass/                        # Android project root
    ├── build.gradle.kts               # Root Gradle build
    ├── settings.gradle.kts            # Module settings
    ├── gradle.properties              # Gradle config
    ├── gradlew / gradlew.bat          # Gradle wrapper
    ├── gradle/                        # Gradle wrapper jar
    ├── local.properties               # Android Studio SDK path (not in repo)
    ├── .env                           # Signing credentials (not in repo)
    └── app/                           # Main application module
        ├── build.gradle.kts           # App-level build config
        ├── mi-release-key.jks         # Release keystore (not in repo)
        └── src/
            └── main/
                ├── AndroidManifest.xml
                ├── res/
                │   ├── values/        # English strings
                │   ├── values-es/     # Spanish strings
                │   └── xml/           # Backup rules, network security
                └── java/com/example/mywifipass/
                    │
                    ├── MainActivity.kt          # Entry point
                    ├── LoginActivity.kt         # Admin login
                    ├── AdminActivity.kt         # Admin panel
                    ├── NetworkDetailActivity.kt # Per-network detail
                    ├── DeepLinkActivity.kt      # mywifipass:// handler
                    │
                    ├── controller/
                    │   ├── MainController.kt    # Network CRUD, FIDO2, CSR
                    │   ├── LoginController.kt   # Validator login (credentials + QR)
                    │   ├── AdminController.kt   # Admin QR validation + attendee auth
                    │   └── RoleController.kt    # Role/auth state, redirect decisions
                    │
                    ├── model/
                    │   ├── data/                # Data classes
                    │   │   ├── Network.kt
                    │   │   ├── LoginCredentials.kt
                    │   │   ├── QrLoginCredentials.kt
                    │   │   └── QrData.kt
                    │   └── repository/          # Data access layer
                    │       ├── AuthRepository.kt
                    │       └── NetworkRepository.kt
                    │
                    ├── ui/                      # Jetpack Compose UI
                    │   ├── MyComponents.kt      # MainScreen, NetworkDetailScreen
                    │   ├── Cards.kt             # NetworkCard, MyCardList
                    │   ├── Forms.kt             # Input forms
                    │   ├── Dialogs.kt           # Alert dialogs
                    │   ├── theme/               # Material 3 theming
                    │   │   ├── Color.kt
                    │   │   ├── Theme.kt
                    │   │   └── Type.kt
                    │   └── components/          # Reusable widgets
                    │       ├── NotificationSystem.kt
                    │       └── ApiErrorDialog.kt
                    │
                    ├── fido2/                   # FIDO2 / WebAuthn
                    │   ├── Fido2Service.kt      # Auth flow orchestrator
                    │   └── CredentialHelper.kt   # Credential Manager wrapper
                    │
                    ├── backend/
                    │   ├── api_petitions/
                    │   │   └── ApiPetitions.kt  # HTTP client + all API calls
                    │   ├── certificates/
                    │   │   ├── CSR.kt           # CSR generation (JCE RSA-2048 + Bouncy Castle signing)
                    │   │   └── EapTLS_Certificate.kt  # Cert parsing + install
                    │   ├── database/
                    │   │   └── Database.kt      # Room DB + DAOs
                    │   ├── security/
                    │   │   ├── UrlValidator.kt
                    │   │   └── CertificateValidator.kt
                    │   ├── wifi_connection/
                    │   │   └── EapTLSConnection.kt
                    │   └── Utilities.kt         # Shared helpers
                    │
```

## Key Design Decisions

- **No ViewModels** - the app uses four controllers (`MainController`, `LoginController`, `AdminController`, `RoleController`) to bridge Compose state and backend logic. Room queries are run on coroutines directly from the controller.
- **Compose-only UI** - all screens are built with Jetpack Compose (no XML layouts).
- **Single Activity pattern lite** - most navigation happens within `MainActivity` via composable state switches; `LoginActivity`, `AdminActivity`, and `NetworkDetailActivity` are separate for clean lifecycle boundaries.
- **Room for offline persistence** - networks are cached locally so the app works between server restarts.
- **Coroutine-based async** - all network, DB, and crypto operations run on `Dispatchers.IO`.
