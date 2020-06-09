package com.exponea.sdk.testutil.mocks

import android.app.Application
import com.exponea.sdk.R

internal class MockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_AppCompat)
    }
}
