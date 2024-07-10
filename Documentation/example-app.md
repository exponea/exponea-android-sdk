---
title: Example App
excerpt: Build, run, and navigate the example app included with the Android SDK
slug: android-sdk-example-app
categorySlug: integrations
parentDocSlug: android-sdk
---

The Exponea Android SDK includes an example application you can use as a reference implementation. You can build and run the app, test Engagement features, and compare the code and behavior of your implementation with the expected behavior and code in the example app.

## Prerequisites

You must have the following software installed to be able to build and run the example app:

- [Android Studio](https://developer.android.com/studio) with a [virtual device](https://developer.android.com/studio/run/managing-avds) set up

## Build and Run the Example App

1. Clone the [exponea-android-sdk](https://github.com/exponea/exponea-android-sdk) repository on GitHub:
   ```shell
   git clone https://github.com/exponea/exponea-android-sdk.git
   ```
2. Open the `exponea-android-sdk` project in Android Studio.
3. Open the file `sdk/build.gradle` and find the following line 
   ```
   apply from: 'publish-maven.gradle'
   ```
   Comment it out so it look like this:
   ```
   //apply from: 'publish-maven.gradle'
   ```
4. Run the example app on the Android emulator (`Run` > `Run 'app'` or Ctrl + R).

> 📘
>
> To enable push notifications in the example app, you must also configure the [Firebase integration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configure-firebase) or [Huawei integration](https://documentation.bloomreach.com/engagement/docs/android-sdk-configure-huawei) in the Exponea web app.

## Navigate the Example App

![Example app screens: configuration, fetch, track, track event](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/android-example-app-1.png)

When you run the app in the simulator, you'll see the **Authentication** view. Enter your [project token, API token, and API base URL](mobile-sdks-api-access-management). Optionally, enter an email address hard ID in the `Registered` field to identify the customer. Then click `Authenticate` to [initialize the SDK](ios-sdk-setup#initialize-the-sdk).
> [`AuthenticationActivity.kt`](https://github.com/exponea/exponea-android-sdk/blob/main/app/src/main/java/com/exponea/example/view/AuthenticationActivity.kt)

> 👍
>
> Make sure to prexif your API key with "Token ", for example:
> `Token 0b7uuqicb0fwuv1tqz7ubesxzj3kc3dje3lqyqhzd94pgwnypdiwxz45zqkhjmbf`.

The app provides several views, accessible using the bottom navigation, to test the different SDK features:

- The **Fetch** view enables you to fetch recommendations and consents, and open the app inbox.
  > [`FetchFragment.kt`](https://github.com/exponea/exponea-android-sdk/blob/main/app/src/main/java/com/exponea/example/view/fragments/FetchFragment.kt)
  
- The **Track** view enables you to test tracking of different events and properties. The `Custom Event` and `Identify Customer` buttons lead to their separate views to enter test data.
  > [`TrackFragment.kt`](https://github.com/exponea/exponea-android-sdk/blob/bf48aba5a58e5632bdc5d963c18ee24d7e200ec9/app/src/main/java/com/exponea/example/view/fragments/TrackFragment.kt)
  > [`TrackCustomAttributesDialog.kt`](https://github.com/exponea/exponea-android-sdk/blob/bf48aba5a58e5632bdc5d963c18ee24d7e200ec9/app/src/main/java/com/exponea/example/view/dialogs/TrackCustomAttributesDialog.kt)
  > [`TrackCustomEventDialog.kt`](https://github.com/exponea/exponea-android-sdk/blob/bf48aba5a58e5632bdc5d963c18ee24d7e200ec9/app/src/main/java/com/exponea/example/view/dialogs/TrackCustomEventDialog.kt)

- The **Manual Flush** view lets you trigger a manual data flush.
  > [`FlushFragment.kt`](https://github.com/exponea/exponea-android-sdk/blob/bf48aba5a58e5632bdc5d963c18ee24d7e200ec9/app/src/main/java/com/exponea/example/view/fragments/FlushFragment.kt)

- The **Anonymize** view lets you anonimyze the current user.
  > [`AnonymizeFragment.kt`](https://github.com/exponea/exponea-android-sdk/blob/bf48aba5a58e5632bdc5d963c18ee24d7e200ec9/app/src/main/java/com/exponea/example/view/fragments/AnonymizeFragment.kt)

- The **InAppCB** view displays in-app content blocks. Use placeholder IDs `example_top`, `ph_x_example_iOS`, `example_list`, `example_carousel`, and `example_carousel_and` in your in-app content block settings.
  > [`InAppContentBlocksFragment.kt`](https://github.com/exponea/exponea-android-sdk/blob/bf48aba5a58e5632bdc5d963c18ee24d7e200ec9/app/src/main/java/com/exponea/example/view/fragments/InAppContentBlocksFragment.kt)
  > [`fragment_inapp_content_blocks.xml`](https://github.com/exponea/exponea-android-sdk/blob/main/app/src/main/res/layout/fragment_inapp_content_blocks.xml)

Try out the different features in the app, then find the customer profile in the Engagement web app (under `Data & Assets` > `Customers`) to see the properties and events tracked by the SDK.

If you left the `Registered` field blank, the customer is tracked anonymously using a cookie soft ID. You can look up the cookie value in the logs and find the corresponding profile in the Engagement web app.

If you entered a hard ID (use an email address as value) in the `Registered` field, the customer is identified and can be found in Engagement web app by their email address.

> 📘
>
> Refer to [Customer Identification](https://documentation.bloomreach.com/engagement/docs/customer-identification) for more information on soft IDs and hard IDs.

![Example app screens: identify, flush, anonymize, content blocks](https://raw.githubusercontent.com/exponea/exponea-android-sdk/main/Documentation/images/android-example-app-2.png)

## Troubleshooting

If you encounter any issues building the example app, the following may help:

- In Android Studio, select `Build` > `Clean Project`, then `Build` > `Rebuild Project`.