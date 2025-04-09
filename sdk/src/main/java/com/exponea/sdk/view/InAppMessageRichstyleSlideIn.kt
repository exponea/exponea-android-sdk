package com.exponea.sdk.view

import android.animation.Animator
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.exponea.sdk.Exponea
import com.exponea.sdk.databinding.InAppMessageRichstyleSlideInBinding
import com.exponea.sdk.models.ButtonUiPayload
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageUiPayload
import com.exponea.sdk.style.ImagePosition
import com.exponea.sdk.style.ImageSizing
import com.exponea.sdk.style.MessagePosition
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ensureOnMainThread
import com.exponea.sdk.view.component.InAppButtonView
import com.exponea.sdk.view.component.InAppImageView
import com.google.android.material.behavior.SwipeDismissBehavior

internal class InAppMessageRichstyleSlideIn : PopupWindow, InAppMessageView {
    companion object {
        const val ANIMATION_DURATION = 250L
    }

    private var viewBinding: InAppMessageRichstyleSlideInBinding

    private val activity: Activity
    private val payload: InAppMessageUiPayload
    private val onButtonClick: (InAppMessagePayloadButton) -> Unit
    private var onDismiss: ((Boolean, InAppMessagePayloadButton?) -> Unit)?
    private var onError: (String) -> Unit
    private var userInteraction = false

    override val isPresented: Boolean
        get() = isShowing

    private var targetImageViews: List<InAppImageView>

