# Sylo

Sylo is a privacy-first personal finance tracker for Android. It captures
expenses through manual entry, voice, and automatic bank SMS parsing, then
gives you a clear picture of your spending — all stored locally in an
encrypted database.

## Features

- **Manual expense entry** with category, currency, and amount validation.
- **Voice capture** — hold a button to speak an expense, release to confirm;
  reviewed before it's saved.
- **Automatic bank SMS capture** — parses bank/wallet payment SMS on a
  schedule (plus real-time notification capture) and records transactions
  without user input, with an in-app notification for each capture.
- **Dashboard & analytics** — spending totals, trends, and category breakdown,
  with adaptive multi-column layouts on tablets and foldables.
- **Transaction history** with search and filtering.
- **App lock** — biometric/PIN unlock, with re-lock on every app restart
  (session-only authentication).
- **Mandatory first-run balance setup** so analytics are meaningful from day one.
- **Verified email** via Android's Credential Manager / Digital Credentials API.
- **Encrypted local storage** — SQLCipher-backed Room database; no data leaves
  the device.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Jetpack Navigation 3** (multiple back stacks, adaptive scene strategies)
- **Hilt** for dependency injection
- **Room + SQLCipher** for encrypted local persistence
- **DataStore** for user preferences
- **WorkManager** for scheduled SMS bank-statement scanning
- **Android Credential Manager** (Digital Credentials / OpenID4VP) for verified email
- Custom **Gradle convention plugins** (`build-logic`) and version catalog
  (`gradle/libs.versions.toml`) for consistent module configuration
- **Product flavors** — `dev`, `staging`, `prod` build environments

## Module structure

This is a multi-module Gradle project organized by architectural layer:

```
app/                        # Application entry point, navigation graph, DI wiring
core/
  core-common/               # Shared utilities
  core-ui/                   # Design system: theme, shared composables
  core-navigation/           # Navigation destinations/contracts
  core-network/              # Retrofit/network setup
  core-database/             # Room/SQLCipher database, repositories
  core-security/             # Biometric auth, verified-email identity flow
feature/
  feature-auth/               # PIN/biometric setup and unlock
  feature-dashboard/          # Home dashboard and analytics
  feature-transactions/       # Expense entry, transaction history
  feature-voice/              # Voice-based expense capture
  feature-settings/           # App settings, identity verification
build-logic/                 # Custom Gradle convention plugins (AGP, Hilt, Compose)
```

Features do not depend on one another — only on `core` modules — so the app
module is the only place that assembles the full navigation graph.

## Requirements

- Android Studio (AGP 9.2.1)
- JDK 17
- `compileSdk` 37, `minSdk` 28, `targetSdk` 35

## Getting started

```bash
./gradlew :app:assembleDevDebug
```

Available build variants combine a flavor (`dev`, `staging`, `prod`) with a
build type (`debug`, `release`), e.g. `assembleProdRelease`.

## Permissions

Sylo requests the following permissions, each tied to an optional feature:

| Permission | Used for |
|---|---|
| `RECORD_AUDIO` | Voice expense capture |
| `READ_SMS` | Scheduled bank-statement SMS scanning |
| `POST_NOTIFICATIONS` | Capture alerts and reminders |
| `USE_BIOMETRIC` | App lock unlock |

All of these are optional at runtime — the app remains usable without
granting them, with the corresponding feature disabled.
