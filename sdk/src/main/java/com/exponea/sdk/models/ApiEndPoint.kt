package com.exponea.sdk.models

internal data class ApiEndPoint(private val endPointName: EndPointName, private val token: String) {
    private val splitterToken = "$$$"
    private var url: String = ""

    enum class EndPointName {
        TRACK_CUSTOMERS,
        TRACK_EVENTS,
        TRACK_CAMPAIGN,
        TOKEN_ROTATE,
        TOKEN_REVOKE,
        CUSTOMERS_PROPERTY,
        CUSTOMERS_ID,
        CUSTOMERS_SEGMENTATION,
        CUSTOMERS_EXPRESSION,
        CUSTOMERS_PREDICTION,
        CUSTOMERS_RECOMMENDATION,
        CUSTOMERS_ATTRIBUTES,
        CUSTOMERS_ANONYMIZE,
        CUSTOMERS_EXPORT_ALL_PROPERTIES,
        CUSTOMERS_EXPORT_ALL,
        CONFIGURE_BANNER,
        SHOW_BANNER,
        CONSENTS
    }

    init {
        this.url = when (endPointName) {
            EndPointName.TRACK_CUSTOMERS                 -> "/track/v2/projects/$splitterToken/customers"
            EndPointName.TRACK_EVENTS                    -> "/track/v2/projects/$splitterToken/customers/events"
            EndPointName.TRACK_CAMPAIGN                  -> "/track/v2/projects/$splitterToken/campaigns/clicks"
            EndPointName.TOKEN_ROTATE                    -> "/data/v2/$splitterToken/tokens/rotate"
            EndPointName.TOKEN_REVOKE                    -> "/data/v2/$splitterToken/tokens/revoke"
            EndPointName.CUSTOMERS_PROPERTY              -> "/track/v2/projects/$splitterToken/customers"
            EndPointName.CUSTOMERS_ID                    -> "/data/v2/$splitterToken/customers/id"
            EndPointName.CUSTOMERS_SEGMENTATION          -> "/data/v2/$splitterToken/customers/segmentation"
            EndPointName.CUSTOMERS_EXPRESSION            -> "/data/v2/$splitterToken/customers/expression"
            EndPointName.CUSTOMERS_PREDICTION            -> "/data/v2/$splitterToken/customers/prediction"
            EndPointName.CUSTOMERS_RECOMMENDATION        -> "/data/v2/$splitterToken/customers/recommendation"
            EndPointName.CUSTOMERS_ATTRIBUTES            -> "/data/v2/projects/$splitterToken/customers/attributes"
            EndPointName.CUSTOMERS_ANONYMIZE             -> "/data/v2/$splitterToken/customers/anonymize"
            EndPointName.CUSTOMERS_EXPORT_ALL_PROPERTIES -> "/data/v2/$splitterToken/customers/export-one"
            EndPointName.CUSTOMERS_EXPORT_ALL            -> "/data/v2/$splitterToken/customers/export"
            EndPointName.CONFIGURE_BANNER                -> "/track/v2/projects/$splitterToken/configuration/banners"
            EndPointName.SHOW_BANNER                     -> "/data/v2/projects/$splitterToken/customers/personalisation/show-banners"
            EndPointName.CONSENTS                        -> "/data/v2/projects/$splitterToken/consent/categories"
        }

        this.url = this.url.replace(splitterToken, token)
    }

    override fun toString(): String {
        return url
    }
}
