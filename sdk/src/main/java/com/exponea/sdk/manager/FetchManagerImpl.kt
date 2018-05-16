package com.exponea.sdk.manager

import com.exponea.sdk.models.*
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Response
import java.io.IOException

class FetchManagerImpl(val api: ExponeaService, val gson: Gson) : FetchManager {

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
}