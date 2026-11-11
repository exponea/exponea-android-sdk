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
     * Contains IDs (with rendered content) of content blocks in same order as they are loaded by `updateData` method.
     * Order of these IDs are kept also after `removeItem` method.
     * !!! According to Infinite-loop behaviour and as these IDs are representing "visible" items,
     * this List of IDs is 'prefixed' with last ID and 'suffixed' with first ID:
     * If ContentBlocks data = <A, B, C>
     * Then IDs are = <C, A, B, C, A>
     * For Infinite-loop behaviour see ContentBlockCarouselViewController::onPageScrollStateChanged
     */
    private val shownContentBlockIds = mutableListOf<ContentBlockItemKey>()

    /**
     * Contains pure content blocks list as they are loaded by `updateData` method.
     */
    private val contentBlocksData = mutableListOf<InAppContentBlock>()
    private val contentBlocksById = mutableMapOf<String, InAppContentBlock>()
    /**
     * Data and RecyclerView notification has to be updated together on main thread,
     * otherwise RecyclerView may detect inconsistency between item count and its state.
     */
    fun updateData(
        newData: List<InAppContentBlock>,
        newDataKeys: List<ContentBlockItemKey> = newData.map { ContentBlockItemKey.of(it) }
    ) = ensureOnMainThread {
        val enhancedNewDataKeys = multiplyFirstAndLastItems(newDataKeys)
        val contentBlockDiffCallback = ContentBlockDiffCallback(shownContentBlockIds, enhancedNewDataKeys)
        val diffResult = DiffUtil.calculateDiff(contentBlockDiffCallback, false)
        shownContentBlockIds.clear()
        shownContentBlockIds.addAll(enhancedNewDataKeys)
        contentBlocksData.clear()
        contentBlocksData.addAll(newData)
        contentBlocksById.clear()
        contentBlocksData.forEach { contentBlocksById[it.id] = it }
        diffResult.dispatchUpdatesTo(this)
    }

    fun removeItem(contentBlock: InAppContentBlock) = ensureOnMainThread {
        val keysById = shownContentBlockIds.associateBy { it.id }
        val remainingData = contentBlocksData.filter { it.id != contentBlock.id }
        // reuse already computed keys, content of remaining items has not changed
        updateData(remainingData, remainingData.map { keysById[it.id] ?: ContentBlockItemKey.of(it) })
    }

    fun getLoadedData(): List<InAppContentBlock> {
        return contentBlocksData.toList()
    }

    /**
     * Due to Infinity-loop behaviour, we need to 'prefix' list with last ID and 'suffix' with first ID.
     * When list is empty or has only single item -> Infinity-loop behaviour is turned off by unmodified result.
     */
    private fun multiplyFirstAndLastItems(newData: List<ContentBlockItemKey>): List<ContentBlockItemKey> {
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
        val contentBlockId = shownContentBlockIds[position].id
        val contentBlock = contentBlocksById[contentBlockId]
        holder.updateContent(contentBlock)
    }

    override fun onViewAttachedToWindow(holder: ContentBlockCarouselViewHolder) {
        super.onViewAttachedToWindow(holder)
        val view = holder.getContentBlockPlaceholderView()
        view?.htmlContainer?.onResume()
        view?.refreshContent()
    }

    override fun onViewDetachedFromWindow(holder: ContentBlockCarouselViewHolder) {
        val view = holder.getContentBlockPlaceholderView()
        view?.htmlContainer?.onPause()
        // Clear rendered state before holder reuse to avoid stale content flashes after rebind.
        view?.resetContent()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: ContentBlockCarouselViewHolder) {
        // Recycled holder must not keep previous assignment, it could be rendered for another position.
        holder.clearContent()
        super.onViewRecycled(holder)
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
        val currentContentBlock = contentBlockLoader.assignedContentBlock
        if (currentContentBlock?.id == contentBlock?.id &&
            currentContentBlock?.htmlContent == contentBlock?.htmlContent) {
            // View holder shows same InAppContentBlock with same content, do nothing
            return
        }
        contentBlockLoader.assignedContentBlock = contentBlock
        contentBlock?.let {
            InAppContentBlockTiming.log(
                contentBlockId = it.id,
                source = InAppContentBlockTiming.RequestSource.CAROUSEL_ITEM_BOUND,
                contentBlockName = it.name
            )
        }
        val placeholderView = getContentBlockPlaceholderView()
        // Only load if already attached; if not, onViewAttachedToWindow will trigger the load
        // when the page actually becomes visible, preventing tile allocation for off-screen WebViews.
        if (placeholderView != null && placeholderView.isAttachedToWindow) {
            placeholderView.refreshContent()
        } else {
            Logger.w(this, "InAppCbCarousel: View was not created properly, unable to update content")
        }
    }

    fun clearContent() {
        contentBlockLoader.assignedContentBlock = null
        getContentBlockPlaceholderView()?.let {
            // invalidates render of previous block that may be still posted to main thread
            it.controller.cleanUp()
            it.resetContent()
        }
    }

    fun getContentBlockPlaceholderView(): InAppContentBlockPlaceholderView? {
        return (itemView as LinearLayout).getChildAt(0) as? InAppContentBlockPlaceholderView
    }
}

/**
 * Identifies rendered carousel item. Personalized content of same content block may change by reload,
 * so fingerprint of `htmlContent` is part of the key to rebind such item.
 * Fingerprint (length + hash) is used instead of HTML itself to keep DiffUtil comparison cheap on main thread;
 * prefer to create keys on background thread as hash computation is linear to HTML size.
 */
internal data class ContentBlockItemKey(
    val id: String,
    val htmlLength: Int,
    val htmlHash: Int
) {
    companion object {
        fun of(contentBlock: InAppContentBlock): ContentBlockItemKey {
            val html = contentBlock.htmlContent
            return ContentBlockItemKey(contentBlock.id, html?.length ?: -1, html?.hashCode() ?: 0)
        }
    }
}

internal class ContentBlockDiffCallback(
    private val oldData: List<ContentBlockItemKey>,
    private val newData: List<ContentBlockItemKey>
) :
    DiffUtil.Callback() {
    override fun getOldListSize() = oldData.size
    override fun getNewListSize() = newData.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldData[oldItemPosition].id == newData[newItemPosition].id
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldData[oldItemPosition] == newData[newItemPosition]

    /**
     * Non-null payload lets item animator rebind the same view holder for content change,
     * instead of creating another holder (with new WebView) for cross-fade animation.
     */
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any = CONTENT_CHANGED_PAYLOAD

    companion object {
        const val CONTENT_CHANGED_PAYLOAD = "content_changed"
    }
}
