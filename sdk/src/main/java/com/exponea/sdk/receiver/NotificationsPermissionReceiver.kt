package com.exponea.sdk.receiver

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.runOnBackgroundThread
import com.exponea.sdk.view.NotificationsPermissionActivity

class NotificationsPermissionReceiver(
    private val listener: (Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != getBroadcastAction(context)) {
            Logger.e(this, "Invalid action value ${intent.action}")
            return
        }
        context.unregisterReceiver(this)
        val permissionGranted = intent.getBooleanExtra(
            ACTION_PERMISSIONS_RESULT_BOOL,
            false
        )
        Logger.i(this, "Push notification permission has been " +
            if (permissionGranted) "GRANTED" else "DENIED"
        )
        runOnBackgroundThread {
            runCatching {
                listener(permissionGranted)
            }.logOnException()
        }
    }

    companion object {
        private const val ACTION_PERMISSIONS_RESPONSE = "NotificationPermission.RESPONSE"
        internal const val ACTION_PERMISSIONS_RESULT_BOOL = "NotificationPermission.RESULT"

        fun getBroadcastAction(context: Context): String {
            return context.packageName + "." + ACTION_PERMISSIONS_RESPONSE
        }

        fun isPermissionGranted(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return true
            }
            val permissionState: Int = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            return permissionState == PackageManager.PERMISSION_GRANTED
        }

        fun requestPushAuthorization(context: Context, listener: (Boolean) -> Unit) {
            runOnBackgroundThread {
                runCatching {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Logger.i(this, "Push notifications permission is not needed")
                        listener(true)
                        return@runCatching
                    }
                    val permissionState: Int = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (permissionState == PackageManager.PERMISSION_GRANTED) {
                        Logger.i(this, "Push notifications permission already granted")
                        listener(true)
                        return@runCatching
                    }
                    val intent = Intent(context, NotificationsPermissionActivity::class.java)
                    if (context !is Activity) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.applicationContext.registerReceiver(
                        NotificationsPermissionReceiver(listener),
                        IntentFilter(getBroadcastAction(context)),
                        RECEIVER_NOT_EXPORTED
                    )
                    context.startActivity(intent)
                }.logOnException()
            }
        }
    }
}
