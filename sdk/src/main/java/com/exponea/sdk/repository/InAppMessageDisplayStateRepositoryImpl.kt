package com.exponea.sdk.repository

import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageDisplayState
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.fromJson
import com.google.gson.Gson
import java.util.Calendar
import java.util.Date

internal class InAppMessageDisplayStateRepositoryImpl(
    private val preferences: ExponeaPreferences,
    private val gson: Gson
) : InAppMessageDisplayStateRepository {
    companion object {
        private const val KEY = "ExponeaInAppMessageDisplayStates"
    }

    init {
        deleteOldDisplayStates()
    }

    override fun get(message: InAppMessage): InAppMessageDisplayState = synchronized(this) {
        return getDisplayStates()[message.id] ?: InAppMessageDisplayState(null, null)
    }

    override fun setDisplayed(message: InAppMessage, date: Date) = synchronized(this) {
        val displayStates = getDisplayStates()
        displayStates[message.id] = InAppMessageDisplayState(date, get(message).interacted)
        setDisplayStates(displayStates)
    }

    override fun setInteracted(message: InAppMessage, date: Date) = synchronized(this) {
        val displayStates = getDisplayStates()
        displayStates[message.id] = InAppMessageDisplayState(get(message).displayed, date)
        setDisplayStates(displayStates)
    }

    override fun clear() {
        preferences.remove(KEY)
    }

    private fun setDisplayStates(displayStates: Map<String, InAppMessageDisplayState>) {
        preferences.setString(KEY, gson.toJson(displayStates))
    }

    private fun getDisplayStates(): MutableMap<String, InAppMessageDisplayState> {
        val dataString = preferences.getString(KEY, "")
        if (dataString.isEmpty()) {
            return hashMapOf()
        }
        return gson.fromJson(dataString)
    }

    private fun deleteOldDisplayStates() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val cutOffDate = calendar.time
        val filtered = getDisplayStates().filter {
            if (cutOffDate.before(it.value.displayed ?: Date(0)) || cutOffDate.before(it.value.interacted ?: Date(0))) {
                return@filter true
            }
            return@filter false
        }
        setDisplayStates(filtered)
    }
}
