package com.exponea.example.managers

import android.util.Base64
import android.util.Log
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.minutes

/**
 * !!! WARN for developers.
 * This implementation is just proof of concept for the Example App.
 * In production, JWT tokens MUST be generated on a secure backend.
 * Never embed signing secrets in a shipping application.
 */
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

    /**
     * JWT KID/Secret configuration.
     *
     * @param secret HMAC shared secret for signing. Null or blank disables the generator.
     * @param kid Key ID (`kid` header) identifying the stream signing secret.
     */
    fun configure(secret: String, kid: String) {
        this.secret = secret.takeIf { it.isNotBlank() }
        this.kid = kid.takeIf { it.isNotBlank() }
        Log.d(tag, "Configured, secret present: ${this.secret != null}, kid: ${this.kid}")
    }

    /**
     * Returns true if this generator has been configured with a non-null secret and kid.
     */
    fun isConfigured(): Boolean = secret != null && kid != null

    /**
     * Generates a JWT token with the given customer IDs embedded in the `ids` claim.
     *
     * Token structure:
     * - Header: `typ=JWT`, `alg=HS512`, `kid=<configured-key-id>`
     * - Payload: `exp=<unix-seconds>`, `ids={<id_type>: <id_value>, ...}`
     * - Signature: HMAC-SHA512 with the configured shared secret
     *
     * @param customerIds map of trusted IDs (e.g. "registered" to "user@example.com")
     * @return signed JWT string, or null if the generator is not configured, provided customer IDs are empty,
     * or if token generation fails
     */
    fun generateToken(customerIds: Map<String, String>): String? {
        val currentSecret = secret ?: run {
            Log.w(tag, "Secret is not configured, skipping token generation.")
            return null
        }
        val currentKid = kid ?: run {
            Log.w(tag, "Kid is not configured, skipping token generation.")
            return null
        }
        if (customerIds.isEmpty()) {
            Log.w(tag, "Token without customer IDs would not be valid, skipping generation.")
            return null
        }
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

            val token = "$signingInput.$signature"
            Log.d(tag, "Token generated, expires at $expiresAt, ids: ${customerIds.keys}.")
            token
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
