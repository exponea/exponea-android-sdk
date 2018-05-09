package com.exponea.sdk.manager

import com.exponea.sdk.models.CustomerIds

interface PersonalizationManager {
    fun showBanner(projectToken: String, customerIds: CustomerIds)
    fun getBannersConfiguration(projectToken: String, customerIds: CustomerIds)
    fun getPersonalization(projectToken: String, customerIds: CustomerIds)
}