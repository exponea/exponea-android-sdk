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
     * Method called when in-app message is shown.
     */
    fun inAppMessageShown(message: InAppMessage, context: Context)

    /**
     * Method called when any error occurs while showing in-app message.
     * In-app message could be NULL if error is not related to in-app message.
     */
    fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context)

    /**
     * Method called when in-app message action is performed.
     */
    fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context)

    /**
     * Method called when in-app message close action is performed.
     * The `interaction` is true if message has been closed by user interaction.
     * The `button` is null if default closing action is performed - user clicked on X button or swiped message away.
     * If user closed message by cancel button (cancel action created for message), the button will contains available
     * information.
     * The `button` is always null if message has not been closed by user interaction.
     */
    fun inAppMessageCloseAction(
        message: InAppMessage,
        button: InAppMessageButton?,
        interaction: Boolean,
        context: Context
    )
}
