package com.swappergallery.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.swappergallery.data.model.EditLayer
import com.swappergallery.data.model.EditProject
import kotlinx.coroutines.flow.Flow

@Dao
interface EditProjectDao {

    // -- EditProject --

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: EditProject): Long

    @Update
    suspend fun updateProject(project: EditProject)

    @Delete
    suspend fun deleteProject(project: EditProject)

    @Query("SELECT * FROM edit_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): EditProject?

    @Query("SELECT * FROM edit_projects WHERE imageUri = :uri LIMIT 1")
    suspend fun getProjectByUri(uri: String): EditProject?

    @Query("SELECT * FROM edit_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<EditProject>>

    @Query("SELECT imageUri FROM edit_projects")
    suspend fun getAllEditedUris(): List<String>

    // -- EditLayer --

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: EditLayer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayers(layers: List<EditLayer>)

    @Update
    suspend fun updateLayer(layer: EditLayer)

    @Delete
    suspend fun deleteLayer(layer: EditLayer)

    @Query("SELECT * FROM edit_layers WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun getLayersForProject(projectId: Long): List<EditLayer>

    @Query("SELECT * FROM edit_layers WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun observeLayersForProject(projectId: Long): Flow<List<EditLayer>>

    @Query("DELETE FROM edit_layers WHERE projectId = :projectId")
    suspend fun deleteAllLayersForProject(projectId: Long)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM edit_layers WHERE projectId = :projectId")
    suspend fun getNextOrderIndex(projectId: Long): Int

    // -- Combined --

    @Transaction
    suspend fun deleteProjectAndLayers(project: EditProject) {
        deleteAllLayersForProject(project.id)
        deleteProject(project)
    }

    /**
     * Atomically replace all layers for a project.
     * Used by undo/redo to ensure no data loss if app crashes mid-operation.
     */
    @Transaction
    suspend fun replaceAllLayers(projectId: Long, layers: List<EditLayer>) {
        deleteAllLayersForProject(projectId)
        insertLayers(layers)
    }
}
