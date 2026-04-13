---
title: Authorization for Android SDK
excerpt: Full authorization reference for the Android SDK
slug: android-sdk-authorization
category:
  uri: /branches/2/categories/guides/Developers
parent:
  uri: android-sdk-setup
---

The SDK exchanges data with Bloomreach APIs — either the Engagement API directly (when using `ProjectConfig`) or the [Data hub Event Stream API](https://documentation.bloomreach.com/data-hub/docs/event-streams) (when using `StreamConfig`) — through authorized HTTP/HTTPS communication. The SDK supports several authorization modes depending on the integration type:

- **Token authorization** (default for `ProjectConfig`) — public API access using an API key
- **Customer token authorization** (optional for `ProjectConfig`) — private API access using JWT via `AuthorizationProvider`
- **SDK auth token authorization** (for `StreamConfig`) — JWT-based authentication with automatic token lifecycle management

Developers can choose the appropriate authorization mode for the required level of security.

## Project Integration

### Token authorization

The default token authorization mode provides [public API access](https://documentation.bloomreach.com/engagement/reference/authentication#public-api-access) using an API key as a token. 

Token authorization is used for the following API endpoints by default:
* `POST /track/v2/projects/<projectToken>/customers` for tracking of customer data
* `POST /track/v2/projects/<projectToken>/customers/events` for tracking of event data
* `POST /track/v2/projects/<projectToken>/campaigns/clicks` for tracking campaign events
* `POST /data/v2/projects/<projectToken>/customers/attributes` for fetching recommendations
* `GET /data/v2/projects/<projectToken>/consent/categories` for fetching consents
* `POST /webxp/s/<projectToken>/inappmessages?compatibility=3` for fetching In-app messages
* `POST /webxp/s/<projectToken>/inappcontentblocks?v=2` for fetching In-app content blocks
* `POST /webxp/projects/<projectToken>/appinbox/fetch` for fetching of AppInbox data
* `POST /webxp/projects/<projectToken>/appinbox/markasread` for marking of AppInbox message as read
* `POST /campaigns/send-self-check-notification?project_id=<projectToken>` for part of self-check push notification flow

Developers must set the token using the `authorization` [Configuration for Android SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-configuration) parameter when [initializing the SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup#initialize-the-sdk):

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val configuration = ExponeaConfiguration()
        configuration.integrationConfig = ProjectConfig(
            baseUrl = "https://api.exponea.com",
            projectToken = "YOUR_PROJECT_TOKEN",
            authorization = "Token YOUR_API_KEY"
        )
        Exponea.init(App.instance, configuration)
    }
}
```

> ❗️
>
> Now you need to specify your `applicationId`. Refer to the [Configure application ID](#configure-application-id) for further information.

### Customer token authorization

Customer token authorization is optional and provides [private API access](https://documentation.bloomreach.com/engagement/reference/authentication#private-api-access) to select Engagement API endpoints. The [customer token](https://documentation.bloomreach.com/engagement/docs/customer-token) contains encoded customer IDs and a signature. When the Bloomreach Engagement API receives a customer token, it first verifies the signature and only processes the request if the signature is valid.

> ❗️
>
> Customer token authorization using `advancedAuthEnabled` and `AuthorizationProvider` is only supported with `ProjectConfig` integration. If `advancedAuthEnabled` is set to `true` with `StreamConfig`, the SDK will log a warning and ignore the setting. For stream-based integrations, use [SDK auth token authorization](#sdk-auth-token-authorization) instead.

The customer token is encoded using **JSON Web Token (JWT)**, an open industry standard [RFC 7519](https://tools.ietf.org/html/rfc7519) that defines a compact and self-contained way for securely transmitting information between parties.

The SDK sends the customer token in `Bearer <value>` format. Currently, the SDK supports customer token authorization for the following Engagement API endpoints:

* `POST /webxp/projects/<projectToken>/appinbox/fetch` for fetching of AppInbox data
* `POST /webxp/projects/<projectToken>/appinbox/markasread` for marking of AppInbox message as read

Developers can enable customer token authorization by setting the `advancedAuthEnabled` [configuration](configuration.md) parameter to `true` when [initializing the SDK](setup.md#initialize-the-sdk):

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val configuration = ExponeaConfiguration()
        configuration.integrationConfig = ProjectConfig(
            baseUrl = "https://api.exponea.com",
            projectToken = "YOUR_PROJECT_TOKEN",
            authorization = "Token YOUR_API_KEY" // !!! basic authorization is required
        )
        configuration.advancedAuthEnabled = true
        Exponea.init(App.instance, configuration)
    }
}
```
> ❗️
>
> Now you need to specify your `applicationId`. Refer to the [Configure application ID](#configure-application-id) for further information.

Additionally, developers must implement the `com.exponea.sdk.services.AuthorizationProvider` interface, ensuring that the `getAuthorizationToken` method returns a valid JWT token that encodes the relevant customer ID(s) and private API key ID:

```kotlin
class ExampleAuthProvider : AuthorizationProvider {
    override fun getAuthorizationToken(): String? {
        return "YOUR JWT TOKEN"
    }
}
```

You must register your `AuthorizationProvider` in your app's `AndroidManifest.xml` file as:

```xml
<application>
    ...
    <meta-data
        android:name="ExponeaAuthProvider"
        android:value="com.your.app.security.ExampleAuthProvider"
        />
</application>
```

> ❗️
>
> Customer tokens must be generated by a party that can securely verify the customer's identity. Usually, this means that customer tokens should be generated during the application backend login procedure. When the customer identity is verified (using password, 3rd party authentication, Single Sign-On, etc.), the application backend should generate the customer token and send it to the device running the SDK.

> 📘
>
> Refer to [Generating customer token](https://documentation.bloomreach.com/engagement/docs/customer-token#generate-customer-tokens) in the customer token documentation for step-by-step instructions to generate a JWT customer token.

#### Troubleshooting

If you implement `AuthorizationProvider` but it is not working as expected, check the logs for the following:
1. If you enable customer token authorization by setting the configuration flag `advancedAuthEnabled` to `true` but the SDK can't find a provider implementation, it will log the following message:
`Advanced auth has been enabled but provider has not been found`
2. If you register an `ExponeaAuthProvider` class in `AndroidManifest.xml` but the SDK cannot find it, it will log the following message:
`Registered <your class> class has not been found with detailed info`
3. If you register an `ExponeaAuthProvider` class in `AndroidManifest.xml` that doesn't implement the `AuthorizationProvider` interface, it will log the following message:
`Registered <your class> class has to implement com.exponea.sdk.services.AuthorizationProvider`

#### Asynchronous implementation of AuthorizationProvider

The customer token value is requested for every HTTP call at runtime. The method `getAuthorizationToken()` is written for synchronous usage but is invoked in a background thread. Therefore, you are able to block any asynchronous token retrieval (i.e. other HTTP call) and wait for the result by blocking this thread. If the token retrieval fails, you may return a NULL value but the request will automatically fail.

```kotlin
class ExampleAuthProvider : AuthorizationProvider {
    override fun getAuthorizationToken(): String? = runBlocking {
        return@runBlocking suspendCoroutine { done ->
            retrieveTokenAsync(
                    success = {token -> done.resume(token)},
                    error = {error -> done.resume(null)}
            )
        }
    }
}
```

> 👍
>
> Different network libraries support different approaches but the principle stays same - feel free to block the invocation of the `getAuthorizationToken` method.

#### Customer token retrieval policy

The customer token value is requested for every HTTP call that requires it.

Typically, JWT tokens have their own expiration lifetime and can be used multiple times. The SDK does not store the token in any cache. Developers may implement their own token cache as they see fit. For example:

```kotlin
class ExampleAuthProvider : AuthorizationProvider {

    private var tokenCache: String? = null

    override fun getAuthorizationToken(): String? = runBlocking {
         if (tokenCache.isNullOrEmpty()) {
             tokenCache = suspendCoroutine { done ->
                 retrieveTokenAsync(
                     success = {token -> done.resume(token)},
                     error = {error -> done.resume(null)}
                 )
             }
         }
         return@runBlocking tokenCache
     }
}
```

> ❗️
>
> Please consider to store your cached token more securely. Android offers multiple options such as [KeyStore](https://developer.android.com/training/articles/keystore) or [Encrypted Shared Preferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences).

> ❗️
>
> A customer token is valid until its expiration and is assigned to the current customer IDs. Bear in mind that if customer IDs change (due to invoking the `identifyCustomer` or `anonymize` methods), the customer token may become invalid for future HTTP requests invoked for new customer IDs.

## Stream Integration

### SDK auth token authorization

When using `StreamConfig` integration, the SDK supports JWT-based authentication through the SDK auth token. This token is used for all authorized API calls in stream-based integrations.

#### Setting the SDK auth token

You can set the SDK auth token in three ways:

1. **During SDK initialization** by including it in the `CustomerIdentity` passed to `Exponea.init()`. Refer to [Initialize with customer identity](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup#initialize-with-customer-identity) for details.

2. **During customer identification** by including it in the `CustomerIdentity`:

```kotlin
Exponea.identifyCustomer(
    customerIdentity = CustomerIdentity(
        customerIds = mapOf("registered" to "jane.doe@example.com"),
        sdkAuthToken = "your-jwt-token"
    )
)
```

3. **Independently** using the `setSdkAuthToken` method:

```kotlin
Exponea.setSdkAuthToken("your-jwt-token")
```

> ❗️
>
> `setSdkAuthToken` is only supported with `StreamConfig` integration. Calling it with `ProjectConfig` will log a warning and the call will be ignored.

#### Token storage

The SDK auth token is stored persistently and survives app restarts. The token is automatically cleared when `anonymize()` is called or when `identifyCustomer()` is called without providing a token.

#### SdkAuthCallback

Register an `SdkAuthCallback` to be notified of authentication failures and to provide fresh tokens:

```kotlin
Exponea.sdkAuthCallback = object : SdkAuthCallback {
    override fun onAuthFailure(error: SdkAuthError) {
        val newToken = fetchNewTokenFromYourBackend()
        Exponea.setSdkAuthToken(newToken)
    }
}
```

The `SdkAuthError` object contains:

| Name        | Type                 | Description                                                                              |
|-------------|----------------------|------------------------------------------------------------------------------------------|
| errorCode   | SdkAuthErrorCode     | One of `TOKEN_ABOUT_TO_EXPIRE`, `TOKEN_EXPIRED`, `TOKEN_REJECTED`, `TOKEN_NOT_PROVIDED`. |
| customerIds | Map<String, String?> | The current customer IDs                                                                 |

> ❗️
>
> The `SdkAuthCallback` is invoked on a background thread. If you need to perform asynchronous work (such as fetching a new token from your backend), you can block the thread while waiting for the result. The SDK will re-read the token from its repository after the callback returns.

> ❗️
>
> The SDK auth token is assigned to the current customer IDs. If customer IDs change (due to invoking `identifyCustomer` or `anonymize`), the token is cleared. Make sure to provide a new token for the new customer.

## Configure application ID

**Multiple mobile apps:** If your Engagement project supports multiple mobile apps, specify the `applicationId` in your configuration. This helps distinguish between different apps in your project.

```kotlin
configuration.applicationId = "<Your application id>" 
```
Make sure your `applicationId` value matches exactly Application ID configured in your Bloomreach Engagement under **Project Settings > Campaigns > Channels > Push Notifications.**

**Single mobile app:** If your Engagement project supports only one app, you can skip the `applicationId` configuration. The SDK will automatically use the default value "default-application".
