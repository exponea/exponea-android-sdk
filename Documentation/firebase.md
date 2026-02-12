---
title: Firebase Cloud Messaging for Android SDK
excerpt: Integrate Firebase Cloud Messaging in your app to support Engagement push notifications on Android devices
slug: android-sdk-firebase
categorySlug: integrations
parentDocSlug: android-sdk-push-notifications
---

To be able to send [Push notifications for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications) from the Engagement platform and receive them in your app on Android devices, you must set up a Firebase project, implement Firebase messaging in your app, and configure the Firebase Cloud Messaging integration in the Engagement web app.

> 👍
>
> The SDK provides a push setup self-check feature to help developers successfully set up push notifications. The self-check will try to track the push token, request the Engagement backend to send a silent push to the device, and check if the app is ready to open push notifications.
>
> To enable the setup check, set `Exponea.checkPushSetup = true` before [initializing the SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup#initialize-the-sdk).
>
> We suggest you turn the self-check feature on while implementing the push notifications for the first time or if you need to do some troubleshooting.

## Set up Firebase

First, you must set up a Firebase project. For step-by-step instructions, please refer to [Add Firebase to your Android project](https://firebase.google.com/docs/android/setup#console) in the official Firebase documentation.

To summarize, you'll create a project using the Firebase console, download a generated `google-services.json` configuration file and add it to your app, and update the Gradle build scripts in your app.

#### Checklist:
- [ ] The `google-services.json` file downloaded from the Firebase console is in your **application** folder, for example, *my-project/app/google-services.json*.
- [ ] Your **application** Gradle build file (for example, *my-project/app/build.gradle*) contains `apply plugin: 'com.google.gms.google-services'`.
- [ ] Your **top level** Gradle build file (for example, *my-project/build.gradle*) has `classpath 'com.google.gms:google-services:X.X.X'` listed in build script dependencies.

## Implement Firebase messaging in your app

Next, you must create and register a service that extends `FirebaseMessagingService`. The SDK's automatic tracking relies on your app providing this implementation.

> 👍
>
>  This implementation is not included in the SDK in order to keep it as small as possible and avoid including the libraries that are not essential for its functionality. You can copy the example code below and use it in your app.


1. Create the service:
   ```kotlin
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
2. Register the service in `AndroidManifest.xml`:
   ```xml
    <service android:name="MyFirebaseMessagingService" android:exported="false" >  
        <intent-filter> 
            <action android:name="com.google.firebase.MESSAGING_EVENT" />  
        </intent-filter>
    </service>   
   ```

The SDK will only handle push notification messages sent from the Engagement platform. A helper method `Exponea.isExponeaPushNotification()` is also provided.

After running your application, the SDK tracks the push token to the Engagement platform. If you enabled the self-check feature, it will confirm successful tracking.
You can also verify token tracking manually by locating the customer in the Bloomreach Engagement web application:

- **SDK versions below 4.6.0:** Check the customer property `google_push_notification_id`
- **SDK versions 4.6.0 and higher:** Check the `notification_state` event with property `push_notification_token`

> ❗️Important
>
> SDK versions 4.6.0 and higher use event-based token tracking to support multiple mobile applications per project. Learn more about [Token tracking via notification_state event](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#token-tracking-via-notification_state-event).

A push token is typically generated at the first application start, but it has its own lifecycle. Your `FirebaseMessagingService` implementation is triggered only if a token is created or its value has changed. Please validate your expectations against the defined [token update triggers](https://firebase.google.com/docs/cloud-messaging/android/client#sample-register)

> ❗️
>
> As of Android 13 (API level 33), a runtime notification permission must be registered in your `AndroidManifest.xml` and must also be granted by the user for your application to be able to show push notifications. The SDK takes care of registering the permission. However, your app must ask for notification permission from the user by invoking `Exponea.requestPushAuthorization(context)`. Refer to [Request notification permission](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#request-notification-permission) for details.
>
> If your marketing flow strictly requires normal push notifications usage, configure the SDK to track only authorized push tokens by setting [Configuration for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) requirePushAuthorization to `true`. Refer to [Require notification permission](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#require-notification-permission) for details.

> ❗️
>
> If you are integrating a new Firebase project in an existing project, or if you are changing Firebase project completely, you may face an issue that your 'FirebaseMessagingService' is not called automatically.
>
> To retrieve a fresh FCM token, consider requesting a token manually as soon as possible after Firebase initialization:
>
> ```kotlin
> import android.app.Application
> import com.exponea.sdk.Exponea
> import com.google.firebase.installations.FirebaseMessaging
> 
> class ExponeaApp : Application() {
>     override fun onCreate() {
>        super.onCreate()
>        FirebaseMessaging.getInstance().token.addOnSuccessListener {
>            Exponea.handleNewToken(applicationContext, it)
>        }
>     }
> }
> ```

> ❗️
>
> The methods `Exponea.handleNewToken` and `Exponea.handleRemoteMessage` can be used before SDK initialization if a previous initialization was done. In such a case, each method will track events with the configuration of the last initialization. Consider initializing the SDK in `Application::onCreate` to make sure a fresh configuration is applied in case of an application update.

## Configure the Firebase Cloud Messaging integration in Engagement

Finally, you must configure the Firebase Cloud Messaging integration in Engagement so the platform can use it to send push notifications.

The setup requires you to use a private key from a Service Account that you create in Google Cloud and then copy-paste that key into the integration authentication in Bloomreach Engagement.

Follow the steps below:

1. **Create a service account.** To create a new service account in Google Cloud, navigate to `Service Accounts` and choose your project. On the Service Accounts page, select `Create Service Account`. It is possible to use roles to define more granular access.

2. **Generate a new private key**. Locate the FCM service account you created in the previous step, then select `Actions` > `Manage Keys`. Select `Add Key` > `Create new key`. Download the JSON key file.

3. **Add the Firebase Cloud Messaging integration** to your Engagement project. In Engagement, navigate to `Data & Assets` > `Integration`. Click on `Add new integration` and select `Firebase Cloud Messaging` for sending push notifications via the push notification node. Please note that if you’d like to send push notifications via webhooks, you must select `Firebase Service Account Authentication` instead.
![](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/firebase-1.png)

4. **Insert the key from step 2** into the Firebase Cloud Messaging integration settings page in the `Service Account JSON Credentials` field. Click on `Save integration`.
![](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/firebase-2.png)

5. **Select the Firebase Cloud Messaging integration** in `Project Settings` > `Channels` > `Push notifications` > `Firebase Cloud Messaging integration`. Click on `Save changes`.

The Engagement platform should now be able to send push notifications to Android devices via the push notification node.

#### Checklist

- [ ] If you run the app, the self-check should be able to send and receive a silent push notification. 
  ![](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/self-check.png)
- [ ] You should now be able to send push notifications using the Engagement web app and receive them in your app. Refer to [Mobile push notifications](https://documentation.bloomreach.com/engagement/docs/mobile-push-notifications#creating-a-new-notification) to learn how to create push notifications in the Engagement web app.
- [ ] Send a test push notification from Engagement to the device and tap on it. Your broadcast receiver should be called.

> 👍
>
> The Engagement service for sending push notifications and the Firebase connection may take a minute to wake up properly. If sending a push notification fails, try restarting the app. If the issue persists after 2-3 retries, review your setup.

You should now be able to use Engagement push notifications. You may disable the self-check or leave it on to check your push setup in every debug build run.
