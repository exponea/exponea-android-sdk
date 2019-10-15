package com.exponea.sdk.database

interface ExponeaDatabase<T> {
    fun all(): ArrayList<T>
    fun add(item: T): Boolean
    fun update(item: T): Boolean
    fun get(id: String): T?
    fun remove(id: String): Boolean
    fun clear(): Boolean
}

