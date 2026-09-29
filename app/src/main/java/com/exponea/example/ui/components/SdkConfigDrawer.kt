package com.exponea.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.config.SdkConfigSummary
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrWhite

@Composable
fun SdkConfigDrawerContent(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = BrWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .automationId(AutomationIds.CONFIG_DRAWER)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exponea configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrBlack
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .automationId(AutomationIds.CLOSE_CONFIG_DRAWER)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = AutomationIds.CLOSE_CONFIG_DRAWER,
                        tint = BrBlack
                    )
                }
            }
            HorizontalDivider()
            SdkConfigSummary.entries().forEach { entry ->
                ConfigDrawerRow(label = entry.label, value = entry.value)
            }
        }
    }
}

@Composable
private fun ConfigDrawerRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = BrGrey600
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = BrBlack,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    HorizontalDivider()
}
