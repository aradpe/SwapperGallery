package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.swappergallery.data.model.LayerData
import com.swappergallery.ui.editor.components.ColorPicker
import com.swappergallery.ui.editor.components.SliderControl

data class DrawToolState(
    val color: Long = 0xFFFF0000,
    val strokeWidth: Float = 8f,
    val opacity: Float = 1f,
    val isEraser: Boolean = false,
    val shapeType: LayerData.ShapeType = LayerData.ShapeType.FREEHAND
)

@Composable
fun DrawToolPanel(
    drawState: DrawToolState,
    onStateChange: (DrawToolState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Shape type selection
        Text("Shape", color = Color.White.copy(alpha = 0.7f))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = drawState.shapeType == LayerData.ShapeType.FREEHAND,
                onClick = { onStateChange(drawState.copy(shapeType = LayerData.ShapeType.FREEHAND)) },
                label = { Text("Free") }
            )
            FilterChip(
                selected = drawState.shapeType == LayerData.ShapeType.LINE,
                onClick = { onStateChange(drawState.copy(shapeType = LayerData.ShapeType.LINE, isEraser = false)) },
                label = { Text("Line") }
            )
            FilterChip(
                selected = drawState.shapeType == LayerData.ShapeType.ARROW,
                onClick = { onStateChange(drawState.copy(shapeType = LayerData.ShapeType.ARROW, isEraser = false)) },
                label = { Text("Arrow") }
            )
            FilterChip(
                selected = drawState.shapeType == LayerData.ShapeType.RECTANGLE,
                onClick = { onStateChange(drawState.copy(shapeType = LayerData.ShapeType.RECTANGLE, isEraser = false)) },
                label = { Text("Rect") }
            )
            FilterChip(
                selected = drawState.shapeType == LayerData.ShapeType.CIRCLE,
                onClick = { onStateChange(drawState.copy(shapeType = LayerData.ShapeType.CIRCLE, isEraser = false)) },
                label = { Text("Circle") }
            )
        }

        // Brush/Eraser toggle (only for freehand)
        if (drawState.shapeType == LayerData.ShapeType.FREEHAND) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !drawState.isEraser,
                    onClick = { onStateChange(drawState.copy(isEraser = false)) },
                    label = { Text("Brush") }
                )
                FilterChip(
                    selected = drawState.isEraser,
                    onClick = { onStateChange(drawState.copy(isEraser = true)) },
                    label = { Text("Eraser") }
                )
            }
        }

        if (!(drawState.shapeType == LayerData.ShapeType.FREEHAND && drawState.isEraser)) {
            Text("Color", color = Color.White.copy(alpha = 0.7f))
            ColorPicker(
                selectedColor = drawState.color,
                onColorSelected = { onStateChange(drawState.copy(color = it)) }
            )
        }

        SliderControl(
            label = "Size",
            value = drawState.strokeWidth,
            onValueChange = { onStateChange(drawState.copy(strokeWidth = it)) },
            valueRange = 1f..50f
        )

        SliderControl(
            label = "Opacity",
            value = drawState.opacity * 100f,
            onValueChange = { onStateChange(drawState.copy(opacity = it / 100f)) },
            valueRange = 10f..100f
        )
    }
}
