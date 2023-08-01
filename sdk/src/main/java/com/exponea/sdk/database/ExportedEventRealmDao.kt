package com.exponea.sdk.database

import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.ExportedEventRealm
import com.exponea.sdk.models.toExportedEvent
import com.exponea.sdk.models.toRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy.ALL
import io.realm.kotlin.ext.query

internal class ExportedEventRealmDao : ExportedEventDao {

    var database: Realm

    companion object {
        internal const val DATABASE_NAME = "ExponeaEventRealm.db"
    }

    init {
        val realmConf = RealmConfiguration.Builder(setOf(ExportedEventRealm::class))
            .name(DATABASE_NAME)
            .schemaVersion(1)
            .build()
        database = Realm.open(realmConf)
    }

    override fun all(): List<ExportedEvent> {
        return database.query<ExportedEventRealm>().find().map { it.toExportedEvent() }
    }

    override fun count(): Long {
        return database.query<ExportedEventRealm>().count().find()
    }

    override fun get(id: String): ExportedEvent? {
        return database.query<ExportedEventRealm>("id == $0", id).first().find()?.toExportedEvent()
    }

    override fun add(item: ExportedEvent) {
        database.writeBlocking {
            copyToRealm(item.toRealm(), updatePolicy = ALL)
        }
    }

    override fun update(item: ExportedEvent) {
        database.writeBlocking {
            copyToRealm(item.toRealm(), updatePolicy = ALL)
        }
    }

    override fun delete(id: String) {
        database.writeBlocking {
            query<ExportedEventRealm>("id == $0", id).first().find()?.let {
                delete(it)
            }
        }
    }

    override fun clear() {
        database.writeBlocking {
            this.delete(ExportedEventRealm::class)
        }
    }
}
