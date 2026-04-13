package com.exponea.sdk.services

import android.content.Context
import android.content.pm.PackageManager
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.util.Logger

internal class CustomAuthProviderFactory(
    private val context: Context,
    exponeaConfiguration: ExponeaConfiguration
) {
    private var customAuthProvider: AuthorizationProvider? = null

    init {
        reset(exponeaConfiguration)
    }

    internal fun getAuthorizationProvider(): AuthorizationProvider? {
        return customAuthProvider
    }

    fun reset(newConfiguration: ExponeaConfiguration) {
        if (newConfiguration.integrationConfig is ProjectConfig && newConfiguration.advancedAuthEnabled) {
            customAuthProvider = tryLoadAuthorizationProvider(context)
            if (customAuthProvider == null) {
                Logger.e(this, "Advanced auth has been enabled but provider has not been found")
                throw InvalidConfigurationException("""
                Customer token authorization provider is enabled but cannot be found.
                Please check your configuration against https://github.com/exponea/exponea-android-sdk/blob/main/Documentation/authorization.md
                """.trimIndent()
                )
            }
        }
    }

    private fun tryLoadAuthorizationProvider(context: Context): AuthorizationProvider? {
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
                Please check your configuration against https://github.com/exponea/exponea-android-sdk/blob/main/Documentation/authorization.md
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
                Please check your configuration against https://github.com/exponea/exponea-android-sdk/blob/main/Documentation/authorization.md
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
