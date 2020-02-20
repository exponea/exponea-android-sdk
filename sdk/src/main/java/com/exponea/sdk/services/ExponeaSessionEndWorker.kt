package com.exponea.sdk.services

import android.content.Context
import android.os.Looper
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.exponea.sdk.Exponea
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger
import java.util.concurrent.CountDownLatch

internal class ExponeaSessionEndWorker(
    context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    override fun doWork(): Result {
        Logger.d(this, "doWork -> Starting...")
        val countDownLatch = CountDownLatch(1)
        val config = ExponeaConfigRepository.get(applicationContext) ?: return Result.failure()

        if (!Exponea.isInitialized) {
            Looper.prepare()
            Exponea.init(applicationContext, config)
        }

        try {
            Exponea.trackAutomaticSessionEnd()
            Logger.d(this, "doWork -> Starting flushing data")
            Exponea.flushData {
                Logger.d(this, "doWork -> Finished")
                countDownLatch.countDown()
            }

            try {
                countDownLatch.await()
            } catch (e: InterruptedException) {
                Logger.e(this, "doWork -> countDownLatch was interrupted", e)
                return Result.failure()
            }

            Logger.d(this, "doWork -> Success!")

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
