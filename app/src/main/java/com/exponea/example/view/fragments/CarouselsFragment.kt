package com.exponea.example.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.databinding.FragmentCarouselsBinding
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.util.Logger

class CarouselsFragment : BaseFragment() {

    private lateinit var viewBinding: FragmentCarouselsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentCarouselsBinding.inflate(inflater, container, false)
        return viewBinding.root
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
        Exponea.getInAppContentBlocksCarousel(requireContext(), "example_carousel_and")?.let {
            viewBinding.contentBlocksLayout.addView(it)
        }
    }

    private fun prepareExampleDefaultCarouselCbPlaceholder() {
        viewBinding.contentBlocksCarouselDefault.let {
            val initName = it.getShownContentBlock()?.name
            val initIndex = it.getShownIndex()
            val initCount = it.getShownCount()
            viewBinding.contentBlocksCarouselStatus.text = """
                Carousel is emptyShowing ${initName ?: ""} as ${initIndex + 1} of $initCount
                """.trimIndent()
        }
        viewBinding.contentBlocksCarouselDefault.behaviourCallback = object : ContentBlockCarouselCallback {

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
                viewBinding.contentBlocksCarouselStatus.text = "Showing ${blockName ?: ""} as ${index + 1} of $count"
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
        viewBinding.contentBlocksCarouselCustom.contentBlockSelector = object : ContentBlockSelector() {
            override fun filterContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                return source.filter { !it.name.lowercase().contains("discarded") }
            }

            override fun sortContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                return super.sortContentBlocks(source).asReversed()
            }
        }
    }
}
