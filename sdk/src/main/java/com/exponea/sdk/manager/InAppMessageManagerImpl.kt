package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.util.Logger

internal class InAppMessageManagerImpl(
    context: Context,
    private val configuration: ExponeaConfiguration,
    private val customerIdsRepository: CustomerIdsRepository,
    private val inAppMessagesCache: InAppMessagesCache,
    private val fetchManager: FetchManager,
    private val bitmapCache: InAppMessageBitmapCache = InAppMessageBitmapCacheImpl(context)
) : InAppMessageManager {

    override fun preload(callback: ((Result<Unit>) -> Unit)?) {
        fetchManager.fetchInAppMessages(
            projectToken = configuration.projectToken,
            customerIds = customerIdsRepository.get(),
            onSuccess = { result ->
                Logger.i(this, "In-app messages preloaded successfully.")
                inAppMessagesCache.set(result.results)
                preloadImages(result.results)
                callback?.invoke(Result.success(Unit))
            },
            onFailure = {
                Logger.e(this, "Preloading in-app messages failed.")
                callback?.invoke(Result.failure(Exception("Preloading in-app messages failed.")))
            }
        )
    }

    private fun preloadImages(messages: List<InAppMessage>) {
        bitmapCache.clearExcept(messages.map { it.payload.imageUrl })
        messages.forEach { bitmapCache.preload(it.payload.imageUrl) }
    }

    private fun canDisplay(message: InAppMessage): Boolean {
        return bitmapCache.has(message.payload.imageUrl)
    }

    override fun get(): InAppMessage? {
        val messages = inAppMessagesCache.get().filter {
            canDisplay(it)
        }
        return if (messages.isNotEmpty()) messages.random() else null
    }
}
