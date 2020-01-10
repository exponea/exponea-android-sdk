package com.exponea.sdk.manager

import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.BannerResult
import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationDeserializer
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.CustomerRecommendationResponse
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.Personalization
import com.exponea.sdk.models.Result
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.util.Logger
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

internal class FetchManagerImpl(
    private val api: ExponeaService
) : FetchManager {
    private val gson = GsonBuilder()
        .registerTypeAdapter(
            CustomerRecommendation::class.java,
            CustomerRecommendationDeserializer()
        ).create()

    private fun <T> getFetchCallback(
        resultType: TypeToken<Result<T>>, // gson needs to know the type of result, T gets erased at compile time
        onSuccess: (Result<T>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ): Callback {
        return object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseCode = response.code()
                Logger.d(this, "Response Code: $responseCode")
                val jsonBody = response.body()?.string()
                if (response.isSuccessful) {
                    var result: Result<T>?
                    try {
                        result = gson.fromJson<Result<T>>(jsonBody, resultType.type)
                        if (result.results == null) {
                            throw Exception("Unable to parse response from the server.")
                        }
                    } catch (e: Exception) {
                        val error = FetchError(jsonBody, e.localizedMessage ?: "Unknown error")
                        Logger.e(this, "Failed to deserialize fetch response: $error")
                        onFailure(Result(false, error))
                        return
                    }
                    onSuccess(result) // we need to call onSuccess outside of pokemon exception handling above
                } else {
                    val error = FetchError(jsonBody, response.message())
                    Logger.e(this, "Failed to fetch data: $error")
                    onFailure(Result(false, error))
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                val error = FetchError(null, e.localizedMessage ?: "Unknown error")
                Logger.e(this, "Fetch configuration Failed $e")
                onFailure(Result(false, error))
            }
        }
    }

    override fun fetchBannerConfiguration(
        projectToken: String,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<Personalization>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.getBannerConfiguration(projectToken).enqueue(
            getFetchCallback(
                object : TypeToken<Result<ArrayList<Personalization>>>() {},
                onSuccess,
                onFailure
            )
        )
    }

    override fun fetchBanner(
        projectToken: String,
        bannerConfig: Banner,
        onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.postFetchBanner(projectToken, bannerConfig).enqueue(
            getFetchCallback(
                object : TypeToken<Result<ArrayList<BannerResult>>>() {},
                onSuccess,
                onFailure
            )
        )
    }

    override fun fetchConsents(
        projectToken: String,
        onSuccess: (Result<ArrayList<Consent>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.postFetchConsents(projectToken).enqueue(
            getFetchCallback(
                object : TypeToken<Result<ArrayList<Consent>>>() {},
                onSuccess,
                onFailure
            )
        )
    }

    override fun fetchRecommendation(
        projectToken: String,
        recommendationRequest: CustomerRecommendationRequest,
        onSuccess: (Result<ArrayList<CustomerRecommendation>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.postFetchAttributes(projectToken, recommendationRequest).enqueue(
            getFetchCallback(
                object : TypeToken<Result<ArrayList<CustomerRecommendationResponse>>>() {},
                { result: Result<ArrayList<CustomerRecommendationResponse>> ->
                    if (result.results.isNotEmpty()) {
                        val innerResult = result.results[0]
                        if (innerResult.success && innerResult.value != null) {
                            onSuccess(Result(true, innerResult.value))
                        } else {
                            onFailure(Result(false, FetchError(null, innerResult.error ?: "Server returned error")))
                        }
                    } else {
                        onFailure(Result(false, FetchError(null, "Server returned empty results")))
                    }
                },
                onFailure
            )
        )
    }

    override fun fetchInAppMessages(
        projectToken: String,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<InAppMessage>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.postFetchInAppMessages(projectToken, customerIds).enqueue(
            getFetchCallback(
                object : TypeToken<Result<ArrayList<InAppMessage>>>() {},
                onSuccess,
                onFailure
            )
        )
    }
}
