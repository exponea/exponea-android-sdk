package com.exponea.sdk.manager

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.exponea.sdk.Exponea
import com.exponea.sdk.R.layout
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.MessageItemViewHolder

internal class AppInboxAdapter(
    private val items: MutableList<MessageItem> = mutableListOf<MessageItem>(),
    private val drawableCache: DrawableCache,
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
        drawableCache.showImage(
            contentSource?.imageUrl,
            target.image,
            onImageNotLoaded = {
                it.visibility = View.GONE
            }
        )
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
