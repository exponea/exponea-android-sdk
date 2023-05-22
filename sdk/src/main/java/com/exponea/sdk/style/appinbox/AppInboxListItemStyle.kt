package com.exponea.style

import com.exponea.sdk.util.ConversionUtils
import com.exponea.sdk.util.MessageItemViewHolder

data class AppInboxListItemStyle(
    var backgroundColor: String? = null,
    var readFlag: ImageViewStyle? = null,
    var receivedTime: TextViewStyle? = null,
    var title: TextViewStyle? = null,
    var content: TextViewStyle? = null,
    var image: ImageViewStyle? = null
) {
    fun applyTo(target: MessageItemViewHolder) {
        ConversionUtils.parseColor(backgroundColor)?.let {
            target.itemContainer?.setBackgroundColor(it)
        }
        nonNull(readFlag, target.readFlag) { style, view ->
            style.applyTo(view)
        }
        nonNull(receivedTime, target.receivedTime) { style, view ->
            style.applyTo(view)
        }
        nonNull(title, target.title) { style, view ->
            style.applyTo(view)
        }
        nonNull(content, target.content) { style, view ->
            style.applyTo(view)
        }
        nonNull(image, target.image) { style, view ->
            style.applyTo(view)
        }
    }
    private inline fun <T1 : Any, T2 : Any, R : Any> nonNull(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
        return if (p1 != null && p2 != null) block(p1, p2) else null
    }
}
