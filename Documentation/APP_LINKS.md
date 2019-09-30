## 🔍 Enabling Android App Links
Android App Links are HTTP URLs that bring users directly to specific content in your Android app. In order to enable your app to handle App Links, you need to add intent filter to your Android manifest and host Digital Asset Link JSON file on your domain.

### Adding intent filter to Android manifest
You can use [Android studio](https://developer.android.com/studio/write/app-link-indexing.html#intent) to help you set this up, or do it [manually](https://developer.android.com/training/app-links/verify-site-associations.html#request-verify). 

It's important to set `android:autoVerify="true"` so that the Android system can check your Digital Asset Link JSON and automatically handle App Links.

#### 💻 Example
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

### Adding Digital Asset Link JSON to your domain
Just like with intent filters, [Android studio](https://developer.android.com/studio/write/app-link-indexing.html#associatesite) can help generate files for you. If you decide to do this manually, official [Google documentation](https://developer.android.com/training/app-links/verify-site-associations.html#web-assoc) explains how to do it in detail.

Resulting file should be hosted at `https://your-domain.name/.well-known/assetlinks.json`.

#### 💻 Example
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

> **NOTE:** To get certificate SHA256 fingerprint you can use `keytool -list -v -keystore my-release-key.keystore`

## 🔍 Tracking Android App Links
Exponea SDK can automatically decide whether the Intent that opened your application is an App Link, so you just need to call `Exponea.handleCampaignIntent(intent, applicationContext)`.

In order to track session events with App Link parameters, you should call `Exponea.handleCampaignIntent` **before** your Activity onResume method is called. Ideally, make the call in your MainActivity `.onCreate` method.

#### 💻 Example
```kotlin
 class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Exponea.handleCampaignIntent(intent, applicationContext)
    }
}
```