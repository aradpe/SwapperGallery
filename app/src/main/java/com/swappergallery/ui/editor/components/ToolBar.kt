package com.swappergallery.ui.editor.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ToolButton(
            icon = Icons.Default.TextFields,
            label = "Text",
            isSelected = activeTool == EditorTool.TEXT,
            onClick = { onToolSelected(EditorTool.TEXT) }
        )
        ToolButton(
            icon = Icons.Default.Brush,
            label = "Draw",
            isSelected = activeTool == EditorTool.DRAW,
            onClick = { onToolSelected(EditorTool.DRAW) }
        )
        ToolButton(
            icon = Icons.Default.ContentCut,
            label = "Crop",
            isSelected = activeTool == EditorTool.CROP,
            onClick = { onToolSelected(EditorTool.CROP) }
        )
        ToolButton(
            icon = Icons.Default.FilterVintage,
            label = "Filter",
            isSelected = activeTool == EditorTool.FILTER,
            onClick = { onToolSelected(EditorTool.FILTER) }
        )
        ToolButton(
            icon = Icons.Default.Tune,
            label = "Adjust",
            isSelected = activeTool == EditorTool.ADJUST,
            onClick = { onToolSelected(EditorTool.ADJUST) }
        )
        ToolButton(
            icon = Icons.Default.EmojiEmotions,
            label = "Sticker",
            isSelected = activeTool == EditorTool.STICKER,
            onClick = { onToolSelected(EditorTool.STICKER) }
        )
        ToolButton(
            icon = Icons.Default.BlurOn,
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
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = tint
        )
    }
}
