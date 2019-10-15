package com.exponea.example.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import com.google.android.material.textfield.TextInputEditText


fun TextInputEditText.isValid(): Boolean {
    val isValid = !text.toString().isEmpty()
    error = if (isValid) {
        null
    } else {
        "Empty Field"
    }
    return isValid
}

fun TextInputEditText.isVaildUrl(): Boolean {
    val isEmpty = text.toString().isEmpty()
    val text = text ?: ""
    val isUrl = Patterns.WEB_URL.matcher(text).matches() && (text.startsWith("https://") || text.startsWith("http://"))
    error = when {
        isEmpty -> "Empty URL"
        !isUrl -> "Invalid Url"
        else -> null
    }
    return !isEmpty && isUrl
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

fun HashMap<String, Any>.asJson(): String {
    var string = "{\n"
    this.toList().forEachIndexed { index, pair ->
        string += "\t ${pair.first}: ${pair.second}"
        if (index != this.size - 1) string += ",\n"
    }
    return "$string\n}"
}

