package com.exponea.sdk.models

data class AttributesResponse(
        var success: Boolean,
        var results: List<Attribute>?,
        var errors: String
) {


     data class Attribute(
            val success: Boolean,
            val value: String?
    )
}