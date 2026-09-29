package com.exponea.example.tracking

/**
 * Predefined in-app message test campaigns for the Track Custom Event dialog.
 *
 * Convention (QA / Engagement backend):
 *   event name = [eventName], property key = `property`,
 *   property value = `test_msg_<type>`.
 *
 * Session Start is excluded — it fires on app launch, not via this dialog.
 */
data class InAppMessagePreset(
    val label: String,
    val category: String,
    val eventName: String,
    val properties: Map<String, String>
)

private const val TEST_EVENT = "event_name"
private const val PROPERTY_KEY = "property"

val inAppMessagePresets: List<InAppMessagePreset> = listOf(
    // Core rendering
    InAppMessagePreset("Control Group", "Core rendering", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_control_group")),
    InAppMessagePreset("Alert Rich", "Core rendering", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_alert_rich")),
    InAppMessagePreset("Alert Legacy", "Core rendering", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_alert_legacy")),
    InAppMessagePreset("Slide-in", "Core rendering", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_slidein")),
    InAppMessagePreset("Modal", "Core rendering", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_modal")),
    InAppMessagePreset("Fullscreen", "Core rendering", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_fullscreen")),
    InAppMessagePreset("HTML", "Core rendering", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_html")),
    InAppMessagePreset("HTML No Close", "Core rendering", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_html_noclose")),
    InAppMessagePreset(
        "Data Link External",
        "Core rendering",
        TEST_EVENT,
        mapOf(PROPERTY_KEY to "test_msg_datalink")
    ),
    InAppMessagePreset(
        "UL Data Link + HTML Attr",
        "Core rendering",
        TEST_EVENT,
        mapOf(PROPERTY_KEY to "test_msg_datalink_html_attr")
    ),
    // Behaviour
    InAppMessagePreset("Auto Close 5s", "Behaviour", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_autoclose")),
    InAppMessagePreset("Stop SDK", "Behaviour", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_stop_sdk")),
    InAppMessagePreset("Stop And Restart", "Behaviour", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_stop_restart")),
    // GDPR
    InAppMessagePreset("GDPR Dialog", "GDPR", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_gdpr")),
    InAppMessagePreset(
        "GDPR No Consents Customer",
        "GDPR",
        TEST_EVENT,
        mapOf(PROPERTY_KEY to "test_msg_gdpr_no_consents")
    ),
    // A/B testing
    InAppMessagePreset("AB Variant A", "A/B testing", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_ab_variant_a")),
    InAppMessagePreset("AB Variant B", "A/B testing", TEST_EVENT, mapOf(PROPERTY_KEY to "test_msg_ab_variant_b"))
)

val inAppMessagePresetCategories: List<String> = buildList {
    val seen = mutableSetOf<String>()
    inAppMessagePresets.forEach { preset ->
        if (seen.add(preset.category)) {
            add(preset.category)
        }
    }
}
