## 🚀 Fetching Data

Exponea SDK has some methods to retrieve your data from the Exponea APP.
All the responses will be available in the `onSuccess` and `onFailure` call back properties.

#### Get customer attributes

It's possible to get all the customer attributes you have sent to the Exponea APP through the following method.


```
fun fetchCustomerAttributes(
        customerAttributes: CustomerAttributes,
        onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
        onFailure: (String) -> Unit
)
```

#### 💻 Usage

```
// Preparing the data.
val attributes = CustomerAttributes(
        customerId = CustomerIds(registered = "john@doe.com"),
        attributes = MutableList(hashMapOf(Pair("name", "John"))
)

// Call fetchCustomerAttributes to get the customer attributes.
Exponea.fetchCustomerAttributes(
        customerAttributes = attributes,
        onSuccess = {
			// On success, the SDK will return a list of a CustomerAttributeModel object.
        },
        onFailure = {
			// On failuire, SDK will return a String with the error.
        }
)
```

#### Get customer events

Export all the events for a specific customer.

```
fun fetchCustomerEvents(
        customerIds: CustomerIds,
        eventTypes: ArrayList<String> = arrayListOf("page_view"),
        order: String = "desc",
        limit: Int = 3,
        skip: Int = 100,
        onFailure: (String) -> Unit,
        onSuccess: (Result<ArrayList<CustomerEventModel>>) -> Unit
) 
```

#### 💻 Usage

```
// Preparing the data.
val attributes = CustomerAttributes(
        customerId = CustomerIds(registered = "john@doe.com"),
        attributes = MutableList(hashMapOf(Pair("name", "John"))
)

// Call fetchCustomerEvents to get the customer attributes.
Exponea.fetchCustomerEvents(
        customerAttributes = attributes,
        onSuccess = {
			// On success, the SDK will return a array list of a CustomerEventModel object.
        },
        onFailure = {
			// On failuire, SDK will return a String with the error.
        }
)
```

#### Get customer recommendation

Get items recommended for a customer.

```
fun fetchRecommendation(
        customerIds: CustomerIds,
        customerRecommendation: CustomerRecommendation,
        onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
        onFailure: (String) -> Unit
)
```

#### 💻 Usage

```
// Preparing the data.
val customerId = CustomerIds(registered = "john@doe.com"),
val recommendation = CustomerRecommendation(
        type = "recommendation",
        id =  "592ff585fb60094e02bfaf6a",
        size = 10,
        strategy = "winner",
        knowItems = false,
        anti = false,
        items = MutableList(hashMapOf(
                Pair("123": 2),
                Pair("234": 4))
        )
)

// Call fetchRecommendation to get the customer attributes.
Exponea.fetchRecommendation(
        customerIds = customerId,
        customerRecommendation = recommendation, 
        onSuccess = {
			// On success, the SDK will return a list of a CustomerAttributeModel object.
        },
        onFailure = {
			// On failuire, SDK will return a String with the error.
        }
)
```