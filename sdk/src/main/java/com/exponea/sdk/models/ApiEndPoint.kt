package com.exponea.sdk.models

internal data class ApiEndPoint(
    private val endPointName: EndPointName
) {

    companion object {
        const val TOKEN_PLACEHOLDER = "{projectToken}"
        const val STREAM_ID_PLACEHOLDER = "{streamId}"
        const val COOKIE_ID_PLACEHOLDER = "{cookieId}"
    }

    enum class EndPointName(internal val urlTemplate: String) {
        TRACK_CUSTOMERS("/track/v2/projects/$TOKEN_PLACEHOLDER/customers"),
        U_TRACK_CUSTOMERS("/track/u/v1/customers?stream_id=$STREAM_ID_PLACEHOLDER"),
        TRACK_EVENTS("/track/v2/projects/$TOKEN_PLACEHOLDER/customers/events"),
        U_TRACK_EVENTS("/track/u/v1/customers/events?stream_id=$STREAM_ID_PLACEHOLDER"),
        TRACK_CAMPAIGN("/track/v2/projects/$TOKEN_PLACEHOLDER/campaigns/clicks"),
        U_TRACK_CAMPAIGN("/track/u/v1/campaigns/clicks?stream_id=$STREAM_ID_PLACEHOLDER"),
        CUSTOMERS_ATTRIBUTES("/data/v2/projects/$TOKEN_PLACEHOLDER/customers/attributes"),
        U_RECOMMENDATIONS("/optimization/streams/$STREAM_ID_PLACEHOLDER/recommend/user"),
        CONSENTS("/data/v2/projects/$TOKEN_PLACEHOLDER/consent/categories"),
        U_CONSENTS("/data/v2/streams/$STREAM_ID_PLACEHOLDER/consent/categories"),
        IN_APP_MESSAGES("/webxp/s/$TOKEN_PLACEHOLDER/inappmessages?compatibility=3"),
        U_IN_APP_MESSAGES("/webxp/streams/$STREAM_ID_PLACEHOLDER/inappmessages?compatibility=3"),
        PUSH_SELF_CHECK("/campaigns/send-self-check-notification?project_id=$TOKEN_PLACEHOLDER"),
        U_PUSH_SELF_CHECK("/campaigns/streams/$STREAM_ID_PLACEHOLDER/send-self-check-notification"),
        MESSAGE_INBOX("/webxp/projects/$TOKEN_PLACEHOLDER/appinbox/fetch"),
        U_MESSAGE_INBOX("/webxp/streams/$STREAM_ID_PLACEHOLDER/appinbox/fetch"),
        MESSAGE_INBOX_READ("/webxp/projects/$TOKEN_PLACEHOLDER/appinbox/markasread"),
        U_MESSAGE_INBOX_READ("/webxp/streams/$STREAM_ID_PLACEHOLDER/appinbox/markasread"),
        INAPP_CONTENT_BLOCKS_STATIC("/wxstatic/projects/$TOKEN_PLACEHOLDER/bundle-android.json?v=2"),
        U_INAPP_CONTENT_BLOCKS_STATIC("/wxstatic/streams/$STREAM_ID_PLACEHOLDER/bundle-android.json?v=2"),
        INAPP_CONTENT_BLOCKS_PERSONAL("/webxp/s/$TOKEN_PLACEHOLDER/inappcontentblocks?v=2"),
        U_INAPP_CONTENT_BLOCKS_PERSONAL("/webxp/streams/$STREAM_ID_PLACEHOLDER/inappcontentblocks?v=2"),
        SEGMENTS("/webxp/projects/$TOKEN_PLACEHOLDER/segments?cookie=$COOKIE_ID_PLACEHOLDER"),
        U_SEGMENTS("/webxp/streams/$STREAM_ID_PLACEHOLDER/segments?cookie=$COOKIE_ID_PLACEHOLDER"),
        LINK_CUSTOMER_IDS("/webxp/projects/$TOKEN_PLACEHOLDER/cookies/$COOKIE_ID_PLACEHOLDER/link-ids"),
        U_LINK_CUSTOMER_IDS("/webxp/streams/$STREAM_ID_PLACEHOLDER/cookies/$COOKIE_ID_PLACEHOLDER/link-ids")
    }

    fun applyPlaceholders(placeholderMappings: Map<String, String>): String {
        var url = endPointName.urlTemplate
        placeholderMappings.forEach {
            url = url.replace(it.key, it.value)
        }
        return url
    }

    fun applyStreamId(streamId: String) = applyPlaceholders(mapOf(STREAM_ID_PLACEHOLDER to streamId))

    fun applyProjectToken(projectToken: String) = applyPlaceholders(mapOf(TOKEN_PLACEHOLDER to projectToken))
}
