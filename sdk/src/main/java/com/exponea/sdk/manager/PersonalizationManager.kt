package com.exponea.sdk.manager

import com.exponea.sdk.models.Banner

interface PersonalizationManager {
    fun getBannersConfiguration(projectToken: String)
    fun getPersonalization(projectToken: String, banner: Banner)
}