package com.exponea.sdk.shadows

import android.media.Ringtone
import com.exponea.sdk.runcatching.ExponeaExceptionThrowing
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(Ringtone::class)
class OnlyDefaultShadowRingtone : ShadowRingtone() {

    @Implementation
    override fun play() {
        if (withUri?.toString() != "content://settings/system/notification_sound") {
            throw ExponeaExceptionThrowing.TestPurposeException()
        }
        super.play()
    }
}
