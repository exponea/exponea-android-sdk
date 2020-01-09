package com.exponea.sdk.runcatching

import com.exponea.sdk.ExponeaComponent
import io.mockk.every

object ExponeaExceptionThrowing {
    class TestPurposeException : Exception("Exception for test purposes")

    fun makeExponeaThrow() {
        // let's mock every manager in ExponeaComponent
        // to make sure any public method throws
        every { anyConstructed<ExponeaComponent>().eventManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().sessionManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().connectionManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().flushManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().personalizationManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().backgroundTimerManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().iapManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().serviceManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().fcmManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().fetchManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().fileManager } throws TestPurposeException()
        every { anyConstructed<ExponeaComponent>().networkManager } throws TestPurposeException()
    }
}
