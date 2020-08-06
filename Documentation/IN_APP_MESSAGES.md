## In-app messages
Exponea SDK allows you to display native in-app messages based on definitions set up on Exponea web application. You can find information on how to create your messages in [Exponea documentation](https://docs.exponea.com/docs/in-app-messages).

No developer work is required for in-app messages, they work automatically after the SDK is initialized.

### Troubleshooting
As with everything that's supposed works automatically, the biggest problem is what to do when it doesn't. 

#### Logging
The SDK logs a lot of useful information about presenting in-app messages on the default `INFO` level. To see why each individual message was/wasn't displayed, make sure your logger level is at most `INFO`. You can set the logger level using `Exponea.loggerLevel = Logger.Level.DEBUG` before initializing the SDK.

#### Displaying in-app messages
In-app messages are triggered when an event is tracked based on conditions setup on Exponea backend. Once a message passes those filters, the SDK will try to present the message. 

The SDK hooks into application lifecycle and every time an activity is resumed, it will remember it and use it for presenting in-app message. In-app messages are displayed in a new Activity that is started for them (except for slide-in message that is directly injected into currently running Activity).

> It's important that the SDK knows of the current activity. Ideally, initialize the SDK before `onResume` of your main activity, so that lifecycle hook will be called once the Activity is resumed. If it's not possible in your case, you can call `init()` with your current activity as the `context` parameter. If your activity extends `AppCompatActivity` and is already resumed, it will be used as a host for in-app messages.

If your application decides to present new Activity at the same time as in-app message is being presented a race condition is created. The message may be displayed, but immediately covered by newly started Activity. Keep this in mind if the logs tell you your message was displayed but you don't see it.

> Show on `App load` displays in-app message when a `session_start` event is tracked. If you close and quickly reopen the app, it's possible that the session did not timeout and message won't be displayed. If you use manual session tracking, the message won't be displayed unless you track `session_start` event yourself.