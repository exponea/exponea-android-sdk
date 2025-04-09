package com.exponea.example.view

sealed interface DeeplinkFlow {
    object Fetch : DeeplinkFlow
    object Track : DeeplinkFlow
    object Manual : DeeplinkFlow
    object Anonymize : DeeplinkFlow
    object InAppCb : DeeplinkFlow
    object StopAndContinue : DeeplinkFlow
    object StopAndRestart : DeeplinkFlow
}
