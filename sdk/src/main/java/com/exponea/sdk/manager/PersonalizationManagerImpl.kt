package com.exponea.sdk.manager

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.*
import com.exponea.sdk.util.Logger
import java.util.*
import kotlin.collections.ArrayList

/**
 * Personalization Manager is responsible to get the banner ids and fetch
 * the content for personalization of active banners.
 * When getPersonalization is called, the banners will be show in a web view instance.
 */

internal class PersonalizationManagerImpl(
        private val context: Context
) : PersonalizationManager {

    private var preferencesIds: MutableList<String> = mutableListOf()

    override fun showBanner(projectToken: String, customerIds: CustomerIds) {
        getBannersConfiguration(
                projectToken = projectToken,
                customerIds = customerIds,
                onSuccess = {
                    if (canShowBanner(it.results)) {
                        getPersonalization(projectToken, customerIds)
                    }
                },
                onFailure = {
                    Logger.e(this, "Check the error log for more information.")
                }
        )
    }

    /**
     * Call the fetch manager and get the banner configuration.
     */

    override fun getBannersConfiguration(
            projectToken: String,
            customerIds: CustomerIds,
            onSuccess: (Result<ArrayList<Personalization>>) -> Unit,
            onFailure: (String) -> Unit) {

        Exponea.component.fetchManager.fetchBannerConfiguration(
                projectToken = projectToken,
                customerIds = customerIds,
                onSuccess = onSuccess,
                onFailure = onFailure)
    }

    /**
     * Call the fetch manager and get the banner.
     */

    override fun getPersonalization(projectToken: String, customerIds: CustomerIds) {

        val banner = Banner(customerIds = customerIds, personalizationIds = preferencesIds)

        Exponea.component.fetchManager.fetchBanner(
                projectToken = projectToken,
                bannerConfig = banner,
                onSuccess = {
                    val data = it.results.first()
                    if (saveHtml(data)) showWebView(data.script, data.style)
                },
                onFailure = {
                    Logger.e(this, "Check the error log for more information.")
                }
        )
    }

    private fun canShowBanner(personalization: ArrayList<Personalization>) : Boolean {

        var isExpirationValid: Boolean = true
        var isMobileAvailable: Boolean = false

        for (data in personalization) {
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

            // Only shows the valid banners.
            if (isExpirationValid && isMobileAvailable) {
                data.id?.let {
                    preferencesIds.add(it)
                }
            }
            else {
                Logger.d(this, "Banner ${data.id} is not valid")
            }
        }
        return preferencesIds.isNotEmpty()
    }

    /**
     * Save the html content into a temporary file.
     */

    private fun saveHtml(data: BannerResult) : Boolean {
        data.html?.let {
            Exponea.component.fileManager.createFile(
                    filename = Constants.General.bannerFilename,
                    type = Constants.General.bannerFilenameExt
            )

            Exponea.component.fileManager.writeToFile(
                    filename = Constants.General.bannerFullFilename,
                    text = it
            )
            return true
        }
        return false
    }

    /**
     * Receive the script and css from the exponea api and prepare
     * the webview to be presented
     */

    private fun showWebView(script: String?, css: String?) {
        val webView: WebView = WebView(context)
        // Inject the javascript into webview.
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject the JavaScript into the WebView if exists.
                script?.let { webView.loadUrl(it) }
                // Inject the CSS into the WebView if exits.
                css?.let { webView.loadUrl(it) }
            }
        }
        // Load webview with all the received data.
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