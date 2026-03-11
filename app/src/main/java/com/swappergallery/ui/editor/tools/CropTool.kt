package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType
import com.swappergallery.ui.editor.components.SliderControl

enum class AspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    THREE_TWO("3:2", 3f / 2f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

@Composable
fun CropToolPanel(
    existingData: LayerData.CropData? = null,
    onApplyCrop: (LayerType, LayerData) -> Unit,
    onUpdateCrop: (LayerData) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRatio by remember { mutableStateOf(AspectRatio.FREE) }
    var rotation by remember { mutableFloatStateOf(existingData?.rotation ?: 0f) }
    var left by remember { mutableFloatStateOf(existingData?.left ?: 0f) }
    var top by remember { mutableFloatStateOf(existingData?.top ?: 0f) }
    var right by remember { mutableFloatStateOf(existingData?.right ?: 1f) }
    var bottom by remember { mutableFloatStateOf(existingData?.bottom ?: 1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Aspect Ratio", color = Color.White.copy(alpha = 0.7f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AspectRatio.entries.forEach { ratio ->
                FilterChip(
                    selected = selectedRatio == ratio,
                    onClick = { selectedRatio = ratio },
                    label = { Text(ratio.label) }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90°", tint = Color.White)
            }
            Text(
                text = "${rotation.toInt()}°",
                color = Color.White
            )
        }

        SliderControl(
            label = "Fine Rotation",
            value = rotation,
            onValueChange = { rotation = it },
            valueRange = 0f..360f
        )

        SliderControl(
            label = "Left",
            value = left * 100f,
            onValueChange = { left = (it / 100f).coerceIn(0f, right - 0.1f) },
            valueRange = 0f..90f
        )
        SliderControl(
            label = "Top",
            value = top * 100f,
            onValueChange = { top = (it / 100f).coerceIn(0f, bottom - 0.1f) },
            valueRange = 0f..90f
        )
        SliderControl(
            label = "Right",
            value = right * 100f,
            onValueChange = { right = (it / 100f).coerceIn(left + 0.1f, 1f) },
            valueRange = 10f..100f
        )
        SliderControl(
            label = "Bottom",
            value = bottom * 100f,
            onValueChange = { bottom = (it / 100f).coerceIn(top + 0.1f, 1f) },
            valueRange = 10f..100f
        )

        Button(
            onClick = {
                val data = LayerData.CropData(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    rotation = rotation
                )
                if (existingData != null) {
                    onUpdateCrop(data)
                } else {
                    onApplyCrop(LayerType.CROP, data)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(if (existingData != null) "Update Crop" else "Apply Crop")
        }
    }
}
