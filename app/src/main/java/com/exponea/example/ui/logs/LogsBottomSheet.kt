package com.exponea.example.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exponea.example.R
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.logging.LogEntry
import com.exponea.example.logging.MemoryLogger
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey200
import com.exponea.example.ui.theme.BrGrey300
import com.exponea.example.ui.theme.BrGrey400
import com.exponea.example.ui.theme.BrGrey700
import com.exponea.example.ui.theme.BrWhite
import com.exponea.example.ui.theme.BrYellow
import com.exponea.example.ui.theme.LogPillDebug
import com.exponea.example.ui.theme.LogPillError
import com.exponea.example.ui.theme.LogPillInfo
import com.exponea.example.ui.theme.LogPillVerbose
import com.exponea.example.ui.theme.LogPillWarn
import com.exponea.example.ui.theme.MonospaceCodeStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogsBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var entries by remember { mutableStateOf(MemoryLogger.INSTANCE.snapshot()) }
    var levelFilter by remember { mutableStateOf<LogEntry.Level?>(null) }
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    LaunchedEffect(Unit) {
        MemoryLogger.INSTANCE.events.collectLatest { event ->
            when (event) {
                is MemoryLogger.Event.Appended -> {
                    entries = MemoryLogger.INSTANCE.snapshot()
                    val lastVisibleIndex = entries.indexOfLast { entry ->
                        levelFilter == null || entry.level == levelFilter
                    }
                    if (lastVisibleIndex >= 0) {
                        listState.animateScrollToItem(lastVisibleIndex)
                    }
                }
                MemoryLogger.Event.Cleared -> entries = emptyList()
            }
        }
    }

    val visibleEntries = entries.filter { entry ->
        levelFilter == null || entry.level == levelFilter
    }

    LaunchedEffect(sheetState) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrBlack)
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SDK Logs", color = BrWhite, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { copyLogs(context, visibleEntries) },
                        modifier = Modifier
                            .size(40.dp)
                            .automationId(AutomationIds.LOGS_COPY)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = AutomationIds.LOGS_COPY,
                            tint = BrWhite
                        )
                    }
                    IconButton(
                        onClick = { MemoryLogger.INSTANCE.clear() },
                        modifier = Modifier
                            .size(40.dp)
                            .automationId(AutomationIds.LOGS_CLEAR)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = AutomationIds.LOGS_CLEAR,
                            tint = BrWhite
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .automationId(AutomationIds.LOGS_CLOSE)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clear),
                            contentDescription = AutomationIds.LOGS_CLOSE,
                            tint = BrWhite
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrWhite)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LogFilterChip(
                    label = "All",
                    selected = levelFilter == null,
                    onClick = { levelFilter = null },
                    automationId = AutomationIds.LOGS_FILTER_ALL
                )
                LogEntry.Level.entries.forEach { level ->
                    LogFilterChip(
                        label = level.short,
                        selected = levelFilter == level,
                        onClick = { levelFilter = level },
                        automationId = logFilterAutomationId(level)
                    )
                }
            }
            HorizontalDivider(color = BrGrey300)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .navigationBarsPadding()
                    .automationId(AutomationIds.LIST_LOGS),
                reverseLayout = false
            ) {
                if (visibleEntries.isEmpty()) {
                    item(key = "logs_empty") {
                        Text(
                            text = "No logs to display.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .automationId(AutomationIds.LOGS_EMPTY),
                            color = BrGrey700,
                            fontSize = 12.sp,
                            fontFamily = MonospaceCodeStyle.fontFamily
                        )
                    }
                }
                items(visibleEntries, key = { it.id }) { entry ->
                    LogEntryRow(
                        entry = entry,
                        formattedTime = timeFormat.format(Date(entry.timestamp))
                    )
                }
            }
        }
    }
}

@Composable
private fun LogFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    automationId: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.automationId(automationId),
        shape = shape,
        color = if (selected) BrYellow else BrWhite,
        border = BorderStroke(1.dp, if (selected) BrBlack else BrGrey400)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = BrBlack,
            fontSize = 12.sp,
            fontFamily = MonospaceCodeStyle.fontFamily
        )
    }
}

@Composable
private fun LogEntryRow(
    entry: LogEntry,
    formattedTime: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrWhite)
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.level.name,
                modifier = Modifier
                    .background(entry.level.pillBackgroundColor(), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = BrBlack,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonospaceCodeStyle.fontFamily
            )
            Text(
                text = "$formattedTime · ${entry.tag}",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                color = BrGrey700,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = MonospaceCodeStyle.fontFamily
            )
        }
        Text(
            text = entry.message,
            modifier = Modifier.padding(top = 2.dp),
            color = BrBlack,
            fontSize = 12.sp,
            fontFamily = MonospaceCodeStyle.fontFamily
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = BrGrey200
        )
    }
}

private fun LogEntry.Level.pillBackgroundColor(): Color = when (this) {
    LogEntry.Level.VERBOSE -> LogPillVerbose
    LogEntry.Level.DEBUG -> LogPillDebug
    LogEntry.Level.INFO -> LogPillInfo
    LogEntry.Level.WARN -> LogPillWarn
    LogEntry.Level.ERROR -> LogPillError
}

private fun copyLogs(context: Context, entries: List<LogEntry>) {
    val text = entries.joinToString("\n") { "[${it.level.short}] ${it.tag}: ${it.message}" }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("SDK Logs", text))
}

private fun logFilterAutomationId(level: LogEntry.Level): String = when (level) {
    LogEntry.Level.VERBOSE -> AutomationIds.LOGS_FILTER_VERBOSE
    LogEntry.Level.DEBUG -> AutomationIds.LOGS_FILTER_DEBUG
    LogEntry.Level.INFO -> AutomationIds.LOGS_FILTER_INFO
    LogEntry.Level.WARN -> AutomationIds.LOGS_FILTER_WARN
    LogEntry.Level.ERROR -> AutomationIds.LOGS_FILTER_ERROR
}
