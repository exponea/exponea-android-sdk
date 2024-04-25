## :arrow_double_up: [SDK version update guide](./../Guides/VERSION_UPDATE.md)

## Release Notes
## Release Notes for 3.13.1
#### April 25, 2024
* Bug Fixes
  * Fixed: Segmentation API callback with includeFirstLoad=true wasn't notified for empty (cache) vs empty (fetch) segmentations data


## Release Notes for 3.13.0
#### April 16, 2024
* Features
  * Segmentation API feature support
* Bug Fixes
  * Fixed: Customer Token authorization could be forced to used after anonymization


## Release Notes for 3.12.0
#### March 28, 2024
* Features
  * In-app message load refactoring (show on the first load etc.)
  * In-app message documentation extended with tracking and consent information
* Bug Fixes
  * Fixed: NotificationChannel is not registered after autobackup and app reinstall
  * Fixed: Config for automaticPushNotification is not stored locally
  * Fixed: Image for App Inbox "PUSH" message detail is not shown correctly
  * Fixed: PUSH token tracking event is not consider allowDefaultCustomerProperties flag


## Release Notes for 3.11.2
#### January 11, 2024
* Bug Fixes
  * Fixed: Invoking of In-app content blocks behaviour callback from outside is not reflected to local flags about showing and interaction


## Release Notes for 3.11.1
#### December 23, 2023
* Bug Fixes
  * Fixed: Using of HtmlNormalizer is not available as public, and normalization process can not be set
  * Fixed: Loading of In-app content block HTML message with same content does not trigger onMessageShown event


## Release Notes for 3.11.0
#### December 15, 2023
* Features
  * More detailed logging added to In-app content blocks
  * In-app content blocks behaviour handler for view lifecycle events
  * In-app content blocks tracking API with handling of tracking consent according to DSGVO/GDPR


## Release Notes for 3.10.0
#### November 27, 2023
* Features
  * In-app content blocks listener for event when content is loaded and ready to show
  * Push notification permission request usage example added to documentation for Android 13 (API 33)
  * SDK configuration extended with allowWebViewCookies param to allow cookies usage for WebView
* Bug Fixes
  * Fixed: Source code contains small lint issues


## Release Notes for 3.9.0
#### October 31, 2023
* Features
  * Viewport meta tag was removed from forbidden constructs for all HTML messages due to scaling issues
  * Tracking of push token has been described with more details in documentation
* Bug Fixes
  * Fixed: Showing of In-app Content Block do not respect priority properly
  * Fixed: Missing permission in Manifest to requesting notification permission feature


## Release Notes for 3.8.0
#### October 20, 2023
* Features
  * Example of Universal link handling added into example app
  * Link for In-app content block guide has been added into documentation
  * Anonymize feature has been described with more details in documentation
  * Configuration property 'tokenTrackFrequency' has been described with more details in documentation
  * Push notification Payload structure has been documented
  * Push notification clicked event could be tracked without requirement of runtime SDK init (from killed state)
  * Push notification permission request support
  * App Inbox detail image inset added as Configuration property
  * Project token validation added into SDK initialization process
* Bug Fixes
  * Fixed: Screen orientation 'sensor' was applied while showing of In-app message
  * Fixed: Images with long URL was not correctly cached for In-app messages
  * Fixed: Loading of In-app content blocks was causing ANR on UI thread
  * Fixed: Showing of image with non-https URL was causing exception or crash
  * Fixed: Push token was not tracked while 'automaticPushNotification' was disabled
  * Fixed: Example app for HMS calls API for tracking of FCM push token
  * Fixed: Click on button was ignored if action was created in HTML Visual builder


## Release Notes for 3.7.1
#### September 08, 2023
* Features
  * Small internal refactoring for better compatibility with MAUI wrapper


## Release Notes for 3.7.0
#### July 28, 2023
* Features
  * In-app content block feature has been added into SDK
  * Documentation is updated to describe fetching of In-app messages while `identifyCustomer` process in detail
* Bug Fixes
  * Fixed: Action `click` event from In-app messages, and App Inbox HTML Inbox messages tracked button text with HTML tags
  * Fixed: Proguard configuration for GSON library was missing for R8


## Release Notes for 3.6.1
#### May 22, 2023
* Features
  * New AppInbox provider has been created for easier styling of UI components
  * Android API supported version has been increased to 33
  * Small internal refactoring for better compatibility with other wrappers


