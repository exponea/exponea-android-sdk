package com.exponea.sdk.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.IntegrationConfigType
import com.exponea.sdk.models.IntegrationConfiguration
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.Route
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FlushManagerTest : ExponeaSDKTest() {
    private val testBaseUrl = "mock_base_url.com"
    private val projectToken = "mock_project_token"
    private val projectId = "old-project-id"
    private val authorization = "mock_auth"
    private val configuration = ExponeaConfiguration()
    private lateinit var context: Context
    private lateinit var manager: FlushManager
    private lateinit var repo: EventRepository
    private lateinit var connectionManager: ConnectionManager
    private lateinit var service: ExponeaService

    /** Event types in the order they were posted. */
    private val sentTypes = CopyOnWriteArrayList<String?>()

    /** Response code to answer with per event type, [defaultResponseCode] when not listed. */
    private val responseCodes = mutableMapOf<String, Int>()

    /** Event types whose request fails with an [IOException] instead of getting a response. */
    private val networkErrorTypes = mutableSetOf<String>()

    private var defaultResponseCode = 200

    /**
     * Sets up the flush over the real repository, with a service that answers every posted event with
     * the code from [responseCodes] (or [defaultCode]), fails it for [networkErrorTypes] and records
     * its type to [sentTypes].
     */
    private fun setup(connected: Boolean = true, defaultCode: Int = 200) {
        defaultResponseCode = defaultCode
        context = ApplicationProvider.getApplicationContext()

        connectionManager = mockk()
        every { connectionManager.isConnectedToInternet() } returns connected
        service = mockk(relaxed = true)
        every { service.postEvent(any<ProjectConfig>(), any(), any()) } answers {
            val event = secondArg<Event>()
            sentTypes.add(event.type)
            if (event.type in networkErrorTypes) {
                callFailingWithNetworkError()
            } else {
                callWithCode(responseCodes[event.type] ?: defaultResponseCode)
            }
        }
        repo = EventRepositoryImpl(context)
        repo.clear()
        manager = newManager(repo)
    }

    private fun newManager(
        repository: EventRepository,
        onEventUploaded: (ExportedEvent) -> Unit = {}
    ) = FlushManagerImpl(
        configuration,
        repository,
        service,
        connectionManager,
        mockk(relaxed = true),
        onEventUploaded
    )

    private fun callWithCode(code: Int): Call {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body("".toResponseBody())
                .build()
        }.build()
        return client.newCall(Request.Builder().url("http://localhost/").get().build())
    }

    private fun callFailingWithNetworkError(): Call {
        val client = OkHttpClient.Builder().addInterceptor { throw IOException("Network is unreachable") }.build()
        return client.newCall(Request.Builder().url("http://localhost/").get().build())
    }

    private fun flushAndAwait(flushManager: FlushManager = manager): Result<Unit> {
        val result = AtomicReference<Result<Unit>>()
        val done = CountDownLatch(1)
        flushManager.flushData {
            result.set(it)
            done.countDown()
        }
        assertThat("Flush did not finish", done.await(30, TimeUnit.SECONDS), equalTo(true))
        return result.get()
    }

    private fun createTestEvent(includeProject: Boolean, type: String? = "test_event"): ExportedEvent {
        val event = ExportedEvent(
                projectId = "old-project-id",
                type = type,
                timestamp = System.currentTimeMillis() / 1000.0,
                customerIds = hashMapOf(),
                properties = hashMapOf("property" to "value"),
                route = Route.TRACK_EVENTS,
                integrationConfiguration = if (includeProject)
                    IntegrationConfiguration(projectToken, testBaseUrl, authorization, IntegrationConfigType.PROJECT)
                else null
        )
        repo.add(event)
        return event
    }

    private fun buildEvent(
        type: String,
        timestamp: Double,
        route: Route? = Route.TRACK_EVENTS,
        tries: Int = 0
    ) = ExportedEvent(
        projectId = "project",
        type = type,
        timestamp = timestamp,
        customerIds = hashMapOf(),
        properties = hashMapOf("property" to "value"),
        route = route,
        tries = tries,
        integrationConfiguration = IntegrationConfiguration(
            "token",
            "http://localhost",
            "auth",
            IntegrationConfigType.PROJECT
        )
    )

    /**
     * Flushes the single stored event, expects it to be uploaded and returns what was posted
     * as the integration config and the event.
     */
    private fun flushAndCaptureSentEvent(): Pair<ProjectConfig, Event> {
        assertThat(flushAndAwait().exceptionOrNull(), nullValue())
        assertThat(repo.count(), equalTo(0))
        val projectConfigSlot = slot<ProjectConfig>()
        val eventSlot = slot<Event>()
        verify {
            service.postEvent(
                capture(projectConfigSlot),
                capture(eventSlot),
                true
            )
        }
        return projectConfigSlot.captured to eventSlot.captured
    }

    private fun assertSentWithTestProject(stored: ExportedEvent, projectConfig: ProjectConfig, sent: Event) {
        assertThat(projectConfig.baseUrl, equalTo(testBaseUrl))
        assertThat(projectConfig.projectToken, equalTo(projectToken))
        assertThat(projectConfig.authorization, equalTo(authorization))
        assertThat(sent.type, equalTo(stored.type))
        assertThat(sent.timestamp, notNullValue())
        assertThat(sent.timestamp, equalTo(stored.timestamp))
    }

    @Test
    fun `should flush event`() {
        setup()
        val testEvent = createTestEvent(true)

        val (projectConfig, sentEvent) = flushAndCaptureSentEvent()

        assertSentWithTestProject(testEvent, projectConfig, sentEvent)
    }

    @Test
    fun `should flush old event without exponea project`() {
        setup()
        val testEvent = createTestEvent(false)

        val (projectConfig, sentEvent) = flushAndCaptureSentEvent()

        assertThat(projectConfig.baseUrl, equalTo(Constants.Repository.baseURL))
        assertThat(projectConfig.projectToken, equalTo(projectId))
        assertThat(projectConfig.authorization, nullValue())
        assertThat(sentEvent.type, equalTo(testEvent.type))
        assertThat(sentEvent.timestamp, equalTo(testEvent.timestamp))
    }

    @Test
    fun `should fail to flush without internet connection`() {
        setup(connected = false)
        createTestEvent(true)
        every { connectionManager.isConnectedToInternet() } returns false
        assertThat(flushAndAwait().exceptionOrNull(), notNullValue())
        assertThat(repo.count(), equalTo(1))
        assertThat(repo.all().single().tries, equalTo(0))
    }

    @Test
    fun `should increase tries on flush failure`() {
        setup(defaultCode = 400)
        createTestEvent(true)

        assertThat(flushAndAwait().exceptionOrNull(), notNullValue())
        assertThat(repo.count(), equalTo(1))
        assertThat(repo.all().single().tries, equalTo(1))
    }

    @Test
    fun `should only flush once`() {
        Exponea.loggerLevel = Logger.Level.VERBOSE
        setup(defaultCode = 400)
        createTestEvent(true)

        val testRunMaxMillis = 10000L
        val flushInvokeTries = 10
        // counts down once per callback: 1 for the first flush + flushInvokeTries for queued ones
        val allCallbacksDone = CountDownLatch(1 + flushInvokeTries)
        val firstFlushBlocking = CountDownLatch(1)
        val unblockFirstFlush = CountDownLatch(1)

        every { connectionManager.isConnectedToInternet() } answers {
            firstFlushBlocking.countDown()
            assertTrue(unblockFirstFlush.await(testRunMaxMillis, TimeUnit.MILLISECONDS))
            true
        }

        // Start the first flush — it will block inside isConnectedToInternet
        thread(start = true) {
            manager.flushData {
                assertTrue(it.isFailure)
                allCallbacksDone.countDown()
            }
        }

        // Wait until the first flush is blocking in isConnectedToInternet
        assertTrue(firstFlushBlocking.await(testRunMaxMillis, TimeUnit.MILLISECONDS))

        // Start concurrent flushes — their callbacks are queued, not fired immediately
        repeat(flushInvokeTries) { _ ->
            thread(start = true) {
                manager.flushData {
                    // Callback fires when the ongoing flush finishes, not with "already in progress"
                    assertTrue(it.isFailure)
                    assertNotEquals("Flushing already in progress", it.exceptionOrNull()?.localizedMessage)
                    allCallbacksDone.countDown()
                }
            }
        }

        // Give concurrent threads time to queue their callbacks before releasing the first flush
        Thread.sleep(200)

        // Unblock the first flush
        unblockFirstFlush.countDown()

        // Wait for all callbacks (first flush + queued) to fire
        assertTrue(allCallbacksDone.await(testRunMaxMillis, TimeUnit.MILLISECONDS))

        // Verify that only one network request was made
        verify(exactly = 1) {
            service.postEvent(any<ProjectConfig>(), any(), true)
        }
    }

    @Test
    fun `should post timestamp when tracking push notifications`() {
        setup()
        val testEvent = createTestEvent(true, type = Constants.EventTypes.push)

        val (projectConfig, sentEvent) = flushAndCaptureSentEvent()

        assertSentWithTestProject(testEvent, projectConfig, sentEvent)
    }

    @Test
    fun `should send backlog oldest first and empty the queue`() {
        setup()
        repo.add(buildEvent("c", timestamp = 300.0))
        repo.add(buildEvent("a", timestamp = 100.0))
        repo.add(buildEvent("d", timestamp = 400.0))
        repo.add(buildEvent("b", timestamp = 200.0))

        val result = flushAndAwait()

        assertThat(result.exceptionOrNull(), nullValue())
        assertThat(sentTypes, contains<String?>("a", "b", "c", "d"))
        assertThat(repo.count(), equalTo(0))
    }

    @Test
    fun `should succeed for empty queue`() {
        setup()

        val result = flushAndAwait()

        assertThat(result.exceptionOrNull(), nullValue())
        assertThat(sentTypes, empty())
    }

    @Test
    fun `should skip event for the rest of the run on server error and keep it queued`() {
        setup()
        responseCodes["b"] = 503
        repo.add(buildEvent("a", timestamp = 100.0))
        repo.add(buildEvent("b", timestamp = 200.0))
        repo.add(buildEvent("c", timestamp = 300.0))

        val result = flushAndAwait()

        assertThat(result.exceptionOrNull(), notNullValue())
        // b is attempted exactly once, the events behind it are still sent
        assertThat(sentTypes, contains<String?>("a", "b", "c"))
        val remaining = repo.all()
        assertThat(remaining.map { it.type }, contains<String?>("b"))
        // 5xx is not counted as a try
        assertThat(remaining.single().tries, equalTo(0))
    }

    @Test
    fun `should reset skip flags after run so the next flush retries the event`() {
        setup()
        responseCodes["b"] = 500
        repo.add(buildEvent("a", timestamp = 100.0))
        repo.add(buildEvent("b", timestamp = 200.0))

        assertThat(flushAndAwait().exceptionOrNull(), notNullValue())
        assertThat(repo.all().filter { it.shouldBeSkipped }, empty())

        sentTypes.clear()
        responseCodes["b"] = 200
        assertThat(flushAndAwait().exceptionOrNull(), nullValue())
        assertThat(sentTypes, contains<String?>("b"))
        assertThat(repo.count(), equalTo(0))
    }

    @Test
    fun `should count a try on client error and not block following events`() {
        setup()
        responseCodes["a"] = 400
        repo.add(buildEvent("a", timestamp = 100.0))
        repo.add(buildEvent("b", timestamp = 200.0))

        val result = flushAndAwait()

        assertThat(result.exceptionOrNull(), notNullValue())
        assertThat(sentTypes, contains<String?>("a", "b"))
        val remaining = repo.all().single()
        assertThat(remaining.type, equalTo("a"))
        assertThat(remaining.tries, equalTo(1))
        assertThat(remaining.shouldBeSkipped, equalTo(false))
    }

    @Test
    fun `should drop event on client error once max tries is reached`() {
        setup()
        responseCodes["a"] = 400
        repo.add(buildEvent("a", timestamp = 100.0, tries = configuration.maxTries - 1))
        repo.add(buildEvent("b", timestamp = 200.0))

        flushAndAwait()

        assertThat(sentTypes, contains<String?>("a", "b"))
        assertThat(repo.count(), equalTo(0))
    }

    @Test
    fun `should remove events with invalid route and still send the others`() {
        setup()
        repo.add(buildEvent("bad1", timestamp = 100.0, route = null))
        repo.add(buildEvent("a", timestamp = 200.0))
        repo.add(buildEvent("bad2", timestamp = 300.0, route = null))
        repo.add(buildEvent("b", timestamp = 400.0))

        val result = flushAndAwait()

        assertThat(result.exceptionOrNull(), nullValue())
        assertThat(sentTypes, contains<String?>("a", "b"))
        assertThat(repo.count(), equalTo(0))
    }

    @Test
    fun `should flush event tracked during flush in the same run`() {
        setup()
        repo.add(buildEvent("a", timestamp = 100.0))
        // a new event arrives while the first one is being sent
        every { service.postEvent(any<ProjectConfig>(), any(), any()) } answers {
            val sent = secondArg<Event>()
            sentTypes.add(sent.type)
            if (sent.type == "a") repo.add(buildEvent("late", timestamp = 200.0))
            callWithCode(200)
        }

        assertThat(flushAndAwait().exceptionOrNull(), nullValue())

        assertThat(sentTypes, contains<String?>("a", "late"))
        assertThat(repo.count(), equalTo(0))
    }

    @Test
    fun `should only flush events added through temporary repository`() {
        setup()
        // pre-existing event, e.g. stored by the main SDK instance
        repo.add(buildEvent("existing", timestamp = 100.0))
        val temporary = TemporaryEventRepositoryImpl(context)
        temporary.add(buildEvent("mine", timestamp = 200.0))

        val result = flushAndAwait(newManager(temporary))

        assertThat(result.exceptionOrNull(), nullValue())
        assertThat(sentTypes, contains<String?>("mine"))
        assertThat(repo.all().map { it.type }, contains<String?>("existing"))
    }

    @Test
    fun `should not touch pre-existing events when temporary repository events fail`() {
        setup()
        responseCodes["mine"] = 503
        repo.add(buildEvent("existing", timestamp = 100.0))
        val temporary = TemporaryEventRepositoryImpl(context)
        temporary.add(buildEvent("mine", timestamp = 200.0))

        val result = flushAndAwait(newManager(temporary))

        assertThat(result.exceptionOrNull(), notNullValue())
        assertThat(sentTypes, contains<String?>("mine"))
        assertThat(repo.all().map { it.type }, containsInAnyOrder<String?>("existing", "mine"))
        assertThat(repo.all().single { it.type == "existing" }.shouldBeSkipped, equalTo(false))
    }

    @Test
    fun `should end the run with failure when sending throws unexpectedly`() {
        setup()
        repo.add(buildEvent("a", timestamp = 100.0))
        every { service.postEvent(any<ProjectConfig>(), any(), any()) } throws IllegalStateException("boom")

        val result = flushAndAwait()

        assertThat(result.exceptionOrNull(), notNullValue())
        assertThat(manager.isRunning, equalTo(false))
        // the event was not lost
        assertThat(repo.count(), equalTo(1))

        // and the next flush is not blocked by the failed one
        every { service.postEvent(any<ProjectConfig>(), any(), any()) } answers {
            sentTypes.add(secondArg<Event>().type)
            callWithCode(200)
        }
        assertThat(flushAndAwait().exceptionOrNull(), nullValue())
        assertThat(sentTypes, contains<String?>("a"))
        assertThat(repo.count(), equalTo(0))
    }

    @Test
    fun `should count a try when preparing request throws and not block following events`() {
        setup()
        repo.add(buildEvent("a", timestamp = 100.0))
        repo.add(buildEvent("b", timestamp = 200.0))
        every { service.postEvent(any<ProjectConfig>(), any(), any()) } answers {
            val event = secondArg<Event>()
            sentTypes.add(event.type)
            if (event.type == "a") throw IllegalArgumentException("malformed") else callWithCode(200)
        }

        val result = flushAndAwait()

        assertThat(result.exceptionOrNull(), notNullValue())
        assertThat(manager.isRunning, equalTo(false))
        assertThat(sentTypes, contains<String?>("a", "b"))
        val remaining = repo.all().single()
        assertThat(remaining.type, equalTo("a"))
        assertThat(remaining.tries, equalTo(1))
        assertThat(remaining.shouldBeSkipped, equalTo(false))
    }

    @Test
    fun `should drop event whose request cannot be prepared once max tries is reached`() {
        setup()
        repo.add(buildEvent("a", timestamp = 100.0, tries = configuration.maxTries - 1))
        every { service.postEvent(any<ProjectConfig>(), any(), any()) } throws IllegalArgumentException("malformed")

        flushAndAwait()

        assertThat(repo.count(), equalTo(0))
    }

    @Test
    fun `should end the run and not stay running when recording a response outcome throws`() {
        setup()
        repo.add(buildEvent("a", timestamp = 100.0))
        var failUpload = true
        val throwingManager = newManager(repo) { if (failUpload) throw IllegalStateException("host callback") }

        val result = flushAndAwait(throwingManager)

        assertThat(result.exceptionOrNull(), notNullValue())
        assertThat(throwingManager.isRunning, equalTo(false))
        assertThat(repo.count(), equalTo(0))

        // the next flush is not blocked
        failUpload = false
        assertThat(flushAndAwait(throwingManager).exceptionOrNull(), nullValue())
        assertThat(repo.count(), equalTo(0))
    }

    @Test
    fun `should count a try on network error and not block following events`() {
        setup()
        networkErrorTypes += "a"
        repo.add(buildEvent("a", timestamp = 100.0))
        repo.add(buildEvent("b", timestamp = 200.0))

        val result = flushAndAwait()

        assertThat(result.exceptionOrNull(), notNullValue())
        assertThat(sentTypes, contains<String?>("a", "b"))
        val remaining = repo.all().single()
        assertThat(remaining.type, equalTo("a"))
        assertThat(remaining.tries, equalTo(1))
        assertThat(remaining.shouldBeSkipped, equalTo(false))
    }

    @Test
    fun `should notify about uploaded events in the order they were sent`() {
        setup()
        val uploaded = CopyOnWriteArrayList<String?>()
        repo.add(buildEvent("b", timestamp = 200.0))
        repo.add(buildEvent("a", timestamp = 100.0))

        assertThat(flushAndAwait(newManager(repo) { uploaded.add(it.type) }).exceptionOrNull(), nullValue())

        assertThat(uploaded, contains<String?>("a", "b"))
    }

    @Test
    fun `should notify only about events that were uploaded`() {
        setup()
        val uploaded = CopyOnWriteArrayList<String?>()
        responseCodes["a"] = 400
        responseCodes["c"] = 503
        networkErrorTypes += "d"
        repo.add(buildEvent("a", timestamp = 100.0))
        repo.add(buildEvent("b", timestamp = 200.0))
        repo.add(buildEvent("c", timestamp = 300.0))
        repo.add(buildEvent("d", timestamp = 400.0))
        repo.add(buildEvent("e", timestamp = 500.0, route = null))

        flushAndAwait(newManager(repo) { uploaded.add(it.type) })

        assertThat(uploaded, contains<String?>("b"))
    }
}
