package com.exponea.sdk.manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.exponea.sdk.models.FlushPeriod
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.services.ExponeaPeriodicFlushWorker

internal class ServiceManagerImpl : ServiceManager {

    override fun startPeriodicFlush(context: Context, flushPeriod: FlushPeriod) {
        val request = PeriodicWorkRequest.Builder(
                ExponeaPeriodicFlushWorker::class.java,
                flushPeriod.amount,
                flushPeriod.timeUnit
        ).setConstraints(
                Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ExponeaPeriodicFlushWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        )
    }

    override fun stopPeriodicFlush(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ExponeaPeriodicFlushWorker.WORK_NAME)
    }

    override fun onIntegrationStopped() {
        ExponeaContextProvider.applicationContext?.let { stopPeriodicFlush(it) }
    }
}
