package com.exponea.sdk.manager

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.*
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue
import com.google.gson.Gson
import java.util.*

/**
 * Personalization Manager is responsible to get the banner ids and fetch
 * the content for personalization of active banners.
 * When getPersonalization is called, the banners will be show in a web view instance.
 */

class PersonalizationManagerImpl(
        private val exponeaService: ExponeaService,
        private val context: Context
) : PersonalizationManager {

    val gson = Gson()

    private var preferencesIds: MutableList<String> = mutableListOf()

    override fun showBanner(projectToken: String, customerIds: CustomerIds) {
        getBannersConfiguration(projectToken, customerIds)
    }

    override fun getBannersConfiguration(projectToken: String, customerIds: CustomerIds) {
        fetchConfiguration(projectToken, customerIds)
    }

    override fun getPersonalization(projectToken: String, customerIds: CustomerIds) {
        fetchActiveBanners(projectToken, customerIds)
    }

    private fun fetchConfiguration(projectToken: String, customerIds: CustomerIds) {
        exponeaService
                .getBannerConfiguration(projectToken)
                .enqueue(
                        { _, response ->
                            val responseCode = response.code()
                            Logger.d(this@PersonalizationManagerImpl, "Response Code: $responseCode")
                            if (responseCode in 200..299) {
                                val result = response.body().toString()
                                val personalization = gson.fromJson(result, Personalization::class.java)
                                onSerializadedSuccess(personalization, projectToken, customerIds)
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

    private fun fetchActiveBanners(projectToken: String, customerIds: CustomerIds) {

        val banner = Banner(
                customerIds = customerIds,
                personalizationIds = preferencesIds
        )

        exponeaService
                .postFetchBanner(projectToken, banner)
                .enqueue(
                        { _, response ->
                            val responseCode = response.code()
                            Logger.d(this@PersonalizationManagerImpl, "Response Code: $responseCode")
                            if (responseCode in 200..299) {
                                val result = response.body().toString()
                                val bannerResult = gson.fromJson(result, BannerResult::class.java)
                                Exponea.component.fileManager.createFile(
                                        filename = Constants.General.bannerFilename,
                                        type = Constants.General.bannerFilenameExt
                                )
                                Exponea.component.fileManager.writeToFile(
                                        filename = Constants.General.bannerFullFilename,
                                        text = result
                                )
                                // Present the WebView with Banner data.
                                TODO("Wait for Exponea answer on how to show the banner when we have more than 1 banner.")
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

    private fun onSerializadedSuccess(
            personalization: Personalization,
            projectToken: String,
            customerIds: CustomerIds
    ) {

        var isExpirationValid: Boolean = true
        var isMobileAvailable: Boolean = false

        for (data in personalization.data) {
            // Check if the banner has expiration date and if it's valid.
            // If doesn't have any expiration setted, then is allowed.
            data.dateFilter?.let {
                if (it.enabled) {
                    isExpirationValid = checkExpirationDate(it.fromDate?.toLong(), it.toDate?.toLong())
                }
            }
            data.deviceTarget?.let {
                if ((it.type == "mobile") || (it.type == "any")) {
                    isMobileAvailable = true
                }
            }

            if (isExpirationValid && isMobileAvailable) {
                data.id?.let {
                    preferencesIds.add(it)
                }
                fetchActiveBanners(projectToken, customerIds)
            }
            else {
                Logger.d(
                        this@PersonalizationManagerImpl,
                        "There is no banner to show")
            }
        }
    }

    private fun showWebView(script: String, css: String) {
        val webView: WebView = WebView(context)
        webView.settings.javaScriptEnabled = true
        // Inject the javascript into webview.
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject the JavaScript into the WebView.
                webView.loadUrl(script)
                // Inject the CSS into the WebView.
                webView.loadUrl(css)
            }
        }
        webView.loadUrl(ClassLoader.getSystemResource(Constants.General.bannerFullFilename).file)
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