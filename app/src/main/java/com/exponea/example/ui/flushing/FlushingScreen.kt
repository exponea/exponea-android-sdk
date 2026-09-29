package com.exponea.example.ui.flushing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.model.Constants
import com.exponea.example.tracking.TrackPageView
import com.exponea.example.ui.common.CollectUiMessages
import com.exponea.example.ui.components.BrButton
import com.exponea.example.ui.components.BrButtonStyle
import com.exponea.example.ui.components.ResultPanel
import com.exponea.example.ui.components.SectionCard
import com.exponea.example.ui.theme.BloomreachTheme
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey100
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrWhite
import com.exponea.example.ui.theme.BrYellow
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.util.Logger

@Composable
fun FlushingScreen(
    modifier: Modifier = Modifier,
    viewModel: FlushingViewModel = viewModel()
) {
    TrackPageView(Constants.ScreenNames.FLUSHING)
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectUiMessages(viewModel.messages)

    FlushingScreenContent(
        modifier = modifier,
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onFlushDialogDismiss = viewModel::onFlushDialogDismiss,
        onGetFlushModeClick = viewModel::onGetFlushModeClick,
        onSetFlushModeClick = viewModel::onSetFlushModeClick,
        onGetFlushPeriodClick = viewModel::onGetFlushPeriodClick,
        onSetFlushPeriodClick = viewModel::onSetFlushPeriodClick,
        onFlushDataClick = viewModel::onFlushDataClick,
        onGetLogLevelClick = viewModel::onGetLogLevelClick,
        onSetLogLevelClick = viewModel::onSetLogLevelClick
    )
}

@Composable
private fun FlushingScreenContent(
    state: FlushingUiState,
    onTabSelected: (FlushingTab) -> Unit,
    onFlushDialogDismiss: () -> Unit,
    onGetFlushModeClick: () -> Unit,
    onSetFlushModeClick: (FlushMode) -> Unit,
    onGetFlushPeriodClick: () -> Unit,
    onSetFlushPeriodClick: (Long) -> Unit,
    onFlushDataClick: () -> Unit,
    onGetLogLevelClick: () -> Unit,
    onSetLogLevelClick: (Logger.Level) -> Unit,
    modifier: Modifier = Modifier
) {
    state.flushDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = onFlushDialogDismiss,
            containerColor = BrWhite,
            titleContentColor = BrBlack,
            textContentColor = BrGrey600,
            title = {
                Text(
                    dialog.title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.automationId(AutomationIds.FLUSH_RESULT_TITLE)
                )
            },
            text = dialog.message?.let { message ->
                { Text(message) }
            },
            confirmButton = {
                TextButton(
                    onClick = onFlushDialogDismiss,
                    modifier = Modifier.automationId(AutomationIds.FLUSH_RESULT_OK)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = BrBlack)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrGrey100)
            .verticalScroll(rememberScrollState())
    ) {
        if (state.isFlushing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
        }
        FlushingTabBar(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected
        )
        when (state.selectedTab) {
            FlushingTab.FLUSH_MODE -> FlushModeSection(
                state = state,
                onGetFlushModeClick = onGetFlushModeClick,
                onSetFlushModeClick = onSetFlushModeClick
            )
            FlushingTab.FLUSH_PERIOD -> FlushPeriodSection(
                state = state,
                onGetFlushPeriodClick = onGetFlushPeriodClick,
                onSetFlushPeriodClick = onSetFlushPeriodClick,
                onFlushDataClick = onFlushDataClick
            )
            FlushingTab.LOG_LEVEL -> LogLevelSection(
                state = state,
                onGetLogLevelClick = onGetLogLevelClick,
                onSetLogLevelClick = onSetLogLevelClick
            )
        }
    }
}

