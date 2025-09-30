import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.DeviceIdManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceIdManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        DeviceIdManager.clear(context)
    }

    @Test
    fun `get device id from manager`() {
        val deviceId = DeviceIdManager.getDeviceId(context)
        assertNotNull(deviceId)
        assertEquals(deviceId, DeviceIdManager.getDeviceId(context))
    }

    @Test
    fun `get device id from SharedPreferences after clearing internal cache`() {
        val firstId = DeviceIdManager.getDeviceId(context)
        clearInternalCachedDeviceId()

        val secondId = DeviceIdManager.getDeviceId(context)
        assertEquals(firstId, secondId)
    }

    private fun clearInternalCachedDeviceId() {
        val field = DeviceIdManager::class.java.getDeclaredField("cachedDeviceId")
        field.isAccessible = true
        field.set(null, null)
    }

    @Test
    fun `get device id after DeviceIdManager clear`() {
        val deviceId1 = DeviceIdManager.getDeviceId(context)
        DeviceIdManager.clear(context)
        val deviceId2 = DeviceIdManager.getDeviceId(context)
        assertNotEquals(deviceId1, deviceId2)
    }

    @Test
    fun `all threads should return the same deviceId`() {
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(1)
        val results = mutableListOf<String>()

        try {
            repeat(threadCount) {
                executor.execute {
                    latch.await() // wait until all threads are ready
                    val id = DeviceIdManager.getDeviceId(context)
                    synchronized(results) { results.add(id) }
                }
            }

            latch.countDown() // release all threads at the same time
            executor.shutdown() // stop accepting new tasks

            // wait max 2 seconds for all threads to finish
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow() // force stop if not finished
            }
            val uniqueIds = results.toSet()
            assertEquals(1, uniqueIds.size)
        } finally {
            // cleanup
            executor.shutdownNow()
            DeviceIdManager.clear(context)
        }
    }
}
