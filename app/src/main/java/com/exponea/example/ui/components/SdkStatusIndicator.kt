package com.exponea.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exponea.example.automation.AutomationIds
import com.exponea.example.status.SdkLifecycleState
import com.exponea.example.status.SdkStatusStore
import com.exponea.example.ui.theme.BrGreen
import com.exponea.example.ui.theme.BrRed
import com.exponea.example.ui.theme.BrWhite
import com.exponea.example.ui.theme.BrYellow

@Composable
fun SdkStatusIndicator(
    modifier: Modifier = Modifier,
    store: SdkStatusStore = SdkStatusStore
) {
    val state by store.state.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .size(12.dp)
            .border(1.dp, BrWhite, CircleShape)
            .background(colorFor(state), CircleShape)
            .semantics {
                contentDescription = AutomationIds.STATUS_SDK_INDICATOR
                stateDescription = state.semanticsValue
            }
    )
}

private fun colorFor(state: SdkLifecycleState): Color = when (state) {
    SdkLifecycleState.NOT_INITIALIZED -> BrRed
    SdkLifecycleState.ANONYMOUS -> BrYellow
    SdkLifecycleState.IDENTIFIED -> BrGreen
}
