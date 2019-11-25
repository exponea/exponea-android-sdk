package com.exponea.sdk.telemetry.model

import java.util.UUID

internal data class EventLog(
    val id: String,
    val name: String,
    val timestampMS: Long,
    val runId: String,
    val properties: Map<String, String>
) {
    constructor(name: String, properties: Map<String, String>, runId: String) : this(
        id = UUID.randomUUID().toString(),
        name = name,
        runId = runId,
        timestampMS = System.currentTimeMillis(),
        properties = properties
    )
}