## In-app messages
Exponea SDK allows you to display native in-app messages based on definitions set up on the Exponea web application. You can find information on creating your messages in [Exponea documentation](https://docs.exponea.com/docs/in-app-messages).

No developer work is required for in-app messages; they work automatically after the SDK is initialized.

### Troubleshooting
As with everything that's supposed to work automatically, the biggest problem is what to do when it doesn't. 

### Logging
The SDK logs a lot of useful information about presenting in-app messages on the default `INFO` level. To see why each message was/wasn't displayed, make sure your logger level is most `INFO`. You can set the logger level using `Exponea.loggerLevel = Logger.Level.DEBUG` before initializing the SDK.

### Example logs

Let's look at an example of how the logs may look when displaying an in-app message.
1. ```
    In-app message data preloaded, picking a message to display
    ```
    In-app message definitions must be preloaded in order to display the message. If the preload is still in progress, we store the events until preload is complete and perform the message picking logic afterward.

2. ```
    Picking in-app message for eventType payment. 2 messages available: [App load in-app message, Payment in-app message].
    ```
    This log contains the list of **all** message names we received from the server. If you don't see your message here, double-check the setup on the Exponea web application. Make sure your targeting includes the current customer.

3.  ```
    Message 'App load in-app message' failed event filter. Message filter: {"event_type":"session_start","filter":[]} Event type: payment properties: {price=2011.1, product_title=Item #1} timestamp: 1.59921557821E9
    ```
    We show reasons why some messages are not picked. In this example, message failed event filter - the type was set for `session_start`, but `payment` was tracked.
4. ```
    1 messages available after filtering. Picking the highest priority message.
    ```
    After applying all the filters, we have one message left. You can set priority on your messages. The highest priority message should be displayed.
5. ```
    Got 1 messages with highest priority. [Payment in-app message]
    ```
    There may be a tie between a few messages with the same priority. In that case, we pick one at random.
6. ```
    Attempting to show in-app message 'Payment in-app message'
    ```
    The message picked for displaying was `Payment in-app message`
8. ```
    Posting show to the main thread with delay 0ms.
    ```
    Message display request is posted to the main thread, where it will be displayed in the last resumed Activity. 
9. ```
    Attempting to present in-app message.
    ```
    Called from the main thread, if a failure happens after this point, please check next section about `Displaying in-app messages`.
10. ```
    In-app message presented.
    ```
    Everything went well, and you should see your message. It was presented in the current Activity. In case you don't see the message, it's possible that the view hierarchy has changed, and the message is no longer on screen.

### Displaying in-app messages
In-app messages are triggered when an event is tracked based on conditions setup on the Exponea backend. Once a message passes those filters, the SDK will try to present the message. 

The SDK hooks into the application lifecycle, and every time an activity is resumed, it will remember it and use it for presenting in-app message. In-app messages are displayed in a new Activity that is started for them (except for slide-in message that is directly injected into the currently running Activity).

> It's essential that the SDK knows of the current Activity. Ideally, initialize the SDK before `onResume` of your main Activity so that the lifecycle hook will be called once the Activity is resumed. If it's not possible in your case, you can call `init()` with your current Activity as the `context` parameter. If your Activity extends `AppCompatActivity` and is already resumed, it will be used as a host for in-app messages.

If your application decides to present new Activity at the same time as the in-app message is being presented, a race condition is created. The message may be displayed but immediately covered by the newly started Activity. Keep this in mind if the logs tell you your message was displayed, but you don't see it.

> Show on `App load` displays in-app message when a `session_start` event is tracked. If you close and quickly reopen the app, it's possible that the session did not timeout, and the message won't be displayed. If you use manual session tracking, the message won't be displayed unless you track `session_start` event yourself.

### In-app images caching
To reduce the number of API calls and fetching time of in-app messages, SDK is caching the images displayed in messages. Therefore, once the SDK downloads the image, an image with the same URL may not be downloaded again, and will not change, since it was already cached. For this reason, we recommend always using different URLs for different images.


### Custom in-app message actions
If you want to override default SDK behavior, when in-app message action is performed (button is clicked, message is closed), or you want to add your code to be performed along with code executed by the SDK, you can set up `inAppMessageActionCallback` on Exponea instance.

```kotlin
Exponea.inAppMessageActionCallback = object : InAppMessageCallback {  
    // If overrideDefaultBehavior is set to true, default in-app action will not be performed ( e.g. deep link )
    override var overrideDefaultBehavior = true  
    // If trackActions is set to false, click and close in-app events will not be tracked automatically
    override var trackActions = false  
  
    override fun inAppMessageAction(  
        messageId: String,  
        button: InAppMessageButton?,  
        interaction: Boolean,  
        context: Context  
    ) {  
        // Here goes your code
        // On in-app click, the button contains button text and button URL and the interaction is true
        // On in-app close, the button is null, and the interaction is false.
    }  
}

```