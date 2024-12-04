package com.exponea.sdk.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.exponea.sdk.R
import com.exponea.sdk.databinding.MessageInboxDetailBinding

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
        val viewBinding = MessageInboxDetailBinding.bind(View.inflate(context, R.layout.message_inbox_detail, this))
        // from message_inbox_detail_push.xml
        this.pushContainer = viewBinding.messageDetailPushMode.root
        this.imageView = viewBinding.messageDetailPushMode.messageDetailImage
        this.receivedTimeView = viewBinding.messageDetailPushMode.messageDetailReceivedTime
        this.titleView = viewBinding.messageDetailPushMode.messageDetailTitle
        this.contentView = viewBinding.messageDetailPushMode.messageDetailContent
        this.actionsContainerView = viewBinding.messageDetailPushMode.messageDetailActionsContainer
        // from message_inbox_detail_html.xml
        this.htmlContainer = viewBinding.messageDetailHtmlMode.root
        this.webView = viewBinding.messageDetailHtmlMode.messageDetailWebview
        // all modes has to be hidden before usage
        this.pushContainer.visibility = GONE
        this.htmlContainer.visibility = GONE
    }
}
