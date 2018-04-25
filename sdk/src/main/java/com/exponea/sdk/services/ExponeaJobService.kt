package com.exponea.sdk.services

import android.app.job.JobParameters
import android.app.job.JobService
import com.exponea.sdk.util.Logger

class ExponeaJobService : JobService() {
    companion object {
        var isRunning: Boolean = false
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Logger.d(this, "onStartJob")
        isRunning = true
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Logger.d(this, "onStopJob")
        isRunning = false
        return true
    }

}