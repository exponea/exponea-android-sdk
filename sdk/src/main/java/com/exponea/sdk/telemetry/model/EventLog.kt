package com.exponea.sdk.telemetry.model

import java.util.UUID

internal data class EventLog(
    val id: String,
    val name: String,
    val timestampMS: Long,
    val properties: Map<String, String>
) {
    constructor(name: String, properties: Map<String, String>) : this(
        id = UUID.randomUUID().toString(),
        name = name,
        timestampMS = System.currentTimeMillis(),
        properties = properties
    )
}