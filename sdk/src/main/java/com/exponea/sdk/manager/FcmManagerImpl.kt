package com.exponea.sdk.manager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.util.Logger
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat;
import com.exponea.sdk.models.ExponeaConfiguration

class FcmManagerImpl (
        val context: Context,
        val configuration: ExponeaConfiguration
): FcmManager, FirebaseMessagingService() {

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

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)

        Logger.d(this, "Push Notification received.")

        val title = message?.data?.get("title")?.also {
            it
        } ?: run {
           ""
        }

        val body = message?.data?.get("body")?.also {
            it
        } ?: run {
            ""
        }

        val notificationId = message?.data?.get("notification_id")?.toInt().also {
            it
        } ?: run {
            0
        }

        showNotifications(title, body, notificationId);

    }

    private fun showNotifications(title: String, msg: String, notificationId: Int) {
        val i = Intent(this, context::class.java)

        val pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE,
                i, PendingIntent.FLAG_UPDATE_CURRENT)


        val notification = NotificationCompat.Builder(this)
                .setContentText(msg)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)

        configuration.pushIcon?.let {
            notification
                    .setSmallIcon(it)
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification.build())
    }
}