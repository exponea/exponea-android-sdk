package com.exponea.sdk.database

import com.exponea.sdk.models.DatabaseStorageObject
import io.paperdb.Paper

class ExponeaDatabaseImpl<T>(private val databaseName: String) : ExponeaDatabase<DatabaseStorageObject<T>> {
    val book = Paper.book(databaseName)

    override fun all(): ArrayList<DatabaseStorageObject<T>> {
        val list = arrayListOf<DatabaseStorageObject<T>>()
        val keys = book.allKeys

        for (key in keys) {
            val value = book.read<DatabaseStorageObject<T>>(key)
            list.add(value)
        }

        return list
    }

    override fun add(item: DatabaseStorageObject<T>): Boolean {
        book.write(item.id, item)
        return true
    }

    override fun update(item: DatabaseStorageObject<T>): Boolean {
        book.write(item.id, item)
        return true
    }

    override fun get(id: String): DatabaseStorageObject<T> {
        return book.read(id)
    }

    override fun remove(id: String): Boolean {
        book.delete(id)
        return true
    }

    override fun clear(): Boolean {
        book.destroy()
        return true
    }

}