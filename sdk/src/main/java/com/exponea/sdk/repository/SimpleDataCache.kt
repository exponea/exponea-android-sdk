package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.ParameterizedType

abstract class SimpleDataCache<T>(
    context: Context,
    private val gson: Gson,
    storageFileName: String
) {
    private val storageFile = File(context.filesDir, storageFileName)

    private val dataTypeToken = getTypeToken()

    @Volatile private var data: T? = null

    init {
        try {
            if (storageFile.exists()) {
                data = gson.fromJson(storageFile.readText(), dataTypeToken)
            }
        } catch (e: Throwable) {
            Logger.w(this, "Error while loading '$storageFileName': $e")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getTypeToken(): TypeToken<T> {
        // because of Class extending, it searches for 'SimpleDataCache' definition
        var clazz: Class<*> = javaClass
        var superClazz = clazz.superclass
        while (SimpleDataCache::class.java != superClazz) {
            clazz = superClazz
            superClazz = superClazz.superclass
        }
        val typeArgument = (clazz.genericSuperclass as ParameterizedType).actualTypeArguments[0]
        return if (typeArgument is ParameterizedType) {
            // supports SimpleDataCache<Collection<Class>> or so
            TypeToken.getParameterized(
                typeArgument.rawType,
                typeArgument.actualTypeArguments[0]
            ) as TypeToken<T>
        } else {
            // supports SimpleDataCache<Class>
            val typeTokenClass = typeArgument as Class<T>
            TypeToken.get(typeTokenClass)
        }
    }

    @Synchronized
    fun getData(): T? = data

    @Synchronized
    fun setData(data: T) {
        storageFile.writeText(gson.toJson(data))
        this.data = data
    }

    @Synchronized
    fun clearData(): Boolean {
        data = null
        return storageFile.delete()
    }

    fun getDataLastModifiedMillis(): Long {
        return storageFile.lastModified()
    }
}
