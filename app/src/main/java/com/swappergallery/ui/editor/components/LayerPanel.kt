package com.swappergallery.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swappergallery.data.model.EditLayer
import com.swappergallery.ui.theme.EditorSurface

@Composable
fun LayerPanel(
    layers: List<EditLayer>,
    selectedLayerId: Long?,
    onLayerClick: (Long) -> Unit,
    onToggleVisibility: (Long) -> Unit,
    onDeleteLayer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EditorSurface)
            .padding(8.dp)
    ) {
        Text(
            text = "Layers (${layers.size})",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (layers.isEmpty()) {
            Text(
                text = "No layers yet. Use the tools below to add edits.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(layers.reversed(), key = { it.id }) { layer ->
                    LayerItem(
                        layer = layer,
                        isSelected = layer.id == selectedLayerId,
                        onLayerClick = { onLayerClick(layer.id) },
                        onToggleVisibility = { onToggleVisibility(layer.id) },
                        onDelete = { onDeleteLayer(layer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerItem(
    layer: EditLayer,
    isSelected: Boolean,
    onLayerClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        Color.White.copy(alpha = 0.05f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onLayerClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = layer.name.ifEmpty { layer.type.name },
                color = if (layer.visible) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
            Text(
                text = layer.type.name.lowercase().replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }

        IconButton(onClick = onToggleVisibility) {
            Icon(
                imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle visibility",
                tint = if (layer.visible) Color.White else Color.White.copy(alpha = 0.3f)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete layer",
                tint = Color.Red.copy(alpha = 0.7f)
            )
        }
    }
}
