package com.exponea.sdk.models

import com.exponea.sdk.models.ApiEndPoint.EndPointName.CONSENTS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.CUSTOMERS_ATTRIBUTES
import com.exponea.sdk.models.ApiEndPoint.EndPointName.INAPP_CONTENT_BLOCKS_PERSONAL
import com.exponea.sdk.models.ApiEndPoint.EndPointName.INAPP_CONTENT_BLOCKS_STATIC
import com.exponea.sdk.models.ApiEndPoint.EndPointName.IN_APP_MESSAGES
import com.exponea.sdk.models.ApiEndPoint.EndPointName.MESSAGE_INBOX
import com.exponea.sdk.models.ApiEndPoint.EndPointName.MESSAGE_INBOX_READ
import com.exponea.sdk.models.ApiEndPoint.EndPointName.PUSH_SELF_CHECK
import com.exponea.sdk.models.ApiEndPoint.EndPointName.TRACK_CAMPAIGN
import com.exponea.sdk.models.ApiEndPoint.EndPointName.TRACK_CUSTOMERS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.TRACK_EVENTS

internal data class ApiEndPoint(private val endPointName: EndPointName, private val token: String) {
    private val splitterToken = "$$$"
    private var url: String = ""

    enum class EndPointName {
        TRACK_CUSTOMERS,
        TRACK_EVENTS,
        TRACK_CAMPAIGN,
        CUSTOMERS_ATTRIBUTES,
        CONSENTS,
        IN_APP_MESSAGES,
        PUSH_SELF_CHECK,
        MESSAGE_INBOX,
        MESSAGE_INBOX_READ,
        INAPP_CONTENT_BLOCKS_STATIC,
        INAPP_CONTENT_BLOCKS_PERSONAL
    }

    init {
        this.url = when (endPointName) {
            TRACK_CUSTOMERS -> "/track/v2/projects/$splitterToken/customers"
            TRACK_EVENTS -> "/track/v2/projects/$splitterToken/customers/events"
            TRACK_CAMPAIGN -> "/track/v2/projects/$splitterToken/campaigns/clicks"
            CUSTOMERS_ATTRIBUTES -> "/data/v2/projects/$splitterToken/customers/attributes"
            CONSENTS -> "/data/v2/projects/$splitterToken/consent/categories"
            IN_APP_MESSAGES -> "/webxp/s/$splitterToken/inappmessages?v=1"
            MESSAGE_INBOX -> "/webxp/projects/$splitterToken/appinbox/fetch"
            MESSAGE_INBOX_READ -> "/webxp/projects/$splitterToken/appinbox/markasread"
            PUSH_SELF_CHECK -> "/campaigns/send-self-check-notification?project_id=$splitterToken"
            INAPP_CONTENT_BLOCKS_STATIC -> "/wxstatic/projects/$splitterToken/bundle-android.json?v=2"
            INAPP_CONTENT_BLOCKS_PERSONAL -> "/webxp/s/$splitterToken/inappcontentblocks?v=2"
        }

        this.url = this.url.replace(splitterToken, token)
    }

    override fun toString(): String {
        return url
    }
}
