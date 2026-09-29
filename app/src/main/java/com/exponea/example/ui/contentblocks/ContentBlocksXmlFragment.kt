package com.exponea.example.ui.contentblocks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.exponea.example.R
import com.exponea.example.databinding.FragmentContentBlocksBinding
import com.exponea.example.model.Constants
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.view.ContentBlockCarouselView
import kotlin.math.ceil

class ContentBlocksXmlFragment : Fragment() {

    private var _binding: FragmentContentBlocksBinding? = null
    private val binding get() = _binding!!

    private var carouselsBuilt = false
    private var placeholdersBuilt = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentBlocksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackPageView()

        binding.cbSwitchCarousels.setOnCheckedChangeListener { _, isChecked ->
            renderMode(isChecked)
        }
        renderMode(binding.cbSwitchCarousels.isChecked)
    }

    private fun renderMode(showCarousels: Boolean) {
        if (showCarousels) {
            binding.cbPlaceholdersGroup.visibility = View.GONE
            binding.cbCarouselsGroup.visibility = View.VISIBLE
            if (!carouselsBuilt) {
                buildCarousels()
                carouselsBuilt = true
            }
        } else {
            binding.cbCarouselsGroup.visibility = View.GONE
            binding.cbPlaceholdersGroup.visibility = View.VISIBLE
            if (!placeholdersBuilt) {
                buildPlaceholders()
                placeholdersBuilt = true
            }
        }
    }

    private fun buildPlaceholders() {
        addPlaceholderInto(
            container = binding.cbPlaceholderExampleTop,
            placeholderId = "example_top",
            configuration = InAppContentBlockPlaceholderConfiguration(defferedLoad = true)
        )
        addPlaceholderInto(
            container = binding.cbPlaceholderExampleAndroid,
            placeholderId = "ph_x_example_Android",
            configuration = null
        )

        binding.cbProductsList.layoutManager = LinearLayoutManager(requireContext())
        val products = (1..Constants.CB_PRODUCT_LIST_SIZE).map { idx ->
            ProductItem(
                icon = listOf(
                    R.drawable.ic_placeholder_image,
                    R.drawable.ic_tracking,
                    R.drawable.ic_fetching,
                    R.drawable.ic_content_blocks
                ).random(),
                title = "Product $idx",
                description = "Demo description for product $idx."
            )
        }
        binding.cbProductsList.adapter = ProductsAdapter(products)
    }

    private fun addPlaceholderInto(
        container: FrameLayout,
        placeholderId: String,
        configuration: InAppContentBlockPlaceholderConfiguration?
    ) {
        if (!Exponea.isInitialized) return
        val placeholder = if (configuration != null) {
            Exponea.getInAppContentBlocksPlaceholder(placeholderId, requireContext(), configuration)
        } else {
            Exponea.getInAppContentBlocksPlaceholder(placeholderId, requireContext())
        }
        placeholder?.let {
            container.removeAllViews()
            container.addView(
                it,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun buildCarousels() {
        if (!Exponea.isInitialized) return
        // Default carousel: example_carousel
        binding.cbCarouselDefault.behaviourCallback = carouselStatusCallback()

        // Filtered carousel: example_carousel
        addCarouselInto(
            container = binding.cbCarouselCustom,
            view = ContentBlockCarouselView(
                requireContext(),
                "example_carousel",
                maxMessagesCount = 5,
                scrollDelay = 10
            ).apply {
                contentBlockSelector = object : ContentBlockSelector() {
                    override fun filterContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                        return source.filter { !it.name.lowercase().contains("discarded") }
                    }

                    override fun sortContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                        return super.sortContentBlocks(source).asReversed()
                    }
                }
            }
        )

        // Android carousel: example_carousel_and
        Exponea.getInAppContentBlocksCarousel(requireContext(), "example_carousel_and")?.let {
            addCarouselInto(binding.cbCarouselAndroid, it)
        }
    }

    private fun addCarouselInto(container: FrameLayout, view: View) {
        container.removeAllViews()
        container.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun carouselStatusCallback() = object : ContentBlockCarouselCallback {
        override val overrideDefaultBehavior = false
        override val trackActions = true

        private var count = 0
        private var index = -1
        private var name: String? = null

        override fun onMessageShown(
            placeholderId: String,
            contentBlock: InAppContentBlock,
            index: Int,
            count: Int
        ) {
            name = contentBlock.name
            this.index = index
            this.count = count
            updateStatus()
        }

        override fun onMessagesChanged(count: Int, messages: List<InAppContentBlock>) {
            if (count == 0) {
                name = null
                index = -1
            }
            this.count = count
            updateStatus()
        }

        override fun onNoMessageFound(placeholderId: String) = Unit
        override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) = Unit
        override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) = Unit
        override fun onActionClicked(
            placeholderId: String,
            contentBlock: InAppContentBlock,
            action: InAppContentBlockAction
        ) = Unit
        override fun onHeightUpdate(placeholderId: String, height: Int) = Unit

        private fun updateStatus() {
            _binding?.cbCarouselStatus?.text = if (count == 0) {
                "Carousel is empty"
            } else {
                "Showing ${name ?: ""} as ${index + 1} of $count"
            }
        }
    }

    private fun trackPageView() {
        if (!Exponea.isInitialized) return
        Exponea.trackEvent(
            eventType = "page_view",
            properties = PropertiesList(hashMapOf("name" to Constants.ScreenNames.CONTENT_BLOCKS_XML))
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class ProductItem(val icon: Int, val title: String, val description: String)

    private class ProductsAdapter(private val data: List<ProductItem>) :
        RecyclerView.Adapter<ProductsAdapter.Holder>() {

        private val productType = 0
        private val contentBlockType = 1
        private val contentBlockEvery = 5

        override fun getItemCount(): Int = data.size + data.size / contentBlockEvery

        override fun getItemViewType(position: Int): Int =
            if (position % contentBlockEvery == 0) contentBlockType else productType

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return when (viewType) {
                contentBlockType -> {
                    val cb = Exponea.getInAppContentBlocksPlaceholder(
                        "example_list",
                        parent.context,
                        InAppContentBlockPlaceholderConfiguration(defferedLoad = true)
                    )?.apply { minimumHeight = 250 }
                    val view = cb
                        ?: LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_product_empty, parent, false)
                    Holder(view, isContentBlock = true)
                }
                else -> Holder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false),
                    isContentBlock = false
                )
            }
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            if (holder.isContentBlock) return
            val dataPos = (position - ceil(position.toDouble() / contentBlockEvery)).toInt()
            data.getOrNull(dataPos)?.let {
                holder.image?.setImageResource(it.icon)
                holder.title?.text = it.title
                holder.desc?.text = it.description
            }
        }

        class Holder(itemView: View, val isContentBlock: Boolean) : RecyclerView.ViewHolder(itemView) {
            val image: ImageView? = itemView.findViewById(R.id.product_image)
            val title: TextView? = itemView.findViewById(R.id.product_title)
            val desc: TextView? = itemView.findViewById(R.id.product_desc)
        }
    }
}
