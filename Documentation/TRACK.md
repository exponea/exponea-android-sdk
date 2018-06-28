## 🔍 Track Events

You can define any event types for each of your project based on your business model or your current goals. If you have product e-commerce website, your basic customer journey will probably/most likely be:

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
        timestamp: Long?,
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
        timestamp = Date().time
        eventType =  "page_view"
)
```

## 🔍 Customer Properties

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
