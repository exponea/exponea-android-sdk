package com.exponea.example.navigation

import android.content.Intent
import com.exponea.example.ui.navigation.Routes
import com.exponea.sdk.util.isViewUrlIntent

object DeeplinkResolver {

    fun resolve(intent: Intent?): DeeplinkFlow? {
        fun String.toDeeplinkDestination(): DeeplinkFlow? = when {
            contains("track") -> DeeplinkFlow.Track
            contains("flush") -> DeeplinkFlow.Manual
            contains("fetch") -> DeeplinkFlow.Fetch
            contains("inappcb") -> DeeplinkFlow.InAppCb
            contains("anonymize") -> DeeplinkFlow.Anonymize
            contains("stopAndContinue") -> DeeplinkFlow.StopAndContinue
            contains("stopAndRestart") -> DeeplinkFlow.StopAndRestart
            else -> null
        }
        return when {
            intent.isViewUrlIntent("http") ->
                intent?.data?.path?.toDeeplinkDestination()
            intent.isViewUrlIntent("exponea") ->
                intent?.data?.path?.toDeeplinkDestination()
                    ?: intent?.data?.host?.toDeeplinkDestination()
            else -> null
        }
    }

    fun toRoute(flow: DeeplinkFlow): String? = when (flow) {
        DeeplinkFlow.Fetch -> Routes.FETCHING
        DeeplinkFlow.Track -> Routes.TRACKING
        DeeplinkFlow.Manual -> Routes.FLUSHING
        DeeplinkFlow.Anonymize -> Routes.ANONYMIZE
        DeeplinkFlow.InAppCb -> Routes.CONTENT_BLOCKS
        DeeplinkFlow.StopAndContinue, DeeplinkFlow.StopAndRestart -> null
    }
}
