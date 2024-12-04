package com.exponea.sdk.database

import androidx.room.TypeConverter
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.Route
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.fromJson
import com.google.gson.Gson

class Converters {

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
    fun fromProject(value: ExponeaProject?): String {
        if (value == null) return ""
        return value.projectToken + separator + value.authorization + separator + value.baseUrl
    }

    @TypeConverter
    fun toProject(value: String): ExponeaProject? {
        if (value.isEmpty()) return null
        val parts = value.split(separator)
        return if (parts.size < 3) {
            null
        } else {
            val result = ExponeaProject(
                projectToken = parts[0],
                authorization = parts[1],
                baseUrl = parts[2],
                inAppContentBlockPlaceholdersAutoLoad = toStringList(parts.getOrNull(3))
            )
            return result
        }
    }

    @TypeConverter
    fun toStringList(source: String?): List<String> {
        if (source.isNullOrBlank()) {
            return emptyList()
        }
        try {
            return Gson().fromJson<List<String>>(source)
        } catch (ex: Exception) {
            Logger.e(this, ex.message ?: "Unable to deserialize the list", ex)
        }
        return emptyList()
    }

    @TypeConverter
    fun fromStringList(data: List<String>?): String? {
        if (data == null) return ""
        try {
            return Gson().toJson(data)
        } catch (ex: Exception) {
            Logger.e(this, ex.message ?: "Unable to serialize the list", ex)
        }
        return null
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
