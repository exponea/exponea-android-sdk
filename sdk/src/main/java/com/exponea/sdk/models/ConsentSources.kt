package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class ConsentSources(
    @SerializedName("crm")
    var createdFromCRM: Boolean = false,
    @SerializedName("import")
    var imported: Boolean = false,
    @SerializedName("page")
    var fromConsentPage: Boolean = false,
    @SerializedName("private_api")
    var privateAPI: Boolean = false,
    @SerializedName("public_api")
    var publicAPI: Boolean = false,
    @SerializedName("scenario")
    var trackedFromScenario: Boolean = false
)
