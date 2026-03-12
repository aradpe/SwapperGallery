package com.swappergallery.ui.editor

import android.content.ContentResolver
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swappergallery.data.model.EditLayer
import com.swappergallery.data.model.EditProject
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType
import com.swappergallery.data.repository.BackupManager
import com.swappergallery.data.repository.EditRepository
import com.swappergallery.data.repository.SaveResult
import com.swappergallery.util.ImageCompositor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class EditorTool {
    NONE, TEXT, DRAW, CROP, FILTER, ADJUST, STICKER, BLUR
}

data class EditorUiState(
    val imageUri: String = "",
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val project: EditProject? = null,
    val layers: List<EditLayer> = emptyList(),
    val activeTool: EditorTool = EditorTool.NONE,
    val selectedLayerId: Long? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isCompositing: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val writePermissionRequest: IntentSender? = null,
    val saveError: String? = null
)

data class UndoState(
    val layers: List<EditLayer>,
    val description: String
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val editRepository: EditRepository,
    private val backupManager: BackupManager,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<UndoState>()
    private val redoStack = mutableListOf<UndoState>()
    private var previewJob: Job? = null
    private var undoRedoJob: Job? = null

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Downscale a bitmap if it exceeds maxDim on its longest side. */
    private fun downscaleIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    fun loadImage(uri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(imageUri = uri, isLoading = true)

            try {
                // Check for existing project
                val existingProject = editRepository.getProjectByUri(uri)

                if (existingProject != null && backupManager.hasBackup(existingProject.backupFileName)) {
                    // Load from backup + layers
                    val original = backupManager.loadBackup(existingProject.backupFileName)
                    val layers = editRepository.getLayersForProject(existingProject.id)

                    val downscaled = original?.let {
                        val ds = downscaleIfNeeded(it, 2048)
                        if (ds !== it) it.recycle() // Free full-res after downscaling
                        ds
                    }

                    _uiState.value = _uiState.value.copy(
                        originalBitmap = downscaled,
                        project = existingProject,
                        layers = layers,
                        isLoading = false
                    )
                    updatePreview()
                } else {
                    // First time editing - create backup at full res, keep working copy downscaled
                    val imageUri = Uri.parse(uri)
                    val original = backupManager.loadBitmapFromUri(imageUri)

                    if (original != null) {
                        val backup = backupManager.createBackup(imageUri)
                        val project = editRepository.getOrCreateProject(
                            imageUri = uri,
                            backupFileName = backup.fileName,
                            width = backup.width,
                            height = backup.height
                        )

                        val downscaled = downscaleIfNeeded(original, 2048)
                        if (downscaled !== original) original.recycle() // Free full-res after downscaling

                        _uiState.value = _uiState.value.copy(
                            originalBitmap = downscaled,
                            project = project,
                            layers = emptyList(),
                            isLoading = false
                        )
                        updatePreview()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            saveError = "Could not load image. Please check app permissions."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    saveError = "Error loading image: ${e.message}"
                )
            }
        }
    }

    fun selectTool(tool: EditorTool) {
        undoSavedForCurrentEdit = false
        undoSavedForDrag = false
        val newTool = if (_uiState.value.activeTool == tool) EditorTool.NONE else tool
        if (newTool == EditorTool.NONE) {
            dismissTool()
            return
        }
        // If we have a selected layer, check if it matches the new tool type
        if (_uiState.value.selectedLayerId != null) {
            val selectedLayer = _uiState.value.layers.find { it.id == _uiState.value.selectedLayerId }
            val matchesTool = when (selectedLayer?.type) {
                LayerType.TEXT -> newTool == EditorTool.TEXT
                LayerType.STICKER -> newTool == EditorTool.STICKER
                LayerType.DRAWING -> newTool == EditorTool.DRAW
                LayerType.CROP -> newTool == EditorTool.CROP
                LayerType.FILTER -> newTool == EditorTool.FILTER
                LayerType.ADJUSTMENT -> newTool == EditorTool.ADJUST
                LayerType.BLUR -> newTool == EditorTool.BLUR
                null -> false
            }
            if (matchesTool) {
                // Same tool type — keep layer selected, just update tool panel
                _uiState.value = _uiState.value.copy(activeTool = newTool)
                return
            }
            // Different tool type — deselect and fall through to find/create matching layer
            _uiState.value = _uiState.value.copy(selectedLayerId = null)
        }
        // Check if there's an existing layer of this type to re-edit
        val targetType = toolToLayerType(newTool)
        if (targetType != null) {
            val existing = _uiState.value.layers.lastOrNull { it.type == targetType }
            if (existing != null) {
                _uiState.value = _uiState.value.copy(
                    activeTool = newTool,
                    selectedLayerId = existing.id
                )
                return
            }
        }
        // No existing layer — create one
        ensureLayerForTool(newTool)
    }

    private fun toolToLayerType(tool: EditorTool): LayerType? = when (tool) {
        EditorTool.CROP -> LayerType.CROP
        EditorTool.FILTER -> LayerType.FILTER
        EditorTool.ADJUST -> LayerType.ADJUSTMENT
        EditorTool.BLUR -> LayerType.BLUR
        else -> null
    }

    fun dismissTool() {
        undoSavedForCurrentEdit = false
        undoSavedForDrag = false
        _uiState.value = _uiState.value.copy(
            activeTool = EditorTool.NONE,
            selectedLayerId = null
        )
    }

    private fun ensureLayerForTool(tool: EditorTool) {
        val project = _uiState.value.project ?: return

        val triple: Triple<LayerType, LayerData, String> = when (tool) {
            EditorTool.CROP -> Triple(LayerType.CROP, LayerData.CropData(), "Crop")
            EditorTool.ADJUST -> Triple(LayerType.ADJUSTMENT, LayerData.AdjustmentData(), "Adjustment")
            EditorTool.BLUR -> Triple(LayerType.BLUR, LayerData.BlurData(intensity = 10f), "Blur")
            EditorTool.TEXT -> Triple(LayerType.TEXT, LayerData.TextData(text = "Text"), "Text")
            EditorTool.FILTER -> Triple(LayerType.FILTER, LayerData.FilterData(filterName = "none", intensity = 0f), "Filter")
            else -> {
                // DRAW, STICKER don't need auto-creation — just show the panel
                _uiState.value = _uiState.value.copy(activeTool = tool)
                return
            }
        }

        val (type, data, name) = triple
        saveUndoState("Add ${type.name.lowercase()}")

        viewModelScope.launch {
            try {
                val layer = editRepository.addLayer(project.id, type, data, name)
                val layers = editRepository.getLayersForProject(project.id)
                _uiState.value = _uiState.value.copy(
                    activeTool = tool,
                    layers = layers,
                    selectedLayerId = layer.id,
                    hasUnsavedChanges = true
                )
                updatePreview()
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    saveError = "Could not create layer: ${e.message}"
                )
            }
        }
    }

    fun selectLayer(layerId: Long?) {
        undoSavedForCurrentEdit = false
        undoSavedForDrag = false
        if (layerId != null) {
            // Auto-switch to the matching tool so drag/edit gestures work
            val layer = _uiState.value.layers.find { it.id == layerId }
            val tool = when (layer?.type) {
                LayerType.TEXT -> EditorTool.TEXT
                LayerType.STICKER -> EditorTool.STICKER
                LayerType.DRAWING -> EditorTool.DRAW
                LayerType.CROP -> EditorTool.CROP
                LayerType.FILTER -> EditorTool.FILTER
                LayerType.ADJUSTMENT -> EditorTool.ADJUST
                LayerType.BLUR -> EditorTool.BLUR
                else -> _uiState.value.activeTool
            }
            _uiState.value = _uiState.value.copy(
                selectedLayerId = layerId,
                activeTool = tool
            )
        } else {
            _uiState.value = _uiState.value.copy(selectedLayerId = layerId)
        }
    }

    // -- Layer operations --

    fun addLayer(type: LayerType, data: LayerData, name: String = "") {
        val project = _uiState.value.project ?: return
        saveUndoState("Add ${type.name.lowercase()}")

        viewModelScope.launch {
            val layer = editRepository.addLayer(project.id, type, data, name)
            val layers = editRepository.getLayersForProject(project.id)
            _uiState.value = _uiState.value.copy(
                layers = layers,
                selectedLayerId = layer.id,
                hasUnsavedChanges = true
            )
            updatePreview()
        }
    }

    private var updateJob: Job? = null

    // Track whether an undo snapshot has been taken for the current edit gesture.
    // Reset when tool is dismissed or a different layer is selected.
    private var undoSavedForCurrentEdit = false

    // Track whether an undo snapshot has been taken for the current drag/transform gesture.
    // Reset when the gesture ends (commitDrag) or selection changes.
    private var undoSavedForDrag = false

    fun updateLayerData(layerId: Long, data: LayerData) {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return
        // Only save undo state once per edit gesture (not every slider tick)
        if (!undoSavedForCurrentEdit) {
            saveUndoState("Update ${layer.type.name.lowercase()}")
            undoSavedForCurrentEdit = true
        }

        // Update in-memory immediately so preview uses latest state (no race)
        val updatedLayer = layer.copy(data = editRepository.serializeLayerData(data))
        val updatedLayers = _uiState.value.layers.map { if (it.id == layerId) updatedLayer else it }
        _uiState.value = _uiState.value.copy(
            layers = updatedLayers,
            hasUnsavedChanges = true
        )
        updatePreview()

        // Persist to DB in background (cancel previous pending write)
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            try {
                editRepository.updateLayerData(layer, data)
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun toggleLayerVisibility(layerId: Long) {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return
        saveUndoState("Toggle visibility")

        // Update in-memory immediately for responsive UI
        val updatedLayer = layer.copy(visible = !layer.visible)
        val updatedLayers = _uiState.value.layers.map { if (it.id == layerId) updatedLayer else it }
        _uiState.value = _uiState.value.copy(
            layers = updatedLayers,
            hasUnsavedChanges = true
        )
        updatePreview()

        // Persist to DB in background
        viewModelScope.launch {
            editRepository.updateLayer(updatedLayer)
        }
    }

    fun deleteLayer(layerId: Long) {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return
        saveUndoState("Delete ${layer.type.name.lowercase()}")

        // If the deleted layer is currently selected, dismiss tool panel too
        val wasSelected = _uiState.value.selectedLayerId == layerId

        // Update in-memory immediately for responsive UI
        val updatedLayers = _uiState.value.layers.filter { it.id != layerId }
        _uiState.value = _uiState.value.copy(
            layers = updatedLayers,
            selectedLayerId = null,
            activeTool = if (wasSelected) EditorTool.NONE else _uiState.value.activeTool,
            hasUnsavedChanges = true
        )
        updatePreview()

        // Persist to DB in background
        viewModelScope.launch {
            editRepository.deleteLayer(layer)
        }
    }

    // -- Undo / Redo --

    private fun saveUndoState(description: String) {
        undoStack.add(UndoState(_uiState.value.layers.toList(), description))
        redoStack.clear()
        if (undoStack.size > 50) undoStack.removeAt(0) // Limit undo history
    }

    fun undo() {
        if (undoStack.isEmpty() || undoRedoJob?.isActive == true) return
        val state = undoStack.removeLast()
        redoStack.add(UndoState(_uiState.value.layers.toList(), state.description))

        // Immediately update in-memory state so UI reflects the change
        _uiState.value = _uiState.value.copy(
            layers = state.layers,
            selectedLayerId = null,
            activeTool = EditorTool.NONE,
            hasUnsavedChanges = true
        )
        updatePreview()

        // Persist to DB in background (preserves orderIndex, visible, name)
        undoRedoJob = viewModelScope.launch {
            val project = _uiState.value.project ?: return@launch
            editRepository.restoreLayers(project.id, state.layers)
            // Re-read layers to sync IDs from DB
            val layers = editRepository.getLayersForProject(project.id)
            _uiState.value = _uiState.value.copy(layers = layers)
        }
    }

    fun redo() {
        if (redoStack.isEmpty() || undoRedoJob?.isActive == true) return
        val state = redoStack.removeLast()
        undoStack.add(UndoState(_uiState.value.layers.toList(), state.description))

        // Immediately update in-memory state
        _uiState.value = _uiState.value.copy(
            layers = state.layers,
            selectedLayerId = null,
            activeTool = EditorTool.NONE,
            hasUnsavedChanges = true
        )
        updatePreview()

        // Persist to DB in background (preserves orderIndex, visible, name)
        undoRedoJob = viewModelScope.launch {
            val project = _uiState.value.project ?: return@launch
            editRepository.restoreLayers(project.id, state.layers)
            val layers = editRepository.getLayersForProject(project.id)
            _uiState.value = _uiState.value.copy(layers = layers)
        }
    }

    // -- Save --

    fun save() {
        // Commit any pending drawing paths by switching away from draw tool
        if (_uiState.value.activeTool == EditorTool.DRAW) {
            dismissTool()
        }

        viewModelScope.launch {
            // Brief delay to allow LaunchedEffect to commit pending drawings
            kotlinx.coroutines.delay(100)

            val state = _uiState.value
            val project = state.project ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            try {
                // Load full-res original from backup for final save quality
                val fullResOriginal = withContext(Dispatchers.IO) {
                    backupManager.loadBackup(project.backupFileName)
                } ?: state.originalBitmap
                if (fullResOriginal == null) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = "Could not load original image for saving."
                    )
                    return@launch
                }

                val composite = withContext(Dispatchers.Default) {
                    ImageCompositor.composite(fullResOriginal, state.layers)
                }

                val uri = Uri.parse(state.imageUri)
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

                when (val result = backupManager.saveBitmapToUri(composite, uri, mimeType)) {
                    is SaveResult.Success -> {
                        editRepository.updateProject(
                            project.copy(updatedAt = System.currentTimeMillis())
                        )
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            hasUnsavedChanges = false
                        )
                    }
                    is SaveResult.NeedsWriteAccess -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            writePermissionRequest = result.intentSender
                        )
                    }
                    is SaveResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveError = "Failed to save: ${result.error.message}"
                        )
                    }
                }

                // Recycle full-res bitmaps to free memory after saving
                if (composite !== fullResOriginal) composite.recycle()
                if (fullResOriginal !== state.originalBitmap) fullResOriginal.recycle()
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = "Save failed: ${e.message}"
                )
            }
        }
    }

    fun onWritePermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(writePermissionRequest = null)
        if (granted) {
            save() // Retry now that we have permission
        } else {
            _uiState.value = _uiState.value.copy(
                saveError = "Write permission denied. Cannot save to original file."
            )
        }
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveError = null)
    }

    // -- Preview --

    private fun updatePreview() {
        val original = _uiState.value.originalBitmap ?: return

        // Cancel any in-flight preview to avoid concurrent OOM
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            // Brief delay so rapid changes don't all trigger heavy compositing
            kotlinx.coroutines.delay(50)
            // Read layers AFTER the delay to get the most up-to-date state
            val layers = _uiState.value.layers
            _uiState.value = _uiState.value.copy(isCompositing = true)
            val preview = withContext(Dispatchers.Default) {
                try {
                    // originalBitmap is already capped at 2048px from loadImage()
                    ImageCompositor.composite(original, layers)
                } catch (_: Throwable) {
                    null
                }
            }
            if (preview != null) {
                val old = _uiState.value.previewBitmap
                _uiState.value = _uiState.value.copy(previewBitmap = preview, isCompositing = false)
                // Recycle old preview to free memory. Use a short delay to ensure
                // the render thread has finished drawing the old bitmap before recycling.
                if (old != null && old !== preview && old !== _uiState.value.originalBitmap) {
                    val toRecycle = old
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(200)
                        toRecycle.recycle()
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(isCompositing = false)
            }
        }
    }

    // -- Drag to move --

    fun dragSelectedLayer(deltaX: Float, deltaY: Float) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val data = getLayerData(selectedId) ?: return

        // Save undo state once at the start of the drag gesture (before any changes)
        if (!undoSavedForDrag) {
            val layer = _uiState.value.layers.find { it.id == selectedId } ?: return
            saveUndoState("Move ${layer.type.name.lowercase()}")
            undoSavedForDrag = true
        }

        val updated = when (data) {
            is LayerData.TextData -> data.copy(
                x = (data.x + deltaX).coerceIn(0f, 1f),
                y = (data.y + deltaY).coerceIn(0f, 1f)
            )
            is LayerData.StickerData -> data.copy(
                x = (data.x + deltaX).coerceIn(0f, 1f),
                y = (data.y + deltaY).coerceIn(0f, 1f)
            )
            else -> return
        }

        // Update in-memory immediately for smooth dragging (no undo state per drag frame)
        val layer = _uiState.value.layers.find { it.id == selectedId } ?: return
        val updatedLayer = layer.copy(data = editRepository.serializeLayerData(updated))
        val updatedLayers = _uiState.value.layers.map { if (it.id == selectedId) updatedLayer else it }
        _uiState.value = _uiState.value.copy(
            layers = updatedLayers,
            hasUnsavedChanges = true
        )
        updatePreview()
    }

    fun transformSelectedLayer(panX: Float, panY: Float, zoomDelta: Float, rotationDelta: Float) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val data = getLayerData(selectedId) ?: return

        // Save undo state once at the start of the transform gesture (before any changes)
        if (!undoSavedForDrag) {
            val layer = _uiState.value.layers.find { it.id == selectedId } ?: return
            saveUndoState("Transform ${layer.type.name.lowercase()}")
            undoSavedForDrag = true
        }

        val updated = when (data) {
            is LayerData.TextData -> data.copy(
                x = (data.x + panX).coerceIn(0f, 1f),
                y = (data.y + panY).coerceIn(0f, 1f),
                scale = (data.scale * zoomDelta).coerceIn(0.1f, 10f),
                rotation = data.rotation + rotationDelta
            )
            is LayerData.StickerData -> data.copy(
                x = (data.x + panX).coerceIn(0f, 1f),
                y = (data.y + panY).coerceIn(0f, 1f),
                scale = (data.scale * zoomDelta).coerceIn(0.1f, 10f),
                rotation = data.rotation + rotationDelta
            )
            else -> return
        }

        val layer = _uiState.value.layers.find { it.id == selectedId } ?: return
        val updatedLayer = layer.copy(data = editRepository.serializeLayerData(updated))
        val updatedLayers = _uiState.value.layers.map { if (it.id == selectedId) updatedLayer else it }
        _uiState.value = _uiState.value.copy(
            layers = updatedLayers,
            hasUnsavedChanges = true
        )
        updatePreview()
    }

    fun commitDrag() {
        // Persist the drag result to Room (undo state was already saved at gesture start)
        val selectedId = _uiState.value.selectedLayerId ?: return
        val layer = _uiState.value.layers.find { it.id == selectedId } ?: return
        undoSavedForDrag = false
        viewModelScope.launch {
            editRepository.updateLayer(layer)
        }
    }

    // -- Canvas tap / element cycling --

    private var lastTapLayers: List<Long> = emptyList()
    private var lastTapCycleIndex: Int = 0

    fun handleCanvasTap(normalizedX: Float, normalizedY: Float) {
        val hitRadius = 0.08f
        val hitLayers = _uiState.value.layers.filter { layer ->
            if (!layer.visible) return@filter false
            val data = editRepository.deserializeLayerData(layer)
            when (data) {
                is LayerData.TextData -> {
                    val dx = data.x - normalizedX
                    val dy = data.y - normalizedY
                    kotlin.math.sqrt((dx * dx + dy * dy).toDouble()) < hitRadius * kotlin.math.max(data.scale, 1f)
                }
                is LayerData.StickerData -> {
                    val dx = data.x - normalizedX
                    val dy = data.y - normalizedY
                    kotlin.math.sqrt((dx * dx + dy * dy).toDouble()) < hitRadius * kotlin.math.max(data.scale, 1f)
                }
                else -> false
            }
        }

        if (hitLayers.isEmpty()) {
            // Tapped empty space: deselect current element (instead of teleporting)
            if (_uiState.value.selectedLayerId != null) {
                dismissTool()
            }
            lastTapLayers = emptyList()
            lastTapCycleIndex = 0
            return
        }

        val hitLayerIds = hitLayers.map { it.id }
        if (hitLayerIds == lastTapLayers) {
            lastTapCycleIndex = (lastTapCycleIndex + 1) % hitLayerIds.size
        } else {
            lastTapLayers = hitLayerIds
            lastTapCycleIndex = 0
        }
        selectLayer(hitLayerIds[lastTapCycleIndex])
    }

    fun renameLayer(layerId: Long, newName: String) {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return
        val updatedLayer = layer.copy(name = newName, updatedAt = System.currentTimeMillis())
        val updatedLayers = _uiState.value.layers.map { if (it.id == layerId) updatedLayer else it }
        _uiState.value = _uiState.value.copy(layers = updatedLayers)
        viewModelScope.launch {
            editRepository.updateLayer(updatedLayer)
        }
    }

    // -- Layer reordering --

    fun moveLayerUp(layerId: Long) {
        val layers = _uiState.value.layers.sortedBy { it.orderIndex }
        val idx = layers.indexOfFirst { it.id == layerId }
        if (idx < 0 || idx >= layers.size - 1) return // Already at top or not found

        saveUndoState("Reorder layer")
        val a = layers[idx]
        val b = layers[idx + 1]
        val updatedA = a.copy(orderIndex = b.orderIndex)
        val updatedB = b.copy(orderIndex = a.orderIndex)

        val updatedLayers = _uiState.value.layers.map { layer ->
            when (layer.id) {
                a.id -> updatedA
                b.id -> updatedB
                else -> layer
            }
        }
        _uiState.value = _uiState.value.copy(layers = updatedLayers, hasUnsavedChanges = true)
        updatePreview()

        viewModelScope.launch {
            editRepository.updateLayer(updatedA)
            editRepository.updateLayer(updatedB)
        }
    }

    fun moveLayerDown(layerId: Long) {
        val layers = _uiState.value.layers.sortedBy { it.orderIndex }
        val idx = layers.indexOfFirst { it.id == layerId }
        if (idx <= 0) return // Already at bottom or not found

        saveUndoState("Reorder layer")
        val a = layers[idx]
        val b = layers[idx - 1]
        val updatedA = a.copy(orderIndex = b.orderIndex)
        val updatedB = b.copy(orderIndex = a.orderIndex)

        val updatedLayers = _uiState.value.layers.map { layer ->
            when (layer.id) {
                a.id -> updatedA
                b.id -> updatedB
                else -> layer
            }
        }
        _uiState.value = _uiState.value.copy(layers = updatedLayers, hasUnsavedChanges = true)
        updatePreview()

        viewModelScope.launch {
            editRepository.updateLayer(updatedA)
            editRepository.updateLayer(updatedB)
        }
    }

    // -- Helpers --

    fun getLayerData(layerId: Long): LayerData? {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return null
        return editRepository.deserializeLayerData(layer)
    }
}
