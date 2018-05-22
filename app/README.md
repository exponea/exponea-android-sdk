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
