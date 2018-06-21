package com.exponea.sdk.manager

import android.content.Context
import androidx.work.*
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.services.ExponeaWorkRequest
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import java.util.concurrent.TimeUnit


class BackgroundTimerManagerImpl(private val context: Context, private val configuration: ExponeaConfiguration) : BackgroundTimerManager {
    private val keyUniqueName = "KeyUniqueName"

    override fun startTimer() {
        ExponeaConfigRepository.set(context, configuration)
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()


        val workRequest = OneTimeWorkRequest
                .Builder(ExponeaWorkRequest::class.java)
                .setConstraints(constraints)
                .setInitialDelay(configuration.sessionTimeout.toLong(), TimeUnit.SECONDS)
                .build()

        WorkManager
                .getInstance()
                .beginUniqueWork(
                        keyUniqueName,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                )
                .enqueue()

        Logger.d(this, "BackgroundTimerManagerImpl.start() -> enqueued background task...")
    }

    override fun stopTimer() {
        Logger.d(this, "BackgroundTimerManagerImpl.stop() -> cancelling all work")
        WorkManager
                .getInstance()
                .cancelUniqueWork(keyUniqueName)
    }
}