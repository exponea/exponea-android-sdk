package com.exponea.sdk.services

import android.content.Context
import android.content.pm.PackageManager
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfiguration.Companion.BEARER_AUTH_PREFIX
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.util.Logger

internal open class ExponeaProjectFactory(
    private val context: Context,
    exponeaConfiguration: ExponeaConfiguration
) {

    private var configuration: ExponeaConfiguration
    private var customAuthProvider: AuthorizationProvider? = null

    init {
        configuration = exponeaConfiguration
        if (configuration.advancedAuthEnabled) {
            customAuthProvider = tryLoadAuthorizationProvider(context)
            if (customAuthProvider == null) {
                Logger.e(this, "Advanced auth has been enabled but provider has not been found")
                throw InvalidConfigurationException("""
                Customer token authorization provider is enabled but cannot be found.
                Please check your configuration against https://github.com/exponea/exponea-android-sdk/blob/develop/Documentation/AUTHORIZATION.md
                """.trimIndent()
                )
            }
        }
    }

    val mainExponeaProject
        get() = ExponeaProject(
            configuration.baseURL,
            configuration.projectToken,
            configuration.authorization
        )

    /**
     * Returns ExponeaProject that:
     *  - contains CustomerID auth token if AuthProvider is registered
     *  - contains Api auth token otherwise
     *
     *  !!! Access it in background thread due to possibility of fetching of Customer Token value
     */
    val mutualExponeaProject: ExponeaProject
        get() {
            val authProvider = customAuthProvider
            if (authProvider == null) {
                return mainExponeaProject
            }
            var authToken = authProvider.getAuthorizationToken()
            if (authToken?.isNotBlank() == true && !authToken.startsWith(BEARER_AUTH_PREFIX)) {
                authToken = BEARER_AUTH_PREFIX + authToken
            }
            return ExponeaProject(
                configuration.baseURL,
                configuration.projectToken,
                authToken
            )
        }

    fun reset(newConfiguration: ExponeaConfiguration) {
        configuration = newConfiguration
        customAuthProvider = tryLoadAuthorizationProvider(context)
    }

    internal fun tryLoadAuthorizationProvider(context: Context): AuthorizationProvider? {
        val customProviderClassname = readAuthorizationProviderName(context)
        if (customProviderClassname == null) {
            // valid exit, no ExponeaAuthProvider in metadata
            Logger.i(this, "CustomerID auth provider is not registered")
            return null
        }
        val customProviderClass = try {
            Class.forName(customProviderClassname)
        } catch (e: ClassNotFoundException) {
            Logger.e(this, "Registered $customProviderClassname class has not been found", e)
            throw InvalidConfigurationException("""
                Customer token authorization provider is registered but cannot be found.
                Please check your configuration against https://github.com/exponea/exponea-android-sdk/blob/develop/Documentation/AUTHORIZATION.md
                """.trimIndent()
            )
        }
        val customProviderInstance = customProviderClass.newInstance()
        if (customProviderInstance is AuthorizationProvider) {
            return customProviderInstance
        }
        Logger.e(this, "Registered $customProviderClassname class has to implement" +
            "${AuthorizationProvider::class.qualifiedName}")
        throw InvalidConfigurationException("""
                Customer token authorization provider is registered but mismatches implementation requirements.
                Please check your configuration against https://github.com/exponea/exponea-android-sdk/blob/develop/Documentation/AUTHORIZATION.md
                """.trimIndent()
        )
    }

    internal open fun readAuthorizationProviderName(context: Context): String? {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        if (appInfo.metaData == null) {
            // valid exit, no metadata
            return null
        }
        return appInfo.metaData["ExponeaAuthProvider"] as String?
    }
}
