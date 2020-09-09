## 🔍 Tracking
Exponea SDK allows you to track events that occur while using the app and add properties of your customer. When SDK is first initialized we generate a cookie for the customer that will be used for all the tracking. You can retrieve that cookie using `Exponea.customerCookie`.

> If you need to reset the tracking and start fresh with a new user, you can use [Anonymize](./ANONYMIZE.md) functionality.

## 🔍 Tracking Events
> Some events are tracked automatically. We track installation event once for every customer and when `automaticSessionTracking` is enabled in [ExponeaConfiguration](./CONFIG.md) we automatically track session events.

You can define any event types for each of your projects based on your business model or your current goals. If you have product e-commerce website, your basic customer journey will probably/most likely be:

* Visiting your App
* Searching for specific product
* Product page
* Adding product to the cart
* Going through ordering process
* Payment

So the possible events for tracking will be: ‘search’, ‘product view’, ‘add product to cart’, ‘checkout’, ‘purchase’. Remember that you can define any event names you wish. Our recommendation is to make them self-descriptive and human understandable.

In the SDK you can track an event using the following accessor:

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

It's possible to set values in the [ExponeaConfiguration](../Documentation/CONFIG.md) to be sent in every tracking event. Notice that those values will be overwritten if the tracking event has properties with the same key name.

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
