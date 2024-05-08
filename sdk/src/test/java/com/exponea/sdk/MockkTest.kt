package com.exponea.sdk

import io.mockk.every
import io.mockk.mockkConstructor
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * We are currently using Mockk library for unit tests.
 * Mockk library has changed from version 1.11.0 where unexpected behaviour occurs.
 * To be able to register and update for 'every {}' stub block we need to register one 'every {}' stub block
 * before instance creation. Only then the behaviour stays same as before 1.11.0.
 * This test class provides description of changed behaviour and also validates the behaviour of current Mockk library.
 * If this behaviour will be fixed (as many Github issues for same problem has been reported)
 * then this test class will show.
 */
@RunWith(RobolectricTestRunner::class)
internal class MockkTest {

    open class MockkObj {
        fun add(a: Int, b: Int) = a + b
        fun sub(a: Int, b: Int) = a - b
    }
    @Test
    fun `mockk should pass - origin mockk behaviour`() {
        // constructor for class has to be moccked
        mockkConstructor(MockkObj::class)
        // any stub for any method has to be registered first
        every { anyConstructed<MockkObj>().add(any(), any()) } returns 4
        // new instance has to be created but after stub registration
        val mockkObjInstance = MockkObj()
        // and only then everything is working as expected
        assertEquals(4, mockkObjInstance.add(1, 2))
        // also stub updates are working
        every { anyConstructed<MockkObj>().add(any(), any()) } returns 5
        assertEquals(5, mockkObjInstance.add(1, 2))
        // also stubs for other methods are updating as expected
        assertEquals(5, mockkObjInstance.sub(10, 5))
        every { anyConstructed<MockkObj>().sub(any(), any()) } returns 2
        assertEquals(2, mockkObjInstance.sub(10, 5))
    }

    @Test
    fun `mockk should not pass - origin mockk behaviour`() {
        // constructor for class has to be moccked
        mockkConstructor(MockkObj::class)
        // new instance is created but is not automatically mocked as expected
        val mockkObjInstance = MockkObj()
        // so this stub will not be applied
        every { anyConstructed<MockkObj>().add(any(), any()) } returns 4
        // so original method is called and returns 3 instead of 4
        assertEquals(3, mockkObjInstance.add(1, 2))
        // and no stub update will be applied
        every { anyConstructed<MockkObj>().add(any(), any()) } returns 5
        // so original method is still called and returns 3 instead of 5
        assertEquals(3, mockkObjInstance.add(1, 2))
        assertEquals(5, mockkObjInstance.sub(10, 5))
        every { anyConstructed<MockkObj>().sub(any(), any()) } returns 2
        assertEquals(5, mockkObjInstance.sub(10, 5))
        // but stub upon instance is working
        every { mockkObjInstance.add(any(), any()) } returns 4
        assertEquals(4, mockkObjInstance.add(1, 2))
        every { mockkObjInstance.sub(any(), any()) } returns 2
        assertEquals(2, mockkObjInstance.sub(10, 5))
    }

    @Test
    fun `mockk should pass - modified mockk behaviour`() {
        // copies `mockk should not pass - origin mockk behaviour` test but used mockkConstructorFix
        mockkConstructorFix(MockkObj::class) {
            every { anyConstructed<MockkObj>().add(any(), any()) }
        }
        val mockkObjInstance = MockkObj()
        every { anyConstructed<MockkObj>().add(any(), any()) } returns 4
        assertEquals(4, mockkObjInstance.add(1, 2))
        every { anyConstructed<MockkObj>().add(any(), any()) } returns 5
        assertEquals(5, mockkObjInstance.add(1, 2))
        assertEquals(5, mockkObjInstance.sub(10, 5))
        every { anyConstructed<MockkObj>().sub(any(), any()) } returns 2
        assertEquals(2, mockkObjInstance.sub(10, 5))
        every { mockkObjInstance.add(any(), any()) } returns 4
        assertEquals(4, mockkObjInstance.add(1, 2))
        every { mockkObjInstance.sub(any(), any()) } returns 2
        assertEquals(2, mockkObjInstance.sub(10, 5))
    }
}

inline fun <reified T : kotlin.Any> mockkConstructorFix(
    clazz: kotlin.reflect.KClass<T>
) = mockkConstructorFix<T, Any>(clazz, null)

inline fun <reified T : kotlin.Any, reified S : kotlin.Any> mockkConstructorFix(
    clazz: kotlin.reflect.KClass<T>,
    noinline stubBlock: (() -> io.mockk.MockKStubScope<S, S>)? = null
) {
    mockkConstructor(clazz)
    stubBlock?.let {
        it() answers { callOriginal() }
        return
    }
    val noArgsFun = clazz.declaredFunctions
        .filter { it.visibility == kotlin.reflect.KVisibility.PUBLIC }
        .firstOrNull {
        // first parameter is instance parameter
        it.parameters.size <= 1
    }
    noArgsFun?.let {
        // register stub
        every { anyConstructed<T>().invokeNoArgs(noArgsFun.name) } answers { callOriginal() }
        return
    }
    val field = clazz.declaredMemberProperties.firstOrNull { it.visibility == kotlin.reflect.KVisibility.PUBLIC }
    field?.let {
        // register stub
        every { anyConstructed<T>().getProperty(field.name) } answers { callOriginal() }
        return
    }
    // not able to register any stub
    fail("Unable to mock constructor of class ${clazz.simpleName}")
}
