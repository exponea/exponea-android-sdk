package com.exponea.sdk.repository

import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockDisplayState
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.fromJson
import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal class InAppContentBlockDisplayStateRepositoryImpl(
    private var preferences: ExponeaPreferences,
    private val gsonBase: Gson = ExponeaGson.instance
) : InAppContentBlockDisplayStateRepository {
    companion object {
        internal const val KEY = "ExponeaInAppContentBlockDisplayStates"
        internal const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        internal const val LEGACY_DATE_FORMAT = "MMM dd, yyyy HH:mm:ss a"
    }

    // Default gson date serializer sucks :(
    // There are issues in telemetry where we could not deserialize Date we serialized before
    // We'll set the format explicitly and also make sure old format still works
    private val gson: Gson = gsonBase.newBuilder()
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

    override fun get(message: InAppContentBlock): InAppContentBlockDisplayState = synchronized(this) {
        return getDisplayStates()[message.id] ?: InAppContentBlockDisplayState(
            null, 0,
            null, 0
        )
    }

    override fun setDisplayed(message: InAppContentBlock, date: Date) = synchronized(this) {
        val displayStates = getDisplayStates()
        val displayState = get(message)
        displayStates[message.id] = InAppContentBlockDisplayState(
            date,
            displayState.displayedCount + 1,
            displayState.interactedLast,
            displayState.interactedCount
        )
        setDisplayStates(displayStates)
    }

    override fun setInteracted(message: InAppContentBlock, date: Date) = synchronized(this) {
        val displayStates = getDisplayStates()
        val displayState = get(message)
        displayStates[message.id] = InAppContentBlockDisplayState(
            displayState.displayedLast,
            displayState.displayedCount,
            date,
            displayState.interactedCount + 1
        )
        setDisplayStates(displayStates)
    }

    override fun clear() {
        preferences.remove(KEY)
    }

    private fun setDisplayStates(displayStates: Map<String, InAppContentBlockDisplayState>) {
        preferences.setString(KEY, gson.toJson(displayStates))
    }

    private fun getDisplayStates(): MutableMap<String, InAppContentBlockDisplayState> {
        val dataString = preferences.getString(KEY, "") ?: ""
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
            if (cutOffDate.before(it.value.displayedLast ?: Date(0)) ||
                cutOffDate.before(it.value.interactedLast ?: Date(0))) {
                return@filter true
            }
            return@filter false
        }
        setDisplayStates(filtered)
    }
}
