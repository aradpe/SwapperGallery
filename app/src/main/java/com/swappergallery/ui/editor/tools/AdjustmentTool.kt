package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType
import com.swappergallery.ui.editor.components.SliderControl

@Composable
fun AdjustmentToolPanel(
    existingData: LayerData.AdjustmentData? = null,
    onApplyAdjustment: (LayerType, LayerData) -> Unit,
    onUpdateAdjustment: (LayerData) -> Unit,
    modifier: Modifier = Modifier
) {
    var brightness by remember { mutableFloatStateOf(existingData?.brightness ?: 0f) }
    var contrast by remember { mutableFloatStateOf(existingData?.contrast ?: 0f) }
    var saturation by remember { mutableFloatStateOf(existingData?.saturation ?: 0f) }
    var warmth by remember { mutableFloatStateOf(existingData?.warmth ?: 0f) }
    var sharpness by remember { mutableFloatStateOf(existingData?.sharpness ?: 0f) }
    var highlights by remember { mutableFloatStateOf(existingData?.highlights ?: 0f) }
    var shadows by remember { mutableFloatStateOf(existingData?.shadows ?: 0f) }
    var vignette by remember { mutableFloatStateOf(existingData?.vignette ?: 0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SliderControl(label = "Brightness", value = brightness, onValueChange = { brightness = it })
        SliderControl(label = "Contrast", value = contrast, onValueChange = { contrast = it })
        SliderControl(label = "Saturation", value = saturation, onValueChange = { saturation = it })
        SliderControl(label = "Warmth", value = warmth, onValueChange = { warmth = it })
        SliderControl(label = "Sharpness", value = sharpness, onValueChange = { sharpness = it }, valueRange = 0f..100f)
        SliderControl(label = "Highlights", value = highlights, onValueChange = { highlights = it })
        SliderControl(label = "Shadows", value = shadows, onValueChange = { shadows = it })
        SliderControl(label = "Vignette", value = vignette, onValueChange = { vignette = it }, valueRange = 0f..100f)

        Button(
            onClick = {
                val data = LayerData.AdjustmentData(
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    warmth = warmth,
                    sharpness = sharpness,
                    highlights = highlights,
                    shadows = shadows,
                    vignette = vignette
                )
                if (existingData != null) {
                    onUpdateAdjustment(data)
                } else {
                    onApplyAdjustment(LayerType.ADJUSTMENT, data)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(if (existingData != null) "Update Adjustment" else "Apply Adjustment")
        }
    }
}
