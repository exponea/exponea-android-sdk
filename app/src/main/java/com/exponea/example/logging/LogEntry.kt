package com.exponea.example.logging

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val level: Level,
    val tag: String,
    val message: String
) {
    enum class Level(val short: String) {
        VERBOSE("V"),
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E");

        companion object {
            fun fromShort(short: String): Level = when (short.trim().uppercase()) {
                "V" -> VERBOSE
                "D" -> DEBUG
                "I" -> INFO
                "W" -> WARN
                "E", "F" -> ERROR
                else -> INFO
            }
        }
    }
}
