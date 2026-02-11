package com.exponea.sdk.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.net.toUri
import com.exponea.sdk.services.MessagingUtils
import kotlin.random.Random

internal object UrlOpener {
    fun openUrlExternal(context: Context, url: String?) {
        val uri = getValidatedUri(url) ?: return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            data = uri
        }

        try {
            if (VERSION.SDK_INT < VERSION_CODES.M) {
                context.startActivity(intent)
            } else {
                PendingIntent.getActivities(
                    context.applicationContext,
                    Random.nextInt(),
                    arrayOf(intent),
                    MessagingUtils.getPendingIntentFlags()
                ).send()
            }
        } catch (e: Exception) {
            Logger.e(this, "Failed to open URL: $url", e)
        }
    }

    fun openUrlInApp(context: Context, url: String?) {
        val uri = getValidatedUri(url) ?: return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = uri
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e(this, "Failed to open URL: $uri", e)
        }
    }

    fun getValidatedUri(url: String?): Uri? {
        val trimmedUrl = url?.trim()

        if (trimmedUrl.isNullOrBlank()) {
            Logger.e(this, "URL is empty or blank")
            return null
        }

        val uri = try {
            trimmedUrl.toUri()
        } catch (e: Exception) {
            Logger.e(this, "Cannot parse URL: $trimmedUrl", e)
            return null
        }

        if (uri.scheme.isNullOrBlank()) {
            Logger.e(this, "Invalid URL scheme: $trimmedUrl")
            return null
        }
        return uri
    }
}
