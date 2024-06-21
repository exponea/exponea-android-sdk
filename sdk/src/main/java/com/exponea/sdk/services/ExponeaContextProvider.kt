package com.exponea.sdk.services

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.OnForegroundStateListener
import com.exponea.sdk.util.logOnException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Uses Android app lifecycle system - ContentProviders are loaded in main thread after App start
 * See more: https://firebase.blog/posts/2016/12/how-does-firebase-initialize-on-android
 */
internal class ExponeaContextProvider : ContentProvider() {

    companion object {

        private val foregroundStateListeners = CopyOnWriteArrayList<OnForegroundStateListener>()

        fun registerForegroundStateListener(listener: OnForegroundStateListener) {
            foregroundStateListeners.add(listener)
        }

        fun removeForegroundStateListener(listener: OnForegroundStateListener) {
            foregroundStateListeners.removeAll { it == listener }
        }

        var applicationContext: Context? = null
            get() {
                if (field == null) {
                    Logger.w(this, """
                        Application context not loaded, Check ContextProvider registration in the Manifest!
                        """.trimIndent())
                }
                return field
            }
        var applicationIsForeground = false
            set(value) {
                field = value
                foregroundStateListeners.forEach {
                    notifyForegroundStateListener(it, value)
                }
            }

        private fun notifyForegroundStateListener(listener: OnForegroundStateListener, value: Boolean) {
            runCatching {
                listener.onStateChanged(value)
            }.logOnException()
        }

        internal fun reset() {
            foregroundStateListeners.clear()
        }
    }

    override fun onCreate(): Boolean {
        applicationContext = context?.applicationContext
        val contextAvailable = context != null
        if (contextAvailable) {
            Logger.d(this, "Application context loaded")
        } else {
            Logger.w(this, """
                Application context not found, Check ContextProvider registration in the Manifest!
            """.trimIndent())
        }
        registerActivityLifecycleCallbacks()
        return contextAvailable
    }

    private fun registerActivityLifecycleCallbacks() {
        if (applicationContext == null || applicationContext !is Application) {
            Logger.e(this, """
                Unable to register App lifecycle for no App context.
            """.trimIndent())
            // Do not block processes that rely on this provider
            applicationIsForeground = true
            return
        }
        (applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                private var numStarted = 0
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityStarted(activity: Activity) {
                    if (numStarted == 0) {
                        applicationIsForeground = true
                    }
                    numStarted++
                }
                override fun onActivityDestroyed(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityStopped(activity: Activity) {
                    numStarted--
                    if (numStarted == 0) {
                        applicationIsForeground = false
                    }
                }
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            }
        )
    }

    override fun attachInfo(context: Context?, info: ProviderInfo?) {
        super.attachInfo(context, info)
        // Validates provider authority value; it has to be unique on device so it MUST DIFFER from class full name.
        // If `applicationId` is not correctly set in build.gradle, authority contains library package
        // so multiple applications on same device will share same authority (is not correct)
        if (info?.authority == ExponeaContextProvider::class.java.name) {
            Logger.w(this, """
                Please provide valid applicationId into your build.gradle file
            """.trimIndent())
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
