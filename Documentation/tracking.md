---
title: Tracking for Android SDK
slug: android-sdk-tracking
category:
  uri: /branches/2/categories/guides/Developers
parent:
  uri: android-sdk
content:
  excerpt: Track customers and events using the Android SDK
---

You can track events in Engagement to learn more about your app’s usage patterns and to segment your customers by their interactions.

By default, the SDK tracks certain events automatically, including:

* Installation (after app installation and after invoking [anonymize](#anonymize))
* User session start and end
* Banner event for showing an in-app message or content block
* `notification_state` event for push notification token tracking (SDK versions 4.6.0 and higher). [Learn more](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#token-tracking-via-notification_state-event).

Additionally, you can track any custom event relevant to your business.

> 📘
>
> Also see [Mobile SDK tracking FAQ](https://support.bloomreach.com/hc/en-us/articles/18153058904733-Mobile-SDK-tracking-FAQ) at Bloomreach Support Help Center.

> ❗️ Protect the privacy of your customers
> 
> Make sure you have obtained and stored tracking consent from your customer before initializing Exponea Android SDK.
> 
> If a customer denies tracking consent and the SDK is **not currently initialized**, use `Exponea.clearLocalCustomerData()` to remove any locally stored data left from a previous SDK run. This brings the SDK to a state as if it was never initialized and prevents reusing existing cookies for returning customers. This method has no effect if there is no prior SDK configuration stored on the device (e.g. a completely fresh install).
> 
> Refer to [Clear local customer data](#clear-local-customer-data) for details.
> 
> If a customer revokes tracking consent after the SDK **is already initialized and running**, use `Exponea.stopIntegration()` to flush pending data, stop SDK integration, and remove all locally stored data.
>
> Refer to [Stop SDK integration](#stop-sdk-integration) for details.

## Events

### Track event

Use the `trackEvent()` method to track any custom event type relevant to your business.

You can use any name for a custom event type. We recommended using a descriptive and human-readable name.

Refer to the [Custom events](https://documentation.bloomreach.com/engagement/docs/custom-events) documentation for an overview of commonly used custom events.

#### Arguments

| Name                      | Type                                  | Description |
| ------------------------- | -------------- | ----------- |
| properties                | PropertiesList | Dictionary of event properties. |
| timestamp                 | Double         | Unix timestamp (in seconds) specifying when the event was tracked. Specify `nil` value to use the current time. |
| eventType **(required)**  | String         | Name of the event type, for example `screen_view`. |

#### Examples

Imagine you want to track which screens a customer views. You can create a custom event `screen_view` for this.

First, create a `PropertiesList` with properties you want to track with this event. In our example, you want to track the name of the screen, so you include a property `screen_name` along with any other relevant properties:

```kotlin
val properties = PropertiesList(
    hashMapOf(
        Pair("screen_name", "dashboard"),
        Pair("other_property", 123.45)
    )
)
```

Pass the properties list to `trackEvent()` along with the `eventType` (`screen_view`) as follows:

```kotlin
Exponea.trackEvent(
    properties = properties,
    timestamp = null,
    eventType =  "screen_view"
)
```

The second example below shows how you can use a nested structure for complex properties if needed:

```kotlin
val properties = PropertiesList(
    hashMapOf(
        Pair("purchase_status", "success"),
        Pair("product_list", arrayOf(
            hashMapOf(
                Pair("product_id", "abc123"),
                Pair("quantity", 2)
            ),
            hashMapOf(
                Pair("product_id", "abc456"),
                Pair("quantity", 1)
            )
        ))
    )
)
Exponea.trackEvent(
    properties = properties,
    timestamp = null,
    eventType =  "purchase"
)
```

> 👍
>
> Optionally, you can provide a custom `timestamp` if the event happened at a different time. By default, the current time will be used.

## Customers

[Identifying your customers](https://documentation.bloomreach.com/engagement/docs/customer-identification) allows you to track them across devices and platforms, improving the quality of your customer data.

Without identification, events are tracked for an anonymous customer, only identified by a cookie. Once the customer is identified by a hard ID, these events will be transferred to a newly identified customer.

> 👍
>
> Keep in mind that, while an app user and a customer record can be related by a soft or hard ID, they are separate entities, each with their own lifecycle. Take a moment to consider how their lifecycles relate and when to use [identify](#identify) and [anonymize](#anonymize).

### Identify

Use the `identifyCustomer()` method to identify a customer using their unique [hard ID](https://documentation.bloomreach.com/engagement/docs/customer-identification#hard-id).

The default hard ID is `registered` and its value is typically the customer's email address. However, your Engagement project may define a different hard ID.

Optionally, you can track additional customer properties such as first and last names, age, etc.

> ❗️
>
> Although it's possible to use `identifyCustomer` with a [soft ID](https://documentation.bloomreach.com/engagement/docs/customer-identification#section-soft-id), developers should use caution when doing this. In some cases (for example, after using `anonymize`) this can unintentionally associate the current user with an incorrect customer profile.

> ❗️
>
> The SDK stores data, including customer hard ID, in a local cache on the device. Removing the hard ID from the local cache requires calling [anonymize](#anonymize) in the app.
> If the customer profile is anonymized or deleted in the Bloomreach Engagement webapp, subsequent initialization of the SDK in the app can cause the customer profile to be reidentified or recreated from the locally cached data.

#### Arguments

| Name                                | Type              | Description                                                           |
|-------------------------------------|-------------------|-----------------------------------------------------------------------|
| customerIdentity **(required)**     | CustomerIdentity  | Customer unique identifiers together with an optional SDK auth token. |
| properties                          | Map<String, Any>? | Optional map of customer properties.                                  |

`CustomerIdentity` has the following fields:

| Name         | Type                  | Description                                                                                          |
|--------------|-----------------------|------------------------------------------------------------------------------------------------------|
| customerIds  | Map<String, String?>  | Map of customer unique identifiers. Only identifiers defined in the Engagement project are accepted. |
| sdkAuthToken | String?               | Optional SDK authentication token. If provided, it will be set as the current SDK auth token.        |

#### Examples

To identify a customer, create a `CustomerIdentity` object containing the customer's identifiers:

```kotlin
val customerIdentity = CustomerIdentity(
    customerIds = mapOf("registered" to "jane.doe@example.com")
)
```

Optionally, create a map with additional customer properties:

```kotlin
val properties = mapOf(
    "first_name" to "Jane",
    "last_name" to "Doe",
    "age" to 32
)
```

Pass the `customerIdentity` and `properties` to `identifyCustomer()`:

```kotlin
Exponea.identifyCustomer(
    customerIdentity = customerIdentity,
    properties = properties
)
```

If you only want to update the customer ID without any additional properties, you can omit the `properties` parameter:

```kotlin
Exponea.identifyCustomer(
    customerIdentity = customerIdentity
)
```

To identify a customer and set the SDK auth token at the same time, include the token in `CustomerIdentity`:

```kotlin
Exponea.identifyCustomer(
    customerIdentity = CustomerIdentity(
        customerIds = mapOf("registered" to "jane.doe@example.com"),
        sdkAuthToken = "your-auth-token"
    )
)
```

### Anonymize

Use the `anonymize()` method to delete all information stored locally and reset the current SDK state. A typical use case for this is when the user signs out of the app.

Invoking this method will cause the SDK to:

* Remove the push notification token for the current customer from local device storage and the customer profile in Engagement.
* Clear local repositories and caches, excluding tracked events.
* Track a new session start if `automaticSessionTracking` is enabled.
* Create a new customer record in Engagement (a new `cookie` soft ID is generated).
* Regenerate `device_id` for the new customer if `regenerateDeviceIdOnAnonymize` is set to `true`. [See Configuration for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration)
* Assign the previous push notification token to the new customer record.
* Preload in-app messages, in-app content blocks, and app inbox for the new customer.
* Track a new `installation` event for the new customer.

> 📘 Note
>
> When the SDK is configured with `StreamConfig` and an SDK auth token is set, the SDK attempts a best-effort flush of pending events before clearing local data. If you need to be notified when the anonymization process finishes, use the callback overload described [below](#anonymize-with-completion-callback).

#### How tokens are removed during anonymization

The SDK removes push notification tokens differently depending on the version:

**SDK versions below 4.6.0:**

- Assigns an empty string to the `google_push_notification_id` or `huawei_push_notification_id` customer property

**SDK versions 4.6.0 and higher:**

- Tracks a `notification_state` event with `valid = false` and `description = Invalidated`

> 📘 Note
>
> Learn more about [Token tracking via notification_state event](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#token-tracking-via-notification_state-event).

You can also use the `anonymize` method to switch to a different integration configuration. The SDK will then track events to a new customer record in the new project or stream, similar to the first app session after installation on a new device.

> ❗️ Avoid anonymous events on logout
>
> `anonymize()` creates a new anonymous customer profile and tracks events against it. If your integration should not generate events against a customer profile that is not identified, use [`stopIntegration()`](#stop-sdk-integration) on logout instead. Unlike `anonymize()`, `stopIntegration()` does not create or track events on an anonymous profile after logout.
>
> The typical example where you do not want anonymous traffic is a `StreamConfig` integration with an [SDK auth token (JWT)](https://documentation.bloomreach.com/engagement/docs/android-sdk-authorization#sdk-auth-token-authorization) on a Data hub event stream configured with [signed-only permissions](https://documentation.bloomreach.com/data-hub/docs/set-up-event-stream-security-and-permissions#configure-permissions).

#### Examples

```kotlin
Exponea.anonymize()
```

Switch to a different project integration:

```kotlin
Exponea.anonymize(
    integrationConfig = ProjectConfig(
        projectToken = "YOUR PROJECT TOKEN",
        baseUrl = "https://api.exponea.com",
        authorization = "Token YOUR API KEY"
    ),
    exponeaConfigurationOverrides = ExponeaConfigurationOverrides(
        integrationRouteMap = mapOf(
            EventType.TRACK_EVENT to listOf(
                ProjectConfig(
                    projectToken = "YOUR PROJECT TOKEN",
                    baseUrl = "https://api.exponea.com",
                    authorization = "Token YOUR API KEY"
                )
            )
        )
    )
)
```

Switch to a different stream integration:

```kotlin
Exponea.anonymize(
    integrationConfig = StreamConfig(
        baseUrl = "https://api.exponea.com",
        streamId = "YOUR_NEW_STREAM_ID"
    )
)
```

#### Anonymize with completion callback

If you need to perform an action after the anonymization process finishes, use the `onAnonymized` callback overload:

```kotlin
Exponea.anonymize {
    // Anonymization is complete
}
```

With a new integration configuration:

```kotlin
Exponea.anonymize(
    integrationConfig = StreamConfig(
        baseUrl = "https://api.exponea.com",
        streamId = "YOUR_NEW_STREAM_ID"
    )
) {
    // Anonymization is complete, now using the new stream
}
```

> 📘 Note
>
> When using `StreamConfig` with JWT, the callback is invoked after the flush attempt finishes. When no flush is needed (e.g. `ProjectConfig` or no JWT set), the callback is invoked immediately.

## Sessions

The SDK tracks sessions automatically by default, producing two events: `session_start` and `session_end`.

The session represents the actual time spent in the app. It starts when the application is launched and ends when it goes into the background. If the user returns to the app before the session times out, the application will continue the current session.

The default session timeout is 60 seconds. Set `sessionTimeout` in the [SDK configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration#automaticsessiontracking) to specify a different timeout.

### Track session manually

To disable automatic session tracking, set `automaticSessionTracking` to `false` in the [SDK configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration#automaticsessiontracking).

Use the `trackSessionStart()` and `trackSessionEnd()` methods to track sessions manually.

#### Examples

```kotlin
Exponea.trackSessionStart()
```

```kotlin
Exponea.trackSessionEnd()
```

> 👍
>
> The default behavior for manually calling `Exponea.trackSessionStart()` multiple times can be controlled by the [manualSessionAutoClose](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) flag, which is set to `true` by default. If a previous session is still open (i.e. it has not been manually closed with `Exponea.trackSessionEnd()`) and `Exponea.trackSessionStart()` is called again, the SDK will automatically track a `session_end` for the previous session and then tracks a new `session_start` event. To prevent this behavior, set the [manualSessionAutoClose](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) flag to `false`.

## Push notifications

If developers [integrate push notification functionality](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#integration) in their app, the SDK automatically tracks push notifications by default.

In the [Configuration for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration), you can disable automatic push notification tracking by setting the Boolean value of the `automaticPushNotification` property to `false`. It is then up to the developer to [manually track push notifications](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#manually-track-push-notifications).

> ❗️
>
> The behavior of push notification tracking may be affected by the tracking consent feature, which in enabled mode requires explicit consent for tracking. Refer to the [Tracking consent for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking-consent) for details.

### Track token manually

Use either the `trackPushToken()` (Firebase) or `trackHmsPushToken` (Huawei) method to [manually track the token](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#track-push-token-fcm) for receiving push notifications. The token is assigned to the currently logged-in customer (with the `identifyCustomer` method).

Invoking this method will track a push token immediately regardless of the value of the `tokenTrackFrequency` [Configuration for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) parameter.

Each time the app becomes active, the SDK calls `verifyPushStatusAndTrackPushToken` and tracks the token.

> 📘 Note
>
> SDK versions 4.6.0 and higher use event-based token tracking. Learn more about [Token tracking via notification_state event](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#token-tracking-via-notification_state-event).

#### Arguments

| Name                 | Type    | Description |
| ---------------------| ------- | ----------- |
| token **(required)** | String  | String containing the push notification token. |

#### Example 

Firebase:

```kotlin
Exponea.trackPushToken("value-of-push-token")
```

Huawei:

```kotlin
Exponea.trackHmsPushToken("value-of-push-token")
```

> ❗️
>
> Remember to detach the push notification token from the signed-out customer profile on logout. Failing to do so may cause multiple customer profiles to share the same token, resulting in duplicate push notifications.
>
> Call [`anonymize()`](#anonymize) or [`stopIntegration()`](#stop-sdk-integration) on logout, depending on whether a new anonymous profile should be created or not. When using `stopIntegration()`, you must re-track the push token after each re-initialization — see [Re-tracking the push token after stopIntegration()](#re-tracking-the-push-token-after-stopintegration).

#### Re-tracking the push token after stopIntegration()

On Android, `stopIntegration()` clears the locally stored push notification token. A subsequent `init()` calls without a stored push token, and the SDK cannot recover the token — even though the host app may still hold a valid token it received earlier from the push service.

If your app re-initializes the SDK by calling `init()` after a previous `stopIntegration()`, the host app must call `trackPushToken()` (FCM) or `trackHmsPushToken()` (HMS) again after each re-initialization, passing the token it already holds:

```kotlin
Exponea.stopIntegration()

// ... later, whenever the SDK is re-initialized ...
Exponea.init(context, configuration)
// Re-track the push token the host app already received from FCM/HMS:
Exponea.trackPushToken("value-of-push-token")
// or, for Huawei:
Exponea.trackHmsPushToken("value-of-push-token")
```

> 📘
>
> Refer to [Firebase Cloud Messaging for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-firebase) and [Huawei Mobile Services](https://documentation.bloomreach.com/engagement/docs/android-sdk-huawei) for information on how to retrieve a valid push notification token.

### Track push notification delivery manually

Use the `trackDeliveredPush()` method to [manually track push notification delivery](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#track-delivered-push-notification).

#### Arguments

| Name      | Type                                   | Description |
| ----------| -------------------------------------- | ----------- |
| data      | [NotificationData](#notificationdata)? | Notification data. |
| timestamp | Double                                 | Unix timestamp (in seconds) specifying when the event was tracked. Specify nil value to use the current time. |

#### NotificationData

| Name                    | Type                          | Description |
| ------------------------| ----------------------------- | ----------- |
| attributes              | HashMap<String, Any>          | Map of data attributes. |
| campaignData            | [CampaignData](#campaigndata) | Campaign data. |
| consentCategoryTracking | String?                       | Consent category. |
| hasTrackingConsent      | Boolean                       | Indicates whether explicit [Tracking consent for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking-consent) has been obtained. |
| hasCustomEventType      | Boolean                       | Indicates whether the notification has a custom event type. |
| eventType               | String?                       | Event type for the notification (default: campaign). |
| sentTimestamp           | Double?                       | Unix timestamp (in seconds). Specify nil value to use the current time. |

#### CampaignData

| Name        | Type    | Description |
| ------------| ------- | ----------- |
| source      | String? | UTM source code. |
| campaign    | String? | UTM campaign code. |
| content     | String? | UTM content code. |
| medium      | String? | UTM method code.|
| term        | String? | UTM term code. |
| payload     | String? | Notification payload in JSON format. |
| createdAt   | Double  | Unix timestamp (in seconds). Specify nil value to use the current time. |
| completeUrl | String? | Campaign URL, defaults to null for push notifications.| 

> 📘
>
> Refer to [UTM parameters](https://documentation.bloomreach.com/engagement/docs/utm-parameters) in the campaigns documentation for details. 

#### Example

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

### Track push notification click manually

Use the `trackClickedPush()` method to [manually track push notification clicks](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#track-clicked-push-notification).

#### Arguments

| Name       | Type                                   | Description |
| -----------| -------------------------------------- | ----------- |
| data       | [NotificationData](#notificationdata)? | Notification data. |
| actionData | [NotificationData](#notificationdata)? | Action data.|
| timestamp  | Double?                                | Unix timestamp (in seconds) specifying when the event was tracked. Specify nil value to use the current time. |

#### Example

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
Exponea.trackClickedPush(
        data = notificationData
        timestamp = currentTimeSeconds()
)
```

## Clear local customer data

Your application should always ask customers for consent to track their app usage. If the customer consents to tracking events at the application level but not at the personal data level, using the `anonymize()` method is usually sufficient.

If the customer doesn't consent to any tracking, it's recommended not to initialize the SDK at all.

If the customer asks to delete personalized data and the SDK is **not currently initialized**, use the `clearLocalCustomerData()` method to remove all locally stored data from a previous SDK run.

> ❗️
>
> `clearLocalCustomerData()` can only be called when the SDK is **not currently initialized**. If the SDK is running, the call is ignored and an error is logged. Use [stopIntegration](#stop-sdk-integration) instead.
>
> `clearLocalCustomerData()` requires that the SDK was previously initialized at some point (a prior SDK configuration must exist on the device). If there is no prior configuration, the call has no effect.

The customer may also revoke all tracking consent after the SDK is fully initialized and tracking is enabled. In this case, you can stop SDK integration and remove all locally stored data using the [stopIntegration](#stop-sdk-integration) method.

Invoking this method will cause the SDK to:

* Remove the push notification token for the current customer from local device storage.
* Clear local repositories and caches, including all previously tracked events that haven't been flushed yet.
* Clear all session start and end information.
* Remove the customer record stored locally.
* Clear any previously loaded in-app messages, in-app content blocks, and app inbox messages.
* Clear the SDK configuration from the last invoked initialization.
* Stop handling of received push notifications.
* Stop tracking of deep links and universal links (your app's handling of them isn't affected).

## Stop SDK integration

Your application should always ask the customer for consent to track their app usage. If the customer consents to tracking of events at the application level but not at the personal data level, using the `anonymize()` method is normally sufficient.

If the customer doesn't consent to any tracking before the SDK is initialized, it's recommended that the SDK isn't initialized at all. For the case of deleting personalized data before SDK initialization, see more info in the usage of the [clearLocalCustomerData](#clear-local-customer-data) method.

The customer may also revoke all tracking consent later, after the SDK is fully initialized and tracking is enabled. In this case, you can stop SDK integration and remove all locally stored data by using the `Exponea.stopIntegration()` method.

> 👍 Preferred when avoiding anonymous events
>
> Use `stopIntegration()` instead of [`anonymize()`](#anonymize) on logout whenever your integration should not generate events against an anonymous customer profile. Unlike `anonymize()`, `stopIntegration()` does not create a new anonymous customer profile. This applies to any SDK configuration; the typical example is a `StreamConfig` integration with an [SDK auth token (JWT)](https://documentation.bloomreach.com/engagement/docs/android-sdk-authorization#sdk-auth-token-authorization) on a Data hub event stream configured with [signed-only permissions](https://documentation.bloomreach.com/data-hub/docs/set-up-event-stream-security-and-permissions#configure-permissions).

> ❗️
>
> `stopIntegration()` can only be called when the SDK **is initialized and running**. If the SDK is not initialized, the call is ignored and an error is logged. To clear locally stored data when the SDK is not running, use [clearLocalCustomerData](#clear-local-customer-data) instead.

Use the `stopIntegration()` method to stop the SDK if it is already running. The SDK will flush all pending data to the server and then delete all information stored locally.

Invoking this method will cause the SDK to:

* Track a `session_end` event (if automatic session tracking is enabled).
* Send a push notification token invalidation event to the server.
* Flush all pending events to the server.
* Remove the push notification token for the current customer from local device storage. If you re-initialize the SDK later (for example for a new logged-in user), you must call `trackPushToken()` or `trackHmsPushToken()` again — see [Re-tracking the push token after stopIntegration()](#re-tracking-the-push-token-after-stopintegration).
* Clear local repositories and caches, including any tracked events that failed to upload.
* Clear all session start and end information.
* Remove the customer record stored locally.
* Clear any In-app messages, In-app content blocks, and App inbox messages previously loaded.
* Clear the SDK configuration from the last invoked initialization.
* Stop handling of received push notifications.
* Stop tracking of Deep links and Universal links (your app's handling of them is not affected).
* Stop and disable any further tracking — subsequent SDK method calls will be dropped until the SDK is re-initialized.
* Stop displaying In-app messages, In-app content blocks, and App inbox messages. Already displayed messages are dismissed.
  * Please validate dismiss behaviour if you [customized](https://documentation.bloomreach.com/engagement/docs/android-sdk-app-inbox#customize-app-inbox) the App Inbox UI layout.

After invoking the `stopIntegration()` method, the SDK will drop any API method invocation until you [initialize the SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup#initialize_the_sdk) again.

You can optionally pass an `onStopped` callback to be notified when the stop process (including flush) is complete:

```kotlin
Exponea.stopIntegration {
    // SDK is now fully stopped and all data cleared
}
```

### Use cases

Correct usage depends on whether the SDK is currently initialized. Consider all scenarios below.

#### Ask the customer for consent

Developers should always respect user privacy, not just to comply with GDPR, but to build trust and create better, more ethical digital experiences.

Permission requests in mobile apps should be clear, transparent, and contextually relevant. Explain why the permission is needed and request it only when necessary, ensuring users can make an informed choice.

You may use system dialog or In-app messages for that purpose.

![](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/gdpr-dialog-example.png)

In the case of the in-app message dialog, you can customize [In-app message action callback](https://documentation.bloomreach.com/engagement/docs/android-sdk-in-app-messages#customize-in-app-message-actions) to handle the user's decision about allowing or denying tracking permission.

```kotlin
Exponea.inAppMessageActionCallback = object : InAppMessageCallback {  
    // set overrideDefaultBehavior to true to handle URL opening manually
    override var overrideDefaultBehavior = true
    // set trackActions to true to keep tracking of click and close actions
    override var trackActions = true
  
    override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context) {
        if (messageIsForGdpr(message)) {
            handleGdprUserResponse(button)
        } else if (button.url != null) {
            openUrl(button)
        }
    }

    override fun inAppMessageCloseAction(
        message: InAppMessage,
        button: InAppMessageButton?,
        interaction: Boolean,
        context: Context
    ) {
        if (messageIsForGdpr(message) && interaction) {
            // regardless from `button` nullability, parameter `interaction` with true tells that user closed message
            Logger.i(this, "Stopping SDK")
            Exponea.stopIntegration()
        }
    }

    override fun inAppMessageShown(message: InAppMessage, context: Context) {
        // Here goes your code
    }

    override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {
        // Here goes your code
    }

    private fun messageIsForGdpr(message: InAppMessage): Boolean {
        // apply your detection for GDPR related In-app
        // our example app is triggering GDPR In-app by custom event tracking so we used it for detection
        // you may implement detection against message title, ID, payload, etc.
        return message.applyEventFilter("event_name", mapOf("property" to "gdpr"), null)
    }

    private fun openUrl(button: InAppMessageButton) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = Uri.parse(button.url)
                }
            )
        } catch (e: ActivityNotFoundException) {
            Logger.e(this, "Unable to open URL", e)
        }
    }
}
```

#### Stop the SDK but upload tracked data

The `stopIntegration()` method automatically flushes all cached data (such as sessions, events, and customer properties) to the server before clearing local storage. No manual flush is needed.

If you need to wait for the flush and teardown to complete before proceeding, use the `onStopped` callback:

```kotlin
Exponea.stopIntegration {
    // SDK is now fully stopped and all data cleared
}
```

#### Wipe all locally stored data without uploading

The SDK caches data (such as sessions, events, and customer properties) in an internal local database and periodically sends them to the Bloomreach Engagement app. These data are kept locally if the device has no network, or if you configured SDK to upload them less frequently.

You may face a use case where the customer gets removed from the Bloomreach Engagement platform, and you want to remove them from local storage too.

Do not initialize the SDK in this case. Depending on your configuration, the SDK may upload the stored tracked events during initialization. This may lead to the customer's profile being recreated in Bloomreach Engagement, because stored events were tracked for that customer and uploading them will result in the recreation of the customer profile based on the assigned customer IDs.

To prevent this from happening, call `clearLocalCustomerData()` without initializing the SDK:

```kotlin
Exponea.clearLocalCustomerData()
```

This removes all previously stored data from the device without uploading it. The next SDK initialization will be considered a fresh new start.

> 📘
>
> `clearLocalCustomerData()` requires that the SDK was previously initialized at some point (a prior SDK configuration must be stored on the device). If there is no prior configuration (e.g. a completely fresh installation with no prior SDK run), this call has no effect — but there is also nothing to remove in that case.

#### Stop the already running SDK

The method `stopIntegration()` can be invoked anytime on a configured and running SDK.

This can be used in case the customer previously consented to tracking but revoked their consent later. You may freely invoke `stopIntegration()` with immediate effect.

```kotlin
// User gave you permission to track
Exponea.configure(...)

// Later, user decides to stop tracking
Exponea.stopIntegration()
```

This results in the SDK flushing all pending events to the server, stopping all internal processes (such as session tracking and push notifications handling), and removing all locally stored data. You can use the `onStopped` callback to confirm the process is complete.

#### Customer denies tracking consent

It is recommended to ask the customer for tracking consent as soon as possible in your application. If the customer denies consent, please do not initialize the SDK at all.

## Payments

The SDK provides a convenience method `trackPaymentEvent` to help you track information about a payment for a product or service within the application.

### Track payment event

Use the `trackPaymentEvent()` method to track payments.

#### Arguments

| Name          | Type          | Description |
| --------------| ------------- | ----------- |
| purchasedItem | PurchasedItem | Dictionary of payment properties. |

#### Examples

First, create a `PurchasedItem` containing the basic information about the purchase:

```kotlin
val item = PurchasedItem(
        value = 12.34,
        currency = "EUR",
        paymentSystem = "Virtual",
        productId = "handbag",
        productTitle = "Awesome leather handbag"
)
```

Pass the `PurchasedItem` to `trackPaymentEvent` as follows:

```kotlin
Exponea.trackPaymentEvent(purchasedItem = item)
```

If your app uses in-app purchases (for example, purchases with in-game gold, coins, etc.), you can track them with `trackPaymentEvent` using `Purchase` and `SkuDetails` objects used in [Google Play Billing Library](https://developer.android.com/google/play/billing/integrate):

```kotlin
val purchase: com.android.billingclient.api.Purchase = ...
val skuDetails: com.android.billingclient.api.SkuDetails = ...
val item = PurchasedItem(
        value = sku.priceAmountMicros / 1000000.0,
        currency = sku.priceCurrencyCode,
        paymentSystem = "Google Play",
        productId = sku.sku,
        productTitle = sku.title,
        receipt = purchase.signature
)

Exponea.trackPaymentEvent(purchasedItem = item)
```

## Default properties

You can [Configuration for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) default properties to be tracked with every event. Note that the value of a default property will be overwritten if the tracking event has a property with the same key.

```kotlin
// Create a new ExponeaConfiguration instance
val configuration = ExponeaConfiguration()
configuration.defaultProperties["thisIsADefaultStringProperty"] = "This is a default string value"
configuration.defaultProperties["thisIsADefaultIntProperty"] = 1

// Start the SDK
Exponea.init(App.instance, configuration)
```

After initializing the SDK, you can change the default properties by setting `Exponea.defaultProperties`.
