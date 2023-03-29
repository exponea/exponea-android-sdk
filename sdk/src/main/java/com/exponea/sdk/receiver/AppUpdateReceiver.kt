package com.exponea.sdk.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.PushTokenRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.util.Logger

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent?.action, true) == false) {
            Logger.e(this, "Application update event received but with different action ${intent?.action}")
            return
        }
        if (context == null) {
            Logger.e(this, "Application update event received but with no context")
            return
        }
        migrateFcmToken(context)
    }

    /**
     * Moves FCM token stored in obsolete PushTokenRepository to new one, that is not backup-ed (as should not be).
     */
    private fun migrateFcmToken(context: Context) {
        val obsoleteRepo = PushTokenRepositoryImpl(ExponeaPreferencesImpl(context))
        if (obsoleteRepo.get().isNullOrEmpty()) {
            Logger.d(this, "FCM token migration not needed")
            return
        }
        val newRepo = PushTokenRepositoryProvider.get(context)
        if (newRepo.get().isNullOrEmpty() == false) {
            Logger.d(this, "FCM token migration already done")
            obsoleteRepo.clear()
            return
        }
        newRepo.setTrackedToken(
            obsoleteRepo.get()!!,
            obsoleteRepo.getLastTrackDateInMilliseconds() ?: 0,
            obsoleteRepo.getLastTokenType()
        )
        obsoleteRepo.clear()
        Logger.d(this, "FCM migration has been done")
    }
}
