package com.exponea.example.managers

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.exponea.example.App
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CustomerTokenStorage(
    private var networkManager: NetworkManager = NetworkManager(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.instance)
) {
    companion object {
        private const val CUSTOMER_TOKEN_CONF = "CustomerTokenConf"
        val INSTANCE = CustomerTokenStorage()
    }

    private var host: String? = null
    private var projectToken: String? = null
    private var publicKey: String? = null
    private var customerIds: HashMap<String, String>? = null
    private var expiration: Int? = null
    private var tokenCache: String? = null
    private var lastTokenRequestTime: Long = 0

    init {
        loadConfiguration()
    }

    fun configure(
        host: String? = this.host,
        projectToken: String? = this.projectToken,
        publicKey: String? = this.publicKey,
        customerIds: HashMap<String, String>? = this.customerIds,
        expiration: Int? = this.expiration
    ) {
        this.host = host
        this.projectToken = projectToken
        this.publicKey = publicKey
        this.customerIds = customerIds
        this.expiration = expiration
        storeConfig()
        tokenCache = null
        lastTokenRequestTime = 0
    }

    fun retrieveJwtToken(): String? {
        val now = System.currentTimeMillis()
        if (TimeUnit.MILLISECONDS.toMinutes(kotlin.math.abs(now - lastTokenRequestTime)) < 5) {
            return tokenCache
        }
        lastTokenRequestTime = now
        if (tokenCache != null) {
            return tokenCache
        }
        synchronized(this) {
            if (tokenCache == null) {
                tokenCache = loadJwtToken()
            }
        }
        return tokenCache
    }

    private fun loadJwtToken(): String? {
        if (host == null || projectToken == null || publicKey == null ||
            customerIds == null || customerIds?.isEmpty() == true
        ) {
            return null
        }
        val reqBody = TokenRequest(
            projectId = projectToken,
            kid = publicKey,
            sub = customerIds,
            exp = expiration
        )
        val response = networkManager.post(
            "$host/webxp/exampleapp/customertokens",
            null,
            json.encodeToString(reqBody)
        ).execute()
        if (!response.isSuccessful) {
            return null
        }
        val jsonResponse = response.body?.string() ?: return null
        return try {
            json.decodeFromString<TokenResponse>(jsonResponse).token
        } catch (_: Exception) {
            null
        }
    }

    private fun storeConfig() {
        prefs.edit {
            putString(CUSTOMER_TOKEN_CONF, json.encodeToString(confAsMap()))
        }
    }

    private fun confAsMap(): Map<String, String?> = mapOf(
        "host" to host,
        "projectToken" to projectToken,
        "publicKey" to publicKey,
        "customerIds" to customerIds?.let { json.encodeToString(it) },
        "expiration" to expiration?.toString()
    )

    private fun loadConfiguration() {
        val confAsJson = prefs.getString(CUSTOMER_TOKEN_CONF, "") ?: return
        if (confAsJson.isEmpty()) return
        val confAsMap = json.decodeFromString<Map<String, String?>>(confAsJson)
        host = confAsMap["host"]
        projectToken = confAsMap["projectToken"]
        publicKey = confAsMap["publicKey"]
        customerIds = HashMap(
            json.decodeFromString<Map<String, String>>(
                confAsMap["customerIds"] ?: "{}"
            )
        )
        expiration = confAsMap["expiration"]?.toIntOrNull()
    }

    @Serializable
    private data class TokenRequest(
        @SerialName("project_id") val projectId: String? = null,
        @SerialName("kid") val kid: String? = null,
        @SerialName("sub") val sub: Map<String, String>? = null,
        @SerialName("exp") val exp: Int? = null
    )

    @Serializable
    private data class TokenResponse(
        @SerialName("customer_token") val token: String? = null,
        @SerialName("expire_time") val expiration: Int? = null
    )
}
