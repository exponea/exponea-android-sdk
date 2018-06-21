package com.exponea.sdk.services

import androidx.work.Worker
import com.exponea.sdk.Exponea
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger
import java.util.*
import java.util.concurrent.CountDownLatch

class ExponeaWorkRequest : Worker() {
    companion object {
        const val KEY_CONFIG_INPUT = "KeyConfigInput"
    }

    override fun doWork(): WorkerResult {
        Logger.d(this, "doWork -> Starting...")
        val countDownLatch = CountDownLatch(1)
        val config = ExponeaConfigRepository.get(applicationContext) ?: return WorkerResult.FAILURE

        if (!Exponea.isInitialized) {
            Exponea.basicInit(applicationContext, config)
        }

        try {
            Exponea.component.sessionManager.trackSessionEnd(Date().time)
            Exponea.component.flushManager.onFlushFinishListener = {
                Logger.d(this, "doWork -> Finished")
                countDownLatch.countDown()
            }

            Logger.d(this, "doWork -> Starting flushing data")
            Exponea.component.flushManager.flushData()

            try {
                countDownLatch.await()
            } catch (e: InterruptedException) {
                Logger.e(this, "doWork -> countDownLatch was interrupted", e)
                return WorkerResult.FAILURE
            }

            Logger.d(this, "doWork -> Success!")

            return WorkerResult.SUCCESS
        } catch (e: Exception) {
            return WorkerResult.FAILURE
        }
    }
}