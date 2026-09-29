package com.exponea.example.ui.common

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow

@Composable
fun CollectUiMessages(messages: Flow<UiMessage>) {
    val context = LocalContext.current
    LaunchedEffect(messages) {
        messages.collect { message ->
            Toast.makeText(
                context,
                message.text,
                if (message.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }
}
