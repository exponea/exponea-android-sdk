package com.exponea.sdk.telemetry.upload

/**
 * This file contains models for Visual Studio App Center Logs API
 * It's only partial model, all required properties and only useful optional ones
 * https://docs.microsoft.com/en-us/appcenter/diagnostics/upload-custom-crashes#upload-a-crash-report
 * https://in.appcenter.ms/preview/swagger.json
 */
internal data class VSAppCenterAPIRequestData(
    val logs: List<VSAppCenterAPILog>
)

internal interface VSAppCenterAPILog {
    val id: String
    val sid: String
    val type: String
    val userId: String?
    val device: VSAppCenterAPIDevice
    val timestamp: String
}

internal data class VSAppCenterAPIStartSession(
    override val id: String,
    override val sid: String,
    override val userId: String? = null,
    override val device: VSAppCenterAPIDevice,
    override val timestamp: String
) : VSAppCenterAPILog {
    override val type: String = "startSession"
}

internal data class VSAppCenterAPIErrorLog(
    override val id: String,
    override val sid: String,
    override val userId: String? = null,
    override val device: VSAppCenterAPIDevice,
    override val timestamp: String,
    val fatal: Boolean,
    val exception: VSAppCenterAPIException,
    val appLaunchTimestamp: String,
    val processId: Int = 0, // this is required by swagger, but we have no reasonable value for it
    val processName: String = "" // this is required by swagger, but we have no reasonable value for it
) : VSAppCenterAPILog {
    override val type: String = "managedError"
}

internal data class VSAppCenterAPIEventLog(
    override val id: String,
    override val sid: String,
    override val userId: String? = null,
    override val device: VSAppCenterAPIDevice,
    override val timestamp: String,
    val name: String,
    val properties: Map<String, String>
) : VSAppCenterAPILog {
    override val type: String = "event"
}

internal data class VSAppCenterAPIDevice(
    val appNamespace: String,
    val appVersion: String,
    val appBuild: String,
    val sdkName: String,
    val sdkVersion: String,
    val osName: String,
    val osVersion: String,
    val model: String?,
    val locale: String
)

internal data class VSAppCenterAPIException(
    val type: String,
    val message: String? = null,
    val frames: List<VSAppCenterAPIExceptionFrame>? = null,
    val stackTrace: String? = null,
    val innerExceptions: List<VSAppCenterAPIException>? = null
)

internal data class VSAppCenterAPIExceptionFrame(
    val className: String,
    val methodName: String,
    val fileName: String,
    val lineNumber: Int
)