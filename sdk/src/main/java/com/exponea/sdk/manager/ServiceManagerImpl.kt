package com.exponea.sdk.manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.exponea.sdk.Exponea
import com.exponea.sdk.services.ExponeaPeriodicFlushWorker
import java.util.concurrent.TimeUnit

class ServiceManagerImpl : ServiceManager {

    override fun startPeriodicFlush(context: Context) {
        val request = PeriodicWorkRequest.Builder(
                ExponeaPeriodicFlushWorker::class.java,
                Exponea.flushPeriod.timeInMillis,
                TimeUnit.MILLISECONDS
        ).setConstraints(
                Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ExponeaPeriodicFlushWorker.TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        )
    }

    override fun stopPeriodicFlush(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(ExponeaPeriodicFlushWorker.TAG)
    }
}
