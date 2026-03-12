package com.swappergallery.data.db

import androidx.room.TypeConverter
import com.swappergallery.data.model.LayerType

class Converters {
    @TypeConverter
    fun fromLayerType(value: LayerType): String = value.name

    @TypeConverter
    fun toLayerType(value: String): LayerType = try {
        LayerType.valueOf(value)
    } catch (_: IllegalArgumentException) {
        LayerType.ADJUSTMENT // Fallback for unknown types
    }
}
