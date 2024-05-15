package com.exponea.sdk.testutil

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File

class MockFile : File(
    ApplicationProvider.getApplicationContext<Context>().filesDir,
    "mock-file"
) {
    init {
        if (!exists()) {
            createNewFile()
            deleteOnExit()
            writeText("mock-file-content")
        }
    }
}
