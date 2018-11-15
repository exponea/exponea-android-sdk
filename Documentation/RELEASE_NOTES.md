## Release Notes

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
