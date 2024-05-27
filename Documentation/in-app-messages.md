---
title: In-App Messages
excerpt: Display native in-app messages based on definitions set up in Engagement using the Android SDK
slug: android-sdk-in-app-messages
categorySlug: integrations
parentDocSlug: android-sdk-in-app-personalization
---

The SDK enables you to display native in-app messages in your app based on definitions set up in Engagement. 

In-app messages work out-of-the-box once the [SDK is installed and configured](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup) in your app; no development work is required. However, you can customize the behavior to meet your specific requirements.

> 📘
>
> Refer to the [In-App Messages](https://documentation.bloomreach.com/engagement/docs/in-app-messages) user guide for instructions on how to create in-app messages in the Engagement web app.

## Tracking

The SDK automatically tracks `banner` events for in-app messages with the following values for the `action` event property:

- `show`
  In-app message displayed to user.
- `click`
  User clicked on action button inside in-app message. The event also contains the corresponding `text` and `link` properties.
- `close`
  User clicked on close button inside in-app message.
- `error`
  Displaying in-app message failed. The event contains an `error` property with an error message.

> ❗️
>
> The behavior of in-app message tracking may be affected by the tracking consent feature, which in enabled mode requires explicit consent for tracking. Refer to the [consent documentation](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking-consent) documentation for details.


## Customization

### Customize In-App Message Actions

You can override the SDK's default behavior when an in-app message action (click button or close message) is performed by setting `inAppMessageActionCallback` on the `Exponea` instance.

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

    override fun inAppMessageShown(message: InAppMessage, context: Context) {
        // Here goes your code
        // Method called when in-app message is shown.
    }

    override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {
        // Here goes your code
        // Method called when any error occurs while showing in-app message.
        // In-app message could be NULL if error is not related to in-app message.
    }
}

