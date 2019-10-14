package com.exponea.sdk.repository

interface UniqueIdentifierRepository {
    fun get(): String
    fun clear(): Boolean
}