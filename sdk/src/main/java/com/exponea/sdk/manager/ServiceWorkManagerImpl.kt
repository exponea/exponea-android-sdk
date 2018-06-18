package com.exponea.sdk.manager

import android.content.Context
import androidx.work.*
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.services.ExponeaWorkRequest
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import java.util.concurrent.TimeUnit


class ServiceWorkManagerImpl(context: Context, private val configuration: ExponeaConfiguration?) : ServiceManager {
    private val keyUniqueName = "KeyUniqueName"
    private val gson = Gson()

    override fun start() {
        val configurationData = try {
            gson.toJson(configuration)
        } catch (exception: Exception) {
            Logger.e(this, "start() -> Failed serializing configuration")
            return
        }

        val input = Data
                .Builder()
                .putString(
                        ExponeaWorkRequest.KEY_CONFIG_INPUT,
                        configurationData
                )
                .build()

        val workRequest = OneTimeWorkRequest
                .Builder(ExponeaWorkRequest::class.java)
                .setInputData(input)
                .setInitialDelay(120, TimeUnit.SECONDS)
                .build()

        WorkManager
                .getInstance()
                .beginUniqueWork(
                        keyUniqueName,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                )
                .enqueue()

        Logger.d(this, "ServiceWorkManagerImpl.start() -> enqueued background task...")
    }

    override fun stop() {
        Logger.d(this, "ServiceWorkManagerImpl.stop() -> cancelling all work")
        WorkManager
                .getInstance()
                .cancelUniqueWork(keyUniqueName)
    }
}