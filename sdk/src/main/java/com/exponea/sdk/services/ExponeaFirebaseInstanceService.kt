package com.exponea.sdk.services

import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger
import com.google.firebase.iid.FirebaseInstanceIdService

class ExponeaFirebaseInstanceService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {

        //TODO to fix lateinit not initialized bug
        /*
        if (!Exponea.isAutoPushNotification) {
            return
        }

        */

        Logger.d(this, "Firebase Token Refreshed")
       // Exponea.component.pushManager.trackFcmToken()
    }
}