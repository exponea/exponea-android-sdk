package com.exponea.sdk.view

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.databinding.InAppMessageDialogBinding
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.style.ImagePosition
import com.exponea.sdk.util.ConversionUtils
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.UiUtils
import com.exponea.sdk.util.ensureOnMainThread
import com.exponea.sdk.util.setBackgroundColor

internal class InAppMessageDialog : InAppMessageView, Dialog {
    private val parentActivity: Activity
    private var viewBinding: InAppMessageDialogBinding
    private val fullScreen: Boolean
    private val payload: InAppMessagePayload
    private val onButtonClick: (InAppMessagePayloadButton) -> Unit
    private var onDismiss: ((Boolean, InAppMessagePayloadButton?) -> Unit)?
    private var onError: (String) -> Unit
    private val imageCache: DrawableCache

    override val isPresented: Boolean
        get() = isShowing

    constructor(
        activity: Activity,
        fullScreen: Boolean,
        payload: InAppMessagePayload,
        image: DrawableCache,
        onButtonClick: (InAppMessagePayloadButton) -> Unit,
        onDismiss: (Boolean, InAppMessagePayloadButton?) -> Unit,
        onError: (String) -> Unit
    ) : super(activity) {
        this.parentActivity = activity
        this.fullScreen = fullScreen
        this.payload = payload
        this.imageCache = image
        this.onButtonClick = onButtonClick
        this.onDismiss = onDismiss
        this.onError = onError
        this.viewBinding = InAppMessageDialogBinding.inflate(LayoutInflater.from(context), null, false)
        setContentView(viewBinding.root)
        Exponea.deintegration.registerForIntegrationStopped(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupPositions()
        setupFullscreen()
        setupImage()
        setupCloseButton()
        setupTitleText()
        setupBodyText()
        setupButtons()
        setupWindow()

        setOnDismissListener {
            Exponea.deintegration.unregisterForIntegrationStopped(this)
            if (!Exponea.isStopped) {
                this.onDismiss?.invoke(false, null)
            }
            parentActivity.finish()
        }
    }

    private fun setupFullscreen() {
        // setup padding
        val padding = if (fullScreen) {
            context.resources.getDimensionPixelSize(R.dimen.exponea_sdk_in_app_message_fullscreen_padding)
        } else {
            context.resources.getDimensionPixelSize(R.dimen.exponea_sdk_in_app_message_dialog_padding)
        }
        viewBinding.inAppMessageDialogContainer.setPadding(padding, padding, padding, padding)

        val rootParams = viewBinding.inAppMessageDialogRoot.layoutParams as ConstraintLayout.LayoutParams
        val imageParams = viewBinding.imageViewImage.layoutParams as ConstraintLayout.LayoutParams
        if (fullScreen) {
            // remove constraint on dialog max. width
            rootParams.matchConstraintMaxWidth = -1

            // let image height take up whole screen
            rootParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            imageParams.height = 0
        } else {
            imageParams.constrainedHeight = true
        }
    }

    private fun setupPositions() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(viewBinding.inAppMessageDialogRoot)
        constraintSet.removeFromVerticalChain(viewBinding.linearLayoutBackground.id)
        constraintSet.removeFromVerticalChain(viewBinding.imageViewImage.id)
        val imagePosition = ImagePosition.parse(payload)
        if (payload.isTextOverImage == true) { // image is from top to bottom, text is either top or bottom
            constraintSet.connect(
                viewBinding.imageViewImage.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                0
            )
            constraintSet.connect(
                viewBinding.imageViewImage.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )
            constraintSet.connect(
                viewBinding.linearLayoutBackground.id,
                if (imagePosition == ImagePosition.SECONDARY) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                if (imagePosition == ImagePosition.SECONDARY) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                0
            )
        } else {
            // which element is at top
            constraintSet.connect(
                if (imagePosition == ImagePosition.PRIMARY) {
                    viewBinding.imageViewImage.id
                } else {
                    viewBinding.linearLayoutBackground.id
                },
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                0
            )
            // 2-way connection between bottom and top of elements
            constraintSet.connect(
                viewBinding.imageViewImage.id,
                if (imagePosition == ImagePosition.SECONDARY) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                viewBinding.linearLayoutBackground.id,
                if (imagePosition == ImagePosition.SECONDARY) ConstraintSet.BOTTOM else ConstraintSet.TOP,
                0
            )
            constraintSet.connect(
                viewBinding.linearLayoutBackground.id,
                if (imagePosition == ImagePosition.SECONDARY) ConstraintSet.BOTTOM else ConstraintSet.TOP,
                viewBinding.imageViewImage.id,
                if (imagePosition == ImagePosition.SECONDARY) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                0
            )
            // which element is at bottom
            constraintSet.connect(
                if (imagePosition == ImagePosition.PRIMARY) {
                    viewBinding.linearLayoutBackground.id
                } else {
                    viewBinding.imageViewImage.id
                },
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )
        }
        constraintSet.applyTo(viewBinding.inAppMessageDialogRoot)
    }

