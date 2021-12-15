package com.exponea.sdk.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.exponea.sdk.models.NotificationAction

internal class ExponeaPushTrackingActivityOlderApi : ExponeaPushTrackingActivity() {

    override fun processPushClick(
        context: Context,
        intent: Intent,
        timestamp: Double
    ) {
        super.processPushClick(context, intent, timestamp)
        if (intent.action == ACTION_URL_CLICKED || intent.action == ACTION_DEEPLINK_CLICKED) {
            val intentWithUrl = Intent(Intent.ACTION_VIEW)
            intentWithUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val url = (intent.getSerializableExtra(EXTRA_ACTION_INFO) as NotificationAction).url
            if (url != null && url.isNotEmpty()) intentWithUrl.data = Uri.parse(url)
            startActivity(intentWithUrl)
        }
    }
}