@Composable
private fun FlushingTabBar(
    selectedTab: FlushingTab,
    onTabSelected: (FlushingTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlushingTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            val shape = RoundedCornerShape(8.dp)
            OutlinedButton(
                onClick = { onTabSelected(tab) },
                modifier = Modifier
                    .weight(1f)
                    .automationId(flushingTabAutomationId(tab)),
                shape = shape,
                border = BorderStroke(1.dp, BrBlack),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) BrYellow else BrWhite,
                    contentColor = BrBlack
                )
            ) {
                Text(
                    text = flushingTabLabel(tab),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun FlushModeSection(
    state: FlushingUiState,
    onGetFlushModeClick: () -> Unit,
    onSetFlushModeClick: (FlushMode) -> Unit
) {
    SectionCard {
        Text("Flush Mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        BrButton(
            text = "Get Flush Mode",
            onClick = onGetFlushModeClick,
            style = BrButtonStyle.Outlined,
            automationId = AutomationIds.GET_FLUSH_MODE
        )
        Spacer(Modifier.height(8.dp))
        ResultPanel(
            text = "Current mode: ${state.currentMode}",
            automationId = AutomationIds.CURRENT_FLUSH_MODE
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            BrButton(
                text = "IMMEDIATE",
                onClick = { onSetFlushModeClick(FlushMode.IMMEDIATE) },
                style = BrButtonStyle.Compact,
                modifier = Modifier.weight(1f),
                automationId = AutomationIds.FLUSH_MODE_IMMEDIATE
            )
            BrButton(
                text = "PERIOD",
                onClick = { onSetFlushModeClick(FlushMode.PERIOD) },
                style = BrButtonStyle.Compact,
                modifier = Modifier.weight(1f),
                automationId = AutomationIds.FLUSH_MODE_PERIOD
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            BrButton(
                text = "APP_CLOSE",
                onClick = { onSetFlushModeClick(FlushMode.APP_CLOSE) },
                style = BrButtonStyle.Compact,
                modifier = Modifier.weight(1f),
                automationId = AutomationIds.FLUSH_MODE_APP_CLOSE
            )
            BrButton(
                text = "MANUAL",
                onClick = { onSetFlushModeClick(FlushMode.MANUAL) },
                style = BrButtonStyle.Compact,
                modifier = Modifier.weight(1f),
                automationId = AutomationIds.FLUSH_MODE_MANUAL
            )
        }
    }
}

@Composable
private fun FlushPeriodSection(
    state: FlushingUiState,
    onGetFlushPeriodClick: () -> Unit,
    onSetFlushPeriodClick: (Long) -> Unit,
    onFlushDataClick: () -> Unit
) {
    SectionCard {
        Text("Flush Period", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        BrButton(
            text = "Get Flush Period",
            onClick = onGetFlushPeriodClick,
            style = BrButtonStyle.Outlined,
            automationId = AutomationIds.GET_FLUSH_PERIOD
        )
        Spacer(Modifier.height(8.dp))
        ResultPanel(
            text = "Current period: ${state.periodDisplay}",
            automationId = AutomationIds.CURRENT_FLUSH_PERIOD
        )
        state.periodHint?.let { hint ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = BrGrey600
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            BrButton(
                text = "30 seconds",
                onClick = { onSetFlushPeriodClick(30) },
                style = BrButtonStyle.Compact,
                modifier = Modifier.weight(1f),
                automationId = AutomationIds.FLUSH_PERIOD_30
            )
            BrButton(
                text = "60 seconds",
                onClick = { onSetFlushPeriodClick(60) },
                style = BrButtonStyle.Compact,
                modifier = Modifier.weight(1f),
                automationId = AutomationIds.FLUSH_PERIOD_60
            )
        }
        Spacer(Modifier.height(12.dp))
        BrButton(
            text = "Flush Data",
            onClick = onFlushDataClick,
            automationId = AutomationIds.FLUSH_DATA
        )
    }
}

@Composable
private fun LogLevelSection(
    state: FlushingUiState,
    onGetLogLevelClick: () -> Unit,
    onSetLogLevelClick: (Logger.Level) -> Unit
) {
    SectionCard {
        Text("Log Level", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        BrButton(
            text = "Get Log Level",
            onClick = onGetLogLevelClick,
            style = BrButtonStyle.Outlined,
            automationId = AutomationIds.GET_LOG_LEVEL
        )
        Spacer(Modifier.height(8.dp))
        ResultPanel(
            text = "Current log level: ${state.currentLogLevel}",
            automationId = AutomationIds.CURRENT_LOG_LEVEL
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            Logger.Level.OFF,
            Logger.Level.ERROR,
            Logger.Level.WARN,
            Logger.Level.INFO,
            Logger.Level.DEBUG,
            Logger.Level.VERBOSE
        ).forEach { level ->
            BrButton(
                text = level.name,
                onClick = { onSetLogLevelClick(level) },
                modifier = Modifier.padding(bottom = 6.dp),
                automationId = logLevelAutomationId(level)
            )
        }
    }
}

private fun flushingTabLabel(tab: FlushingTab): String = when (tab) {
    FlushingTab.FLUSH_MODE -> "Flush Mode"
    FlushingTab.FLUSH_PERIOD -> "Flush Period"
    FlushingTab.LOG_LEVEL -> "Log Level"
}

private fun flushingTabAutomationId(tab: FlushingTab): String = when (tab) {
    FlushingTab.FLUSH_MODE -> AutomationIds.TAB_FLUSH_MODE
    FlushingTab.FLUSH_PERIOD -> AutomationIds.TAB_FLUSH_PERIOD
    FlushingTab.LOG_LEVEL -> AutomationIds.TAB_LOG_LEVEL
}

private fun logLevelAutomationId(level: Logger.Level): String = when (level) {
    Logger.Level.OFF -> AutomationIds.LOG_OFF
    Logger.Level.ERROR -> AutomationIds.LOG_ERROR
    Logger.Level.WARN -> AutomationIds.LOG_WARN
    Logger.Level.INFO -> AutomationIds.LOG_INFO
    Logger.Level.DEBUG -> AutomationIds.LOG_DEBUG
    Logger.Level.VERBOSE -> AutomationIds.LOG_VERBOSE
}

@Preview(showBackground = true, name = "Flush Mode tab")
@Composable
private fun FlushingScreenModePreview() {
    BloomreachTheme {
        FlushingScreenContent(
            state = FlushingUiState(
                selectedTab = FlushingTab.FLUSH_MODE,
                flushMode = FlushMode.IMMEDIATE,
                currentPeriod = "30 seconds",
                currentLogLevel = "INFO"
            ),
            onTabSelected = {},
            onFlushDialogDismiss = {},
            onGetFlushModeClick = {},
            onSetFlushModeClick = {},
            onGetFlushPeriodClick = {},
            onSetFlushPeriodClick = {},
            onFlushDataClick = {},
            onGetLogLevelClick = {},
            onSetLogLevelClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Flush Period tab")
@Composable
private fun FlushingScreenPeriodPreview() {
    BloomreachTheme {
        FlushingScreenContent(
            state = FlushingUiState(
                selectedTab = FlushingTab.FLUSH_PERIOD,
                flushMode = FlushMode.IMMEDIATE,
                currentPeriod = "30 seconds",
                currentLogLevel = "INFO"
            ),
            onTabSelected = {},
            onFlushDialogDismiss = {},
            onGetFlushModeClick = {},
            onSetFlushModeClick = {},
            onGetFlushPeriodClick = {},
            onSetFlushPeriodClick = {},
            onFlushDataClick = {},
            onGetLogLevelClick = {},
            onSetLogLevelClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Log Level tab")
@Composable
private fun FlushingScreenLogLevelPreview() {
    BloomreachTheme {
        FlushingScreenContent(
            state = FlushingUiState(
                selectedTab = FlushingTab.LOG_LEVEL,
                flushMode = FlushMode.PERIOD,
                currentPeriod = "30 seconds",
                currentLogLevel = "INFO"
            ),
            onTabSelected = {},
            onFlushDialogDismiss = {},
            onGetFlushModeClick = {},
            onSetFlushModeClick = {},
            onGetFlushPeriodClick = {},
            onSetFlushPeriodClick = {},
            onFlushDataClick = {},
            onGetLogLevelClick = {},
            onSetLogLevelClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Flush success dialog")
@Composable
private fun FlushingScreenDialogPreview() {
    BloomreachTheme {
        FlushingScreenContent(
            state = FlushingUiState(
                selectedTab = FlushingTab.FLUSH_PERIOD,
                flushMode = FlushMode.PERIOD,
                currentPeriod = "30 seconds",
                currentLogLevel = "INFO",
                flushDialog = FlushDialogState(
                    title = "Flush successful",
                    message = "All pending data has been flushed."
                )
            ),
            onTabSelected = {},
            onFlushDialogDismiss = {},
            onGetFlushModeClick = {},
            onSetFlushModeClick = {},
            onGetFlushPeriodClick = {},
            onSetFlushPeriodClick = {},
            onFlushDataClick = {},
            onGetLogLevelClick = {},
            onSetLogLevelClick = {}
        )
    }
}
