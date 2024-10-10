---
title: SDK version update guide
excerpt: Update Exponea Android SDK in your app to a new version
slug: android-sdk-version-update
categorySlug: integrations
parentDocSlug: android-sdk-release-notes
---

This guide will help you upgrade your Exponea SDK to the latest major version.

## Update from version 3.x.x to 4.x.x

Updating Exponea SDK to version 4 or higher requires making some changes related to in-app messages callback implementations.

The `InAppMessageCallback` interface was changed and simplified, so you have to migrate your implementation of in-app message action and close handling. This migration requires to split your implementation from `inAppMessageAction` into `inAppMessageClickAction` and `inAppMessageCloseAction`, respectively.

Your implementation may have been similar to the following example:

```kotlin
override fun inAppMessageAction(
    message: InAppMessage,
    button: InAppMessageButton?,
    interaction: Boolean,
    context: Context
) {
    if (button == null) {
        // is close action
        onMessageClose(message, interaction)
    } else {
        // is click action
        onMessageClick(message, button)
    }
}
```

To update to version 4 of the SDK, you must remove the `inAppMessageAction` method and refactor your code as follows:

```kotlin
override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context) {
    // is click action
    onMessageClick(message, button)
}

override fun inAppMessageCloseAction(
    message: InAppMessage,
    button: InAppMessageButton?,
    interaction: Boolean,
    context: Context
) {
    // is close action
    onMessageClose(message, interaction)
}
```

A benefit of the new behaviour is that the method `inAppMessageCloseAction` can be called with a non-null `button` parameter. This happens when a user clicks on the Cancel button and enables you to determine which button has been clicked by reading the button text.

## Update from version 2.x.x to 3.x.x

Updating Exponea SDK to version 3 or higher requires you to make some changes related to push notifications.

### Changes regarding FirebaseMessagingService

In order to keep the SDK as small as possible and avoid including libraries that are not essential for it functionality, an implementation of `FirebaseMessagingService` is no longer included in the SDK. As a result, the SDK no longer has a dependency on the Firebase library.

You are required to make the following changes in your application:

#### If you are already using your own service that extends FirebaseMessagingService

1. You must change the second parameter when calling the `Exponea.handleRemoteMessage` method. It no longer accepts a Firebase `RemoteMessage` but now accepts the message data directly instead.

2. Instead of calling `Exponea.trackPushToken` when a new token is obtained, call `Exponea.handleNewToken` with the application context. This method will track the new token to the Engagement platform. If the SDK is not initialized at the moment of invokation, it will persist the token and track it later, after SDK initialization. 

#### If you do not have your own service and you were relying on the implementation included in the SDK

You must implement `FirebaseMessagingService` in your application since the SDK no longer provides an implementation.

1. Create a service that extends `FirebaseMessagingService`
2. Call `Exponea.handleRemoteMessage` when a message is received
3. Call `Exponea.handleNewToken` when a token is obtained
4. Register this service in your `AndroidManifest.xml`

```kotlin 
class MyFirebaseMessagingService : FirebaseMessagingService() {  
  
    private val notificationManager by lazy {  
  getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager  
    }  
  
  override fun onMessageReceived(message: RemoteMessage) {  
        super.onMessageReceived(message)  
        Exponea.handleRemoteMessage(applicationContext, message.data, notificationManager)  
    }  
  
    override fun onNewToken(token: String) {  
        super.onNewToken(token)  
        Exponea.handleNewToken(applicationContext, token)  
    }  
}
```

``` xml
<service android:name="MyFirebaseMessagingService" android:exported="false">  
     <intent-filter> 
         <action android:name="com.google.firebase.MESSAGING_EVENT" />  
     </intent-filter>
</service>
```

### Changes regarding notification trampolining

Android (from version 12) restricts starting any `Intent` from a notification action indirectly. This means an activity can no longer be started from a service or receiver. In version 3.0.0 of the SDK, we made changes to comply with these rules. The SDK starts all notification action `Intent`s directly (opening the app, opening a web browser, opening a deep link). Previous versions required creating a `BroadcastReceiver` to handle an Intent with the action `com.exponea.sdk.action.PUSH_CLICKED` and start the app on the application side. You must remove this code since this receiver acts as a notification trampoline, which, when targeting Android S and above, can cause the app to crash. You can safely **remove** this code; opening your Launcher Activity on a push click is now handled by the SDK. 

If you still need to do some processing in your app, when a notification action is clicked, you can continue to use the receiver for that as long as you don;t start any intent from it if you are targeting Android 12 and higher.

> ❗️
>
> Broadcast for `com.exponea.sdk.action.PUSH_CLICKED`, `com.exponea.sdk.action.PUSH_DEEPLINK_CLICKED` and `com.exponea.sdk.action.PUSH_URL_CLICKED` actions was removed by mistake in version 3.0.0 and added back in 3.0.1. Please update directly to this patch, if you need to use it.

#### Usage

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

### Changes in public API

Exponea SDK no longer has a dependency on the Firebase library. For this reason, the signature of public methods that accepted a Firebase `RemoteMessage` has changed. These methods now accept message data directly instead. You can obtain them by calling `getData()` on the `RemoteMessage` object.

```
fun handleRemoteMessage(
  applicationContext: Context,  
  message: RemoteMessage?, 
  manager: NotificationManager,  
  showNotification: Boolean = true  
): Boolean
```

has changed to

```
fun handleRemoteMessage(  
  applicationContext: Context,  
  messageData: Map<String, String>?,  
  manager: NotificationManager,  
  showNotification: Boolean = true  
): Boolean
```

and 

`fun isExponeaPushNotification(message: RemoteMessage?): Boolean`

has changed to  

`fun isExponeaPushNotification(messageData: Map<String, String>?): Boolean`

> ❗️
>
> Invoking `Exponea.handleNewToken`, `Exponea.handleNewHmsToken`, and `Exponea.handleRemoteMessage` is allowed before SDK initialization in case it was initialized previously. In such a case, these methods will track events with the configuration of the last initialization. Please consider initializing the SDK in `Application::onCreate` to ensure a fresh configuration is applied in case of an update of your application.

