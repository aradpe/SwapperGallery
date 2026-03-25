package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ShapeIcon("✏️", drawState.shapeType == LayerData.ShapeType.FREEHAND) {
                onStateChange(drawState.copy(shapeType = LayerData.ShapeType.FREEHAND))
            }
            ShapeIcon("╱", drawState.shapeType == LayerData.ShapeType.LINE) {
                onStateChange(drawState.copy(shapeType = LayerData.ShapeType.LINE, isEraser = false))
            }
            ShapeIcon("↗", drawState.shapeType == LayerData.ShapeType.ARROW) {
                onStateChange(drawState.copy(shapeType = LayerData.ShapeType.ARROW, isEraser = false))
            }
            ShapeIcon("▢", drawState.shapeType == LayerData.ShapeType.RECTANGLE) {
                onStateChange(drawState.copy(shapeType = LayerData.ShapeType.RECTANGLE, isEraser = false))
            }
            ShapeIcon("○", drawState.shapeType == LayerData.ShapeType.CIRCLE) {
                onStateChange(drawState.copy(shapeType = LayerData.ShapeType.CIRCLE, isEraser = false))
            }
        }

        // Brush/Eraser toggle (only for freehand)
        if (drawState.shapeType == LayerData.ShapeType.FREEHAND) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                ShapeIcon("🖌️", !drawState.isEraser) {
                    onStateChange(drawState.copy(isEraser = false))
                }
                ShapeIcon("🧹", drawState.isEraser) {
                    onStateChange(drawState.copy(isEraser = true))
                }
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

@Composable
private fun ShapeIcon(emoji: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}
