package com.exponea.example.ui.contentblocks

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exponea.example.R
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.contentblocks.CarouselCallbackFactory
import com.exponea.example.contentblocks.ContentBlockCallbackFactory
import com.exponea.example.contentblocks.ExampleProductList
import com.exponea.example.model.Constants
import com.exponea.example.tracking.TrackPageView
import com.exponea.example.ui.components.ResultPanel
import com.exponea.example.ui.components.SectionCard
import com.exponea.example.ui.components.SectionTitle
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey100
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.view.ContentBlockCarouselView

@Composable
fun ContentBlocksScreen(
    modifier: Modifier = Modifier,
    viewModel: ContentBlocksViewModel = viewModel()
) {
    TrackPageView(Constants.ScreenNames.CONTENT_BLOCKS)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrGrey100)
    ) {
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show carousels",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrBlack
                )
                Switch(
                    checked = state.showCarousels,
                    onCheckedChange = viewModel::onShowCarouselsChanged,
                    modifier = Modifier.automationId(AutomationIds.TOGGLE_CAROUSELS),
                    thumbContent = null
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            if (!state.showCarousels) {
                PlaceholderSection(title = "Placeholder: example_top") {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .automationId(AutomationIds.CB_PLACEHOLDER_EXAMPLE_TOP),
                        factory = { ctx ->
                            Exponea.getInAppContentBlocksPlaceholder(
                                "example_top",
                                ctx,
                                InAppContentBlockPlaceholderConfiguration(defferedLoad = true)
                            )?.apply {
                                setOnHeightUpdateListener { height ->
                                    viewModel.onExampleTopHeightUpdated(height)
                                }
                                val original = behaviourCallback
                                behaviourCallback =
                                    ContentBlockCallbackFactory.createExampleTopCallback(ctx, original)
                            } ?: TextView(ctx).apply { text = ctx.getString(R.string.sdk_not_initialized) }
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    ResultPanel(text = state.exampleTopHeight)
                }
                PlaceholderSection(title = "Placeholder: ph_x_example_Android") {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .automationId(AutomationIds.CB_PLACEHOLDER_EXAMPLE_PLATFORM),
                        factory = { ctx ->
                            Exponea.getInAppContentBlocksPlaceholder("ph_x_example_Android", ctx)
                                ?: TextView(ctx).apply { text = ctx.getString(R.string.sdk_not_initialized) }
                        }
                    )
                }
                PlaceholderSection(title = "Inline placeholder: example_list (every 5th row)") {
                    ExampleProductList(
                        modifier = Modifier.automationId(AutomationIds.CB_PLACEHOLDER_EXAMPLE_LIST)
                    )
                }
            } else {
                SectionCard {
                    SectionTitle("Default carousel: example_carousel")
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .automationId(AutomationIds.CB_CAROUSEL_DEFAULT),
                        factory = { ctx ->
                            ContentBlockCarouselView(ctx, "example_carousel").apply {
                                behaviourCallback = CarouselCallbackFactory.createDefault { status ->
                                    viewModel.onCarouselStatusUpdated(status)
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    ResultPanel(
                        text = state.defaultCarouselStatus,
                        automationId = AutomationIds.CAROUSEL_STATUS
                    )
                }
                SectionCard {
                    SectionTitle("Filtered carousel: example_carousel")
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .automationId(AutomationIds.CB_CAROUSEL_FILTERED),
                        factory = { ctx ->
                            ContentBlockCarouselView(
                                context = ctx,
                                placeholderId = "example_carousel",
                                maxMessagesCount = 5,
                                scrollDelay = 10
                            ).apply {
                                contentBlockSelector = object : ContentBlockSelector() {
                                    override fun filterContentBlocks(
                                        source: List<InAppContentBlock>
                                    ): List<InAppContentBlock> {
                                        return source.filter { !it.name.lowercase().contains("discarded") }
                                    }

                                    override fun sortContentBlocks(
                                        source: List<InAppContentBlock>
                                    ): List<InAppContentBlock> {
                                        return super.sortContentBlocks(source).asReversed()
                                    }
                                }
                            }
                        }
                    )
                }
                SectionCard {
                    SectionTitle("Android carousel: example_carousel_and")
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .automationId(AutomationIds.CB_CAROUSEL_PLATFORM),
                        factory = { ctx ->
                            Exponea.getInAppContentBlocksCarousel(ctx, "example_carousel_and")
                                ?: TextView(ctx).apply { text = ctx.getString(R.string.sdk_not_initialized) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderSection(
    title: String,
    content: @Composable () -> Unit
) {
    SectionCard {
        SectionTitle(title)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
