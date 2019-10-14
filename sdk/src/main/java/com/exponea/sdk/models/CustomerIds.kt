package com.exponea.sdk.models

import com.exponea.sdk.util.Logger
import okhttp3.Cookie
import java.util.HashMap

data class CustomerIds(
        internal var externalIds : HashMap<String, Any?> = hashMapOf()
) {

    init {
        externalIds.remove(COOKIE)
    }

    companion object {
        private const val COOKIE = "cookie"
    }

    internal var cookie: String? = null

    internal constructor(cookie: String) : this() {
        this.cookie = cookie
    }




    fun withId(key: String, value: Any?) : CustomerIds {
        if (key == COOKIE) {
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
            set(COOKIE, cookie as Any?)
        }
    }

}