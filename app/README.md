

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

# Main Screen

`MainActivity` will launch shortly after configuration is completed. The activity itself doesn't do much except in-app navigation. It's the <b>fragments</b> inside of activity that are important. All the fragments are derived from the `BaseFragment`

## Screen Tracking
`BaseFragment` contains only one method, it allows us to track every screen user has navigated to
```
Exponea.trackEvent(
        eventType =  "page_view",
        properties = properties,
        timestamp = null
)
```
So each time fragment get created (i.e user navigates to it), we can track it using `fun trackPage(pageName: String)` method, where name of the screen(`pageName`) is the only parameter. Perfect example for tracking customer events specific for your application!

## Fetch, Track, Flush

`MainActivity` allow user to navigate between 3 fragments:`FetchFragment`, `TrackFragment` and `FlushFragment`. Goal of each fragment is to showcase different aspects of SDK:
- Data fetching
- Common events Tracking
- Manual flushing

### FetchFragment

This fragment contains three buttons and the output window below them.
Every button click will call specific fetch method from the SDK. Let's take a look at **Attributes** button.

Here we specify `onClickListener` as usual
```
attributesButton.setOnClickListener {
           fetchCustomerAttributes()
       }
```
Now the `fetchCustomerAttributes()` method!
```

FetchCustomAttributeDialog.show(childFragmentManager, {
            setProgressBarVisible(true)
            Exponea.fetchCustomerAttributes(
                    customerAttributes = it,
                    onFailure = { onFetchFailed(it) },
                    onSuccess = {onFetchSuccess(it)}
            )
        })
    }

    ```
Clicking the button will cause the dialog to pop up, where user will be able to specify specific attribute to fetch
In the end it will construct `CustomerAttributes` and send it via callback like so:
```
attributes.apply {
            withProperty("first_name")
            withProperty("email")
        }

onFetch(customerAttributes)

```
And finally, we will call `Exponea.fetchCustomerAttributes()` method from the SDK with specified **attributes** and **callbacks**
```
Exponea.fetchCustomerAttributes(
               customerAttributes = attributes,
               onFailure = { onFetchFailed(it) },
               onSuccess = {onFetchSuccess(it)}
       )

```
Our callbacks are pretty simple. We just take whatever we got from the server and put it's string representation to the TextView bellow our buttons
```
private fun onFetchSuccess(result: Result<List<CustomerAttributeModel>>) {
       runOnUiThread {
            setProgressBarVisible(false)
            resultTextView.text = result.toString()
        }
    }
```
> Note that both `onFailure` callback and `onSuccess` callback will be called on the separate thread. So in this example we used `Handler` to post changes on the Main Thread and  update UI accordingly.

### TrackFragment

This fragment consist of `ListView`, several buttons and contains different tracking examples for different events.

##### Payments tracking
`ListView` represents list of items that can be purchased by customer. Each item click will result in calling `trackPayment()` method. This method is simply  constructing `PurchasedItem` object and sends it to according SDK method
```
val purchasedItem = PurchasedItem(
                value = 2011.1,
                currency = "USD",
                paymentSystem = "System",
                productId = id.toString(),
                productTitle = mockItems()[position]
        )
        Exponea.trackPaymentEvent(
                customerIds = customerIds,
                purchasedItem = purchasedItem)
```

##### Firebase Cloud Messaging Notifications

Next three buttons are here showcase push notifications tracking, i.e notification delivered, clicked etc.

```
private fun trackPushDelivered() {
        val customerIds = CustomerIds(cookie = App.instance.userIdManager.uniqueUserID)
        Exponea.trackDeliveredPush(
                fcmToken = "Fcm Token"
        )
    }
```
##### Acting upon notification Clicking
> Thats applies only if automaticPushNotification is set to `true`

If you want specify action to be completed when user opens push notification. You'll have to register your own `Broadcast Receiver` like so
```
        <receiver
            android:name=".services.MyReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter><action android:name="com.exponea.sdk.action.PUSH_CLICKED"/> </intent-filter>
        </receiver>
  ```

The last but not least: `onReceiveMethod`

  ```
  override fun onReceive(context: Context, intent: Intent) {
    when(intent.action) {
        ExponeaPushReceiver.ACTION_CLICKED -> {

                // Extract payload data
                val data = intent.getParcelableExtra<NotificationData>(ExponeaPushReceiver.EXTRA_DATA)
                Log.i("Receiver", "Payload: $data")

                // Act upon push receiving
                context.startActivity(Intent(context, MainActivity::class.java))
            }

  ```





##### identifyCustomer
Final one allows you to update customer properties.
It will also show a dialog, where you can specify attributes you want save or update
```
val props = PropertiesList(hashMapOf("first_name" to "newName", "email" to "another@email.com"))
      Exponea.updateCustomerProperties(
              properties = props
      )
```

### FlushFragment

This one is pretty simple. Just one button. All the events that we've tracked so far along the way, are waiting for a moment when they will be sent to the Exponea API. This **Flush** button does nothing but makes it happen right here right now.
```
settingsBtnFlush.setOnClickListener {
            Exponea.flush()
        }
```
