## 🔍 Payments

#### In-App Purchases

In order to use the automatic payment tracking, the Exponea SDK needs to set the BillingClient class listeners.

All In-App Purchase will handle all the purchases made inside the
app using the Google Play Store. After capture the purchased item, it will be send to the database in order to be flushed and send to the Exponea API.

The listeners can be activate or deactivated by setting the `automaticSessionTracking` property in Exponea Configuration.

#### Virtual Payments

If you use in your project some virtual payments (e.g. purchase with in-game gold, coins, ...), now you can track them with simple call `trackVirtualPayment`.

```
fun trackVirtualPayment(
        customerId: CustomerIds,
        item: PurchasedItem
)
```

#### 💻 Usage
```
// Preparing the data.
val customerIds = CustomerIds(registered = "john@doe.com")
val item = PurchasedItem(
        value = 0.911702,
        currency = "EUR",
        paymentSystem = "Virtual",
        productId = "android.test.purchased",
        productTitle = "Silver sword",
        deviceModel = "LGE Nexus 5",
        deviceType = "mobile",
        ip = "10.0.1.58",
        osName = "Android",
        osVersion = "5.0.1",
        sdk = "AndroidSDK",
        sdkVersion = "1.1.4"
)

// Call fetchCustomerAttributes to get the customer attributes.
Exponea.trackVirtualPayment(
        customerId = customerIds,
        item = item)
```

