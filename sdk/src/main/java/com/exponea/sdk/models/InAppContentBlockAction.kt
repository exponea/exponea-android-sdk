package com.exponea.sdk.models

data class InAppContentBlockAction(
    val contentBlock: InAppContentBlock,
    val type: InAppContentBlockActionType,
    val name: String? = null,
    val url: String? = null
)

enum class InAppContentBlockActionType {
    DEEPLINK, BROWSER, CLOSE
}
