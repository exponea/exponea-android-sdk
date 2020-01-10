package com.exponea.sdk.runcatching

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PurchasedItem
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

internal object PublicApiTestCases {
    val properties = arrayOf(
        Pair(Exponea::campaignTTL, Constants.Campaign.defaultCampaignTTL),
        Pair(Exponea::isAutomaticSessionTracking, Constants.Session.defaultAutomaticTracking),
        Pair(Exponea::flushMode, Constants.Flush.defaultFlushMode),
        Pair(Exponea::flushPeriod, Constants.Flush.defaultFlushPeriod),
        Pair(Exponea::isAutoPushNotification, Constants.PushNotif.defaultAutomaticListening),
        Pair(Exponea::loggerLevel, Constants.Logger.defaultLoggerLevel),
        Pair(Exponea::sessionTimeout, Constants.Session.defaultTimeout),
        Pair(Exponea::tokenTrackFrequency, Constants.Token.defaultTokenFrequency),
        Pair(Exponea::isInitialized, false),
        Pair(Exponea::notificationDataCallback, null)
    )

    val initMethods: Array<Pair<KFunction<Any>, () -> Any>> = arrayOf(
        Pair<KFunction1<Context, Boolean>, () -> Any>(
            Exponea::init,
            { Exponea.init(ApplicationProvider.getApplicationContext()) }
        ),
        Pair<KFunction2<Context, ExponeaConfiguration, Unit>, () -> Any>(
            Exponea::init,
            { Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration()) }
        ),
        Pair<KFunction<Any>, () -> Any>(
            Exponea::initFromFile,
            { Exponea.init(ApplicationProvider.getApplicationContext()) }
        )
    )

    val methods = arrayOf(
        Pair(Exponea::anonymize,
            { Exponea.anonymize() }
        ),
        Pair(
            Exponea::showBanners,
            { Exponea.showBanners(CustomerIds()) }
        ),
        Pair(
            Exponea::identifyCustomer,
            { Exponea.identifyCustomer(CustomerIds(), PropertiesList(hashMapOf())) }
        ),
        Pair(
            Exponea::flushData,
            { Exponea.flushData() }
        ),
        Pair(
            Exponea::getConsents,
            { Exponea.getConsents({}, {}) }
        ),
        Pair(
            Exponea::getPersonalizationWebLayer,
            { Exponea.getPersonalizationWebLayer({}, {}) }
        ),
        Pair(
            Exponea::handleCampaignIntent,
            {
                val campaignId = "http://example.com/route/to/campaing" +
                    "?utm_source=mock-utm-source" +
                    "&utm_campaign=mock-utm-campaign" +
                    "&utm_content=mock-utm-content" +
                    "&utm_medium=mock-utm-medium" +
                    "&utm_term=mock-utm-term" +
                    "&xnpe_cmp=mock-xnpe-xmp"
                val intent = Intent().apply {
                    this.action = Intent.ACTION_VIEW
                    this.addCategory(Intent.CATEGORY_DEFAULT)
                    this.addCategory(Intent.CATEGORY_BROWSABLE)
                    this.data = Uri.parse(campaignId)
                }
                Exponea.handleCampaignIntent(intent, ApplicationProvider.getApplicationContext())
            }
        ),
        Pair(
            Exponea::handleRemoteMessage,
            {
                val notificationManager = ApplicationProvider.getApplicationContext<Context>()
                    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                Exponea.handleRemoteMessage(null, notificationManager, false)
            }
        ),
        Pair(
            Exponea::trackClickedPush,
            { Exponea.trackClickedPush() }
        ),
        Pair(
            Exponea::trackDeliveredPush,
            { Exponea.trackDeliveredPush() }
        ),
        Pair(
            Exponea::trackEvent,
            { Exponea.trackEvent(PropertiesList(hashMapOf()), null, null) }
        ),
        Pair(
            Exponea::trackPaymentEvent,
            {
                val purchasedItem = PurchasedItem(
                    1.0,
                    "eur",
                    "mock-payment-system",
                    "mock-product-id",
                    "mock-product-title"
                )
                Exponea.trackPaymentEvent(1.0, purchasedItem)
            }
        ),
        Pair(
            Exponea::trackPushToken,
            { Exponea.trackPushToken("mock-push-token") }
        ),
        Pair(
            Exponea::trackSessionEnd,
            { Exponea.trackSessionEnd() }
        ),
        Pair(
            Exponea::trackSessionStart,
            { Exponea.trackSessionStart() }
        ),
        Pair(
            Exponea::fetchRecommendation,
            { Exponea.fetchRecommendation(CustomerRecommendationOptions("", true), {}, {}) }
        )
    )
}
