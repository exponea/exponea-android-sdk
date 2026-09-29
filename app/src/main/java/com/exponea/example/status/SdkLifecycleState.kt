package com.exponea.example.status

enum class SdkLifecycleState(val semanticsValue: String) {
    NOT_INITIALIZED("notInitialized"),
    ANONYMOUS("anonymous"),
    IDENTIFIED("identified")
}
