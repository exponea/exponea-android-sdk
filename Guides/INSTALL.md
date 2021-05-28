
## Installation

1. Open `build.gradle` file located in your project folder
2. Add ExponeaSDK dependency and sync your project
    ```groovy
    dependencies {
        implementation 'com.exponea.sdk:sdk:2.9.4'
    }
    ```
3. After synchronization is complete, you can start using the SDK.


## Configuration
 In order to use ExponeaSDK you have to initialize it with a configuration that connects the SDK to your web application and sets up individual SDK features. You can configure your Exponea instance either in code (preferred) or using *.json* configuration file.

Minimal configuration requires you to provide `Authorization Token`, `Project Token` and `Base URL`.
You can find these parameters in `Exponea web application`.

> [How do I get these parameters?](./CONFIGURATION.md)


##### Using Code
``` kotlin
// Init your exponea configuration
val configuration = ExponeaConfiguration()

// Set Authorization Token
configuration.authorization = authorizationToken

// Set Project Token
configuration.projectToken = projectToken

// Set Base URL
configuration.baseURL = apiUrl
```


##### Using Configuration file
The SDK searches for a file called `exponea_configuration.json` that must be inside the "assets" folder of your application
```
{
  "projectToken": "place your project token here",
  "authorization": "place your authorization token here",
  "baseURL": "place base url here"
}
```

> [Check out our example app to learn how configuration file could look like](../app/src/main/assets/exponea_configuration.json)

> [Learn more about how you can configure ExponeaSDK](../Documentation/CONFIG.md)

## Initializing

#### In an Application subclass
Once you have your configuration ready, it is time to initialize ExponeaSDK. Best place for initialization is in your Application `onCreate()` method - it's called only once and very early in the application lifecycle. Application is the class for maintaining global application state. It usually looks similar to this (along with our SDK initialization code).
``` kotlin
class MyApplication : Application() {
  override fun onCreate(){
    super.onCreate()

    // TODO Your implementation here
    val configuration = ExponeaConfiguration()

    configuration.authorization = "Token jlk5askvxss99asmnbgayrks333"
    configuration.projectToken = "47b5cc2c-e661-11e8-bb95-0a580a201692"
    configuration.baseURL = "https://api.exponea.com"

    // Sdk initialization
    Exponea.init(this, configuration)
    // or just Exponea.init(this) if using configuration file
  }
}
```

You'll need to register the custom application class in your `AndroidManifest.xml`:
```xml
<application
       android:name=".MyApplication">
       ...
</application>
 ```

#### In an Activity
You may decide that start of your application is not the best time to initialize Exponea SDK (e.g. for performance reasons). You can also initialize Exponea from any `Activity`, but it's important to do so as early as possible. Ideal place is your activity's `onCreate` method.

The SDK hooks into application lifecycle to e.g. track sessions, so we need to keep track of activities `onResume` callbacks. If you need to initialize the SDK after the activity has been resumed, do so with the `context` of the current activity.

## That's it!
Your are now able to use Exponea in your application!
