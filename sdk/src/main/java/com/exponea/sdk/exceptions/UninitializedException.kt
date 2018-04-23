package com.exponea.sdk.exceptions

import com.exponea.sdk.models.Constants

class UninitializedException(message: String = Constants.ErrorMessages.sdkNotConfigured) : Exception()
