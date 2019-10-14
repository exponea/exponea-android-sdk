package com.exponea.sdk.models

import java.util.*

data class DatabaseStorageObject<T>(
        var id: String = UUID.randomUUID().toString(),
        var tries: Int = 0,
        var projectId: String,
        var item: T,
        var route: Route?,
        var shouldBeSkipped: Boolean = false
)