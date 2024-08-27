package com.exponea.sdk.shadows

import android.content.Context
import android.media.Ringtone
import android.net.Uri
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Suppress("UNUSED_PARAMETER")
@Implements(Ringtone::class)
open class ShadowRingtone {
    companion object {
        var lastRingtone: ShadowRingtone? = null
    }
    var withUri: Uri? = null
    var wasPlayed: Boolean = false

    @Implementation
    fun __constructor__(context: Context, allowRemote: Boolean) {
        lastRingtone = this
    }

    @Implementation
    fun setUri(uri: Uri) {
        withUri = uri
    }

    @Implementation
    open fun play() {
        wasPlayed = true
    }
}
