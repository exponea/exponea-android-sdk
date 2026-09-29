package com.exponea.example.logging

import android.os.Process
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MemoryLogger private constructor() {

    companion object {
        val INSTANCE = MemoryLogger()
        private const val MAX_ENTRIES = 500
    }

    private val buffer: ArrayDeque<LogEntry> = ArrayDeque(MAX_ENTRIES)
    private val lock = Any()
    private val pid: Int = Process.myPid()
    private var nextId = 0L

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<Event> = _events.asSharedFlow()

    @Volatile
    private var readerThread: Thread? = null

    sealed class Event {
        data class Appended(val entry: LogEntry) : Event()
        data object Cleared : Event()
    }

    @Synchronized
    fun start() {
        if (readerThread?.isAlive == true) return
        readerThread = Thread {
            try {
                runCatching { Runtime.getRuntime().exec(arrayOf("logcat", "-c")).waitFor() }
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-v", "threadtime"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                while (!Thread.currentThread().isInterrupted) {
                    val line = reader.readLine() ?: break
                    parseLine(line)?.let { append(it) }
                }
            } catch (_: Throwable) {
                // logcat unreachable on some devices
            }
        }.also {
            it.name = "MemoryLogger-Reader"
            it.isDaemon = true
            it.start()
        }
    }

    fun snapshot(): List<LogEntry> {
        synchronized(lock) { return buffer.toList() }
    }

    fun clear() {
        synchronized(lock) { buffer.clear() }
        _events.tryEmit(Event.Cleared)
    }

    fun append(level: LogEntry.Level, tag: String, message: String) {
        append(newEntry(System.currentTimeMillis(), level, tag, message))
    }

    private fun newEntry(
        timestamp: Long,
        level: LogEntry.Level,
        tag: String,
        message: String
    ): LogEntry {
        synchronized(lock) {
            return LogEntry(
                id = nextId++,
                timestamp = timestamp,
                level = level,
                tag = tag,
                message = message
            )
        }
    }

    private fun append(entry: LogEntry) {
        synchronized(lock) {
            if (buffer.size >= MAX_ENTRIES) buffer.removeFirst()
            buffer.addLast(entry)
        }
        _events.tryEmit(Event.Appended(entry))
    }

    private fun parseLine(line: String): LogEntry? {
        if (line.length < 33 || line.startsWith("---")) return null
        return try {
            val parts = line.trim().split(WHITESPACE_REGEX, limit = 6)
            if (parts.size < 6) return null
            val linePid = parts[2].toIntOrNull() ?: return null
            if (linePid != pid) return null
            val levelChar = parts[4]
            val tagAndMessage = parts[5]
            val colonIdx = tagAndMessage.indexOf(":")
            val (tag, message) = if (colonIdx > 0) {
                tagAndMessage.substring(0, colonIdx).trim() to
                    tagAndMessage.substring(colonIdx + 1).trim()
            } else {
                "" to tagAndMessage.trim()
            }
            newEntry(
                timestamp = System.currentTimeMillis(),
                level = LogEntry.Level.fromShort(levelChar),
                tag = tag,
                message = message
            )
        } catch (_: Throwable) {
            null
        }
    }
}

private val WHITESPACE_REGEX = "\\s+".toRegex()
