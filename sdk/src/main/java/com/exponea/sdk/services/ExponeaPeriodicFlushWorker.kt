package com.exponea.sdk.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.returnOnException
import java.util.concurrent.CountDownLatch

internal class ExponeaPeriodicFlushWorker(
    context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    companion object {
        const val WORK_NAME = "ExponeaPeriodicFlushWorker"
    }

    override fun doWork(): Result {
        Logger.d(this, "doWork -> Starting...")
        if (!Exponea.isInitialized) {
            return Result.failure()
        }
        if (Exponea.flushMode != FlushMode.PERIOD) {
            return Result.failure()
        }
        runCatching {
            val countDownLatch = CountDownLatch(1)
            Exponea.component.flushManager.onFlushFinishListener = {
                // Once our flushing is done we want to tell the system that we finished and we should reschedule
                countDownLatch.countDown()
            }
            Exponea.component.flushManager.flushData()
            try {
                countDownLatch.await()
            } catch (e: InterruptedException) {
                Logger.e(this, "doWork -> flush was interrupted", e)
                return Result.failure()
            }
        }.returnOnException { Result.failure() }
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        Logger.d(this, "onStopped")
    }
}
