package com.exponea.sdk.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.exponea.sdk.services.MessagingUtils
import kotlin.random.Random

object UrlOpener {
    fun openUrl(context: Context, url: String) {
        try {
            val uriToOpen = Uri.parse(url)
            Logger.v(this, "Opening URL $uriToOpen")
            val intentWithUrl = Intent(Intent.ACTION_VIEW)
            intentWithUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            intentWithUrl.data = uriToOpen
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                context.startActivity(intentWithUrl)
                return
            }
            // Newer Api
            PendingIntent.getActivities(
                context.applicationContext,
                Random.nextInt(),
                arrayOf(intentWithUrl),
                MessagingUtils.getPendingIntentFlags()
            ).send()
        } catch (e: Exception) {
            Logger.e(this, "Unable to open URL $url", e)
        }
    }
}
