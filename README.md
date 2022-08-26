<p align="center">
  <img src="./Documentation/logo_yellow.png?raw=true" alt="Exponea"/>
</p>

### Specs
[![API](https://img.shields.io/badge/API-14%2B-yellow.svg?style=flat)](https://android-arsenal.com/api?level=14)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](https://opensource.org/licenses/Apache-2.0)

## Exponea Android SDK

This library allows you to interact from your application or game with the Exponea App.

Exponea empowers B2C marketers to raise conversion rates, improve acquisition ROI, and maximize customer lifetime value.

It has been written 100% in Kotlin with ❤️

## 📦 Installation

### Download

Download via Gradle:

```groovy
dependencies {
  implementation 'com.exponea.sdk:sdk:3.1.0'
}
```

Download via Maven:

```groovy
<dependency>
    <groupId>com.exponea.sdk</groupId>
    <artifactId>sdk</artifactId>
    <version>3.1.0</version>
</dependency>
```

## 📱 Demo Application

Check out our [sample project](https://github.com/exponea/exponea-android-sdk/tree/master/app) to try it yourself! 😉

## 💻 Usage

### Getting Started

To implement the Exponea SDK you must configure the SDK first:

*  **[Detailed Guides about ExponeaSDK Integrations](./Guides/README.md)**
    * [SDK version update guide](./Guides/VERSION_UPDATE.md)
* [Configuration](./Documentation/CONFIG.md)
* [Project Mapping](./Documentation/PROJECT_MAPPING.md)

Then you can start using all supported features:

* [Track Events / Customer properties](./Documentation/TRACK.md)
* [Track Android App Links](./Documentation/APP_LINKS.md)
* [Push Notification Events](./Documentation/PUSH.md)
* [Flush](./Documentation/FLUSH.md)
* [Fetch Data](./Documentation/FETCH.md)
* [Payments](./Documentation/PAYMENT.md)
* [Anonymize](./Documentation/ANONYMIZE.md)
* [In-app messages](./Documentation/IN_APP_MESSAGES.md)

If facing any issues, look for **Troubleshooting** section in the respective document.

## 🔗 Useful links

* [Exponea Developer Hub](https://developers.exponea.com)
* [Exponea App](https://app.exponea.com/login)

## 📝 Release Notes

Release notes can be found [here](./Documentation/RELEASE_NOTES.md).

## ⚠️ Version Disclaimer

This SDK supports **API 14+** and **Android 4+**. If you wish to interact with Exponea on lower API/Android versions please refer to the [old SDK located here](https://github.com/infinario/android-sdk).
