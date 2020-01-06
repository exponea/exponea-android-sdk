package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.paperdb.Paper
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaComponentTest : ExponeaSDKTest() {
    @Test
    fun `gson should serialize obscure number values`() {
        Paper.init(ApplicationProvider.getApplicationContext())
        val gson = ExponeaComponent(ExponeaConfiguration(), ApplicationProvider.getApplicationContext()).gson
        assertEquals(
            """{"zero":0.0,"inf":"Infinity","pi":3.14159,"-inf":"-Infinity","nan":"NaN"}""",
            gson.toJson(hashMapOf(
                "pi" to 3.14159f,
                "zero" to 0.0f,
                "inf" to Float.POSITIVE_INFINITY,
                "-inf" to Float.NEGATIVE_INFINITY,
                "nan" to Float.NaN
            ))
        )
        assertEquals(
            """{"zero":0.0,"inf":"Infinity","pi":3.14159,"-inf":"-Infinity","nan":"NaN"}""",
            gson.toJson(hashMapOf(
                "pi" to 3.14159,
                "zero" to 0.0,
                "inf" to Double.POSITIVE_INFINITY,
                "-inf" to Double.NEGATIVE_INFINITY,
                "nan" to Double.NaN
            ))
        )
    }
}
