---
title: Configuration
excerpt: Full configuration reference for the Android SDK
slug: android-sdk-configuration
categorySlug: integrations
parentDocSlug: android-sdk-setup
---

This page provides an overview of all configuration parameters for the SDK. You can either configure the SDK in code using an `ExponeaConfiguration` object or in a file called `exponea_configuration.json` inside the `assets` folder of your application. 

> 📘
>
> Refer to [Initialize the SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup#initialize-the-sdk) for instructions.

## Configuration parameters

* `projectToken` **(required)**
   * Your project token. You can find this in the Engagement web app under `Project settings` > `Access management` > `API`.

* `authorization` **(required)**
   * Format `"Token <token>"` where `<token>` is an Engagement API key.
   * The token must be an Engagement **public** key. See [Mobile SDKs API Access Management](mobile-sdks-api-access-management) for details.
   * For more information, refer to [Exponea API documentation](https://docs.exponea.com/reference#access-keys).

* `baseURL`
  * Your API base URL which can be found in the Engagement web app under `Project settings` > `Access management` > `API`.
  * Default value `https://api.exponea.com`.
  * If you have custom base URL, you must set this property.

* `projectRouteMap`
  * If you need to track events into more than one project, you can define project information for "event types" which should be tracked multiple times.
    Example:
    ```kotlin
    var projectRouteMap = mapOf<EventType, List<ExponeaProject>> (
        EventType.TRACK_CUSTOMER to listOf(
            ExponeaProject(
                "https://api.exponea.com",
                "YOUR_PROJECT_TOKEN",
                "Token YOUR_API_KEY"
            )
        )
    )
    ```
  
* `defaultProperties`
  * A list of properties to be added to all tracking events.
  * Default value: `nil`

* `allowDefaultCustomerProperties`
  * Flag to apply `defaultProperties` list to `identifyCustomer` tracking event
  * Default value: `true`

* `automaticSessionTracking`
  * Flag to control the automatic tracking of `session_start` and `session_end` events.
  * Default value: `true`

* `sessionTimeout`
  * The session is the actual time spent in the app. It starts when the app is launched and ends when the app goes into the background.
  * This value is used to calculate the session timing.
  * Default value: `60` seconds.
  * Read more about [Tracking Sessions](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking#session)

* `automaticPushNotification`
  * Controls if the SDK will handle push notifications automatically.
  * Default value: `true`

* `pushIcon`
  * Icon to be displayed in a push notification.
  * Refer to https://developer.android.com/design/ui/mobile/guides/home-screen/notifications#notification-header for details.

* `pushAccentColor`
  * Accent color of push notification. Changes the color of the small icon and the notification buttons. For example: `Color.GREEN`.
  * This is a color ID, not a resource ID. When using colors from resources you must specify the resource, for example: `context.resources.getColor(R.color.something)`.
  * Refer to https://developer.android.com/design/ui/mobile/guides/home-screen/notifications#notification-header for details.

* `pushChannelName`
  * Name of the channel to be created for the push notifications.
  * Only available for API level 26+. Refer to https://developer.android.com/training/notify-user/channels for details.

* `pushChannelDescription`
  * Description of the channel to be created for the push notifications.
  * Only available for API level 26+. Refer to https://developer.android.com/training/notify-user/channels for details.

* `pushChannelId`
  * Channel ID for push notifications.
  * Only available for API level 26+. Refer to https://developer.android.com/training/notify-user/channels for details.

* `pushNotificationImportance`
  * Notification importance for the notification channel.
  * Only available for API level 26+. Refer to https://developer.android.com/training/notify-user/channels for details.

* `tokenTrackFrequency`
  * Indicates the frequency with which the SDK should track the push notification token to Engagement.
  * Default value: `ON_TOKEN_CHANGE`
  * Possible values:
    * `ON_TOKEN_CHANGE` - tracks push token if it differs from a previously tracked one
    * `EVERY_LAUNCH` - always tracks push token
    * `DAILY` - tracks push token once per day

* `requirePushAuthorization`
  * Flag indicating whether the SDK should check [push notification permission status](https://developer.android.com/develop/ui/views/notifications/notification-permission) and only track the push token if the user granted permission to receive push notifications.
  * Possible values:
    * `true` - tracks the push token only if the user granted permission to receive push notifications. An empty token value is tracked if the user denied permission. This is useful to send normal push notifications to a target audience that allows receiving notifications.
    * `false` - tracks the push token regardless of notification permission status. This is useful to send silent push notifications that do not require permission from the user.
  * Default value: `false`

* `maxTries`
  * Controls how many times the SDK should attempt to flush an event before aborting. Useful for example in case the API is down or some other temporary error happens.
  * The SDK will consider the data to be flushed if this number is exceeded and delete the data from the queue.
  * Default value: `10`

* `advancedAuthEnabled`
  * If set, advanced authorization is used for communication with the Engagement APIs listed in [Customer Token Authorization](https://documentation.bloomreach.com/engagement/docs/android-sdk-authorization#customer-token-authorization).
  * Refer to the [authorization documentation](https://documentation.bloomreach.com/engagement/docs/android-sdk-authorization) for details.

* `inAppContentBlocksPlaceholders`
  * If set, all [In-app content blocks](https://documentation.bloomreach.com/engagement/docs/android-sdk-in-app-content-blocks) will be prefetched right after the SDK is initialized.

* `allowWebViewCookies`
  * Flag to enable or disable cookies in WebViews.
  * Default value: `false`
  * > ❗️
    >
    > **Disclaimer**:
    > * For security purposes, cookies are by default disabled in WebViews.
    > * This setting has effect on all WebViews in the application, NOT ONLY the ones used by the SDK.
    > * DO NOT CHANGE THIS SETTING unless you know the risks associated with enabling and storing cookies.
    > * By changing this setting and enabling cookies in WebViews you take full responsibility for any security vulnerabilities or incidents caused by them.

* `manualSessionAutoClose`
    * Determines whether the SDK automatically tracks `session_end` for sessions that remain open when `Exponea.trackSessionStart()` is called multiple times in manual session tracking mode.
    * Default value: `true`