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

    override fun fetchCustomerAttributes(projectToken: String,
                                         attributes: CustomerAttributes,
                                         onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
                                         onFailure: (Result<FetchError>) -> Unit) {

        api.postFetchAttributes(projectToken, attributes).enqueue(
                onResponse = {_, response: Response ->
                    val jsonBody = response.body()?.string()
                    val type =  object: TypeToken<Result<List<CustomerAttributeModel>>>(){}.type

                    if (response.code() in 200..203) {
                        val result = gson.fromJson<Result<List<CustomerAttributeModel>>>(jsonBody, type)
                        onSuccess(result)
                    } else {
                        val error = FetchError(jsonBody, response.message())
                        val result  = Result(false, results = error)
                        Logger.e(this, "Fetch Failed: $result")
                        onFailure(result)
                    }
                },
                onFailure = {_, exception ->
                    val error = FetchError(null, exception.toString())
                    val result = Result(false, error)
                    Logger.e(this, "Fetch failed: $result", exception)
                    onFailure(result)
                }
        )
    }

    override fun fetchCustomerEvents(projectToken: String,
                                     customerEvents: CustomerEvents,
                                     onSuccess: (Result<ArrayList<CustomerEventModel>>) -> Unit,
                                     onFailure: (Result<FetchError>) -> Unit) {

        api.postFetchEvents(projectToken, customerEvents).enqueue(
                onResponse = {_, response: Response ->
                    val jsonBody = response.body()?.string()
                    val type = object : TypeToken<Result<ArrayList<CustomerEventModel>>>(){}.type
                    if (response.code() in 200..203) {
                        val result = gson.fromJson<Result<ArrayList<CustomerEventModel>>>(jsonBody, type)
                        onSuccess(result)

                    } else {
                        val error = FetchError(jsonBody, response.message())
                        val result  = Result(false, results = error)
                        Logger.e(this, "Fetch Failed: $result")
                        onFailure(result)
                    }
                },
                onFailure = {_, exception: IOException ->
                    val error = FetchError(null, exception.toString())
                    val result = Result(false, error)
                    Logger.e(this, "Failed to fetch events: $result", exception)
                    onFailure(result)
                }
        )
    }

    override fun fetchBannerConfiguration(projectToken: String,
                                          customerIds: CustomerIds,
                                          onSuccess: (Result<ArrayList<Personalization>>) -> Unit,
                                          onFailure: (String) -> Unit) {

        api.getBannerConfiguration(projectToken)
                .enqueue(
                        onResponse = { _, response ->
                            val responseCode = response.code()
                            Logger.d(this, "Response Code: $responseCode")
                            val jsonBody = response.body()?.string()
                            if (responseCode in 200..299) {
                                try {
                                    val type = object : TypeToken<Result<ArrayList<Personalization>>>(){}.type
                                    val result = gson.fromJson<Result<ArrayList<Personalization>>>(jsonBody, type)
                                    onSuccess(result)
                                } catch (e: Exception) {
                                    // Return failure when find any exception while trying to deserialize the response.
                                    Logger.e(this, "Failed to deserialize banner configuration: ${e.localizedMessage}")
                                    onFailure("Failed to deserialize banner: ${e.localizedMessage}")
                                }
                            } else {
                                Logger.e(this, "Failed to fetch events: ${response.message()}\n" + "Body: $jsonBody")
                                onFailure("Failed to fetch events: ${response.message()}\n" + "Body: $jsonBody")
                            }
                        },
                        onFailure = { _, ioException ->
                            Logger.e(this, "Fetch configuration Failed $ioException")
                        }
                )
    }

    override fun fetchBanner(projectToken: String,
                             bannerConfig: Banner,
                             onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
                             onFailure: (String) -> Unit) {

        api.postFetchBanner(projectToken, bannerConfig)
                .enqueue(
                        onResponse = { _, response ->
                            val responseCode = response.code()
                            Logger.d(this, "Response Code: $responseCode")
                            val jsonBody = response.body()?.string()
                            if (responseCode in 200..299) {
                                try {
                                    val type = object : TypeToken<Result<ArrayList<BannerResult>>>(){}.type
                                    val result = gson.fromJson<Result<ArrayList<BannerResult>>>(jsonBody, type)
                                    onSuccess(result)
                                } catch (e: Exception) {
                                    // Return failure when find any exception while trying to deserialize the response.
                                    Logger.e(this, "Failed to deserialize banner: ${e.localizedMessage}")
                                    onFailure("Failed to deserialize banner: ${e.localizedMessage}")
                                }
                            } else {
                                Logger.e(this, "Failed to fetch events: ${response.message()}\n" + "Body: $jsonBody")
                                onFailure("Failed to fetch events: ${response.message()}\n" + "Body: $jsonBody")
                            }
                        },
                        onFailure = { _, ioException ->
                            Logger.e(this, "Fetch configuration Failed $ioException")
                        }
                )
    }
}