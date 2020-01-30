package com.exponea.sdk.view

import android.animation.Animator
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.exponea.sdk.R
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayload.Companion.parseColor
import com.exponea.sdk.models.InAppMessagePayload.Companion.parseFontSize
import com.exponea.sdk.util.setBackgroundColor
import com.google.android.material.behavior.SwipeDismissBehavior

internal class InAppMessageSlideIn : PopupWindow, InAppMessageView {
    companion object {
        const val ANIMATION_DURATION = 250L
        const val DISPLAY_DURATION = 4000L
    }

    private val activity: Activity
    private val payload: InAppMessagePayload
    private val onButtonClick: () -> Unit
    private var onDismiss: (() -> Unit)?
    private val bitmap: Bitmap

    constructor(
        activity: Activity,
        payload: InAppMessagePayload,
        image: Bitmap,
        onButtonClick: () -> Unit,
        onDismiss: () -> Unit
    ) : super(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {
        this.activity = activity
        this.payload = payload
        this.bitmap = image
        this.onButtonClick = onButtonClick
        this.onDismiss = onDismiss

        val inflater = LayoutInflater.from(activity)
        contentView = inflater.inflate(R.layout.in_app_message_slide_in, null, false)

        setupBackground()
        setupImage()
        setupTitleText()
        setupBodyText()
        setupButton()

        setOnDismissListener { this.onDismiss?.invoke() }
    }

    override fun show() {
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        contentView.translationY = -contentView.measuredHeight.toFloat()
        contentView
            .animate()
            .setDuration(ANIMATION_DURATION)
            .translationY(0f)
            .start()

        showAtLocation(activity.window.decorView.rootView, Gravity.TOP, 0, 0)
        setupSwipeToDismiss()
        Handler(Looper.getMainLooper()).postDelayed({ if (isShowing) dismiss() }, DISPLAY_DURATION)
    }

    override fun dismiss() {
        val superDismiss = { super.dismiss() }
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        contentView
            .animate()
            .setDuration(ANIMATION_DURATION)
            .translationY(-contentView.measuredHeight.toFloat())
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator?) { superDismiss() }
                override fun onAnimationCancel(animation: Animator?) { superDismiss() }
                override fun onAnimationStart(animation: Animator?) {}
                override fun onAnimationRepeat(animation: Animator?) {}
            })
            .start()
    }

    private fun setupSwipeToDismiss() {
        val containerView = contentView.findViewById<LinearLayout>(R.id.inAppMessageSlideInContainer)
        val swipeToDismissBehavior = SwipeDismissBehavior<LinearLayout>()
        swipeToDismissBehavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_START_TO_END)
        swipeToDismissBehavior.setListener(object : SwipeDismissBehavior.OnDismissListener {
                override fun onDismiss(view: View) { dismiss() }
                override fun onDragStateChanged(state: Int) {}
            })
        (containerView.layoutParams as CoordinatorLayout.LayoutParams).behavior = swipeToDismissBehavior
    }

    private fun setupBackground() {
        val inAppMessageSlideInContainer = contentView.findViewById<View>(R.id.inAppMessageSlideInContainer)
        inAppMessageSlideInContainer.setBackgroundColor(
            R.drawable.in_app_message_slide_in_background,
            parseColor(payload.backgroundColor, Color.WHITE)
        )
    }

    private fun setupImage() {
        val imageViewImage = contentView.findViewById<ImageView>(R.id.imageViewImage)
        imageViewImage.setImageBitmap(bitmap)
    }

    private fun setupTitleText() {
        val textViewTitle = contentView.findViewById<TextView>(R.id.textViewTitle)
        textViewTitle.text = payload.title
        textViewTitle.setTextColor(parseColor(payload.titleTextColor, Color.BLACK))
        textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, parseFontSize(payload.titleTextSize, 22f))
    }

    private fun setupBodyText() {
        val textViewBody = contentView.findViewById<TextView>(R.id.textViewBody)
        textViewBody.text = payload.bodyText
        textViewBody.setTextColor(parseColor(payload.bodyTextColor, Color.BLACK))
        textViewBody.setTextSize(TypedValue.COMPLEX_UNIT_DIP, parseFontSize(payload.bodyTextSize, 14f))
    }

    private fun setupButton() {
        val buttonAction = contentView.findViewById<Button>(R.id.buttonAction)
        buttonAction.text = payload.buttonText
        buttonAction.setTextColor(parseColor(payload.buttonTextColor, Color.BLACK))
        buttonAction.setBackgroundColor(
            R.drawable.in_app_message_slide_in_button,
            parseColor(payload.buttonBackgroundColor, Color.LTGRAY)
        )
        buttonAction.setOnClickListener {
            onButtonClick()
            onDismiss = null // clear the dismiss listener, we called the button listener
            dismiss()
        }
    }
}
