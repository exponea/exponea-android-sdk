package com.exponea.sdk.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.exponea.sdk.R
import kotlinx.android.synthetic.main.message_inbox_detail.view.message_detail_html_mode
import kotlinx.android.synthetic.main.message_inbox_detail.view.message_detail_push_mode
import kotlinx.android.synthetic.main.message_inbox_detail_html.view.message_detail_webview
import kotlinx.android.synthetic.main.message_inbox_detail_push.view.message_detail_actions_container
import kotlinx.android.synthetic.main.message_inbox_detail_push.view.message_detail_content
import kotlinx.android.synthetic.main.message_inbox_detail_push.view.message_detail_image
import kotlinx.android.synthetic.main.message_inbox_detail_push.view.message_detail_received_time
import kotlinx.android.synthetic.main.message_inbox_detail_push.view.message_detail_title

class AppInboxDetailView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    RelativeLayout(context, attrs, defStyleAttr) {

    public lateinit var pushContainer: RelativeLayout
    public lateinit var actionsContainerView: LinearLayout
    public lateinit var contentView: TextView
    public lateinit var titleView: TextView
    public lateinit var receivedTimeView: TextView
    public lateinit var imageView: ImageView
    public lateinit var htmlContainer: RelativeLayout
    public lateinit var webView: ExponeaWebView

    init {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.message_inbox_detail, this)
        // from message_inbox_detail_push.xml
        this.pushContainer = this.message_detail_push_mode as RelativeLayout
        this.imageView = this.message_detail_image
        this.receivedTimeView = this.message_detail_received_time
        this.titleView = this.message_detail_title
        this.contentView = this.message_detail_content
        this.actionsContainerView = this.message_detail_actions_container
        // from message_inbox_detail_html.xml
        this.htmlContainer = this.message_detail_html_mode as RelativeLayout
        this.webView = this.message_detail_webview
        // all modes has to be hidden before usage
        this.pushContainer.visibility = GONE
        this.htmlContainer.visibility = GONE
    }
}
