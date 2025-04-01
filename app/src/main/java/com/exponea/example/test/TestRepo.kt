package com.exponea.example.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class TestRepo {
    suspend fun fetchData() {
        withContext(Dispatchers.IO) {
            delay(2000L)
        }
    }
}
