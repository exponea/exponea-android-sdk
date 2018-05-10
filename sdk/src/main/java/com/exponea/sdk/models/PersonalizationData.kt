package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class PersonalizationData (
        var id: String? = null,
        @SerializedName("date_filter")
        var dateFilter: DateFilter? = null,
        @SerializedName("device_target")
        var deviceTarget: TypeUrl? = null,
        var frequency: String? = null,
        var trigger: Trigger? = null
)