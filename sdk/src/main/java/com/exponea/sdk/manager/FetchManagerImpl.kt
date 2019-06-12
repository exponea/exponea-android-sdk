package com.exponea.sdk.manager

import com.exponea.sdk.models.*
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Response
import java.io.IOException

class FetchManagerImpl(
        private val api: ExponeaService,
        private val gson: Gson
) : FetchManager {

    override fun fetchBannerConfiguration(
            projectToken: String,
            customerIds: CustomerIds,
            onSuccess: (Result<ArrayList<Personalization>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    ) {

        api.getBannerConfiguration(projectToken)
                .enqueue(
                        onResponse = { _, response ->
                            val responseCode = response.code()
                            Logger.d(this, "Response Code: $responseCode")
                            val jsonBody = response.body()?.string()
                            if (responseCode in 200..299) {
                                try {
                                    val type = object : TypeToken<Result<ArrayList<Personalization>>>() {}.type
                                    val result = gson.fromJson<Result<ArrayList<Personalization>>>(
                                            jsonBody,
                                            type
                                    )
                                    onSuccess(result)
                                } catch (e: Exception) {
                                    // Return failure when find any exception while trying to deserialize the response.
                                    val error = FetchError(jsonBody, e.localizedMessage)
                                    Logger.e(
                                            this,
                                            "Failed to deserialize banner configuration: $error"
                                    )
                                    onFailure(Result(false, error))
                                }
                            } else {
                                val error = FetchError(jsonBody, response.message())
                                Logger.e(this, "Failed to fetch events: $error")
                                onFailure(Result(false, error))
                            }
                        },
                        onFailure = { _, ioException ->
                            val error = FetchError(null, ioException.localizedMessage)
                            Logger.e(this, "Fetch configuration Failed $ioException")
                            onFailure(Result(false, error))
                        }
                )
    }

    override fun fetchBanner(
            projectToken: String,
            bannerConfig: Banner,
            onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    ) {

        api.postFetchBanner(projectToken, bannerConfig)
                .enqueue(
                        onResponse = { _, response ->
                            val responseCode = response.code()
                            Logger.d(this, "Response Code: $responseCode")
                            val jsonBody = response.body()?.string()
                            if (responseCode in 200..299) {
                                try {
                                    val type = object : TypeToken<Result<ArrayList<BannerResult>>>() {}.type
                                    val result = gson.fromJson<Result<ArrayList<BannerResult>>>(
                                            jsonBody,
                                            type
                                    )
                                    onSuccess(result)
                                } catch (e: Exception) {
                                    // Return failure when find any exception while trying to deserialize the response.
                                    val error = FetchError(jsonBody, e.localizedMessage)
                                    Logger.e(this, "Failed to deserialize banner: $error")
                                    onFailure(Result(false, error))
                                }
                            } else {
                                val error = FetchError(jsonBody, response.message())
                                Logger.e(this, "Failed to fetch events: $error")
                                onFailure(Result(false, error))
                            }
                        },
                        onFailure = { _, ioException ->
                            val error = FetchError(null, ioException.localizedMessage)
                            Logger.e(this, "Fetch configuration Failed: $error", ioException)
                            onFailure(Result(false, error))
                        }
                )
    }

    override fun fetchConsents(
            projectToken: String,
            onSuccess: (Result<ArrayList<Consent>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    ) {
        api.postFetchConsents(projectToken)
                .enqueue(
                        onResponse = { _, response ->
                            val responseCode = response.code()
                            Logger.d(this, "Response Code: $responseCode")
                            val jsonBody = response.body()?.string()
                            if (response.isSuccessful) {
                                try {
                                    val type = object : TypeToken<Result<ArrayList<Consent>>>() {}.type
                                    val result = gson.fromJson<Result<ArrayList<Consent>>>(
                                            jsonBody,
                                            type
                                    )
                                    onSuccess(result)
                                } catch (e: Exception) {
                                    // Return failure when find any exception while trying to deserialize the response.
                                    val error = FetchError(jsonBody, e.localizedMessage)
                                    Logger.e(this, "Failed to deserialize banner: $error")
                                    onFailure(Result(false, error))
                                }
                            } else {
                                val error = FetchError(jsonBody, response.message())
                                Logger.e(this, "Failed to fetch events: $error")
                                onFailure(Result(false, error))
                            }
                        },
                        onFailure = { _, ioException ->
                            val error = FetchError(null, ioException.localizedMessage)
                            Logger.e(this, "Fetch configuration Failed: $error", ioException)
                            onFailure(Result(false, error))
                        }
                )
    }
}