

# Firebase push notifications quick start
> In order to send push notifications, we need to track Firebase token to Exponea servers. Please make sure [tracking](./TRACKING.md) is working before configuring push notifications.

Exponea SDK contains self-check functionality to help you successfully set up push notifications. Self-check will try to track push token, request Exponea backend to send a silent push to the device, and check the app is ready to open push notifications. These checks correspond to steps in this guide.

To enable self-check functionality set `Exponea.checkPushSetup=true` before initializing the SDK

``` kotlin
    Exponea.checkPushSetup = true
    Exponea.init(context, ExponeaConfiguration(...))
```

> **Quick Tip:** We suggest you turn the self-check feature on while implementing the push notifications for the first time or if you need to do some troubleshooting.

## Setting up Firebase
To send/receive push notifications, you have to set up the Firebase project. [Official Firebase documentation](https://firebase.google.com/docs/android/setup#console) describes this process. You'll need to create a project in the Firebase console, add generated `google-services.json` to your app and update Gradle scripts.

#### Checklist:
 - `google-services.json` file downloaded from the Firebase console is in your **application** folder e.g., *my-project/app/google-services.json*
 - your **application** gradle build file(*my-project/app/build.gradle*) contains `apply plugin: 'com.google.gms.google-services'`
 - your **top level** gradle build file(*my-project/build.gradle*) has `classpath 'com.google.gms:google-services:X.X.X'` listed in build script dependencies.

 ## Implement FirebaseMessagingService

Our automatic tracking relies on the implementation of FirebaseMessagingService. Therefore, you will need to create and register a service that extends FirebaseMessagingService.

> **Note:**We decided not to include this implementation in our SDK since we want to keep it as small as possible and avoid including the libraries that are not essential for its functionality. You can copy this code and use it in your app.

1. Create the service
``` kotlin
import android.app.NotificationManager  
import android.content.Context  
import com.exponea.sdk.Exponea  
import com.google.firebase.messaging.FirebaseMessagingService  
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (!Exponea.handleRemoteMessage(applicationContext, message.data, notificationManager)) {
            // push notification is from another push provider
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Exponea.handleNewToken(applicationContext, token)
    }
}
```

2. Register the service in your AndroidManifest.xml
```xml
<service android:name="MyFirebaseMessagingService" android:exported="false" >  
     <intent-filter> 
         <action android:name="com.google.firebase.MESSAGING_EVENT" />  
     </intent-filter>
</service>
```
Exponea SDK will only handle push notification messages coming from Exponea servers. You can also use helper method `Exponea.isExponeaPushNotification()`.

 If you run the app, the SDK should track push token to Exponea servers. Self-check will tell you that, or you can find your customer in the Exponea web app and check user property `google_push_notification_id`

## Configuring Exponea to use your Firebase project 
 You need to connect the Exponea web application to your Firebase project.
   1. Open `Project settings` in your Exponea web app
   2. Navigate to `Channels/Push notifications`
   3. Enter your Firebase server key as `Firebase Cloud Messaging API Key`

[Exponea web app push notification configuration](./FIREBASE.md) guide contains screenshots showing where the data is located.

 #### Checklist:
  - If you run the app, self-check should be able to send and receive silent push notification. 
  ![](pics/self-check.png)
  - you should now be able to send push notifications using the Exponea web app. [Sending Push notifications](./PUSH_SEND.md) guide shows how to send a test push notification.

> **Quick Tip:** Exponea service for sending push notifications/Firebase connection may take a minute to wake up properly. If sending push fails, try restarting the app. If the issue persists after 2-3 retries, review your setup.

 #### Checklist:
 - send a test push notification from Exponea to the device and tap on it. Your broadcast receiver should be called.

## Great job!
 You should now be able to use Exponea push notifications. You can disable the self-check now or leave it on to check your push setup in every debug build run. 
 
  To further configure push notifications, check the complete documentation for [Configuration](../Documentation/CONFIG.md) and [Push notifications](../Documentation/PUSH.md)