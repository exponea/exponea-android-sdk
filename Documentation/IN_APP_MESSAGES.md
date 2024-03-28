## In-app messages
Exponea SDK allows you to display native in-app messages based on definitions set up on the Exponea web application. You can find information on creating your messages in [Exponea documentation](https://docs.exponea.com/docs/in-app-messages).

No developer work is required for in-app messages; they work automatically after the SDK is initialized.

### Troubleshooting
As with everything that's supposed to work automatically, the biggest problem is what to do when it doesn't. 

### Logging
The SDK logs a lot of useful information about presenting in-app messages on the default `INFO` level. To see why each message was/wasn't displayed, make sure your logger level is most `INFO`. You can set the logger level using `Exponea.loggerLevel = Logger.Level.INFO` before initializing the SDK.
If you are facing any unexpected behaviour and `INFO` logs are not sufficient enough, try to set log level do `VERBOSE` to got more detailed information.

> Note: All logs assigned to In-app handling process are prefixed with `[InApp]` shortcut to bring easier search-ability to you. Bear in mind that some supporting processes (such as Image caching) are logging without this prefix. 
### Example logs

Let's look at an example of how the logs may look when displaying an in-app message.
1. ```
   Event {eventCategory}:{eventType} occurred, going to trigger In-app show process
   ```
   In-app process has been triggered by SDK usage of identifyCustomer() or event tracking.
1. ```
    Register request for in-app message to be shown for $eventType
    ```
   Each non-null `eventType` registers a 'show request' to show an In-app message after whole process is done.
1. ```
    [In-app message data preloaded. ]Picking a message to display
    ```
   In-app show process starts successfully and messages are prepared to be selected and shown. Log message `In-app message data preloaded.` means that In-app message definitions preload has to be done here.
2. ```
    Picking in-app message for eventType {eventType}. {X} messages available: [{message1 name}, {message2 name}, ...].
    ```
    This log contains `eventType` for which the messages going to be searched. Then count of `X` messages and the names of **all** messages received from the server is listed in log.
3.  ```
    Message '{message name}' failed event filter. Message filter: {"event_type":"session_start","filter":[]} Event type: payment properties: {price=2011.1, product_title=Item #1} timestamp: 1.59921557821E9
    ```
    We show reasons why some messages are not picked. In this example, message failed event filter - the type was set for `session_start`, but `payment` was tracked.
3. ```
   {X} messages available after filtering. Going to pick the highest priority messages.
   ```
   Log informs that `X` messages match all requirements given by own filter. Filter could be configured on Bloomreach engagement app for each message. See `Schedule`, `Show on` and `Display` in In-app message settings.
4. ```
   Got {X} messages with highest priority for eventType {eventType}. [{message1 name}, {message2 name}, ...]
   ```
   There may be a tie between a few messages with the same priority. All messages with same highest priority are listed.
6. ```
    Picking top message '{message name}' for eventType {eventType}
   ```
   The single message is randomly picked from filtered messages with same highest priority for `eventType`
7. ```
   Got {X} messages available to show. [{message1 name}, {message2 name}, ...].
   ```
    All `X` messages has been collected for registered 'show requests'. Process continues with selecting of message with highest priority.
8. ```
   Picking top message '{message name}' to be shown.
   ```
   The single message is randomly picked from all filtered messages. This message is going to be shown to user.
9. ```
   Only logging in-app message for control group '${message.name}'
   ```
   A/B testing In-app message or message without payload is not shown to user but 'show' event is tracked for your analysis.
6. ```
    Attempting to show in-app message '{message name}'
    ```
    In-app message that meant to be show to user (not A/B testing) is going to be shown
7. ```
    Posting show to main thread with delay {X}ms.
    ```
    Message display request is posted to the main thread with delay of `X` milliseconds. Delay is configured by `Display delay` in In-app message settings. Message will be displayed in the last resumed Activity. 
8. ```
    Attempting to present in-app message.
    ```
    Called from the main thread, if a failure happens after this point, please check next section about `Displaying in-app messages`.
9. ```
    In-app message presented.
    ```
    Everything went well, and you should see your message. It was presented in the current Activity. In case you don't see the message, it's possible that the view hierarchy has changed, and the message is no longer on screen.

### Displaying in-app messages
In-app messages are triggered when an event is tracked based on conditions setup on the Exponea backend. Once a message passes those filters, the SDK will try to present the message. 

The SDK hooks into the application lifecycle, and every time an activity is resumed, it will remember it and use it for presenting in-app message. In-app messages are displayed in a new Activity that is started for them (except for slide-in message that is directly injected into the currently running Activity).

> It's essential that the SDK knows of the current Activity. Ideally, initialize the SDK before `onResume` of your main Activity so that the lifecycle hook will be called once the Activity is resumed. If it's not possible in your case, you can call `init()` with your current Activity as the `context` parameter. If your Activity extends `AppCompatActivity` and is already resumed, it will be used as a host for in-app messages.

If your application decides to present new Activity at the same time as the in-app message is being presented, a race condition is created. The message may be displayed but immediately covered by the newly started Activity. Keep this in mind if the logs tell you your message was displayed, but you don't see it.

> Show on `App load` displays in-app message when a `session_start` event is tracked. If you close and quickly reopen the app, it's possible that the session did not timeout, and the message won't be displayed. If you use manual session tracking, the message won't be displayed unless you track `session_start` event yourself.

Message is able to be shown only if it is loaded and also its image is loaded too. In case that message is not yet fully loaded (including its image) then the request-to-show is registered in SDK for that message so SDK will show it after full load.
Due to prevention of unpredicted behaviour (i.e. image loading takes too long) that request-to-show has timeout of 3 seconds.

> If message loading hits timeout of 3 seconds then message will be shown on 'next request'. For example the 'session_start' event triggers a showing of message that needs to be fully loaded but it timeouts, then message will not be shown. But it will be ready for next `session_start` event so it will be shown on next 'application run'.

### In-app images caching
To reduce the number of API calls and fetching time of in-app messages, SDK is caching the images displayed in messages. Therefore, once the SDK downloads the image, an image with the same URL may not be downloaded again, and will not change, since it was already cached. For this reason, we recommend always using different URLs for different images.

### In-app messages loading
In-app messages reloading is triggered by any case of:
- when `Exponea.identifyCustomer` is called
- when `Exponea.anonymize` is called
- when any event is tracked (except Push clicked, opened or session ends) and In-app messages cache is older then 30 minutes from last load
Any In-app message images are preloaded too so message is able to be shown after whole process is finished. Please considers it while testing of In-app feature.
It is common behaviour that if you change an In-app message data on platform then this change is reflected in SDK after 30 minutes due to usage of messages cache. Do call `Exponea.identifyCustomer` or `Exponea.anonymize` if you want to reflect changes immediately.

> Note: Invoking of `Exponea.anonymize` does fetch In-apps immediately but `Exponea.identifyCustomer` needs to be sent to backend successfully. The reason is to register customer IDs on backend properly to correctly assign an In-app messages. If you have set other then `Exponea.flushMode = FlushMode.IMMEDIATE` you need to call `Exponea.flushData()` to finalize `identifyCustomer` process and trigger a In-app messages fetch.

### In-app messages tracking

In-app messages are tracked automatically by SDK. You may see these `action` values in customers tracked events:

- 'show' - event is tracked if message has been shown to user
- 'click' - event is tracked if user clicked on action button inside message. Event contains 'text' and 'link' properties that you might be interested in
- 'close' - event is tracked if user clicked on button with close action inside message or message has been dismissed automatically by defined 'Closing timeout'
- 'error' - event is tracked if showing of message has failed. Event contains 'error' property with meaningful description

The behaviour of In-app messages tracking may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).
Tracking of 'show' and 'error' event is done by SDK and behaviour cannot be overridden. These events are tracked only if:

