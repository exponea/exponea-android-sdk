---
title: Android App Links for Android SDK
excerpt: Enable and track Android App Links in your app using the Android SDK
slug: android-sdk-app-links
categorySlug: integrations
parentDocSlug: android-sdk
---

Android App Links (sometimes called Universal Links) allow the links you send through Engagement to open directly in your native mobile application without any redirects that would hinder your users' experience.

For details on how Android App Links work and how they can improve your users' experience, refer to the [Universal Links](https://documentation.bloomreach.com/engagement/docs/universal-link) section in the Campaigns documentation.

This page describes the steps required to support and track incoming Android App Links in your app using the Android SDK.

## Enable Android App Links

To support Android App Links in your app, you must create a two-way association between your app and your website and specify the URLs that your app handles. To this end, you must add an intent filter to your app's Android manifest and host a Digital Asset Link JSON file on your domain.

### Add intent filter to Android manifest

The [App Links Assistant in Android Studio](https://developer.android.com/studio/write/app-link-indexing.html#intent) can help you create intent filters in your manifest and map existing URLs from your website to activities in your app. The App Links Assistant also adds template code in each corresponding activity to handle the intent.

Alternatively, you can set this up manually by following the instructions in [Verify Android App Links](https://developer.android.com/training/app-links/verify-android-applinks) in the official Android documentation.

Ensure the intent filter contains the `android:autoVerify="true"` attribute to signal to the Android system that it should check your Digital Asset Link JSON and automatically handle App Links.

Example:

```xml
<activity ...>

    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="www.your-domain.name" />
    </intent-filter>

</activity>
```

### Add Digital Asset Link JSON to your domain

You must declare the association between your website and your intent filters by hosting a [Digital Asset Links](https://developers.google.com/digital-asset-links/v1/getting-started) JSON file at the following location:

```
https://domain.name/.well-known/assetlinks.json
```

Again, the [App Links Assistant in Android Studio](https://developer.android.com/studio/write/app-link-indexing.html#associatesite) can help generate the file for you.

Alternatively, you can do this manually following the instructions in [Declare website associations](https://developer.android.com/training/app-links/verify-android-applinks#web-assoc) in the official Android documentation.

Example:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "your.package.name",
    "sha256_cert_fingerprints":["SHA256 fingerprint of your app’s signing certificate"]
  }
}]
```

## Track Android App Links

The SDK can automatically decide whether the intent that opened your application is an App Link. All that is required is calling `Exponea.handleCampaignIntent(intent, applicationContext)`.

App Link parameters are automatically tracked in `session_start` events when a new session is started for a given Universal Link click. If your app starts a new session, the campaign parameters (`utm_source`, `utm_campaign`, `utm_content`, `utm_medium`, `utm_term` and `xnpe_cmp`) are sent within the session parameters to enable you to attribute the new session to an App Link click.

If the App Link contains a parameter `xnpe_cmp` then an additional `campaign` event is tracked. The parameter `xnpe_cmp` represents a campaign identifier typically generated for Email or SMS campaigns.

To track session events with App Link parameters, you must call `Exponea.handleCampaignIntent` **before** your Activity `onResume` method is called. Ideally, make the call in your MainActivity `.onCreate` method.

Example:

```kotlin
 class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Exponea.handleCampaignIntent(intent, applicationContext)
    }
}
```

> 👍
>
> Note that `handleCampaignIntent` takes care of tracking only. You must still read the data from the intent and use it to determine the relevant app content to render. This is outside the scope of the SDK but for an example implementation you can take a look at the `resolveDeeplinkDestination` and `handleDeeplinkDestination` methods in [`MainActivity`](https://github.com/exponea/exponea-android-sdk/blob/main/app/src/main/java/com/exponea/example/view/MainActivity.kt) in the [Example app for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-example-app).

> ❗️
>
> If an existing session is resumed by opening an App Link, the resumed session is **NOT** attributed to the App Link click, and the App Link click parameters are not tracked in the `session_start` event. Session behavior is determined by the `automaticSessionTracking` and `sessionTimeout` parameters described in [Configuration for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration). Please consider this in case of manual session handling or while testing universal link tracking during the development.

> ❗️
>
> It's possible to invoke `Exponea.handleCampaignIntent` before SDK initialization if the SDK was initialized previously. In such a case, `Exponea.handleCampaignIntent` will track events using the configuration of the last initialization. Consider initializing the SDK initialization in `Application::onCreate` to ensure up-to-date configuration is applied in case of an update of your application.
