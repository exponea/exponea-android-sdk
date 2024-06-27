package com.exponea.example.view.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.Logger
import kotlin.math.ceil
import kotlinx.android.synthetic.main.fragment_inapp_content_blocks.content_blocks_carousel_custom
import kotlinx.android.synthetic.main.fragment_inapp_content_blocks.content_blocks_carousel_default
import kotlinx.android.synthetic.main.fragment_inapp_content_blocks.content_blocks_carousel_status
import kotlinx.android.synthetic.main.fragment_inapp_content_blocks.content_blocks_layout
import kotlinx.android.synthetic.main.fragment_inapp_content_blocks.content_blocks_list

class InAppContentBlocksFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_inapp_content_blocks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.subtitle = "InApp Content Blocks"

        // Track visited screen
        trackPage(Constants.ScreenNames.inAppContentBlocksScreen)

        prepareExampleDefaultCarouselCbPlaceholder()
        prepareExampleCustomCarouselCbPlaceholder()
        prepareExampleAndroidCarouselCbPlaceholder()
        prepareExampleTopCbPlaceholder()
        prepareExampleAndroidCbPlaceholder()
        prepareExampleListCbPlaceholder()
    }

    private fun prepareExampleAndroidCarouselCbPlaceholder() {
        // nothing to do, keep it with default configuration
    }

    private fun prepareExampleDefaultCarouselCbPlaceholder() {
        content_blocks_carousel_default?.let {
            val initName = it.getShownContentBlock()?.name
            val initIndex = it.getShownIndex()
            val initCount = it.getShownCount()
            content_blocks_carousel_status.text = "Showing ${initName ?: ""} as ${initIndex + 1} of $initCount"
        }
        content_blocks_carousel_default.behaviourCallback = object : ContentBlockCarouselCallback {
            private var count: Int = 0
            private var index: Int = -1
            private var blockName: String? = null

            override fun onMessageShown(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                index: Int,
                count: Int
            ) {
                this.blockName = contentBlock.name
                this.index = index
                this.count = count
                updateCarouselStatus()
            }

            private fun updateCarouselStatus() {
                content_blocks_carousel_status.text = "Showing ${blockName ?: ""} as ${index + 1} of $count"
            }

            override fun onMessagesChanged(count: Int, messages: List<InAppContentBlock>) {
                if (count == 0) {
                    this.blockName = null
                    this.index = -1
                }
                this.count = count
                updateCarouselStatus()
            }
        }
    }

    private fun prepareExampleListCbPlaceholder() {
        content_blocks_list.layoutManager = LinearLayoutManager(requireContext())
        val data = ArrayList<ProductsViewModel>()
        for (i in 1..1000) {
            val icon = listOf(
                android.R.drawable.ic_dialog_map,
                android.R.drawable.stat_sys_speakerphone,
                android.R.drawable.arrow_up_float,
                android.R.drawable.ic_menu_search
            ).random()
            val desc = listOf(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod",
                "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium",
                "Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit",
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore"
            ).random()
            data.add(ProductsViewModel(
                icon,
                "Product $i",
                desc
            ))
        }
        content_blocks_list.adapter = ProductsAdapter(data)
    }

    private fun prepareExampleCustomCarouselCbPlaceholder() {
        content_blocks_carousel_custom.contentBlockSelector = object : ContentBlockSelector() {
            override fun filterContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                return source.filter { !it.name.lowercase().contains("discarded") }
            }

            override fun sortContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                return super.sortContentBlocks(source).asReversed()
            }
        }
    }

    private fun prepareExampleAndroidCbPlaceholder() {
        content_blocks_layout.addView(TextView(requireContext()).apply { text = "Placeholder: ph_x_example_Android" })
        Exponea.getInAppContentBlocksPlaceholder("ph_x_example_Android", requireContext())?.let {
            content_blocks_layout.addView(it, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun prepareExampleTopCbPlaceholder() {
        content_blocks_layout.addView(TextView(requireContext()).apply { text = "Placeholder: example_top" })
        Exponea.getInAppContentBlocksPlaceholder(
            "example_top",
            requireContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )?.let {
            it.setOnContentReadyListener { contentLoaded ->
                Logger.i(this, "InApp CB has dimens width ${it.width}px height ${it.height}px")
            }
            val origBehaviour = it.behaviourCallback
            it.behaviourCallback = object : InAppContentBlockCallback {
                override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                    origBehaviour.onMessageShown(placeholderId, contentBlock)
                    Logger.i(this, "Content block with HTML: ${contentBlock.htmlContent}")
                    contentBlock.htmlContent?.let { rawHtml ->
                        val normalizationConfig = HtmlNormalizer.HtmlNormalizerConfig(
                            true,
                            false
                        )
                        val normalizedHtml = HtmlNormalizer(requireContext(), rawHtml)
                            .normalize(normalizationConfig)
                            .html
                        Logger.i(this, "Normalized HTML: $normalizedHtml")
                    }
                }
                override fun onNoMessageFound(placeholderId: String) {
                    origBehaviour.onNoMessageFound(placeholderId)
                }
                override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                    if (contentBlock == null) {
                        return
                    }
                    Exponea.trackInAppContentBlockErrorWithoutTrackingConsent(
                        placeholderId, contentBlock, errorMessage
                    )
                }
                override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                    Exponea.trackInAppContentBlockCloseWithoutTrackingConsent(placeholderId, contentBlock)
                }
                override fun onActionClicked(
                    placeholderId: String,
                    contentBlock: InAppContentBlock,
                    action: InAppContentBlockAction
                ) {
                    Exponea.trackInAppContentBlockClickWithoutTrackingConsent(
                        placeholderId, action, contentBlock
                    )
                    try {
                        requireContext().startActivity(
                            Intent(Intent.ACTION_VIEW).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                data = Uri.parse(action.url)
                            }
                        )
                    } catch (e: ActivityNotFoundException) {
                        Logger.e(this, "Unable to perform deeplink", e)
                    }
                }
            }
            content_blocks_layout.addView(it, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }
}

