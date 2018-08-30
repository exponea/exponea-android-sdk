package com.exponea.sdk.services

import androidx.work.Worker
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.util.Logger
import java.util.concurrent.CountDownLatch

class ExponeaJobService : Worker() {

    companion object {
        const val TAG = "ExponeaJobServiceWork"
    }

    override fun doWork(): Result {
        Logger.d(this, "doWork -> Starting...")
        val countDownLatch = CountDownLatch(1)
        // If our flush mode isn't set to period then we should cancel all future jobs
        if (Exponea.flushMode != FlushMode.PERIOD) {
            return Result.SUCCESS
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
                return Result.FAILURE
            }
        } catch (e: UninitializedPropertyAccessException) {
            return Result.SUCCESS
        } catch (e: Exception) {
            return Result.FAILURE
        }
        return Result.RETRY
    }

    override fun onStopped(cancelled: Boolean) {
        super.onStopped(cancelled)
        Logger.d(this, "onStopJob")
    }
}