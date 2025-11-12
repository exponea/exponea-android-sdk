---
title: Initial SDK setup
excerpt: Install and configure the Android SDK
slug: android-sdk-setup
categorySlug: integrations
parentDocSlug: android-sdk
---

## Install the SDK

The Exponea Android SDK can be installed or updated using [Gradle](https://gradle.org/) or [Maven](https://maven.apache.org/). In case of Gradle, you can use Kotlin or Groovy for your build configuration files.

> 📘
>
> Refer to https://github.com/exponea/exponea-android-sdk/releases for the latest Exponea Android SDK release.

### Gradle (Kotlin)

1. In your app's `build.gradle.kts` file, add `com.exponea.sdk:sdk` inside the `dependencies { }` section:
   ```kotlin
   implementation("com.exponea.sdk:sdk:4.7.0")
   ```
2. Rebuild your project (`Build` > `Rebuild Project`).

### Gradle (Groovy)

1. In your app's `build.gradle` file, add `com.exponea.sdk:sdk` inside the `dependencies { }` section:
   ```groovy
   implementation 'com.exponea.sdk:sdk:4.7.0'
   ```
2. Rebuild your project (`Build` > `Rebuild Project`).

### Maven

1. In your app's `pom.xml` file, add `com.exponea.sdk:sdk` inside the `<dependencies> </dependencies>` section:
   ```xml
   <dependency>
      <groupId>com.exponea.sdk</groupId>
      <artifactId>sdk</artifactId>
      <version>4.7.0</version>
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

The required configuration parameters are `projectToken`, `authorization`, and `baseURL`. You can find these in the Bloomreach Engagement webapp under `Project settings` > `Access management` > `API`.

> 📘
>
> Refer to [Mobile SDKs API access management](mobile-sdks-api-access-management) for details.

You can configure the SDK in [code](#using-configuration-in-code) (preferred) or using a [JSON configuration file](#using-a-configuration-file).

### Using configuration in code

Import the SDK:

```kotlin
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration

```

Initialize the SDK:

```kotlin
val configuration = ExponeaConfiguration()

configuration.authorization = "Token YOUR_API_KEY"
configuration.projectToken = "YOUR_PROJECT_TOKEN"
configuration.baseURL = "https://api.exponea.com"

Exponea.init(this, configuration)
```


#### Configure application ID

**Multiple mobile apps:** If your Engagement project supports multiple mobile apps, specify the `applicationId` in your configuration. This helps distinguish between different apps in your project.


```kotlin
configuration.applicationId = "<Your application id>" 
```

Make sure your `applicationId` value matches exactly Application ID configured in your Bloomreach Engagement under **Project Settings > Campaigns > Channels > Push Notifications.**

**Single mobile app:** If your Engagement project supports only one app, you can skip the `applicationId` configuration. The SDK will automatically use the default value "default-application".


### Using a configuration file

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
> Refer to [`exponea_configuration.json`](https://github.com/exponea/exponea-android-sdk/blob/main/app/src/main/assets/exponea_configuration.json) in the [example app](https://documentation.bloomreach.com/engagement/docs/android-sdk-example-app) for an example configuration file.

### Where to place the SDK initialization code

#### In an application subclass

Your [`Application`](https://developer.android.com/reference/android/app/Application)'s `onCreate()` method is the best place to do the initialization - it's called only once and very early in the application lifecycle. Application is the class for maintaining global application state.

It should look similar to the example below:

```kotlin
class MyApplication : Application() {
  override fun onCreate(){
    super.onCreate()

    val configuration = ExponeaConfiguration()

    configuration.authorization = "Token jlk5askvxss99asmnbgayrks333"
    configuration.projectToken = "47b5cc2c-e661-11e8-bb95-0a580a201692"
    configuration.baseURL = "https://api.exponea.com"

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

The SDK can be further configured by setting additional properties of the `ExponeaConfiguration` object or `exponea_configuration.json` file. For a complete list of available configuration parameters, refer to the [Configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) documentation.

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

### Data flushing

Read [Data flushing](https://documentation.bloomreach.com/engagement/docs/android-sdk-data-flushing) to learn more about how the SDK uploads data to the Engagement API and how to customize this behavior.

## Troubleshooting

### Build error "Manifest merger failed"

You may get a build error similar to the following, especially in a default new "empty activity" project generated by Android Studio:

```
Manifest merger failed : Attribute application@fullBackupContent value=(@xml/backup_rules) from AndroidManifest.xml:8:9-54
	is also present at [com.exponea.sdk:sdk:4.7.0] AndroidManifest.xml:15:9-70 value=(@xml/exponea_default_backup_rules).
```

The SDK and the new app generated by Android Studio both enable the [auto backup feature](https://developer.android.com/guide/topics/data/autobackup) in `AndroidManifest.xml` but each with their own backup rules. It is up to you as a developer to [manage the manifest files](https://developer.android.com/build/manage-manifests) and ensure they can be merged properly.

Your options include:
- Use the SDK's backup rules:
  ```xml
  <application
      android:allowBackup="true"
      ...
      >
  </application>
  ```
  (Remove `android:fullBackupContent="@xml/backup_rules"`)
- Define your own backup rules in `app/src/main/res/xml/backup_rules.xml` and specify they should replace the SDK's backup rules:
  ```xml
  <application
      android:allowBackup="false"
      android:fullBackupContent="@xml/backup_rules"
      tools:replace="android:fullBackupContent"
      ...
      >
  </application>
  ```
- Turn off auto backup:
  ```xml
  <application
      android:allowBackup="false"
      ...
      >
  </application>
  ```
  (Remove `android:fullBackupContent="@xml/backup_rules"`)
