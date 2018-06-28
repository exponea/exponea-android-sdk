package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.UniqueIdentifierRepository
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService

class PushManagerImpl(
        private val uniqueIdentifierRepository: UniqueIdentifierRepository
) : PushManager, FirebaseMessagingService() {

    override val fcmToken: String
        get() = FirebaseInstanceId.getInstance().token.toString()


    override fun trackFcmToken() {
        Exponea.trackPushToken(fcmToken)
    }

    override fun trackDeliveredPush(data: NotificationData?) {
        Exponea.trackDeliveredPush(
                fcmToken = fcmToken,
                data = data
        )
    }

    override fun trackClickedPush(data: NotificationData?) {
        Exponea.trackClickedPush(
                fcmToken = fcmToken,
                data = data
        )
    }

    override fun onCreate() {
        super.onCreate()

        trackClickedPush()
    }


}