package com.exponea.example.test

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class TestWorkerUtil(private val workManager: WorkManager) {

    fun startWork() {
        workManager.beginUniqueWork(
            "testWorkerJobId",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequest
                .Builder(TestWorker::class.java)
                .build()
        ).enqueue()
    }
}