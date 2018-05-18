package com.exponea.example.utils

import android.support.design.widget.TextInputEditText
import android.text.Editable
import android.text.TextWatcher


fun TextInputEditText.isValid() : Boolean{
    val isValid = !text.toString().isEmpty()
    error = if (isValid) {
        null
    } else {
        "Empty Field"
    }
    return isValid
}

fun TextInputEditText.onTextChanged(callback: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            //
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            //
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val text = s.toString()
            callback(text)
            isValid()
        }
    })
}
