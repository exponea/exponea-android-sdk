package com.exponea.sdk.models

internal data class ApiEndPoint(private val endPointName: EndPointName, private val token: String) {
    private val splitterToken = "$$$"
    private var url: String = ""

    enum class EndPointName {
        TRACK_CUSTOMERS,
        TRACK_EVENTS,
        TRACK_CAMPAIGN,
        CUSTOMERS_RECOMMENDATION,
        CONFIGURE_BANNER,
        SHOW_BANNER,
        CONSENTS
    }

    init {
        this.url = when (endPointName) {
            EndPointName.TRACK_CUSTOMERS -> "/track/v2/projects/$splitterToken/customers"
            EndPointName.TRACK_EVENTS -> "/track/v2/projects/$splitterToken/customers/events"
            EndPointName.TRACK_CAMPAIGN -> "/track/v2/projects/$splitterToken/campaigns/clicks"
            EndPointName.CUSTOMERS_RECOMMENDATION -> "/data/v2/$splitterToken/customers/recommendation"
            EndPointName.CONFIGURE_BANNER -> "/track/v2/projects/$splitterToken/configuration/banners"
            EndPointName.SHOW_BANNER -> "/data/v2/projects/$splitterToken/customers/personalisation/show-banners"
            EndPointName.CONSENTS -> "/data/v2/projects/$splitterToken/consent/categories"
        }

        this.url = this.url.replace(splitterToken, token)
    }

    override fun toString(): String {
        return url
    }
}
