package com.exponea.example.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
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
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.util.Logger
import kotlin.math.ceil
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

        content_blocks_layout.addView(TextView(requireContext()).apply { text = "Placeholder: example_top" })
        Exponea.getInAppContentBlocksPlaceholder("example_top", requireContext())?.let {
            content_blocks_layout.addView(it, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ))
        }

        content_blocks_layout.addView(TextView(requireContext()).apply { text = "Placeholder: ph_x_example_Android" })
        Exponea.getInAppContentBlocksPlaceholder("ph_x_example_Android", requireContext())?.let {
            content_blocks_layout.addView(it, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ))
        }

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
        Logger.d(this, "ContentBlocks: Creating ViewHolder of type $viewType")
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
        Logger.d(this, "ContentBlocks: Binding ViewHolder of type $viewType")
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
