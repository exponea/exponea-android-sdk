package com.exponea.sdk.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.exponea.sdk.util.currentTimeSeconds
import java.util.UUID
import kotlin.collections.HashMap

@Entity(tableName = "exported_event")
internal data class ExportedEvent(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "tries") var tries: Int = 0,
    @ColumnInfo(name = "project_id") var projectId: String,
    @ColumnInfo(name = "route") var route: Route?,
    @ColumnInfo(name = "should_be_skipped") var shouldBeSkipped: Boolean = false,
    @ColumnInfo(name = "exponea_project") var exponeaProject: ExponeaProject? = null,
    @ColumnInfo(name = "event_type") var type: String? = null,
    @ColumnInfo(name = "timestamp") var timestamp: Double? = currentTimeSeconds(),
    @ColumnInfo(name = "age") var age: Double? = null,
    @ColumnInfo(name = "customer_ids") var customerIds: HashMap<String, String?>? = null,
    @ColumnInfo(name = "properties") var properties: HashMap<String, Any>? = null,
    @ColumnInfo(name = "sdk_event_type") var sdkEventType: String? = null
)
