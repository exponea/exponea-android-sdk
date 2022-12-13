## Tracking consent

Based on the recent judgment (May 2022) made by the Federal Court of Justice in Germany (Bundesgerichtshof – BGH) 
regarding the EU Datenschutz Grundverordnung (EU-GDPR), all access to data on the affected person’s device would 
require explicit consent. For more info see [Configuration of the tracking consent categories](https://documentation.bloomreach.com/engagement/docs/configuration-of-tracking-consent).

The SDK is adapted to the rules and is controlled according to the data received from the Push Notifications or InApp Messages.
If the tracking consent feature is disabled, the Push Notifications and InApp Messages data do not contain 'hasTrackingConsent' and their tracking behaviour has not been changed, so if the attribute 'hasTrackingConsent' is not present in data, SDK considers it as 'true'.
If the tracking consent feature is enabled, Push Notifications and InApp Messages data contain 'hasTrackingConsent' and the SDK tracks events according to the boolean value of this field.

Disallowed tracking consent ('hasTrackingConsent' provided with 'false' value) can be overridden with URL query param 'xnpe_force_track' with 'true' value.

### Event for push notification delivery

Event is normally tracked by calling `Exponea.trackDeliveredPush` or `Exponea.handleRemoteMessage`. This methods are tracking
a delivered event only if:

* Tracking consent feature is disabled
* Tracking consent feature is enabled and 'hasTrackingConsent' has 'true' value

If you are using `Exponea.trackDeliveredPush` method manually and you want to avoid to consider tracking, you may use `Exponea.trackDeliveredPushWithoutTrackingConsent` instead. This method will do track event ignoring tracking consent state.

### Event for clicked push notification

Event is normally tracked by calling `Exponea.trackClickedPush`. This method is tracking
a clicked event only if:

* Tracking consent feature is disabled
* Tracking consent feature is enabled and 'hasTrackingConsent' has 'true' value
* Action URL contains 'xnpe_force_track' with 'true' value independently from 'hasTrackingConsent' value

> Event that is tracked because of `xnpe_force_track` (forced tracking) will contains an additional property `tracking_forced` with value `true` 

If you are using `Exponea.trackClickedPush` method manually and you want to avoid to consider tracking, you may use `Exponea.trackClickedPushWithoutTrackingConsent` instead. This method will do track event ignoring tracking consent state.

### Event for clicked InApp Message

Event is normally tracked by calling `Exponea.trackInAppMessageClick`. This method is tracking
a clicked event only if:

* Tracking consent feature is disabled
* Tracking consent feature is enabled and 'hasTrackingConsent' has 'true' value
* Action URL contains 'xnpe_force_track' with 'true' value independently from 'hasTrackingConsent' value

> Event that is tracked because of `xnpe_force_track` (forced tracking) will contains an additional property `tracking_forced` with value `true`

If you are using `Exponea.trackInAppMessageClick` method manually and you want to avoid to consider tracking, you may use `Exponea.trackInAppMessageClickWithoutTrackingConsent` instead. This method will do track event ignoring tracking consent state.

### Event for closed InApp Message

Event is normally tracked by calling `Exponea.trackInAppMessageClose`. This method is tracking a delivered event only if:

* Tracking consent feature is disabled
* Tracking consent feature is enabled and 'hasTrackingConsent' has 'true' value

If you are using `Exponea.trackInAppMessageClose` method manually and you want to avoid to consider tracking, you may use `Exponea.trackInAppMessageCloseWithoutTrackingConsent` instead. This method will do track event ignoring tracking consent state.

### Event for opened AppInbox Message

Event is normally tracked by calling `Exponea.trackAppInboxOpened`. This method is tracking a delivered event only if:

* Tracking consent feature is disabled
* Tracking consent feature is enabled and 'hasTrackingConsent' has 'true' value

If you are using `Exponea.trackAppInboxOpened` method manually and you want to avoid to consider tracking, you may use `Exponea.trackAppInboxOpenedWithoutTrackingConsent` instead. This method will do track event ignoring tracking consent state.

### Event for clicked AppInbox Message action

Event is normally tracked by calling `Exponea.trackAppInboxClick`. This method is tracking a clicked event only if:

* Tracking consent feature is disabled
* Tracking consent feature is enabled and 'hasTrackingConsent' has 'true' value
* Action URL contains 'xnpe_force_track' with 'true' value independently from 'hasTrackingConsent' value

> Event that is tracked because of `xnpe_force_track` (forced tracking) will contains an additional property `tracking_forced` with value `true`

If you are using `Exponea.trackAppInboxClick` method manually and you want to avoid to consider tracking, you may use `Exponea.trackAppInboxClickWithoutTrackingConsent` instead. This method will do track event ignoring tracking consent state.
