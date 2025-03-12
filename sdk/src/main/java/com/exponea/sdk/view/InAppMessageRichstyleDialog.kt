package com.exponea.sdk.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import com.exponea.sdk.R
import com.exponea.sdk.databinding.InAppMessageRichstyleDialogBinding
import com.exponea.sdk.models.ButtonUiPayload
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageUiPayload
import com.exponea.sdk.style.ImagePosition
import com.exponea.sdk.style.ImageSizing
import com.exponea.sdk.util.Logger
import com.exponea.sdk.view.component.InAppButtonView
import com.exponea.sdk.view.component.InAppImageView

internal class InAppMessageRichstyleDialog : InAppMessageView, Dialog {
    private var viewBinding: InAppMessageRichstyleDialogBinding
    private val fullScreen: Boolean
    private val payload: InAppMessageUiPayload
    private val onButtonClick: (InAppMessagePayloadButton) -> Unit
    private var onDismiss: ((Boolean, InAppMessagePayloadButton?) -> Unit)?
    private var onError: (String) -> Unit

    override val isPresented: Boolean
        get() = isShowing

    private var targetImageViews: List<InAppImageView>

    constructor(
        context: Context,
        fullScreen: Boolean,
        payload: InAppMessageUiPayload,
        onButtonClick: (InAppMessagePayloadButton) -> Unit,
        onDismiss: (Boolean, InAppMessagePayloadButton?) -> Unit,
        onError: (String) -> Unit
    ) : super(context) {
        this.fullScreen = fullScreen
        this.payload = payload
        this.onButtonClick = onButtonClick
        this.onDismiss = onDismiss
        this.onError = onError
        viewBinding = InAppMessageRichstyleDialogBinding.inflate(LayoutInflater.from(context), null, false)
        setContentView(viewBinding.root)
        targetImageViews = listOf(
            viewBinding.inAppMessageDialogTopImage,
            viewBinding.inAppMessageDialogBottomImage,
            viewBinding.inAppMessageDialogBackgroundImage
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupContainer()
        setupImage()
        setupCloseButton()
        setupTitleText()
        setupBodyText()
        setupButtons()

        setOnDismissListener {
            this.onDismiss?.invoke(false, null)
        }
    }

    private fun setupContainer() {
        val containerWrapLayoutParams = viewBinding.inAppMessageDialogWidthLimiter.layoutParams as MarginLayoutParams
        containerWrapLayoutParams.setMargins(
            payload.container.containerMargin.left.toPx(),
            payload.container.containerMargin.top.toPx(),
            payload.container.containerMargin.right.toPx(),
            payload.container.containerMargin.bottom.toPx()
        )
        viewBinding.inAppMessageDialogWidthLimiter.layoutParams = containerWrapLayoutParams
        viewBinding.inAppMessageDialogBody.setPadding(
            payload.container.containerPadding.left.toPx(),
            payload.container.containerPadding.top.toPx(),
            payload.container.containerPadding.right.toPx(),
            payload.container.containerPadding.bottom.toPx()
        )
        if (fullScreen) {
            viewBinding.inAppMessageDialogWidthLimiter.setMaxWidth(-1)
            setHeight(viewBinding.inAppMessageDialogContainer, ViewGroup.LayoutParams.MATCH_PARENT)
            setHeight(viewBinding.inAppMessageDialogBody, ViewGroup.LayoutParams.MATCH_PARENT)
            viewBinding.inAppMessageDialogContent.isFillViewport = true
        } else {
            viewBinding.inAppMessageDialogWidthLimiter.setMaxWidth(
                context.resources.getDimension(R.dimen.exponea_sdk_in_app_message_dialog_max_width).toInt()
            )
            setHeight(viewBinding.inAppMessageDialogContainer, ViewGroup.LayoutParams.WRAP_CONTENT)
            setHeight(viewBinding.inAppMessageDialogBody, ViewGroup.LayoutParams.WRAP_CONTENT)
            viewBinding.inAppMessageDialogContent.isFillViewport = false
        }
        viewBinding.inAppMessageDialogContainer.radius = payload.container.containerRadius.toPrecisePx()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        viewBinding.inAppMessageDialogContainer.setCardBackgroundColor(Color.TRANSPARENT)
        viewBinding.inAppMessageDialogBackground.setBackgroundColor(payload.container.backgroundColor)
        payload.container.backgroundOverlayColor?.let {
            viewBinding.inAppMessageDialogBackground.foreground = ColorDrawable(it)
        }
        window?.attributes?.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes?.height = WindowManager.LayoutParams.MATCH_PARENT
        window?.setDimAmount(0.5f)
    }

    private fun setHeight(target: View, height: Int) {
        val newLayoutParams = target.layoutParams
        newLayoutParams.height = height
        target.layoutParams = newLayoutParams
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
            payload.image.style.sizing == ImageSizing.FULLSCREEN ->
                viewBinding.inAppMessageDialogBackgroundImage
            payload.container.imagePosition == ImagePosition.PRIMARY ->
                viewBinding.inAppMessageDialogTopImage
            payload.container.imagePosition == ImagePosition.SECONDARY ->
                viewBinding.inAppMessageDialogBottomImage
            payload.container.imagePosition == ImagePosition.OVERLAY ->
                viewBinding.inAppMessageDialogBackgroundImage
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
        if (!payload.title.style.enabled) {
            viewBinding.textViewTitle.visibility = View.GONE
            return
        }
        viewBinding.textViewTitle.applyStyle(payload.title.style)
        viewBinding.textViewTitle.text = payload.title.value
    }

    private fun setupBodyText() {
        if (!payload.content.style.enabled) {
            viewBinding.textViewBody.visibility = View.GONE
            return
        }
        viewBinding.textViewBody.applyStyle(payload.content.style)
        viewBinding.textViewBody.text = payload.content.value
    }

    private fun setupCloseButton() {
        if (!payload.closeButton.style.enabled) {
            viewBinding.buttonClose.visibility = View.GONE
            return
        }
        viewBinding.buttonClose.setOnClickListener {
            dismissMessageWithClosingInteraction(null)
        }
        viewBinding.buttonClose.applyStyle(payload.closeButton.style)
        viewBinding.buttonClose.setImageDrawable(payload.closeButton.icon)
    }

    private fun dismissMessageWithClosingInteraction(buttonPayload: InAppMessagePayloadButton?) {
        onDismiss?.invoke(true, buttonPayload)
        // clear the dismiss listener, we called the manual listener
        onDismiss = null
        dismiss()
    }

    private fun setupButtons() {
        viewBinding.buttonsContainer.applyStyle(payload.container.buttonsAlignment)
        payload.buttons.mapNotNull { buttonPayload ->
            buildActionButton(buttonPayload)
        }.forEach { buttonView ->
            viewBinding.buttonsContainer.addView(buttonView)
        }
    }

    private fun buildActionButton(buttonPayload: ButtonUiPayload?): InAppButtonView? {
        if (buttonPayload?.originPayload == null) {
            return null
        }
        if (!buttonPayload.style.enabled) {
            return null
        }
        val button = InAppButtonView(context)
        button.applyStyle(buttonPayload.style)
        button.text = buttonPayload.text
        if (buttonPayload.originPayload.buttonType == InAppMessageButtonType.CANCEL) {
            button.setOnClickListener {
                dismissMessageWithClosingInteraction(buttonPayload.originPayload)
            }
        } else {
            button.setOnClickListener {
                onButtonClick(buttonPayload.originPayload)
                onDismiss = null // clear the dismiss listener, we called the button listener
                dismiss()
            }
        }
        return button
    }

    override fun show() {
        try {
            super.show()
        } catch (e: Exception) {
            val messageMode = if (fullScreen) "Fullscreen" else "Modal"
            Logger.e(this, "[InApp] Unable to show $messageMode in-app message", e)
            onError.invoke("Invalid app foreground state")
        }
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            val messageMode = if (fullScreen) "Fullscreen" else "Modal"
            Logger.e(this, "[InApp] Dismissing $messageMode in-app message failed", e)
        }
    }
}
