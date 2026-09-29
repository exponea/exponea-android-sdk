package com.exponea.example.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exponea.example.R
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.model.Constants
import com.exponea.example.tracking.TrackPageView
import com.exponea.example.ui.common.CollectUiMessages
import com.exponea.example.ui.components.BrButton
import com.exponea.example.ui.components.BrButtonStyle
import com.exponea.example.ui.components.ResultPanel
import com.exponea.example.ui.components.SectionCard
import com.exponea.example.ui.dialogs.IdentifyCustomerDialog
import com.exponea.example.ui.dialogs.TrackCustomEventDialog
import com.exponea.example.ui.theme.BloomreachTheme
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey100
import com.exponea.sdk.models.PropertiesList

@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = viewModel()
) {
    TrackPageView(Constants.ScreenNames.TRACKING)
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectUiMessages(viewModel.messages)

    TrackingScreenContent(
        modifier = modifier,
        state = state,
        onIdentifyClick = viewModel::onIdentifyClick,
        onSetAuthTokenClick = viewModel::onSetAuthTokenClick,
        onCustomEventClick = viewModel::onCustomEventClick,
        onTrackPaymentClick = viewModel::onTrackPaymentClick,
        onTrackPageViewClick = viewModel::onTrackPageViewClick,
        onRequestPushPermissionClick = viewModel::onRequestPushPermissionClick,
        onTrackPushTokenClick = viewModel::onTrackPushTokenClick,
        onTrackPushDeliveredClick = viewModel::onTrackPushDeliveredClick,
        onTrackPushClickedClick = viewModel::onTrackPushClickedClick,
        onIdentifyDialogDismiss = viewModel::onIdentifyDialogDismiss,
        onIdentifyCustomer = viewModel::onIdentifyCustomer,
        onCustomEventDialogDismiss = viewModel::onCustomEventDialogDismiss,
        onTrackCustomEvent = viewModel::onTrackCustomEvent,
        onCopyCookieIdClick = viewModel::onCopyCookieIdClick
    )
}

@Composable
private fun TrackingScreenContent(
    state: TrackingUiState,
    onIdentifyClick: () -> Unit,
    onSetAuthTokenClick: () -> Unit,
    onCustomEventClick: () -> Unit,
    onTrackPaymentClick: () -> Unit,
    onTrackPageViewClick: () -> Unit,
    onRequestPushPermissionClick: () -> Unit,
    onTrackPushTokenClick: () -> Unit,
    onTrackPushDeliveredClick: () -> Unit,
    onTrackPushClickedClick: () -> Unit,
    onIdentifyDialogDismiss: () -> Unit,
    onIdentifyCustomer: (HashMap<String, Any>, Boolean) -> Unit,
    onCustomEventDialogDismiss: () -> Unit,
    onTrackCustomEvent: (String, PropertiesList) -> Unit,
    onCopyCookieIdClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.showIdentifyDialog) {
        IdentifyCustomerDialog(
            onDismiss = onIdentifyDialogDismiss,
            onIdentify = onIdentifyCustomer
        )
    }
    if (state.showCustomEventDialog) {
        TrackCustomEventDialog(
            onDismiss = onCustomEventDialogDismiss,
            onTrack = onTrackCustomEvent
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrGrey100)
            .verticalScroll(rememberScrollState())
    ) {
        SectionCard {
            Text(
                "Cookie ID",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.automationId(AutomationIds.COOKIE_ID)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResultPanel(
                    text = state.cookieId,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCopyCookieIdClick,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(40.dp)
                        .automationId(AutomationIds.COPY_COOKIE_ID)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = AutomationIds.COPY_COOKIE_ID,
                        tint = BrBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        SectionCard {
            Text("Customer", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BrButton(
                text = "Identify Customer",
                onClick = onIdentifyClick,
                automationId = AutomationIds.IDENTIFY_CUSTOMER
            )
            if (state.showStreamAuthActions) {
                Spacer(Modifier.height(6.dp))
                BrButton(
                    text = "Set Auth Token",
                    onClick = onSetAuthTokenClick,
                    style = BrButtonStyle.Outlined,
                    automationId = AutomationIds.SET_AUTH_TOKEN
                )
            }
        }
        SectionCard {
            Text("Events", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BrButton(
                text = "Track Custom Event",
                onClick = onCustomEventClick,
                style = BrButtonStyle.Outlined,
                automationId = AutomationIds.TRACK_CUSTOM_EVENT
            )
            Spacer(Modifier.height(6.dp))
            BrButton(
                text = "Track Payment",
                onClick = onTrackPaymentClick,
                style = BrButtonStyle.Outlined,
                automationId = AutomationIds.TRACK_PAYMENT
            )
            Spacer(Modifier.height(6.dp))
            BrButton(
                text = "Track Page View",
                onClick = onTrackPageViewClick,
                style = BrButtonStyle.Outlined,
                automationId = AutomationIds.TRACK_PAGE_VIEW
            )
        }
        SectionCard {
            Text("Push", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BrButton(
                text = "Request Push Permission",
                onClick = onRequestPushPermissionClick,
                automationId = AutomationIds.REQUEST_PUSH
            )
            Spacer(Modifier.height(6.dp))
            BrButton(
                text = "Track FCM Token",
                onClick = onTrackPushTokenClick,
                style = BrButtonStyle.Outlined,
                automationId = AutomationIds.TRACK_FCM_TOKEN
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrButton(
                    text = "Track Delivered",
                    onClick = onTrackPushDeliveredClick,
                    style = BrButtonStyle.Outlined,
                    modifier = Modifier.weight(1f),
                    automationId = AutomationIds.TRACK_PUSH_DELIVERED
                )
                BrButton(
                    text = "Track Clicked",
                    onClick = onTrackPushClickedClick,
                    style = BrButtonStyle.Outlined,
                    modifier = Modifier.weight(1f),
                    automationId = AutomationIds.TRACK_PUSH_CLICKED
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Project mode")
@Composable
private fun TrackingScreenPreview() {
    BloomreachTheme {
        TrackingScreenContent(
            state = TrackingUiState(
                cookieId = "abc123-def456-7890",
                showStreamAuthActions = false
            ),
            onIdentifyClick = {},
            onSetAuthTokenClick = {},
            onCustomEventClick = {},
            onTrackPaymentClick = {},
            onTrackPageViewClick = {},
            onRequestPushPermissionClick = {},
            onTrackPushTokenClick = {},
            onTrackPushDeliveredClick = {},
            onTrackPushClickedClick = {},
            onIdentifyDialogDismiss = {},
            onIdentifyCustomer = { _, _ -> },
            onCustomEventDialogDismiss = {},
            onTrackCustomEvent = { _, _ -> },
            onCopyCookieIdClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Stream mode")
@Composable
private fun TrackingScreenStreamPreview() {
    BloomreachTheme {
        TrackingScreenContent(
            state = TrackingUiState(
                cookieId = "stream-cookie-id",
                showStreamAuthActions = true
            ),
            onIdentifyClick = {},
            onSetAuthTokenClick = {},
            onCustomEventClick = {},
            onTrackPaymentClick = {},
            onTrackPageViewClick = {},
            onRequestPushPermissionClick = {},
            onTrackPushTokenClick = {},
            onTrackPushDeliveredClick = {},
            onTrackPushClickedClick = {},
            onIdentifyDialogDismiss = {},
            onIdentifyCustomer = { _, _ -> },
            onCustomEventDialogDismiss = {},
            onTrackCustomEvent = { _, _ -> },
            onCopyCookieIdClick = {}
        )
    }
}
