package com.exponea.example.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.util.Logger
import kotlinx.android.synthetic.main.fragment_carousels.content_blocks_carousel_custom
import kotlinx.android.synthetic.main.fragment_carousels.content_blocks_carousel_default
import kotlinx.android.synthetic.main.fragment_carousels.content_blocks_carousel_status
import kotlinx.android.synthetic.main.fragment_inapp_content_blocks.content_blocks_layout

class CarouselsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_carousels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.let {
            it.subtitle = "InApp Carousels"
        }

        // Track visited screen
        trackPage(Constants.ScreenNames.inAppContentBlocksScreen)

        prepareExampleDefaultCarouselCbPlaceholder()
        prepareExampleCustomCarouselCbPlaceholder()
        prepareExampleAndroidCarouselCbPlaceholder()
    }

    private fun prepareExampleAndroidCarouselCbPlaceholder() {
        val androidCarousel = Exponea.getInAppContentBlocksCarousel(requireContext(), "example_carousel_and")
        content_blocks_layout?.addView(androidCarousel)
    }

    private fun prepareExampleDefaultCarouselCbPlaceholder() {
        content_blocks_carousel_default?.let {
            val initName = it.getShownContentBlock()?.name
            val initIndex = it.getShownIndex()
            val initCount = it.getShownCount()
            content_blocks_carousel_status?.text = "Showing ${initName ?: ""} as ${initIndex + 1} of $initCount"
        }
        content_blocks_carousel_default?.behaviourCallback = object : ContentBlockCarouselCallback {

            override val overrideDefaultBehavior = false
            override val trackActions = true

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
                content_blocks_carousel_status?.text = "Showing ${blockName ?: ""} as ${index + 1} of $count"
            }

            override fun onMessagesChanged(count: Int, messages: List<InAppContentBlock>) {
                if (count == 0) {
                    this.blockName = null
                    this.index = -1
                }
                this.count = count
                updateCarouselStatus()
            }

            override fun onNoMessageFound(placeholderId: String) {
                Logger.i(this, "Carousel $placeholderId is empty")
            }

            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                Logger.e(this, "Carousel $placeholderId error: $errorMessage")
            }

            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                Logger.i(this, "Message ${contentBlock.name} has been closed in carousel $placeholderId")
            }

            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                Logger.i(this, "Action ${action.name} has been clicked in carousel $placeholderId")
            }

            override fun onHeightUpdate(placeholderId: String, height: Int) {
                Logger.i(this, "Carousel $placeholderId has new height $height")
            }
        }
    }

    private fun prepareExampleCustomCarouselCbPlaceholder() {
        content_blocks_carousel_custom?.contentBlockSelector = object : ContentBlockSelector() {
            override fun filterContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                return source.filter { !it.name.lowercase().contains("discarded") }
            }

            override fun sortContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                return super.sortContentBlocks(source).asReversed()
            }
        }
    }
}
