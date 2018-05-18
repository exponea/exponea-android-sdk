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
            const val EVENT_TRACK_CUSTOMER = "CustomerEvent"
            const val EVENT_UPDATE_CUSTOMER = "UpdateCustomer"

        }
    }
}