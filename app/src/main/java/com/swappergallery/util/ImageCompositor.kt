package com.swappergallery.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.swappergallery.data.model.EditLayer
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType
import kotlinx.serialization.json.Json

object ImageCompositor {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun composite(original: Bitmap, layers: List<EditLayer>): Bitmap {
        var current = original.copy(Bitmap.Config.ARGB_8888, true)
            ?: return original // copy() can return null on low memory

        // Each pass has its own try-catch so a failing filter/blur
        // doesn't prevent drawings/stickers from rendering.

        // First pass: apply crop if present
        try {
            val cropLayer = layers.firstOrNull { it.type == LayerType.CROP && it.visible }
            if (cropLayer != null) {
                val cropData = json.decodeFromString<LayerData>(cropLayer.data) as? LayerData.CropData
                if (cropData != null) {
                    val prev = current
                    current = applyCrop(current, cropData)
                    if (current !== prev && prev !== original) prev.recycle()
                }
            }
        } catch (_: Throwable) { /* skip crop on error */ }

        // Second pass: apply adjustments, filters, blur (each layer individually)
        for (layer in layers.sortedBy { it.orderIndex }) {
            if (!layer.visible) continue
            try {
                val data = json.decodeFromString<LayerData>(layer.data)
                val prev = current
                current = when (layer.type) {
                    LayerType.ADJUSTMENT -> applyAdjustment(current, data as LayerData.AdjustmentData)
                    LayerType.FILTER -> applyFilter(current, data as LayerData.FilterData)
                    LayerType.BLUR -> applyBlur(current, data as LayerData.BlurData)
                    else -> current
                }
                // Recycle old bitmap if a new one was created (free memory for next layer)
                if (current !== prev && prev !== original) prev.recycle()
            } catch (_: Throwable) { /* skip this layer on error, continue with next */ }
        }

        // Third pass: draw overlays (drawing, text, stickers)
        try {
            val canvas = Canvas(current)
            for (layer in layers.sortedBy { it.orderIndex }) {
                if (!layer.visible) continue
                try {
                    val data = json.decodeFromString<LayerData>(layer.data)
                    when (layer.type) {
                        LayerType.DRAWING -> drawDrawing(canvas, current.width, current.height, data as LayerData.DrawingData)
                        LayerType.TEXT -> drawText(canvas, current.width, current.height, data as LayerData.TextData)
                        LayerType.STICKER -> drawSticker(canvas, current.width, current.height, data as LayerData.StickerData)
                        else -> { /* Already applied */ }
                    }
                } catch (_: Throwable) { /* skip this overlay on error */ }
            }
        } catch (_: Throwable) { /* canvas creation failed */ }

        return current
    }

