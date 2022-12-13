package com.exponea.sdk.models

import com.exponea.sdk.models.MessageItemAction.Type.NO_ACTION

class MessageItemAction {
    var type: Type = NO_ACTION
    var title: String? = null
    var url: String? = null

    /**
     * Each action is used to trigger a different event when clicking
     */
    enum class Type(val value: String) {
        /**
         * Opens this app. Does nothing particullar but exists for mapping
         */
        APP("app"),
        /**
         * Opens URL in browser
         */
        BROWSER("browser"),
        /**
         * Opens Deeplink URL
         */
        DEEPLINK("deeplink"),
        /**
         * Does nothing particullar but exists for yet-non-existing-mapping
         */
        NO_ACTION("no_action");

        companion object {
            fun find(value: String?) = Type.values().find { it.value == value }
        }
    }
}
