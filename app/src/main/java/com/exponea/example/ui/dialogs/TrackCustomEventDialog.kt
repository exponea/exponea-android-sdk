package com.exponea.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.exponea.example.automation.AutomationIds
import com.exponea.example.automation.automationId
import com.exponea.example.tracking.InAppMessagePreset
import com.exponea.example.tracking.inAppMessagePresetCategories
import com.exponea.example.tracking.inAppMessagePresets
import com.exponea.example.ui.components.BrButton
import com.exponea.example.ui.components.BrButtonStyle
import com.exponea.example.ui.components.ResultPanel
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrWhite
import com.exponea.example.util.asJson
import com.exponea.sdk.models.PropertiesList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackCustomEventDialog(
    onDismiss: () -> Unit,
    onTrack: (eventName: String, properties: PropertiesList) -> Unit
) {
    val propsMap = remember { mutableStateMapOf("property" to "some value" as Any) }
    var eventName by remember { mutableStateOf("event_name") }
    var propertyName by remember { mutableStateOf("property") }
    var propertyValue by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<InAppMessagePreset?>(null) }

    fun applyPreset(preset: InAppMessagePreset?) {
        selectedPreset = preset
        if (preset != null) {
            eventName = preset.eventName
            propsMap.clear()
            preset.properties.forEach { (key, value) ->
                propsMap[key] = value
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = BrWhite
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Track Custom Event",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrBlack,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                InAppMessagePresetDropdown(
                    selectedPreset = selectedPreset,
                    onPresetSelected = ::applyPreset,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DialogOutlinedField(
                    label = "Event name",
                    value = eventName,
                    onValueChange = { eventName = it },
                    enabled = selectedPreset == null,
                    automationId = AutomationIds.EVENT_NAME,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DialogOutlinedField(
                    label = "Attribute name",
                    value = propertyName,
                    onValueChange = { propertyName = it },
                    automationId = AutomationIds.EVENT_ATTR_NAME,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DialogOutlinedField(
                    label = "Attribute value",
                    value = propertyValue,
                    onValueChange = { propertyValue = it },
                    automationId = AutomationIds.EVENT_ATTR_VALUE,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BrButton(
                    text = "Add",
                    onClick = {
                        val name = propertyName.trim()
                        val value = propertyValue.trim()
                        if (name.isNotEmpty() && value.isNotEmpty()) {
                            propsMap[name] = value
                            propertyName = ""
                            propertyValue = ""
                        }
                    },
                    style = BrButtonStyle.Outlined,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .automationId(AutomationIds.EVENT_ADD_ATTR)
                )
                ResultPanel(
                    text = propsMap.asJson(),
                    modifier = Modifier
                        .heightIn(min = 80.dp)
                        .padding(bottom = 16.dp),
                    automationId = AutomationIds.EVENT_PROPS_JSON
                )
                BrButton(
                    text = "Track",
                    onClick = {
                        val name = eventName.trim().ifBlank { "event_name" }
                        onTrack(name, PropertiesList(HashMap(propsMap)))
                        onDismiss()
                    },
                    automationId = AutomationIds.TRACK_EVENT
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InAppMessagePresetDropdown(
    selectedPreset: InAppMessagePreset?,
    onPresetSelected: (InAppMessagePreset?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = selectedPreset?.label ?: "(custom)"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("In-app message preset") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .automationId(AutomationIds.INAPP_PRESET),
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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("(custom)") },
                onClick = {
                    onPresetSelected(null)
                    expanded = false
                }
            )
            inAppMessagePresetCategories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrGrey600
                        )
                    },
                    onClick = {},
                    enabled = false
                )
                inAppMessagePresets
                    .filter { it.category == category }
                    .forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = preset.label,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            },
                            onClick = {
                                onPresetSelected(preset)
                                expanded = false
                            }
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
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
