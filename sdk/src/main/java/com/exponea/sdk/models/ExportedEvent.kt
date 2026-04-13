package com.exponea.sdk.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.exponea.sdk.util.currentTimeSeconds
import java.util.UUID
import kotlin.collections.HashMap

@Entity(tableName = "exported_event")
internal data class ExportedEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "tries") var tries: Int = 0,
    @Deprecated("Obsolete property, will be removed in next versions.")
    @ColumnInfo(name = "project_id") val projectId: String? = null,
    @ColumnInfo(name = "route") val route: Route?,
    @ColumnInfo(name = "should_be_skipped") var shouldBeSkipped: Boolean = false,
    @ColumnInfo(name = "integration_config") val integrationConfiguration: IntegrationConfiguration? = null,
    @ColumnInfo(name = "event_type") val type: String? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Double? = currentTimeSeconds(),
    @ColumnInfo(name = "customer_ids") val customerIds: HashMap<String, String?>? = null,
    @ColumnInfo(name = "properties") val properties: HashMap<String, Any>? = null,
    @ColumnInfo(name = "sdk_event_type") val sdkEventType: String? = null
)

internal data class IntegrationConfiguration(
    val integrationId: String,
    val baseUrl: String,
    val authorization: String?,
    val type: IntegrationConfigType
)

internal enum class IntegrationConfigType {
    PROJECT,
    STREAM
}
