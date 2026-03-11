package com.swappergallery.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.swappergallery.data.model.EditLayer
import com.swappergallery.data.model.EditProject

@Database(
    entities = [EditProject::class, EditLayer::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun editProjectDao(): EditProjectDao
}
