package com.exponea.example.automation

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

fun Modifier.automationId(id: String): Modifier = semantics {
    contentDescription = id
}
