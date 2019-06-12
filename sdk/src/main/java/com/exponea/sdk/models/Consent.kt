package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class Consent(
    var id: String = "",
    @SerializedName("legitimate_interest")
    var legitimateInterest: Boolean = false,
    var sources: ConsentSources = ConsentSources(),
    var translations: HashMap<String, HashMap<String, String>> = hashMapOf()
)