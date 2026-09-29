package com.exponea.example.navigation

sealed interface DeeplinkFlow {
    data object Fetch : DeeplinkFlow
    data object Track : DeeplinkFlow
    data object Manual : DeeplinkFlow
    data object Anonymize : DeeplinkFlow
    data object InAppCb : DeeplinkFlow
    data object StopAndContinue : DeeplinkFlow
    data object StopAndRestart : DeeplinkFlow
}
