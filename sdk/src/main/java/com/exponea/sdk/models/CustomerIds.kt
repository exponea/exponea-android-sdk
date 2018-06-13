package com.exponea.sdk.models

data class CustomerIds(
        var registered: String? = null
) {

    var cookie: String? = null
        internal set(value) {
            field = value
        }
}