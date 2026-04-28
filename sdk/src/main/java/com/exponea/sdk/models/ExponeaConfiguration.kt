package com.exponea.sdk.models

import android.app.NotificationManager
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.models.Constants.ApplicationId.APP_ID_DEFAULT_VALUE
import com.exponea.sdk.models.Constants.ApplicationId.APP_ID_MAX_LENGTH
import com.exponea.sdk.models.Constants.ApplicationId.APP_ID_VALIDATION_REGEX
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.getId

data class ExponeaConfiguration(
    /**
     * Integration configuration specifying how the SDK connects to Bloomreach platform.
     * Can be either [ProjectConfig] for project-based integration or [StreamConfig] for stream-based integration.
     */
    var integrationConfig: IntegrationConfig = ProjectConfig(projectToken = ""),
    /**
     * Maps event types to additional project configurations for routing events to multiple projects.
     * Only applicable when [integrationConfig] is [ProjectConfig]. Ignored for [StreamConfig].
     */
    var integrationRouteMap: Map<EventType, List<ProjectConfig>> = emptyMap(),
    /** Level of HTTP logging, default value is BODY. */
    var httpLoggingLevel: HttpLoggingLevel = HttpLoggingLevel.BODY,
    /** Maximum retries value to flush data to api. */
    var maxTries: Int = 10,
    /** Timeout session value considered for app usage. */
    var sessionTimeout: Double = Constants.Session.defaultTimeout,
    /** Defines time to live of campaign click event in seconds considered for app usage. */
    var campaignTTL: Double = Constants.Campaign.defaultCampaignTTL,
    /** Flag to control automatic session tracking */
    var automaticSessionTracking: Boolean = Constants.Session.defaultAutomaticTracking,
    /** Flag to control if the App will handle push notifications automatically. */
    var automaticPushNotification: Boolean = Constants.PushNotif.defaultAutomaticListening,
    /** Flag if the SDK can check ([push notification permission status](https://developer.android.com/develop/ui/views/notifications/notification-permission)) and only tracks the push token if the user is authorized to receive push notifications. */
    var requirePushAuthorization: Boolean = Constants.PushNotif.defaultPushAuthorizationRequired,
    /** Icon to be shown in push notifications. */
    var pushIcon: Int? = null,
    /** Accent color of push notification icon and buttons.
     * A color id, not resource id is expected here, e.g. context.resources.getColor(R.color.something)
     */
    var pushAccentColor: Int? = null,
    /** Channel name for push notifications. Only for API level 26+. */
    var pushChannelName: String = "Exponea",
    /** Channel description for push notifications. Only for API level 26+. */
    var pushChannelDescription: String = "Notifications",
    /** Channel ID for push notifications. Only for API level 26+. */
    var pushChannelId: String = "0",
    /** Notification importance for the notification channel. Only for API level 26+. */
    var pushNotificationImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    /** A list of properties to be added to all tracking events */
    var defaultProperties: HashMap<String, Any> = hashMapOf(),
    /** How ofter the token is tracked */
    var tokenTrackFrequency: TokenFrequency = TokenFrequency.ON_TOKEN_CHANGE,

    /** If true, default properties are applied also for 'identifyCustomer' event. */
    var allowDefaultCustomerProperties: Boolean = true,

    /**
     * Enables Customer Token authentication for secure tracking.
     * Only applicable when [integrationConfig] is [ProjectConfig]. Ignored for [StreamConfig].
     */
    var advancedAuthEnabled: Boolean = false,

    /**
     * Automatically load content of In-app content blocks assigned to these Placeholder IDs
     */
    var inAppContentBlockPlaceholdersAutoLoad: List<String> = emptyList(),

    /**
     * Defines margin-top of Image in App Inbox detail screen in dp. Default value (null) will result
     * in `?attr/actionBarSize` defined in application theme.
     * This is useful in case of transparent toolbar theming.
     */
    var appInboxDetailImageInset: Int? = null,

    /**
     * Flag that enables or disables cookies in WebViews. Cookies in WebView could be misused by malware so it is
     * recommended to keep them disabled (default value). According to shared CookieManager in android, this flag
     * could affect all WebView instances used by application. If your application is using WebView and page logic
     * depends on cookies, you may allow them with `true` value.
     */
    var allowWebViewCookies: Boolean = false,

    /**
     * Flag that enables or disables manual session auto-close.
     * It determines whether the SDK automatically tracks `session_end` for sessions that remain open when `Exponea.trackSessionStart()` is called multiple times in manual session tracking mode.
     */
    var manualSessionAutoClose: Boolean = true,

    /**
     * This applicationId defines a unique identifier for the mobile app within the Engagement project.
     * Change this value only if your Engagement project contains and supports multiple mobile apps.
     * This identifier distinguishes between different apps in the same project.
     * Your applicationId value must be the same as the one defined in your Engagement project settings.
     * If your Engagement project supports only one app, skip the applicationId configuration. The SDK will use the default value automatically.
     * Must be in a specific format, see rules:
     *
     * Rules for applicationId:
     * - Starts with one or more lowercase letters or digits
     * - Additional words are separated by single hyphens or dots
     * - No leading or trailing hyphen or dots
     * - No consecutive hyphens or dots
     * - Maximum length is 50 characters
     */
    var applicationId: String = APP_ID_DEFAULT_VALUE
) {

    @Deprecated("Please use projectToken in integrationConfig instead")
    var projectToken: String = ""
        set(value) {
            field = value
            integrationConfig.let {
                if (it is ProjectConfig) {
                    integrationConfig = ProjectConfig(
                        it.baseUrl,
                        value,
                        it.authorization
                    )
                }
            }
        }

    @Deprecated("Please use baseUrl in integrationConfig instead")
    var baseURL: String = Constants.Repository.baseURL
        set(value) {
            field = value
            integrationConfig.let { ic ->
                integrationConfig = when (ic) {
                    is ProjectConfig -> ProjectConfig(value, ic.projectToken, ic.authorization)
                    is StreamConfig -> StreamConfig(value, ic.streamId)
                }
            }
        }

    @Deprecated("Please use authorization in integrationConfig instead")
    var authorization: String? = null
        set(value) {
            field = value
            integrationConfig.let { ic ->
                integrationConfig = when (ic) {
                    is ProjectConfig -> ProjectConfig(ic.baseUrl, ic.projectToken, value)
                    is StreamConfig -> StreamConfig(ic.baseUrl, ic.streamId)
                }
            }
        }

    @Deprecated(
        "Please use integrationRouteMap instead",
        replaceWith = ReplaceWith("integrationRouteMap")
    )
    var projectRouteMap: Map<EventType, List<ExponeaProject>> = emptyMap()
        set(value) {
            field = value
            integrationRouteMap = value.mapValues { entry ->
                entry.value.map { project ->
                    ProjectConfig(
                        baseUrl = project.baseUrl,
                        projectToken = project.projectToken,
                        authorization = project.authorization
                    )
                }
            }
        }

    @Deprecated(
        message = "Please use main constructor",
        replaceWith = ReplaceWith("ExponeaConfiguration(" +
            "integrationConfig, integrationRouteMap, httpLoggingLevel, maxTries, " +
            "sessionTimeout, campaignTTL, automaticSessionTracking, automaticPushNotification, " +
            "requirePushAuthorization, pushIcon, pushAccentColor, pushChannelName, pushChannelDescription, " +
            "pushChannelId, pushNotificationImportance, defaultProperties, tokenTrackFrequency, " +
            "allowDefaultCustomerProperties, advancedAuthEnabled, inAppContentBlockPlaceholdersAutoLoad, " +
            "appInboxDetailImageInset, allowWebViewCookies, manualSessionAutoClose, applicationId)"
        )
    )
    constructor(
        projectToken: String = "",
        projectRouteMap: Map<EventType, List<ExponeaProject>> = emptyMap(),
        authorization: String? = null,
        baseURL: String = Constants.Repository.baseURL,
        httpLoggingLevel: HttpLoggingLevel = HttpLoggingLevel.BODY,
        maxTries: Int = 10,
        sessionTimeout: Double = Constants.Session.defaultTimeout,
        campaignTTL: Double = Constants.Campaign.defaultCampaignTTL,
        automaticSessionTracking: Boolean = Constants.Session.defaultAutomaticTracking,
        automaticPushNotification: Boolean = Constants.PushNotif.defaultAutomaticListening,
        requirePushAuthorization: Boolean = Constants.PushNotif.defaultPushAuthorizationRequired,
        pushIcon: Int? = null,
        pushAccentColor: Int? = null,
        pushChannelName: String = "Exponea",
        pushChannelDescription: String = "Notifications",
        pushChannelId: String = "0",
        pushNotificationImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        defaultProperties: HashMap<String, Any> = hashMapOf(),
        tokenTrackFrequency: TokenFrequency = TokenFrequency.ON_TOKEN_CHANGE,
        allowDefaultCustomerProperties: Boolean = true,
        advancedAuthEnabled: Boolean = false,
        inAppContentBlockPlaceholdersAutoLoad: List<String> = emptyList(),
        appInboxDetailImageInset: Int? = null,
        allowWebViewCookies: Boolean = false,
        manualSessionAutoClose: Boolean = true,
        applicationId: String = APP_ID_DEFAULT_VALUE
    ) : this(
        integrationConfig = ProjectConfig(
            baseUrl = baseURL,
            projectToken = projectToken,
            authorization = authorization
        ),
        integrationRouteMap = projectRouteMap.mapValues { entry ->
            entry.value.map { project ->
                ProjectConfig(
                    baseUrl = project.baseUrl,
                    projectToken = project.projectToken,
                    authorization = project.authorization
                )
            }
        },
        httpLoggingLevel = httpLoggingLevel,
        maxTries = maxTries,
        sessionTimeout = sessionTimeout,
        campaignTTL = campaignTTL,
        automaticSessionTracking = automaticSessionTracking,
        automaticPushNotification = automaticPushNotification,
        requirePushAuthorization = requirePushAuthorization,
        pushIcon = pushIcon,
        pushAccentColor = pushAccentColor,
        pushChannelName = pushChannelName,
        pushChannelDescription = pushChannelDescription,
        pushChannelId = pushChannelId,
        pushNotificationImportance = pushNotificationImportance,
        defaultProperties = defaultProperties,
        tokenTrackFrequency = tokenTrackFrequency,
        allowDefaultCustomerProperties = allowDefaultCustomerProperties,
        advancedAuthEnabled = advancedAuthEnabled,
        inAppContentBlockPlaceholdersAutoLoad = inAppContentBlockPlaceholdersAutoLoad,
        appInboxDetailImageInset = appInboxDetailImageInset,
        allowWebViewCookies = allowWebViewCookies,
        manualSessionAutoClose = manualSessionAutoClose,
        applicationId = applicationId
    )

    companion object {
        const val TOKEN_AUTH_PREFIX = "Token "
        const val BASIC_AUTH_PREFIX = "Basic "
        const val BEARER_AUTH_PREFIX = "Bearer "
    }

    enum class HttpLoggingLevel {
        /** No logs. */
        NONE,
        /** Logs request and response lines. */
        BASIC,
        /** Logs request and response lines and their respective headers. */
        HEADERS,
        /** Logs request and response lines and their respective headers and bodies (if present). */
        BODY
    }

    enum class TokenFrequency {
        /** Tracked on the first launch or if the token changes */
        ON_TOKEN_CHANGE,
        /** Tracked every time the app is launched */
        EVERY_LAUNCH,
        /** Tracked once on days when the user opens the app */
        DAILY
    }

    fun validate() {
        validateIntegrationId(integrationConfig)
        validateBaseUrl(integrationConfig)
        when (val config = integrationConfig) {
            is ProjectConfig -> {
                validateBasicAuthValue(config.authorization)
                validateIntegrationIdsInRouteMap(integrationRouteMap)
            }
            is StreamConfig -> {
                warnIfAdvancedAuthEnabledForStream()
                warnIfRouteMapConfiguredForStream()
            }
        }
        validateApplicationId(applicationId)
    }

    private fun validateIntegrationId(integrationConfig: IntegrationConfig) {
        if (integrationConfig.getId().isBlank()) {
            throw InvalidConfigurationException(
                """
                Provided ${
                    when (integrationConfig) {
                        is ProjectConfig -> "project token"
                        is StreamConfig -> "stream id"
                    }
                } is not valid. Cannot be empty string.
            """.trimIndent()
            )
        }
        val projectTokenAllowedCharacters = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '-'
        if (integrationConfig.getId().any { projectTokenAllowedCharacters.contains(it).not() }) {
            throw InvalidConfigurationException(
                """
                Provided ${
                    when (integrationConfig) {
                        is ProjectConfig -> "project token"
                        is StreamConfig -> "stream id"
                    }
                } is not valid. Only alphanumeric symbols and dashes are allowed.
            """.trimIndent()
            )
        }
    }

    private fun validateIntegrationIdsInRouteMap(integrationRouteMap: Map<EventType, List<ProjectConfig>>) {
        integrationRouteMap.forEach { (type, integrations) ->
            integrations.forEach { integration ->
                try {
                    validateIntegrationId(integration)
                    validateBaseUrl(integration)
                } catch (e: Exception) {
                    throw InvalidConfigurationException(
                        "Integration route mapping for event type $type is not valid. ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun warnIfAdvancedAuthEnabledForStream() {
        if (advancedAuthEnabled) {
            Logger.w(
                this,
                "Advanced authentication is only supported when using ProjectConfig. " +
                "This setting will be ignored for StreamConfig."
            )
        }
    }

    private fun warnIfRouteMapConfiguredForStream() {
        if (integrationRouteMap.isNotEmpty()) {
            Logger.w(
                this,
                "Integration route mapping is only supported when using ProjectConfig. " +
                "This setting will be ignored for StreamConfig."
            )
        }
    }

    private fun validateBaseUrl(integrationConfig: IntegrationConfig) {
        val baseUrl = integrationConfig.baseUrl
        val hasValidScheme = baseUrl.startsWith("http://") || baseUrl.startsWith("https://")
        if (!hasValidScheme) {
            throw InvalidConfigurationException(
                if (baseUrl.isBlank()) {
                    "Provided baseUrl is not valid. Cannot be empty string."
                } else {
                    "Provided baseUrl is not valid. Must start with 'http://' or 'https://' (got '$baseUrl')."
                }
            )
        }
    }

    private fun validateBasicAuthValue(authToken: String?) {
        if (authToken?.startsWith(BASIC_AUTH_PREFIX) == true) {
            throw InvalidConfigurationException("""
                Basic authentication is not supported by mobile SDK for security reasons.
                Use Token authentication instead.
                For more details see https://documentation.bloomreach.com/engagement/reference/technical-information#public-api-access
                """.trimIndent()
            )
        } else if (authToken?.startsWith(TOKEN_AUTH_PREFIX) == false) {
            throw InvalidConfigurationException("""
                Use 'Token <access token>' as authorization for SDK.
                For more details see https://documentation.bloomreach.com/engagement/reference/technical-information#public-api-access
                """.trimIndent()
            )
        }
    }

    private fun validateApplicationId(applicationId: String) {
        when {
            applicationId == APP_ID_DEFAULT_VALUE -> return
            applicationId.length > APP_ID_MAX_LENGTH -> {
                throw InvalidConfigurationException(
                    "The provided applicationId exceeds the maximum length of $APP_ID_MAX_LENGTH characters."
                )
            }
            !Regex(APP_ID_VALIDATION_REGEX).matches(applicationId) -> {
                throw InvalidConfigurationException(
                    "The provided applicationId is not in the correct format."
                )
            }
        }
    }
}
