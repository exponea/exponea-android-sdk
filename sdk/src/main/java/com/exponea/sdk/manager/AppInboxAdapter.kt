package com.exponea.sdk.manager

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.exponea.sdk.Exponea
import com.exponea.sdk.databinding.MessageInboxListItemBinding
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.MessageItemViewHolder
import com.exponea.sdk.util.ensureOnMainThread
import com.exponea.sdk.util.logOnException

internal class AppInboxAdapter(
    private val items: MutableList<MessageItem> = mutableListOf(),
    private val onItemClicked: (MessageItem, Int) -> Unit
) : Adapter<MessageItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageItemViewHolder {
        val viewBinding = MessageInboxListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageItemViewHolder(viewBinding)
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
        Exponea.initGate.waitForInitialize {
            runCatching {
                val sdkComponent = Exponea.getComponent()
                if (sdkComponent == null) {
                    Logger.e(this, "AppInbox: SDK is not initialized properly")
                    ensureOnMainThread {
                        target.image.visibility = View.GONE
                    }
                    return@runCatching
                }
                sdkComponent.drawableCache.showImage(
                    contentSource?.imageUrl,
                    target.image,
                    onImageNotLoaded = {
                        ensureOnMainThread {
                            target.image.visibility = View.GONE
                        }
                    }
                )
            }.logOnException()
        }
        target.itemContainer.setOnClickListener {
            trackItemClicked(source)
            onItemClicked.invoke(source, position)
        }
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
