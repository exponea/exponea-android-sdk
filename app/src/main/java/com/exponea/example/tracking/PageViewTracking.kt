package com.exponea.example.tracking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.PropertiesList

fun trackPageViewEvent(screenName: String) {
    if (!Exponea.isInitialized) return
    Exponea.trackEvent(
        eventType = "page_view",
        properties = PropertiesList(hashMapOf("name" to screenName))
    )
}

@Composable
fun TrackPageView(screenName: String) {
    LaunchedEffect(screenName) {
        trackPageViewEvent(screenName)
    }
}
