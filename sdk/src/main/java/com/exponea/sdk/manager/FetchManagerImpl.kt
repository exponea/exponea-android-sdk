package com.exponea.sdk.manager

import androidx.annotation.WorkerThread
import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.CustomerRecommendationResponse
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.RecommendationsRequest
import com.exponea.sdk.models.RecommendationsResponse
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.internal.closeQuietly

internal class FetchManagerImpl(
    private val api: ExponeaService,
    private val gson: Gson
) : FetchManager {

    /**
     * Creates Callback to read HTTP response body and parses it into standardised {com.exponea.sdk.models.Result}
     */
    private fun <T> getStandardFetchCallback(
        resultType: TypeToken<Result<T>>,
        onSuccess: (Result<T>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ): Callback {
        return object : Callback {
            @Suppress("UNCHECKED_CAST")
            override fun onResponse(call: Call, response: Response) {
                val result = parseStandardResult(response, resultType)
                if (result.success == true) {
                    onSuccess(result as Result<T>)
                } else {
                    onFailure(result as Result<FetchError>)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onFailure(parseErrorResult(e))
            }
        }
    }

    /**
     * Creates Callback to read HTTP response body and parses it into non-standardised structure of T.
     * Data are transformed to {com.exponea.sdk.models.Result} with success or error status info.
     */
    private fun <T> getFetchRawCallback(
        resultType: TypeToken<T>,
        onSuccess: (Result<T>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ): Callback {
        return object : Callback {
            @Suppress("UNCHECKED_CAST")
            override fun onResponse(call: Call, response: Response) {
                val jsonBody = response.body?.string()
                val result = parseRawResponse(response, jsonBody, resultType)
                if (result.success == true) {
                    onSuccess(result as Result<T>)
                } else {
                    onFailure(result as Result<FetchError>)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onFailure(parseErrorResult(e))
            }
        }
    }

    private fun <T> parseRawResponse(
        response: Response,
        jsonBody: String?,
        resultType: TypeToken<T>
    ): Result<out Any?> {
        val responseCode = response.code
        Logger.d(this, "Response Code: $responseCode")
        if (response.isSuccessful) {
            try {
                val result: T? = gson.fromJson(jsonBody, resultType.type)
                return Result(true, result)
            } catch (e: Exception) {
                val error = FetchError(jsonBody, e.localizedMessage ?: "Unknown error")
                Logger.e(this, "Failed to deserialize fetch response: $error")
                return Result(false, error)
            }
        } else {
            val error = FetchError(jsonBody, response.message)
            Logger.e(this, "Failed to fetch data: $error")
            return Result(false, error)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> parseStandardResult(response: Response, resultType: TypeToken<Result<T>>): Result<out Any?> {
        val jsonBody = response.body?.string()
        val result = parseRawResponse(response, jsonBody, resultType)
        if (result.success != true) {
            return result as Result<FetchError>
        }
        val standardResult = result.results as? Result<T?>
        if (standardResult?.success == null) {
            return Result(
                false,
                FetchError(jsonBody, "Unable to parse response from the server.")
            )
        }
        if (!standardResult.success) {
            Logger.e(this, "Server returns false state")
            return Result(
                false,
                FetchError(null, "Failure state from server returned")
            )
        }
        return standardResult
    }

    private fun parseErrorResult(e: Exception): Result<FetchError> {
        val error = FetchError(null, e.localizedMessage ?: "Unknown error")
        Logger.e(this, "Fetch configuration Failed $e")
        return Result(false, error)
    }

    private fun getVoidCallback(
        onSuccess: (Result<Any?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ): Callback {
        val emptyType = object : TypeToken<Result<Any?>>() {}
        return getStandardFetchCallback(emptyType, onSuccess, onFailure)
    }

    override fun fetchConsents(
        integrationConfig: IntegrationConfig,
        onSuccess: (Result<ArrayList<Consent>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.fetchConsents(integrationConfig).enqueue(
            getStandardFetchCallback(
                object : TypeToken<Result<ArrayList<Consent>?>>() {},
                { result: Result<ArrayList<Consent>?> ->
                    if (result.results?.isNotEmpty() ?: false) {
                        onSuccess(Result(true, result.results!!))
                    } else {
                        onFailure(Result(false, FetchError(null, "Server returned empty results")))
                    }
                },
                onFailure
            )
        )
    }

    override fun fetchRecommendation(
        integrationConfig: IntegrationConfig,
        customerIds: Map<String, Any?>,
        options: CustomerRecommendationOptions,
        onSuccess: (Result<ArrayList<CustomerRecommendation>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        when (integrationConfig) {
            is ProjectConfig ->
                api.postFetchAttributes(
                    integrationConfig,
                    CustomerRecommendationRequest(customerIds, options)
                ).enqueue(
                    getStandardFetchCallback(
                        object : TypeToken<Result<ArrayList<CustomerRecommendationResponse>?>>() {},
                        { result: Result<ArrayList<CustomerRecommendationResponse>?> ->
                            result.results?.takeIf { it.isNotEmpty() }?.let { results ->
                                val innerResult = results[0]
                                if (innerResult.success && innerResult.value != null) {
                                    onSuccess(Result(true, innerResult.value))
                                } else {
                                    onFailure(
                                        Result(
                                            false,
                                            FetchError(null, innerResult.error ?: "Server returned error")
                                        )
                                    )
                                }
                            } ?: onFailure(
                                Result(false, FetchError(null, "Server returned empty results"))
                            )
                        },
                        onFailure
                    )
                )
            is StreamConfig ->
                api.postFetchRecommendations(
                    integrationConfig,
                    RecommendationsRequest(
                        customerIds = customerIds,
                        engineId = options.id,
                        fillWithRandom = options.fillWithRandom,
                        size = options.size,
                        items = options.items,
                        noTrack = options.noTrack,
                        catalogAttributesWhitelist = options.catalogAttributesWhitelist
                    )
                ).enqueue(
                    getFetchRawCallback(
                        object : TypeToken<RecommendationsResponse>() {},
                        { result: Result<RecommendationsResponse> ->
                            if (result.results.success && result.results.data != null) {
                                onSuccess(Result(true, result.results.data))
                            } else {
                                onFailure(
                                    Result(
                                        false,
                                        FetchError(
                                            null,
                                            result.results.errors.takeIf { it.isNotEmpty() }
                                                ?: "Server returned error"
                                        )
                                    )
                                )
                            }
                        },
                        onFailure
                    )
                )
        }
    }

    override fun fetchInAppMessages(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<InAppMessage>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.postFetchInAppMessages(integrationConfig, customerIds).enqueue(
            getStandardFetchCallback(
                object : TypeToken<Result<ArrayList<InAppMessage>?>>() {},
                { result: Result<ArrayList<InAppMessage>?> ->
                    onSuccess(Result(true, result.results ?: arrayListOf()))
                },
                onFailure
            )
        )
    }

    override fun fetchAppInbox(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        syncToken: String?,
        applicationId: String,
        onSuccess: (Result<ArrayList<MessageItem>?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.postFetchAppInbox(integrationConfig, customerIds, syncToken, applicationId).enqueue(
            getStandardFetchCallback(
                object : TypeToken<Result<ArrayList<MessageItem>?>>() {},
                onSuccess,
                onFailure
            )
        )
    }

    override fun markAppInboxAsRead(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        syncToken: String,
        messageIds: List<String>,
        onSuccess: (Result<Any?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.postReadFlagAppInbox(integrationConfig, customerIds, messageIds, syncToken).enqueue(
            getVoidCallback(onSuccess, onFailure)
        )
    }

    override fun fetchStaticInAppContentBlocks(
        integrationConfig: IntegrationConfig,
        onSuccess: (Result<ArrayList<InAppContentBlock>?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        api.fetchStaticInAppContentBlocks(integrationConfig).enqueue(
            getStandardFetchCallback(
                object : TypeToken<Result<ArrayList<InAppContentBlock>?>>() {},
                onSuccess,
                onFailure
            )
        )
    }

    override fun fetchPersonalizedContentBlocks(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        contentBlockIds: List<String>,
        onSuccess: (Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        if (contentBlockIds.isEmpty()) {
            onSuccess(Result(true, ArrayList()))
            return
        }
        api.fetchPersonalizedInAppContentBlocks(integrationConfig, customerIds, contentBlockIds).enqueue(
            getStandardFetchCallback(
                object : TypeToken<Result<ArrayList<InAppContentBlockPersonalizedData>?>>() {},
                onSuccess,
                onFailure
            )
        )
    }

    override fun fetchSegments(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        onSuccess: (Result<SegmentationCategories>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) {
        val engagementCookieId = customerIds.cookie
        if (engagementCookieId.isNullOrEmpty()) {
            Logger.w(this, "Fetch of segments for no cookie ID is forbidden")
            onFailure(Result(false, FetchError(null, "No cookie ID found")))
            return
        }
        api.fetchSegments(integrationConfig, engagementCookieId).enqueue(
            getFetchRawCallback(
                resultType = object : TypeToken<Map<String, ArrayList<Map<String, String>>>?>() {},
                onSuccess = { rawData ->
                    val segmentationsData = rawData.results
                    if (segmentationsData == null) {
                        onFailure(Result(
                            false,
                            FetchError(null, "Fetch of segments got NULL response")
                        ))
                    } else {
                        val segmentations = SegmentationCategories(segmentationsData.mapValues { rawEntry ->
                            ArrayList(rawEntry.value.map { rawSegments -> Segment(rawSegments) })
                        })
                        val transformedData = Result(
                            success = rawData.success,
                            results = segmentations
                        )
                        onSuccess(transformedData)
                    }
                },
                onFailure = onFailure
            )
        )
    }

    @WorkerThread
    override fun linkCustomerIdsSync(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds
    ): Result<out Any?> {
        val engagementCookieId = customerIds.cookie
        if (engagementCookieId.isNullOrEmpty()) {
            Logger.w(this, "Fetch of segments for no cookie ID is forbidden")
            return Result(false, FetchError(null, "No cookie ID found"))
        }
        val externalIds = customerIds.externalIds
        val call = api.linkIdsToCookie(
            integrationConfig, engagementCookieId, externalIds
        )
        var response: Response? = null
        try {
            response = call.execute()
            val emptyType = object : TypeToken<Any?>() {}
            val jsonBody = response.body?.string()
            return parseRawResponse(response, jsonBody, emptyType)
        } catch (e: Exception) {
            return parseErrorResult(e)
        } finally {
            response?.closeQuietly()
        }
    }
}
