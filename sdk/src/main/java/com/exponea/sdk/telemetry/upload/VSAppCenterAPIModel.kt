package com.exponea.sdk.telemetry.upload

/**
 * This file contains models for Visual Studio App Center Logs API
 * It's only partial model, all required properties and only useful optional ones
 * https://docs.microsoft.com/en-us/appcenter/diagnostics/upload-custom-crashes#upload-a-crash-report
 * https://in.appcenter.ms/preview/swagger.json
 */
internal data class VSAppCenterAPIRequestData(
    val logs: List<VSAppCenterAPIErrorReport>
)

internal data class VSAppCenterAPIErrorReport(
    val id: String,
    val type: String,
    val fatal: Boolean,
    val userId: String? = null,
    val device: VSAppCenterAPIDevice,
    val exception: VSAppCenterAPIException,
    val timestamp: String,
    val appLaunchTimestamp: String,
    val processId: Int = 0, // this is required by swagger, but we have no reasonable value for it
    val processName: String = "" // this is required by swagger, but we have no reasonable value for it
)

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