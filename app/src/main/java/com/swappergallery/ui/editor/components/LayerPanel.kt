package com.swappergallery.ui.editor.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
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
    onRenameLayer: (Long, String) -> Unit,
    onMoveLayerUp: (Long) -> Unit,
    onMoveLayerDown: (Long) -> Unit,
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
                val sorted = layers.sortedByDescending { it.orderIndex }
                items(sorted, key = { it.id }) { layer ->
                    LayerItem(
                        layer = layer,
                        isSelected = layer.id == selectedLayerId,
                        onLayerClick = { onLayerClick(layer.id) },
                        onToggleVisibility = { onToggleVisibility(layer.id) },
                        onDelete = { onDeleteLayer(layer.id) },
                        onRename = { onRenameLayer(layer.id, it) },
                        onMoveUp = { onMoveLayerUp(layer.id) },
                        onMoveDown = { onMoveLayerDown(layer.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LayerItem(
    layer: EditLayer,
    isSelected: Boolean,
    onLayerClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    val displayName = layer.name.ifEmpty { layer.type.name }

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
            .combinedClickable(
                onClick = onLayerClick,
                onLongClick = { isEditing = true }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isEditing) {
                val focusRequester = remember { FocusRequester() }
                var textFieldValue by remember {
                    mutableStateOf(TextFieldValue(displayName, TextRange(displayName.length)))
                }

                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val newName = textFieldValue.text.trim()
                        if (newName.isNotEmpty()) onRename(newName)
                        isEditing = false
                    }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White, fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = Color.White
                    )
                )
            } else {
                Text(
                    text = displayName,
                    color = if (layer.visible) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
            Text(
                text = layer.type.name.lowercase().replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }

        IconButton(onClick = onMoveUp) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Move up",
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
        IconButton(onClick = onMoveDown) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Move down",
                tint = Color.White.copy(alpha = 0.6f)
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
