## 🕵 Anonymize

Anonymize is a feature that allows you to switch users. Typical use-case is user login/logout.

Anonymize will delete all stored information and reset the current customer. New customer will be generated, install and session start events tracked. Push notification token from the old user will be wiped and tracked for the new user, to make sure the device won't get duplicate push notifications.

#### 💻 Usage

``` kotlin
Exponea.anonymize()
```

> Keep in mind that invoking of `anonymize` will remove also a Push notification token from storage. To load a current token, your application should retrieve a valid token manually before using any Push notification feature. So it may be called right after `anonymize` or before/after `identifyCustomer`, it depends on your Push notifications usage.
> Guide how to retrieve a valid Push notification token is written for [FCM](../Guides/PUSH_QUICKSTART_FIREBASE.md) and [HMS](../Guides/PUSH_QUICKSTART_HUAWEI.md).

### Project settings switch
Anonymize also allows you to switch to a different project, keeping the benefits described above. New user will have the same events as if the app was installed on a new device.

#### 💻 Usage

``` kotlin
Exponea.anonymize(
    exponeaProject = ExponeaProject(
        baseUrl= "https://api.exponea.com",
        projectToken= "project-token",
        authorization= "Token your-auth-token"
    ),
    projectRouteMap = mapOf(
        EventType.TRACK_EVENT to listOf(
            ExponeaProject(
                baseUrl= "https://api.exponea.com",
                projectToken= "project-token",
                authorization= "Token your-auth-token"
            )
        )
    ),
    advancedAuthToken = "token123"
)
```

