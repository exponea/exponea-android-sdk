package com.exponea.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.exponea.example.App
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.managers.LocalJwtTokenGenerator
import com.exponea.example.model.Constants
import com.exponea.example.model.SdkSetupState
import com.exponea.example.ui.components.BrButton
import com.exponea.example.ui.components.BrButtonStyle
import com.exponea.example.ui.components.ResultPanel
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrWhite
import com.exponea.example.util.asJson

@Composable
fun IdentifyCustomerDialog(
    onDismiss: () -> Unit,
    onIdentify: (properties: HashMap<String, Any>, withAuthToken: Boolean) -> Unit
) {
    val context = LocalContext.current
    val attributes = remember { mutableStateMapOf<String, Any>() }
    var propertyName by remember { mutableStateOf(Constants.CUSTOMER_ID_REGISTERED) }
    var propertyValue by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = BrWhite
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Identify Customer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrBlack,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ResultPanel(
                    text = "${Constants.CUSTOMER_ID_REGISTERED}: ${App.instance.registeredIdManager.registeredID}",
                    modifier = Modifier.padding(bottom = 12.dp),
                    automationId = AutomationIds.REGISTERED_ID_LABEL
                )
                DialogOutlinedField(
                    label = "Attribute name",
                    value = propertyName,
                    onValueChange = { propertyName = it },
                    automationId = AutomationIds.IDENTIFY_ATTR_NAME,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DialogOutlinedField(
                    label = "Attribute value",
                    value = propertyValue,
                    onValueChange = { propertyValue = it },
                    automationId = AutomationIds.IDENTIFY_ATTR_VALUE,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BrButton(
                    text = "Add",
                    onClick = {
                        val name = propertyName.trim()
                        val value = propertyValue.trim()
                        if (name.isNotEmpty() && value.isNotEmpty()) {
                            attributes[name] = value
                            propertyName = ""
                            propertyValue = ""
                        }
                    },
                    style = BrButtonStyle.Outlined,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .automationId(AutomationIds.IDENTIFY_ADD_ATTR)
                )
                ResultPanel(
                    text = attributes.asJson(),
                    modifier = Modifier
                        .heightIn(min = 80.dp)
                        .padding(bottom = 16.dp),
                    automationId = AutomationIds.IDENTIFY_ATTRS_JSON
                )
                BrButton(
                    text = "Identify",
                    onClick = {
                        onIdentify(HashMap(attributes), false)
                        onDismiss()
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    automationId = AutomationIds.IDENTIFY
                )
                if (SdkSetupState.isStreamConfig) {
                    BrButton(
                        text = "Identify with auth token",
                        onClick = {
                            if (!LocalJwtTokenGenerator.INSTANCE.isConfigured()) {
                                Toast.makeText(
                                    context,
                                    "JWT Key ID and Secret must be provided during configuration.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@BrButton
                            }
                            onIdentify(HashMap(attributes), true)
                            onDismiss()
                        },
                        style = BrButtonStyle.Outlined,
                        automationId = AutomationIds.IDENTIFY_AUTH
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogOutlinedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    automationId: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .automationId(automationId),
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
}
