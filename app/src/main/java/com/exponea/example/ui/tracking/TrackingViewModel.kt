package com.exponea.example.ui.tracking

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exponea.example.App
import com.exponea.example.managers.CustomerTokenStorage
import com.exponea.example.managers.LocalJwtTokenGenerator
import com.exponea.example.model.Constants
import com.exponea.example.model.SdkSetupState
import com.exponea.example.push.TokenTracker
import com.exponea.example.status.SdkLifecycleState
import com.exponea.example.status.SdkStatusStore
import com.exponea.example.tracking.trackPageViewEvent
import com.exponea.example.ui.common.UiMessage
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIdentity
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PurchasedItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackingUiState(
    val cookieId: String = "<not initialized>",
    val showStreamAuthActions: Boolean = false,
    val showIdentifyDialog: Boolean = false,
    val showCustomEventDialog: Boolean = false
)

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        TrackingUiState(
            cookieId = Exponea.customerCookie ?: "<not initialized>",
            showStreamAuthActions = SdkSetupState.isStreamConfig
        )
    )
    val state: StateFlow<TrackingUiState> = _state.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            SdkStatusStore.state.collect {
                refreshCookieId()
            }
        }
    }

    fun refreshCookieId() {
        _state.update { it.copy(cookieId = Exponea.customerCookie ?: "<not initialized>") }
    }

    fun onCopyCookieIdClick() {
        val cookieId = _state.value.cookieId
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Cookie ID", cookieId))
        sendMessage("Cookie ID copied")
    }

    fun onIdentifyClick() {
        _state.update { it.copy(showIdentifyDialog = true) }
    }

    fun onIdentifyDialogDismiss() {
        _state.update { it.copy(showIdentifyDialog = false) }
    }

    fun onIdentifyCustomer(properties: HashMap<String, Any>, withAuthToken: Boolean) {
        val props = HashMap(properties)
        val registeredIdUpdate = props.remove(Constants.CUSTOMER_ID_REGISTERED) as? String
        if (registeredIdUpdate != null) {
            App.instance.registeredIdManager.registeredID = registeredIdUpdate
        }
        val customerIds = hashMapOf(
            Constants.CUSTOMER_ID_REGISTERED to App.instance.registeredIdManager.registeredID
        )
        CustomerTokenStorage.INSTANCE.configure(customerIds = customerIds)
        Exponea.identifyCustomer(
            customerIdentity = CustomerIdentity(
                customerIds = customerIds,
                sdkAuthToken = if (withAuthToken) {
                    LocalJwtTokenGenerator.INSTANCE.generateToken(customerIds)
                } else {
                    null
                }
            ),
            properties = props
        )
        SdkSetupState.isCustomerIdentified = true
        SdkStatusStore.set(SdkLifecycleState.IDENTIFIED)
        refreshCookieId()
        sendMessage("Customer identified")
        _state.update { it.copy(showIdentifyDialog = false) }
    }

    fun onSetAuthTokenClick() {
        if (!LocalJwtTokenGenerator.INSTANCE.isConfigured()) {
            sendMessage("JWT Key ID and Secret must be provided during SDK configuration.", long = true)
            return
        }
        if (!SdkSetupState.isCustomerIdentified) {
            sendMessage("Customer must be identified first.", long = true)
            return
        }
        val customerIds = mapOf(
            Constants.CUSTOMER_ID_REGISTERED to App.instance.registeredIdManager.registeredID
        )
        val token = LocalJwtTokenGenerator.INSTANCE.generateToken(customerIds)
        if (token != null) {
            Exponea.setSdkAuthToken(token)
            sendMessage("Auth token set.")
        } else {
            sendMessage("Token generation failed.")
        }
    }

    fun onCustomEventClick() {
        _state.update { it.copy(showCustomEventDialog = true) }
    }

    fun onCustomEventDialogDismiss() {
        _state.update { it.copy(showCustomEventDialog = false) }
    }

    fun onTrackCustomEvent(eventName: String, properties: PropertiesList) {
        Exponea.trackEvent(eventType = eventName, properties = properties)
        sendMessage("Event tracked")
        _state.update { it.copy(showCustomEventDialog = false) }
    }

    fun onTrackPaymentClick() {
        Exponea.trackPaymentEvent(
            purchasedItem = PurchasedItem(
                value = 2011.1,
                currency = "USD",
                paymentSystem = "System",
                productId = "1",
                productTitle = "Item #1"
            )
        )
        sendMessage("Payment tracked")
    }

    fun onTrackPageViewClick() {
        trackPageViewEvent(Constants.ScreenNames.TRACKING)
        sendMessage("Page view tracked: ${Constants.ScreenNames.TRACKING}")
    }

    fun onRequestPushPermissionClick() {
        Exponea.requestPushAuthorization(getApplication()) { granted ->
            Log.i("TrackingViewModel", "Push notifications are allowed: $granted")
            sendMessage("Push permission: $granted")
        }
    }

    fun onTrackPushTokenClick() {
        TokenTracker().trackToken(getApplication())
        sendMessage("Tracking push token…")
    }

    fun onTrackPushDeliveredClick() {
        Exponea.trackDeliveredPush(data = NotificationData(hashMapOf("campaign_id" to "id")))
        sendMessage("Push delivered tracked")
    }

    fun onTrackPushClickedClick() {
        Exponea.trackClickedPush(NotificationData(hashMapOf("campaign_id" to "id")))
        sendMessage("Push clicked tracked")
    }

    private fun sendMessage(text: String, long: Boolean = false) {
        viewModelScope.launch {
            _messages.send(UiMessage(text, long))
        }
    }
}
