package com.exponea.sdk.testutil.mocks

import android.app.Application
import android.content.pm.ApplicationInfo

internal open class MockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(androidx.appcompat.R.style.Theme_AppCompat)
    }
}

internal open class DebugMockApplication : MockApplication() {
    override fun getApplicationInfo(): ApplicationInfo {
        return ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_DEBUGGABLE
        }
    }
}

internal open class ReleaseMockApplication : MockApplication() {
    override fun getApplicationInfo(): ApplicationInfo {
        return ApplicationInfo().apply {
            flags = 0
        }
    }
}
