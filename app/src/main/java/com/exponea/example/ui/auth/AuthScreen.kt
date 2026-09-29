package com.exponea.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exponea.example.R
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.config.IntegrationMode
import com.exponea.example.ui.common.CollectUiMessages
import com.exponea.example.ui.components.BrButton
import com.exponea.example.ui.components.BrButtonStyle
import com.exponea.example.ui.theme.BloomreachTheme
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey200
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrWhite

@Composable
fun AuthScreen(
    onInitSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectUiMessages(viewModel.messages)

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                AuthUiEvent.InitSuccess -> onInitSuccess()
            }
        }
    }

    AuthScreenContent(
        state = state,
        onModeChange = viewModel::onModeChange,
        onProjectTokenChange = viewModel::onProjectTokenChange,
        onAuthorizationChange = viewModel::onAuthorizationChange,
        onAdvancedAuthKeyChange = viewModel::onAdvancedAuthKeyChange,
        onStreamIdChange = viewModel::onStreamIdChange,
        onJwtKeyIdChange = viewModel::onJwtKeyIdChange,
        onJwtSecretChange = viewModel::onJwtSecretChange,
        onRegisteredIdChange = viewModel::onRegisteredIdChange,
        onApiUrlChange = viewModel::onApiUrlChange,
        onApplicationIdChange = viewModel::onApplicationIdChange,
        onStartClick = viewModel::onStartClick,
        onClearLocalDataClick = viewModel::onClearLocalDataClick
    )
}

@Composable
private fun AuthScreenContent(
    state: AuthUiState,
    onModeChange: (IntegrationMode) -> Unit,
    onProjectTokenChange: (String) -> Unit,
    onAuthorizationChange: (String) -> Unit,
    onAdvancedAuthKeyChange: (String) -> Unit,
    onStreamIdChange: (String) -> Unit,
    onJwtKeyIdChange: (String) -> Unit,
    onJwtSecretChange: (String) -> Unit,
    onRegisteredIdChange: (String) -> Unit,
    onApiUrlChange: (String) -> Unit,
    onApplicationIdChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onClearLocalDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_br_logo),
            contentDescription = "Bloomreach",
            modifier = Modifier
                .width(220.dp)
                .padding(top = 40.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "SDK Configuration",
            modifier = Modifier
                .fillMaxWidth()
                .automationId(AutomationIds.AUTH_TITLE),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BrBlack,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        AuthModeToggle(
            selectedMode = state.mode,
            onModeChange = onModeChange
        )
        Spacer(Modifier.height(16.dp))

        if (state.mode == IntegrationMode.PROJECT) {
            AuthField(
                "Project token",
                state.projectToken,
                onProjectTokenChange,
                AutomationIds.AUTH_PROJECT_TOKEN
            )
            AuthField(
                "Authorization token",
                state.authorizationToken,
                onAuthorizationChange,
                AutomationIds.AUTH_AUTHORIZATION_TOKEN
            )
            AuthField(
                "Advanced auth key (optional)",
                state.advancedAuthKey,
                onAdvancedAuthKeyChange,
                AutomationIds.AUTH_ADVANCED_AUTH
            )
        } else {
            AuthField("Stream ID", state.streamId, onStreamIdChange, AutomationIds.AUTH_STREAM_ID)
            AuthField("JWT Key ID", state.jwtKeyId, onJwtKeyIdChange, AutomationIds.AUTH_JWT_KEY_ID)
            AuthField(
                "JWT Secret",
                state.jwtSecret,
                onJwtSecretChange,
                AutomationIds.AUTH_JWT_SECRET,
                isPassword = true
            )
        }

        AuthField(
            "Registered customer ID",
            state.registeredId,
            onRegisteredIdChange,
            AutomationIds.AUTH_REGISTERED_ID
        )
        AuthField("API URL", state.apiUrl, onApiUrlChange, AutomationIds.AUTH_API_URL)
        AuthField(
            "Application ID (optional)",
            state.applicationId,
            onApplicationIdChange,
            AutomationIds.AUTH_APPLICATION_ID
        )

        Spacer(Modifier.height(16.dp))
        BrButton(
            text = "Start",
            onClick = onStartClick,
            modifier = Modifier.automationId(AutomationIds.AUTH_START)
        )
        Spacer(Modifier.height(6.dp))
        BrButton(
            text = "Clear local data",
            onClick = onClearLocalDataClick,
            style = BrButtonStyle.Outlined,
            modifier = Modifier.automationId(AutomationIds.AUTH_CLEAR_LOCAL_DATA)
        )
    }
}

@Composable
private fun AuthModeToggle(
    selectedMode: IntegrationMode,
    onModeChange: (IntegrationMode) -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, BrBlack, shape)
    ) {
        AuthModeToggleOption(
            label = "Project",
            selected = selectedMode == IntegrationMode.PROJECT,
            onClick = { onModeChange(IntegrationMode.PROJECT) },
            modifier = Modifier
                .weight(1f)
                .automationId(AutomationIds.AUTH_MODE_PROJECT)
        )
        AuthModeToggleOption(
            label = "Stream",
            selected = selectedMode == IntegrationMode.STREAM,
            onClick = { onModeChange(IntegrationMode.STREAM) },
            modifier = Modifier
                .weight(1f)
                .automationId(AutomationIds.AUTH_MODE_STREAM)
        )
    }
}

@Composable
private fun AuthModeToggleOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (selected) BrGrey200 else BrWhite)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = BrBlack
        )
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    automationId: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .automationId(automationId),
        singleLine = !isPassword,
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrBlack,
            unfocusedBorderColor = BrBlack,
            focusedLabelColor = BrGrey600,
            unfocusedLabelColor = BrGrey600,
            cursorColor = BrBlack,
            focusedTextColor = BrBlack,
            unfocusedTextColor = BrBlack,
            focusedContainerColor = BrWhite,
            unfocusedContainerColor = BrWhite
        ),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }
    )
}

@Preview(showBackground = true, name = "Project mode")
@Composable
private fun AuthScreenProjectPreview() {
    BloomreachTheme {
        AuthScreenContent(
            state = AuthUiState(
                mode = IntegrationMode.PROJECT,
                projectToken = "project-token",
                authorizationToken = "auth-token",
                registeredId = "customer-123",
                apiUrl = "https://api.exponea.com"
            ),
            onModeChange = {},
            onProjectTokenChange = {},
            onAuthorizationChange = {},
            onAdvancedAuthKeyChange = {},
            onStreamIdChange = {},
            onJwtKeyIdChange = {},
            onJwtSecretChange = {},
            onRegisteredIdChange = {},
            onApiUrlChange = {},
            onApplicationIdChange = {},
            onStartClick = {},
            onClearLocalDataClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Stream mode")
@Composable
private fun AuthScreenStreamPreview() {
    BloomreachTheme {
        AuthScreenContent(
            state = AuthUiState(
                mode = IntegrationMode.STREAM,
                streamId = "stream-id",
                jwtKeyId = "jwt-key-id",
                jwtSecret = "secret",
                registeredId = "customer-123",
                apiUrl = "https://api.exponea.com"
            ),
            onModeChange = {},
            onProjectTokenChange = {},
            onAuthorizationChange = {},
            onAdvancedAuthKeyChange = {},
            onStreamIdChange = {},
            onJwtKeyIdChange = {},
            onJwtSecretChange = {},
            onRegisteredIdChange = {},
            onApiUrlChange = {},
            onApplicationIdChange = {},
            onStartClick = {},
            onClearLocalDataClick = {}
        )
    }
}
