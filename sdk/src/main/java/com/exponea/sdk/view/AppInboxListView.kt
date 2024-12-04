package com.exponea.sdk.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.exponea.sdk.R
import com.exponea.sdk.databinding.MessageInboxListBinding

class AppInboxListView@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    public lateinit var listView: RecyclerView
    public lateinit var statusEmptyMessageView: TextView
    public lateinit var statusEmptyTitleView: TextView
    public lateinit var statusErrorMessageView: TextView
    public lateinit var statusErrorTitleView: TextView
    public lateinit var statusProgressView: ProgressBar
    public lateinit var statusContainterView: LinearLayout

    init {
        init()
    }

    private fun init() {
        val viewBinding = MessageInboxListBinding.bind(View.inflate(context, R.layout.message_inbox_list, this))
        this.statusContainterView = viewBinding.messageInboxStatusContainer
        this.statusProgressView = viewBinding.messageInboxStatusProgress
        this.statusEmptyTitleView = viewBinding.messageInboxEmptyStatusTitle
        this.statusEmptyMessageView = viewBinding.messageInboxEmptyStatusMessage
        this.statusErrorTitleView = viewBinding.messageInboxErrorStatusTitle
        this.statusErrorMessageView = viewBinding.messageInboxErrorStatusMessage
        this.listView = viewBinding.messageInboxList
    }
}
