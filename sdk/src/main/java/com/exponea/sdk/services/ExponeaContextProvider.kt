package com.exponea.sdk.services

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import com.exponea.sdk.util.Logger

/**
 * Uses Android app lifecycle system - ContentProviders are loaded in main thread after App start
 * See more: https://firebase.blog/posts/2016/12/how-does-firebase-initialize-on-android
 */
internal class ExponeaContextProvider : ContentProvider() {

    companion object {
        var applicationContext: Context? = null
            get() {
                if (field == null) {
                    Logger.w(this, """
                        Application context not loaded, Check ContextProvider registration in the Manifest!
                        """.trimIndent())
                }
                return field
            }
    }

    override fun onCreate(): Boolean {
        applicationContext = context
        val contextAvailable = context != null
        if (contextAvailable) {
            Logger.e(this, "Application context loaded")
        } else {
            Logger.w(this, """
                Application context not found, Check ContextProvider registration in the Manifest!
            """.trimIndent())
        }
        return contextAvailable
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
