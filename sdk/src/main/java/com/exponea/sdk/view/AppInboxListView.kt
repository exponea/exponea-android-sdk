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
import kotlinx.android.synthetic.main.message_inbox_list.view.message_inbox_empty_status_message
import kotlinx.android.synthetic.main.message_inbox_list.view.message_inbox_empty_status_title
import kotlinx.android.synthetic.main.message_inbox_list.view.message_inbox_error_status_message
import kotlinx.android.synthetic.main.message_inbox_list.view.message_inbox_error_status_title
import kotlinx.android.synthetic.main.message_inbox_list.view.message_inbox_list
import kotlinx.android.synthetic.main.message_inbox_list.view.message_inbox_status_container
import kotlinx.android.synthetic.main.message_inbox_list.view.message_inbox_status_progress

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
        View.inflate(context, R.layout.message_inbox_list, this)
        this.statusContainterView = this.message_inbox_status_container
        this.statusProgressView = this.message_inbox_status_progress
        this.statusEmptyTitleView = this.message_inbox_empty_status_title
        this.statusEmptyMessageView = this.message_inbox_empty_status_message
        this.statusErrorTitleView = this.message_inbox_error_status_title
        this.statusErrorMessageView = this.message_inbox_error_status_message
        this.listView = this.message_inbox_list
    }
}
