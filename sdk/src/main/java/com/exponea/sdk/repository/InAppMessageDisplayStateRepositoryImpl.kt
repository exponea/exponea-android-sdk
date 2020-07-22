package com.exponea.sdk.repository

import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageDisplayState
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.fromJson
import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal class InAppMessageDisplayStateRepositoryImpl(
    private val preferences: ExponeaPreferences,
    gson: Gson
) : InAppMessageDisplayStateRepository {
    companion object {
        internal const val KEY = "ExponeaInAppMessageDisplayStates"
        internal const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        internal const val LEGACY_DATE_FORMAT = "MMM dd, yyyy HH:mm:ss a"
    }

    // Default gson date serializer sucks :(
    // There are issues in telemetry where we could not deserialize Date we serialized before
    // We'll set the format explicitly and also make sure old format still works
    private val gson: Gson = gson.newBuilder()
        .setDateFormat(DATE_FORMAT)
        .registerTypeHierarchyAdapter(Date::class.java, JsonDeserializer<Date> { src, _, _ ->
            try { // try to parse old date format for compatibility
                return@JsonDeserializer SimpleDateFormat(LEGACY_DATE_FORMAT).parse(src.asString)
            } catch (ignored: Throwable) {}

            try { // try to parse new date format
                return@JsonDeserializer SimpleDateFormat(DATE_FORMAT, Locale.US).parse(src.asString)
            } catch (ignored: Throwable) {}

            // if everything fails, we know the message was displayed/interacted,
            // but don't know when, let's use "a long time ago"
            return@JsonDeserializer Date(0)
        })
        .create()

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
