package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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

@Composable
fun BlurToolPanel(
    existingData: LayerData.BlurData? = null,
    onApplyBlur: (LayerType, LayerData) -> Unit,
    onUpdateBlur: (LayerData) -> Unit,
    modifier: Modifier = Modifier
) {
    var blurType by remember { mutableStateOf(existingData?.type ?: LayerData.BlurType.FULL) }
    var intensity by remember { mutableFloatStateOf(existingData?.intensity ?: 10f) }
    var centerX by remember { mutableFloatStateOf(existingData?.centerX ?: 0.5f) }
    var centerY by remember { mutableFloatStateOf(existingData?.centerY ?: 0.5f) }
    var radius by remember { mutableFloatStateOf(existingData?.radius ?: 0.3f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Blur Type", color = Color.White.copy(alpha = 0.7f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = blurType == LayerData.BlurType.FULL,
                onClick = { blurType = LayerData.BlurType.FULL },
                label = { Text("Full") }
            )
            FilterChip(
                selected = blurType == LayerData.BlurType.RADIAL,
                onClick = { blurType = LayerData.BlurType.RADIAL },
                label = { Text("Radial") }
            )
            FilterChip(
                selected = blurType == LayerData.BlurType.LINEAR,
                onClick = { blurType = LayerData.BlurType.LINEAR },
                label = { Text("Linear") }
            )
        }

        SliderControl(
            label = "Intensity",
            value = intensity,
            onValueChange = { intensity = it },
            valueRange = 1f..25f
        )

        if (blurType == LayerData.BlurType.RADIAL) {
            SliderControl(
                label = "Center X",
                value = centerX * 100f,
                onValueChange = { centerX = it / 100f },
                valueRange = 0f..100f
            )
            SliderControl(
                label = "Center Y",
                value = centerY * 100f,
                onValueChange = { centerY = it / 100f },
                valueRange = 0f..100f
            )
            SliderControl(
                label = "Clear Radius",
                value = radius * 100f,
                onValueChange = { radius = it / 100f },
                valueRange = 5f..80f
            )
        }

        Button(
            onClick = {
                val data = LayerData.BlurData(
                    type = blurType,
                    intensity = intensity,
                    centerX = centerX,
                    centerY = centerY,
                    radius = radius
                )
                if (existingData != null) {
                    onUpdateBlur(data)
                } else {
                    onApplyBlur(LayerType.BLUR, data)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(if (existingData != null) "Update Blur" else "Apply Blur")
        }
    }
}
