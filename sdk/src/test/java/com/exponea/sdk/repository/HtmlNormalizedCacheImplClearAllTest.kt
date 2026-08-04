package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.util.HtmlNormalizer
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HtmlNormalizedCacheImplClearAllTest {

    private lateinit var context: Context
    private lateinit var prefs: ExponeaPreferencesImpl
    private lateinit var cache: HtmlNormalizedCacheImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = ExponeaPreferencesImpl(context)
        cache = HtmlNormalizedCacheImpl(context, prefs)
    }

    @Test
    fun `clearAll removes HTML cache metadata from preferences`() {
        cache.set("block-1", "<html></html>", HtmlNormalizer.NormalizedResult())

        cache.clearAll()

        assertFalse(prefs.getString("${HtmlNormalizedCacheImpl.HASH_PREFIX}block-1", "").isNotEmpty())
        assertFalse(prefs.getString("${HtmlNormalizedCacheImpl.FILE_PREFIX}block-1", "").isNotEmpty())
        assertNull(cache.get("block-1", "<html></html>"))
    }
}
