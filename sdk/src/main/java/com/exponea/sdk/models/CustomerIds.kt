package com.exponea.sdk.models

import com.exponea.sdk.util.Logger
import okhttp3.Cookie
import java.util.HashMap

data class CustomerIds(
        private var externalIds : HashMap<String, Any?> = hashMapOf()
) {

    var cookie: String? = null
        internal set(value) {
            field = value
        }

    internal constructor(cookie: String) : this() {
        this.cookie = cookie
    }

    fun withId(key: String, value: Any?) : CustomerIds {
        if (key == "cookie") {
            Logger.e(this, "Changing cookie is not allowed")
            return this
        }
        externalIds[key] = value
        return this
    }



    internal fun toHashMap() : HashMap<String, Any?> {
        if (cookie.isNullOrEmpty()) {
            Logger.e(this, "Empty cookie")
        }
        return externalIds.apply {
            set("cookie", cookie as Any?)
        }
    }

}