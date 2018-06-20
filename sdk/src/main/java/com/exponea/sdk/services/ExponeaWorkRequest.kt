package com.exponea.sdk.services

import androidx.work.Worker
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
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
        val gson = Gson()
        val countDownLatch = CountDownLatch(1)
        val configInput = inputData.getString(KEY_CONFIG_INPUT, null) ?: return WorkerResult.FAILURE
        val config = gson.fromJson<ExponeaConfiguration>(configInput, ExponeaConfiguration::class.java)

        if( ! Exponea.isInitialized) {
            Exponea.init(applicationContext, config)
            Logger.d(this, "doWork -> Initialized SDK")
        }
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
    }
}