```

If you set `trackActions` to **false** but you still want to track click and close events under some circumstances, you can use the methods `trackInAppMessageClick` or `trackInAppMessageClose` in the action method:

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

The `trackInAppMessageClose` method will track a 'close' event with the 'interaction' property set to `true` by default. You can use the method's optional parameter `interaction` to override this value.

> ❗️
>
> The behaviour of `trackInAppMessageClick` and `trackInAppMessageClose` may be affected by the tracking consent feature, which in enabled mode requires explicit consent for tracking. Refer to the [Tracking Consent](https://documentation.bloomreach.com/engagement/docs/android-sdk-tracking-consent) documentation for details.

## Troubleshooting

This section provides helpful pointers for troubleshooting in-app message issues.

> 👍 Set INFO or VERBOSE Log Level
> The SDK logs a lot of information in at `INFO` level while loading in-app messages. When troubleshooting in-app message issues, first ensure to [set the SDK's log level](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup#log-level) at least to `INFO`.
>
> If you find that `INFO` log messages are not sufficient for a particular use case, try setting the log level to `VERBOSE` to get more detailed information.

> 👍
>
> All log messages related to in-app message handling are prefixed with `[InApp]` to make them easier to find. Bear in mind that some supporting processes (such as image caching) are logging messages without this prefix.

### In-App Message Not Displayed

When troubleshooting why an in-app message did not display on your device, always first make sure that the in-app message was preloaded to the device, then troubleshoot message display.

#### Troubleshoot In-App Messages Preloading Issues

- The SDK requests in-app messages from the Engagement platform any time one of the following occurs:
  - `Exponea.identifyCustomer` is called
  - `Exponea.anonymize` is called
  - Any event (except push notification clicked or opened, or session ends) is tracked **and** the in-app messages cache is older then 30 minutes
- The SDK should subsequently receive a response from the Engagement platform containing all available in-app messages targeted at the current customer. The SDK preload these messages in a local cache.
- If you create or modify an in-app message in Engagement, typically any changes you made are reflected in the SDK after 30 minutes due to the in-app messages being cached. Call `Exponea.identifyCustomer` or `Exponea.anonymize` to trigger reloading so changes are reflected immediately.
- Analyze the [log messages](#log-messages) (especially examples 2-5) to determine whether the SDK is requesting and receiving in-app messages and your message was preloaded.
- If the SDK is requesting and receiving in-app messages but your message is not preloaded:
  - The local cache may be outdated. Wait for or trigger the next preload.
  - The current customer may not match the audience targeted by the in-app message. Verify the message's audience in Engagement.

> ❗️
>
> Invoking `Exponea.anonymize` triggers fetching in-app messages immediately but `Exponea.identifyCustomer` needs to be flushed to the backend successfully first. This is because the backend must know the customer so it can assign the in-app messages with matching audience. If you have set `Exponea.flushMode` to anything other then `FlushMode.IMMEDIATE`, you must call `Exponea.flushData()` to finalize the `identifyCustomer` process and trigger an in-app messages fetch.

#### Troubleshoot In-App Message Display Issues

If your app is successfully requesting and receiving in-app messages but they are not displayed, consider the following:

- In-app messages are triggered when an event is tracked based on conditions set up in Engagement. Once a message passes those filters, the SDK will try to present the message.

- The SDK hooks into the application lifecycle, and every time an activity is resumed, the SDK will use it for presenting the in-app message. In-app messages are displayed in a new Activity that is started for them (except for slide-in messages which are directly injected into the currently running Activity).

- It's possible that your application decides to present another Activity right at the same time, creating a race condition. In this case, the message might be displayed and immediately dismissed because its parent leaves the screen. Keep this in mind if the [logs](#log-messages) tell you your message was displayed but you don't see it.
  
- It's essential that the SDK is aware of the current Activity. Ideally, initialize the SDK before `onResume` of your main Activity so that the lifecycle hook will be called once the Activity is resumed. If that's not possible, you can call `init()` with your current Activity as the `context` parameter. If your Activity extends `AppCompatActivity` and is already resumed, it will be used as a host for in-app messages.  

- In-app messages configured to show on `App load` are displayed when a `session_start` event is tracked. If you close and quickly reopen the app, it's possible that the session did not time out and the message won't be displayed. If you use manual session tracking, the message won't be displayed unless you track a `session_start` event yourself.

- An in-app message can only be displayed if it is loaded, including its images. If the message is not yet fully loaded, the SDK registers a request-to-show for that message so it will be displayed once it is fully loaded. The request-to-show has a timeout of 3 seconds. This means that in case of unpredicted behavior, such as image loading taking too long, the message may not be displayed directly.

- If in-app message loading hits the timeout of 3 seconds, the message will be displayed the next time its trigger event is tracked. For example, if a `session_start` event triggers an in-app message but loading that message times out, it will not be displayed directly. However, once loaded, it will display the next time a `session_start` event is tracked.

- Image downloads are limited to 10 seconds per image. If an in-app message contains a large image that cannot be downloaded within this time limit, the in-app message will not be displayed. For an HTML in-app message that contains multiple images, this restriction applies per image, but failure of any image download will prevent this HTML in-app message from being displayed.

### In-App Message Shows Incorrect Image

- To reduce the number of API calls and fetching time of in-app messages, the SDK caches the images contained in messages. Once the SDK downloads an image, an image with the same URL may not be downloaded again. If a message contains a new image with the same URL as a previously used image, the previous image is displayed since it was already cached. For this reason, we recommend always using different URLs for different images.

### In-App Message Actions Not Tracked

- If you have implemented a custom `inAppMessageActionCallback`, actions are only tracked automatically if `trackActions` is set to `true`. If `trackActions` is set to `false`, you must manually track the action in the `inAppMessageAction` method. Refer to [Customize In-App Message Actions](#customize-in-app-message-actions) above for details.

### Log Messages

While troubleshooting in-app message issues, you can follow the process of requesting, receiving, preloading, and displaying in-app messages through the information logged by the SDK at verbose log level. Look for messages similar to the ones below:


Let's look at an example of how the logs may look when displaying an in-app message.
1. ```
   Event {eventCategory}:{eventType} occurred, going to trigger In-app show process
   ```
   The in-app message process was triggered by SDK usage of the `identifyCustomer()` method or the tracking of an event.
2. ```
   Register request for in-app message to be shown for $eventType
   ```
   For each non-null event of type `eventType`, the SDK registers a 'show request' to display an in-app message once the process finishes.
3. ```
   --> POST https://api.exponea.com/webxp/s/2c4f2d02-1dbe-11eb-844d-2a3b671acf41/inappmessages?v=1
   ```  
   The SDK requested in-app messages from the Engagement platform.
4. ```
   <-- 200 https://api.exponea.com/webxp/s/2c4f2d02-1dbe-11eb-844d-2a3b671acf41/inappmessages?v=1 (2293ms)
   ```
   The SDK received in-app messages from the Engagement platform. You should see the in-app messages data in JSON format a few lines below the above message:
   ```
   {"success":true,"data":[{"id":"65baf899dd467ee54357e371","name":"Payment in-app message","date_filter":{"enabled":false},"frequency":"until_visitor_interacts","load_priority":null,"load_delay":null,"close_timeout":null,"trigger":{"event_type": "payment", "filter": [], "type": "event"},"has_tracking_consent":true,"message_type":"slide_in","variant_id":0,"variant_name":"Variant A","is_html":false,"payload":{"title":"Payment In-App Message","body_text":"This is an example of your in-app personalization body text.","image_url":"https://asset-templates.exponea.com/misc/media/canyon/canyon.jpg","title_text_color":"#000000","title_text_size":"22px","body_text_color":"#000000","body_text_size":"14px","background_color":"#ffffff","message_position":"top","buttons":[{"button_text":"Action","button_type":"deep-link","button_link":"https://www.bloomreach.com","button_text_color":"#2dbaee","button_background_color":"#ffffff"}]}}]}
   ```
5. ```
   [In-app message data preloaded. ]Picking a message to display
   ```
   In-app messages were preloaded in the local cache. The SDK will proceed with the logic to select an in-app message to display.
6. ```
   Picking in-app message for eventType {eventType}. {X} messages available: [{message1 name}, {message2 name}, ...].
   ```
   This log message includes a list of **all** in-app messages received from the server and preloaded in the local cache. If you don't see your message here, it may not have been available yet the last time the SDK requested in-app messages. If you have confirmed the message was available when the last preload occurred, the current user may not match the audience targeted by the in-app message. Check the in-app message set up in Engagement. 
7. ```
   Message '{message name}' failed event filter. Message filter: {"event_type":"session_start","filter":[]} Event type: payment properties: {price=2011.1, product_title=Item #1} timestamp: 1.59921557821E9
   ```
   The SDK tells you why it didn't select this particular message. In this example, the message failed the event filter - the event type was set for `payment`, but `session_start` was tracked.
8. ```
   {X} messages available after filtering. Going to pick the highest priority messages.
   ```
   After applying all the filters, there are `X` in-app messages left that satisfy the criteria to be displayed to the current user. The filters are determined by the in-app messages' settings in Engagement, including `Schedule`, `Show on` and `Display`.
9. ```
   Got {X} messages with highest priority for eventType {eventType}. [{message1 name}, {message2 name}, ...]
   ```
   There may be multiple messages with the same priority. All messages with the highest priority are listed.
10. ```
    Picking top message '{message name}' for eventType {eventType}
    ```
    If more than one messages is eligible, the SDK will select the one that has the highest priority for `eventType`.
11. ```
    Got {X} messages available to show. [{message1 name}, {message2 name}, ...].
    ```
    The SDK has collected all `X` messages eligible to be displayed. The process to select a single message to display continues.
12. ```
    Picking top message '{message name}' to be shown.
    ```
    The SDK selects the in-app message with the highest priority configured in Engagement. If there are multiple messages with the highest priority, the SDK randomly selects a single  message from the candidates. This message will be displayed to user.
13. ```
    Only logging in-app message for control group '${message.name}'
    ```
    An A/B testing variant of an in-app message or a message without payload is not displayed to the user but the SDK tracks a 'show' event for analysis.
14. ```
    Attempting to show in-app message '{message name}'
    ```
    An in-app message has been selected to be displayed in the app (no A/B testing).
15. ```
    Posting show to main thread with delay {X}ms.
    ```
    A message display request is posted to the main thread with a delay of `X` milliseconds. The delay can be configured using the `Display delay` field in the in-app message's settings in Engagement. The message will be displayed in the last resumed Activity. 
16. ```
    Attempting to present in-app message.
    ```
    Logged from the main thread. If a failure happens after this point, refer to [Troubleshoot In-App Message Display Issues](#troubleshoot-in-app-message-display-issues) above for pointers.
17. ```
    In-app message presented.
    ```
    Everything went well and you should see your message. It was presented in the current Activity. In case you don't see the message, it's possible that the view hierarchy changed and the message is no longer on screen. Refer to [Troubleshoot In-App Message Display Issues](#troubleshoot-in-app-message-display-issues) above for details.

