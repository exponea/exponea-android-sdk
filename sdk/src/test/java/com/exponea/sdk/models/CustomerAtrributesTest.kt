package com.exponea.sdk.models

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CustomerAtrributesTest {


    @Test
    fun testConstructor() {
        val customerAttributes = CustomerAttributes()
        assertTrue(customerAttributes.attributes.isEmpty())
    }

    @Test
    fun testAddProperty() {
        val customerAttributes = CustomerAttributes()
        customerAttributes.withProperty("name")
        val map = customerAttributes.attributes[0] as HashMap<String, String>
        assertTrue (map[CustomerAttributes.TYPE] == CustomerAttributes.TYPE_PROPERTY)
        assertTrue (map[CustomerAttributes.TYPE_PROPERTY] == "name")
    }


    @Test
    fun testAddPrediction() {
        val customerAttributes = CustomerAttributes()
        customerAttributes.withPrediction("name")
        val map = customerAttributes.attributes[0] as HashMap<String, String>
        assertTrue (map[CustomerAttributes.TYPE] == CustomerAttributes.TYPE_PREDICTION)
        assertTrue (map[CustomerAttributes.TYPE_ID] == "name")
    }
    @Test
    fun testAddSegment() {
        val customerAttributes = CustomerAttributes()
        customerAttributes.withSegmentation("name")
        val map = customerAttributes.attributes[0] as HashMap<String, String>
        assertTrue (map[CustomerAttributes.TYPE] == CustomerAttributes.TYPE_SEGMENTATION)
        assertTrue (map[CustomerAttributes.TYPE_ID] == "name")
    }
    @Test
    fun testAddAggregation() {
        val customerAttributes = CustomerAttributes()
        customerAttributes.withAggregation("name")
        val map = customerAttributes.attributes[0] as HashMap<String, String>
        assertTrue (map[CustomerAttributes.TYPE] == CustomerAttributes.TYPE_AGGREGATE)
        assertTrue (map[CustomerAttributes.TYPE_ID] == "name")
    }

    @Test
    fun testAddExpression() {
        val customerAttributes = CustomerAttributes()
        customerAttributes.withExpression("name")
        val map = customerAttributes.attributes[0] as HashMap<String, String>
        assertTrue (map[CustomerAttributes.TYPE] == CustomerAttributes.TYPE_EXPRESSION)
        assertTrue (map[CustomerAttributes.TYPE_ID] == "name")
    }

    @Test
    fun testAddId() {
        val customerAttributes = CustomerAttributes()
        customerAttributes.withId("name")
        val map = customerAttributes.attributes[0] as HashMap<String, String>
        assertTrue (map[CustomerAttributes.TYPE] == CustomerAttributes.TYPE_ID)
        assertTrue (map[CustomerAttributes.TYPE_ID] == "name")
    }
}