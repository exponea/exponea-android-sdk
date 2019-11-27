package com.exponea.sdk.runcatching

import com.exponea.sdk.Exponea
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PublicApiCoveredTest : ExponeaSDKTest() {
    @Test
    fun shouldCoverAllPublicProperties() {
        Exponea::class.declaredMemberProperties.forEach { property ->
            if (property.visibility == KVisibility.PUBLIC) {
                assertTrue(
                    PublicApiTestCases.properties.any { it.first.name == property.name },
                    "Public property $property not found in PublicApiTestCases."
                )
            }
        }
    }

    @Test
    fun shouldCoverAllPublicMethods() {
        val compareFunctions: (KFunction<*>, KFunction<*>) -> Boolean = { f1, f2 ->
            f1.toString().equals(f2.toString())
        }
        Exponea::class.declaredFunctions.forEach { method ->
            if (method.visibility == KVisibility.PUBLIC) {
                assertTrue(
                    PublicApiTestCases.methods.any { compareFunctions(it.first, method) } ||
                        PublicApiTestCases.initMethods.any { compareFunctions(it.first, method) },
                    "Public method $method not found in PublicApiTestCases."
                )
            }
        }
    }
}
