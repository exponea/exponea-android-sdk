package com.exponea.example.ui.fetching

import android.app.Activity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.model.Constants
import com.exponea.example.tracking.TrackPageView
import com.exponea.example.ui.components.BrButton
import com.exponea.example.ui.components.ResultPanel
import com.exponea.example.ui.components.SectionCard
import com.exponea.example.ui.components.SectionTitle
import com.exponea.example.ui.dialogs.FetchRecommendationDialog
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey100
import com.exponea.example.ui.theme.BrGrey200
import com.exponea.example.ui.theme.BrGrey400
import com.exponea.example.ui.theme.BrYellow
import com.exponea.sdk.Exponea

@Composable
fun FetchingScreen(
    modifier: Modifier = Modifier,
    viewModel: FetchingViewModel = viewModel()
) {
    TrackPageView(Constants.ScreenNames.FETCHING)
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.showRecommendationDialog) {
        FetchRecommendationDialog(
            onDismiss = viewModel::onRecommendationDialogDismiss,
            onFetch = viewModel::onFetchRecommendation
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrGrey100)
    ) {
        SectionCard {
            SectionTitle("Fetch")
            Spacer(Modifier.height(8.dp))
            BrButton(
                text = "Fetch Consents",
                onClick = viewModel::onFetchConsentsClick,
                automationId = AutomationIds.FETCH_CONSENTS
            )
            Spacer(Modifier.height(6.dp))
            BrButton(
                text = "Fetch Recommendations",
                onClick = viewModel::onFetchRecommendationsClick,
                automationId = AutomationIds.FETCH_RECOMMENDATIONS
            )
            Spacer(Modifier.height(6.dp))
            BrButton(
                text = "Fetch Segments",
                onClick = viewModel::onFetchSegmentsClick,
                automationId = AutomationIds.FETCH_SEGMENTS
            )
            Spacer(Modifier.height(6.dp))

            if (Exponea.isInitialized) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .automationId(AutomationIds.APP_INBOX_PROVIDER),
                    factory = { ctx ->
                        Exponea.getAppInboxButton(ctx as Activity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                    }
                )
            }
        }
        SectionCard(modifier = Modifier.weight(1f), fillHeight = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Result", modifier = Modifier.weight(1f))
                ClearResultButton(onClick = viewModel::onClearResultClick)
            }
            if (state.isLoading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .automationId(AutomationIds.FETCH_LOADING),
                    color = BrYellow,
                    trackColor = BrGrey100
                )
            }
            Spacer(Modifier.height(8.dp))
            ResultPanel(
                text = state.resultText,
                modifier = Modifier.weight(1f),
                scrollable = true,
                automationId = AutomationIds.FETCH_RESULT
            )
        }
    }
}

@Composable
private fun ClearResultButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isPressed) BrGrey200 else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = BrGrey400, bounded = true, radius = 20.dp),
                onClick = onClick
            )
            .automationId(AutomationIds.CLEAR_RESULT),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = AutomationIds.CLEAR_RESULT,
            modifier = Modifier.size(20.dp),
            tint = BrBlack
        )
    }
}
