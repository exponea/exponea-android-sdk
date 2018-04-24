package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

/*
{
 "customer_ids": {
  "cookie": "382d4221-3441-44b7-a676-3eb5f515157f"
 },
 "attributes": [
  {
   "type": "property",
   "property": "first_name"
  },
  {
   "type": "property",
   "property": "last_name"
  },
  {
   "type": "segmentation",
   "id": "592ff585fb60094e02bfaf6a"
  }
 ]
}
* */

data class CustomerAttributes(
        @SerializedName("customer_id")
        var customerId: HashMap<String, String>? = hashMapOf(),
        @SerializedName("attributes")
        var attributes: ArrayList(attributes)? = [attributes]
)



data class attributes(
    var type: HashMap<String, String>? = hashMapOf(),
    var value: HashMap<String, String>? = hashMapOf()
)
