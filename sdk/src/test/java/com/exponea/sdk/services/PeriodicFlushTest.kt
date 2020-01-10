package com.exponea.sdk.services

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.FlushPeriod
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkConstructor
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PeriodicFlushTest : ExponeaSDKTest() {

    @Before
    fun before() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext(),
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )
    }

    @After
    fun after() {
        clearAllMocks()
    }

    private fun getUniqueWorkInfo(workName: String): WorkInfo? {
        val workInfos = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
            .getWorkInfosForUniqueWork(workName).get()
        return workInfos.firstOrNull()
    }

    private fun executeWork(): WorkInfo {
        val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
        val request = OneTimeWorkRequest.Builder(ExponeaPeriodicFlushWorker::class.java).build()
        workManager.enqueue(request).result.get()

        return workManager.getWorkInfoById(request.id).get()
    }

    @Test
    fun `worker should fail when Exponea SDK is not initialized`() {
        assertEquals(WorkInfo.State.FAILED, executeWork().state)
    }

    @Test
    fun `worker should flush in periodic mode`() {
        mockkConstructor(FlushManagerImpl::class)
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.PERIOD

        var flushCalled = false
        every {
            anyConstructed<FlushManagerImpl>().flushData()
        } answers {
            flushCalled = true
            Exponea.component.flushManager.onFlushFinishListener?.invoke()
        }

        assertEquals(WorkInfo.State.SUCCEEDED, executeWork().state)
        assertTrue(flushCalled)
    }

    @Test
    fun `worker should fail and not flush in manual mode`() {
        mockkConstructor(FlushManagerImpl::class)
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL

        var flushCalled = false
        every {
            anyConstructed<FlushManagerImpl>().flushData()
        } answers {
            flushCalled = true
            Exponea.component.flushManager.onFlushFinishListener?.invoke()
        }

        assertEquals(WorkInfo.State.FAILED, executeWork().state)
        assertFalse(flushCalled)
    }

    @Test
    fun `should start periodic flush when flushMode is period`() {
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.PERIOD

        assertNotNull(getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME))
    }

    @Test
    fun `should not start periodic flush when flushMode isn't period`() {
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        waitUntilFlushed()

        FlushMode.values().forEach { flushMode ->
            if (flushMode == FlushMode.PERIOD) return@forEach
            Exponea.flushMode = flushMode
            assertNull(getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME))
        }
    }

    @Test
    fun `should stop periodic flush`() {
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.PERIOD

        assertNotNull(getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME))
        assertEquals(getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME)?.state, WorkInfo.State.ENQUEUED)

        Exponea.flushMode = FlushMode.IMMEDIATE

        assertEquals(getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME)?.state, WorkInfo.State.CANCELLED)
    }

    @Test
    fun `should run flush periodically`() {
        val testDriver = WorkManagerTestInitHelper.getTestDriver(ApplicationProvider.getApplicationContext())!!

        mockkConstructor(FlushManagerImpl::class)
        var flushCalls = 0
        every {
            anyConstructed<FlushManagerImpl>().flushData()
        } answers {
            flushCalls++
            Exponea.component.flushManager.onFlushFinishListener?.invoke()
        }

        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        waitUntilFlushed()
        flushCalls = 0

        Exponea.flushMode = FlushMode.PERIOD

        for (i in 1..5) {
            testDriver.setAllConstraintsMet(getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME)!!.id)
            testDriver.setPeriodDelayMet(getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME)!!.id)
            assertEquals(i, flushCalls)
        }
    }

    @Test // it's not possible to check periodicity of tasks in work manager, let's just check new task was queued
    fun `should enqueue new task when flushPeriod changes`() {
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.PERIOD
        val oldTaskId = getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME)!!.id
        Exponea.flushPeriod = FlushPeriod(1, TimeUnit.DAYS)
        val newTaskId = getUniqueWorkInfo(ExponeaPeriodicFlushWorker.WORK_NAME)!!.id
        assertNotEquals(oldTaskId, newTaskId)
    }
}