* Tracking consent feature is disabled
* Tracking consent feature is enabled and 'hasTrackingConsent' has 'true' value

### Custom in-app message actions
If you want to override default SDK behavior, when in-app message action is performed (button is clicked, a message is closed), or you want to add your code to be performed along with code executed by the SDK, you can set up `inAppMessageActionCallback` on Exponea instance.

```kotlin
Exponea.inAppMessageActionCallback = object : InAppMessageCallback {  
    // If overrideDefaultBehavior is set to true, default in-app action will not be performed ( e.g. deep link )
    override var overrideDefaultBehavior = true  
    // If trackActions is set to false, click and close in-app events will not be tracked automatically
    override var trackActions = false  
  
    override fun inAppMessageAction(  
        message: InAppMessage,
        button: InAppMessageButton?,  
        interaction: Boolean,  
        context: Context  
    ) {  
        // Here goes your code
        // On in-app click, the button contains button text and button URL, and the interaction is true
        // On in-app close by user interaction, the button is null and the interaction is true
        // On in-app close by non-user interaction (i.e. timeout), the button is null and the interaction is false
    }
}

```

If you set `trackActions` to **false** but you still want to track click/close event under some circumstances, you can call Exponea methods `trackInAppMessageClick` or `trackInAppMessageClose` in the action method:

```kotlin
override fun inAppMessageAction(  
        message: InAppMessage,  
        button: InAppMessageButton?,  
        interaction: Boolean,  
        context: Context  
    ) {    
        if (<your-special-condition>) {
            if (interaction) {  
                Exponea.trackInAppMessageClick(message, button?.text, button?.url)  
            } else {  
                Exponea.trackInAppMessageClose(message)  
            }
        }
    }  
```

Method `trackInAppMessageClose` will track a 'close' event with 'interaction' field of TRUE value by default. You are able to use a optional parameter 'interaction' of this method to override this value.

> The behaviour of `trackInAppMessageClick` and `trackInAppMessageClose` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).
