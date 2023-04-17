package com.exponea.sdk.models

import com.exponea.sdk.util.Logger

data class CustomerIds(
    internal var externalIds: HashMap<String, String?> = hashMapOf()
) {

    init {
        externalIds.remove(COOKIE)
    }

    companion object {
        internal const val COOKIE = "cookie"
    }

    internal var cookie: String? = null

    internal constructor(cookie: String) : this() {
        this.cookie = cookie
    }

    fun withId(key: String, value: String?): CustomerIds {
        if (key == COOKIE) {
            Logger.e(this, "Changing cookie is not allowed")
            return this
        }
        externalIds[key] = value
        return this
    }

    internal fun toHashMap(): HashMap<String, String?> {
        if (cookie.isNullOrEmpty()) {
            Logger.e(this, "Empty cookie")
        }
        return externalIds.apply {
            set(COOKIE, cookie)
        }
    }
}
