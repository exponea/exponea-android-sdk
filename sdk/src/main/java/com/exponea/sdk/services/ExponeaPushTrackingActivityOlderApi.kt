package com.exponea.sdk.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.exponea.sdk.ExponeaExtras
import com.exponea.sdk.ExponeaExtras.Companion.ACTION_DEEPLINK_CLICKED
import com.exponea.sdk.ExponeaExtras.Companion.ACTION_URL_CLICKED
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_ACTION_INFO
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.util.Logger

internal class ExponeaPushTrackingActivityOlderApi : ExponeaPushTrackingActivity() {

    override fun processPushClick(context: Context, intent: Intent) {
        super.processPushClick(context, intent)
        if (intent.action == ACTION_URL_CLICKED || intent.action == ACTION_DEEPLINK_CLICKED) {
            val intentWithUrl = Intent(Intent.ACTION_VIEW)
            intentWithUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val url = (intent.getSerializableExtra(EXTRA_ACTION_INFO) as NotificationAction).url
            if (!url.isNullOrEmpty()) intentWithUrl.data = Uri.parse(url)
            finish()
            Logger.d(this, "Opening VIEW intent for url: $url")
            startActivity(intentWithUrl)
        } else if (intent.action == ExponeaExtras.ACTION_CLICKED) {
            val openAppIntent = MessagingUtils.getIntentAppOpen(context.applicationContext)
            val launchActivityClass = openAppIntent?.component?.className?.let { Class.forName(it) }
            if (launchActivityClass == null) {
                Logger.e(this, "Application launch intent has not been found, check your manifest")
                return
            }
            val launchIntent = Intent(context, launchActivityClass)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            finish()
            Logger.d(this, "Opening launch intent for app: ${launchActivityClass.name}")
            startActivity(launchIntent)
        }
    }
}
