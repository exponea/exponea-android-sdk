package com.exponea.sdk.manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.exponea.sdk.Exponea
import com.exponea.sdk.services.ExponeaJobService
import java.util.concurrent.TimeUnit

class ServiceManagerImpl : ServiceManager {

    override fun start(context: Context) {
        val request = PeriodicWorkRequest.Builder(
                ExponeaJobService::class.java,
                Exponea.flushPeriod.timeInMillis,
                TimeUnit.MILLISECONDS
        ).setConstraints(
                Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ExponeaJobService.TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        )
    }

    override fun stop(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(ExponeaJobService.TAG)
    }
}
