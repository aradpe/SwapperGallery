package com.swappergallery.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val presetColors = listOf(
    0xFFFFFFFF, // White
    0xFF000000, // Black
    0xFFFF0000, // Red
    0xFFFF5722, // Deep Orange
    0xFFFF9800, // Orange
    0xFFFFEB3B, // Yellow
    0xFF4CAF50, // Green
    0xFF00BCD4, // Cyan
    0xFF2196F3, // Blue
    0xFF3F51B5, // Indigo
    0xFF9C27B0, // Purple
    0xFFE91E63, // Pink
    0xFF795548, // Brown
    0xFF607D8B, // Blue Grey
    0xFF9E9E9E, // Grey
)

@Composable
fun ColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showTransparent: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showTransparent) {
            TransparentCircle(
                isSelected = (selectedColor.toInt() ushr 24) == 0,
                onClick = { onColorSelected(0x00000000) }
            )
        }
        for (color in presetColors) {
            ColorCircle(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun TransparentCircle(isSelected: Boolean, onClick: () -> Unit) {
    val borderMod = if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
        else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.DarkGray)
            .drawBehind {
                drawLine(Color.Red, Offset(4f, 4f), Offset(size.width - 4f, size.height - 4f), strokeWidth = 2f)
            }
            .then(borderMod)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ColorCircle(
    color: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(color.toInt()))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, Color.White, CircleShape)
                } else {
                    Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                }
            )
            .clickable(onClick = onClick)
    )
}
