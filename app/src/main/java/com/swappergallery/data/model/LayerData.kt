package com.swappergallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class LayerData {

    @Serializable
    @SerialName("text")
    data class TextData(
        val text: String,
        val fontFamily: String = "sans-serif",
        val color: Long = 0xFFFFFFFF,
        val fontSize: Float = 48f,
        val x: Float = 0.5f,       // Normalized 0-1 relative to image width
        val y: Float = 0.5f,       // Normalized 0-1 relative to image height
        val rotation: Float = 0f,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val backgroundColor: Long = 0x00000000,
        val outlineColor: Long = 0xFF000000,
        val outlineWidth: Float = 0f,
        val scale: Float = 1f
    ) : LayerData()

    @Serializable
    @SerialName("drawing")
    data class DrawingData(
        val paths: List<DrawPath> = emptyList()
    ) : LayerData()

    @Serializable
    @SerialName("draw_path")
    data class DrawPath(
        val points: List<PathPoint>,
        val color: Long = 0xFFFF0000,
        val strokeWidth: Float = 8f,
        val alpha: Float = 1f,
        val isEraser: Boolean = false
    )

    @Serializable
    @SerialName("path_point")
    data class PathPoint(
        val x: Float,   // Normalized 0-1
        val y: Float     // Normalized 0-1
    )

    @Serializable
    @SerialName("crop")
    data class CropData(
        val left: Float = 0f,     // Normalized 0-1
        val top: Float = 0f,
        val right: Float = 1f,
        val bottom: Float = 1f,
        val rotation: Float = 0f   // Degrees
    ) : LayerData()

    @Serializable
    @SerialName("filter")
    data class FilterData(
        val filterName: String,
        val intensity: Float = 1f   // 0-1
    ) : LayerData()

    @Serializable
    @SerialName("adjustment")
    data class AdjustmentData(
        val brightness: Float = 0f,     // -100 to 100
        val contrast: Float = 0f,       // -100 to 100
        val saturation: Float = 0f,     // -100 to 100
        val warmth: Float = 0f,         // -100 to 100
        val sharpness: Float = 0f,      // 0 to 100
        val highlights: Float = 0f,     // -100 to 100
        val shadows: Float = 0f,        // -100 to 100
        val vignette: Float = 0f        // 0 to 100
    ) : LayerData()

    @Serializable
    @SerialName("sticker")
    data class StickerData(
        val emoji: String,               // Emoji character or resource name
        val x: Float = 0.5f,
        val y: Float = 0.5f,
        val scale: Float = 1f,
        val rotation: Float = 0f
    ) : LayerData()

    @Serializable
    @SerialName("blur")
    data class BlurData(
        val blurType: BlurType = BlurType.FULL,
        val intensity: Float = 25f,      // Blur radius
        val centerX: Float = 0.5f,       // For radial blur
        val centerY: Float = 0.5f,
        val radius: Float = 0.3f         // Radius of clear area for radial blur (normalized)
    ) : LayerData()

    @Serializable
    enum class BlurType {
        FULL,
        RADIAL,
        LINEAR
    }
}
