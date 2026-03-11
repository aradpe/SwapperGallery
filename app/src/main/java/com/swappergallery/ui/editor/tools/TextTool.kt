package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.swappergallery.data.model.LayerData
import com.swappergallery.ui.editor.components.ColorPicker
import com.swappergallery.ui.editor.components.SliderControl

@Composable
fun TextToolPanel(
    existingData: LayerData.TextData? = null,
    onUpdateText: (LayerData) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(existingData?.text ?: "") }
    var color by remember { mutableStateOf(existingData?.color ?: 0xFFFFFFFF) }
    var fontSize by remember { mutableFloatStateOf(existingData?.fontSize ?: 48f) }
    var bold by remember { mutableStateOf(existingData?.bold ?: false) }
    var italic by remember { mutableStateOf(existingData?.italic ?: false) }
    var rotation by remember { mutableFloatStateOf(existingData?.rotation ?: 0f) }
    var outlineWidth by remember { mutableFloatStateOf(existingData?.outlineWidth ?: 0f) }
    var outlineColor by remember { mutableStateOf(existingData?.outlineColor ?: 0xFF000000) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun currentData() = LayerData.TextData(
        text = text,
        color = color,
        fontSize = fontSize,
        bold = bold,
        italic = italic,
        rotation = rotation,
        outlineWidth = outlineWidth,
        outlineColor = outlineColor,
        x = existingData?.x ?: 0.5f,
        y = existingData?.y ?: 0.5f,
        scale = existingData?.scale ?: 1f,
        backgroundColor = existingData?.backgroundColor ?: 0x00000000,
        fontFamily = existingData?.fontFamily ?: "sans-serif"
    )

    fun onChanged() {
        if (text.isNotBlank()) {
            onUpdateText(currentData())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; onChanged() },
            label = { Text("Enter text") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White.copy(alpha = 0.7f),
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconToggleButton(checked = bold, onCheckedChange = { bold = it; onChanged() }) {
                Icon(
                    Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    tint = if (bold) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
            IconToggleButton(checked = italic, onCheckedChange = { italic = it; onChanged() }) {
                Icon(
                    Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    tint = if (italic) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
        }

        Text("Color", color = Color.White.copy(alpha = 0.7f))
        ColorPicker(selectedColor = color, onColorSelected = { color = it; onChanged() })

        SliderControl(
            label = "Font Size",
            value = fontSize,
            onValueChange = { fontSize = it; onChanged() },
            valueRange = 12f..200f
        )

        SliderControl(
            label = "Rotation",
            value = rotation,
            onValueChange = { rotation = it; onChanged() },
            valueRange = -180f..180f
        )

        SliderControl(
            label = "Outline",
            value = outlineWidth,
            onValueChange = { outlineWidth = it; onChanged() },
            valueRange = 0f..20f
        )

    }
}
