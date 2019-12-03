package com.exponea.sdk.models

import com.exponea.sdk.util.asOptionalString
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

data class CustomerRecommendationResponse(
    val success: Boolean,
    val value: ArrayList<CustomerRecommendation>?,
    val error: String?
)

/**
 * Data contains catalogue data fields.
 * Fields are JsonElements as received from the server.
 * Their type should correspond to type set when creating the catalogue.
 */
data class CustomerRecommendation(
    val engineName: String,
    val itemId: String,
    val recommendationId: String,
    val recommendationVariantId: String?,
    val data: Map<String, JsonElement>
)

class CustomerRecommendationDeserializer() : JsonDeserializer<CustomerRecommendation> {
    companion object {
        val baseKeys = arrayListOf("engine_name", "item_id", "recommendation_id", "recommendation_variant_id")
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): CustomerRecommendation? {
        val jsonObject = json.asJsonObject
        return CustomerRecommendation(
            jsonObject["engine_name"].asString,
            jsonObject["item_id"].asString,
            jsonObject["recommendation_id"].asString,
            jsonObject["recommendation_variant_id"].asOptionalString,
            hashMapOf(
                *jsonObject
                    .entrySet()
                    .filter { entry -> !baseKeys.contains(entry.key) }
                    .map { entry -> entry.key to entry.value }
                    .toTypedArray()
            )
        )
    }
}
