package com.exponea.sdk.database

import androidx.room.TypeConverter
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.Route
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
                ExponeaProject(projectToken = parts[0], authorization = parts[1], baseUrl = parts[2])
            }
        }

        @TypeConverter
        fun toAnyMap(value: String): HashMap<String, Any>? {
            if (value.isEmpty()) return null
            return Gson().fromJson<HashMap<String, Any>>(value)
        }

        @TypeConverter
        fun fromAnyMap(data: HashMap<String, Any>?): String {
            if (data == null) return ""
            return Gson().toJson(data)
        }

        @TypeConverter
        fun toStringMap(value: String): HashMap<String, String>? {
            if (value.isEmpty()) return null
            return Gson().fromJson<HashMap<String, String>>(value)
        }

        @TypeConverter
        fun fromStringMap(data: HashMap<String, String>?): String {
            if (data == null) return ""
            return Gson().toJson(data)
        }
    }
