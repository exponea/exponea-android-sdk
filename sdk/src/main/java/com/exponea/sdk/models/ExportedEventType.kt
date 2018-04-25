package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName
import java.util.*

/*
Example Payload

{
 "customer_ids": { "registered": "unique ID" } ,
   "project_id":"project token",
   "type": "event name",
   "properties": {
       "event property name": "event property value",
   },
   "timestamp": 1465906739
}

 */
data class ExportedEventType(
        override var id: String = UUID.randomUUID().toString(),
        var type: String?,
        var timestamp: Double?,
        @SerializedName("customer_ids")
        var customerIds: CustomerIds? = CustomerIds(),
        var properties: HashMap<String, String>? = null,
        var errors: HashMap<String, String>? = null
) : DatabaseItem