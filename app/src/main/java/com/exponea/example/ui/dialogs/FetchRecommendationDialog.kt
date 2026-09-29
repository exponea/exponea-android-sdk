package com.exponea.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.ui.components.BrButton
import com.exponea.example.ui.components.BrButtonStyle
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrWhite
import com.exponea.sdk.models.CustomerRecommendationOptions

@Composable
fun FetchRecommendationDialog(
    onDismiss: () -> Unit,
    onFetch: (CustomerRecommendationOptions) -> Unit
) {
    var recommendationId by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = BrWhite
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Fetch Recommendation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrBlack,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = recommendationId,
                    onValueChange = { recommendationId = it },
                    label = { Text("Recommendation ID") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .automationId(AutomationIds.RECOMMENDATION_ID),
                    singleLine = true,
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
                    )
                )
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    BrButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        style = BrButtonStyle.Outlined,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .automationId(AutomationIds.RECOMMENDATION_DIALOG_CANCEL)
                    )
                    BrButton(
                        text = "Fetch",
                        onClick = {
                            val id = recommendationId.trim()
                            if (id.isNotBlank()) {
                                onFetch(
                                    CustomerRecommendationOptions(
                                        id = id,
                                        fillWithRandom = true
                                    )
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .automationId(AutomationIds.RECOMMENDATION_DIALOG_FETCH)
                    )
                }
            }
        }
    }
}
