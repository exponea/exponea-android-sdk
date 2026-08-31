package com.exponea.sdk.testutil

import android.content.ComponentName
import android.content.Context
import android.content.pm.ProviderInfo
import android.os.Binder
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.manager.InAppContentBlockManagerImpl
import com.exponea.sdk.manager.InAppMessageManagerImpl
import com.exponea.sdk.manager.PushNotificationSelfCheckManagerImpl
import com.exponea.sdk.manager.ReloadMode
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.network.ExponeaServiceImpl
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.telemetry.upload.SentryTelemetryUpload
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication

internal open class ExponeaSDKTest {
    /**
     * SDK initialization tests should not start an App Inbox request that can outlive
     * MockK teardown. Low-level FetchManager tests override this to exercise the real call.
     */
    protected open val stubAppInboxFetch: Boolean = true

    companion object {
        fun skipInstallEvent() {
            DeviceInitiatedRepositoryImpl(
                ExponeaPreferencesImpl(
                    ApplicationProvider.getApplicationContext()
                )
            ).set(true)
        }
    }

    @Before
    fun prepareServiceBinderForRoom() {
        Shadow.extract<ShadowApplication>(RuntimeEnvironment.getApplication()).let { appShadow ->
            appShadow.setComponentNameAndServiceForBindService(ComponentName("", ""), Binder())
            appShadow.setBindServiceCallsOnServiceConnectedDirectly(false)
        }
    }

    @Before
    fun disableTelemetry() {
        mockkConstructorFix(SentryTelemetryUpload::class) {
            every { anyConstructed<SentryTelemetryUpload>().sendSentryEnvelope(any(), any()) }
        }
        every { anyConstructed<SentryTelemetryUpload>().sendSentryEnvelope(any(), any()) } answers {
            secondArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }
        mockkConstructorFix(TelemetryManager::class)
        every { anyConstructed<TelemetryManager>().reportEvent(any(), any()) } just Runs
    }

    @Before
    fun disableInAppMessagePrefetch() {
        mockkConstructorFix(InAppMessageManagerImpl::class)
        every {
            anyConstructed<InAppMessageManagerImpl>().detectReloadMode(any(), any(), any())
        } returns ReloadMode.STOP
        every { anyConstructed<InAppMessageManagerImpl>().pickAndShowMessage() } just Runs
    }

    @Before
    fun disableInAppContentBlocksPrefetch() {
        mockkConstructorFix(InAppContentBlockManagerImpl::class)
        every { anyConstructed<InAppContentBlockManagerImpl>().loadInAppContentBlockPlaceholders() } just Runs
    }

    @Before
    fun mockNetworkManagers() {
        mockkConstructorFix(FetchManagerImpl::class) {
            every { anyConstructed<FetchManagerImpl>().fetchSegments(any<ProjectConfig>(), any(), any(), any()) }
            every { anyConstructed<FetchManagerImpl>().fetchSegments(any<StreamConfig>(), any(), any(), any()) }
        }
        if (stubAppInboxFetch) {
            // SDK initialization may schedule an App Inbox refresh. Individual tests that
            // exercise it install their own response; the shared fixture must not start a
            // real OkHttp request that can outlive MockK cleanup.
            every {
                anyConstructed<FetchManagerImpl>().fetchAppInbox(
                    any<ProjectConfig>(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just Runs
            every {
                anyConstructed<FetchManagerImpl>().fetchAppInbox(any<StreamConfig>(), any(), any(), any(), any(), any())
            } just Runs
        }
        mockkConstructorFix(ExponeaServiceImpl::class) {
            every { anyConstructed<ExponeaServiceImpl>().fetchSegments(any<ProjectConfig>(), any()) }
            every { anyConstructed<ExponeaServiceImpl>().fetchSegments(any<StreamConfig>(), any()) }
        }
        mockkConstructorFix(FlushManagerImpl::class)
    }

    @Before
    fun disablePushNotificationSelfCheck() {
        mockkConstructorFix(PushNotificationSelfCheckManagerImpl::class)
        every {
            anyConstructed<PushNotificationSelfCheckManagerImpl>().start()
        } just Runs
    }

    @Before
    fun registersForegroundStateAppCheck() {
        ExponeaContextProvider.applicationIsForeground = false
        Robolectric
            .buildContentProvider(ExponeaContextProvider::class.java)
            .create(ProviderInfo().apply {
                authority = "${ApplicationProvider.getApplicationContext<Context>().packageName}.sdk.contextprovider"
                grantUriPermissions = true
            }).get()
    }

    @After
    fun afterExponeaTest() {
        // Keep MockK teardown separate from SDK reset. Mocked manager cleanup can itself
        // schedule callbacks, which makes unmockkAll contend with active instrumentation.
        // App Inbox refreshes are stubbed above so no network callback can outlive this step.
        unmockAllSafely()
        resetExponea()
    }

    fun unmockAllSafely() {
        // mockk has a problem when it sometimes throws an exception, in that case just try again
        try {
            unmockkAll()
        } catch (_: ConcurrentModificationException) {
            unmockAllSafely()
        }
    }

    fun resetExponea() {
        if (Exponea.isInitialized && Exponea.componentForTesting.flushManager.isRunning) {
            // Database is shared between Exponea instances, FlushingManager is not
            // If flushing is ongoing after test is done, it can flush events from another test
            // You can use waitUntilFlushed() to fix this
            throw RuntimeException("Flushing still in progress after test is done!")
        }
        Exponea.reset()
    }
}
