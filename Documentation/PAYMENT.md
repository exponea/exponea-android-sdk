## 🔍 Payments

Exponea SDK has a convenience method `trackPaymentEvent` to help you track information about a payment for product/service within the application.
```
fun trackPaymentEvent(
        timestamp: Double = currentTimeSeconds(),
        purchasedItem: PurchasedItem
)
```
To support multiple platforms and use-cases, SDK defines abstraction `PurchasedItem` that contains basic information about the purchase.
```
data class PurchasedItem(
    var value: Double,
    var currency: String,
    var paymentSystem: String,
    var productId: String,
    var productTitle: String,
    var receipt: String? = null
)
```
#### 💻 Usage

```
val item = PurchasedItem(
        value = 12.34,
        currency = "EUR",
        paymentSystem = "Virtual",
        productId = "handbag",
        productTitle = "Awesome leather handbag"
)

Exponea.trackPaymentEvent( item = item)
```

### In-App Purchases


If your app uses in-app purchases (e.g. purchase with in-game gold, coins, ...), you can track them with `trackPaymentEvent` using `Purchase` and `SkuDetails` objects used in Google Play Billing Library.

#### 💻 Usage

```
val purchase: com.android.billingclient.api.Purchase = ...
val skuDetails: com.android.billingclient.api.SkuDetails = ...
val item = PurchasedItem(
        value = sku.priceAmountMicros / 1000000.0,
        currency = sku.priceCurrencyCode,
        paymentSystem = "Google Play",
        productId = sku.sku,
        productTitle = sku.title,
        receipt = purchase.signature
)

Exponea.trackPaymentEvent( item = item)
```
