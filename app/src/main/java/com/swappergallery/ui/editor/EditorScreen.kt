package com.swappergallery.ui.editor

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType
import com.swappergallery.ui.editor.components.EditorToolBar
import com.swappergallery.ui.editor.components.LayerPanel
import com.swappergallery.ui.editor.tools.AdjustmentToolPanel
import com.swappergallery.ui.editor.tools.BlurToolPanel
import com.swappergallery.ui.editor.tools.CropToolPanel
import com.swappergallery.ui.editor.tools.DrawToolPanel
import com.swappergallery.ui.editor.tools.DrawToolState
import com.swappergallery.ui.editor.tools.FilterToolPanel
import com.swappergallery.ui.editor.tools.StickerToolPanel
import com.swappergallery.ui.editor.tools.TextToolPanel
import com.swappergallery.ui.theme.EditorBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageUri: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLayers by remember { mutableStateOf(false) }
    var drawState by remember { mutableStateOf(DrawToolState()) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(imageUri) {
        viewModel.loadImage(imageUri)
    }

    // Auto-close layer panel when a tool panel opens to maximize canvas space
    LaunchedEffect(uiState.activeTool) {
        if (uiState.activeTool != EditorTool.NONE) {
            showLayers = false
        }
    }

    // Handle write permission request from the system
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onWritePermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(uiState.writePermissionRequest) {
        uiState.writePermissionRequest?.let { intentSender ->
            writePermissionLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }
    }

    // Show save errors via snackbar
    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearSaveError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (viewModel.canUndo) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.canRedo
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (viewModel.canRedo) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(onClick = {
                        showLayers = !showLayers
                        if (showLayers) viewModel.selectTool(EditorTool.NONE)
                    }) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = "Layers",
                            tint = if (showLayers) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = uiState.hasUnsavedChanges && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save",
                                tint = if (uiState.hasUnsavedChanges) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EditorBackground
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EditorBackground)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorBackground)
                .padding(padding)
        ) {
            // Layer panel (collapsible)
            AnimatedVisibility(
                visible = showLayers,
                enter = slideInVertically(),
                exit = slideOutVertically()
            ) {
                LayerPanel(
                    layers = uiState.layers,
                    selectedLayerId = uiState.selectedLayerId,
                    onLayerClick = { viewModel.selectLayer(it) },
                    onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                    onDeleteLayer = { viewModel.deleteLayer(it) },
                    onRenameLayer = { id, name -> viewModel.renameLayer(id, name) },
                    modifier = Modifier.heightIn(max = 200.dp)
                )
            }

            // Canvas area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                EditorCanvas(
                    previewBitmap = uiState.previewBitmap,
                    activeTool = uiState.activeTool,
                    drawState = drawState,
                    hasSelectedLayer = uiState.selectedLayerId != null,
                    onDrawingComplete = { paths ->
                        if (paths.isNotEmpty()) {
                            viewModel.addLayer(
                                LayerType.DRAWING,
                                LayerData.DrawingData(paths = paths),
                                "Drawing"
                            )
                        }
                    },
                    onLayerTransform = { panX, panY, zoom, rotation ->
                        viewModel.transformSelectedLayer(panX, panY, zoom, rotation)
                    },
                    onLayerDragEnd = {
                        viewModel.commitDrag()
                    },
                    onLayerTap = { x, y ->
                        viewModel.handleCanvasTap(x, y)
                    }
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Tool bar
            EditorToolBar(
                activeTool = uiState.activeTool,
                onToolSelected = { viewModel.selectTool(it) }
            )

            // Tool-specific panel
            AnimatedVisibility(
                visible = uiState.activeTool != EditorTool.NONE,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .background(EditorBackground)
                ) {
                    // Done header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.activeTool.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
                        )
                        IconButton(onClick = { viewModel.dismissTool() }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    val selectedLayerData = uiState.selectedLayerId?.let { viewModel.getLayerData(it) }

                    when (uiState.activeTool) {
                        EditorTool.TEXT -> {
                            TextToolPanel(
                                existingData = selectedLayerData as? LayerData.TextData,
                                onUpdateText = { data ->
                                    uiState.selectedLayerId?.let { viewModel.updateLayerData(it, data) }
                                }
                            )
                        }
                        EditorTool.DRAW -> {
                            DrawToolPanel(
                                drawState = drawState,
                                onStateChange = { drawState = it }
                            )
                        }
                        EditorTool.CROP -> {
                            CropToolPanel(
                                existingData = selectedLayerData as? LayerData.CropData,
                                onUpdateCrop = { data ->
                                    uiState.selectedLayerId?.let { viewModel.updateLayerData(it, data) }
                                }
                            )
                        }
                        EditorTool.FILTER -> {
                            FilterToolPanel(
                                existingData = selectedLayerData as? LayerData.FilterData,
                                onUpdateFilter = { data ->
                                    uiState.selectedLayerId?.let { viewModel.updateLayerData(it, data) }
                                }
                            )
                        }
                        EditorTool.ADJUST -> {
                            AdjustmentToolPanel(
                                existingData = selectedLayerData as? LayerData.AdjustmentData,
                                onUpdateAdjustment = { data ->
                                    uiState.selectedLayerId?.let { viewModel.updateLayerData(it, data) }
                                }
                            )
                        }
                        EditorTool.STICKER -> {
                            StickerToolPanel(
                                onAddSticker = { type, data ->
                                    viewModel.addLayer(type, data)
                                }
                            )
                        }
                        EditorTool.BLUR -> {
                            BlurToolPanel(
                                existingData = selectedLayerData as? LayerData.BlurData,
                                onUpdateBlur = { data ->
                                    uiState.selectedLayerId?.let { viewModel.updateLayerData(it, data) }
                                }
                            )
                        }
                        EditorTool.NONE -> { /* No panel */ }
                    }
                }
            }
        }
    }
}
