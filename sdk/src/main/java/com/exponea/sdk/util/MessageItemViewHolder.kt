package com.exponea.sdk.util

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.message_inbox_list_item.view.message_item_container
import kotlinx.android.synthetic.main.message_inbox_list_item.view.message_item_content
import kotlinx.android.synthetic.main.message_inbox_list_item.view.message_item_image
import kotlinx.android.synthetic.main.message_inbox_list_item.view.message_item_read_flag
import kotlinx.android.synthetic.main.message_inbox_list_item.view.message_item_received_time
import kotlinx.android.synthetic.main.message_inbox_list_item.view.message_item_title

/**
 * AppInbox message viewholder
 */
class MessageItemViewHolder(target: View) : ViewHolder(target) {
    val itemContainer = target.message_item_container
    val readFlag = target.message_item_read_flag
    val receivedTime = target.message_item_received_time
    val title = target.message_item_title
    val content = target.message_item_content
    val image = target.message_item_image
}
