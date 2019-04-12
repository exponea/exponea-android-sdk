package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import kotlinx.coroutines.delay

object ExponeaMockApi {

    suspend fun flush() {
        Exponea.flushData()
        delay(2000L)
    }

}