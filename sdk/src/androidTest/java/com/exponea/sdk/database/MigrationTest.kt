package com.exponea.sdk.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.IntegrationConfigType
import com.exponea.sdk.models.IntegrationConfiguration
import com.exponea.sdk.models.Route
import com.exponea.sdk.util.currentTimeSeconds
import java.io.IOException
import java.util.UUID
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDbName = "MigrationTestExponeaEventDatabase"
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ExponeaDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1to2() {
        val testEvent = getTestEvent()
        val age = 2.0
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """INSERT INTO exported_event (id,
                                                    tries,
                                                    project_id,
                                                    route,
                                                    should_be_skipped,
                                                    event_type,
                                                    age,
                                                    timestamp)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                arrayOf(
                    testEvent.id,
                    testEvent.tries,
                    testEvent.projectId,
                    testEvent.route,
                    testEvent.shouldBeSkipped,
                    testEvent.type,
                    age,
                    testEvent.timestamp
                )
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(
            testDbName,
            2,
            true,
            ExponeaDatabase.migration1to2
        )

        db.query(
            """
                SELECT id,
                       tries,
                       project_id,
                       route,
                       should_be_skipped,
                       event_type,
                       age,
                       timestamp,
                       sdk_event_type
                FROM exported_event
                """
        ).use {
            assertThat(it.count, equalTo(1))

            it.moveToFirst()

            assertThat(it.getString(0), equalTo(testEvent.id))
            assertThat(it.getInt(1), equalTo(testEvent.tries))
            assertThat(it.getString(2), equalTo(testEvent.projectId))
            assertThat(it.getString(3), equalTo(testEvent.route?.name))
            assertThat(it.getInt(4), equalTo(1))
            assertThat(it.getString(5), equalTo(testEvent.type))
            assertThat(it.getDouble(6), equalTo(age))
            assertThat(it.getDouble(7), equalTo(testEvent.timestamp))
            assertThat(it.getString(8), equalTo(null))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate2to3() {
        val testEvent = getTestEvent()
        helper.createDatabase(testDbName, 2).apply {
            execSQL(
                """INSERT INTO exported_event (id,
                                                    tries,
                                                    project_id,
                                                    route,
                                                    should_be_skipped,
                                                    event_type,
                                                    age,
                                                    timestamp,
                                                    sdk_event_type)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                arrayOf(
                    testEvent.id,
                    testEvent.tries,
                    testEvent.projectId,
                    testEvent.route,
                    testEvent.shouldBeSkipped,
                    testEvent.type,
                    2.0,
                    testEvent.timestamp,
                    testEvent.sdkEventType
                )
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(testDbName, 3, true)

        db.query(
            """
                SELECT id,
                       tries,
                       project_id,
                       route,
                       should_be_skipped,
                       event_type,
                       timestamp,
                       sdk_event_type
                FROM exported_event
                """
        ).use {
            assertThat(it.count, equalTo(1))

            it.moveToFirst()

            assertThat(it.getString(0), equalTo(testEvent.id))
            assertThat(it.getInt(1), equalTo(testEvent.tries))
            assertThat(it.getString(2), equalTo(testEvent.projectId))
            assertThat(it.getString(3), equalTo(testEvent.route?.name))
            assertThat(it.getInt(4), equalTo(1))
            assertThat(it.getString(5), equalTo(testEvent.type))
            assertThat(it.getDouble(6), equalTo(testEvent.timestamp))
            assertThat(it.getString(7), equalTo(testEvent.sdkEventType))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3to4() {
        val testEvent = getTestEvent()
        val serializedExponeaProject =
            "512d0389-1395-46d6-a82a-60c57251e2bb§§§§§null§§§§§https://api.exponea.com"

        helper.createDatabase(testDbName, 3).apply {
            execSQL(
                """
                    INSERT INTO exported_event (id,
                                               tries,
                                               project_id,
                                               route,
                                               should_be_skipped,
                                               exponea_project,
                                               event_type,
                                               timestamp,
                                               sdk_event_type)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    testEvent.id,
                    testEvent.tries,
                    testEvent.projectId,
                    testEvent.route,
                    testEvent.shouldBeSkipped,
                    serializedExponeaProject,
                    testEvent.type,
                    testEvent.timestamp,
                    testEvent.sdkEventType
                )
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 4, true)

        db.query(
            """
                SELECT id,
                       tries,
                       project_id,
                       route,
                       should_be_skipped,
                       integration_config,
                       event_type,
                       timestamp,
                       sdk_event_type
                FROM exported_event
            """
        ).use {
            assertThat(it.count, equalTo(1))

            it.moveToFirst()

            assertThat(it.getString(0), equalTo(testEvent.id))
            assertThat(it.getInt(1), equalTo(testEvent.tries))
            assertThat(it.getString(2), equalTo(testEvent.projectId))
            assertThat(it.getString(3), equalTo(testEvent.route?.name))
            assertThat(it.getInt(4), equalTo(1))
            assertThat(it.getString(5), equalTo(serializedExponeaProject))
            assertThat(it.getString(6), equalTo(testEvent.type))
            assertThat(it.getDouble(7), equalTo(testEvent.timestamp))
            assertThat(it.getString(8), equalTo(testEvent.sdkEventType))
        }

        val eventWithNullProjectId = ExportedEvent(
            id = UUID.randomUUID().toString(),
            tries = 1,
            projectId = null,
            route = Route.TRACK_EVENTS,
            shouldBeSkipped = true,
            type = "page_view",
            timestamp = currentTimeSeconds(),
            sdkEventType = "TRACK_EVENT"
        )

        db.apply {
            execSQL(
                """
                    INSERT INTO exported_event (id,
                                               tries,
                                               project_id,
                                               route,
                                               should_be_skipped,
                                               event_type,
                                               timestamp,
                                               sdk_event_type)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    eventWithNullProjectId.id,
                    eventWithNullProjectId.tries,
                    eventWithNullProjectId.projectId,
                    eventWithNullProjectId.route,
                    eventWithNullProjectId.shouldBeSkipped,
                    eventWithNullProjectId.type,
                    eventWithNullProjectId.timestamp,
                    eventWithNullProjectId.sdkEventType
                )
            )
        }

        db.query(
            """
                SELECT id,
                       tries,
                       project_id,
                       route,
                       should_be_skipped,
                       event_type,
                       timestamp,
                       sdk_event_type
                FROM exported_event
                WHERE id = '${eventWithNullProjectId.id}'
            """
        ).use {
            assertThat(it.count, equalTo(1))

            it.moveToFirst()

            assertThat(it.getString(0), equalTo(eventWithNullProjectId.id))
            assertThat(it.getInt(1), equalTo(eventWithNullProjectId.tries))
            assertThat(it.getString(2), equalTo(null))
            assertThat(it.getString(3), equalTo(eventWithNullProjectId.route?.name))
            assertThat(it.getInt(4), equalTo(1))
            assertThat(it.getString(5), equalTo(eventWithNullProjectId.type))
            assertThat(it.getDouble(6), equalTo(eventWithNullProjectId.timestamp))
            assertThat(it.getString(7), equalTo(eventWithNullProjectId.sdkEventType))
        }
    }

    /**
     * Full-chain migration test: V1 to latest (currently V4).
     * Update the target version and assertions whenever a new schema version is added.
     * The goal is to always cover the longest possible migration path (V1 to current).
     */
    @Test
    @Throws(IOException::class)
    fun migrate1toLatest() {
        val latestDbVersion = 4

        val testEvent = getTestEvent()
        val age = 2.0
        val serializedExponeaProject =
            "512d0389-1395-46d6-a82a-60c57251e2bb§§§§§null§§§§§https://api.exponea.com"
        val customerIds = """{"registered":"jane.doe@example.com"}"""
        val properties = """{"first_name":"Jane","age":32}"""

        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """INSERT INTO exported_event (id,
                                               tries,
                                               project_id,
                                               route,
                                               should_be_skipped,
                                               exponea_project,
                                               event_type,
                                               timestamp,
                                               age,
                                               customer_ids,
                                               properties)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    testEvent.id,
                    testEvent.tries,
                    testEvent.projectId,
                    testEvent.route,
                    testEvent.shouldBeSkipped,
                    serializedExponeaProject,
                    testEvent.type,
                    testEvent.timestamp,
                    age,
                    customerIds,
                    properties
                )
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName,
            latestDbVersion,
            true,
            ExponeaDatabase.migration1to2
        )

        db.query(
            """
                SELECT id,
                       tries,
                       project_id,
                       route,
                       should_be_skipped,
                       integration_config,
                       event_type,
                       timestamp,
                       customer_ids,
                       properties,
                       sdk_event_type
                FROM exported_event
            """
        ).use {
            assertThat(it.count, equalTo(1))
            it.moveToFirst()

            assertThat(it.getString(0), equalTo(testEvent.id))
            assertThat(it.getInt(1), equalTo(testEvent.tries))
            assertThat(it.getString(2), equalTo(testEvent.projectId))
            assertThat(it.getString(3), equalTo(testEvent.route?.name))
            assertThat(it.getInt(4), equalTo(1))
            assertThat(it.getString(5), equalTo(serializedExponeaProject))
            assertThat(it.getString(6), equalTo(testEvent.type))
            assertThat(it.getDouble(7), equalTo(testEvent.timestamp))
            assertThat(it.getString(8), equalTo(customerIds))
            assertThat(it.getString(9), equalTo(properties))
            assertThat(it.getString(10), equalTo(null))
        }

        db.apply {
            execSQL(
                """
                    INSERT INTO exported_event (id,
                                               tries,
                                               project_id,
                                               route,
                                               should_be_skipped,
                                               event_type,
                                               timestamp,
                                               sdk_event_type)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    "null-project-id-test",
                    0,
                    null,
                    Route.TRACK_EVENTS.name,
                    false,
                    "test_event",
                    currentTimeSeconds(),
                    null
                )
            )
        }

        db.query(
            """
                SELECT project_id FROM exported_event WHERE id = 'null-project-id-test'
            """
        ).use {
            assertThat(it.count, equalTo(1))
            it.moveToFirst()
            assertThat(it.getString(0), equalTo(null))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3to4_converterIntegration() {
        val eventId = UUID.randomUUID().toString()
        val serializedOldProject =
            "proj-token§§§§§Token auth-token§§§§§https://api.exponea.com"

        helper.createDatabase(testDbName, 3).apply {
            execSQL(
                """
                    INSERT INTO exported_event (id,
                                               tries,
                                               project_id,
                                               route,
                                               should_be_skipped,
                                               exponea_project,
                                               event_type,
                                               timestamp,
                                               sdk_event_type)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    eventId,
                    0,
                    "test-project",
                    Route.TRACK_EVENTS.name,
                    false,
                    serializedOldProject,
                    "page_view",
                    currentTimeSeconds(),
                    "TRACK_EVENT"
                )
            )
            close()
        }

        helper.runMigrationsAndValidate(testDbName, 4, true)

        val roomDb = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ExponeaDatabase::class.java,
            testDbName
        ).addMigrations(ExponeaDatabase.migration1to2).build()

        try {
            val event = roomDb.exportedEventDao().get(eventId)
            assertThat(event, notNullValue())
            val config = event!!.integrationConfiguration
            assertThat(config, notNullValue())
            assertThat(config!!.integrationId, equalTo("proj-token"))
            assertThat(config.authorization, equalTo("Token auth-token"))
            assertThat(config.baseUrl, equalTo("https://api.exponea.com"))
            assertThat(config.type, equalTo(IntegrationConfigType.PROJECT))

            val streamEvent = ExportedEvent(
                id = UUID.randomUUID().toString(),
                tries = 0,
                route = Route.TRACK_EVENTS,
                integrationConfiguration = IntegrationConfiguration(
                    integrationId = "stream-id",
                    baseUrl = "https://stream.exponea.com",
                    authorization = null,
                    type = IntegrationConfigType.STREAM
                ),
                type = "purchase",
                sdkEventType = "TRACK_EVENT"
            )
            roomDb.exportedEventDao().add(streamEvent)

            val readBack = roomDb.exportedEventDao().get(streamEvent.id)
            assertThat(readBack, notNullValue())
            val streamConfig = readBack!!.integrationConfiguration
            assertThat(streamConfig, notNullValue())
            assertThat(streamConfig!!.integrationId, equalTo("stream-id"))
            assertThat(streamConfig.baseUrl, equalTo("https://stream.exponea.com"))
            assertThat(streamConfig.authorization, nullValue())
            assertThat(streamConfig.type, equalTo(IntegrationConfigType.STREAM))
        } finally {
            roomDb.close()
        }
    }

    private fun getTestEvent() = ExportedEvent(
        id = UUID.randomUUID().toString(),
        tries = 3,
        projectId = "Test Project ID",
        route = Route.TRACK_EVENTS,
        shouldBeSkipped = true,
        type = "page_view",
        timestamp = currentTimeSeconds(),
        sdkEventType = "TRACK_EVENT"
    )
}
