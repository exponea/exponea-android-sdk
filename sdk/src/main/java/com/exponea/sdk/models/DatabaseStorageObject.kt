package com.exponea.sdk.models

import java.util.UUID

internal data class DatabaseStorageObject<T>(
    var id: String = UUID.randomUUID().toString(),
    var tries: Int = 0,
    @Deprecated("We need to keep this for old database fields compatibility", ReplaceWith("exponeaProject"))
    var projectId: String,
    var item: T,
    var route: Route?,
    var shouldBeSkipped: Boolean = false,
    var exponeaProject: ExponeaProject?
)
