@file:Suppress("DEPRECATION")

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
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.MessageItemAction
import com.exponea.sdk.models.MessageItemAction.Type.BROWSER
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PurchasedItem
import com.exponea.sdk.repository.AppInboxCacheImplTest
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction4

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
        Pair(Exponea::defaultProperties, hashMapOf<String, Any>()),
        Pair(Exponea::isInitialized, false),
        Pair(Exponea::notificationDataCallback, null),
        Pair(Exponea::inAppMessageActionCallback, Constants.InApps.defaultInAppMessageDelegate),
        Pair(Exponea::customerCookie, null),
        Pair(Exponea::checkPushSetup, false),
        Pair(Exponea::appInboxProvider, Constants.AppInbox.defaulAppInboxProvider)
    )

    val initMethods: Array<Pair<KFunction<Any>, () -> Any>> = arrayOf(
        Pair<KFunction1<Context, Boolean>, () -> Any>(
            Exponea::init
        ) { Exponea.init(ApplicationProvider.getApplicationContext()) },
        Pair<KFunction2<Context, ExponeaConfiguration, Unit>, () -> Any>(
            Exponea::init
        ) { Exponea.init(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(projectToken = "mock-token")
        ) },
        Pair<KFunction<Any>, () -> Any>(
            Exponea::initFromFile
        ) { Exponea.init(ApplicationProvider.getApplicationContext()) }
    )

    val methods = arrayOf(
        Pair(Exponea::anonymize
        ) { Exponea.anonymize() },
        Pair(
            Exponea::identifyCustomer
        ) { Exponea.identifyCustomer(CustomerIds(), PropertiesList(hashMapOf())) },
        Pair(
            Exponea::flushData
        ) { Exponea.flushData() },
        Pair(
            Exponea::getConsents
        ) { Exponea.getConsents({}, {}) },
        Pair(
            Exponea::handleCampaignIntent
        ) {
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
        },
        Pair<KFunction4<Context, Map<String, String>, NotificationManager, Boolean, Boolean>, () -> Any>(
            Exponea::handleRemoteMessage
        ) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val messageData = mapOf(Pair("source", Constants.PushNotif.source))
            Exponea.handleRemoteMessage(context, messageData, notificationManager, false)
        },
        Pair<KFunction1<Map<String, String>, Boolean>, () -> Any>(
            Exponea::isExponeaPushNotification
        ) {
            Exponea.isExponeaPushNotification(null)
        },
        Pair(
            Exponea::trackClickedPush
        ) { Exponea.trackClickedPush() },
        Pair(
            Exponea::trackClickedPushWithoutTrackingConsent
        ) { Exponea.trackClickedPushWithoutTrackingConsent() },
        Pair(
            Exponea::trackDeliveredPush
        ) { Exponea.trackDeliveredPush() },
        Pair(
            Exponea::trackDeliveredPushWithoutTrackingConsent
        ) { Exponea.trackDeliveredPushWithoutTrackingConsent() },
        Pair(
            Exponea::trackEvent
        ) { Exponea.trackEvent(PropertiesList(hashMapOf()), null, null) },
        Pair(
            Exponea::trackPaymentEvent
        ) {
            val purchasedItem = PurchasedItem(
                1.0,
                "eur",
                "mock-payment-system",
                "mock-product-id",
                "mock-product-title"
            )
            Exponea.trackPaymentEvent(1.0, purchasedItem)
        },
        Pair<KFunction1<String, Unit>, () -> Any>(
            Exponea::trackPushToken
        ) { Exponea.trackPushToken("mock-push-token") },
        Pair<KFunction1<String, Unit>, () -> Any>(
            Exponea::trackHmsPushToken
        ) { Exponea.trackHmsPushToken("mock-push-token") },
        Pair(
            Exponea::trackSessionEnd
        ) { Exponea.trackSessionEnd() },
        Pair(
            Exponea::trackSessionStart
        ) { Exponea.trackSessionStart() },
        Pair(
            Exponea::fetchRecommendation
        ) { Exponea.fetchRecommendation(CustomerRecommendationOptions("", true), {}, {}) },
        Pair<KFunction2<Context, String, Unit>, () -> Any>(
            Exponea::handleNewToken
        ) { Exponea.handleNewToken(ApplicationProvider.getApplicationContext(), "mock-push-token") },
        Pair<KFunction2<Context, String, Unit>, () -> Any>(
            Exponea::handleNewHmsToken
        ) { Exponea.handleNewHmsToken(ApplicationProvider.getApplicationContext(), "mock-push-token") },
        Pair(
            Exponea::trackInAppMessageClick
        ) { Exponea.trackInAppMessageClick(
            InAppMessageTest.getInAppMessage(),
            "mock-button-text",
            "mock-button-link") },
        Pair(
            Exponea::trackInAppMessageClickWithoutTrackingConsent
        ) { Exponea.trackInAppMessageClickWithoutTrackingConsent(
            InAppMessageTest.getInAppMessage(),
            "mock-button-text",
            "mock-button-link") },
        Pair(
            Exponea::trackInAppMessageClose
        ) { Exponea.trackInAppMessageClose(InAppMessageTest.getInAppMessage())
        },
        Pair(Exponea::trackInAppMessageCloseWithoutTrackingConsent) {
            Exponea.trackInAppMessageCloseWithoutTrackingConsent(InAppMessageTest.getInAppMessage())
        },
        Pair(Exponea::trackInAppMessageCloseWithoutTrackingConsent) {
            Exponea.trackInAppMessageCloseWithoutTrackingConsent(InAppMessageTest.getInAppMessage())
        },
        Pair(Exponea::getAppInboxButton) {
            Exponea.getAppInboxButton(ApplicationProvider.getApplicationContext())
        },
        Pair(Exponea::getAppInboxListView) {
            Exponea.getAppInboxListView(
                ApplicationProvider.getApplicationContext(),
                onItemClicked = { _, _ -> }
            )
        },
        Pair(Exponea::getAppInboxListFragment) {
            Exponea.getAppInboxListFragment(ApplicationProvider.getApplicationContext())
        },
        Pair(Exponea::getAppInboxDetailFragment) {
            Exponea.getAppInboxDetailFragment(ApplicationProvider.getApplicationContext(), "1")
        },
        Pair(Exponea::getAppInboxDetailView) {
            Exponea.getAppInboxDetailView(ApplicationProvider.getApplicationContext(), "1")
        },
        Pair(Exponea::fetchAppInbox) {
            Exponea.fetchAppInbox(callback = { _ -> })
        },
        Pair(Exponea::fetchAppInboxItem) {
            Exponea.fetchAppInboxItem("1") { _ -> }
        },
        Pair(Exponea::trackAppInboxOpened) {
            Exponea.trackAppInboxOpened(AppInboxCacheImplTest.buildMessage("1"))
        },
        Pair(Exponea::trackAppInboxOpenedWithoutTrackingConsent) {
            Exponea.trackAppInboxOpenedWithoutTrackingConsent(AppInboxCacheImplTest.buildMessage("1"))
        },
        Pair(Exponea::trackAppInboxClick) {
            Exponea.trackAppInboxClick(buildMessageItemAction(), AppInboxCacheImplTest.buildMessage("1"))
        },
        Pair(Exponea::trackAppInboxClickWithoutTrackingConsent) {
            Exponea.trackAppInboxClickWithoutTrackingConsent(
                buildMessageItemAction(),
                AppInboxCacheImplTest.buildMessage("1")
            )
        },
        Pair(Exponea::markAppInboxAsRead) {
            Exponea.markAppInboxAsRead(
                AppInboxCacheImplTest.buildMessage("1"),
                null
            )
        },
        Pair(Exponea::getInAppContentBlocksPlaceholder) {
            Exponea.getInAppContentBlocksPlaceholder("placeholder1", ApplicationProvider.getApplicationContext())
        },
        Pair(Exponea::requestPushAuthorization) {
            Exponea.requestPushAuthorization(ApplicationProvider.getApplicationContext()) { }
        }
    )

    private fun buildMessageItemAction(): MessageItemAction {
        return MessageItemAction().apply {
            url = "https://test.com"
            type = BROWSER
            title = "test"
        }
    }

    val awaitInitMethods = arrayOf(
        Exponea::identifyCustomer,
        Exponea::flushData,
        Exponea::getConsents,
        Exponea::trackClickedPush,
        Exponea::trackClickedPushWithoutTrackingConsent,
        Exponea::trackDeliveredPush,
        Exponea::trackDeliveredPushWithoutTrackingConsent,
        Exponea::trackEvent,
        Exponea::trackPaymentEvent,
        Exponea::trackPushToken,
        Exponea::trackHmsPushToken,
        Exponea::trackSessionEnd,
        Exponea::trackSessionStart,
        Exponea::fetchRecommendation,
        Exponea::trackInAppMessageClick,
        Exponea::trackInAppMessageClickWithoutTrackingConsent,
        Exponea::trackInAppMessageClose,
        Exponea::trackInAppMessageCloseWithoutTrackingConsent,
        Exponea::fetchAppInbox,
        Exponea::fetchAppInboxItem,
        Exponea::trackAppInboxOpened,
        Exponea::trackAppInboxOpenedWithoutTrackingConsent,
        Exponea::trackAppInboxClick,
        Exponea::trackAppInboxClickWithoutTrackingConsent,
        Exponea::markAppInboxAsRead
    )

    val sdkLessMethods = arrayOf(
        Exponea::handleCampaignIntent,
        Exponea::handleRemoteMessage,
        Exponea::handleNewToken,
        Exponea::handleNewHmsToken
    )
}