    private fun setupImage() {
        val imagePosition = ImagePosition.parse(payload)
        viewBinding.imageViewImage.isOnTop = imagePosition == ImagePosition.PRIMARY
        viewBinding.imageViewImage.textOverImage = payload.isTextOverImage == true
        imageCache.showImage(
            payload.imageUrl,
            viewBinding.imageViewImage,
            onImageNotLoaded = {
                onError("Image '${payload.imageUrl}' not loaded successfully")
                onDismiss = null // clear the dismiss listener, we called the button listener
                dismiss()
            }
        )
    }

    private fun setupTitleText() {
        if (payload.title.isNullOrEmpty()) {
            viewBinding.textViewTitle.visibility = View.GONE
            return
        }
        viewBinding.textViewTitle.text = payload.title
        viewBinding.textViewTitle.setTextColor(ConversionUtils.parseColor(payload.titleTextColor) ?: Color.BLACK)
        viewBinding.textViewTitle.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            UiUtils.parseFontSize(payload.titleTextSize, 22f)
        )
    }

    private fun setupBodyText() {
        if (payload.bodyText.isNullOrEmpty()) {
            viewBinding.textViewBody.visibility = View.GONE
            return
        }
        viewBinding.textViewBody.text = payload.bodyText
        viewBinding.textViewBody.setTextColor(ConversionUtils.parseColor(payload.bodyTextColor) ?: Color.BLACK)
        viewBinding.textViewBody.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            UiUtils.parseFontSize(payload.bodyTextSize, 14f)
        )
    }

    private fun setupCloseButton() {
        viewBinding.buttonClose.setOnClickListener {
            dismissMessageWithClosingInteraction(null)
        }
        viewBinding.buttonClose.setTextColor(ConversionUtils.parseColor(payload.closeButtonIconColor) ?: Color.WHITE)
    }

    private fun dismissMessageWithClosingInteraction(buttonPayload: InAppMessagePayloadButton?) {
        onDismiss?.invoke(true, buttonPayload)
        // clear the dismiss listener, we called the manual listener
        onDismiss = null
        dismiss()
    }

    private fun setupButtons() {
        val buttonsCount = if (payload.buttons != null) payload.buttons.count() else 0
        val button1Payload = if (payload.buttons != null && payload.buttons.isNotEmpty()) payload.buttons[0] else null
        val button2Payload = if (payload.buttons != null && payload.buttons.count() > 1) payload.buttons[1] else null
        setupButton(viewBinding.buttonAction1, button1Payload, buttonsCount)
        setupButton(viewBinding.buttonAction2, button2Payload, buttonsCount)
    }

    private fun setupButton(buttonAction: Button, buttonPayload: InAppMessagePayloadButton?, buttonsCount: Int) {
        if (buttonPayload == null) {
            viewBinding.buttonSpace.visibility = View.GONE
            buttonAction.visibility = View.GONE
            return
        }
        if (buttonsCount == 2) {
            buttonAction.maxWidth = context.resources
                    .getDimensionPixelSize(R.dimen.exponea_sdk_in_app_message_max_buttons_width)
        }
        if (buttonsCount == 1) {
            buttonAction.maxWidth = context.resources
                    .getDimensionPixelSize(R.dimen.exponea_sdk_in_app_message_max_button_width)
        }
        buttonAction.text = buttonPayload.text
        buttonAction.setTextColor(ConversionUtils.parseColor(buttonPayload.textColor) ?: Color.BLACK)
        buttonAction.setBackgroundColor(
            R.drawable.in_app_message_dialog_button,
            ConversionUtils.parseColor(buttonPayload.backgroundColor) ?: Color.LTGRAY
        )
        if (buttonPayload.buttonType == InAppMessageButtonType.CANCEL) {
            buttonAction.setOnClickListener {
                dismissMessageWithClosingInteraction(buttonPayload)
            }
        } else {
            buttonAction.setOnClickListener {
                onButtonClick(buttonPayload)
                onDismiss = null // clear the dismiss listener, we called the button listener
                dismiss()
            }
        }
    }

    private fun setupWindow() {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (payload.isTextOverImage == true) {
            viewBinding.linearLayoutBackground.setBackgroundColor(Color.TRANSPARENT)
        } else {
            val imagePosition = ImagePosition.parse(payload)
            val backgroundDrawable = if (imagePosition == ImagePosition.PRIMARY)
                R.drawable.in_app_message_dialog_background_bottom
                else R.drawable.in_app_message_dialog_background_top
            viewBinding.linearLayoutBackground.setBackgroundColor(
                backgroundDrawable,
                ConversionUtils.parseColor(payload.backgroundColor) ?: Color.WHITE
            )
        }

        window?.attributes?.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes?.height = WindowManager.LayoutParams.MATCH_PARENT
        window?.setDimAmount(0.5f)
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

    override fun onIntegrationStopped() {
        ensureOnMainThread {
            dismiss()
        }
    }
}
