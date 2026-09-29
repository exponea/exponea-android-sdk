package com.exponea.example.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { prettyPrint = true }

fun Map<String, Any>.asJson(): String = json.encodeToString(
    mapValues { (_, value) -> value.toString() }
)
