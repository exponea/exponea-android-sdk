package com.exponea.sdk.network.auth

import android.util.Base64
import com.exponea.sdk.util.Logger
import java.nio.charset.StandardCharsets
import org.json.JSONObject

internal object JwtUtils {

    fun parseJwtExpiration(rawToken: String): Long? {
        if (rawToken.contains(' ')) {
            Logger.w(this, "Invalid JWT format: token must not contain spaces or prefixes")
            return null
        }

        val parts = rawToken.split('.')

        if (parts.size != 3) {
            Logger.w(this, "Invalid JWT format: expected 3 parts but got ${parts.size}")
            return null
        }

        val payload = parts[1]

        return try {
            val decoded = Base64.decode(payload, Base64.URL_SAFE)
            val json = JSONObject(String(decoded, StandardCharsets.UTF_8))
            val exp = json.optLong("exp", 0L)
            if (exp > 0L) exp else null
        } catch (e: Exception) {
            Logger.w(this, "Unable to parse JWT expiration: ${e.message}")
            null
        }
    }
}
