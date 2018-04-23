package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.util.Logger

@SuppressLint("StaticFieldLeak")
object Exponea {
    private var context: Context? = null

    /**
     * Set which level the debugger should output log messages
     */
    var loggerLevel: Logger.Level
        get () = Logger.level
        set(value) {
            Logger.level = value
        }

    fun init(context: Context) {
        Logger.i(this, "Init")
        this.context = context
    }
}