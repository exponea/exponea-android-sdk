package com.exponea.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exponea.example.automation.automationId
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey200
import com.exponea.example.ui.theme.BrGrey300
import com.exponea.example.ui.theme.BrGrey700
import com.exponea.example.ui.theme.BrWhite
import com.exponea.example.ui.theme.MonospaceCodeStyle

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = BrWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
            content = content
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = BrBlack
    )
}

@Composable
fun ResultPanel(
    text: String,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    automationId: String? = null
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BrGrey300, RoundedCornerShape(4.dp))
            .background(BrGrey200, RoundedCornerShape(4.dp))
            .padding(8.dp)
            .then(if (automationId != null) Modifier.automationId(automationId) else Modifier)
    ) {
        Text(
            text = text,
            modifier = if (scrollable) {
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            } else {
                Modifier.fillMaxWidth()
            },
            style = MonospaceCodeStyle,
            color = BrGrey700
        )
    }
}
