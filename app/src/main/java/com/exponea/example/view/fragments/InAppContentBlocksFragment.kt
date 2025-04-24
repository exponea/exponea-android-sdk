package com.exponea.example.view.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.exponea.example.R
import com.exponea.example.databinding.FragmentInappContentBlocksBinding
import com.exponea.example.models.Constants
import com.exponea.example.view.MainActivity
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.Logger
import kotlin.math.ceil

class InAppContentBlocksFragment : BaseFragment() {

    private lateinit var viewBinding: FragmentInappContentBlocksBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentInappContentBlocksBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.let {
            it.subtitle = "InApp Content Blocks"
        }

        // Track visited screen
        trackPage(Constants.ScreenNames.inAppContentBlocksScreen)

        prepareExampleTopCbPlaceholder()
        prepareExampleAndroidCbPlaceholder()
        prepareExampleListCbPlaceholder()
    }

    private val OPEN_CAROUSEL_ACTION_ID = 1

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(
            Menu.NONE,
            OPEN_CAROUSEL_ACTION_ID,
            Menu.NONE,
            "Carousels"
        )
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            OPEN_CAROUSEL_ACTION_ID -> {
                openCarousel()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openCarousel() {
        (activity as? MainActivity)?.openCarousel()
    }

    private fun prepareExampleListCbPlaceholder() {
        viewBinding.contentBlocksList.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.contentBlocksList.layoutParams?.apply {
            height = Resources.getSystem().getDisplayMetrics().heightPixels / 2
        }
        viewBinding.contentBlocksList.layoutParams = viewBinding.contentBlocksList.layoutParams
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
        viewBinding.contentBlocksList.adapter = ProductsAdapter(data)
    }

    private fun prepareExampleAndroidCbPlaceholder() {
        viewBinding.contentBlocksLayout.addView(TextView(requireContext()).apply {
            text = "Placeholder: ph_x_example_Android"
        })
        Exponea.getInAppContentBlocksPlaceholder("ph_x_example_Android", requireContext())?.let {
            viewBinding.contentBlocksLayout.addView(it, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun prepareExampleTopCbPlaceholder() {
        viewBinding.contentBlocksLayout.addView(TextView(requireContext()).apply { text = "Placeholder: example_top" })
        val examplePlaceholderHeightInfoView = TextView(requireContext()).apply { text = "Height: 0px" }
        Exponea.getInAppContentBlocksPlaceholder(
            "example_top",
            requireContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )?.let {
            it.setOnContentReadyListener { contentLoaded ->
                Logger.i(this, "InApp CB has dimens width ${it.width}px height ${it.height}px")
            }
            it.setOnHeightUpdateListener { height ->
                Logger.i(this, "InApp CB has height ${height}px")
                examplePlaceholderHeightInfoView.text = "Height: ${height}px"
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
            viewBinding.contentBlocksLayout.addView(it, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        viewBinding.contentBlocksLayout.addView(examplePlaceholderHeightInfoView)
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
