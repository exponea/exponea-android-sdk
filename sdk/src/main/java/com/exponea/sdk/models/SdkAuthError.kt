package com.exponea.sdk.models

class SdkAuthError(
    val errorCode: SdkAuthErrorCode,
    val customerIds: Map<String, String?>
)

enum class SdkAuthErrorCode {

    TOKEN_ABOUT_TO_EXPIRE,

    TOKEN_EXPIRED,

    TOKEN_REJECTED,

    TOKEN_NOT_PROVIDED
}
