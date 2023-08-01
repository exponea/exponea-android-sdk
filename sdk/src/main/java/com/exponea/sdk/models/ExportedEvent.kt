package com.exponea.sdk.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.exponea.sdk.database.Converters
import com.exponea.sdk.util.currentTimeSeconds
import io.realm.kotlin.types.RealmObject
import java.util.UUID

internal data class ExportedEvent(
    var id: String = UUID.randomUUID().toString(),
    var tries: Int = 0,
    var projectId: String,
    var route: Route?,
    var shouldBeSkipped: Boolean = false,
    var exponeaProject: ExponeaProject? = null,
    var type: String? = null,
    var timestamp: Double? = currentTimeSeconds(),
    var age: Double? = null,
    var customerIds: HashMap<String, String?>? = null,
    var properties: HashMap<String, Any>? = null
)

/**
 * ExportedEvent for Realm Database
 */
internal open class ExportedEventRealm : RealmObject {
    @io.realm.kotlin.types.annotations.PrimaryKey
    var id: String = UUID.randomUUID().toString()
    var tries: Int = 0
    var projectId: String? = null
    var route: String? = null
    var shouldBeSkipped: Boolean = false
    var exponeaProject: String? = null
    var type: String? = null
    var timestamp: Double? = currentTimeSeconds()
    var age: Double? = null
    var customerIds: String? = null
    var properties: String? = null
}

/**
 * ExportedEvent for Room Database
 */
@Entity(tableName = "exported_event")
internal data class ExportedEventRoom(
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
    @ColumnInfo(name = "properties") var properties: HashMap<String, Any>? = null
)

internal fun ExportedEvent.toRealm(): ExportedEventRealm {
    val source = this
    return ExportedEventRealm().apply {
        this.id = source.id
        this.tries = source.tries
        this.projectId = source.projectId
        this.route = source.route?.name
        this.shouldBeSkipped = source.shouldBeSkipped
        this.exponeaProject = Converters.instance.fromProject(source.exponeaProject)
        this.type = source.type
        this.timestamp = source.timestamp
        this.age = source.age
        this.customerIds = Converters.instance.fromStringMap(source.customerIds)
        this.properties = Converters.instance.fromAnyMap(source.properties)
    }
}

internal fun ExportedEventRealm.toExportedEvent(): ExportedEvent {
    return ExportedEvent(
        id = this.id,
        tries = this.tries,
        projectId = this.projectId!!,
        route = Converters.instance.toRoute(this.route ?: ""),
        shouldBeSkipped = this.shouldBeSkipped,
        exponeaProject = Converters.instance.toProject(this.exponeaProject ?: ""),
        type = this.type,
        timestamp = this.timestamp,
        age = this.age,
        customerIds = Converters.instance.toStringMap(this.customerIds),
        properties = Converters.instance.toAnyMap(this.properties)
    )
}
