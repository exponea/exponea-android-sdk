package com.exponea.sdk.util

import android.util.Base64
import androidx.core.net.toUri

internal object LocalResourceUrlMapper {
    private const val SCHEME = "https"
    private const val HOST = "appassets.androidplatform.net"
    private const val ROOT_SEGMENT = "exponea"
    private const val CONTENT_BLOCKS_SEGMENT = "content-blocks"
    private const val IMAGES_SEGMENT = "images"
    private const val FONTS_SEGMENT = "fonts"
    private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    enum class ResourceType {
        IMAGE, FONT
    }

    data class LocalResource(
        val type: ResourceType,
        val originalUrl: String
    )

    fun imageUrl(originalUrl: String): String = buildLocalUrl(IMAGES_SEGMENT, originalUrl)

    fun fontUrl(originalUrl: String): String = buildLocalUrl(FONTS_SEGMENT, originalUrl)

    fun parse(url: String): LocalResource? {
        val uri = runCatching { url.toUri() }.getOrNull() ?: return null
        if (!uri.scheme.equals(SCHEME, ignoreCase = true) || uri.host != HOST) {
            return null
        }
        val pathSegments = uri.pathSegments
        if (pathSegments.size != 4 ||
            pathSegments[0] != ROOT_SEGMENT ||
            pathSegments[1] != CONTENT_BLOCKS_SEGMENT
        ) {
            return null
        }
        val type = when (pathSegments[2]) {
            IMAGES_SEGMENT -> ResourceType.IMAGE
            FONTS_SEGMENT -> ResourceType.FONT
            else -> return null
        }
        val originalUrl = runCatching {
            String(Base64.decode(pathSegments[3], BASE64_FLAGS), Charsets.UTF_8)
        }.getOrNull() ?: return null
        return LocalResource(type, originalUrl)
    }

    fun isLocalResourceUrl(url: String): Boolean = parse(url) != null

    private fun buildLocalUrl(resourceSegment: String, originalUrl: String): String {
        val encodedUrl = Base64.encodeToString(originalUrl.toByteArray(Charsets.UTF_8), BASE64_FLAGS)
        return "$SCHEME://$HOST/$ROOT_SEGMENT/$CONTENT_BLOCKS_SEGMENT/$resourceSegment/$encodedUrl"
    }
}
