package com.exponea.example.test

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TestWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted val workerParams: WorkerParameters,
    private val testRepo: TestRepo
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        println("### Test worker do work")
        testRepo.fetchData()
        println("### Test worker job complete")
        return Result.success()
    }
}
