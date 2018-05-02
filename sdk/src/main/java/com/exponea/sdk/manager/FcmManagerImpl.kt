package com.exponea.sdk.manager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.PropertiesList
import com.google.firebase.iid.FirebaseInstanceId
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat;
import com.exponea.sdk.models.ExponeaConfiguration

class FcmManagerImpl (
        val context: Context,
        val configuration: ExponeaConfiguration
): FcmManager {

    private val REQUEST_CODE = 1

    override fun getFcmToken(): String {
        return FirebaseInstanceId.getInstance().getToken().toString()
    }

    override fun trackFcmToken() {
        val uuid = context.getSharedPreferences("PREF_UNIQUE_ID", Context.MODE_PRIVATE)
        val properties: PropertiesList = PropertiesList(hashMapOf(Pair("push_notification_token", getFcmToken())))
        val customerIds: CustomerIds = CustomerIds(cookie = uuid.toString())
        Exponea.updateCustomerProperties(customerIds, properties)
    }

    override fun showNotification(title: String, message: String, id: Int, manager: NotificationManager) {
        val i = Intent(Intent.ACTION_MAIN)

        i.addCategory(Intent.CATEGORY_HOME)
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE,
                i, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context)
                .setContentText(message)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)

        configuration.pushIcon?.let {
            notification
                    .setSmallIcon(it)
        }

        manager.notify(id, notification.build())
    }
}