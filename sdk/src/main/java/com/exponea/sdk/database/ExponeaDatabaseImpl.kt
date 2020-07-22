package com.exponea.sdk.database

import android.content.Context
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import io.paperdb.PaperDbException

/**
 * Database uses PaperDB that has an issue with multi-threading https://github.com/pilgr/Paper/issues/114
 * Until that's fixed we need to synchronize access to database.
 * Good thing is that this is the only class that uses PaperDB
 */
internal class ExponeaDatabaseImpl<T>(
    context: Context,
    databaseName: String
) : ExponeaDatabase<DatabaseStorageObject<T>> {
    init {
        Paper.init(context)
    }
    private val book = Paper.book(databaseName)

    override fun all(): ArrayList<DatabaseStorageObject<T>> = synchronized(this) {
        val list = arrayListOf<DatabaseStorageObject<T>>()
        val keys = book.allKeys

        for (key in keys) {
            val value = get(key) ?: continue
            list.add(value)
        }

        return list
    }

    override fun count(): Int {
        return book.allKeys.size
    }

    override fun add(item: DatabaseStorageObject<T>): Boolean = synchronized(this) {
        return try {
            book.write(item.id, item)
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error writing object $item to the database", exception)
            false
        }
    }

    override fun update(item: DatabaseStorageObject<T>): Boolean = synchronized(this) {
        return try {
            book.write(item.id, item)
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error updating $item", exception)
            false
        }
    }

    override fun get(id: String): DatabaseStorageObject<T>? = synchronized(this) {
        return try {
            book.read(id) as DatabaseStorageObject<T>?
        } catch (exception: PaperDbException) {

            // Delete invalid data in case of exception
            remove(id)
            Logger.e(this, "Error reading from database", exception)
            null
        }
    }

    override fun remove(id: String): Boolean = synchronized(this) {
        return try {
            book.delete(id)
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error deleting item from database", exception)
            false
        }
    }

    override fun clear(): Boolean = synchronized(this) {
        return try {
            book.destroy()
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error clearing database", exception)
            false
        }
    }
}
