package com.exponea.sdk.manager

import androidx.work.*
import com.exponea.sdk.Exponea
import com.exponea.sdk.services.ExponeaJobService
import java.util.concurrent.TimeUnit

internal class ServiceManagerImpl : ServiceManager {

    override fun start() {
        val request = PeriodicWorkRequest.Builder(
                ExponeaJobService::class.java,
                Exponea.flushPeriod.timeInMillis,
                TimeUnit.MILLISECONDS
        ).setConstraints(
                Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
        ).build()

        WorkManager.getInstance().enqueueUniquePeriodicWork(
                ExponeaJobService.TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        )
    }

    override fun stop() {
        WorkManager.getInstance().cancelAllWorkByTag(ExponeaJobService.TAG)
    }
}