package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.repository.UniqueIdentifierRepository
import com.exponea.sdk.util.Logger
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService

class PushManagerImpl(
        private val uniqueIdentifierRepository: UniqueIdentifierRepository
) : PushManager, FirebaseMessagingService() {

    override val fcmToken: String?
        get() = try {
            FirebaseInstanceId.getInstance()?.token
        } catch (e: Exception) {
            // Firebase not initiated return null
            Logger.w(this, e.message ?: "Firebase not initiated")
            null
        }


    override fun trackFcmToken() {
        if (fcmToken != null) {
            Exponea.trackPushToken(fcmToken!!)
        }
    }

    override fun trackDeliveredPush(data: NotificationData?) {
        Exponea.trackDeliveredPush(
                data = data
        )
    }

    override fun trackClickedPush(data: NotificationData?) {
        Exponea.trackClickedPush(
                data = data
        )
    }

    override fun onCreate() {
        super.onCreate()

        trackClickedPush()
    }


}