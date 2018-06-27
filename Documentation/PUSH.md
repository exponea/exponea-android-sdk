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

When It's coming to setting up Firebase inside you application, you have options

#### Using Firebase Assistant inside Android Studio
  1. To open the Firebase Assistant in Android Studio:
  **Click Tools**  > **Firebase** to open the `Assistant window`.
  2. Click **Cloud Messaging** , then click the provided tutorial link **Set Up Cloud Messaging**.
  3. Click the **Connect** button to Firebase button to connect to Firebase and add the necessary code to your app.
  4.  Click the **Add FCM to your App** button to allow `gradle` add needed dependencies for you
  5. That's it! You're good to go, checkout [Firebase Docs](https://firebase.google.com/docs/android/setup?authuser=0) if you have any questions

#### Manually add Firebase
  1. Open you Project and navigate to `Project overview -> Project Settings`
  2. Click Add Firebase to your Android app and follow the setup steps. If you're importing an existing Google project, this may happen automatically and you can just download the config file.
  3. When prompted, enter your app's **package name**. It's important to enter the package name your app is using; this can only be set when you add an app to your Firebase project.
  4. During the process, you'll download a `google-services.json` file. You can download this file again at any time.
  5. Add rules to your root-level build.gradle file, to include the google-services plugin and the Google's Maven repository:
  ```
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

  ```

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

7. That's should be it! Checkout [Firebase Docs](https://firebase.google.com/docs/android/setup?authuser=0) if you have any questions


  ### Enabling Exponea push notifications
Once you have configured Firebase you it need to obtaint **Google Cloud Messaging API key**. You can find in your Firebase Project's Settings: `Project Overview` -> `Project Settings` -> `Cloud Messaging`. `Server Key` is your **Google Cloud Messaging API Key**. All you need to do is to enter this key in the input field on the **Project / Settings / Push Notifications** in the Exponea web application.

## 🔍 Automatic track Push Notification

In the Exponea SDK configuration, you can enable or disable the automatic push notification tracking setting the Boolean value to the `isAutoPushNotification` property.

If the `isAutoPushNotification` is enabled, then the SDK will add track the "campaign" event with the correct properties.

In case you decide to deactivate the automatic push notification, you can still track this event manually.

#### Track FCM Token

```
fun trackFcmToken(
        fcmToken: String
)
```

#### 💻 Usage

```

Exponea.trackFcmToken(
        fcmToken = "382d4221-3441-44b7-a676-3eb5f515157f"
)
```

#### Track Delivered Push Notification

```
fun trackDeliveredPush(
        fcmToken: String,
        timestamp: Long? = null
)
```

#### 💻 Usage

```
val customerIds = CustomerIds(registered = "john@doe.com")

Exponea.trackDeliveredPush(
        timestamp = Date().time
)
```

#### Track Clicked Push Notification

```
fun trackClickedPush(
        fcmToken: String,
        timestamp: Long? = null
)
```

#### 💻 Usage

```

Exponea.trackClickedPush(
        fcmToken = "382d4221-3441-44b7-a676-3eb5f515157f"
        timestamp = Date().time
)
```
