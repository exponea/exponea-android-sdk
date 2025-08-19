package com.exponea.sdk.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.Route
import com.exponea.sdk.util.currentTimeSeconds
import java.io.IOException
import java.util.UUID
import org.hamcrest.CoreMatchers.equalTo
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
