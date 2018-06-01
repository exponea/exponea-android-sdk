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

fun HashMap<String, Any>.asJson() : String{
    var string = "{\n"
    this.toList().forEachIndexed { index, pair ->
        string += "\t ${pair.first}: ${pair.second}"
        if (index != this.size - 1) string += ",\n"
    }
    return "$string\n}"
}