## Release Notes for 3.6.0
#### April 17, 2023
* Features
  * Ability to track a user 'interaction' while closing InApp message
  * Support section added to main Readme
  * JSoup library updated to 1.15.4 due to reported vulnerability of library
  * InApp loading flow has been described more deeply in documentation
* Bug Fixes
  * Fixed: Marking of App Inbox message as read failed due to invalid usage of Customer IDs


## Release Notes for 3.5.0
#### March 30, 2023
* Features
  * Now we are able to show PUSH notification without requirement of runtime SDK init (from killed state), all events will be tracked
* Bug Fixes
  * Fixed: Empty in-app message response did not clear the cache so previously loaded messages persisted there
  * Fixed: Invalid empty response can lead to crash
  * Fixed: PUSH token handling issue when token update first run - not saved when receiving before SDK init
  * Fixed: Clicked PUSH event did not start a session properly
  * Fixed: Incorrect sessionTimeout values in documentation

## Release Notes for 3.4.0
#### February 16, 2023
* Features
  * Extended App Inbox functionality with new HTML message type - Inbox message
  * Updated documentation for Deeplink handling in active Activity
  * Added support for Customer token authorization
  * All SDK methods are invoked after SDK has been initialised
* Bug Fixes
  * Fixed: Documentation for NotificationData usage has been updated

## Release Notes for 3.3.0
#### December 15, 2022
* Features
  * Added App Inbox feature with PUSH message type support

## Release Notes for 3.2.1
#### November 24, 2022
* Features
  * Updated guide documentation for FCM integration setup
  * Upgraded build and SDK dependencies of core libraries
  * Added Configuration flag to be able to disable tracking of default properties along with customer properties
* Bug Fixes
  * Fixed: Supported Android version needs to be updated in docs
  * Fixed: Handling of universal links in HTML InApp messages was not working properly


## Release Notes for 3.2.0
#### October 19, 2022
* Features
  * Added feature to handle tracking consent according to DSGVO/GDPR
  * Guiding documentation added for Push notification update after certain circumstances
* Bug Fixes
  * Fixed: License established to MIT
  * Fixed: Documentation for Push token retrieval shows FirebaseMessaging usage


## Release Notes for 3.1.0
#### August 09, 2022
* Features
  * Added a support of HTML InApp messages
  * Added a link to 'Version update' guideline into release notes and README


## Release Notes for 3.0.6
#### July 28, 2022
* Features
  * Added a support for gzip and brotli HTTP compression for lower bandwith
* Bug Fixes
  * Fixed: Delivered push notification is not shown and tracked on Android 26+ after app cold-start by opening a push notification (This issue is since version 2.9.7)


## Release Notes for 3.0.5
#### May 23, 2022
* Features
  * Added example of Firebase token retrieval after adding Exponea SDK into already existing project
  * Documentation note added that default properties are sent along with customer properties
  * Example of usage of recommendation_id rewritten to match iOS documentation
  * Documentation note for App Links data tracking with new vs resumed session
* Bug Fixes
  * Fixed: PushToken repository migration to support auto-backup due to issues with obsolete tokens


## Release Notes for 3.0.4
#### April 22, 2022
* Features
  * Warn in log when outdated SDK version is used
* Bug Fixes
  * Fixed: Track to dev project also if SDK is used from the tests
  * Fixed: Development logs in production appCenter project
  * Fixed: In-app closing when screen is rotated


## Release Notes for 3.0.3
#### March 03, 2022
* Features
  * Callback for reacting on in-app message action clicks - check [In-app messages documentation](./IN_APP_MESSAGES.md) 

## Release Notes for 3.0.2
#### February 04, 2022
* Features
  * OkHttp library version bump

## Release Notes for 3.0.1
#### January 19, 2022
* Bug Fixes
  * Fixed: Broadcast push notification actions (Removed by mistake in 3.0.0)


## Release Notes for 3.0.0
#### January 04, 2022
* Features
  * Huawei push notifications support
  * Changes regarding Android S - Notification trampolining removed, androidx.work:work-runtime library updated, exported attribute added where necessary
  * Minimal SDK version support has been increased to 17
* Bug Fixes
  * Fixed: In-Apps caching fixed - Images with the same URL are downloaded only once
  * Fixed: In-Apps fetching fixed - In-app messages are no longer fetched when push notification auto initializes the SDK. In-apps are fetched only after the app is started
  * Fixed: Notification sound file name can be now specified with the extension
  * Fixed: Inconsistency with iOS SDK - Added platform attribute for a banner event, send banner type propery value in lower case


