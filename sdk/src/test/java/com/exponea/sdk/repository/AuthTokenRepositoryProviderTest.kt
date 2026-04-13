package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.mockkConstructorFix
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class AuthTokenRepositoryProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkConstructorFix(AuthTokenRepositoryImpl::class) {
            every { anyConstructed<AuthTokenRepositoryImpl>().clear() }
        }
        every { anyConstructed<AuthTokenRepositoryImpl>().clear() } just Runs
    }

    @After
    fun tearDown() {
        AuthTokenRepositoryProvider.clear()
        unmockkAll()
    }

    @Test
    fun `get should return non-null repository`() {
        val repo = AuthTokenRepositoryProvider.get(context)
        assertThat(repo, notNullValue())
    }

    @Test
    fun `get should return same instance on subsequent calls`() {
        val first = AuthTokenRepositoryProvider.get(context)
        val second = AuthTokenRepositoryProvider.get(context)
        assertThat(second, sameInstance(first))
    }

    @Test
    fun `clear should call clear on existing instance`() {
        AuthTokenRepositoryProvider.get(context)
        AuthTokenRepositoryProvider.clear()

        verify(exactly = 1) { anyConstructed<AuthTokenRepositoryImpl>().clear() }
    }

    @Test
    fun `clear should not fail when no instance exists`() {
        AuthTokenRepositoryProvider.clear()
        verify(exactly = 0) { anyConstructed<AuthTokenRepositoryImpl>().clear() }
    }

    @Test
    fun `get should create new instance after clear`() {
        val first = AuthTokenRepositoryProvider.get(context)
        AuthTokenRepositoryProvider.clear()
        val second = AuthTokenRepositoryProvider.get(context)

        assertThat(second, not(sameInstance(first)))
    }

    @Test
    fun `get should use application context`() {
        val repo = AuthTokenRepositoryProvider.get(context)
        assertThat(repo, notNullValue())
    }

    @Test
    fun `get should create preferences with dedicated EXPONEA_AUTH file`() {
        AuthTokenRepositoryProvider.get(context)
        verify { context.applicationContext.getSharedPreferences("EXPONEA_AUTH", any()) }
    }
}
