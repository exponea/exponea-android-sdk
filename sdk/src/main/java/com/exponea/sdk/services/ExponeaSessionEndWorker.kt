package com.exponea.sdk.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.returnOnException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

internal class ExponeaSessionEndWorker(
    context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    override fun doWork(): Result {
        Logger.d(this, "doWork -> Starting...")
        return Exponea.runCatching {
            Exponea.requireInitialized {
                val countDownLatch = CountDownLatch(1)
                Exponea.trackAutomaticSessionEnd()
                Logger.d(this, "doWork -> Starting flushing data")
                Exponea.flushData {
                    Logger.d(this, "doWork -> Finished")
                    countDownLatch.countDown()
                }
                try {
                    var successFinish = countDownLatch.await(20, SECONDS)
                    if (!successFinish) {
                        Logger.e(this, "doWork -> Timeout!")
                        return@requireInitialized Result.failure()
                    }
                } catch (e: InterruptedException) {
                    Logger.e(this, "doWork -> countDownLatch was interrupted", e)
                    return@requireInitialized Result.failure()
                }
                Logger.d(this, "doWork -> Success!")
                return@requireInitialized Result.success()
            }
        }.returnOnException { Result.failure() } ?: Result.failure()
    }
}
