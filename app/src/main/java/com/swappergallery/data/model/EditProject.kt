package com.swappergallery.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edit_projects")
data class EditProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val backupFileName: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
