

## Exponea SDK - Sample App
<p align="center">
  <img src="./logo.jpg?raw=true" alt="Exponea"/>
</p>

# Introduction

Goal of this app is to briefly showcase what you can do with Exponea SDK and how to implement it



# Initialize
The configuration object must be configured before starting using the SDK.

Sample app uses `AuthenticationActivity` to init SDK, if you launch it, you will see familiar Login screen with several Input fields

Let's get started by creating configuration object ( this can also be done by  applying valid configuration file)
```
val configuration = ExponeaConfiguration()
```

Next we should provide our sdk with credentials
```
configuration.authorization = authorizationToken
configuration.projectToken = projectToken
```

Time to Initialize our sdk by providing an application context
```
Exponea.init(App.instance, configuration)
```

Additional set ups can be made, such as configuring logger level, tweaking flush mode/period etc.
```
Exponea.init(App.instance, configuration)
// Set our debug level to debug
Exponea.loggerLevel = Logger.Level.DEBUG
// Set up our flushing
Exponea.flushMode = FlushMode.PERIOD
Exponea.flushPeriod = FlushPeriod(1, TimeUnit.MINUTES)
```

After SDK has been configured, new activity will launch

# Application

`MainActivity` will launch shortly after configuration is completed. The activity itself doesn't do much except in-app navigation. It's the <b>fragments</b> inside of activity that are important. There are 3 <b>fragment</b>  for each navigation button, each of them is derived from `BaseFragment`.

`BaseFragment` contains only one method, that allows us to track every screen user has navigated to
```
Exponea.trackCustomerEvent(
        eventType =  "page_view",
        customerIds = CustomerIds(cookie = userID),
        properties = properties,
        timestamp = null
)
```
So each time fragment get created (i.e user navigates to it), we can track it using `fun trackPage(pageName: String)` method, where name of the screen(`pageName`) is the only parameter.
