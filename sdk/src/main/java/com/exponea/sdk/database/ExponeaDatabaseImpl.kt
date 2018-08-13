package com.exponea.sdk.database

import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import io.paperdb.PaperDbException

class ExponeaDatabaseImpl<T>(private val databaseName: String) : ExponeaDatabase<DatabaseStorageObject<T>> {
    val book = Paper.book(databaseName)

    override fun all(): ArrayList<DatabaseStorageObject<T>> {
        val list = arrayListOf<DatabaseStorageObject<T>>()
        val keys = book.allKeys

        for (key in keys) {
            val value = get(key) ?: continue
            list.add(value)
        }

        return list
    }

    override fun add(item: DatabaseStorageObject<T>): Boolean {
        return try {
            book.write(item.id, item)
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error writing object $item to the database", exception)
            false
        }
    }

    override fun update(item: DatabaseStorageObject<T>): Boolean {
        return try {
            book.write(item.id, item)
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error updating $item", exception)
            false
        }
    }

    override fun get(id: String): DatabaseStorageObject<T>? {
        return try {
            book.read(id) as DatabaseStorageObject<T>?
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error reading from database", exception)
            null
        }
    }

    override fun remove(id: String): Boolean {
        return try {
            book.delete(id)
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error deleting item from database", exception)
            false
        }
    }

    override fun clear(): Boolean {
        return try {
            book.destroy()
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error clearing database", exception)
            false
        }
    }

}