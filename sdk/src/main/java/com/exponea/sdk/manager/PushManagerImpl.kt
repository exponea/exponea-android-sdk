package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.UniqueIdentifierRepository
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService

class PushManagerImpl(
        private val uniqueIdentifierRepository: UniqueIdentifierRepository
) : PushManager, FirebaseMessagingService() {

    override val fcmToken: String
        get() = FirebaseInstanceId.getInstance().token.toString()

    val uniqueToken = uniqueIdentifierRepository.get()
    val customerIds = CustomerIds(cookie = uniqueToken)

    override fun trackFcmToken() {
        Exponea.trackPushToken(customerIds, fcmToken)
    }

    override fun trackDeliveredPush() {
        Exponea.trackDeliveredPush(
                customerIds = customerIds,
                fcmToken = fcmToken
        )
    }

    override fun trackClickedPush() {
        Exponea.trackClickedPush(
                customerIds = customerIds,
                fcmToken = fcmToken
        )
    }

    override fun onCreate() {
        super.onCreate()

        trackClickedPush()
    }


}