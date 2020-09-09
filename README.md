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
  implementation 'com.exponea.sdk:sdk:2.8.2'
}
```

Download via Maven:

```groovy
<dependency>
    <groupId>com.exponea.sdk</groupId>
    <artifactId>sdk</artifactId>
    <version>2.8.2</version>
</dependency>
```

## 📱 Demo Application

Check out our [sample project](https://github.com/exponea/exponea-android-sdk/tree/master/app) to try it yourself! 😉

## 💻 Usage

### Getting Started

To implement the Exponea SDK you must configure the SDK first:

*  **[Detailed Guides about ExponeaSDK Integrations](./Guides/README.md)**
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

## 🔗 Useful links

* [Exponea Developer Hub](https://developers.exponea.com)
* [Exponea App](https://app.exponea.com/login)

## 📝 Release Notes

Release notes can be found [here](./Documentation/RELEASE_NOTES.md).

## ⚠️ Version Disclaimer

This SDK supports **API 14+** and **Android 4+**. If you wish to interact with Exponea on lower API/Android versions please refer to the [old SDK located here](https://github.com/infinario/android-sdk).

## 📄 License

**ExponeaSDK** is available under the Apache 2.0 license. See the [LICENSE](https://opensource.org/licenses/Apache-2.0) file for more info.

```
   Copyright 2018 Exponea

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
