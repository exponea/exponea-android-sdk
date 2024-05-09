---
title: Push Notifications
excerpt: Enable push notifications in your app using the Android SDK
slug: android-sdk-push-notifications
categorySlug: integrations
parentDocSlug: android-sdk
---

Engagement enables sending push notifications to your app users using [scenarios](https://documentation.bloomreach.com/engagement/docs/scenarios-1). The mobile application handles the push message using the SDK and renders the notification on the customer's device.

Push notifications can also be silent, used only to update the app’s interface or trigger some background task.

> 📘
>
> Refer to [Mobile Push Notifications](https://documentation.bloomreach.com/engagement/docs/mobile-push-notifications#creating-a-new-notification) to learn how to create push notifications in the Engagement web app.

## Integration

This section describes the steps to add the minimum push notification functionality (receive alert notifications) to your app.

### Standard (Firebase) Integration

To be able to send [push notifications](https://documentation.bloomreach.com/engagement/docs/android-push-notifications) from the Engagement platform and receive them in your app on Android devices, you must:

1. Set up a Firebase project.
2. Implement Firebase messaging in your app.
3. Configure the Firebase Cloud Messaging integration in the Engagement web app.

> 📘
>
> Follow the instructions in [Firebase Cloud Messaging](https://documentation.bloomreach.com/engagement/docs/android-sdk-firebase).

> 👍
>
> Please note that with Google deprecating and removing the FCM legacy API in June 2024, Bloomreach Engagement is now using Firebase HTTP v1 API.
>
> If your Engagement project uses a deprecated version of the Firebase integration, you must [readd and reconfigure the FCM integration following the current instructions](https://documentation.bloomreach.com/engagement/docs/android-sdk-firebase#configure-the-firebase-cloud-messaging-integration-in-engagement).
>
> ![](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/fcm-deprecated.png)



### Huawei Integration

To be able to send [push notifications](https://documentation.bloomreach.com/engagement/docs/android-push-notifications) from the Engagement platform and receive them in your app on Huawei devices, you must:

1. Set up Huawei Mobile Services (HMS)
2. Implement HMS in your app.
3. Configure the Huawei Push Service integration in the Engagement web app.

> 📘
>
> Follow the instructions in [Huawei Mobile Services](https://documentation.bloomreach.com/engagement/docs/android-sdk-huawei).

### Request Notification Permission

As of Android 13 (API level 33), a new runtime notification permission `POST_NOTIFICATIONS` must be registered in your `AndroidManifest.xml` and must also be granted by the user for your application to be able to show push notifications.

The SDK already registers the `POST_NOTIFICATIONS` permission.

The runtime permission dialog to ask the user to grant the permission must be triggered from your application. You may use SDK API for that purpose:

```kotlin
Exponea.requestPushAuthorization(requireContext()) { granted ->
    Logger.i(this, "Push notifications are allowed: $granted")
}
```

The behavior of this callback is as follows:

* For Android API level <33:
  * Permission is not required, return `true` automatically.
* For Android API level 33+:
  * Show the dialog, return the user's decision (`true`/`false`).
  * In case of previously granted permission, don't show the dialog return `true`.

## Customization

This section describes the customizations you can implement once you have integrated the minimum push notification functionality.

### Configure Automatic Push Notification Tracking

By default, the SDK tracks push notifications automatically. In the [SDK configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration), you can set the desired frequency using the `tokenTrackFrequency` property (default value is `ON_TOKEN_CHANGE`). You can also disable automatic push notification tracking by setting the Boolean value of the `automaticPushNotification` property to `false`.

If `automaticPushNotification` is enabled, the SDK will display push notifications from Engagement and track a "campaign" event for every delivered/opened push notification with the relevant properties.

### Respond to Push Notifications

When [creating a push notification](https://documentation.bloomreach.com/engagement/docs/mobile-push-notifications#creating-a-new-notification) in the Engagment web app, you can choose from three different actions to be performed when tapping the notification or additional buttons displayed with the notification.

#### Open App

The "Open app" action generates an intent with action `com.exponea.sdk.action.PUSH_CLICKED`. The SDK automatically responds to it by opening your app's launcher activity.

> ❗️
>
> Previous SDK versions (<=2.9.7) required the creation of your own broadcast receiver to handle the open action, but since notification trampolining (opening activity from a receiver  or a service) is no longer allowed since Android S, your activity intent will be opened directly from the notification, and this receiver is no longer needed on the application side.

#### Deep link

The "Deep link" action creates a "view" intent that contains the URL specified when setting up the action in Engagement. To respond to this intent, create an intent filter on the activity that handles it in your Android manifest file. For details, refer to [Create Deep Links to App Content](https://developer.android.com/training/app-links/deep-linking) in the official Android documentation.

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <!-- Accepts URIs that begin with "my-schema://”-->
    <data android:scheme="my-schema" />
</intent-filter>
```

> ❗️
>
> If deep link intent is sent to an Activity that is currently active, the default behavior is that the intent is delivered to [onNewIntent](https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent)) method rather than [onCreate](https://developer.android.com/reference/android/app/Activity#onCreate(android.os.Bundle)).

#### Open web browser

The "Open web browser" action is handled automatically by the SDK and no work is required from the developer to handle it.

### Handle Additional Data Payload

When [creating a push notification](https://documentation.bloomreach.com/engagement/docs/mobile-push-notifications#creating-a-new-notification) in the Engagment web app, you can set it up to contain additional data. Whenever a notification arrives, the SDK will call `notificationCallback`, which you can set on the `Exponea` object. The additional data is provided as a `Map<String, String>`.

``` kotlin
Exponea.notificationDataCallback = {
     extra -> // handle the additional data
}
```

Note that if the SDK previously received any additional data while no listener was attached to the callback, it will dispatch that data as soon as a listener is attached.

> 👍
>
> `Exponea.notificationDataCallback` callback will be called after you attach the listener (next app start) and the SDK is initialized. If you need to respond to the notification received immediately, implement your own `FirebaseMessagingService` and set the notification data callback in `onMessageReceived` function before calling `Exponea.handleRemoteMessage`.

> ❗️
>
> The behaviour of `trackDeliveredPush` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).

### Custom Processing of Notification Actions

When a user clicks on a notification or its buttons, the SDK automatically performs the configured action (open app, browser, etc.). If you require additional processing when this event occurs, you can create a receiver for this purpose. The SDK broadcastings `com.exponea.sdk.action.PUSH_CLICKED`, `com.exponea.sdk.action.PUSH_DEEPLINK_CLICKED`, and `com.exponea.sdk.action.PUSH_URL_CLICKED` actions, and you can specify them in an intent filter to respond to them.

Registration in `AndroidManifest.xml`:

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

Receiver class:

```kotlin
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

### Silent Push Notifications

The Engagement web app allows you to set up silent push notifications, that are not displayed to the user. The SDK tracks a `campaign` event when a silent push notification is delivered. Silent push notifications cannot be opened but if you have set up extra data in the payload, the SDK will call `Exponea.notificationDataCallback` as described in [Handle Extra Data Payload](#handle-extra-data-payload).

### Manually Track Push Notifications

If you disable [automatic push notification tracking](#configure-automatic-push-notification-tracking) or if you want to track push notification from other providers, you can manually track events related to push notifications.

#### Track Push Token (FCM)

Use the `trackPushToken` method to manually track the FCM push token:

``` kotlin
Exponea.trackPushToken(
        token = "382d4221-3441-44b7-a676-3eb5f515157f"
)
```

Invoking this method will track the push token immediately regardless of the [SDK configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configutation) for `tokenTrackFrequency`.

#### Track Delivered Push Notification

Use the `trackDeliveredPush` method to manually track a delivered push notification:

```kotlin
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

> ❗️
>
> The behaviour of `trackDeliveredPush` may be affected by the tracking consent feature, which, when enabled, requires explicit consent for tracking. Read more in the [tracking consent documentation](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking-consent).

#### Track Clicked Push Notification

Use the `trackClickedPush` method to manually track a clicked push notification:

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

> The behaviour of `trackClickedPush` may be affected by the tracking consent feature, which, when enabled, requires explicit consent for tracking. Read more in the [tracking consent documentation](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking-consent).

### Custom Push Notification Data Processing

If the provided `Exponea.handleRemoteMessage` method does not fit the requirements of your app, or you decide to disable automatic push notifications, you must handle push notifications and process their payload yourself.

Notification payloads are generated from (possibly complex) scenarios in the Engagement platform and contain all data for Android, iOS and web platforms. Therefore, the payload itself can be complex.

Notification payloads use a JSON data structure.

#### Payload Example

```json
{
    "notification_id": 123,
    "url": "https://example.com/main_action",
    "title": "Notification title",
    "action": "app|browser|deeplink|self-check",
    "message": "Notification message",
    "image": "https://example.com/image.jpg",
    "actions": [
        {"title": "Action 1", "action": "app|browser|deeplink", "url": "https://example.com/action1"}
    ],
    "sound": "default",
    "aps": {
        "alert": {"title": "Notification title", "body": "Notification message"},
        "mutable-content": 1
    },
    "attributes": {
        "event_type": "campaign",
        "campaign_id": "123456",
        "campaign_name": "Campaign name",
        "action_id": 1,
        "action_type": "mobile notification",
        "action_name": "Action 1",
        "campaign_policy": "policy",
        "consent_category": "General consent",
        "subject": "Subject",
        "language": "en",
        "platform": "ios|android",
        "sent_timestamp": 1631234567.89,
        "recipient": "user@example.com"
    },
    "url_params": {"param1": "value1", "param2": "value2"},
    "source": "xnpe_platform",
    "silent": false,
    "has_tracking_consent": true,
    "consent_category_tracking": "Tracking consent name"
}
```

## Troubleshooting

If push notifications aren't working as expected in your app, consider the following frequent issues and their possible solutions:

### Clicking on a push notification does not open the app on Xiaomi Redmi devices

Xiaomi MIUI handles battery optimization in its own way, which can sometimes affect the behavior of push notifications.

If battery optimization is on for devices running MIUI, it can make push notifications stop showing or not working after the click. Unfortunately, there is nothing we can do on our end to prevent this, but you can try this to solve the issues:

- Turn off any battery optimizations in `Settings` > `Battery & Performance`.
- Set the "No restrictions" option in the battery saver options for your app.
- And (probably) most important, turn off `Memory and MIUI Optimization` under `Developer Options`.

### Push notification token is missing after anonymization

Your app may be using `Exponea.anonymize()` as a sign out feature.

Keep in mind that invoking the `anonymize` method will remove the push notification token from storage. Your application should retrieve a valid token manually before using any push notification features. You may do this directly after `anonymize` or before or after `identifyCustomer`, depending on your push notifications usage.

> 📘
>
> Refer to [Firebase Cloud Messaging](https://documentation.bloomreach.com/engagement/docs/android-sdk-firebase) and [Huawai Mobile Services](https://documentation.bloomreach.com/engagement/docs/android-sdk-huawei) for information on how to retrieve a valid push notification token.

### Multiple customer profiles have the same push notification token assigned

This is most likely because your app does not call [`anonymize`](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking#anonymize) when a user logs out.

It's essential to call `anonymize` when a user logs out of your app to ensure that the push notification token is removed from that user's profile. The token will be assigned to the user who logs in next. If your app fails to call `anonymize` at one user's logout and a different user logs in, both users will have the same token in their profile.