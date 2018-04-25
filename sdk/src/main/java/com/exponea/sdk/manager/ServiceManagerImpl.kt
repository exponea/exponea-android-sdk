package com.exponea.sdk.manager

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.services.ExponeaJobService
import com.exponea.sdk.util.Logger


class ServiceManagerImpl(context: Context) : ServiceManager {
    /**
     * Id for our job used for starting and canceling the job
     */
    private val jobID: Int = 1400
    /**
     * Job scheduler service
     */
    private val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    /**
     * Our component name
     */
    private val componentName = ComponentName(context, ExponeaJobService::class.java)

    override fun start() {
        Logger.d(this, "Starting Service")

        val period = Exponea.flushPeriod.timeInMillis

        val job = JobInfo.Builder(jobID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(period)
                .build()

        jobScheduler.schedule(job)
    }

    override fun stop() {
        Logger.d(this, "Stopping Service")
        jobScheduler.cancel(jobID)
    }

}