package com.exponea.sdk.manager

import android.text.format.DateUtils
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.repository.FirebaseTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService

class PushManagerImpl(
        private val firebaseTokenRepository: FirebaseTokenRepository
) : PushManager, FirebaseMessagingService() {

    override val fcmToken: String?
        get() = firebaseTokenRepository.get()

    override val lastTrackDateInMilliseconds: Long
        get() = firebaseTokenRepository.getLastTrackDateInMilliseconds()
                ?: System.currentTimeMillis()

    override fun trackFcmToken(token: String?) {

        val shouldUpdateToken = when (Exponea.tokenTrackFrequency) {
            ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE -> token != null && token != fcmToken
            ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH -> true
            ExponeaConfiguration.TokenFrequency.DAILY -> !DateUtils.isToday(lastTrackDateInMilliseconds)
            else -> true
        }

        if (token != null && shouldUpdateToken) {
            firebaseTokenRepository.set(token, System.currentTimeMillis())
            Exponea.trackPushToken(token)
        }

    }

    override fun trackDeliveredPush(data: NotificationData?) {
        Exponea.trackDeliveredPush(
                data = data
        )
    }

    override fun trackClickedPush(data: NotificationData?, action: NotificationAction?) {
        Exponea.trackClickedPush(
                data = data,
                actionData = action
        )
    }

    override fun onCreate() {
        super.onCreate()
        trackClickedPush()
    }

}