class ProductsAdapter(private val data: List<ProductsViewModel>) : Adapter<ProductsAdapter.ViewHolder>() {

    private val CONTENT_BLOCK_TYPE = 1
    private val PRODUCT_TYPE = 0
    private val CONTENT_BLOCK_FREQUENCY = 5

    override fun getItemCount(): Int {
        return data.size + (data.size / CONTENT_BLOCK_FREQUENCY)
    }

    override fun getItemViewType(position: Int): Int {
        if (position % CONTENT_BLOCK_FREQUENCY == 0) {
            return CONTENT_BLOCK_TYPE
        }
        return PRODUCT_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Logger.v(this, "InAppCbCarousel: Creating ViewHolder of type $viewType")
        return when (viewType) {
            CONTENT_BLOCK_TYPE -> {
                var contentBlocksPlaceholder: View? = Exponea.getInAppContentBlocksPlaceholder(
                    "example_list",
                    parent.context,
                    InAppContentBlockPlaceholderConfiguration(
                        defferedLoad = true
                    )
                )?.apply {
                    // Tips: to fit products list layout, we are able to play with dimensions
                    this.minimumHeight = 250
                }
                if (contentBlocksPlaceholder == null) {
                    // Exponea SDK is not initialized
                    contentBlocksPlaceholder = LayoutInflater.from(parent.context)
                        .inflate(R.layout.product_item_empty, parent, false)
                }
                ViewHolder(contentBlocksPlaceholder!!)
            }
            PRODUCT_TYPE -> ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.product_item, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        Logger.v(this, "InAppCbCarousel: Binding ViewHolder of type $viewType")
        when (viewType) {
            CONTENT_BLOCK_TYPE -> {
                // nothing, InAppContentBlockView will load itself
            }
            PRODUCT_TYPE -> {
                val dataPosition = position - ceil(position.toDouble() / CONTENT_BLOCK_FREQUENCY)
                data.getOrNull(dataPosition.toInt())?.let {
                    holder.image?.setImageResource(it.icon)
                    holder.title?.text = it.title
                    holder.desc?.text = it.desc
                }
            }
            else -> {
                Logger.e(this, "Unknown view type $viewType")
            }
        }
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val image: ImageView? = itemView.findViewById(R.id.product_image)
        val title: TextView? = itemView.findViewById(R.id.product_title)
        val desc: TextView? = itemView.findViewById(R.id.product_desc)
    }
}

data class ProductsViewModel(val icon: Int, val title: String, val desc: String)
