package com.exponea.sdk.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.exponea.sdk.receiver.NotificationsPermissionReceiver
import com.exponea.sdk.util.Logger

class NotificationsPermissionActivity : Activity() {
    private val permissionRequestCode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Logger.w(this, "Push notifications permission is not needed")
            sendBroadcastResult(true)
            finish()
            return
        }
        val permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            Logger.w(this, "Push notifications permission already granted")
            sendBroadcastResult(true)
            finish()
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            permissionRequestCode
        )
    }

    private fun sendBroadcastResult(result: Boolean) {
        val intent = Intent()
        intent.action = NotificationsPermissionReceiver.ACTION_PERMISSIONS_RESPONSE
        intent.putExtra(NotificationsPermissionReceiver.ACTION_PERMISSIONS_RESULT_BOOL, result)
        sendBroadcast(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Logger.i(this, "Permission got code $requestCode")
        if (requestCode == permissionRequestCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Logger.i(this, "Push notifications permission has been granted")
                sendBroadcastResult(true)
            } else {
                Logger.w(this, "Push notifications permission has been denied")
                sendBroadcastResult(false)
            }
            finish()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
