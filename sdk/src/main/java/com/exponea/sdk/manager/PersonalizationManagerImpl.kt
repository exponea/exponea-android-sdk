package com.exponea.sdk.manager

import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.Personalization
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.util.FileManager.gson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue
import java.util.*

/**
 * Personalization Manager is responsible to get the banner ids and fetch
 * the content for personalization of active banners.
 * When getPersonalization the banners will be show in a web view instance.
 */

class PersonalizationManagerImpl(
        private val exponeaService: ExponeaService
) : PersonalizationManager {

    private val banner: Banner = Banner()
    private var preferencesIds: MutableList<String> = mutableListOf()

    override fun getBannersConfiguration(projectToken: String) {
        fetchConfiguration(projectToken)
    }

    override fun getPersonalization(projectToken: String, banner: Banner) {
        fetchActiveBanners(projectToken, banner)
    }

    private fun fetchConfiguration(projectToken: String) {
        exponeaService
                .getBannerConfiguration(projectToken)
                .enqueue(
                        { _, response ->
                            val responseCode = response.code()
                            Logger.d(this@PersonalizationManagerImpl, "Response Code: $responseCode")
                            if (responseCode in 200..299) {
                                val result = response.body().toString()
                                val personalization = gson.fromJson(result, Personalization::class.java)
                                onSerializadedSuccess(personalization)
                            } else {
                                Logger.d(
                                        this@PersonalizationManagerImpl,
                                        "Something went wrong while trying to deserialize Json")
                            }
                        },
                        { _, ioException ->
                            Logger.e(
                                    this@PersonalizationManagerImpl,
                                    "Fetch configuration Failed $ioException")
                            ioException.printStackTrace()
                        }
                )
    }

    private fun fetchActiveBanners(projectToken: String, banner: Banner) {
        exponeaService
                .postFetchBanner(projectToken, banner)
                .enqueue(
                        { _, response ->
                            val responseCode = response.code()
                            Logger.d(this@PersonalizationManagerImpl, "Response Code: $responseCode")
                            if (responseCode in 200..299) {
                                val result = response.body().toString()
                                val personalization = gson.fromJson(result, Personalization::class.java)
                                onSerializadedSuccess(personalization)
                            } else {
                                Logger.d(
                                        this@PersonalizationManagerImpl,
                                        "Something went wrong while trying to deserialize Json")
                            }
                        },
                        { _, ioException ->
                            Logger.e(
                                    this@PersonalizationManagerImpl,
                                    "Fetch configuration Failed $ioException")
                            ioException.printStackTrace()
                        }
                )
    }

    private fun onSerializadedSuccess(personalization: Personalization) {

        var isExpirationValid: Boolean = true

        for (data in personalization.data) {
            // Check if the banner has expiration date and if it's valid.
            // If doesn't have any expiration setted, then is allowed.
            data.dateFilter?.let {
                if (it.enabled) {
                    isExpirationValid = checkExpirationDate(it.fromDate?.toLong(), it.toDate?.toLong())
                }
            }
        }
//
//        val data = personalization.data.flatMap { (id, _, _, _, _) -> listOf(id).toMutableList() }
//        banner.personalizationIds = data

    }

    // Helper Methods
    private fun checkExpirationDate(start: Long?, end: Long?): Boolean {

        val fromDate = start?.let { it } ?: run { return false }
        val toDate = end?.let { it } ?: run { return false }

        val startDate = Date(fromDate)
        val endDate = Date(toDate)
        val currDate = Date()

        return ( currDate >= startDate) && (currDate <= endDate)
    }

}