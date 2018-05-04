package com.exponea.example.models

class Constants {
    class ScreenNames {
        companion object {
            var mainScreen = "Main"
            var discoverScreen = "Discover"
            var searchScreen = "Search"
            var purchaseScreen = "Purchase"
            var settingsScreen = "Settings"
        }
    }
    class Events {
        companion object {
            const val EVENT_PURCHASE = "Purchase"
            const val EVENT_TRACK = "Track"
            const val EVENT_TRACK_CUSTOMER = "TrackCustomer"
            const val EVENT_INSTALL = "Install"

        }
    }
}