    constructor(
        activity: Activity,
        payload: InAppMessageUiPayload,
        onButtonClick: (InAppMessagePayloadButton) -> Unit,
        onDismiss: (Boolean, InAppMessagePayloadButton?) -> Unit,
        onError: (String) -> Unit
    ) : super(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {
        this.activity = activity
        this.payload = payload
        this.onButtonClick = onButtonClick
        this.onDismiss = onDismiss
        this.onError = onError
        viewBinding = InAppMessageRichstyleSlideInBinding.inflate(LayoutInflater.from(activity), null, false)
        contentView = viewBinding.root
        targetImageViews = listOf(
            viewBinding.inAppMessageSlideInBackgroundImage,
            viewBinding.inAppMessageSlideInLeftImage,
            viewBinding.inAppMessageSlideInRightImage
        )
        Exponea.deintegration.registerForIntegrationStopped(this)
        setupContainer()
        setupBackground()
        setupImage()
        setupCloseButton()
        setupTitleText()
        setupBodyText()
        setupButtons()
        setOnDismissListener {
            Exponea.deintegration.unregisterForIntegrationStopped(this)
            if (Exponea.isStopped) {
                return@setOnDismissListener
            }
            if (!userInteraction) {
                this.onDismiss?.invoke(false, null)
            }
        }
    }

    private fun setupCloseButton() {
        val buttonClose = viewBinding.buttonClose
        if (!payload.closeButton.style.enabled) {
            buttonClose.visibility = View.GONE
            return
        }
        buttonClose.setOnClickListener {
            onCloseManually(null)
        }
        buttonClose.applyStyle(payload.closeButton.style)
        buttonClose.setImageDrawable(payload.closeButton.icon)
    }

    private fun setupContainer() {
        val container = viewBinding.inAppMessageSlideInContainer
        val containerLayoutParams = container.layoutParams as ViewGroup.MarginLayoutParams
        containerLayoutParams.setMargins(
            payload.container.containerMargin.left.toPx(),
            payload.container.containerMargin.top.toPx(),
            payload.container.containerMargin.right.toPx(),
            payload.container.containerMargin.bottom.toPx()
        )
        container.layoutParams = containerLayoutParams
        val inAppMessageSlideInBody = viewBinding.inAppMessageSlideInBody
        inAppMessageSlideInBody.setPadding(
            payload.container.containerPadding.left.toPx(),
            payload.container.containerPadding.top.toPx(),
            payload.container.containerPadding.right.toPx(),
            payload.container.containerPadding.bottom.toPx()
        )
        container.radius = payload.container.containerRadius.toPrecisePx()
    }

    override fun show() {
        try {
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val containerPosition = payload.container.containerPosition
            contentView.translationY = when (containerPosition) {
                MessagePosition.BOTTOM -> 1
                else -> -1
            } * contentView.measuredHeight.toFloat()
            contentView
                .animate()
                .setDuration(ANIMATION_DURATION)
                .translationY(0f)
                .start()

            showAtLocation(
                activity.window.decorView.rootView,
                if (containerPosition == MessagePosition.BOTTOM) Gravity.BOTTOM else Gravity.TOP,
                0,
                0
            )
            setupSwipeToDismiss()
        } catch (e: Exception) {
            Logger.e(this, "[InApp] Unable to show SlideIn in-app message", e)
            onError.invoke("Invalid app foreground state")
        }
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
                when (payload.container.containerPosition) {
                    MessagePosition.BOTTOM -> 1
                    else -> -1
                } * contentView.measuredHeight.toFloat()
            )
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) { superDismiss() }
                override fun onAnimationCancel(animation: Animator) { superDismiss() }
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            .start()
    }

    override fun onIntegrationStopped() {
        ensureOnMainThread {
            dismiss()
        }
    }

    private fun setupSwipeToDismiss() {
        val containerView = viewBinding.inAppMessageSlideInContainer
        val swipeToDismissBehavior = SwipeDismissBehavior<LinearLayout>()
        swipeToDismissBehavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_START_TO_END)
        swipeToDismissBehavior.listener = object : SwipeDismissBehavior.OnDismissListener {
            override fun onDismiss(view: View) {
                onCloseManually(null)
            }
            override fun onDragStateChanged(state: Int) {}
        }
        (containerView.layoutParams as CoordinatorLayout.LayoutParams).behavior = swipeToDismissBehavior
    }

    private fun setupBackground() {
        val container = viewBinding.inAppMessageSlideInContainer
        container.setCardBackgroundColor(Color.TRANSPARENT)
        val inAppMessageSlideInBackground = viewBinding.inAppMessageSlideInBackground
        inAppMessageSlideInBackground.setBackgroundColor(payload.container.backgroundColor)
        payload.container.backgroundOverlayColor?.let {
            inAppMessageSlideInBackground.foreground = ColorDrawable(it)
        }
    }

    private fun setupImage() {
        val targetImageView: InAppImageView? = activateTargetImageView()
        targetImageView?.applyStyle(payload.image.style)
        targetImageView?.setImageDrawable(payload.image.source)
    }

    private fun activateTargetImageView(): InAppImageView? {
        val targetImageView: InAppImageView? = when {
            !payload.image.style.enabled -> null
            payload.image.source == null -> null
            payload.image.style.sizing == ImageSizing.FULLSCREEN -> {
                viewBinding.inAppMessageSlideInBackgroundImage
            }
            payload.container.imagePosition == ImagePosition.PRIMARY -> {
                viewBinding.inAppMessageSlideInLeftImage
            }
            payload.container.imagePosition == ImagePosition.SECONDARY -> {
                viewBinding.inAppMessageSlideInRightImage
            }
            payload.container.imagePosition == ImagePosition.OVERLAY -> {
                viewBinding.inAppMessageSlideInBackgroundImage
            }
            else -> {
                Logger.w(this, "Unable to determine target image view for image")
                null
            }
        }
        targetImageView?.visibility = View.VISIBLE
        targetImageViews
            .filter { it != targetImageView }
            .forEach { it.visibility = View.GONE }
        return targetImageView
    }

    private fun setupTitleText() {
        val textViewTitle = viewBinding.textViewTitle
        if (!payload.title.style.enabled) {
            textViewTitle.visibility = View.GONE
            return
        }
        textViewTitle.applyStyle(payload.title.style)
        textViewTitle.text = payload.title.value
    }

    private fun setupBodyText() {
        val textViewBody = viewBinding.textViewBody
        if (!payload.content.style.enabled) {
            textViewBody.visibility = View.GONE
            return
        }
        textViewBody.applyStyle(payload.content.style)
        textViewBody.text = payload.content.value
    }

    private fun setupButtons() {
        val buttonsContainer = viewBinding.buttonsContainer
        buttonsContainer.applyStyle(payload.container.buttonsAlignment)
        payload.buttons.mapNotNull { buttonPayload ->
            buildActionButton(buttonPayload)
        }.forEach { buttonView ->
            buttonsContainer.addView(buttonView)
        }
    }

    private fun buildActionButton(buttonPayload: ButtonUiPayload?): InAppButtonView? {
        if (buttonPayload?.originPayload == null) {
            return null
        }
        if (!buttonPayload.style.enabled) {
            return null
        }
        val button = InAppButtonView(activity)
        button.applyStyle(buttonPayload.style)
        button.text = buttonPayload.text
        if (buttonPayload.originPayload.buttonType == InAppMessageButtonType.CANCEL) {
            button.setOnClickListener {
                onCloseManually(buttonPayload.originPayload)
            }
        } else {
            button.setOnClickListener {
                onActionClicked(buttonPayload.originPayload)
            }
        }
        return button
    }

    private fun onCloseManually(cancelButtonPayload: InAppMessagePayloadButton?) {
        userInteraction = true
        onDismiss?.invoke(true, cancelButtonPayload)
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
