
## 🔍 Tracking
Exponea SDK allows you to track events while using the app and add your customer’s properties. When SDK is first initialized, we generate a cookie for the customer that will be used for all the tracking. You can retrieve that cookie using `Exponea.customerCookie`.

> If you need to reset the tracking and start fresh with a new user, you can use [Anonymize](./ANONYMIZE.md) functionality.

## 🔍 Tracking Events
> Some events are tracked automatically. We track installation events once for every customer, and when `automaticSessionTracking` is enabled in [ExponeaConfiguration](./CONFIG.md) we automatically track session events.

You can define any event type for each of your projects based on your business model or current goals. If you have a product e-commerce website, your primary customer journey will probably/most likely be:

* Visiting your App
* Searching for a specific product
* Product page
* Adding product to the cart
* Going through the ordering process
* Payment

So the possible events for tracking will be: ‘search’, ‘product view’, ‘add product to cart’, ‘checkout’, ‘purchase’. Remember that you can define any event names you wish. Our recommendation is to make them self-descriptive and human-understandable.

In the SDK, you can track an event using the following accessor:

```
fun trackEvent(
        properties: PropertiesList,
        timestamp: Double?,
        eventType: String?
)
```

#### 💻 Usage

```
// Preparing the data.
val properties = PropertiesList(hashMapOf(Pair("name", "John")))

// Call trackEvent to send the event to Exponea API.
Exponea.trackEvent(
        properties = properties,
        timestamp = currentTimeSeconds(),
        eventType =  "page_view"
)
```

## 🔍 Default Properties

It’s possible to set values in the [ExponeaConfiguration](../Documentation/CONFIG.md) to be sent in every tracking event. Notice that those values will be overwritten if the tracking event has properties with the same key name.

#### 💻 Usage

```
// Create a new ExponeaConfiguration instance
val configuration = ExponeaConfiguration()
configuration.defaultProperties["thisIsADefaultStringProperty"] = "This is a default string value"
configuration.defaultProperties["thisIsADefaultIntProperty"] = 1

// Start the SDK
Exponea.init(App.instance, configuration)
```

> Once Exponea is configured, you can also change default properties setting `Exponea.defaultProperties`.

## 🔍 Tracking Customer Properties

#### identify Customer

Save or update your customer data in the Exponea APP through this method.

```
fun identifyCustomer(
        customerIds: CustomerIds,
        properties: PropertiesList
)
```

#### 💻 Usage

```
// Preparing the data.
val customerIds = CustomerIds().withId("registered","donald@exponea.com")
val properties = PropertiesList(hashMapOf(Pair("name", "John")))

// Call identifyCustomer to send the event to Exponea API.
Exponea.identifyCustomer(
        customerIds = customerIds,
        properties = properties
)
```

### 🧳 Storing the events

When events are tracked and for any reason can not be sent to the server immediately (no connection, server maintenance, sending error), they stay persisted in a local database. In case of reasons like no connection or server downtime, they are never deleted, and SDK is trying to send them until the request is successful. In case send is unsuccessful for another reason, SDK counts sending tries, and if a number of tries exceed the maximum, the event is deleted. The maximum of tries is set to 10 by default, but you can set this value by setting the `maxTries` field on the `ExponeaConfiguration` object when configuring the SDK. 
