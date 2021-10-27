package com.exponea.sdk.database

import android.content.Context
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import io.paperdb.PaperDbException

internal class PaperDatabase(
    context: Context,
    databaseName: String
) {
    init {
        Paper.init(context)
    }
    private val book = Paper.book(databaseName)

    fun all(): ArrayList<DatabaseStorageObject<ExportedEventType>> = synchronized(this) {
        val list = arrayListOf<DatabaseStorageObject<ExportedEventType>>()
        val keys = book.allKeys

        for (key in keys) {
            val value = get(key) ?: continue
            list.add(value)
        }

        return list
    }

    fun get(id: String): DatabaseStorageObject<ExportedEventType>? = synchronized(this) {
        return try {
            book.read(id) as DatabaseStorageObject<ExportedEventType>?
        } catch (exception: PaperDbException) {

            // Delete invalid data in case of exception
            remove(id)
            Logger.e(this, "Error reading from database", exception)
            null
        }
    }

    fun count(): Int {
        return book.allKeys.size
    }

    fun remove(id: String): Boolean = synchronized(this) {
        return try {
            book.delete(id)
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error deleting item from database", exception)
            false
        }
    }

    fun clear(): Boolean = synchronized(this) {
        return try {
            book.destroy()
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error clearing database", exception)
            false
        }
    }

    fun add(item: DatabaseStorageObject<ExportedEventType>): Boolean = synchronized(this) {
        return try {
            book.write(item.id, item)
            true
        } catch (exception: PaperDbException) {
            Logger.e(this, "Error writing object $item to the database", exception)
            false
        }
    }
}
