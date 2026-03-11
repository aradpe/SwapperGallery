package com.swappergallery.ui.editor

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    private val backupManager: BackupManager
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<UndoState>()
    private val redoStack = mutableListOf<UndoState>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun loadImage(uri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(imageUri = uri, isLoading = true)

            // Check for existing project
            val existingProject = editRepository.getProjectByUri(uri)

            if (existingProject != null && backupManager.hasBackup(existingProject.backupFileName)) {
                // Load from backup + layers
                val original = backupManager.loadBackup(existingProject.backupFileName)
                val layers = editRepository.getLayersForProject(existingProject.id)

                _uiState.value = _uiState.value.copy(
                    originalBitmap = original,
                    project = existingProject,
                    layers = layers,
                    isLoading = false
                )
                updatePreview()
            } else {
                // First time editing - create backup
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

                    _uiState.value = _uiState.value.copy(
                        originalBitmap = original,
                        project = project,
                        layers = emptyList(),
                        isLoading = false
                    )
                    updatePreview()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun selectTool(tool: EditorTool) {
        _uiState.value = _uiState.value.copy(
            activeTool = if (_uiState.value.activeTool == tool) EditorTool.NONE else tool
        )
    }

    fun selectLayer(layerId: Long?) {
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

    fun updateLayerData(layerId: Long, data: LayerData) {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return
        saveUndoState("Update ${layer.type.name.lowercase()}")

        viewModelScope.launch {
            editRepository.updateLayerData(layer, data)
            val layers = editRepository.getLayersForProject(layer.projectId)
            _uiState.value = _uiState.value.copy(
                layers = layers,
                hasUnsavedChanges = true
            )
            updatePreview()
        }
    }

    fun toggleLayerVisibility(layerId: Long) {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return
        saveUndoState("Toggle visibility")

        viewModelScope.launch {
            editRepository.updateLayer(layer.copy(visible = !layer.visible))
            val layers = editRepository.getLayersForProject(layer.projectId)
            _uiState.value = _uiState.value.copy(
                layers = layers,
                hasUnsavedChanges = true
            )
            updatePreview()
        }
    }

    fun deleteLayer(layerId: Long) {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return
        saveUndoState("Delete ${layer.type.name.lowercase()}")

        viewModelScope.launch {
            editRepository.deleteLayer(layer)
            val layers = editRepository.getLayersForProject(layer.projectId)
            _uiState.value = _uiState.value.copy(
                layers = layers,
                selectedLayerId = null,
                hasUnsavedChanges = true
            )
            updatePreview()
        }
    }

    // -- Undo / Redo --

    private fun saveUndoState(description: String) {
        undoStack.add(UndoState(_uiState.value.layers.toList(), description))
        redoStack.clear()
        if (undoStack.size > 50) undoStack.removeAt(0) // Limit undo history
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val state = undoStack.removeLast()
        redoStack.add(UndoState(_uiState.value.layers.toList(), state.description))

        viewModelScope.launch {
            val project = _uiState.value.project ?: return@launch
            // Restore layers from undo state
            editRepository.run {
                val currentLayers = getLayersForProject(project.id)
                for (layer in currentLayers) deleteLayer(layer)
                for (layer in state.layers) {
                    addLayer(project.id, layer.type,
                        json.decodeFromString<LayerData>(layer.data), layer.name)
                }
            }
            val layers = editRepository.getLayersForProject(project.id)
            _uiState.value = _uiState.value.copy(
                layers = layers,
                hasUnsavedChanges = true
            )
            updatePreview()
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val state = redoStack.removeLast()
        undoStack.add(UndoState(_uiState.value.layers.toList(), state.description))

        viewModelScope.launch {
            val project = _uiState.value.project ?: return@launch
            editRepository.run {
                val currentLayers = getLayersForProject(project.id)
                for (layer in currentLayers) deleteLayer(layer)
                for (layer in state.layers) {
                    addLayer(project.id, layer.type,
                        json.decodeFromString<LayerData>(layer.data), layer.name)
                }
            }
            val layers = editRepository.getLayersForProject(project.id)
            _uiState.value = _uiState.value.copy(
                layers = layers,
                hasUnsavedChanges = true
            )
            updatePreview()
        }
    }

    // -- Save --

    fun save() {
        val state = _uiState.value
        val original = state.originalBitmap ?: return
        val project = state.project ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)

            val composite = withContext(Dispatchers.Default) {
                ImageCompositor.composite(original, state.layers)
            }

            val uri = Uri.parse(state.imageUri)
            val mimeType = "image/jpeg" // Default; could detect from original

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
                    // Ask user for write permission via system dialog
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
        val state = _uiState.value
        val original = state.originalBitmap ?: return

        viewModelScope.launch {
            val preview = withContext(Dispatchers.Default) {
                ImageCompositor.composite(original, state.layers)
            }
            _uiState.value = _uiState.value.copy(previewBitmap = preview)
        }
    }

    // -- Drag to move --

    fun dragSelectedLayer(deltaX: Float, deltaY: Float) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val data = getLayerData(selectedId) ?: return

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

    fun commitDrag() {
        // Persist the drag result to Room
        val selectedId = _uiState.value.selectedLayerId ?: return
        val layer = _uiState.value.layers.find { it.id == selectedId } ?: return
        saveUndoState("Move ${layer.type.name.lowercase()}")
        viewModelScope.launch {
            editRepository.updateLayer(layer)
        }
    }

    // -- Helpers --

    fun getLayerData(layerId: Long): LayerData? {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return null
        return editRepository.deserializeLayerData(layer)
    }
}
