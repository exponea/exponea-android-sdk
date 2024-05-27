package com.exponea.sdk.models

import android.content.Context

interface InAppMessageCallback {
    /**
     * If overrideDefaultBehavior is set to true, default in-app action will not be performed ( e.g. deep link )
     **/
    val overrideDefaultBehavior: Boolean
    /**
     * If trackActions is set to false, click and close in-app events will not be tracked automatically
     **/
    var trackActions: Boolean

    /**
     * Method called when in-app message action is performed.
     * On in-app click, the button contains button text and button URL and the interaction is true
     * On in-app close, the button is null and the interaction is true if user closes message manually.
     */
    fun inAppMessageAction(message: InAppMessage, button: InAppMessageButton?, interaction: Boolean, context: Context)

    /**
     * Method called when in-app message is shown.
     */
    fun inAppMessageShown(message: InAppMessage, context: Context)

    /**
     * Method called when any error occurs while showing in-app message.
     * In-app message could be NULL if error is not related to in-app message.
     */
    fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context)
}
