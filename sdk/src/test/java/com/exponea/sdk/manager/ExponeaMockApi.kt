package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.*
import kotlinx.coroutines.experimental.delay
import java.util.*

object ExponeaMockApi {



    suspend fun flush() {
        Exponea.flushData()
        delay(2000L)
    }

}