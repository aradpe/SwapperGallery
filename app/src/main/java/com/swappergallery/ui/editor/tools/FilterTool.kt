package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType
import com.swappergallery.ui.editor.components.SliderControl

data class FilterPreset(
    val name: String,
    val displayName: String,
    val color: Color // Preview color hint
)

val filterPresets = listOf(
    FilterPreset("none", "None", Color.Gray),
    FilterPreset("b&w", "B&W", Color.DarkGray),
    FilterPreset("sepia", "Sepia", Color(0xFFA0785A)),
    FilterPreset("vintage", "Vintage", Color(0xFFB8956A)),
    FilterPreset("vivid", "Vivid", Color(0xFFFF4081)),
    FilterPreset("cool", "Cool", Color(0xFF4FC3F7)),
    FilterPreset("warm", "Warm", Color(0xFFFFB74D)),
    FilterPreset("noir", "Noir", Color(0xFF424242)),
    FilterPreset("fade", "Fade", Color(0xFFBDBDBD)),
    FilterPreset("dramatic", "Dramatic", Color(0xFF5D4037)),
    FilterPreset("chrome", "Chrome", Color(0xFF78909C)),
    FilterPreset("invert", "Invert", Color(0xFF00BCD4)),
    FilterPreset("pastel", "Pastel", Color(0xFFE1BEE7)),
)

@Composable
fun FilterToolPanel(
    existingData: LayerData.FilterData? = null,
    onApplyFilter: (LayerType, LayerData) -> Unit,
    onUpdateFilter: (LayerData) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(existingData?.filterName ?: "none") }
    var intensity by remember { mutableFloatStateOf(existingData?.intensity ?: 1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Filters", color = Color.White.copy(alpha = 0.7f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterPresets.forEach { preset ->
                FilterPresetItem(
                    preset = preset,
                    isSelected = selectedFilter == preset.name,
                    onClick = {
                        selectedFilter = preset.name
                        if (preset.name == "none") return@FilterPresetItem
                        val data = LayerData.FilterData(
                            filterName = preset.name,
                            intensity = intensity
                        )
                        if (existingData != null) {
                            onUpdateFilter(data)
                        } else {
                            onApplyFilter(LayerType.FILTER, data)
                        }
                    }
                )
            }
        }

        if (selectedFilter != "none") {
            SliderControl(
                label = "Intensity",
                value = intensity * 100f,
                onValueChange = {
                    intensity = it / 100f
                    val data = LayerData.FilterData(
                        filterName = selectedFilter,
                        intensity = intensity
                    )
                    if (existingData != null) {
                        onUpdateFilter(data)
                    }
                },
                valueRange = 0f..100f
            )
        }
    }
}

@Composable
private fun FilterPresetItem(
    preset: FilterPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(preset.color)
                .then(
                    if (isSelected) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp)
                    )
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = preset.displayName.take(3),
                color = Color.White,
                fontSize = 12.sp
            )
        }
        Text(
            text = preset.displayName,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
