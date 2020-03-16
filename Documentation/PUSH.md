## 📣  Push Notification

Exponea SDK allows you to easily create complex scenarios which you can use to send push notifications directly to your customers. The following section explains how to enable receiving push notifications.

For push notifications to work, you need a working Firebase project. The following steps shows you how to create one. If you already have created a Firebase project and you have your **project number (or sender ID)** and **Google Cloud Messaging API key (or Server Key)**, you may skip this part of the tutorial and proceed directly to enabling of the push notifications in the Exponea SDK.

### Setting Up  Firebase project

### Create your project
1. In your preferred browser, navigate to [Firebase Console](https://console.firebase.google.com/u/0/)
2. Create a Firebase project in the Firebase console, if you don't already have one. Click Add project. If you already have an existing Google project associated with your mobile app, select it from the Project name drop down menu. Otherwise, enter a project name to create a new project.
3. Optional: Edit your Project ID. Your project is given a unique ID automatically, and it's used in publicly visible Firebase features such as database URLs and your Firebase Hosting subdomain. You can change it now if you want to use a specific subdomain.
4. Follow the remaining setup steps and click Create project (or Add Firebase if you're using an existing project) to begin provisioning resources for your project. This typically takes a few minutes. When the process completes, you'll be taken to the project overview.


### Add Firebase to Your App

When it comes to setting up Firebase inside you application, you have multiple options

#### Using Firebase Assistant inside Android Studio
  1. To open the Firebase Assistant in Android Studio:
  **Click Tools**  > **Firebase** to open the `Assistant window`.
  2. Click **Cloud Messaging** , then click the provided tutorial link **Set Up Cloud Messaging**.
  3. Click the **Connect** button to Firebase button to connect to Firebase and add the necessary code to your app.
  4.  Click the **Add FCM to your App** button to allow `gradle` add needed dependencies for you
  5. That's it! You're good to go, check out [Firebase Docs](https://firebase.google.com/docs/android/setup?authuser=0) if you have any questions

#### Manually add Firebase
  1. Open you Project and navigate to `Project overview -> Project Settings`
  2. Click Add Firebase to your Android app and follow the setup steps. If you're importing an existing Google project, this may happen automatically and you can just download the config file.
  3. When prompted, enter your app's **package name**. It's important to enter the package name your app is using; this can only be set when you add an app to your Firebase project.
  4. During the process, you'll download a `google-services.json` file. You can download this file again at any time.
  5. Add rules to your root-level build.gradle file, to include the google-services plugin and the Google's Maven repository:
  ``` gradle
  buildscript {
      // ...
      dependencies {
          // ...
          classpath 'com.google.gms:google-services:4.0.1' // google-services plugin
      }
  }

  allprojects {
      // ...
      repositories {
          // ...
          google() // Google's Maven repository
      }
  }
  )
  ```

6. Then, in your module Gradle file (usually the app/build.gradle), add the apply plugin line at the bottom of the file to enable the Gradle plugin:

  ``` gradle
  apply plugin: 'com.android.application'

  android {
    // ...
  }

  dependencies {
    // ...
    implementation 'com.google.firebase:firebase-core:16.0.1'
    implementation 'com.google.firebase:firebase-messaging:17.0.0'

    // Getting a "Could not find" error? Make sure you have
    // added the Google maven respository to your root build.gradle
  }

  // ADD THIS AT THE BOTTOM
  apply plugin: 'com.google.gms.google-services'

  ```

7. That's should be it! Check out [Firebase Docs](https://firebase.google.com/docs/android/setup?authuser=0) if you have any questions


  ### Confuring Exponea Web App
Once you have configured Firebase you it need to obtained **Google Cloud Messaging API key**. You can find in your Firebase Project's Settings. See [this guide](../Guides/FIREBASE.md)
to help you configure Exponea web APP


## 🔍 Automatic tracking of Push Notifications

In the [Exponea SDK configuration](CONFIG.md), you can enable or disable the automatic push notification tracking by setting the Boolean value to the `automaticPushNotification` property and potentially setting up the desired frequency to the `tokenTrackFrequency`(default value is ON_TOKEN_CHANGE).

With `automaticPushNotification` enabled, the SDK will correctly display push notifications and track a "campaign" event for every delivered/opened push notification with the correct properties.

## 🔍 Responding to Push notifications

When creating notification using Exponea Web App, you can choose from 3 different actions to be used when tapping the notification or additional buttons on notification.

### 1. Open app
Open app action generates an intent with action `com.exponea.sdk.action.PUSH_CLICKED` that you can respond to in any application. Most common use case is opening your app and starting an activity. To do this, you need to setup a BroadcastReceiver in your Android manifest that will respond to intents with action `com.exponea.sdk.action.PUSH_CLICKED`.

``` xml
<receiver
    android:name=".services.MyReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="com.exponea.sdk.action.PUSH_CLICKED" />
    </intent-filter>
</receiver>
```

In the BroadcastReceiver you should launch your activity. Campaign data is included in the intent as `ExponeaPushReceiver.EXTRA_DATA`.
``` kotlin
class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Extract payload data
        val data = intent.getParcelableExtra<NotificationData>(
          ExponeaPushReceiver.EXTRA_DATA
        )
        Log.i("Receiver", "Payload: $data")
        // Process the data if you need to

        // Start an activity
        val launchIntent = Intent(context, MainActivity::class.java)
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(context, launchIntent, null)
    }
}
```

### 2. Deep link
Deep link action creates "view" intent that contains the url specified when setting up this action. To respond to this intent, create intent filter on the activity that should handle it in your Android manifest file. More information can be found in the [official Android documentation](https://developer.android.com/training/app-links/deep-linking).
``` xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <!-- Accepts URIs that begin with "my-schema://”-->
    <data android:scheme="my-schema" />
</intent-filter>
```


### 3. Open web browser
Open web browser is handled automatically by the SDK and no work is required from the developer to handle it.

## 🔍 Manual tracking of Push Notifications
In case you decide to deactivate the automatic push notification, you can still track events manually.

#### Track Push Token (FCM)

``` kotlin
fun trackPushToken(
        fcmToken: String
)
```
#### 💻 Usage

``` kotlin
Exponea.trackPushToken(
        fcmToken = "382d4221-3441-44b7-a676-3eb5f515157f"
)
```

#### Track Delivered Push Notification

``` kotlin
fun trackDeliveredPush(
        data: NotificationData? = null,
        timestamp: Double? = null
)
```

#### 💻 Usage

``` kotlin
// create NotificationData from your push payload
val notificationData = NotificationData(
    subject = "my subject",
    campaignName = "My campaign",
    actionName = "my-action",
    platform = "android",
    ...
)
Exponea.trackDeliveredPush(
        data = notificationData
        timestamp = currentTimeSeconds()
)
```

#### Track Clicked Push Notification

``` kotlin
fun trackClickedPush(
        data: NotificationData? = null,
        timestamp: Double? = null
)
```

#### 💻 Usage

``` kotlin
// create NotificationData from your push payload
val notificationData = NotificationData(
    subject = "my subject",
    campaignName = "My campaign",
    actionName = "my-action",
    platform = "android",
    ...
)
Exponea.trackClickedPush(
        data = notificationData
        timestamp = currentTimeSeconds()
)
```
## 🔍 Tracking notification extra data
  Some notifications can have an extra data payload. Whenever a notification arrives the Exponea SDK will call `notificationCallback` that you can set on the `Exponea` object. The extras are a `Map<String, String>`.

#### 💻 Usage

``` kotlin
Exponea.notificationDataCallback = {
     extra -> //handle the extras value
}
```

Note that if a previous data was received and no listener was attached to the callback, that data will be dispatched as soon as a listener is attached.
