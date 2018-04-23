package com.exponea.sdk.database

import com.exponea.sdk.models.DatabaseItem
import io.paperdb.Paper

class PaperExponeaDatabase<T : DatabaseItem>(private val databaseName: String) : ExponeaDatabase<T> {
    val book = Paper.book(databaseName)

    override fun all(): ArrayList<T> {
        val list = arrayListOf<T>()
        val keys = book.allKeys

        for (key in keys) {
            val value = book.read<T>(key)
            list.add(value)
        }

        return list
    }

    override fun add(item: T): Boolean {
        book.write(item.id, item)
        return true
    }

    override fun update(item: T): Boolean {
        book.write(item.id, item)
        return true
    }

    override fun get(id: String): T {
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