package com.exponea.sdk.manager

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.FirebaseTokenRepository
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.telemetry.model.EventType
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.isResumedActivity
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

internal class PushNotificationSelfCheckManagerImpl(
    context: Context,
    private val configuration: ExponeaConfiguration,
    private val customerIdsRepository: CustomerIdsRepository,
    private val tokenRepository: FirebaseTokenRepository,
    private val flushManager: FlushManager,
    private val exponeaService: ExponeaService,
    private val operationsTimeout: Long = 5000
) : PushNotificationSelfCheckManager {
    companion object {
        val steps = arrayListOf(
            "Track push token",
            "Request push notification",
            "Receive push notification",
            "Check `open app` intent filter"
        )
    }

    data class SelfCheckResponse(val success: Boolean)

    private var application = context.applicationContext as Application
    private var currentResumedActivity: Activity? = if (context.isResumedActivity()) context as? Activity else null
    private var selfCheckPushReceived: Boolean = false

    private val lifecycleListener = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity) {
            currentResumedActivity = null
        }
        override fun onActivityResumed(activity: Activity) {
            currentResumedActivity = activity
        }
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    }

    override fun selfCheckPushReceived() {
        selfCheckPushReceived = true
    }

    override fun start() {
        application.registerActivityLifecycleCallbacks(lifecycleListener)
        GlobalScope.launch {
            startInternal()
            application.unregisterActivityLifecycleCallbacks(lifecycleListener)
        }
    }

    suspend fun startInternal() {
        Exponea.telemetry?.reportEvent(EventType.SELF_CHECK, hashMapOf("step" to "0"))
        Logger.i(this, "Waiting for push token.")
        val pushToken = waitForPushToken()
        if (pushToken == null) {
            showResult(
                0,
                "Unable to get push token. Check your Firebase setup. " +
                    "If you're using your own FirebaseMessagingService don't forget to call Exponea.trackPushToken()"
            )
            return
        }
        Exponea.telemetry?.reportEvent(EventType.SELF_CHECK, hashMapOf("step" to "1"))
        Logger.i(this, "Requesting self-check push notification.")
        if (!requestSelfCheckPush(pushToken)) {
            showResult(
                1,
                "Unable to send self-check push notification from Exponea. " +
                    "Check your push notification setup in Exponea administration."
            )
            return
        }
        Exponea.telemetry?.reportEvent(EventType.SELF_CHECK, hashMapOf("step" to "2"))
        Logger.i(this, "Waiting for self-check push notification.")
        if (!waitForSelfCheckPushReceived()) {
            showResult(
                2,
                "Unable to receive self-check push notification from Exponea. " +
                    "Check your push notification setup in Exponea administration. " +
                    "If you're using your own FirebaseMessagingService, " +
                    "don't forget to call Exponea.handleRemoteMessage()"
            )
            return
        }
        Exponea.telemetry?.reportEvent(EventType.SELF_CHECK, hashMapOf("step" to "3"))
        Logger.i(this, "Checking BroadcastReceiver for `open app` action.")
        if (!hasPushOpenedBroadcastReceiver(application)) {
            showResult(
                3,
                "Broadcast listener for intent `${ExponeaPushReceiver.ACTION_CLICKED}`" +
                    " required for `open app` action not set."
            )
            return
        }
        Exponea.telemetry?.reportEvent(EventType.SELF_CHECK, hashMapOf("step" to "4"))
        showResult(4, "You are now ready to receive push notifications from Exponea.")
    }

    suspend fun showResult(step: Int, message: String) {
        val title = "Push notification setup self-check ${if (step == steps.size) "succeeded" else "failed"}"
        val completeMessage = "$message \n\nSelf-check only runs in debug builds.\n" +
            "To disable it, set Exponea.checkPushSetup\u00A0=\u00A0false"

        if (step == steps.size) {
            Logger.i(this, "$title. ${completeMessage.replace("\n", " ")}")
        } else {
            Logger.e(this, "$title. ${completeMessage.replace("\n", " ")}")
        }
        withTimeoutOrNull(operationsTimeout) {
            while (currentResumedActivity == null) { delay(operationsTimeout / 10) }
        }
        if (currentResumedActivity != null) {
            Handler(Looper.getMainLooper()).post {
                val builder = AlertDialog.Builder(currentResumedActivity)

                builder.setTitle(title)
                builder.setMessage("${getStepStatus(step)}\n$completeMessage")
                builder.setNeutralButton("OK") { _, _ -> }
                builder.create().show()
            }
        }
    }

    fun getStepStatus(step: Int): String {
        var stepsStatus = ""
        for (doneStep in 0..min(step, steps.size - 1)) {
            stepsStatus += "${if (doneStep < step) "\u2713" else "\u2717"} ${steps[doneStep]} \n"
        }
        return stepsStatus
    }

    fun hasPushOpenedBroadcastReceiver(application: Application): Boolean {
        application.packageManager.queryBroadcastReceivers(Intent(ExponeaPushReceiver.ACTION_CLICKED), 0).forEach {
            if (it.activityInfo.name != "com.exponea.sdk.services.ExponeaPushReceiver") {
                return true
            }
        }
        return false
    }

    suspend fun waitForPushToken(): String? {
        val token = withTimeoutOrNull(operationsTimeout) {
            while (tokenRepository.get() == null) { delay(operationsTimeout / 10) }
            return@withTimeoutOrNull tokenRepository.get()
        }
        // we also have to wait for token to be tracked on Exponea servers
        if (flushManager.isRunning) {
            while (flushManager.isRunning) delay(operationsTimeout / 10)
        } else {
            suspendCoroutine { continuation ->
                flushManager.flushData {
                    continuation.resume(Unit)
                }
            }
        }
        delay(operationsTimeout / 5) // wait a bit more to let Exponea process the token
        return token
    }

    suspend fun requestSelfCheckPush(pushToken: String): Boolean = suspendCoroutine { continuation ->
        exponeaService.postPushSelfCheck(
            configuration.mainExponeaProject,
            customerIdsRepository.get(),
            pushToken
        ).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val response = ExponeaGson.instance.fromJson<SelfCheckResponse>(
                    response.body()?.string(),
                    SelfCheckResponse::class.java
                )
                continuation.resume(response.success)
            }
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(false)
            }
        })
    }

    suspend fun waitForSelfCheckPushReceived(): Boolean {
        withTimeoutOrNull(operationsTimeout) {
            while (!selfCheckPushReceived) { delay(operationsTimeout / 10) }
        }
        return selfCheckPushReceived
    }
}
