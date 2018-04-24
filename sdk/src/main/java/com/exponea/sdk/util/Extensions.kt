package com.exponea.sdk.util

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