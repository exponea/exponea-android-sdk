package com.exponea.example.ui.anonymize

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.model.Constants
import com.exponea.example.tracking.TrackPageView
import com.exponea.example.ui.auth.AuthActivity
import com.exponea.example.ui.components.BrButton
import com.exponea.example.ui.components.BrButtonStyle
import com.exponea.example.ui.components.SectionCard
import com.exponea.example.ui.theme.BloomreachTheme
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey100
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrWhite

@Composable
fun AnonymizeScreen(
    modifier: Modifier = Modifier,
    viewModel: AnonymizeViewModel = viewModel()
) {
    TrackPageView(Constants.ScreenNames.ANONYMIZE)
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                AnonymizeUiEvent.NavigateToAuth -> {
                    context.startActivity(Intent(context, AuthActivity::class.java))
                    (context as? android.app.Activity)?.finish()
                }
            }
        }
    }

    AnonymizeScreenContent(
        modifier = modifier,
        state = state,
        onAnonymizeClick = viewModel::onAnonymizeClick,
        onStopIntegrationClick = viewModel::onStopIntegrationClick,
        onDialogConfirm = viewModel::onDialogConfirm,
        onDialogContinue = viewModel::onDialogContinue,
        onDialogCancel = viewModel::onDialogCancel
    )
}

@Composable
private fun AnonymizeScreenContent(
    state: AnonymizeUiState,
    onAnonymizeClick: () -> Unit,
    onStopIntegrationClick: () -> Unit,
    onDialogConfirm: () -> Unit,
    onDialogContinue: () -> Unit,
    onDialogCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.dialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = onDialogCancel,
            containerColor = BrWhite,
            titleContentColor = BrBlack,
            textContentColor = BrGrey600,
            title = {
                if (dialog.type == AnonymizeDialogType.STOP_INTEGRATION) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dialog.title, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = onDialogCancel,
                            modifier = Modifier
                                .size(32.dp)
                                .automationId(AutomationIds.STOP_DIALOG_CLOSE)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = AutomationIds.STOP_DIALOG_CLOSE,
                                tint = BrBlack
                            )
                        }
                    }
                } else {
                    Text(
                        dialog.title,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.automationId(AutomationIds.ANONYMIZED_TITLE)
                    )
                }
            },
            text = { Text(dialog.message) },
            confirmButton = {
                TextButton(
                    onClick = onDialogConfirm,
                    modifier = Modifier.automationId(
                        when (dialog.type) {
                            AnonymizeDialogType.STOP_INTEGRATION -> AutomationIds.STOP_BACK_TO_AUTH
                            AnonymizeDialogType.ANONYMIZED -> AutomationIds.ANONYMIZED_OK
                        }
                    )
                ) {
                    Text(dialog.confirmLabel, fontWeight = FontWeight.Bold, color = BrBlack)
                }
            },
            dismissButton = dialog.dismissLabel?.let { label ->
                {
                    TextButton(
                        onClick = onDialogContinue,
                        modifier = Modifier.automationId(AutomationIds.STOP_CONTINUE)
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = BrBlack)
                    }
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrGrey100)
    ) {
        SectionCard {
            Text("Anonymize current customer", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Clears local customer data and starts a new anonymous session.",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(16.dp))
            BrButton(
                text = "Anonymize",
                onClick = onAnonymizeClick,
                automationId = AutomationIds.ANONYMIZE
            )
            Spacer(Modifier.height(8.dp))
            BrButton(
                text = "Stop Integration",
                onClick = onStopIntegrationClick,
                style = BrButtonStyle.Danger,
                automationId = AutomationIds.STOP_INTEGRATION
            )
        }
    }
}

@Preview(showBackground = true, name = "Default")
@Composable
private fun AnonymizeScreenPreview() {
    BloomreachTheme {
        AnonymizeScreenContent(
            state = AnonymizeUiState(),
            onAnonymizeClick = {},
            onStopIntegrationClick = {},
            onDialogConfirm = {},
            onDialogContinue = {},
            onDialogCancel = {}
        )
    }
}

@Preview(showBackground = true, name = "Stop integration dialog")
@Composable
private fun AnonymizeScreenStopDialogPreview() {
    BloomreachTheme {
        AnonymizeScreenContent(
            state = AnonymizeUiState(
                dialog = AnonymizeDialogState(
                    type = AnonymizeDialogType.STOP_INTEGRATION,
                    title = "Stop Integration",
                    message = "Do you want to go back to authentication or continue without integration?",
                    confirmLabel = "Back to Auth",
                    dismissLabel = "Continue"
                )
            ),
            onAnonymizeClick = {},
            onStopIntegrationClick = {},
            onDialogConfirm = {},
            onDialogContinue = {},
            onDialogCancel = {}
        )
    }
}
