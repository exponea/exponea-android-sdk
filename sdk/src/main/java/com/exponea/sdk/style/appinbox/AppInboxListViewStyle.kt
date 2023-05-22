package com.exponea.style

import androidx.recyclerview.widget.RecyclerView
import com.exponea.sdk.util.ConversionUtils

data class AppInboxListViewStyle(
    var backgroundColor: String? = null,
    var item: AppInboxListItemStyle? = null
) {
    fun applyTo(target: RecyclerView) {
        ConversionUtils.parseColor(backgroundColor)?.let {
            target.setBackgroundColor(it)
        }
        // note: 'item' style is used elsewhere due to performance reasons
    }
}
