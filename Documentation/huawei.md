---
title: Huawei Mobile Services
excerpt: Integrate Huawei Mobile Services in your app to support Engagement push notifications on Huawei devices
slug: android-sdk-huawei
categorySlug: integrations
parentDocSlug: android-sdk-push-notifications
---

> 📘
>
> Newer phones manufactured by Huawei come with [Huawei Mobile Services (HMS)](https://developer.huawei.com/consumer/en/hms/) - a service that delivers push notifications _instead of_ Google's Firebase Cloud Messaging (FCM).

To be able to send [push notifications](https://documentation.bloomreach.com/engagement/docs/android-push-notifications) from the Engagement platform and receive them in your app on Huawei devices, you must set up Huawei Mobile Services (HMS), implement HMS in your app, and configure the Huawei Push Service integration in the Engagement web app.

> 👍
>
> The SDK provides a push setup self-check feature to help developers successfully set up push notifications. The self-check will try to track the push token, request the Engagement backend to send a silent push to the device, and check if the app is ready to open push notifications.
>
> To enable the setup check, set `Exponea.checkPushSetup = true` before [initializing the SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup#initialize-the-sdk).
>
> We suggest you turn the self-check feature on while implementing the push notifications for the first time or if you need to do some troubleshooting.

## Set up Huawei Mobile Services

First, you must set up Huawei Mobile Services:

1. Register and set up a [Huawei Developer account](https://developer.huawei.com/consumer/en/console).
2. Create a Project and App in AppGallery Connect.
3. Generate and configure a Signing Certificate.
4. Enable Push Kit in AppGallery Connect APIs.
5. Update Gradle scripts and add generated `agconnect-services.json` to your app.
6. Configure the Signing Information in your app.

> 📘
>
> For detailed instructions, please refer to [Preparations for Integrating HUAWEI HMS Core](https://developer.huawei.com/consumer/en/codelab/HMSPreparation/index.html#0) in the official HMS documentation.

## Implement HMS Message Service in your app

Next, you must create and register a service that extends `HmsMessageService`. The SDK's automatic tracking relies on your app providing this implementation.

> 👍
>
>  This implementation is not included in the SDK in order to keep it as small as possible and avoid including the libraries that are not essential for its functionality. You can copy the example code below and use it in your app.

1. Create the service:
    ``` kotlin
    import android.app.NotificationManager  
    import android.content.Context  
    import com.exponea.sdk.Exponea  
    import com.huawei.hms.push.HmsMessageService  
    import com.huawei.hms.push.RemoteMessage

    class MyHmsMessagingService: HmsMessageService() {

        private val notificationManager by lazy {
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }

        override fun onMessageReceived(message: RemoteMessage) {
            super.onMessageReceived(message)
            if (!Exponea.handleRemoteMessage(applicationContext, message.dataOfMap, notificationManager)) {
                // push notification is from another push provider
            }
        }

        override fun onNewToken(token: String) {
            super.onNewToken(token)
            Exponea.handleNewHmsToken(applicationContext, token)
        }
    }
    ```

2. Register the service in `AndroidManifest.xml`:
    ```xml
    <service android:name="MyHmsMessagingService" android:exported="false">  
        <intent-filter> 
            <action android:name="com.huawei.push.action.MESSAGING_EVENT"/>  
        </intent-filter>
    </service>  
    <meta-data  android:name="push_kit_auto_init_enabled" android:value="true"/>
    ```

The SDK will only handle push notification messages sent from the Engagement platform. A helper method `Exponea.isExponeaPushNotification()` is also provided.

After running your application, the SDK tracks the push token to the Engagement platform. If you enabled the self-check feature, it will confirm successful tracking.
You can also verify token tracking manually by locating the customer in the Bloomreach Engagement web application:

- **SDK versions below 4.6.0:** Check the customer property `huawei_push_notification_id`
- **SDK versions 4.6.0 and higher:** Check the `notification_state` event with property `push_notification_token`

> ❗️Important
>
> SDK versions 4.6.0 and higher use event-based token tracking to support multiple mobile applications per project. Learn more about [Token tracking via notification_state event](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#token-tracking-via-notification_state-event).

A push token is typically generated at the first application start, but it has its own lifecycle. Your `HmsMessageService` implementation is triggered only if a token is created or its value has changed. Please validate your expectations against the defined [token update triggers](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/android-client-dev-0000001050042041#section487774626)

> ❗️
>
> As of Android 13 (API level 33), a runtime notification permission must be registered in your `AndroidManifest.xml` and must also be granted by the user for your application to be able to show push notifications. The SDK takes care of registering the permission. However, your app must ask for notification permission from the user by invoking `Exponea.requestPushAuthorization(context)`. Refer to [Request notification permission](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#request-notification-permission) for details.
>
> If your marketing flow strictly requires normal push notifications usage, configure the SDK to track only authorized push tokens by setting [requirePushAuthorization](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) to `true`. Refer to [Require notification permission](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#require-notification-permission) for details.

> ❗️
>
> If you are integrating the SDK into an existing project, you may face an issue that your `HmsMessageService` is not called automatically.
>
> To retrieve a fresh push token, consider requesting a token manually as soon as possible after after application start:
>
> Refer to [Obtaining and Deleting a Push Token](https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides/android-client-dev-0000001050042041) in the HMS documentation for instructions on how to retrieve the current push token.

> ❗️
>
> The methods `Exponea.handleNewToken` and `Exponea.handleRemoteMessage` can be used before SDK initialization if a previous initialization was done. In such a case, each method will track events with the configuration of the last initialization. Consider initializing the SDK in `Application::onCreate` to make sure a fresh configuration is applied in case of an application update.

## Configure the Huawei Push Service integration in Engagement

1. In Huawei App Gallery Connect, navigate to `Project settings` > `App information` > `OAuth 2.0 client ID`. Locate the `Client ID` and `Client secret` copy their values. You will use these to configure the Huawei Push Service integration in Engagement.
   ![HMS - Client ID and Client secret](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/huawei1.png)

2. Open the Engagement web application and navigate to `Data & Assets` > `Integrations`. Click `+ Add new integration`.

3. Locate `Huawei Push Service` and click `+ Add integration`.  
   ![Engagement Integrations - Select Firebase Cloud Messaging integration](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/huawei2.png)

4. Enter the `Client ID` and `Client secret` values you copied in step 1. Click `Save integration` to finish.  
   ![Engagement Integrations - Configure Firebase Cloud Messaging integration](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/huawei3.png)

5. Navigate to `Settings` > `Project settings` > `Channels` > `Push notifications` > `Android Notifications` and set `Huawei integration` to `Huawei Push Service`.  
