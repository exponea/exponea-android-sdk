# Exponea SDK — Example App

Sample application for the Bloomreach Engagement Android SDK. It demonstrates SDK setup, common API calls, and in-app content blocks.

## Build & run

From the repository root:

```bash
# Google Play Services (Firebase push)
./gradlew :app:assembleGmsDebug
./gradlew :app:installGmsDebug

# Huawei Mobile Services
./gradlew :app:assembleHmsDebug
./gradlew :app:installHmsDebug
```

Release builds use ProGuard/R8 (`minifyEnabled true`) to verify SDK rules in a minified app:

```bash
./gradlew :app:assembleGmsRelease
./gradlew :app:assembleHmsRelease
```

## App identity

| | Value |
|---|---|
| **applicationId** | `com.exponea.sdk.example` |
| **namespace / package** | `com.exponea.example` |

## What the app shows

Jetpack Compose UI with bottom navigation:

- **Auth** — configure and initialize the SDK (project or stream integration)
- **Fetching** — consents, recommendations, segments, app inbox
- **Tracking** — payments, push events, custom events, customer identify
- **Flushing** — manual flush and flush settings
- **Anonymize** — customer anonymization
- **Content blocks** — Compose and XML-based content block demos

SDK configuration is available from the in-app config drawer. Logs are shown in a bottom sheet (`MemoryLogger`).

## Further reading

- [Android SDK Example App (official docs)](https://documentation.bloomreach.com/engagement/docs/android-sdk-example-app)
- [Android SDK setup](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup)
