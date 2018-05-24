package com.exponea.sdk.services

import android.app.job.JobParameters
import android.app.job.JobService
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.util.Logger

class ExponeaJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Logger.d(this, "onStartJob")

        // If our flush mode isn't set to period then we should cancel all future jobs
        if (Exponea.flushMode != FlushMode.PERIOD) {
            jobFinished(params, false)
            return false
        }

        Exponea.component.flushManager.onFlushFinishListener = {
            Logger.d(this, "onStartJob -> Finished")
            // Once our flushing is done we want to tell the system that we finished and we should reschedule
            jobFinished(params, true)
        }

        Exponea.component.flushManager.flushData()

        // Return true to let the job know we aren't finished with our work

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Logger.d(this, "onStopJob")
        return Exponea.flushMode == FlushMode.PERIOD
    }
}