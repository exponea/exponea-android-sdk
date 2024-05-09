package com.exponea.sdk.testutil

import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.robolectric.shadows.ShadowLooper

/**
 * Runs all coroutines in Dispatchers.Main only for given block. To invoke scheduler works call function as parameter.
 * Dispatchers are reset to default after block execution.
 * Example:
 * ```
 * @Test
 * fun `your test`() = runInSingleThread { idleThreads ->
 *      var invoked = false
 *      runBackgroundThread {
 *          // do something
 *          invoked = true
 *      }
 *      idleThreads()
 *      assertTrue(invoked)
 * }
 * ```
 */
internal inline fun runInSingleThread(crossinline testBlock: (() -> Unit) -> Unit) {
    mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
    backgroundThreadDispatcher = CoroutineScope(Dispatchers.Main)
    try {
        testBlock.invoke {
            ShadowLooper.idleMainLooper()
        }
    } finally {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Default)
    }
}
