package com.exponea.sdk.testutil

import com.exponea.sdk.Exponea
import org.junit.Before

open class ExponeaSDKTest {
    @Before
    fun resetExponea() {
        Exponea.reset()
    }
}