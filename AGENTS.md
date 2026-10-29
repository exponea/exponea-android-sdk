# Bloomreach Marketing Android SDK

## 1. Scope

Kotlin Android SDK for Bloomreach Marketing, published as `com.exponea.sdk:sdk` via Maven for Android integrators.

Modules: `sdk` (the published library) and `app` (example app).

## 2. Commands

Run from the repo root.

```bash
./gradlew :sdk:assembleRelease          # build the library
./gradlew :app:assembleGmsDebug         # build the example app, FCM (gms flavor)
./gradlew :app:assembleHmsDebug         # build the example app, Huawei HMS (hms flavor)
./gradlew ktlintCheck                   # lint (android style, all subprojects)
./gradlew :sdk:apiCheck                 # public API matches sdk/api/sdk.api
./gradlew :sdk:testDebugUnitTest        # full unit suite (Robolectric, JVM), slow: ~25-30 min; run in background, iterate with --tests
./gradlew :sdk:testDebugUnitTest --tests "com.exponea.sdk.ExponeaTest" # one class
./gradlew :sdk:testDebugUnitTest --tests "com.exponea.sdk.ExponeaTest.should get customer cookie after initialized" # one function (quote the full backtick name, or use *)
./gradlew :sdk:connectedDebugAndroidTest # instrumented tests (sdk/src/androidTest), needs a device/emulator
```

- Flavors exist only in `app` (`messagingService` dimension: `gms`, `hms`); `app/build.gradle` applies the `google-services` / `agconnect` plugin only when the requested task name contains `gms` / `hms`, so always name the flavor.
- The Huawei Maven repo is configured in the root `build.gradle`.
- Toolchain versions (minSdk, compileSdk, JVM, Kotlin) live in `sdk/build.gradle` and root `build.gradle`; read them there.

## 3. Public API

- `sdk/api/sdk.api` lists the public API. `./gradlew :sdk:apiCheck` fails when the compiled API differs from it.
- Run `./gradlew :sdk:apiDump` only for an approved API change, and explain the `sdk.api` diff in the PR description.
- Before a breaking change in a major release, check the React Native and Flutter Android bridges; they compile against this API.

## 4. Architecture

- Entry point: `Exponea` (`sdk/.../Exponea.kt`) is the public singleton facade (`init`, `trackEvent`, `identifyCustomer`, `flushMode`, push / in-app / inbox / segments APIs).
- Wiring: `ExponeaComponent` is an `internal` manual DI container (no Dagger/Hilt); `Exponea` creates one per initialization. Wire new managers and repositories here, as interface + `XxxImpl` like the existing ones in `manager/` and `repository/`.
- SDK-less push path: `handleRemoteMessage` / `handleNewToken` / `handleNewHmsToken` work before `Exponea.init` through `TimeLimitedFcmManagerImpl` (a time-limited subclass of `FcmManagerImpl`, built without `ExponeaComponent`). A change to `FcmManagerImpl` behavior must be mirrored there; it must not call the `Exponea` API.
- Event flow: `EventManager`/`EventRepository` write to Room, then `FlushManager` sends via `ExponeaService` per flush mode (immediate / periodic / app-close / manual).
- Build: the `shadow-aar` plugin repackages some dependencies into the AAR (e.g. `coil` becomes `freeze.coil`). The `sdk` module has no flavors; FCM and HMS support are both built into it. `sdk/proguard-rules.pro` ships as consumer rules and keeps all of `com.exponea.**`, so new SDK classes need no keep rule.

## 5. Conventions

- Wrap the body of every new public `Exponea` function in `runCatching { … }.logOnException()` (or `.returnOnException { <default> }` when it returns a value). Safe mode is on by default and the error is logged and swallowed; in a debuggable app it is rethrown, so do not add extra `try/catch` to hide it. Calls that need the SDK initialized go through `initGate.waitForInitialize { … }`.
- Run background and main-thread work through `runOnBackgroundThread` / `runOnMainThread` / `ensureOn*Thread` (`util/CoroutineDispatchers.kt`) so errors are caught and tests can swap the dispatchers; do not start raw threads or `GlobalScope`.
- Log with `Logger.v/d/i/w/e(this, "…")`, never `android.util.Log`.
- Public API names: callback interfaces end in `Callback` (except `PushNotificationDelegate`); pluggable components are `XxxProvider`, with an `open DefaultXxxProvider` where a default exists (`DefaultAppInboxProvider`); deprecate with `@Deprecated("Please use X instead", ReplaceWith(…))`. New public functions get KDoc.

## 6. Testing

- Unit tests: JUnit + Robolectric (`includeAndroidResources = true`), run on the JVM.
- Instrumented tests: `sdk/src/androidTest` (`AndroidJUnitRunner`). They need a running emulator or device:

  ```bash
  emulator -list-avds                       # available virtual devices
  emulator -avd <name> &                    # start one
  adb wait-for-device && adb devices        # confirm the emulator is listed
  ./gradlew :sdk:connectedDebugAndroidTest
  ```

- Room version bump: add a `MigrationTest` (`sdk/src/androidTest`) case for N→N+1 and update `migrate1toLatest`.
- List every new public `Exponea` member in `runcatching/PublicApiTestCases`; `PublicApiCoveredTest` fails otherwise.
- Stub HTTP with `testutil/ExponeaMockServer` (wraps OkHttp `MockWebServer`; responses come from test resources) instead of mocking the network ad hoc.
- Use `assertThat(actual, matcher)` instead of the specific `assert*` helpers (`assertEquals`, `assertTrue`, `assertNull` ...), also when editing an existing test file. Tests migrate gradually and mixed assertion styles in one file are fine.

What to run before calling a change done, in the listed order. Fix a failing check before running the next one. Start the full `:sdk:testDebugUnitTest` in the background and report the other results while it runs.

| Change touches                          | Run                                                                                                                                                                     |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Docs only                               | nothing                                                                                                                                                                 |
| Internal code                           | `./gradlew ktlintCheck` + `./gradlew :sdk:apiCheck` + affected `./gradlew :sdk:testDebugUnitTest --tests ...`                                                           |
| Room entities, DB version or migrations | `./gradlew ktlintCheck` + `./gradlew :sdk:connectedDebugAndroidTest` + the internal-code row. Needs an emulator/device; if none is available, say the check was not run |
| Example app only (`app/`)               | `./gradlew ktlintCheck` + `./gradlew :app:assembleGmsDebug :app:assembleHmsDebug`                                                                                       |
| Public API                              | `./gradlew ktlintCheck` + `./gradlew :sdk:apiCheck` + `./gradlew :app:assembleGmsDebug :app:assembleHmsDebug` + `./gradlew :sdk:testDebugUnitTest`                      |
| Build / packaging                       | `./gradlew :sdk:assembleRelease :app:assembleGmsDebug :app:assembleHmsDebug`                                                                                            |

## 7. Generated code

- Never hand-edit `sdk/schemas/` (Room exports these on build; schema changes go through a database version bump and migration) or `build/` output.
