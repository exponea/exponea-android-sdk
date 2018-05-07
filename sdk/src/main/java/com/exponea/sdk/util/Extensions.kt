package com.exponea.sdk.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

fun Call.enqueue(onResponse: (Call, Response) -> Unit, onFailure: (Call, IOException) -> Unit) {
    this.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure(call, e)
        }

        override fun onResponse(call: Call, response: Response) {
            onResponse(call, response)
        }
    })
}

fun Application.addSessionObserver(onStart: () -> Unit, onStop: () -> Unit) {
    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity?) {
            onStop()
        }

        override fun onActivityResumed(activity: Activity?) {
            onStart()
        }

        override fun onActivityStarted(activity: Activity?) {}
        override fun onActivityDestroyed(activity: Activity?) {}
        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
        override fun onActivityStopped(activity: Activity?) {}
        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
    })
}