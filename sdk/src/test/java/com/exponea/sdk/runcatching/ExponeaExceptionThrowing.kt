package com.exponea.sdk.runcatching

import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.manager.BackgroundTimerManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.receiver.NotificationsPermissionReceiver
import com.exponea.sdk.services.DefaultAppInboxProvider
import io.mockk.every
import io.mockk.mockkObject

object ExponeaExceptionThrowing {
    class TestPurposeException : Exception("Exception for test purposes")

    private var throwException = false

    fun prepareExponeaToThrow() {
        throwException = false
        mockkConstructorFix(ExponeaComponent::class)
        mockkConstructorFix(DefaultAppInboxProvider::class) {
            every { anyConstructed<DefaultAppInboxProvider>().getAppInboxButton(any()) }
        }
        mockkObject(NotificationsPermissionReceiver)

        every { anyConstructed<ExponeaComponent>().anonymize(any(), any()) } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().eventManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().sessionManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().connectionManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().flushManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().backgroundTimerManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        mockkConstructorFix(BackgroundTimerManagerImpl::class)
        every { anyConstructed<BackgroundTimerManagerImpl>().startTimer() } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<BackgroundTimerManagerImpl>().stopTimer() } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().serviceManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().fcmManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().fetchManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().networkManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().trackingConsentManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().inAppMessageTrackingDelegate } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().appInboxManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().appInboxCache } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<DefaultAppInboxProvider>().getAppInboxButton(any()) } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<DefaultAppInboxProvider>().getAppInboxListFragment(any()) } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<DefaultAppInboxProvider>().getAppInboxDetailFragment(any(), any()) } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().inAppContentBlockManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { NotificationsPermissionReceiver.requestPushAuthorization(any(), any()) } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        every { anyConstructed<ExponeaComponent>().segmentsManager } answers {
            if (throwException) throw TestPurposeException()
            callOriginal()
        }
        Exponea.appInboxProvider = DefaultAppInboxProvider()
    }

    fun makeExponeaThrow() {
        throwException = true
    }
}
