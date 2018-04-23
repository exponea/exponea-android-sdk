package com.exponea.sdk.models

object ApiEndPoint {
    const val splitterToken = "$$$"

    const val trackCustomers = "track/v2/projects/$splitterToken/customers"
    const val trackEvents = "track/v2/projects/$splitterToken/customers/events"
    const val tokenRotate = "data/v2/$splitterToken/tokens/rotate"
    const val tokenRevoke = "data/v2/$splitterToken/tokens/revoke"
    const val customersProperty = "data/v2/$splitterToken/customers/property"
    const val customersId = "data/v2/$splitterToken/customers/id"
    const val customersSegmentation = "data/v2/$splitterToken/customers/segmentation"
    const val customersExpression = "data/v2/$splitterToken/customers/expression"
    const val customersPrediction = "data/v2/$splitterToken/customers/prediction"
    const val customersRecommendation = "data/v2/$splitterToken/customers/recommendation"
    const val customersAttributes = "/data/v2/$splitterToken/customers/attributes"
    const val customersEvents = "/data/v2/projects/$splitterToken/customers/events"
    const val customersAnonymize = "/data/v2/$splitterToken/customers/anonymize"
    const val customersExportAllProperties = "/data/v2/$splitterToken/customers/export-one"
    const val customersExportAll = "/data/v2/$splitterToken/customers/export"
}