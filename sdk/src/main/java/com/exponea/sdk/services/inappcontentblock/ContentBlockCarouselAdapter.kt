package com.exponea.sdk.services.inappcontentblock

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ensureOnMainThread
import com.exponea.sdk.view.InAppContentBlockPlaceholderView

internal class ContentBlockCarouselAdapter(
    private val placeholderId: String,
    private val onPlaceholderCreated: (InAppContentBlockPlaceholderView) -> Unit
) : RecyclerView.Adapter<ContentBlockCarouselViewHolder>() {

    /**
     * Contains IDs of content blocks in same order as they are loaded by `updateData` method.
     * Order of these IDs are kept also after `removeItem` method.
     * !!! According to Infinite-loop behaviour and as these IDs are representing "visible" items,
     * this List of IDs is 'prefixed' with last ID and 'suffixed' with first ID:
     * If ContentBlocks data = <A, B, C>
     * Then IDs are = <C, A, B, C, A>
     * For Infinite-loop behaviour see ContentBlockCarouselViewController::onPageScrollStateChanged
     */
    private val shownContentBlockIds = mutableListOf<String>()

    /**
     * Contains pure content blocks list as they are loaded by `updateData` method.
     */
    private val contentBlocksData = mutableListOf<InAppContentBlock>()

    fun updateData(newData: List<InAppContentBlock>) {
        val newDataIds = newData.map { it.id }
        val enhancedNewDataIds = multiplyFirstAndLastItems(newDataIds)
        val contentBlockDiffCallback = ContentBlockDiffCallback(shownContentBlockIds, enhancedNewDataIds)
        val diffResult = DiffUtil.calculateDiff(contentBlockDiffCallback, false)
        shownContentBlockIds.clear()
        shownContentBlockIds.addAll(enhancedNewDataIds)
        contentBlocksData.clear()
        contentBlocksData.addAll(newData)
        ensureOnMainThread {
            diffResult.dispatchUpdatesTo(this)
        }
    }

    fun removeItem(contentBlock: InAppContentBlock) {
        updateData(contentBlocksData.filter { it.id != contentBlock.id })
    }

    fun getLoadedData(): List<InAppContentBlock> {
        return contentBlocksData.toList()
    }

    /**
     * Due to Infinity-loop behaviour, we need to 'prefix' list with last ID and 'suffix' with first ID.
     * When list is empty or has only single item -> Infinity-loop behaviour is turned off by unmodified result.
     */
    private fun multiplyFirstAndLastItems(newData: List<String>): List<String> {
        return if (newData.size <= 1) {
            newData
        } else {
            listOf(newData.last()) + newData + newData.first()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentBlockCarouselViewHolder {
        return ContentBlockCarouselViewHolder.newInstance(
            placeholderId,
            onPlaceholderCreated,
            parent.context
        )
    }

    override fun onBindViewHolder(holder: ContentBlockCarouselViewHolder, position: Int) {
        val contentBlockId = shownContentBlockIds[position]
        val contentBlock = contentBlocksData.find { it.id == contentBlockId }
        holder.updateContent(contentBlock)
    }

    override fun getItemCount(): Int = shownContentBlockIds.size

    fun getItem(position: Int): InAppContentBlock? {
        return contentBlocksData.getOrNull(position)
    }

    fun getLoadedDataCount(): Int {
        return contentBlocksData.size
    }
}

internal class ContentBlockCarouselViewHolder(
    private val contentBlockLoader: SingleContentBlockLoader,
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        @JvmStatic
        fun newInstance(
            placeholderId: String,
            onPlaceholderCreated: (InAppContentBlockPlaceholderView) -> Unit,
            context: Context
        ): ContentBlockCarouselViewHolder {
            val contentBlockLoader = SingleContentBlockLoader()
            val placeholderWrapper = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val placeholderView = Exponea.getComponent()?.inAppContentBlockManager?.getPlaceholderView(
                placeholderId,
                contentBlockLoader,
                context,
                InAppContentBlockPlaceholderConfiguration(true)
            )?.apply {
                setLayoutParams(
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                placeholderWrapper.addView(this)
                onPlaceholderCreated(this)
            }
            if (placeholderView == null) {
                Logger.e(this, "InAppCbCarousel: Placeholder View has not been created successfully")
            }
            return ContentBlockCarouselViewHolder(
                contentBlockLoader,
                placeholderWrapper
            )
        }
    }

    fun updateContent(contentBlock: InAppContentBlock?) {
        if (contentBlockLoader.assignedContentBlock?.id == contentBlock?.id) {
            // View holder shows same InAppContentBlock, do nothing
            return
        }
        contentBlockLoader.assignedContentBlock = contentBlock
        val placeholderView = getContentBlockPlaceholderView()
        if (placeholderView != null) {
            placeholderView.refreshContent()
        } else {
            Logger.w(this, "InAppCbCarousel: View was not created properly, unable to update content")
        }
    }

    fun getContentBlockPlaceholderView(): InAppContentBlockPlaceholderView? {
        return (itemView as LinearLayout).getChildAt(0) as? InAppContentBlockPlaceholderView
    }
}

internal class ContentBlockDiffCallback(
    private val oldData: List<String>,
    private val newData: List<String>
) :
    DiffUtil.Callback() {
    override fun getOldListSize() = oldData.size
    override fun getNewListSize() = newData.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldData[oldItemPosition] == newData[newItemPosition]
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldData[oldItemPosition] == newData[newItemPosition]
}
