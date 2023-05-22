package com.exponea.sdk.manager

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.exponea.sdk.Exponea
import com.exponea.sdk.R.layout
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.MessageItemViewHolder
import com.exponea.sdk.util.runOnMainThread

internal class AppInboxAdapter(
    private val items: MutableList<MessageItem> = mutableListOf<MessageItem>(),
    private val bitmapCache: InAppMessageBitmapCache,
    private val onItemClicked: (MessageItem, Int) -> Unit
) : Adapter<MessageItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layout.message_inbox_list_item, parent, false)
        return MessageItemViewHolder(view)
    }

    override fun onBindViewHolder(target: MessageItemViewHolder, position: Int) {
        val source = items[position]
        target.readFlag.visibility = if (source.read == true) View.GONE else View.VISIBLE
        val contentSource = source.content
        val receivedMillis: Long = source.receivedTime?.times(1000)?.toLong() ?: System.currentTimeMillis()
        target.receivedTime.text = DateUtils.getRelativeTimeSpanString(
            receivedMillis,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS
        )
        target.title.text = contentSource?.title ?: ""
        target.content.text = contentSource?.message ?: ""
        if (contentSource?.imageUrl.isNullOrBlank()) {
            target.image.visibility = View.GONE
        } else {
            target.image.visibility = View.VISIBLE
            contentSource?.imageUrl?.let { imageUrl ->
                bitmapCache.preload(listOf(imageUrl), { preloaded ->
                    runOnMainThread {
                        if (preloaded) {
                            target.image.setImageBitmap(bitmapCache.get(imageUrl))
                        } else {
                            target.image.visibility = View.GONE
                        }
                    }
                })
            }
        }
        target.itemContainer.setOnClickListener(View.OnClickListener {
            trackItemClicked(source)
            onItemClicked.invoke(source, position)
        })
    }

    private fun trackItemClicked(item: MessageItem) {
        Logger.i(this, "Message item clicked")
        Exponea.trackAppInboxOpened(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun replaceData(newSource: List<MessageItem>) {
        items.clear()
        items.addAll(newSource)
        notifyDataSetChanged()
    }
}
