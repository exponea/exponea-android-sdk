package com.exponea.sdk.view

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.DrawableRes
import com.exponea.sdk.R
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayload.Companion.parseColor
import com.exponea.sdk.models.InAppMessagePayload.Companion.parseFontSize
import kotlinx.android.synthetic.main.in_app_message_dialog.buttonAction
import kotlinx.android.synthetic.main.in_app_message_dialog.buttonClose
import kotlinx.android.synthetic.main.in_app_message_dialog.imageViewImage
import kotlinx.android.synthetic.main.in_app_message_dialog.linearLayoutBackground
import kotlinx.android.synthetic.main.in_app_message_dialog.textViewBody
import kotlinx.android.synthetic.main.in_app_message_dialog.textViewTitle

internal class InAppMessageDialog : Dialog {
    private val payload: InAppMessagePayload
    private val onButtonClick: () -> Unit
    private var onDismiss: (() -> Unit)?
    private val bitmap: Bitmap

    constructor(
        context: Context,
        payload: InAppMessagePayload,
        image: Bitmap,
        onButtonClick: () -> Unit,
        onDismiss: () -> Unit
    ) : super(context) {
        this.payload = payload
        this.bitmap = image
        this.onButtonClick = onButtonClick
        this.onDismiss = onDismiss
        val inflater = LayoutInflater.from(context)
        setContentView(inflater.inflate(R.layout.in_app_message_dialog, null, false))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

    private fun setupImage() {
        imageViewImage.setImageBitmap(bitmap)
    }

    private fun setupTitleText() {
        textViewTitle.text = payload.title
        textViewTitle.setTextColor(parseColor(payload.titleTextColor, Color.BLACK))
        textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, parseFontSize(payload.titleTextSize, 22f))
    }

    private fun setupBodyText() {
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
        buttonAction.text = payload.buttonText
        buttonAction.setTextColor(parseColor(payload.buttonTextColor, Color.BLACK))
        setBackgroundColor(
            buttonAction,
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
        setBackgroundColor(
            linearLayoutBackground,
            R.drawable.in_app_message_dialog_background,
            parseColor(payload.backgroundColor, Color.WHITE)
        )

        window.attributes.width = WindowManager.LayoutParams.MATCH_PARENT
        window.setDimAmount(0.1f)
    }

    @Suppress("DEPRECATION")
    private fun setBackgroundColor(view: View, @DrawableRes backgroundId: Int, color: Int) {
        val drawable = if (Build.VERSION.SDK_INT >= 21)
            context.resources.getDrawable(backgroundId, null)
        else context.resources.getDrawable(backgroundId)
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        if (Build.VERSION.SDK_INT >= 16) {
            view.background = drawable
        } else {
            view.setBackgroundDrawable(drawable)
        }
    }
}
