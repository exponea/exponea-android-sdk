package com.exponea.sdk.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.util.Logger
import java.util.concurrent.CountDownLatch

class ExponeaPeriodicFlushWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    companion object {
        const val TAG = "ExponeaPeriodicFlushWorker"
    }

    override fun doWork(): Result {
        Logger.d(this, "doWork -> Starting...")
        val countDownLatch = CountDownLatch(1)
        // If our flush mode isn't set to period then we should cancel all future jobs
        if (Exponea.flushMode != FlushMode.PERIOD) {
            return Result.success()
        }
        try {
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
        } catch (e: UninitializedPropertyAccessException) {
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
        return Result.retry()
    }

    override fun onStopped() {
        super.onStopped()
        Logger.d(this, "onStopped")
    }

}
