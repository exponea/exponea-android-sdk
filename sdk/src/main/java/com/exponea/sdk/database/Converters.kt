package com.exponea.sdk.database

import androidx.room.TypeConverter
import com.exponea.sdk.models.IntegrationConfigType
import com.exponea.sdk.models.IntegrationConfiguration
import com.exponea.sdk.models.Route
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.fromJson
import com.google.gson.Gson

internal class Converters {

    private val separator = "§§§§§"

    @TypeConverter
    fun fromRoute(value: Route?): String {
        if (value == null) return ""
        return value.name
    }

    @TypeConverter
    fun toRoute(value: String): Route? {
        if (value.isEmpty()) return null
        return Route.valueOf(value)
    }

    @TypeConverter
    fun fromIntegrationConfiguration(value: IntegrationConfiguration?): String {
        return value?.let {
            it.integrationId + separator + it.authorization + separator + it.baseUrl + separator + it.type.name
        } ?: ""
    }

    @TypeConverter
    fun toIntegrationConfiguration(value: String): IntegrationConfiguration? {
        if (value.isEmpty()) return null
        val parts = value.split(separator)
        return if (parts.size < 3) {
            null
        } else {
            IntegrationConfiguration(
                integrationId = parts[0],
                authorization = if (parts[1] == "null") null else parts[1],
                baseUrl = parts[2],
                type = if (parts.size == 3)
                    IntegrationConfigType.PROJECT
                else IntegrationConfigType.valueOf(parts[3])
            )
        }
    }

    @TypeConverter
    fun toAnyMap(value: String?): HashMap<String, Any>? {
        if (value == null || value.isEmpty()) return null
        try {
            return Gson().fromJson<HashMap<String, Any>>(value)
        } catch (ex: Exception) {
            Logger.e(this, ex.message ?: "Unable to deserialize the map", ex)
        }
        return null
    }

    @TypeConverter
    fun fromAnyMap(data: HashMap<String, Any>?): String? {
        if (data == null) return ""
        try {
            return Gson().toJson(data)
        } catch (ex: Exception) {
            Logger.e(this, ex.message ?: "Unable to serialize the map", ex)
        }
        return null
    }

    @TypeConverter
    fun toStringMap(value: String?): HashMap<String, String>? {
        if (value == null || value.isEmpty()) return null
        try {
            return Gson().fromJson<HashMap<String, String>>(value)
        } catch (ex: Exception) {
            Logger.e(this, ex.message ?: "Unable to deserialize the map", ex)
        }
        return null
    }

    @TypeConverter
    fun fromStringMap(data: HashMap<String, String>?): String? {
        if (data == null) return ""
        try {
            return Gson().toJson(data)
        } catch (ex: Exception) {
            Logger.e(this, ex.message ?: "Unable to serialize the map", ex)
        }
        return null
    }

    @TypeConverter
    fun toOptionalStringMap(value: String?): HashMap<String, String?>? {
        if (value == null || value.isEmpty()) return null
        try {
            return Gson().fromJson<HashMap<String, String?>>(value)
        } catch (ex: Exception) {
            Logger.e(this, ex.message ?: "Unable to deserialize the map", ex)
        }
        return null
    }

    @TypeConverter
    fun fromOptionalStringMap(data: HashMap<String, String?>?): String? {
        if (data == null) return ""
        try {
            return Gson().toJson(data)
        } catch (ex: Exception) {
            Logger.e(this, ex.message ?: "Unable to serialize the map", ex)
        }
        return null
    }
}
