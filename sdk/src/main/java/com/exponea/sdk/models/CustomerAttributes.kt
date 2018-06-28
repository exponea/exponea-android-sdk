package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class CustomerAttributes(

        @SerializedName("attributes")
        var attributes: MutableList<HashMap<String, Any>> = mutableListOf()
) {


    internal constructor(customerIds: CustomerIds, attributes: MutableList<HashMap<String, Any>>) : this (attributes) {
        this.customerIds = customerIds
    }

    companion object {
        const val TYPE = "type"
        const val TYPE_PROPERTY = "property"
        const val TYPE_ID = "id"
        const val TYPE_AGGREGATE = "aggregate"
        const val TYPE_EXPRESSION = "expression"
        const val TYPE_PREDICTION = "prediction"
        const val TYPE_SEGMENTATION = "segmentation"
        private const val id = "id"
    }

    internal var customerIds = CustomerIds()

    fun toHashMap() : HashMap<String, Any> {
        return hashMapOf (
                "customer_ids" to customerIds.toHashMap(),
                "attributes" to attributes
        )
    }

    /**
     * Add property to the requested attributes
     * @param propertyName - name of the requested property ( ex: "first_name" )
     */
    fun withProperty(propertyName: String) {
        addAttribute(AttributeTypes.PROPERTY, propertyName)
    }

    /**
     * Add id to the requested attributes
     * @param cookie - name of the external id you want to retrieve
     */
    fun withId(cookie: String) {
        addAttribute(AttributeTypes.ID, cookie)
    }

    /**
     * Add segmentation to the requested attributes
     * @param segmentationId - id of the segmentation you want to retrieve
     */
    fun withSegmentation(segmentationId: String) {
        addAttribute(AttributeTypes.SEGMENTATION, segmentationId)
    }

    /**
     * Add expression to the requested attributes
     * @param expressionId - id of the expression you want to retrieve
     */
    fun withExpression(expressionId: String) {
        addAttribute(AttributeTypes.EXPRESSION, expressionId)
    }

    /**
     * Add aggregation to the requested attributes
     * @param  aggregationId - id of aggregation that you want to retrieve
     */
    fun withAggregation(aggregationId: String) {
        addAttribute(AttributeTypes.AGGREGATION, aggregationId)
    }

    /**
     * Add prediction to the requested attributes
     * @param predictionId: D of aggregation that you want to retrieve
     */
    fun withPrediction(predictionId: String) {
        addAttribute(AttributeTypes.PREDICTION, predictionId)
    }

    /**
     * @param types - Requested attribute type
     * @param associatedValue - Requested attribute identification
     */
    private fun addAttribute(types: AttributeTypes, associatedValue: Any) {
        when (types) {
            AttributeTypes.PROPERTY     -> {
                attributes.add(
                        hashMapOf(
                                Pair(TYPE, TYPE_PROPERTY),
                                Pair(TYPE_PROPERTY, associatedValue)
                        )
                )
            }
            AttributeTypes.PREDICTION   -> {
                attributes.add(
                        hashMapOf(
                                Pair(TYPE, TYPE_PREDICTION),
                                Pair(id, associatedValue)
                        )
                )
            }
            AttributeTypes.ID           -> {
                attributes.add(
                        hashMapOf(
                                Pair(TYPE, TYPE_ID),
                                Pair(TYPE_ID, associatedValue)
                        )
                )
            }
            AttributeTypes.EXPRESSION   -> {
                attributes.add(
                        hashMapOf(
                                Pair(TYPE, TYPE_EXPRESSION),
                                Pair(id, associatedValue)
                        )
                )
            }
            AttributeTypes.SEGMENTATION -> {
                attributes.add(
                        hashMapOf(
                                Pair(TYPE, TYPE_SEGMENTATION),
                                Pair(id, associatedValue)
                        )
                )
            }
            AttributeTypes.AGGREGATION  -> {
                attributes.add(
                        hashMapOf(
                                Pair(TYPE, TYPE_AGGREGATE),
                                Pair(id, associatedValue)
                        )
                )
            }
        }
    }

}
