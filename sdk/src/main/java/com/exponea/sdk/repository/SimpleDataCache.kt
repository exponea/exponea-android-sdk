package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.ParameterizedType

open class SimpleDataCache<T>(
    context: Context,
    private val gson: Gson,
    private val storageFileName: String
) {
    private val storageFile = File(context.cacheDir, storageFileName)
    private var data: T? = null

    private val DATA_TYPE_TOKEN = getTypeToken()

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

    fun setData(data: T) {
        val file = createTempFile()
        file.writeText(gson.toJson(data))
        clearData()
        if (!file.renameTo(storageFile)) {
            Logger.e(this, "Renaming data file to '$storageFileName' failed!")
        }
    }

    fun clearData(): Boolean {
        data = null
        return storageFile.delete()
    }

    fun getData(): T? {
        data?.let { return it }
        try {
            if (storageFile.exists()) {
                val fileData = storageFile.readText()
                val dataArray: T? = gson.fromJson(fileData, DATA_TYPE_TOKEN)
                data = dataArray?.let { return it }
            }
        } catch (e: Throwable) {
            Logger.w(this, "Error while getting stored data from '$storageFileName': $e")
        }
        return data
    }

    fun getDataLastModifiedMillis(): Long {
        return storageFile.lastModified()
    }
}
