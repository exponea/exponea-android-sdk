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

    private data class MemoryNormalizedResult(
        val controlHash: String,
        val version: String,
        val fileName: String,
        val fileLastModified: Long,
        val result: NormalizedResult
    )

    companion object {
        const val DIRECTORY = "exponeasdk_html_storage"
        private const val IN_MEMORY_CACHE_MAX_ITEMS = 128
    }

    private val fileCache = SimpleFileCache(context, DIRECTORY)
    private val inMemoryCache = object : LinkedHashMap<String, MemoryNormalizedResult>(
        IN_MEMORY_CACHE_MAX_ITEMS,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MemoryNormalizedResult>?): Boolean {
            return size > IN_MEMORY_CACHE_MAX_ITEMS
        }
    }
    private val inMemoryCacheLock = Any()

    override fun get(key: String, htmlOrigin: String): NormalizedResult? {
        val controlHash = hashOf(htmlOrigin)
        getInMemoryResult(key, controlHash)?.let { return it }
        val hashKey = asHashKey(key)
        val cachedControlHash = preferences.getString(hashKey, "")
        if (cachedControlHash.isEmpty()) {
            return null
        }
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
            setInMemoryResult(
                key = key,
                controlHash = controlHash,
                fileName = fileName,
                fileLastModified = cachedHtmlFile.lastModified(),
                versionedResult = cachedHtmlWithVersion
            )
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
        val versionedResult = VersionedNormalizedResult(getCurrentCacheVersion(), normalizedResult)
        // content storaging
        val fileName = "InAppContentBlock_cached_$key.json"
        try {
            val cachedResult = ExponeaGson.instance.toJson(versionedResult)
            val file = createTempFile()
            file.writeText(cachedResult)
            val targetFile = fileCache.retrieveFileDirectly(fileName)
            file.renameTo(targetFile)
            setInMemoryResult(
                key = key,
                controlHash = hashOf(htmlOrigin),
                fileName = fileName,
                fileLastModified = targetFile.lastModified(),
                versionedResult = versionedResult
            )
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
        synchronized(inMemoryCacheLock) {
            inMemoryCache.remove(key)
        }
        preferences.remove(asHashKey(key))
        preferences.remove(fileKey)
    }

    override fun clearAll() {
        synchronized(inMemoryCacheLock) {
            inMemoryCache.clear()
        }
        fileCache.clear()
    }

    private fun getInMemoryResult(key: String, controlHash: String): NormalizedResult? {
        val cached = synchronized(inMemoryCacheLock) { inMemoryCache[key] } ?: return null
        if (cached.version != getCurrentCacheVersion()) {
            synchronized(inMemoryCacheLock) { inMemoryCache.remove(key) }
            return null
        }
        if (cached.controlHash != controlHash) {
            return null
        }
        if (!cached.result.valid) {
            synchronized(inMemoryCacheLock) { inMemoryCache.remove(key) }
            return null
        }
        val cachedFile = fileCache.getFile(cached.fileName)
        if (cachedFile == null || cachedFile.lastModified() != cached.fileLastModified) {
            synchronized(inMemoryCacheLock) { inMemoryCache.remove(key) }
            return null
        }
        return cached.result
    }

    private fun setInMemoryResult(
        key: String,
        controlHash: String,
        fileName: String,
        fileLastModified: Long,
        versionedResult: VersionedNormalizedResult
    ) {
        synchronized(inMemoryCacheLock) {
            inMemoryCache[key] = MemoryNormalizedResult(
                controlHash = controlHash,
                version = versionedResult.version,
                fileName = fileName,
                fileLastModified = fileLastModified,
                result = versionedResult.result
            )
        }
    }
}
