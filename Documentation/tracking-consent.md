---
title: Tracking Consent
excerpt: Manage tracking consent using the Android SDK.
slug: android-sdk-tracking-consent
categorySlug: integrations
parentDocSlug: android-sdk-tracking
---

Depending on local data access regulations, access to data on a user's device may require explicit consent. To follow such requirements, Engagement allows you to enable the standalone "tracking consent" feature. This feature activates the tracking consent option for in-app messages, in-app content blocks, and mobile push notifications.

> 📘
>
> Refer to [Configuration of the tracking consent categories](https://documentation.bloomreach.com/engagement/docs/configuration-of-tracking-consent) in the Engagement Consent Management documentation for more information about the tracking consent feature.

## How the SDK Manages Tracking Consent

If the tracking consent feature is enabled, the Engagement platform sents a `has_tracking_consent` attribute along with push notifications, in-app messages, and in-app content blocks data. The SDK tracks events according to the boolean value of this attribute.

If the tracking consent feature is disabled, the `has_tracking_consent` attribute is not included in push notifications, in-app messages, and in-app content blocks data. In this case, the SDK considers `has_tracking_consent` to be `true` and tracks event accordingly.

In case of clicked events, it is possible to override the value of `has_tracking_consent` and force tracking by including the query parameter `xnpe_force_track` with the value `true` in the action URL.

## How the SDK Tracks Events Depending on Tracking Consent

### Push Notification Delivered

The SDK tracks push notification delivery by calling `Exponea.trackDeliveredPush` or `Exponea.handleRemoteMessage`. These methods track a delivered event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.

If you are invoking the `Exponea.trackDeliveredPush` method manually and want to ignore tracking consent, you may use `Exponea.trackDeliveredPushWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.

### Push Notification Clicked

The SDK tracks push notification clicks by calling `Exponea.trackClickedPush`. This method tracks a clicked event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.
- The action URL contains the query parameter `xnpe_force_track` with the value `true` (overriding `has_tracking_consent`).

> 👍
>
> An event that is tracked because `xnpe_force_track` (forced tracking) is enabled will contain an additional property `tracking_forced` with value `true`.

If you are invoking the `Exponea.trackClickedPush` method manually and you want to ignore tracking consent, you may use `Exponea.trackPushOpenedWithoutTrackingConsent` instead.

These methods will track the event regardless of tracking consent.

### In-App Message Clicked

The SDK tracks in-app message clicks by calling `Exponea.trackInAppMessageClick`. This method tracks a clicked event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.
- The action URL contains the query parameter `xnpe_force_track` with the value `true` (overriding `has_tracking_consent`).

> 👍
>
> An event that is tracked because `xnpe_force_track` (forced tracking) is enabled will contain an additional property `tracking_forced` with value `true`.

If you are invoking the `Exponea.trackInAppMessageClick` method manually and want to ignore tracking consent, you may use `Exponea.trackInAppMessageClickWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.

### In-App Message Closed

The SDK tracks in-app message closed events by calling `Exponea.trackInAppMessageClose`. This method tracks a closed event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.

If you are invoking the `Exponea.trackInAppMessageClose` method manually and you want to ignore tracking consent, you may use `Exponea.trackInAppMessageCloseWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.

### App Inbox Message Opened

The SDK tracks app inbox message opening by calling `Exponea.trackAppInboxOpened`. This method tracks an app inbox message open event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.

If you are invoking the `Exponea.trackAppInboxOpened` method manually and you want to ignore tracking consent, you may use `Exponea.trackAppInboxOpenedWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.

### App Inbox Action Clicked

The SDK tracks app inbox action clicks by calling `Exponea.trackAppInboxClick`. This method tracks a clicked event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.
- The action URL contains the query parameter `xnpe_force_track` with the value `true` (overriding `has_tracking_consent`).

> 👍
>
> An event that is tracked because `xnpe_force_track` (forced tracking) is enabled will contain an additional property `tracking_forced` with value `true`.

If you are invoking the `Exponea.trackAppInboxClick` method manually and you want to ignore tracking consent, you may use `Exponea.trackAppInboxClickWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.

### In-App Content Block Displayed

The SDK tracks in-app content block display by calling `Exponea.trackInAppContentBlockShown`. This method tracks a displayed event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.

If you are invoking the `Exponea.trackInAppContentBlockShown` method manually and you want to ignore tracking consent, you may use `Exponea.trackInAppContentBlockShownWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.

### In-App Content Block Clicked

The SDK tracks in-app content block clicks by calling `Exponea.trackInAppContentBlockClick`. This method tracks a clicked event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.
- The action URL contains the query parameter `xnpe_force_track` with the value `true` (overriding `has_tracking_consent`).

> 👍
>
> An event that is tracked because `xnpe_force_track` (forced tracking) is enabled will contain an additional property `tracking_forced` with value `true`.

If you are invoking the `Exponea.trackInAppContentBlockClick` method manually and you want to ignore tracking consent, you may use `Exponea.trackInAppContentBlockClickWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.

### In-App Content Block Closed

The SDK tracks in-app content block closed events by calling `Exponea.trackInAppContentBlockClose`. This method tracks a closed event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.

If you are invoking the `Exponea.trackInAppContentBlockClose` method manually and you want to ignore tracking consent, you may use `Exponea.trackInAppContentBlockCloseWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.

### In-App Content Block Error

The SDK tracks in-app content blocks errors by calling `Exponea.trackInAppContentBlockError` with a meaningful `errorMessage` parameter. This method tracks a delivered event only if one of the following is true:

- The tracking consent feature is disabled.
- The tracking consent feature is enabled and `has_tracking_consent` is `true`.

If you are invoking the `Exponea.trackInAppContentBlockError` method manually and you want to ignore tracking consent, you may use `Exponea.trackInAppContentBlockErrorWithoutTrackingConsent` instead. This method will track the event regardless of tracking consent.
