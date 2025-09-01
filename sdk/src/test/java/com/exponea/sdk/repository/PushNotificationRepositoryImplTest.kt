package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.repository.PushNotificationRepositoryImpl.Companion.KEY_DELIVERED_DATA
import com.exponea.sdk.repository.PushNotificationRepositoryImpl.Companion.MAX_STORED_NOTIFICATIONS
import com.google.gson.Gson
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Test

class PushNotificationRepositoryImplTest {

    private val mockExponeaPreferences: ExponeaPreferences = mockk()
    private val repository = PushNotificationRepositoryImpl(mockExponeaPreferences)

    @Test
    fun `should store notification when no previous data exists`() {
        val data = mapOf("title" to "Title", "message" to "message")
        val expectedResult = Gson().toJson(listOf(data))

        val notificationDataToStore = slot<String>()

        every { mockExponeaPreferences.getString(KEY_DELIVERED_DATA, "") } returns ""
        every { mockExponeaPreferences.setString(KEY_DELIVERED_DATA, capture(notificationDataToStore)) } just Runs

        repository.appendDeliveredNotification(data)

        assertEquals(expectedResult, notificationDataToStore.captured)
    }

    @Test
    fun `should append notification to existing stored data`() {
        val newData = mapOf("title" to "Title 2", "message" to "Message 2")
        val storedData = listOf(mapOf("title" to "Title 1", "message" to "Message 1"))
        val expectedResult = Gson().toJson(storedData + newData)

        val notificationDataToStore = slot<String>()

        every { mockExponeaPreferences.getString(KEY_DELIVERED_DATA, "") } returns Gson().toJson(storedData)
        every { mockExponeaPreferences.setString(KEY_DELIVERED_DATA, capture(notificationDataToStore)) } just Runs

        repository.appendDeliveredNotification(newData)

        assertEquals(expectedResult, notificationDataToStore.captured)
    }

    @Test
    fun `should remove oldest notification when limit is reached`() {
        val newData = mapOf("title" to "Title 101", "message" to "Message 101")
        val storedData = (1..MAX_STORED_NOTIFICATIONS).map { mapOf("title" to "Title $it", "message" to "Message $it") }
        val expectedResult = Gson().toJson(storedData.drop(1) + newData)

        val notificationDataToStore = slot<String>()

        every { mockExponeaPreferences.getString(KEY_DELIVERED_DATA, "") } returns Gson().toJson(storedData)
        every { mockExponeaPreferences.setString(KEY_DELIVERED_DATA, capture(notificationDataToStore)) } just Runs

        repository.appendDeliveredNotification(newData)

        assertEquals(expectedResult, notificationDataToStore.captured)
    }

    @Test
    fun `should remove all notifications exceeding the max limit when appending new notification`() {
        val newData = mapOf("title" to "Title 201", "message" to "Message 201")
        val storedData = (1..200).map { mapOf("title" to "Title $it", "message" to "Message $it") }
        val expectedResult = Gson().toJson(storedData.drop(101) + newData)

        val notificationDataToStore = slot<String>()

        every { mockExponeaPreferences.getString(KEY_DELIVERED_DATA, "") } returns Gson().toJson(storedData)
        every { mockExponeaPreferences.setString(KEY_DELIVERED_DATA, capture(notificationDataToStore)) } just Runs

        repository.appendDeliveredNotification(newData)

        assertEquals(expectedResult, notificationDataToStore.captured)
    }
}
