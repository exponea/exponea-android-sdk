package com.exponea.sdk.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.UniqueIdentifierRepository
import com.google.firebase.iid.FirebaseInstanceId
import android.app.NotificationChannel
import android.os.Build

class FcmManagerImpl(
        private val context: Context,
        private val configuration: ExponeaConfiguration,
        private val uniqueIdentifierRepository: UniqueIdentifierRepository
) : FcmManager {

    private val REQUEST_CODE = 1

    override val fcmToken: String
        get() = FirebaseInstanceId.getInstance().token.toString()

    override fun trackFcmToken() {
        val uniqueToken = uniqueIdentifierRepository.get()
        val properties = PropertiesList(hashMapOf(Pair("push_notification_token", fcmToken)))
        val customerIds = CustomerIds(cookie = uniqueToken)
        Exponea.updateCustomerProperties(customerIds, properties)
    }

    override fun trackDeliveredPush(customerIds: CustomerIds, fcmToken: String) {
        val properties: PropertiesList = PropertiesList(hashMapOf(Pair("push_notification_token", fmcToken)))
        Exponea.trackCustomerEvent(
                customerIds = customerIds,
                properties = properties,
                eventType = "push_notification")
    }

    override fun trackClickedPush(customerIds: CustomerIds, fcmToken: String) {

    }


    override fun showNotification(
            title: String,
            message: String,
            id: Int,
            manager: NotificationManager
    ) {
        val i = Intent(Intent.ACTION_MAIN)

        i.addCategory(Intent.CATEGORY_HOME)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent = PendingIntent.getActivity(
                context, REQUEST_CODE,
                i, PendingIntent.FLAG_UPDATE_CURRENT
        )

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

    override fun createNotificationChannel(
            manager: NotificationManager
    ) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = configuration.pushChannelName
            val description = configuration.pushChannelDescription
            val importance = configuration.pushNotificationImportance
            val channel = NotificationChannel(configuration.pushChannelId, name, importance)

            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            manager.createNotificationChannel(channel)
        }
    }
}