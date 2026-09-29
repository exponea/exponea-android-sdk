package com.exponea.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exponea.example.automation.automationId
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrRed
import com.exponea.example.ui.theme.BrWhite
import com.exponea.example.ui.theme.BrYellow

enum class BrButtonStyle { Primary, Outlined, Danger, Compact }

@Composable
fun BrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BrButtonStyle = BrButtonStyle.Primary,
    enabled: Boolean = true,
    automationId: String? = null
) {
    val shape = RoundedCornerShape(8.dp)
    val minHeight = if (style == BrButtonStyle.Compact) 36.dp else 44.dp
    val baseModifier = modifier
        .then(if (automationId != null) Modifier.automationId(automationId) else Modifier)
        .fillMaxWidth()
        .heightIn(min = minHeight)

    when (style) {
        BrButtonStyle.Primary, BrButtonStyle.Compact -> Button(
            onClick = onClick,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrYellow,
                contentColor = BrBlack,
                disabledContainerColor = BrYellow.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = if (style == BrButtonStyle.Compact) 12.sp else 14.sp
            )
        }
        BrButtonStyle.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, BrBlack),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = BrWhite,
                contentColor = BrBlack
            )
        ) {
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        BrButtonStyle.Danger -> OutlinedButton(
            onClick = onClick,
            modifier = baseModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrRed)
        ) {
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
