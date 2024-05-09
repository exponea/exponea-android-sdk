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

## Events

### Track Event

Use the `trackEvent()` method to track any custom event type relevant to your business.

You can use any name for a custom event type. We recommended using a descriptive and human-readable name.

Refer to the [Custom Events](https://documentation.bloomreach.com/engagement/docs/custom-events) documentation for an overview of commonly used custom events.

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

The default session timeout is 20 seconds. Set `sessionTimeout` in the [SDK configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration#automaticsessiontracking) to specify a different timeout.

### Track Session Manually

To disable automatic session tracking, set `automaticSessionTracking` to `false` in the [SDK configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration#automaticsessiontracking).

Use the `trackSessionStart()` and `trackSessionEnd()` methods to track sessions manually.

#### Examples

```kotlin
Exponea.trackSessionStart()
```

```kotlin
Exponea.trackSessionEnd()
```

## Push Notifications

If developers [integrate push notification functionality](https://documentation.bloomreach.com/engagement/docs/android-sdk-push-notifications#integration) in their app, the SDK automatically tracks the push notification token by default.

### Track Token Manually

Use either the `trackPushToken()` (Firebase) or `trackHmsPushToken` (Huawei) method to manually track the token for receiving push notifications. The token is assigned to the currently logged-in customer (with the `identifyCustomer` method).

Invoking this method will track a push token immediately regardless of the value of 'tokenTrackFrequency' (refer to the [Configuration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) documentation for details).

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

## Payments

The SDK provides a convenience method `trackPaymentEvent` to help you track information about a payment for a product or service within the application.


### Track Payment Event

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

## Default Properties

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
