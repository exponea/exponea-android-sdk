package com.exponea.example.view

sealed interface DeeplinkDestination {
    object Fetch : DeeplinkDestination
    object Track : DeeplinkDestination
    object Manual : DeeplinkDestination
    object Anonymize : DeeplinkDestination
    object InAppCb : DeeplinkDestination
}
