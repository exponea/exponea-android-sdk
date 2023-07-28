package com.exponea.example.view

sealed interface BottomTab {
    object Fetch : BottomTab
    object Track : BottomTab
    object Manual : BottomTab
    object Anonymize : BottomTab
    object InAppContentBlock : BottomTab
}
