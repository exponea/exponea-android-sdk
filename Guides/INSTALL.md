
## Installation

1. Open `build.gradle` file located in your project folder
2. Add ExponeaSDK dependency and sync your project
```groovy
dependencies {
    compile 'com.exponea.sdk:sdk:1.1.7'
}
```
3. After synchronisation is complete, you can start using SDK


## Configuration
 In order to use ExponeaSDK you have to initize and configure it first


You can configure you Exponea instance either in code or using
.json configuarion file.
Minimal configuarion requires you to provide `Authorization Token`, `Project Token` and `Base URL`
You can find these parameteres in `Exponea console`.

> [How do I get these parameters?](./CONFIGURATION.md)


##### Using Code
```
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
You can also use .json file in order to provide your configuration. The SDK searches for a file called
`exponea_configuration.json` that must be inside the "assets" folder of your application
```
{
  "projectToken": "place your project token here",
  "authorization": "place your authorization token here",
  "baseURL": "place base url here"
}
```

> [Check out our example app to learn how configuration file could look like](../app/src/main/assets/exponea_configuration.json)

> [Learn more about how you can configure ExponeaSDK](../Documentation/CONFIG.md)


After configuration is taken care of, it is time to initize ExponeaSDK. Best place to initize it will be in you Application `onCreate()` method as requires your's application `Context`. Application is a the class for maintaining global application state. It's usually looks similar to this ( along with our SDK initilization code ). Heres a basic SDK initization example

```
 class MyApplication : Application() {
   override fun onCreate(){
     super.onCreate()

     // TODO Your implementation here
     val configuration = ExponeaConfiguration()

     configuration.authorization = "Token jlk5askvxss99asmnbgayrks333"
     configuration.projectToken = "47b5cc2c-e661-11e8-bb95-0a580a201692"
     configuration.baseURL = "https://api.exponea.com"

     // Sdk initization
      Exponea.init(this, configuration)
   }
}
```
And it also must be registered `AndroidManafifest.xml` like so:
```xml
<application
       android:name=".MyApplication">
       ...
</application>
 ```

If you are using configuration from file you code above could be simplified
```
override fun onCreate(){
    super.onCreate()    
    Exponea.initFromFile(this)
}
```

> You can also initize Exponea from any `Fragment` or `Activity`, just remember to pass `Context` of the `Application`

 ```
  // In Fragment
   Exponea.init(contex.applicationContext, configuration)

   // In Activity
   Exponea.init(applicationContext, configuration)
```

That's it! Your are now able to use Exponea in your application!
