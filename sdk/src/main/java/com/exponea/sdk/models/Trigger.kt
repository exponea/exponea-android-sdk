package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

internal data class Trigger(
        @SerializedName("include_pages")
        var includePages: MutableList<TypeUrl>? = null,
        @SerializedName("exclude_pages")
        var excludePages: MutableList<TypeUrl>? = null
)