package com.exponea.example.managers

import android.util.Base64
import android.util.Log
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.minutes

class LocalJwtTokenGenerator private constructor() {

    companion object {
        val INSTANCE = LocalJwtTokenGenerator()
        private const val DEFAULT_EXPIRATION_MINUTES = 15
        private const val ALGORITHM = "HmacSHA512"
        private const val ALG_NAME = "HS512"
    }

    private val tag = this::class.simpleName
    private var secret: String? = null
    private var kid: String? = null

    fun configure(secret: String, kid: String) {
        this.secret = secret.takeIf { it.isNotBlank() }
        this.kid = kid.takeIf { it.isNotBlank() }
    }

    fun isConfigured(): Boolean = secret != null && kid != null

    fun generateToken(customerIds: Map<String, String>): String? {
        val currentSecret = secret ?: return null
        val currentKid = kid ?: return null
        if (customerIds.isEmpty()) return null
        return try {
            val expiresAt = Date(
                System.currentTimeMillis() + DEFAULT_EXPIRATION_MINUTES.minutes.inWholeMilliseconds
            )
            val header = """{"typ":"JWT","alg":"$ALG_NAME","kid":"$currentKid"}"""
            val idsJson = customerIds.entries.joinToString(",") { (k, v) ->
                "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
            }
            val payload = """{"exp":${expiresAt.time / 1000},"ids":{$idsJson}}"""
            val encodedHeader = base64Url(header.toByteArray())
            val encodedPayload = base64Url(payload.toByteArray())
            val signingInput = "$encodedHeader.$encodedPayload"
            val mac = Mac.getInstance(ALGORITHM)
            mac.init(SecretKeySpec(currentSecret.toByteArray(), ALGORITHM))
            val signature = base64Url(mac.doFinal(signingInput.toByteArray()))
            "$signingInput.$signature"
        } catch (e: Exception) {
            Log.e(tag, "Token generation failed: ${e.message}")
            null
        }
    }

    private fun base64Url(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
