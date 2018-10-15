package com.exponea.sdk.services


import android.os.Looper
import com.exponea.sdk.Exponea
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger
import com.google.firebase.iid.FirebaseInstanceIdService

class ExponeaFirebaseInstanceService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {

        if (!Exponea.isInitialized) {
            val config = ExponeaConfigRepository.get(applicationContext)
            if (config == null){
                return
            }
            Looper.prepare()
            Exponea.init(applicationContext, config)
        }
        if (!Exponea.isAutoPushNotification) {
            return
        }
        Logger.d(this, "Firebase Token Refreshed")
        Exponea.component.pushManager.trackFcmToken()
    }
}