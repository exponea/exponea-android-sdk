package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.HtmlNormalizer.NormalizedResult
import com.exponea.sdk.util.Logger
import java.math.BigInteger
import java.security.MessageDigest

internal class HtmlNormalizedCacheImpl(
    context: Context,
    private val preferences: ExponeaPreferences
) : HtmlNormalizedCache {

    internal data class VersionedNormalizedResult(
        val version: String,
        val result: NormalizedResult
    )

    companion object {
        const val DIRECTORY = "exponeasdk_html_storage"
    }

    private val fileCache = SimpleFileCache(context, DIRECTORY)

    override fun get(key: String, htmlOrigin: String): NormalizedResult? {
        val hashKey = asHashKey(key)
        val cachedControlHash = preferences.getString(hashKey, "")
        if (cachedControlHash.isEmpty()) {
            return null
        }
        val controlHash = hashOf(htmlOrigin)
        if (!controlHash.equals(cachedControlHash)) {
            Logger.w(this, "HTML cache differs in control hash")
            return null
        }
        val fileKey = asFileNameKey(key)
        val fileName = preferences.getString(fileKey, "")
        if (fileName.isEmpty()) {
            Logger.w(this, "HTML cache file path missing, removing it")
            remove(key)
            return null
        }
        val cachedHtmlFile = fileCache.getFile(fileName)
        if (cachedHtmlFile == null) {
            Logger.w(this, "HTML cache file missing, removing it")
            remove(key)
            return null
        }
        try {
            val cachedHtmlResultContent = cachedHtmlFile.readText(Charsets.UTF_8)
            val cachedHtmlWithVersion: VersionedNormalizedResult? = ExponeaGson.instance.fromJson(
                cachedHtmlResultContent,
                VersionedNormalizedResult::class.java
            )
            if (cachedHtmlWithVersion == null) {
                Logger.w(this, "HTML cache file is corrupted, removing it")
                remove(key)
                return null
            }
            if (cachedHtmlWithVersion.version != getCurrentCacheVersion()) {
                Logger.w(this, "HTML cache file is obsolete, removing it")
                remove(key)
                return null
            }
            if (!cachedHtmlWithVersion.result.valid) {
                Logger.w(this, "HTML cache content is invalid, removing it")
                remove(key)
                return null
            }
            return cachedHtmlWithVersion.result
        } catch (e: Exception) {
            Logger.e(this, "HTML cache cannot be used, removing it", e)
            remove(key)
            return null
        }
    }

    private fun asFileNameKey(key: String) = "InAppContentBlock_file_$key"

    private fun asHashKey(key: String) = "InAppContentBlock_hash_$key"

    private fun hashOf(data: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = BigInteger(1, digest.digest(data.toByteArray(Charsets.UTF_8)))
            return String.format("%032x", hash)
        } catch (e: Exception) {
            Logger.e(this, "Hash for HTML cache failed", e)
            return data.hashCode().toString()
        }
    }

    override fun set(key: String, htmlOrigin: String, normalizedResult: NormalizedResult) {
        if (!normalizedResult.valid) {
            Logger.w(this, "Normalized HTML content is not stored, as it is invalid")
            return
        }
        // content storaging
        val fileName = "InAppContentBlock_cached_$key.json"
        try {
            val versionedResult = VersionedNormalizedResult(getCurrentCacheVersion(), normalizedResult)
            val cachedResult = ExponeaGson.instance.toJson(versionedResult)
            val file = createTempFile()
            file.writeText(cachedResult)
            file.renameTo(fileCache.retrieveFileDirectly(fileName))
        } catch (e: Exception) {
            Logger.e(this, "Hash for HTML cannot be stored", e)
            return
        }
        // metadata storaging
        val hashKey = asHashKey(key)
        val fileNameKey = asFileNameKey(key)
        val controlHash = hashOf(htmlOrigin)
        preferences.setString(hashKey, controlHash)
        preferences.setString(fileNameKey, fileName)
    }

    private fun getCurrentCacheVersion() = BuildConfig.EXPONEA_VERSION_NAME

    override fun remove(key: String) {
        // content removing
        val fileKey = asFileNameKey(key)
        val fileName = preferences.getString(fileKey, "")
        if (fileName.isNotEmpty()) {
            try {
                fileCache.getFile(fileName)?.delete()
            } catch (e: Exception) {
                Logger.e(this, "HTML cache file cannot be removed", e)
            }
        }
        // metadata removing
        preferences.remove(asHashKey(key))
        preferences.remove(fileKey)
    }

    override fun clearAll() {
        fileCache.clear()
    }
}
