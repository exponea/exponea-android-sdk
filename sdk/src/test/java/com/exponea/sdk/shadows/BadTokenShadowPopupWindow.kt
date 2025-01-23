package com.exponea.sdk.shadows

import android.view.WindowManager
import android.view.WindowManager.BadTokenException
import android.widget.PopupWindow
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowPopupWindow

@Implements(value = PopupWindow::class)
open class BadTokenShadowPopupWindow : ShadowPopupWindow() {
    @Implementation
    override fun invokePopup(p: WindowManager.LayoutParams?) {
        throw BadTokenException("Simulation that activity has invalid Window token")
    }
}
