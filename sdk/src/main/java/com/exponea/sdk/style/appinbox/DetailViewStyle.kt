package com.exponea.style

import android.view.View
import android.view.ViewGroup.OnHierarchyChangeListener
import android.widget.Button
import android.widget.ImageView
import com.exponea.sdk.view.AppInboxDetailView

data class DetailViewStyle(
    var title: TextViewStyle? = null,
    var content: TextViewStyle? = null,
    var receivedTime: TextViewStyle? = null,
    var image: ImageViewStyle? = null,
    var button: ButtonStyle? = null
) {
    fun applyTo(view: AppInboxDetailView) {
        title?.applyTo(view.titleView)
        content?.applyTo(view.contentView)
        receivedTime?.applyTo(view.receivedTimeView)
        image?.applyTo(view.imageView as ImageView)
        button?.let { buttonStyle ->
            for (i in 0 until view.actionsContainerView.childCount) {
                (view.actionsContainerView.getChildAt(i) as? Button)?.let { button ->
                    buttonStyle.applyTo(button)
                }
            }
            view.actionsContainerView.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    if (child == null) {
                        return
                    }
                    if (child is Button) {
                        buttonStyle.applyTo(child)
                    }
                }
                override fun onChildViewRemoved(p0: View?, p1: View?) { }
            })
        }
    }
}
