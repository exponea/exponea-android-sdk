package com.exponea.sdk.view

import android.animation.Animator
import android.app.Activity
import android.graphics.Color
import android.text.TextUtils
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
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayload.Companion.parseColor
import com.exponea.sdk.models.InAppMessagePayload.Companion.parseFontSize
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.MessagePosition
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.setBackgroundColor
import com.google.android.material.behavior.SwipeDismissBehavior

internal class InAppMessageSlideIn : PopupWindow, InAppMessageView {
    companion object {
        const val ANIMATION_DURATION = 250L
    }

    private val activity: Activity
    private val payload: InAppMessagePayload
    private val onButtonClick: (InAppMessagePayloadButton) -> Unit
    private var onDismiss: ((Boolean) -> Unit)?
    private var onError: (String) -> Unit
    private val imageCache: DrawableCache
    private var userInteraction = false

    override val isPresented: Boolean
        get() = isShowing

    constructor(
        activity: Activity,
        payload: InAppMessagePayload,
        image: DrawableCache,
        onButtonClick: (InAppMessagePayloadButton) -> Unit,
        onDismiss: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) : super(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {
        this.activity = activity
        this.payload = payload
        this.imageCache = image
        this.onButtonClick = onButtonClick
        this.onDismiss = onDismiss
        this.onError = onError

        val inflater = LayoutInflater.from(activity)
        contentView = inflater.inflate(R.layout.in_app_message_slide_in, null, false)

        setupBackground()
        setupImage()
        setupTitleText()
        setupBodyText()
        setupButtons()

        setOnDismissListener {
            if (!userInteraction) {
                this.onDismiss?.invoke(false)
            }
        }
    }

    override fun show() {
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        contentView.translationY =
            (if (payload.messagePosition == MessagePosition.BOTTOM) 1 else -1) * contentView.measuredHeight.toFloat()
        contentView
            .animate()
            .setDuration(ANIMATION_DURATION)
            .translationY(0f)
            .start()

        showAtLocation(
            activity.window.decorView.rootView,
            if (payload.messagePosition == MessagePosition.BOTTOM) Gravity.BOTTOM else Gravity.TOP,
            0,
            0
        )
        setupSwipeToDismiss()
    }

    override fun dismiss() {
        val superDismiss = {
            try {
                super.dismiss()
            } catch (e: Exception) {
                Logger.e(this, "InAppMessageSlideIn dismiss failed")
            }
        }
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        contentView
            .animate()
            .setDuration(ANIMATION_DURATION)
            .translationY(
                (if (payload.messagePosition == MessagePosition.BOTTOM) 1 else -1) *
                    contentView.measuredHeight.toFloat()
            )
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) { superDismiss() }
                override fun onAnimationCancel(animation: Animator) { superDismiss() }
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            .start()
    }

    private fun setupSwipeToDismiss() {
        val containerView = contentView.findViewById<LinearLayout>(R.id.inAppMessageSlideInContainer)
        val swipeToDismissBehavior = SwipeDismissBehavior<LinearLayout>()
        swipeToDismissBehavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_START_TO_END)
        swipeToDismissBehavior.setListener(object : SwipeDismissBehavior.OnDismissListener {
                override fun onDismiss(view: View) {
                    onCloseManually()
                }
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
        imageCache.showImage(
            payload.imageUrl,
            imageViewImage,
            onImageNotLoaded = {
                onError("Image '${payload.imageUrl}' not loaded successfully")
                onDismiss = null // clear the dismiss listener, we called the button listener
                dismiss()
            }
        )
    }

    private fun setupTitleText() {
        val textViewTitle = contentView.findViewById<TextView>(R.id.textViewTitle)
        if (payload.title.isNullOrEmpty()) {
            textViewTitle.visibility = View.GONE
            return
        }
        textViewTitle.text = payload.title
        textViewTitle.setTextColor(parseColor(payload.titleTextColor, Color.BLACK))
        textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, parseFontSize(payload.titleTextSize, 22f))
    }

    private fun setupBodyText() {
        val textViewBody = contentView.findViewById<TextView>(R.id.textViewBody)
        if (payload.bodyText.isNullOrEmpty()) {
            textViewBody.visibility = View.GONE
            return
        }
        textViewBody.text = payload.bodyText
        textViewBody.setTextColor(parseColor(payload.bodyTextColor, Color.BLACK))
        textViewBody.setTextSize(TypedValue.COMPLEX_UNIT_DIP, parseFontSize(payload.bodyTextSize, 14f))
    }

    private fun setupButtons() {
        val buttonsCount = if (payload.buttons != null) payload.buttons.count() else 0
        val button1Payload = if (payload.buttons != null && payload.buttons.isNotEmpty()) payload.buttons[0] else null
        val button2Payload = if (payload.buttons != null && payload.buttons.count() > 1) payload.buttons[1] else null
        setupButton(contentView.findViewById<Button>(R.id.buttonAction1), button1Payload, buttonsCount)
        setupButton(contentView.findViewById<Button>(R.id.buttonAction2), button2Payload, buttonsCount)
    }

    private fun setupButton(buttonAction: Button, buttonPayload: InAppMessagePayloadButton?, buttonsCount: Int) {
        if (buttonPayload == null) {
            buttonAction.visibility = View.GONE
            return
        }
        if (buttonsCount == 2) {
            buttonAction.maxWidth = activity.resources
                    .getDimensionPixelSize(R.dimen.exponea_sdk_in_app_message_max_buttons_width)
        }
        if (buttonsCount == 1) {
            buttonAction.maxWidth = activity.resources
                    .getDimensionPixelSize(R.dimen.exponea_sdk_in_app_message_max_button_width)
        }
        buttonAction.maxLines = 1
        buttonAction.ellipsize = TextUtils.TruncateAt.END
        buttonAction.text = buttonPayload.buttonText
        buttonAction.setTextColor(parseColor(buttonPayload.buttonTextColor, Color.BLACK))
        buttonAction.setBackgroundColor(
            R.drawable.in_app_message_slide_in_button,
            parseColor(buttonPayload.buttonBackgroundColor, Color.LTGRAY)
        )
        if (buttonPayload.buttonType == InAppMessageButtonType.CANCEL) {
            buttonAction.setOnClickListener {
                onCloseManually()
            }
        } else {
            buttonAction.setOnClickListener {
                onActionClicked(buttonPayload)
            }
        }
    }

    private fun onCloseManually() {
        userInteraction = true
        onDismiss?.invoke(true)
        onDismiss = null // clear the dismiss listener, we called the button listener
        dismiss()
    }

    private fun onActionClicked(buttonPayload: InAppMessagePayloadButton) {
        userInteraction = true
        onButtonClick(buttonPayload)
        onDismiss = null // clear the dismiss listener, we called the button listener
        dismiss()
    }
}
