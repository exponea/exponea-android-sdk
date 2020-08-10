# Track Events using ExponeaSDK
You can define custom event types for your projects based on your business model or your current goals. If you have a product e-commerce website, your basic customer journey will most likely be:

* Visiting your App
* Searching for specific product
* Product page
* Adding product to the cart
* Going through ordering process
* Payment

So the possible events for tracking will be: ‘search’, ‘product view’, ‘add product to cart’, ‘checkout’, ‘purchase’. Remember that you can define any event names you wish. Our recommendation is to make them self-descriptive and human understandable.

By default, `ExponeaSDK` will handle some of the events automatically, this includes:
* User Session tracking
* Push Notifications
* In-app messages lifecycle

Any other custom events can be tracked using `Exponea.trackEvent` method.

``` kotlin
fun trackEvent(
  properties: PropertiesList,
  timestamp: Double? = currentTimeSeconds(),
  eventType: String?
)
```

### Example usage
Let's say we'd like to track that the user visited a specific page in e.g. Activity's `onCreate()` method.

1. First we have to create `PropertiesList` object. It holds a `HashMap`
with properties you want to track with this event. In our example we want to track
name of the screen, so the  **screen name** could one of the properties we are sending
    ```kotlin
    val properties = PropertiesList(hashMapOf("name" to "Settings"))`
    ```

2. Then we can pass this object to `trackEvent()` along with **eventType** (page_view) as following

    ``` kotlin
    Exponea.trackEvent(
      eventType = "page_view",
      properties = properties
    )
    ```
3. You can then go to your customers overview, and see that event was successfully tracked

![](pics/events1.png)

# Track User Properties / User Identification
It is also possible to track/update properties of the current customer using `Exponea.identifyCustomer` method.
``` kotlin
fun identifyCustomer(
  customerIds: CustomerIds,
  properties: PropertiesList
)
```

### Example usage
1. To better identify your users you can create `CustomerIds` object. You can specify your custom Ids by providing `HashMap<String, String?>` or using `withId(key: String, value: String?)` method
    ``` kotlin
    val customerIds = CustomerIds().withId("registered", "Roman")
    ```

    > These ids are so-called `Hard Ids`. Read the difference between hard and soft ids in [Exponea Documentation](https://docs.exponea.com/docs/customer-identification)

2. You can also pass `PropertiesList` object with any other customer related properties, like customer name or payer status
    ``` kotlin
    val propertiesList = PropertiesList(
      hashMapOf("first_name" to "New Name", "paying_customer" to true)
    )
    ```

3. Pass these params to `identifyCustomer()` method
    ``` kotlin
    Exponea.identifyCustomer(
      customerIds = customerIds,
      properties = propertiesList
    )
    ```

4. You can then check identifiers and Properties in Customer overview

![](pics/events2.png)
