package com.exponea.sdk.runcatching

import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.manager.BackgroundTimerManagerImpl
import io.mockk.every
import io.mockk.mockkConstructor

object ExponeaExceptionThrowing {
    class TestPurposeException : Exception("Exception for test purposes")

    fun prepareExponeaToThrow() {
        mockkConstructor(ExponeaComponent::class)
        mockkConstructor(BackgroundTimerManagerImpl::class)
    }

    fun makeExponeaThrow() {
        // let's mock every manager in ExponeaComponent
        // to make sure any public method throws
        every { anyConstructed<ExponeaComponent>().anonymize(any(), any()) } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().eventManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().sessionManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().connectionManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().flushManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().backgroundTimerManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().serviceManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().fcmManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().fetchManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().networkManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().trackingConsentManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().inAppMessageTrackingDelegate } throws TestPurposeException()

        // This will cause onSessionStart/Stop to throw exception
        every { anyConstructed<BackgroundTimerManagerImpl>().startTimer() } throws TestPurposeException()
        every { anyConstructed<BackgroundTimerManagerImpl>().stopTimer() } throws TestPurposeException()
    }
}
