package com.exponea.sdk.models

internal data class ApiEndPoint(
    private val endPointName: EndPointName,
    private val pathParams: Map<String, String> = emptyMap(),
    private val queryParams: List<Pair<String, String>> = emptyList()
) {

    companion object {
        fun forName(name: EndPointName) = ApiEndPoint(name)

        internal const val TOKEN_PATH_PARAM = "$$\$TOK$$$"
        internal const val COOKIE_ID_PATH_PARAM = "$$\$CIT$$$"
    }

    enum class EndPointName(internal val urlTemplate: String) {
        TRACK_CUSTOMERS("/track/v2/projects/$TOKEN_PATH_PARAM/customers"),
        TRACK_EVENTS("/track/v2/projects/$TOKEN_PATH_PARAM/customers/events"),
        TRACK_CAMPAIGN("/track/v2/projects/$TOKEN_PATH_PARAM/campaigns/clicks"),
        CUSTOMERS_ATTRIBUTES("/data/v2/projects/$TOKEN_PATH_PARAM/customers/attributes"),
        CONSENTS("/data/v2/projects/$TOKEN_PATH_PARAM/consent/categories"),
        IN_APP_MESSAGES("/webxp/s/$TOKEN_PATH_PARAM/inappmessages?v=1"),
        PUSH_SELF_CHECK("/webxp/projects/$TOKEN_PATH_PARAM/appinbox/fetch"),
        MESSAGE_INBOX("/webxp/projects/$TOKEN_PATH_PARAM/appinbox/markasread"),
        MESSAGE_INBOX_READ("/campaigns/send-self-check-notification?project_id=$TOKEN_PATH_PARAM"),
        INAPP_CONTENT_BLOCKS_STATIC("/wxstatic/projects/$TOKEN_PATH_PARAM/bundle-android.json?v=2"),
        INAPP_CONTENT_BLOCKS_PERSONAL("/webxp/s/$TOKEN_PATH_PARAM/inappcontentblocks?v=2"),
        SEGMENTS("/webxp/projects/$TOKEN_PATH_PARAM/segments?cookie="),
        LINK_CUSTOMER_IDS("/webxp/projects/$TOKEN_PATH_PARAM/cookies/$COOKIE_ID_PATH_PARAM/link-ids")
    }

    override fun toString(): String {
        var requestUrl = endPointName.urlTemplate
        pathParams.forEach { pathParamDesc ->
            requestUrl = requestUrl.replace(pathParamDesc.key, pathParamDesc.value)
        }
        val queryParamsPart = queryParams.map {
            it.first + "=" + it.second
        }.joinToString("&")
        if (queryParamsPart.isNotBlank()) {
            requestUrl = "$requestUrl?$queryParamsPart"
        }
        return requestUrl
    }

    fun withToken(projectToken: String) = withPathParam(TOKEN_PATH_PARAM, projectToken)

    fun withPathParam(pathKey: String, pathValue: String): ApiEndPoint {
        return ApiEndPoint(
            endPointName = this.endPointName,
            pathParams = this.pathParams.plus(Pair(pathKey, pathValue)),
            queryParams = this.queryParams
        )
    }

    fun withQueryParam(queryKey: String, queryValue: String): ApiEndPoint {
        return ApiEndPoint(
            endPointName = this.endPointName,
            pathParams = this.pathParams,
            queryParams = this.queryParams.plus(Pair(queryKey, queryValue))
        )
    }
}
