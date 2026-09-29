package com.exponea.example.contentblocks

import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.exponea.example.R
import com.exponea.example.model.Constants
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrGrey700
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import kotlin.math.ceil

object ProductListFactory {

    private const val CONTENT_BLOCK_FREQUENCY = 5
    const val LIST_HEIGHT_DP = 400

    fun itemCount(productCount: Int = Constants.CB_PRODUCT_LIST_SIZE): Int {
        return productCount + (productCount / CONTENT_BLOCK_FREQUENCY)
    }

    fun buildRows(productCount: Int = Constants.CB_PRODUCT_LIST_SIZE): List<ExampleListRow> {
        return List(itemCount(productCount)) { position ->
            if (position % CONTENT_BLOCK_FREQUENCY == 0) {
                ExampleListRow.ContentBlock(position)
            } else {
                val productIndex = position - ceil(position.toDouble() / CONTENT_BLOCK_FREQUENCY).toInt()
                ExampleListRow.Product(
                    title = "Product ${productIndex + 1}",
                    description = "Demo description for product ${productIndex + 1}."
                )
            }
        }
    }
}

sealed interface ExampleListRow {
    data class ContentBlock(val position: Int) : ExampleListRow {
        val key: String get() = "content_block_$position"
    }

    data class Product(val title: String, val description: String) : ExampleListRow {
        val key: String get() = "product_$title"
    }
}

@Composable
fun ExampleProductList(
    modifier: Modifier = Modifier,
    productCount: Int = Constants.CB_PRODUCT_LIST_SIZE
) {
    val rows = remember(productCount) { ProductListFactory.buildRows(productCount) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(ProductListFactory.LIST_HEIGHT_DP.dp)
    ) {
        items(rows, key = { row ->
            when (row) {
                is ExampleListRow.ContentBlock -> row.key
                is ExampleListRow.Product -> row.key
            }
        }) { row ->
            when (row) {
                is ExampleListRow.ContentBlock -> {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            Exponea.getInAppContentBlocksPlaceholder(
                                "example_list",
                                ctx,
                                InAppContentBlockPlaceholderConfiguration(defferedLoad = true)
                            )?.apply { minimumHeight = 250 }
                                ?: TextView(ctx).apply {
                                    text = ctx.getString(R.string.sdk_not_initialized)
                                    setPadding(16, 16, 16, 16)
                                }
                        }
                    )
                }
                is ExampleListRow.Product -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = row.title,
                            fontWeight = FontWeight.Bold,
                            color = BrGrey700
                        )
                        Text(
                            text = row.description,
                            color = BrGrey600
                        )
                    }
                }
            }
        }
    }
}
