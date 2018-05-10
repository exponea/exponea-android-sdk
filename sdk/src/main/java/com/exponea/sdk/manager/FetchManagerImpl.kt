package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
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
                                         onFailure: (String) -> Unit) {

        api.postFetchAttributes(projectToken, attributes).enqueue(
                onResponse = {_, response: Response ->
                    val jsonBody = response.body()?.string()
                    val type =  object: TypeToken<Result<List<CustomerAttributeModel>>>(){}.type

                    if (response.code() in 200..203) {
                        val result = gson.fromJson<Result<List<CustomerAttributeModel>>>(jsonBody, type)
                        onSuccess(result)
                    } else {
                        Logger.e(this, "Fetch Failed: ${response.message()}\n" +
                                "Body: $jsonBody")
                        onFailure("Fetch failed: ${response.message()}\n" +
                                "Body: $jsonBody")
                    }
                },
                onFailure = {_, exception ->
                    Logger.e(this, "Fetch failed: exception caught($exception)")
                    onFailure(exception.toString())
                }
        )
    }

    override fun fetchCustomerEvents(projectToken: String,
                                     customerEvents: CustomerEvents,
                                     onSuccess: (Result<ArrayList<CustomerEventModel>>) -> Unit,
                                     onFailure: (String) -> Unit) {

        api.postFetchEvents(projectToken, customerEvents).enqueue(
                onResponse = {_, response: Response ->
                    val jsonBody = response.body()?.string()
                    val type = object : TypeToken<Result<ArrayList<CustomerEventModel>>>(){}.type
                    if (response.code() in 200..203) {
                        val result = gson.fromJson<Result<ArrayList<CustomerEventModel>>>(jsonBody, type)
                        onSuccess(result)

                    } else {
                        Logger.e(this, "Failed to fetch events: ${response.message()}\n" +
                                "Body: $jsonBody")
                        onFailure("Failed to fetch events: ${response.message()}\n" +
                                "Body: $jsonBody")
                    }
                },
                onFailure = {_, exception: IOException ->
                    Logger.e(this, "Failed to fetch events", exception)
                    onFailure(exception.toString())
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
                                    Logger.e(this, "Failed to deserialize banner configuration: ${e.localizedMessage}")
                                }
                            } else {
                                Logger.e(this, "Failed to fetch events: ${response.message()}\n" + "Body: $jsonBody")
                                onFailure("Failed to fetch events: ${response.message()}\n" + "Body: $jsonBody")
                            }
                        },
                        onFailure = { _, ioException ->
                            Logger.e(this, "Fetch configuration Failed $ioException")
                            ioException.printStackTrace()
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
                                    Logger.e(this, "Failed to deserialize banner: ${e.localizedMessage}")
                                }
                            } else {
                                Logger.e(this, "Failed to fetch events: ${response.message()}\n" + "Body: $jsonBody")
                                onFailure("Failed to fetch events: ${response.message()}\n" + "Body: $jsonBody")
                            }
                        },
                        onFailure = { _, ioException ->
                            Logger.e(this, "Fetch configuration Failed $ioException")
                            ioException.printStackTrace()
                        }
                )
    }
}