package com.exponea.style

import com.exponea.sdk.view.AppInboxListView

data class ListScreenStyle(
    var emptyTitle: TextViewStyle? = null,
    var emptyMessage: TextViewStyle? = null,
    var errorTitle: TextViewStyle? = null,
    var errorMessage: TextViewStyle? = null,
    var progress: ProgressBarStyle? = null,
    var list: AppInboxListViewStyle? = null
) {
    fun applyTo(target: AppInboxListView) {
        emptyTitle?.applyTo(target.statusEmptyTitleView)
        emptyMessage?.applyTo(target.statusEmptyMessageView)
        errorTitle?.applyTo(target.statusErrorTitleView)
        errorMessage?.applyTo(target.statusErrorMessageView)
        progress?.applyTo(target.statusProgressView)
        list?.applyTo(target.listView)
    }
}
