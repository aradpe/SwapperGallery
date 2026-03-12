package com.swappergallery.data.repository

import com.swappergallery.data.db.EditProjectDao
import com.swappergallery.data.model.EditLayer
import com.swappergallery.data.model.EditProject
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditRepository @Inject constructor(
    private val dao: EditProjectDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun getOrCreateProject(
        imageUri: String,
        backupFileName: String,
        width: Int,
        height: Int
    ): EditProject {
        val existing = dao.getProjectByUri(imageUri)
        if (existing != null) return existing

        val project = EditProject(
            imageUri = imageUri,
            backupFileName = backupFileName,
            originalWidth = width,
            originalHeight = height
        )
        val id = dao.insertProject(project)
        return project.copy(id = id)
    }

    suspend fun getProjectByUri(uri: String): EditProject? = dao.getProjectByUri(uri)

    suspend fun getProjectById(id: Long): EditProject? = dao.getProjectById(id)

    suspend fun updateProject(project: EditProject) = dao.updateProject(project)

    suspend fun deleteProject(project: EditProject) = dao.deleteProjectAndLayers(project)

    suspend fun getAllEditedUris(): Set<String> = dao.getAllEditedUris().toSet()

    fun observeAllProjects(): Flow<List<EditProject>> = dao.getAllProjects()

    // -- Layer operations --

    suspend fun addLayer(projectId: Long, type: LayerType, data: LayerData, name: String = ""): EditLayer {
        val orderIndex = dao.getNextOrderIndex(projectId)
        val layerName = name.ifEmpty {
            when (type) {
                LayerType.TEXT -> "Text"
                LayerType.DRAWING -> "Drawing"
                LayerType.CROP -> "Crop"
                LayerType.FILTER -> "Filter"
                LayerType.ADJUSTMENT -> "Adjustment"
                LayerType.STICKER -> "Sticker"
                LayerType.BLUR -> "Blur"
            }
        }
        val layer = EditLayer(
            projectId = projectId,
            type = type,
            data = json.encodeToString(data),
            orderIndex = orderIndex,
            name = layerName
        )
        val id = dao.insertLayer(layer)
        return layer.copy(id = id)
    }

    suspend fun updateLayer(layer: EditLayer) {
        dao.updateLayer(layer.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateLayerData(layer: EditLayer, data: LayerData) {
        dao.updateLayer(
            layer.copy(
                data = json.encodeToString(data),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteLayer(layer: EditLayer) = dao.deleteLayer(layer)

    suspend fun getLayersForProject(projectId: Long): List<EditLayer> =
        dao.getLayersForProject(projectId)

    /**
     * Replace all layers for a project with the given list.
     * Preserves all metadata (orderIndex, visible, name, etc.) unlike addLayer().
     * Used by undo/redo to restore exact layer state.
     */
    suspend fun restoreLayers(projectId: Long, layers: List<EditLayer>) {
        dao.deleteAllLayersForProject(projectId)
        // Reset IDs so Room auto-generates new ones, but preserve all other fields
        val layersToInsert = layers.map { it.copy(id = 0) }
        dao.insertLayers(layersToInsert)
    }

    fun observeLayers(projectId: Long): Flow<List<EditLayer>> =
        dao.observeLayersForProject(projectId)

    fun deserializeLayerData(layer: EditLayer): LayerData {
        return json.decodeFromString<LayerData>(layer.data)
    }

    fun serializeLayerData(data: LayerData): String {
        return json.encodeToString(data)
    }
}
