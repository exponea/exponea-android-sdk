package com.exponea.sdk.models

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * Custom JSON adapter to serialize and deserialize IntegrationConfig polymorphic types.
 */
internal class IntegrationConfigJsonAdapter : JsonSerializer<IntegrationConfig>, JsonDeserializer<IntegrationConfig> {

    companion object {
        private const val TYPE_KEY = "type"
        private const val PROJECT_CONFIG_VALUE = "PROJECT"
        private const val DATA_HUB_CONFIG_VALUE = "DATA_HUB"
    }

    override fun serialize(
        src: IntegrationConfig,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement? {
        val jsonElement = Gson().toJsonTree(src)
        jsonElement.getAsJsonObject().addProperty(
            TYPE_KEY,
            when (src) {
                is ProjectConfig -> PROJECT_CONFIG_VALUE
                is StreamConfig -> DATA_HUB_CONFIG_VALUE
            }
        )
        return jsonElement
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): IntegrationConfig? {
        val jsonObject = json!!.getAsJsonObject()
        val typeName = jsonObject.get(TYPE_KEY).asString

        try {
            val cls = when (typeName) {
                PROJECT_CONFIG_VALUE -> ProjectConfig::class.java
                DATA_HUB_CONFIG_VALUE -> StreamConfig::class.java
                else -> throw JsonParseException("Unknown type: $typeName")
            }
            return Gson().fromJson(json, cls)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e)
        }
    }
}