## Release Notes for 2.9.7
#### November 05, 2021
* Features
  * Url for in-app message banner click event is now tracked as link attribute - [Issue #39](https://github.com/exponea/exponea-android-sdk/issues/39)
  * PaperDb database was replaced with the Room database, and migration was implemented. Migration will be tried only once, and if some problem with PaperDB occurs, it will be skipped. Migration will also happen only if the number of events in the database does not exceed a thousand events. This is for performance reasons and because we consider having a thousand events in the database an anomaly since they should be flushed frequently.
* Bug Fixes
  * Fixed: Delivered push notification event is no longer tracked if a user has notifications turned off and push notification arrives in the background.
  * Fixed: Exception, when autoclose is enabled for the in-app message and the Activity is no longer available on message close, was fixed.


## Release Notes for 2.9.6
#### August 16, 2021
* Features
  * JCenter removed
  * OkHttp updated


## Release Notes for 2.9.5
#### July 02, 2021
* Features
  * Flexible event attributes in mobile push notifications. Custom tracking attributes added into push notification payload are automatically included in the events as properties.
* Bug Fixes
  * Fixed: Session start is no longer tracked on anonymizing when automatic session tracking is off.
  * Fixed: In-app message buttons with long text are no longer pushed outside the window.



## Release Notes for 2.9.4
#### May 28, 2021
* Features
  * Documentation improvements
* Bug Fixes
  * Fixed: In-app messages now initialize properly after killing the application.
  * Fixed: In-app message without the image is no longer blocking other in-app messages from showing,


## Release Notes for 2.9.3
#### April 21, 2021
* Features
  * Push notifications events are now chronologically synced (the event with status `sent` always occurs before the event with status `delivered`). 
* Bug Fixes
  * Fixed: The Exponea Android SDK no longer depends on version properties in BuildConfig class as they are no longer available since Gradle Plugin 4.1.
  * Fixed: In-app messages with A/B testing now properly handle the control group.


## Release Notes for 2.9.1
#### January 07, 2021
* Features
  * In-app message clicked event now contains property `text` with label of the button that was interacted with.
  * In-app messages are only preloaded when a new session is started. To ensure that you get fresh in-app messages while testing, kill the app between runs.
  * Updated dependencies, moved all libraries to AndroidX.
* Bug Fixes
  * Fixed: Crash occurring when push notification is received and the notification channel provided in configuration doesn't exist.


## Release Notes for 2.9.0
#### November 20, 2020
* Features
  * Removed Firebase default init override. By default Firebase initializes before the application is started using FirebaseInitProvider. Our SDK was removing this initializer and manually creating Firebase app when the Exponea SDK was initialized, to make Firebase token tracking easier. This has caused issues in React Native, where other packages rely on the default Firebase initializer. In 2.8.3 we made a change that remembers Firebase token until Exponea SDK is initialized, so there is no point in overriding default Firebase initializer any more. 
  * Documentation improvements


## Release Notes for 2.8.3
#### October 06, 2020
* Features
  * Paper DB updated to version 2.7.1 that should fix rare crashes.
  * When Firebase is initialized before Exponea SDK, push notification token is stored until Exponea is initialized.

## Release Notes for 2.8.2
#### September 09, 2020
* Features
  * Default properties that are tracked with every event can be changed at runtime by setting `Exponea.defaultProperties` property.
  * In-app messages definitions are re-downloaded every time the customer is identified. After customer identification, the set of in-app messages available for the customer may change due to targeting.
  
* Bug Fixes
  * Fixed: Notification sound was played when the app notifications were disabled or the notification channel was muted. SDK now respects these settings + “Do not disturb” setting.
  * Fixed: When the SDK was initialized with activity context, this context was stored causing memory leak - the activity could not be garbage collected. The SDK now only stores application context.


## Release Notes for 2.8.1
#### August 07, 2020
* Features
  * Support for new in-app message delay and timeout fields soon to be available in Exponea web app.
  * Troubleshooting guide for [In-app messages](IN_APP_MESSAGES.md).
* Bug Fixes
  * Fixed: Opening a deeplink that couldn't be handled inside in-app message would cause the application to crash. Now, an error is logged.
  * Fixed: In-app message text was not correctly centered.
  * Fixed: Serialization issue in in-app messages would prevent some in-app messages from being displayed. 
  * Fixed: String mode errors with unclosed closable resources.

## Release Notes for 2.8.0
#### July 20, 2020
* Features
  * Silent push notifications support. You're now able to send background updates to your application and respond to them using `Exponea.notificationDataCallback`. Delivery of silent push notifications is tracked to Exponea backend.
  * When the application is started from a push notification, resulting session will contain UTM parameters.
  * Updated push notifications documentation and self-check mechanism to make notifications integration easier.
* Bug Fixes
  * Fixed: Push notification sound is no longer played in `Do not Disturb` mode


## Release Notes for 2.7.4
#### June 30, 2020
* Features
  * When initialized with instance of resumed `AppCompatActivity`, the SDK now automatically starts the session and displays in-app message (if applicable). Before, the SDK would perform these actions after next activity resume.

## Release Notes for 2.7.3
#### May 01, 2020
* Features
  * Retrieve the cookie of the current customer used for tracking by calling `Exponea.customerCookie`.
  * Improved logging for in-app messages explaining why each message should/shouldn’t be displayed.
* Bug Fixes
  * Fixed: Updated Gradle and removed unused dependencies.
  * Removed: Legacy banners implementation that wasn’t working properly


## Release Notes for 2.7.2
#### April 09, 2020
* Features
  * Switching projects in `anonymize()` method. If you need to switch projects, you can use `anonymize()` method to create a new customer and start fresh tracking into a new project. Please see [ANONYMIZE.md](./ANONYMIZE.md) for more information.
* Bug Fixes
  * Fixed: When the app was opened from a push notifications, in-app messages would not initialize properly and would not show until the app is restarted.
  * Fixed: When the SDK was initialized with the application context instead of the activity context, in-app messages would not show properly.
  * Fixed: Tracking to multiple projects. It now requires both project token and authorization token. Please see [PROJECT_MAPPING.md](./PROJECT_MAPPING.md).


## Release Notes for 2.7.1
#### March 24, 2020
* Features
  * Push notification accent color setting - you can now set ExponeaConfiguration property `pushAccentColor` to change color of push notification title and buttons.
  * Improved documentation on handling push notifications and flushing setup.
* Bug Fixes
  * Fixed: Occasional crash during in-app message preload.
  * Fixed: Push notifications deeplinking issue. In some rare cases, push notification with deeplink would not open correctly.

## Release Notes for 2.7.0
#### March 02, 2020
* Features
  * New in-app messages - display rich messages when app starts or an event is tracked - even when offline. This SDK is fully ready for the feature when it is publicly available soon.
* Bug Fixes
  * Fixed: Rare internal database thread-safety related crash.

## Release Notes for 2.6.3
#### February 20, 2020
* Features
  * Updated Firebase messaging library to version 2.1.0
  * Updated minimum SDK version to 16 as recommended by Google
  * Minimal SDK version support has been increased to 16

## Release Notes for 2.6.2
#### January 10, 2020
* Bug Fixes
  * Fixed: Fetch recommendations functionality was reimplemented, it was incorrectly removed at one point. (see [FETCH.md](./FETCH.md))
  * Fixed: Thread safety problem in crash reporting that caused the application to crash (it would still report the crash itself).
  * Fixed: Issue with serializing Infinity, -Infinity and NaN. Double and Float variables can hold these values, but they are not part of JSON standard and Exponea doesn't support them. These values will now be reported as string values, but should be avoided. Fixes [Issue #20](https://github.com/exponea/exponea-android-sdk/issues/20).
  * Removed: Automatic payment tracking was broken and has been removed from the Exponea Android SDK. In case you're interested in this functionality let us know. Manual tracking is simplified - device properties are now automatically added. (see [PAYMENT.md](./PAYMENT.md))

## Release Notes for 2.6.1
#### December 20, 2019
* Bug Fixes
  * Fixed: Google Play Billing Library version > 2.0 would cause the SDK to fail to initialize properly. To mitigate the issue, SDK now only disables automatic payment tracking. Proper solution will be implemented later.

## Release Notes for 2.6.0
#### November 26, 2019
* Features
  * The SDK is now able to report the SDK-related crashes to Exponea. This helps us keep the SDK in a good shape and work on fixes as soon as possible. 
* Bug Fixes
  * Fixed: https://github.com/exponea/exponea-android-sdk/issues/16 - The internal database could have been initialized twice. This won't happen anymore.
  * Fixed: The SDK now correctly reports the version of the host application in the session events.

## 2.5.0
#### November 05, 2019
* Features
  * The SDK has a new protective layer for the public API as well as for the interaction with the operating system. It means that in the production setup it prefers to fail silently instead of crashing the whole application.
  * Push notification events now contain more information about campaigns and the action taken and are consistent with Exponea iOS SDK.
* Bug Fixes
  * Increased overall code quality by fixing many lint errors. This makes many warnings from the SDK disappear when the application is compiled.
  * The internal mechanisms of the SDK are now really hidden and not usable by the application. It prevents developers from using some undocumented internal part of the SDK in an inappropriate way.
  * Fixed: The periodic flushing mechanism has been fixed. Once it was started, it wouldn't stop. Now it does.
  * There are significant improvements in the unit tests of the SDK.

### 2.4.0
#### September 30, 2019
* Features
    * [App links](./APP_LINKS.md): SDK can now track app opens from App Link. Sessions that are started from App Link contain campaign data.
* Bug Fixes
    * Exponea SDK can only be initialized once. Subsequent calls to `Exponea.init` are ignored. Multiple instances of SDK cause sessions to be tracked twice.

### 2.3.3
#### August 28, 2019
* Bug Fixes
  * Fixed: Properties in the events were found to be accumulating over time. This happened always after tracking multiple events (after multiple calls of `Exponea.trackEvent()`, `Exponea.identifyCustomer()` or similar). The impacted versions are thankfully only `2.3.1` and `2.3.2`.

### 2.3.2
* `WorkManager` updated to the latest version to avoid inconsistency and crashes
* Configuration now has a token update frequency, which specifies how often should the push token be sent to Exponea (on change, on every start, daily)

### 2.3.1
* Added option to specify default properties to be tracked with all events

### 2.3.0
* Added option to fetch consent cateogires (see [FETCH.md](./FETCH.md))

### 2.2.7
* Make push notifications valid when they have a title or a message

### 2.2.6
* Improvements to notification tracking parameters

### 2.2.5
* Documentation improvements
* Change notification action_type to "mobile notification" for better tracking purposes

### 2.2.4
* Update dependencies

### 2.2.3
* Possible fix for null enum ordinal crash on routes when loading from database
* Don't display push notifications if title is empty

### 2.2.2
* Fixed a bug with notification body deeplink not getting parsed correctly

### 2.2.1
* Add the ability to use custom Firebase service by create public method in Exponea to handle remote push notification, which tracks and optionally shows notification

### 2.2.0
* Fix for crash with billing client 1.2 dependency 
	- **It is now required to provide a list of SKUs of in app purchases you wish to track. Please see [this document](./PAYMENT.md) for information about how to set this up.**

### 2.1.0
* Advanced tracking for push-notification actions
* Flushing behaviour fixes

### 2.0.0

* Removal of deprecated functions
* Documentation improvements

### 1.2.0-beta-5

* Fix parsing of notification body action deeplink url
* Improve initialising Exponea from configuration file
* Update documentation

### 1.2.0-beta-4

* Add a workaround for a crash happening only on Meizu M5 with Android 6.0 due to OkHttp 

### 1.2.0-beta-3
* Fix an edge case where a notification triggered intent might crash the app
* Add automatic ProGuard rules to the SDK 
* Internal improvements to rich push notifications code

### 1.2.0-beta-2
* Fix image quality in rich push
* Fix dismissal of notification tray on button click

### 1.2.0-beta-1
* Adds support for the rich push notifications feature

### 1.1.7
* Fix for crash when Looper.prepare() wasn't called on Exponea init
* Deprecations of basic auth in favor of token auth
* Anonymize feature now carries over the push token of the device

### 1.1.6
* Automatic session tracking property in configuration is now properly respected
* Fixed a crash when Firebase was initialised before Exponea

### 1.1.5
* Fixed a crash when push notification payload was null

### 1.1.4
* Timestamp type changed from Long to Double for better precision

### 1.1.3
* Fixed anonymize functionality

### 1.1.2
* Default flush mode set to `IMMIDIATE`
* Improved retry mechanism for events flushing


### 1.1.1
* Expandable Notifications

### 1.1.0

* [Anonymize feature](./ANONYMIZE.md)
* Dependencies update
* Pass custom data to FCM manager
* Fixed flushing behavior (Changed timestamp from milliseconds to seconds)
* Fixed push tracking with Android 8+
* SDK is now compatible with Android API 14+  

### 1.0

Initial release.
