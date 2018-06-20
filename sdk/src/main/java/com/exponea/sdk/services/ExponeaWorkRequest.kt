package com.exponea.sdk.services

import android.util.Log
import androidx.work.Worker
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import java.util.*
import java.util.concurrent.CountDownLatch

class ExponeaWorkRequest : Worker() {
    companion object {
        const val KEY_CONFIG_INPUT = "KeyConfigInput"
    }

    override fun doWork(): WorkerResult {
        Logger.d(this, "doWork -> Starting...")
        val countDownLatch = CountDownLatch(1)

        if (!Exponea.isInitialized)
            return WorkerResult.FAILURE
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
            Log.d(KEY_CONFIG_INPUT, e.toString())
            return WorkerResult.FAILURE
        }
    }
}