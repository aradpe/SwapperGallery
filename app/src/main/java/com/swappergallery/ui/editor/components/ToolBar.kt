package com.swappergallery.ui.editor.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swappergallery.ui.editor.EditorTool
import com.swappergallery.ui.theme.ToolSelected
import com.swappergallery.ui.theme.ToolUnselected

@Composable
fun EditorToolBar(
    activeTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ToolButton(
            icon = Icons.Outlined.Crop,
            label = "Crop",
            isSelected = activeTool == EditorTool.CROP,
            onClick = { onToolSelected(EditorTool.CROP) }
        )
        ToolButton(
            icon = Icons.Outlined.FilterVintage,
            label = "Filter",
            isSelected = activeTool == EditorTool.FILTER,
            onClick = { onToolSelected(EditorTool.FILTER) }
        )
        ToolButton(
            icon = Icons.Outlined.Tune,
            label = "Adjust",
            isSelected = activeTool == EditorTool.ADJUST,
            onClick = { onToolSelected(EditorTool.ADJUST) }
        )
        ToolButton(
            icon = Icons.Outlined.EmojiEmotions,
            label = "Sticker",
            isSelected = activeTool == EditorTool.STICKER,
            onClick = { onToolSelected(EditorTool.STICKER) }
        )
        ToolButton(
            icon = Icons.Outlined.Brush,
            label = "Draw",
            isSelected = activeTool == EditorTool.DRAW,
            onClick = { onToolSelected(EditorTool.DRAW) }
        )
        ToolButton(
            icon = Icons.Outlined.TextFields,
            label = "Text",
            isSelected = activeTool == EditorTool.TEXT,
            onClick = { onToolSelected(EditorTool.TEXT) }
        )
        ToolButton(
            icon = Icons.Outlined.BlurOn,
            label = "Blur",
            isSelected = activeTool == EditorTool.BLUR,
            onClick = { onToolSelected(EditorTool.BLUR) }
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) ToolSelected else ToolUnselected

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) Modifier.background(Color.White.copy(alpha = 0.12f))
                    else Modifier
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            fontSize = 9.sp,
            color = tint
        )
    }
}
