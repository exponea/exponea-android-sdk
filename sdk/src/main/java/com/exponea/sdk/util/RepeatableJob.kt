package com.exponea.sdk.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

internal class RepeatableJob(
    private val delayMillis: Long,
    private val action: () -> Unit
) {
    private var internJob: Job? = null

    private var paused: Boolean = false
    private var lastPauseAt: Long? = null
    private var lastRunAt: Long? = null

    fun start() {
        stop("Canceled previous job to start a new one")
        startInternal(delayMillis)
    }

    fun stop(message: String) {
        internJob?.let {
            if (it.isActive) {
                it.cancel(message)
            }
            internJob = null
        }
        paused = false
        lastRunAt = null
        lastPauseAt = null
    }

    fun resume() {
        var millisToFinish = (lastPauseAt ?: 0) - (lastRunAt ?: lastPauseAt ?: 0)
        if (millisToFinish <= 0) {
            Logger.w(this, "No paused job scheduled, starting a new one")
            millisToFinish = delayMillis
        }
        stop("Canceled previous job to resume it with new delay")
        startInternal(millisToFinish)
    }

    fun pause() {
        val lastRun = lastRunAt
        stop("Canceled previous job to pause it")
        paused = true
        lastRunAt = lastRun
        lastPauseAt = System.currentTimeMillis()
    }

    private fun startInternal(millis: Long) {
        if (millis <= 0) {
            Logger.v(this, "Only positive delay is allowed, skipping")
            return
        }
        lastRunAt = System.currentTimeMillis()
        internJob = runOnMainThread(millis) {
            internJob = null
            if (!paused) {
                runCatching { action.invoke() }.logOnException()
                startInternal(delayMillis)
            }
        }
    }

    fun restart() {
        if (paused) {
            paused = false
            start()
            return
        }
        if (internJob == null) {
            startInternal(delayMillis)
        }
    }
}
