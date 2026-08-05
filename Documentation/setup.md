---
title: Initial SDK setup for Android SDK
slug: android-sdk-setup
category:
  uri: /branches/2/categories/guides/Developers
parent:
  uri: android-sdk
content:
  excerpt: Install and configure the Android SDK
---

## Install the SDK

The Exponea Android SDK can be installed or updated using [Gradle](https://gradle.org/) or [Maven](https://maven.apache.org/). In case of Gradle, you can use Kotlin or Groovy for your build configuration files.

> 📘
>
> Refer to the [Android SDK release notes](https://documentation.bloomreach.com/engagement/docs/android-sdk-release-notes) for the latest Exponea Android SDK release.

### Gradle (Kotlin)

1. In your app's `build.gradle.kts` file, add `com.exponea.sdk:sdk` inside the `dependencies { }` section:
   ```kotlin
   implementation("com.exponea.sdk:sdk:5.3.0")
   ```
2. Rebuild your project (`Build` > `Rebuild Project`).

### Gradle (Groovy)

1. In your app's `build.gradle` file, add `com.exponea.sdk:sdk` inside the `dependencies { }` section:
   ```groovy
   implementation 'com.exponea.sdk:sdk:5.3.0'
   ```
2. Rebuild your project (`Build` > `Rebuild Project`).

### Maven

1. In your app's `pom.xml` file, add `com.exponea.sdk:sdk` inside the `<dependencies> </dependencies>` section:
   ```xml
   <dependency>
      <groupId>com.exponea.sdk</groupId>
      <artifactId>sdk</artifactId>
      <version>5.3.0</version>
   </dependency>   
   ```
2. Rebuild your app with Maven.

## Initialize the SDK

Now that you have installed the SDK in your project, you must import, configure, and initialize the SDK in your application code.

> ❗️ Protect the privacy of your customers
>
> Make sure you have obtained and stored tracking consent from your customer before initializing Exponea Android SDK.
>
> To ensure you're not tracking events without the customer's consent, you can use `Exponea.clearLocalCustomerData()` when a customer opts out from tracking (this applies to new users or returning customers who have previously opted out). This will bring the SDK to a state as if it was never initialized. This option also prevents reusing existing cookies for returning customers.
>
> Refer to [Clear local customer data](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking#clear-local-customer-data) for details.
>
> If the customer denies tracking consent after Exponea Android SDK is initialized, you can use `Exponea.stopIntegration()` to stop SDK integration and remove all locally stored data.
>
> Refer to [Stop SDK integration](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking#stop-sdk-integration) for details.

> ❗️ Re-initializing after `stopIntegration()`
>
> If your app re-initializes the SDK by calling `init()` after a previous `stopIntegration()`, call `Exponea.trackPushToken()` (FCM) or `Exponea.trackHmsPushToken()` (HMS) again after each `init()`. `stopIntegration()` clears the local push token, and the SDK cannot recover it. Refer to [Re-tracking the push token after stopIntegration()](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking#re-tracking-the-push-token-after-stopintegration) for details.

The required configuration parameter is `integrationConfig` with `projectToken`, `authorization` and `baseUrl` when using `ProjectConfig`, or `streamId` and `baseUrl` when using `StreamConfig`. For `ProjectConfig`, you can find the credentials in the Bloomreach Engagement webapp under `Project settings` > `Access management` > `API`. For `StreamConfig`, you can find the stream ID in the Data hub app under `Event streams` > *your stream* > `Access Security`.

> 📘
>
> Refer to [Mobile SDKs API access management](https://documentation.bloomreach.com/engagement/docs/mobile-sdks-api-access-management) for details.

You can configure the SDK in [code](#using-configuration-in-code) (preferred) or using a [JSON configuration file](#using-a-configuration-file).

### Using configuration in code

Import the SDK:

```kotlin
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
```

Initialize the SDK with Project configuration:

```kotlin
val configuration = ExponeaConfiguration()

configuration.integrationConfig = ProjectConfig(
    baseUrl = "https://api.exponea.com",
    projectToken = "YOUR_PROJECT_TOKEN",
    authorization = "Token YOUR_API_KEY"
)

Exponea.init(this, configuration)
```
or with Stream configuration:

```kotlin
val configuration = ExponeaConfiguration()

configuration.integrationConfig = StreamConfig(
    baseUrl = "https://api.exponea.com",
    streamId = "YOUR_STREAM_ID"
)

Exponea.init(this, configuration)
```

> 📘  Note
>
> - Refer to [SDK auth token authorization](https://documentation.bloomreach.com/engagement/docs/android-sdk-authorization#sdk-auth-token-authorization) for detailed JWT setup.
> - See the Data hub documentation for an overview of how to [configure Android SDK with JWT authentication](https://documentation.bloomreach.com/data-hub/docs/configure-android-sdk-with-jwt-authentication) for event streams.

#### Initialize with customer identity

You can optionally provide a `CustomerIdentity` during initialization to identify the customer immediately:

```kotlin
val configuration = ExponeaConfiguration()
configuration.integrationConfig = StreamConfig(
    baseUrl = "https://api.exponea.com",
    streamId = "YOUR_STREAM_ID"
)

Exponea.init(
    this,
    configuration,
    customerIdentity = CustomerIdentity(
        customerIds = mapOf("registered" to "jane.doe@example.com"),
        sdkAuthToken = "your-jwt-token"
    )
)
```

#### Configure application ID

**Multiple mobile apps:** If your Engagement project supports multiple mobile apps, specify the `applicationId` in your configuration. This helps distinguish between different apps in your project.


```kotlin
configuration.applicationId = "<Your application id>" 
```

Make sure your `applicationId` value matches exactly Application ID configured in your Bloomreach Engagement under **Project Settings > Campaigns > Channels > Push Notifications.**

**Single mobile app:** If your Engagement project supports only one app, you can skip the `applicationId` configuration. The SDK will automatically use the default value "default-application".


### Using a configuration file

> ❗This option is **deprecated** and will be removed from SDK in the next major version.

Create a file `exponea_configuration.json` inside the `assets` folder of your application with at least the following configuration properties:

```json
{
  "projectToken": "YOUR_PROJECT_TOKEN",
  "authorization": "Token YOUR_API_KEY",
  "baseURL": "https://api.exponea.com"
}
```

Import the SDK in your code:

```kotlin
import com.exponea.sdk.Exponea
```

Initialize the SDK:

```kotlin
Exponea.init(this)
```

The SDK will read the configuration parameters from the configuration file.

> 📘
>
> Refer to [`exponea_configuration.json`](https://github.com/exponea/exponea-android-sdk/blob/main/app/src/main/assets/exponea_configuration.json) in the [Example app for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-example-app) for an example configuration file.

### Where to place the SDK initialization code

#### In an application subclass

Your [`Application`](https://developer.android.com/reference/android/app/Application)'s `onCreate()` method is the best place to do the initialization - it's called only once and very early in the application lifecycle. Application is the class for maintaining global application state.

It should look similar to the example below:

```kotlin
class MyApplication : Application() {
  override fun onCreate(){
    super.onCreate()

    val configuration = ExponeaConfiguration()

    configuration.integrationConfig = ProjectConfig(
        baseUrl = "https://api.exponea.com",
        projectToken = "YOUR_PROJECT_TOKEN",
        authorization = "Token YOUR_API_KEY"
    )

    // SDK initialization
    Exponea.init(this, configuration)
    // or Exponea.init(this) if using configuration file
  }
}
```
Make sure to register your custom application class in `AndroidManifest.xml`:
```xml
<application
   android:name=".MyApplication">
   ...
</application>
```

#### In an activity

You can also initialize the SDK from any `Activity`, but it's essential to do so as early as possible, preferably in your activity's `onCreate()` method.

The SDK hooks into the application lifecycle to track sessions (among other things), so you must keep track of activities' `onResume` callbacks. If you need to initialize the SDK after an activity has been resumed, do so with the context of the current activity.

> ❗️
>
> Certain API methods can be used before SDK initialization if a previous initialization was done.
> These API methods are:
> - `Exponea.handleCampaignIntent`
> - `Exponea.handleRemoteMessage`
> - `Exponea.handleNewToken`
> - `Exponea.handleNewHmsToken`
>
> In such a case, each method will track events with the configuration of the last initialization. Consider initializing the SDK in `Application::onCreate` to make sure a fresh configuration is applied in case of an application update.

### Done!

At this point, the SDK is active and should now be tracking sessions in your app.

## Other SDK configuration

### Advanced configuration

The SDK can be further configured by setting additional properties of the `ExponeaConfiguration` object or `exponea_configuration.json` file. For a complete list of available configuration parameters, refer to the [Configuration for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) documentation.

### Log level

The SDK supports the following log levels defined in `com.exponea.sdk.util.Logger.Level`:

| Log level  | Description |
| -----------| ----------- |
| `OFF`    | Disables all logging |
| `ERROR`   | Serious errors or breaking issues |
| `WARN` | Warnings and recommendations + `ERROR` |
| `INFO` | Informative messages + `WARN` + `ERROR` |
| `DEBUG` | Debugging information + `INFO` + `WARN` + `ERROR`  |
| `VERBOSE` | Information about all SDK actions + `DEBUG` + `INFO` + `WARN` + `ERROR`. |

The default log level is `INFO`. While developing or debugging, setting the log level to `VERBOSE` can be helpful.

You can set the log level at runtime as follows:

```kotlin
Exponea.loggerLevel = Logger.Level.VERBOSE
```

### Observe SDK logs with a LoggerCallback

The [log level](#log-level) controls what the SDK prints to Logcat. Register a `LoggerCallback` to forward the SDK's log lines into your own logging pipeline or a crash-reporting tool.

Implement the `com.exponea.sdk.models.LoggerCallback` interface and register it with `Exponea.registerLoggerCallback`:

```kotlin
object LoggerCallbackImpl : LoggerCallback {
    override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
        // Hand the log line off to your own destination: a file, a telemetry or
        // crash-reporting service, an in-memory buffer, etc. Do it quickly and never
        // block here. `yourLogSink` represents whatever pipeline you provide.
        yourLogSink.submit(level, message, throwable)
    }
}

// Register the callback
Exponea.registerLoggerCallback(LoggerCallbackImpl)

// Unregister it once you no longer need it
Exponea.unregisterLoggerCallback(LoggerCallbackImpl)
```

The callback behaves as follows:

* **Unconditional dispatch:** The callback receives every SDK log line regardless of `Exponea.loggerLevel` or the configured Logcat verbosity, including logs emitted while the SDK is stopped.
* **Read-only side channel:** The callback can't modify the message, suppress the SDK's own Logcat output, or stop other registered callbacks.
* **`throwable` availability:** Only error logs may carry a `throwable`; it's `null` for all other log levels.
* **Lifetime:** A registration persists for the whole process and isn't cleared by `stopIntegration()` or re-initialization. Always pair `registerLoggerCallback` with `unregisterLoggerCallback` to avoid leaking the callback.

> ❗️ **Callback contract**
>
> `onLog` is invoked synchronously, on the thread that produced the log — which may be the app's main thread — and at high frequency. Because of this:
>
> * Don't perform blocking or long-running work inside `onLog`. If you need to forward a log elsewhere, hand the data off (enqueue, buffer, or post to another thread) instead of processing it inline.
> * Never call the SDK's `Logger` (or any SDK code that logs) from inside `onLog`. Dispatch is synchronous and unconditional, so logging through `Logger` — directly or transitively — re-enters dispatch and causes unbounded recursion.

### Data flushing

For more information on how the SDK uploads data to the Engagement API and how to customize this behavior, see [Data flushing for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-data-flushing).

### Android Auto Backup and SDK SharedPreferences

The SDK stores its data in dedicated SharedPreferences files, separate from your app's default preferences file (`<package>_preferences.xml`).

#### SDK default backup behavior

If your app doesn't define its own backup rules, the SDK's default rules apply. The SDK excludes only:

| File | Reason |
|------|--------|
| `EXPONEA_PUSH_TOKEN.xml` | Push token mustn't be restored from backup (obsolete token after reinstall) |
| `EXPONEA_AUTH.xml` | Short-lived auth token (JWT) |

All other SDK SharedPreferences files are included in Auto Backup by default, for example:

| File | Contents (representative) |
|------|---------------------------|
| `EXPONEA_PREFERENCES.xml` | Customer cookie, customer IDs, SDK configuration, session, campaign data |
| `DEVICE_ID_PREFS.xml` | Device ID |
| `EXPONEA_TELEMETRY.xml` | Telemetry install ID |

Your app's default `<package>_preferences.xml` isn't affected and remains eligible for backup.

#### Excluding SDK data from Auto Backup (optional)

If you don't want SDK SharedPreferences cloud back up, add exclusions to your app's backup rule XML files and reference them from your `AndroidManifest.xml`. If you get a manifest merge conflict, see [Troubleshooting](#build-error-manifest-merger-failed).

Use:

- `res/xml/backup_rules.xml` for API 30 and below
- `res/xml/data_extraction_rules.xml` for API 31 and above

**Android 11 and below** (`fullBackupContent`):

```xml
<full-backup-content>
    <!-- your app rules -->
    <exclude domain="sharedpref" path="EXPONEA_PREFERENCES.xml"/>
    <exclude domain="sharedpref" path="EXPONEA_PUSH_TOKEN.xml"/>
    <exclude domain="sharedpref" path="EXPONEA_AUTH.xml"/>
    <exclude domain="sharedpref" path="DEVICE_ID_PREFS.xml"/>
    <exclude domain="sharedpref" path="EXPONEA_TELEMETRY.xml"/>
</full-backup-content>
```

**Android 12 and above** (`dataExtractionRules` — apply to both `cloud-backup` and `device-transfer`):

```xml
<data-extraction-rules>
    <cloud-backup>
        <!-- your app rules -->
        <exclude domain="sharedpref" path="EXPONEA_PREFERENCES.xml"/>
        <exclude domain="sharedpref" path="EXPONEA_PUSH_TOKEN.xml"/>
        <exclude domain="sharedpref" path="EXPONEA_AUTH.xml"/>
        <exclude domain="sharedpref" path="DEVICE_ID_PREFS.xml"/>
        <exclude domain="sharedpref" path="EXPONEA_TELEMETRY.xml"/>
    </cloud-backup>
    <device-transfer>
        <!-- your app rules -->
        <exclude domain="sharedpref" path="EXPONEA_PREFERENCES.xml"/>
        <exclude domain="sharedpref" path="EXPONEA_PUSH_TOKEN.xml"/>
        <exclude domain="sharedpref" path="EXPONEA_AUTH.xml"/>
        <exclude domain="sharedpref" path="DEVICE_ID_PREFS.xml"/>
        <exclude domain="sharedpref" path="EXPONEA_TELEMETRY.xml"/>
    </device-transfer>
</data-extraction-rules>
```

Excluding SDK files doesn't require excluding your app's default `<package>_preferences.xml`.

## Troubleshooting

### Build error "Manifest merger failed"

You may get a build error similar to the following, especially in a default new "empty activity" project generated by Android Studio:

```
Manifest merger failed : Attribute application@fullBackupContent value=(@xml/backup_rules) from AndroidManifest.xml:8:9-54
	is also present at [com.exponea.sdk:sdk:5.3.0] AndroidManifest.xml:15:9-70 value=(@xml/exponea_default_backup_rules).
```

On Android 12 and above, you may see the same conflict for `application@dataExtractionRules`.

The SDK ships default backup rules in `AndroidManifest.xml` (`fullBackupContent` for API 30 and below, `dataExtractionRules` for API 31 and above). Android Studio also generates backup rules for new projects. Ensure you [manage the manifest files](https://developer.android.com/build/manage-manifests) so they can be merged properly.

Your options include:

**Use the SDK's backup rules**

- By default, the SDK excludes only the push token and auth data from backup.
- Other SDK SharedPreferences remain in backup. For more information, see [Android Auto Backup and SDK SharedPreferences](#android-auto-backup-and-sdk-sharedpreferences).

  ```xml
  <application
      android:allowBackup="true"
      ...
      >
  </application>
  ```
  Remove `android:fullBackupContent` and `android:dataExtractionRules` from your app's manifest.

**Use your own backup rules**

- For example, to exclude all SDK SharedPreferences files, see examples above.
- `fullBackupContent` and `dataExtractionRules` require different XML root elements (`<full-backup-content>` vs `<data-extraction-rules>`), so **use separate resource files** unless your app targets API 31+ only (see note below).

  ```xml
  <application
      android:allowBackup="true"
      android:fullBackupContent="@xml/backup_rules"
      android:dataExtractionRules="@xml/data_extraction_rules"
      tools:replace="android:fullBackupContent,android:dataExtractionRules"
      ...
      >
  </application>
  ```

  - `res/xml/backup_rules.xml`: `<full-backup-content>` root (API 30 and below). Copy the [Android 11 and below example](#excluding-sdk-data-from-auto-backup-optional) and add your app rules.
  - `res/xml/data_extraction_rules.xml`: `<data-extraction-rules>` root with `<cloud-backup>` and `<device-transfer>` (API 31 and above). Copy the [Android 12 and above example](#excluding-sdk-data-from-auto-backup-optional) and add your app rules.

  > 📘 Note
  >
  > - `minSdkVersion` 31 or higher: omit `android:fullBackupContent` and use only `android:dataExtractionRules="@xml/data_extraction_rules"` with `tools:replace="android:dataExtractionRules"`
  > - Android 12+: `dataExtractionRules` takes precedence over `fullBackupContent`.

**Turn off `auto backup` entirely**:
  ```xml
  <application
      android:allowBackup="false"
      ...
      >
  </application>
  ```
  - Remove `android:fullBackupContent` and `android:dataExtractionRules` from your app's manifest.
