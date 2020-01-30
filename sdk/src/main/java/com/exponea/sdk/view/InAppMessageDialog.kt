package com.exponea.sdk.view

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.exponea.sdk.R
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayload.Companion.parseColor
import com.exponea.sdk.models.InAppMessagePayload.Companion.parseFontSize
import com.exponea.sdk.models.TextPosition
import com.exponea.sdk.util.setBackgroundColor
import kotlinx.android.synthetic.main.in_app_message_dialog.buttonAction
import kotlinx.android.synthetic.main.in_app_message_dialog.buttonClose
import kotlinx.android.synthetic.main.in_app_message_dialog.imageViewImage
import kotlinx.android.synthetic.main.in_app_message_dialog.inAppMessageDialogContainer
import kotlinx.android.synthetic.main.in_app_message_dialog.inAppMessageDialogRoot
import kotlinx.android.synthetic.main.in_app_message_dialog.linearLayoutBackground
import kotlinx.android.synthetic.main.in_app_message_dialog.textViewBody
import kotlinx.android.synthetic.main.in_app_message_dialog.textViewTitle

internal class InAppMessageDialog : InAppMessageView, Dialog {
    private val fullScreen: Boolean
    private val payload: InAppMessagePayload
    private val onButtonClick: () -> Unit
    private var onDismiss: (() -> Unit)?
    private val bitmap: Bitmap

    constructor(
        context: Context,
        fullScreen: Boolean,
        payload: InAppMessagePayload,
        image: Bitmap,
        onButtonClick: () -> Unit,
        onDismiss: () -> Unit
    ) : super(context) {
        this.fullScreen = fullScreen
        this.payload = payload
        this.bitmap = image
        this.onButtonClick = onButtonClick
        this.onDismiss = onDismiss
        val inflater = LayoutInflater.from(context)
        setContentView(inflater.inflate(R.layout.in_app_message_dialog, null, false))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupPositions()
        setupFullscreen()
        setupImage()
        setupCloseButton()
        setupTitleText()
        setupBodyText()
        setupButton()
        setupWindow()

        setOnDismissListener {
            this.onDismiss?.invoke()
        }
    }

    private fun setupFullscreen() {
        // setup padding
        val padding = if (fullScreen) {
            context.resources.getDimensionPixelSize(R.dimen.exponea_sdk_in_app_message_fullscreen_padding)
        } else {
            context.resources.getDimensionPixelSize(R.dimen.exponea_sdk_in_app_message_dialog_padding)
        }
        inAppMessageDialogContainer.setPadding(padding, padding, padding, padding)

        val rootParams = inAppMessageDialogRoot.layoutParams as ConstraintLayout.LayoutParams
        val imageParams = imageViewImage.layoutParams as ConstraintLayout.LayoutParams
        if (fullScreen) {
            // remove constraint on dialog max. width
            rootParams.matchConstraintMaxWidth = -1

            // let image height take up whole screen
            rootParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            imageParams.height = 0
        }
    }

    private fun setupPositions() {
        var constraintSet = ConstraintSet()
        constraintSet.clone(inAppMessageDialogRoot)
        constraintSet.removeFromVerticalChain(linearLayoutBackground.id)
        constraintSet.removeFromVerticalChain(imageViewImage.id)

        if (payload.isTextOverImage == true) { // image is from top to bottom, text is either top or bottom
            constraintSet.connect(
                imageViewImage.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                0
            )
            constraintSet.connect(
                imageViewImage.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )
            constraintSet.connect(
                linearLayoutBackground.id,
                if (payload.textPosition == TextPosition.TOP) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                if (payload.textPosition == TextPosition.TOP) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                0
            )
        } else {
            // which element is at top
            constraintSet.connect(
                if (payload.textPosition == TextPosition.BOTTOM) imageViewImage.id else linearLayoutBackground.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                0
            )
            // 2-way connection between bottom and top of elements
            constraintSet.connect(
                imageViewImage.id,
                if (payload.textPosition == TextPosition.TOP) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                linearLayoutBackground.id,
                if (payload.textPosition == TextPosition.TOP) ConstraintSet.BOTTOM else ConstraintSet.TOP,
                0
            )
            constraintSet.connect(
                linearLayoutBackground.id,
                if (payload.textPosition == TextPosition.TOP) ConstraintSet.BOTTOM else ConstraintSet.TOP,
                imageViewImage.id,
                if (payload.textPosition == TextPosition.TOP) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                0
            )
            // which element is at bottom
            constraintSet.connect(
                if (payload.textPosition == TextPosition.BOTTOM) linearLayoutBackground.id else imageViewImage.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )
        }
        constraintSet.applyTo(inAppMessageDialogRoot)
    }

    private fun setupImage() {
        imageViewImage.isOnTop = payload.textPosition == TextPosition.BOTTOM
        imageViewImage.textOverImage = payload.isTextOverImage == true
        imageViewImage.setImageBitmap(bitmap)
    }

    private fun setupTitleText() {
        if (payload.title.isNullOrEmpty()) {
            textViewTitle.visibility = View.GONE
            return
        }
        textViewTitle.text = payload.title
        textViewTitle.setTextColor(parseColor(payload.titleTextColor, Color.BLACK))
        textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, parseFontSize(payload.titleTextSize, 22f))
    }

    private fun setupBodyText() {
        if (payload.bodyText.isNullOrEmpty()) {
            textViewBody.visibility = View.GONE
            return
        }
        textViewBody.text = payload.bodyText
        textViewBody.setTextColor(parseColor(payload.bodyTextColor, Color.BLACK))
        textViewBody.setTextSize(TypedValue.COMPLEX_UNIT_DIP, parseFontSize(payload.bodyTextSize, 14f))
    }

    private fun setupCloseButton() {
        buttonClose.setOnClickListener {
            dismiss()
        }
        buttonClose.setTextColor(parseColor(payload.closeButtonColor, Color.WHITE))
    }

    private fun setupButton() {
        if (payload.bodyText.isNullOrEmpty()) {
            buttonAction.visibility = View.GONE
            return
        }
        buttonAction.text = payload.buttonText
        buttonAction.setTextColor(parseColor(payload.buttonTextColor, Color.BLACK))
        buttonAction.setBackgroundColor(
            R.drawable.in_app_message_dialog_button,
            parseColor(payload.buttonBackgroundColor, Color.LTGRAY)
        )
        buttonAction.setOnClickListener {
            onButtonClick()
            onDismiss = null // clear the dismiss listener, we called the button listener
            dismiss()
        }
    }

    private fun setupWindow() {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (payload.isTextOverImage == true) {
            linearLayoutBackground.setBackgroundColor(Color.TRANSPARENT)
        } else {
            val backgroundDrawable = if (payload.textPosition == TextPosition.BOTTOM)
                R.drawable.in_app_message_dialog_background_bottom
                else R.drawable.in_app_message_dialog_background_top
            linearLayoutBackground.setBackgroundColor(
                backgroundDrawable,
                parseColor(payload.backgroundColor, Color.WHITE)
            )
        }

        window.attributes.width = WindowManager.LayoutParams.MATCH_PARENT
        window.setDimAmount(0.1f)
    }
}
