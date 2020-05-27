# Push notifications quick start
> In order to send push notifications, we need to track Firebase token to Exponea servers. Please make sure [tracking](./TRACKING.md) is working before configuring push notifications.

Exponea SDK contains self-check functionality to help you successfully setup push notifications. Self-check will try to track push token, request Exponea backend to send silent push to the device and check the app is ready to open push notifications. These checks correspond to steps in this guide.

To enable self-check functionality set `Exponea.checkPushSetup=true` before initializing the SDK

``` kotlin
    Exponea.checkPushSetup = true
    Exponea.init(context, ExponeaConfiguration(...))
```

> **Quick Tip:** We suggest you turn self-check feature on while implementing the push notifications for the first time or if you need to do some troubleshooting.

## Setting up Firebase
To send/receive push notifications you have to setup Firebase project. [Official Firebase documentation](https://firebase.google.com/docs/android/setup#console) describes this process. You'll need to create a project in Firebase console, add generated `google-services.json` to your app and update gradle scripts.

#### Checklist:
 - `google-services.json` file downloaded from Firebase console is in your **application** folder e.g. *my-project/app/google-services.json*
 - your **application** gradle build file(*my-project/app/build.gradle*) contains `apply plugin: 'com.google.gms.google-services'`
 - your **top level** gradle build file(*my-project/build.gradle*) has `classpath 'com.google.gms:google-services:X.X.X'` listed in build script dependencies.
 - If you run the app, the SDK should track push token to Exponea servers. Self-check will tell you that, or you can find your customer in Exponea web app and check user property `google_push_notification_id`

## Configuring Exponea to use your Firebase project 
 You need to connect Exponea web application to your Firebase project.
   1. Open `Project settings` in your Exponea web app
   2. Navigate to `Channels/Push notifications`
   3. Enter your Firebase server key as `Firebase Cloud Messaging API Key`

[Exponea web app push notification configuration](./FIREBASE.md) guide contains screenshots showing where the data is located.

 #### Checklist:
  - If you run the app, self-check should be able to send and receive silent push notification. ![](pics/self-check.png)
  - you should now be able to send push notifications using Exponea web app. [Sending Push notifications](./PUSH_SEND.md) guide shows how to send a test push notification.

> **Quick Tip:** Exponea service for sending push notifications/Firebase connection may take a minute to properly wake up. If sending push fails, try restarting the app. If the issue persists after 2-3 retries, review your setup.

## Setting up intent filters for opening push notification
To react to push notifications from Exponea you need setup an intent filter for the default notification action `Open app`. You can also setup deep linking by following [detailed push notification documentation](../Documentation/PUSH.md).

Open app action generates an intent with action `com.exponea.sdk.action.PUSH_CLICKED`. To respond to it, you need to setup a BroadcastReceiver in your Android manifest.

``` xml
<receiver
    android:name="MyReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="com.exponea.sdk.action.PUSH_CLICKED" />
    </intent-filter>
</receiver>
```

In the BroadcastReceiver you can launch a corresponding activity(e.g. your main activity). Campaign data is included in the intent as `ExponeaPushReceiver.EXTRA_DATA`.
``` kotlin
class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Extract payload data
        val data = intent.getParcelableExtra<NotificationData>(
          ExponeaPushReceiver.EXTRA_DATA
        )
        // Process the data if you need to

        // Start an activity
        val launchIntent = Intent(context, MainActivity::class.java)
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(context, launchIntent, null)
    }
}
```

 #### Checklist:
 - send a test push notification from Exponea to the device and tap on it. Your broadcast receiver should be called.

## Great job!
 You should now be able to use Exponea push notifications. You can disable the self-check now, or leave it on to check your push setup in every debug build run. 
 
  To further configure push notifications, check the complete documentation for [Configuration](../Documentation/CONFIG.md) and [Push notifications](../Documentation/PUSH.md)
