
## 📣  Push Notifications

Exponea allows you to easily create complex scenarios which you can use to send push notifications directly to your customers. The following section explains how to integrate push notifications into your app.

- [Standard (Firebase) integration](#standard-firebase-integration)
- [Huawei integration](#huawei-integration)


## Standard (Firebase) integration

For firebase push notifications to work, you'll need to setup a few things:
- create a Firebase project in the [Firebase console](https://console.firebase.google.com)
- integrate Firebase into your application
- set the Firebase server key in the Exponea web app
- add and register firebase messaging service

We've created a [Firebase quick start guide](../Guides/PUSH_QUICKSTART_FIREBASE.md) that will guide you through these steps.

## Huawei integration
For huawei push notifications to work, you'll need to setup a few things:
- register for a Huawei developer account at https://id7.cloud.huawei.com
- create a new Huawei app and project in the [AppGallery Connect](https://developer.huawei.com/consumer/en/service/josp/agc/index.html,,AppGalleryConnect#/)
- create Huawei integration in the Exponea web app with your project Client ID and Client Secret
- integrate the Huawei messaging SDK into your app
- add and register huawei messaging service

We've created a [Huawei quick start guide](../Guides/PUSH_QUICKSTART_HUAWEI.md) that will guide you through these steps.

## Automatic tracking of Push Notifications

In the [Exponea SDK configuration](CONFIG.md), you can enable or disable the automatic push notification tracking by setting the Boolean value to the `automaticPushNotification` property and potentially setting up the desired frequency to the `tokenTrackFrequency`(default value is ON_TOKEN_CHANGE).

With `automaticPushNotification` enabled, the SDK will correctly display push notifications from Exponea and track a "campaign" event for every delivered/opened push notification with the correct properties.

## Responding to Push notifications

When creating notification using Exponea Web App, you can choose from 3 different actions to be used when tapping the notification or additional buttons on notification.

### 1. Open app
Open app action generates an intent with action `com.exponea.sdk.action.PUSH_CLICKED`. SDK automatically responds to it by opening the launcher activity of your app. Previous SDK versions (<=2.9.7) required the creation of your own broadcast receiver to handle the open action, but since notification trampolining (opening activity from a receiver or a service) is no longer allowed since Android S, your activity intent will be opened directly from the notification, and this receiver is no longer needed on the application side.

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

> Keep in mind: If deep link intent is send to Activity that is currently active, default behavior is that Intent is delivered to [onNewIntent](https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent)) method rather to [onCreate](https://developer.android.com/reference/android/app/Activity#onCreate(android.os.Bundle)).

### 3. Open web browser
Open web browser is handled automatically by the SDK and no work is required from the developer to handle it.

## Handling notification payload extra data
  You can setup notifications to contain extra data payload. Whenever a notification arrives the Exponea SDK will call `notificationCallback` that you can set on the `Exponea` object. The extras are a `Map<String, String>`.        

#### 💻 Usage

``` kotlin
Exponea.notificationDataCallback = {
     extra -> //handle the extras value
}
```

Note that if a previous data was received and no listener was attached to the callback, that data will be dispatched as soon as a listener is attached.

> If your app is not running in the background, the SDK will auto-initialize when push notification is received. In this case, `Exponea.notificationDataCallback` is not set, so the callback will be called after you attach the listener(next app start). If you need to respond to the notification received immediately, implement your own `FirebaseMessagingService` and set the notification data callback in `onMessageReceived` function before calling `Exponea.handleRemoteMessage`.

> The behaviour of `trackDeliveredPush` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).

## Custom processing of notification actions
When a user clicks on the notification or its buttons, SDK automatically performs linked action (open app, browser, etc.). If you need to do some more processing when this event occurs, you can create a receiver for this purpose. SDK is broadcasting `com.exponea.sdk.action.PUSH_CLICKED`, `com.exponea.sdk.action.PUSH_DEEPLINK_CLICKED` and `com.exponea.sdk.action.PUSH_URL_CLICKED` actions, and you can specify them in intent filer to react to them.

#### 💻 Usage

Registration in AndroidManifest.xml 
``` xml
<receiver
    android:name="MyReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="com.exponea.sdk.action.PUSH_CLICKED" />
        <action android:name="com.exponea.sdk.action.PUSH_DEEPLINK_CLICKED" />  
        <action android:name="com.exponea.sdk.action.PUSH_URL_CLICKED" />
    </intent-filter>
</receiver>
```

Receiver class
``` kotlin
class MyReceiver : BroadcastReceiver() {  
  
  // React on push action click 
  override fun onReceive(context: Context, intent: Intent) {  
        // Extract push data  
        val data = intent.getParcelableExtra<NotificationData>(ExponeaExtras.EXTRA_DATA)  
        val actionInfo = intent.getSerializableExtra(ExponeaExtras.EXTRA_ACTION_INFO) as? NotificationAction  
        val customData = intent.getSerializableExtra(ExponeaExtras.EXTRA_CUSTOM_DATA) as Map<String, String>  
        // Process push data as you need  
    }  
}
```

## Silent push notifications
Exponea web app allows you to setup silent push notifications, that are not displayed to the user. The SDK tracks `campaign` event when the push notification is delivered, just like for regular notifications. There is no opening for those notifications, but if you have set up extra data in the payload, the SDK will call `Exponea.notificationDataCallback` as described in [Handling notification payload extra data](#Handling-notification-payload-extra-data).

## Manual tracking of Push Notifications
In case you decide to deactivate the automatic push notification, or wish to track push notifications from other providers, you can still track events manually.

#### Track Push Token (FCM)

``` kotlin
fun trackPushToken(
        token: String
)
```
#### 💻 Usage

``` kotlin
Exponea.trackPushToken(
        token = "382d4221-3441-44b7-a676-3eb5f515157f"
)
```

#### Track Delivered Push Notification

``` kotlin
fun trackDeliveredPush(
        data: NotificationData? = null,
        timestamp: Double? = null
)
```

> The behaviour of `trackDeliveredPush` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).

#### 💻 Usage

``` kotlin
// create NotificationData from your push payload
val notificationData = NotificationData(
    dataMap = hashMapOf(
        "platform" to "android",
        "subject" to "Subject",
        "type" to "push",
        ...
    ),
    campaignMap = mapOf(
       "utm_campaign" to "Campaign name",
       "utm_medium" to "mobile_push_notification",
       "utm_content" to "en",
       ...
    )
)
Exponea.trackDeliveredPush(
        data = notificationData
        timestamp = currentTimeSeconds()
)
```

> The behaviour of `trackDeliveredPush` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).

#### Track Clicked Push Notification

``` kotlin
fun trackClickedPush(
        data: NotificationData? = null,
        timestamp: Double? = null
)
```

> The behaviour of `trackClickedPush` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).

#### 💻 Usage

``` kotlin
// create NotificationData from your push payload
val notificationData = NotificationData(
    dataMap = hashMapOf(
        "platform" to "android",
        "subject" to "Subject",
        "type" to "push",
        ...
    ),
    campaignMap = mapOf(
       "utm_campaign" to "Campaign name",
       "utm_medium" to "mobile_push_notification",
       "utm_content" to "en",
       ...
    )
)
Exponea.trackClickedPush(
        data = notificationData
        timestamp = currentTimeSeconds()
)
```

> The behaviour of `trackClickedPush` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).

## Troubleshooting
In case of push notifications not working for you, these are frequent issues with the most likely solutions.

### Push notifications do not open the app after clicking on the notification on Xiaomi Redmi devices

Xiaomi MIUI is handling battery optimization in its own way and can sometimes affect the behavior of push notifications. 
If battery optimization is on for devices with MIUI, it can make push notifications stop showing or not working after the click. Unfortunately, there is nothing we can do on our end to prevent this, but you can try this to solve the issues:

- Turn off any battery optimizations in Settings->Battery & Performance you can
- Set the "No restrictions" option in battery saver options for your app
- And (probably) most important, turn off Memory and MIUI Optimization under Developer Options

### Push notification token is missing after anonymization

There is principal usage of `Exponea.anonymize()` as an sign out feature in some applications. Keep in mind that invoking of `anonymize` will remove also a Push notification token from storage. To load a current token, your application should retrieve a valid token manually before using any Push notification feature. So it may be called right after `anonymize` or before/after `identifyCustomer`, it depends on your Push notifications usage.

> Guide how to retrieve a valid Push notification token is written for [FCM](../Guides/PUSH_QUICKSTART_FIREBASE.md) and [HMS](../Guides/PUSH_QUICKSTART_HUAWEI.md).