---
title: Tracking
excerpt: Track customers and events using the Android SDK
slug: android-sdk-tracking
categorySlug: integrations
parentDocSlug: android-sdk
---

You can track events in Engagement to learn more about your app’s usage patterns and to segment your customers by their interactions.

By default, the SDK tracks certain events automatically, including:

* Installation (after app installation and after invoking [anonymize](#anonymize))
* User session start and end
* Banner event for showing an in-app message or content block

Additionally, you can track any custom event relevant to your business.

> 📘
>
> Also see [Mobile SDK tracking FAQ](https://support.bloomreach.com/hc/en-us/articles/18153058904733-Mobile-SDK-tracking-FAQ) at Bloomreach Support Help Center.

> ❗️ Protect the privacy of your customers
> 
> Make sure you have obtained and stored tracking consent from your customer before initializing Exponea Android SDK.
> 
> To ensure you're not tracking events without the customer's consent, you can use `Exponea.clearLocalCustomerData()` when a customer opts out from tracking (this applies to new users or returning customers who have previously opted out). This will bring the SDK to a state as if it was never initialized. This option also prevents reusing existing cookies for returning customers.
> 
> Refer to [Clear local customer data](#clear-local-customer-data) for details.
> 
> If customer denied tracking consent after Exponea Android SDK is initialized, you can use `Exponea.stopIntegration()` to stop SDK integration and remove all locally stored data.
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
> Optionally, you can provide a custom `timestamp` if the event happened at a different time. By default the current time will be used.

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

| Name                        | Type           | Description |
| --------------------------- | -------------- | ----------- |
| customerIds **(required)**  | CustomerIds    | Dictionary of customer unique identifiers. Only identifiers defined in the Engagement project are accepted. |
| properties                  | PropertiesList | Dictionary of customer properties. |
| timestamp                   | Double         | Unix timestamp (in seconds) specifying when the customer properties were updated. Specify `nil` value to use the current time. |

#### Examples

First, create a `CustomerIds` dictionary containing at least the customer's hard ID:

```kotlin
val customerIds = CustomerIds().withId("registered","jane.doe@example.com")
```

Optionally, create a dictionary with additional customer properties:

```kotlin
val properties = PropertiesList(
    hashMapOf(
        Pair("first_name", "Jane"),
        Pair("last_name", "Doe"),
        Pair("age", 32)
    )
)
```

Pass the `customerIds` and `properties` dictionaries to `identifyCustomer()`:

```kotlin
Exponea.identifyCustomer(
    customerIds = customerIds,
    properties = properties
)
```

If you only want to update the customer ID without any additional properties, you can pass a `PropertiesList` initialized with an empty `HashMap` into `properties`:

```swift
Exponea.identifyCustomer(
    customerIds = customerIds,
    properties = PropertiesList(hashMapOf())
)
```

> 👍
>
> Optionally, you can provide a custom `timestamp` if the identification happened at a different time. By default the current time will be used.

### Anonymize

Use the `anonymize()` method to delete all information stored locally and reset the current SDK state. A typical use case for this is when the user signs out of the app.

Invoking this method will cause the SDK to:

* Remove the push notification token for the current customer from local device storage and the customer profile in Engagement.
* Clear local repositories and caches, excluding tracked events.
* Track a new session start if `automaticSessionTracking` is enabled.
* Create a new customer record in Engagement (a new `cookie` soft ID is generated).
* Assign the previous push notification token to the new customer record.
* Preload in-app messages, in-app content blocks, and app inbox for the new customer.
* Track a new `installation` event for the new customer.

You can also use the `anonymize` method to switch to a different Engagement project. The SDK will then track events to a new customer record in the new project, similar to the first app session after installation on a new device.

#### Examples

```kotlin
Exponea.anonymize()
```

Switch to a different project:

```kotlin
Exponea.anonymize(
    exponeaProject = ExponeaProject(
        baseUrl= "https://api.exponea.com",
        projectToken= "YOUR PROJECT TOKEN",
        authorization= "Token YOUR API KEY"
    ),
    projectRouteMap = mapOf(
        EventType.TRACK_EVENT to listOf(
            ExponeaProject(
                baseUrl= "https://api.exponea.com",
                projectToken= "YOUR PROJECT TOKEN",
                authorization= "Token YOUR API KEY"
            )
        )
    ),
    advancedAuthToken = "YOUR JWT TOKEN"
)
```

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

In the [SDK configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration), you can disable automatic push notification tracking by setting the Boolean value of the `automaticPushNotification` property to `false`. It is then up to the developer to [manually track push notifications](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#manually-track-push-notifications).

> ❗️
>
> The behavior of push notification tracking may be affected by the tracking consent feature, which in enabled mode requires explicit consent for tracking. Refer to the [consent documentation](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking-consent) for details.

### Track token manually

Use either the `trackPushToken()` (Firebase) or `trackHmsPushToken` (Huawei) method to [manually track the token](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#track-push-token-fcm) for receiving push notifications. The token is assigned to the currently logged-in customer (with the `identifyCustomer` method).

Invoking this method will track a push token immediately regardless of the value of the `tokenTrackFrequency` [configuration parameter](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration).

Each time the app becomes active, the SDK calls `verifyPushStatusAndTrackPushToken` and tracks the token.

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
> Remember to invoke [anonymize](#anonymize) whenever the user signs out to ensure the push notification token is removed from the user's customer profile. Failing to do this may cause multiple customer profiles share the same token, resulting in duplicate push notifications.

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
| hasTrackingConsent      | Boolean                       | Indicates whether explicit [tracking consent](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking-consent) has been obtained. |
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

If the customer asks to delete personalized data, use the `clearLocalCustomerData()` method to delete all information stored locally before SDK is initialized.

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

Use the `stopIntegration()` method to delete all information stored locally and stop the SDK if it is already running.

Invoking this method will cause the SDK to:

* Remove the push notification token for the current customer from local device storage.
* Clear local repositories and caches, including all previously tracked events that were not flushed yet.
* Clear all session start and end information.
* Remove the customer record stored locally.
* Clear any In-app messages, In-app content blocks, and App inbox messages previously loaded.
* Clear the SDK configuration from the last invoked initialization.
* Stop handling of received push notifications.
* Stop tracking of Deep links and Universal links (your app's handling of them is not affected).

If the SDK is already running, invoking of this method also:

* Stops and disables session start and session end tracking even if your application tries later on.
* Stops and disables any tracking of events even if your application tries later on.
* Stops and disables any flushing of tracked events even if your application tries later on.
* Stops displaying of In-app messages, In-app content blocks, and App inbox messages.
  * Already displayed messages are dismissed.
  * Please validate dismiss behaviour if you [customized](https://documentation.bloomreach.com/engagement/docs/android-sdk-app-inbox#customize-app-inbox) the App Inbox UI layout. 

After invoking the `stopIntegration()` method, the SDK will drop any API method invocation until you [initialize the SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup#initialize_the_sdk) again. 

### Use cases

Correct usage of `stopIntegration()` method depends on the use case so please consider all scenarios.

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

The SDK caches data (such as sessions, events, and customer properties) in an internal local database and periodically sends them to Bloomreach Engagement. These data are kept locally if the device has no network, or if you configured SDK to upload them less frequently.

Invoking the `stopIntegration()` method will remove all these locally stored data that may not be uploaded yet. To avoid loss of these data please request to flush them before stopping the SDK:

```kotlin
// Flushing requires that SDK is initialized
Exponea.configure(...)
// Invoke flush force-fully
Exponea.flushMode = FlushMode.MANUAL
val flushIsDone = Semaphore(0, true)
Exponea.flushData {
    flushIsDone.release()
}
// Flushing process is asynchronous, we should wait until it is done
messageShownInvoked.tryAcquire(20, TimeUnit.SECONDS)
// All data are uploaded, we may stop SDK
Exponea.stopIntegration()
```

#### Stop the SDK and wipe all tracked data

The SDK caches data (such as sessions, events, and customer properties) in an internal local database and periodically sends them to the Bloomreach Engagement app. These data are kept locally if the device has no network, or if you configured SDK to upload them less frequently.

You may face the use case where the customer gets removed from the Bloomreach Enagagement platform and subsequently you want to remove them from local storage too.

Please do not initialize the SDK in this case as, depending on your configuration, the SDK may upload the stored tracked events. This may lead to customer's profile being recreated in Bloomreach Enagagement. This is because stored events may have been tracked for this customer and uploading them will result in the recreation of the customer profile based on the assigned customer IDs.

To prevent this from happening, invoke `stopIntegration()` immediately without initializing the SDK:

```kotlin
Exponea.stopIntegration()
```

This results in all previously stored data being removed from the device. The next SDK initialization will be considered a fresh new start.

#### Stop the already running SDK

The method `stopIntegration()` can be invoked anytime on a configured and running SDK.

This can be used in case the customer previously consented to tracking but revoked their consent later. You may freely invoke `stopIntegration()` with immediate effect.

```kotlin
// User gave you permission to track
Exponea.configure(...)

// Later, user decides to stop tracking
Exponea.stopIntegration()
```

This results in the SDK stopping all internal processes (such as session tracking and push notifications handling) and removing all locally stored data.

Please be aware that `stopIntegration()` stops any further tracking and flushing of data so if you require to upload tracked data to Bloomreach Engagement, then [flush them synchronously](#stop-the-sdk-but-upload-tracked-data) before stopping the SDK.

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

You can [configure](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) default properties to be tracked with every event. Note that the value of a default property will be overwritten if the tracking event has a property with the same key.

```kotlin
// Create a new ExponeaConfiguration instance
val configuration = ExponeaConfiguration()
configuration.defaultProperties["thisIsADefaultStringProperty"] = "This is a default string value"
configuration.defaultProperties["thisIsADefaultIntProperty"] = 1

// Start the SDK
Exponea.init(App.instance, configuration)
```

After initializing the SDK, you can change the default properties by setting `Exponea.defaultProperties`.
