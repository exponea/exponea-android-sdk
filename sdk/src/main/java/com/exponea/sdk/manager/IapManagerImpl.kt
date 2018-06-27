package com.exponea.sdk.manager

import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.*
import com.exponea.sdk.util.Logger

/**
 * In-App Purchase class handles all the purchases made inside the
 * app using the Google Play Store listener.
 * After capture the purchased item it will be send to the database
 * in order to be flushed and send to the Exponea API.
 *
 * @param context Application Context
 */
class IapManagerImpl(context: Context) : IapManager, PurchasesUpdatedListener {

    private val billingClient: BillingClient by lazy {  BillingClient.newBuilder(context).setListener(this).build() }
    private val device = DeviceProperties()
    private val skuList: ArrayList<SkuDetails> = ArrayList()

    /**
     * Starts the connection and implement the billing listener.
     */
    override fun configure() {
        // Starts up BillingClient setup process asynchronously.
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingResponse billingResponseCode: Int) {
                // Store all available products at the Play Store for future use
                // when user purchase item.
                getAvailableProducts()
                Logger.d(this, "Billing service was initiated")
            }

            override fun onBillingServiceDisconnected() {
                Logger.d(this, "Billing service was disconnected")
            }
        })
    }

    /**
     * Check if the listener was successfully started.
     */
    override fun startObservingPayments() {
        // Checks if the client is currently connected to the service.
        if (billingClient.isReady) {
            Logger.d(this, "Billing client was successfully started")
        } else {
            Logger.e(this, "Billing client was not properly started")
        }
    }

    /**
     * Close the connection and release all held resources such as service connections.
     */
    override fun stopObservingPayments() {
        billingClient.endConnection()
    }

    /**
     * Receive the purchased item and send it to the database.
     */
    override fun trackPurchase(properties: HashMap<String, Any>) {
        Exponea.trackEvent(
                eventType = "payment",
                properties = properties,
                type = EventType.PAYMENT
        )
    }

    override fun getAvailableProducts() {
        billingClient.let { billingClient ->
            val params = SkuDetailsParams.newBuilder()
            params.setType(BillingClient.SkuType.INAPP)
            billingClient.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
                if (responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
                    skuList.addAll(skuDetailsList)
                }
            }
        }
    }

    override fun onPurchasesUpdated(
            @BillingResponse responseCode: Int,
            purchases: List<Purchase>?
    ) {
        if (responseCode == BillingResponse.OK && purchases != null) {
            for (purchase in purchases) {

                val sku = skuList.find { it.sku == purchase.sku }

                sku?.let {
                    val product = PurchasedItem(
                            value = it.price.toDouble(),
                            currency = it.priceCurrencyCode,
                            paymentSystem = Constants.General.GooglePlay,
                            productId = it.sku,
                            productTitle = it.title,
                            receipt = null,
                            deviceModel = device.deviceModel,
                            deviceType = device.deviceType,
                            ip = null,
                            osName = device.osName,
                            osVersion = device.osVersion,
                            sdk = device.sdk,
                            sdkVersion = device.sdkVersion
                    )
                    trackPurchase(product.toHashMap())
                }

            }
        } else if (responseCode == BillingResponse.USER_CANCELED) {
            Logger.w(this, "User has canceled the purchased item.")
        } else {
            Logger.e(this, "Could not load the purchase item.")
        }
    }
}