    private fun applyCrop(bitmap: Bitmap, crop: LayerData.CropData): Bitmap {
        val x = (crop.left * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val y = (crop.top * bitmap.height).toInt().coerceIn(0, bitmap.height)
        val w = ((crop.right - crop.left) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val h = ((crop.bottom - crop.top) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)

        if (crop.rotation != 0f) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(crop.rotation)
            val rotated = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
            if (rotated !== cropped) cropped.recycle() // Recycle intermediate bitmap
            return rotated
        }
        return cropped
    }

    private fun applyAdjustment(bitmap: Bitmap, adj: LayerData.AdjustmentData): Bitmap {
        // Skip entirely if all adjustments are at default values
        if (adj.brightness == 0f && adj.contrast == 0f && adj.saturation == 0f &&
            adj.warmth == 0f && adj.highlights == 0f && adj.shadows == 0f &&
            adj.sharpness == 0f && adj.vignette == 0f) {
            return bitmap
        }
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: return bitmap
        val canvas = Canvas(result)
        val paint = Paint()

        val cm = ColorMatrix()

        // Brightness: shift RGB values
        if (adj.brightness != 0f) {
            val b = adj.brightness * 2.55f  // Map -100..100 to -255..255
            val brightnessMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, b,
                    0f, 1f, 0f, 0f, b,
                    0f, 0f, 1f, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(brightnessMatrix)
        }

        // Contrast
        if (adj.contrast != 0f) {
            val c = (adj.contrast / 100f) + 1f  // 0 to 2
            val t = (-0.5f * c + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(contrastMatrix)
        }

        // Saturation
        if (adj.saturation != 0f) {
            val s = (adj.saturation / 100f) + 1f  // 0 to 2
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(s)
            cm.postConcat(satMatrix)
        }

        // Warmth (shift red up and blue down, or vice versa)
        if (adj.warmth != 0f) {
            val w = adj.warmth * 1.5f  // Scale the effect
            val warmthMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, w,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, -w,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(warmthMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // Highlights / Shadows (selective tonal adjustment)
        if (adj.highlights != 0f || adj.shadows != 0f) {
            val pixels = IntArray(result.width * result.height)
            result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
            val sAdj = adj.shadows / 100f
            val hAdj = adj.highlights / 100f
            for (i in pixels.indices) {
                val px = pixels[i]
                var r = Color.red(px); var g = Color.green(px); var b = Color.blue(px)
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                if (sAdj != 0f) {
                    val w = (1f - lum) * (1f - lum)
                    val d = (sAdj * w * 80f).toInt()
                    r = (r + d).coerceIn(0, 255); g = (g + d).coerceIn(0, 255); b = (b + d).coerceIn(0, 255)
                }
                if (hAdj != 0f) {
                    val w = lum * lum
                    val d = (hAdj * w * 80f).toInt()
                    r = (r + d).coerceIn(0, 255); g = (g + d).coerceIn(0, 255); b = (b + d).coerceIn(0, 255)
                }
                pixels[i] = Color.argb(Color.alpha(px), r, g, b)
            }
            result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        }

        // Sharpness (unsharp mask)
        if (adj.sharpness > 0f) {
            val blurred = simpleBlur(result, 2f)
            val amount = adj.sharpness / 50f
            val pixels = IntArray(result.width * result.height)
            val blurPixels = IntArray(result.width * result.height)
            result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
            blurred.getPixels(blurPixels, 0, result.width, 0, 0, result.width, result.height)
            for (i in pixels.indices) {
                val o = pixels[i]; val bl = blurPixels[i]
                val r = (Color.red(o) + amount * (Color.red(o) - Color.red(bl))).toInt().coerceIn(0, 255)
                val g = (Color.green(o) + amount * (Color.green(o) - Color.green(bl))).toInt().coerceIn(0, 255)
                val b = (Color.blue(o) + amount * (Color.blue(o) - Color.blue(bl))).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(Color.alpha(o), r, g, b)
            }
            result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
            blurred.recycle()
        }

        // Vignette
        if (adj.vignette > 0f) {
            applyVignette(canvas, result.width, result.height, adj.vignette / 100f)
        }

        return result
    }

    private fun applyVignette(canvas: Canvas, width: Int, height: Int, intensity: Float) {
        val paint = Paint()
        val cx = width / 2f
        val cy = height / 2f
        val radius = Math.sqrt((cx * cx + cy * cy).toDouble()).toFloat()

        val gradient = RadialGradient(
            cx, cy, radius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((intensity * 80).toInt(), 0, 0, 0),
                Color.argb((intensity * 255).toInt(), 0, 0, 0)
            ),
            floatArrayOf(0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun applyFilter(bitmap: Bitmap, filter: LayerData.FilterData): Bitmap {
        val cm = getFilterColorMatrix(filter.filterName)
        // Skip entirely if no filter applied or intensity is zero
        if (cm == null || filter.intensity <= 0f) {
            return bitmap
        }
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: return bitmap
        val canvas = Canvas(result)
        val paint = Paint()

        // Blend between identity and filter matrix based on intensity
        val identity = ColorMatrix()
        val blended = blendColorMatrix(identity, cm, filter.intensity)
        paint.colorFilter = ColorMatrixColorFilter(blended)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    private fun getFilterColorMatrix(name: String): ColorMatrix? {
        return when (name.lowercase()) {
            "grayscale", "b&w" -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                cm
            }
            "sepia" -> {
                ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "vintage" -> {
                ColorMatrix(
                    floatArrayOf(
                        0.9f, 0.5f, 0.1f, 0f, 20f,
                        0.3f, 0.8f, 0.1f, 0f, 10f,
                        0.2f, 0.3f, 0.5f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "vivid" -> {
                val cm = ColorMatrix()
                cm.setSaturation(1.8f)
                val contrastCm = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, -25f,
                        0f, 1.2f, 0f, 0f, -25f,
                        0f, 0f, 1.2f, 0f, -25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastCm)
                cm
            }
            "cool" -> {
                ColorMatrix(
                    floatArrayOf(
                        0.9f, 0f, 0f, 0f, -10f,
                        0f, 0.95f, 0f, 0f, 0f,
                        0f, 0f, 1.1f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "warm" -> {
                ColorMatrix(
                    floatArrayOf(
                        1.1f, 0f, 0f, 0f, 15f,
                        0f, 1.0f, 0f, 0f, 5f,
                        0f, 0f, 0.9f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "noir" -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val contrastCm = ColorMatrix(
                    floatArrayOf(
                        1.5f, 0f, 0f, 0f, -60f,
                        0f, 1.5f, 0f, 0f, -60f,
                        0f, 0f, 1.5f, 0f, -60f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastCm)
                cm
            }
            "fade" -> {
                ColorMatrix(
                    floatArrayOf(
                        0.9f, 0.05f, 0.05f, 0f, 20f,
                        0.05f, 0.85f, 0.05f, 0f, 20f,
                        0.05f, 0.05f, 0.8f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "dramatic" -> {
                val cm = ColorMatrix()
                cm.setSaturation(0.7f)
                val contrastCm = ColorMatrix(
                    floatArrayOf(
                        1.4f, 0f, 0f, 0f, -50f,
                        0f, 1.4f, 0f, 0f, -50f,
                        0f, 0f, 1.4f, 0f, -50f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastCm)
                cm
            }
            "chrome" -> {
                ColorMatrix(
                    floatArrayOf(
                        1.2f, 0.1f, 0.1f, 0f, 0f,
                        0.1f, 1.1f, 0.1f, 0f, 0f,
                        0.05f, 0.05f, 1.3f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "invert" -> {
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "pastel" -> {
                val cm = ColorMatrix()
                cm.setSaturation(0.5f)
                val brightCm = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 40f,
                        0f, 1f, 0f, 0f, 40f,
                        0f, 0f, 1f, 0f, 40f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(brightCm)
                cm
            }
            else -> null
        }
    }

    private fun blendColorMatrix(a: ColorMatrix, b: ColorMatrix, factor: Float): ColorMatrix {
        val aArr = a.array.copyOf()
        val bArr = b.array.copyOf()
        val result = FloatArray(20)
        for (i in 0 until 20) {
            result[i] = aArr[i] + (bArr[i] - aArr[i]) * factor
        }
        return ColorMatrix(result)
    }

    private fun applyBlur(bitmap: Bitmap, blur: LayerData.BlurData): Bitmap {
        return try {
            when (blur.blurType) {
                LayerData.BlurType.FULL -> simpleBlur(bitmap, blur.intensity)
                LayerData.BlurType.RADIAL -> applyRadialBlur(bitmap, blur)
                LayerData.BlurType.LINEAR -> simpleBlur(bitmap, blur.intensity)
            }
        } catch (_: Throwable) {
            bitmap // Return original if anything goes wrong
        }
    }

    /**
     * Memory-safe blur using downscale → upscale with bilinear filtering.
     * No IntArray allocations, no pixel-level processing — just two Bitmap.createScaledBitmap calls.
     * intensity: 1..25 (1 = slight blur, 25 = heavy blur)
     */
    private fun simpleBlur(bitmap: Bitmap, intensity: Float): Bitmap {
        val clamped = intensity.coerceIn(1f, 25f)
        // Map intensity to a downscale factor: 1→0.5, 25→0.04
        val scale = (1f / (clamped * 0.4f + 1f)).coerceIn(0.02f, 0.5f)
        val smallW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val smallH = (bitmap.height * scale).toInt().coerceAtLeast(1)

        // Downscale (loses detail) → upscale with bilinear filtering = blur
        val small = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        val result = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        if (small !== bitmap && small !== result) small.recycle()
        return result
    }

    private fun applyRadialBlur(bitmap: Bitmap, blur: LayerData.BlurData): Bitmap {
        val blurred = simpleBlur(bitmap, blur.intensity)
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (result == null) {
            if (blurred !== bitmap) blurred.recycle()
            return bitmap
        }
        val canvas = Canvas(result)

        val cx = blur.centerX * bitmap.width
        val cy = blur.centerY * bitmap.height
        val r = (blur.radius * maxOf(bitmap.width, bitmap.height)).coerceAtLeast(1f)

        // Draw blurred as base
        canvas.drawBitmap(blurred, 0f, 0f, null)

        // Create mask: white center (keep original), transparent edges (keep blur)
        val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        maskCanvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), maskPaint)

        // Composite original through the mask
        val origLayer = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (origLayer != null) {
            val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            srcPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            val origCanvas = Canvas(origLayer)
            origCanvas.drawBitmap(maskBitmap, 0f, 0f, srcPaint)
            canvas.drawBitmap(origLayer, 0f, 0f, Paint())
            origLayer.recycle()
        }

        maskBitmap.recycle()
        if (blurred !== bitmap) blurred.recycle()
        return result
    }

    private fun drawDrawing(canvas: Canvas, width: Int, height: Int, drawing: LayerData.DrawingData) {
        // Draw onto a separate transparent bitmap so eraser only erases drawing strokes,
        // not the photo underneath.
        val drawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val drawCanvas = Canvas(drawBitmap)

        // Scale stroke width relative to image size so strokes look the same
        // regardless of resolution. The draw tool stores screen-pixel values;
        // 500 is a reference display dimension in px.
        val strokeScale = minOf(width, height).toFloat() / 500f

        for (drawPath in drawing.paths) {
            if (drawPath.points.size < 2) continue

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = drawPath.color.toInt()
                strokeWidth = drawPath.strokeWidth * strokeScale.coerceAtLeast(1f)
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                alpha = (drawPath.alpha * 255).toInt()
                if (drawPath.isEraser) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            }

            val path = Path()
            val first = drawPath.points.first()
            path.moveTo(first.x * width, first.y * height)

            for (i in 1 until drawPath.points.size) {
                val pt = drawPath.points[i]
                path.lineTo(pt.x * width, pt.y * height)
            }

            drawCanvas.drawPath(path, paint)
        }

        canvas.drawBitmap(drawBitmap, 0f, 0f, null)
        drawBitmap.recycle()
    }

    private fun drawText(canvas: Canvas, width: Int, height: Int, text: LayerData.TextData) {
        if (text.text.isEmpty()) return

        // Scale pixel values relative to image size so text looks the same
        // regardless of resolution. 500 is a reference display dimension.
        val scaleFactor = (minOf(width, height).toFloat() / 500f).coerceAtLeast(1f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = text.color.toInt()
            textSize = text.fontSize * text.scale * scaleFactor
            typeface = Typeface.create(
                text.fontFamily,
                when {
                    text.bold && text.italic -> Typeface.BOLD_ITALIC
                    text.bold -> Typeface.BOLD
                    text.italic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
            )
            textAlign = Paint.Align.CENTER
        }

        val x = text.x * width
        val y = text.y * height

        canvas.save()
        canvas.rotate(text.rotation, x, y)

        val lines = text.text.split("\n")
        val lineHeight = paint.fontMetrics.let { it.descent - it.ascent + it.leading }

        // Draw background if set
        if (text.backgroundColor != 0x00000000L) {
            val bgPaint = Paint().apply {
                color = text.backgroundColor.toInt()
            }
            var maxWidth = 0f
            for (line in lines) {
                val w = paint.measureText(line)
                if (w > maxWidth) maxWidth = w
            }
            val totalHeight = lineHeight * lines.size
            val padding = 16f * scaleFactor
            val cornerRadius = 8f * scaleFactor
            canvas.drawRoundRect(
                RectF(
                    x - maxWidth / 2 - padding,
                    y + paint.fontMetrics.ascent - padding,
                    x + maxWidth / 2 + padding,
                    y + paint.fontMetrics.ascent + totalHeight + padding
                ),
                cornerRadius, cornerRadius, bgPaint
            )
        }

        // Draw each line
        for ((i, line) in lines.withIndex()) {
            val ly = y + i * lineHeight

            // Draw outline if set
            if (text.outlineWidth > 0f) {
                val outlinePaint = Paint(paint).apply {
                    color = text.outlineColor.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = text.outlineWidth * scaleFactor
                }
                canvas.drawText(line, x, ly, outlinePaint)
            }

            // Draw text
            canvas.drawText(line, x, ly, paint)
        }

        canvas.restore()
    }

    private fun drawSticker(canvas: Canvas, width: Int, height: Int, sticker: LayerData.StickerData) {
        // Scale pixel values relative to image size
        val scaleFactor = (minOf(width, height).toFloat() / 500f).coerceAtLeast(1f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 80f * sticker.scale * scaleFactor
            textAlign = Paint.Align.CENTER
        }

        val x = sticker.x * width
        val y = sticker.y * height

        canvas.save()
        canvas.rotate(sticker.rotation, x, y)
        canvas.drawText(sticker.emoji, x, y, paint)
        canvas.restore()
    }
}
