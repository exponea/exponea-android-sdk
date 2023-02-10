package com.exponea.example.managers

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.exponea.example.App
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * !!! WARN for developers.
 * This implementation is just proof of concept. Do not rely on any part of it as possible solution for Customer Token
 * handling.
 * It is in your own interest to provide proper token generating and handling of its cache, expiration and secured storing.
 */
class CustomerTokenStorage(
    private var networkManager: NetworkManager = NetworkManager(),
    private val gson: Gson = Gson(),
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.instance)
) {

    companion object {
        private const val CUSTOMER_TOKEN_CONF = "CustomerTokenConf"
        val INSTANCE = CustomerTokenStorage()
    }

    init {
        loadConfiguration()
        val confJson = gson.toJson(confAsMap())
        Logger.d(this, "[CTS] Conf loaded $confJson")
    }

    private var host: String? = null
    private var projectToken: String? = null
    private var publicKey: String? = null
    private var customerIds: HashMap<String, String>? = null
    private var expiration: Int? = null

    private var tokenCache: String? = null

    private var lastTokenRequestTime: Long = 0

    fun retrieveJwtToken(): String? {
        val now = System.currentTimeMillis()
        if (TimeUnit.MILLISECONDS.toMinutes(abs(now - lastTokenRequestTime)) < 5) {
            // allows request for token once per 5 minutes, doesn't care if cache is NULL
            Logger.d(this, "[CTS] Token retrieved within 5min, using cache $tokenCache")
            return tokenCache
        }
        lastTokenRequestTime = now
        if (tokenCache != null) {
            // return cached value
            Logger.d(this, "[CTS] Token cache returned $tokenCache")
            return tokenCache
        }
        synchronized(this) {
            // recheck nullity just in case
            if (tokenCache == null) {
                tokenCache = loadJwtToken()
            }
        }
        return tokenCache
    }

    private fun loadJwtToken(): String? {
        if (
            host == null || projectToken == null ||
            publicKey == null || customerIds == null || customerIds?.size == 0
        ) {
            Logger.d(this, "[CTS] Not configured yet")
            return null
        }
        val reqBody = hashMapOf(
            "project_id" to projectToken,
            "kid" to publicKey,
            "sub" to customerIds
        )
        expiration?.let {
            reqBody.put("exp", it)
        }
        val jsonRequest = gson.toJson(reqBody)
        val response = networkManager.post(
            "$host/webxp/exampleapp/customertokens",
            null,
            jsonRequest
        ).execute()
        Logger.d(this, "[CTS] Requested for token with $jsonRequest")
        if (!response.isSuccessful) {
            if (response.code == 404) {
                // that is fine, only some BE has this endpoint
                Logger.d(this, "[CTS] Token request returns 404")
                return null
            }
            Logger.e(this, "[CTS] Token request returns ${response.code}")
            return null
        }
        val jsonResponse = response.body?.string()
        val responseData = try {
            gson.fromJson(jsonResponse, Response::class.java)
        } catch (e: Exception) {
            Logger.e(this, "[CTS] Token cannot be parsed from $jsonResponse")
            return null
        }
        if (responseData?.token == null) {
            Logger.e(this, "[CTS] Token received NULL")
        }
        Logger.d(this, "[CTS] Token received ${responseData?.token}")
        return responseData?.token
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
        // reset token
        tokenCache = null
        lastTokenRequestTime = 0
    }

    private fun storeConfig() {
        val confMap = confAsMap()
        val confAsJson = gson.toJson(confMap)
        prefs.edit()
            .putString(CUSTOMER_TOKEN_CONF, confAsJson)
            .apply()
    }
    private fun confAsMap(): Map<String, String?> {
        return mapOf(
            "host" to this.host,
            "projectToken" to this.projectToken,
            "publicKey" to this.publicKey,
            "customerIds" to this.customerIds?.let { gson.toJson(it) },
            "expiration" to this.expiration?.toString()
        )
    }

    private fun loadConfiguration() {
        val confAsJson = prefs.getString(CUSTOMER_TOKEN_CONF, "")
        if (confAsJson.isNullOrEmpty()) {
            return
        }
        val confAsMap: Map<String, String?> = gson.fromJson(
            confAsJson,
            object : TypeToken<Map<String, String?>>() {}.type
        )
        this.host = confAsMap["host"]
        this.projectToken = confAsMap["projectToken"]
        this.publicKey = confAsMap["publicKey"]
        val customerIdsMap = gson.fromJson<HashMap<String, String>>(
            confAsMap["customerIds"] ?: "{}", object : TypeToken<HashMap<String, String>>() {}.type
        )
        this.customerIds = customerIdsMap
        this.expiration = confAsMap["expiration"]?.toInt()
    }

    data class Response(
        @SerializedName("customer_token")
        val token: String?,

        @SerializedName("expire_time")
        val expiration: Int?
    )
}
