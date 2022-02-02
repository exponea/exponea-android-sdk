package com.exponea.sdk.models

class InAppMessageButton(
    var text: String?,
    var url: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InAppMessageButton

        if (text != other.text) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text?.hashCode() ?: 0
        result = 31 * result + (url?.hashCode() ?: 0)
        return result
    }
}
