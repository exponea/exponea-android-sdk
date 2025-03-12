package com.exponea.sdk.view.layout

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.exponea.sdk.style.ButtonAlignment
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Puts children in a row but moves child to next row if it doesn't fit.
 */
internal class RowFlexLayout : LinearLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initDefaults()
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, sAttrs: Int, sRes: Int) : super(context, attrs, sAttrs, sRes) {
        initDefaults()
    }

    internal val realChildren = CopyOnWriteArrayList<View>()

    private fun initDefaults() {
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        regroupChildren(realChildren.toList(), measuredWidth)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun regroupChildren(currentChildren: List<View>, containerWidth: Int) {
        removeCreatedViewsImmediately()
        var activeRow: LinearLayout? = null
        var activeRowWidth = 0
        currentChildren.forEach { child ->
            (child.parent as? ViewGroup)?.removeView(child)
            if (child.layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                // flex - stretch child to full width of container
                addViewDirectly(child)
                activeRow = null
                activeRowWidth = 0
                return@forEach
            }
            // flex - append child until width of container
            var flexRow: LinearLayout = activeRow ?: createRowView().apply {
                this@RowFlexLayout.addViewDirectly(this)
                activeRow = this
                activeRowWidth = 0
            }
            child.measure(
                MeasureSpec.makeMeasureSpec(containerWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            val childFullWidth = child.marginStart + child.measuredWidth + child.marginEnd
            if (flexRow.childCount == 0) {
                // adds child directly also for case that width not fits
                flexRow.addView(child)
                activeRowWidth = childFullWidth
                return@forEach
            }
            val childWidthForAdding = child.marginStart + child.measuredWidth
            if (activeRowWidth + childWidthForAdding <= containerWidth) {
                // child fits, add it to row
                flexRow.addView(child)
                activeRowWidth += childFullWidth
                return@forEach
            }
            // child needs new row
            flexRow = createRowView().apply {
                this@RowFlexLayout.addViewDirectly(this)
                activeRow = this
                activeRowWidth = 0
            }
            flexRow.addView(child)
            activeRowWidth = childFullWidth
        }
    }

    private fun removeCreatedViewsImmediately() {
        realChildren.forEach { each ->
            (each.parent as? LinearLayout)?.let {
                val lt = it.layoutTransition
                it.layoutTransition = null
                it.removeView(each)
                it.layoutTransition = lt
            }
        }
        val lt = layoutTransition
        layoutTransition = null
        removeAllViews()
        removeAllViewsInLayout()
        layoutTransition = lt
    }

    private fun createRowView(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = HORIZONTAL
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        child?.let {
            if (index < 0) {
                realChildren.add(it)
            } else {
                realChildren.add(index, it)
            }
        }
    }

    private fun addViewDirectly(child: View) {
        super.addView(child, -1, child.layoutParams)
    }

    fun applyStyle(buttonsAlignment: ButtonAlignment) {
        gravity = when (buttonsAlignment) {
            ButtonAlignment.LEFT -> Gravity.LEFT
            ButtonAlignment.CENTER -> Gravity.CENTER
            ButtonAlignment.RIGHT -> Gravity.RIGHT
        }
    }
}
