package com.swappergallery.util

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
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

        // First pass: apply crop if present
        val cropLayer = layers.firstOrNull { it.type == LayerType.CROP && it.visible }
        if (cropLayer != null) {
            val cropData = json.decodeFromString<LayerData>(cropLayer.data) as? LayerData.CropData
            if (cropData != null) {
                current = applyCrop(current, cropData)
            }
        }

        // Second pass: apply adjustments and filters
        for (layer in layers.sortedBy { it.orderIndex }) {
            if (!layer.visible) continue
            val data = json.decodeFromString<LayerData>(layer.data)
            current = when (layer.type) {
                LayerType.ADJUSTMENT -> applyAdjustment(current, data as LayerData.AdjustmentData)
                LayerType.FILTER -> applyFilter(current, data as LayerData.FilterData)
                LayerType.BLUR -> applyBlur(current, data as LayerData.BlurData)
                else -> current
            }
        }

        // Third pass: draw overlays (drawing, text, stickers)
        val canvas = Canvas(current)
        for (layer in layers.sortedBy { it.orderIndex }) {
            if (!layer.visible) continue
            val data = json.decodeFromString<LayerData>(layer.data)
            when (layer.type) {
                LayerType.DRAWING -> drawDrawing(canvas, current.width, current.height, data as LayerData.DrawingData)
                LayerType.TEXT -> drawText(canvas, current.width, current.height, data as LayerData.TextData)
                LayerType.STICKER -> drawSticker(canvas, current.width, current.height, data as LayerData.StickerData)
                else -> { /* Already applied */ }
            }
        }

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
            return Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
        }
        return cropped
    }

    private fun applyAdjustment(bitmap: Bitmap, adj: LayerData.AdjustmentData): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
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
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb((intensity * 255).toInt(), 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun applyFilter(bitmap: Bitmap, filter: LayerData.FilterData): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint()

        val cm = getFilterColorMatrix(filter.filterName)
        if (cm != null && filter.intensity > 0f) {
            // Blend between identity and filter matrix based on intensity
            val identity = ColorMatrix()
            val blended = blendColorMatrix(identity, cm, filter.intensity)
            paint.colorFilter = ColorMatrixColorFilter(blended)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

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
        val aArr = FloatArray(20)
        val bArr = FloatArray(20)
        a.getArray().copyInto(aArr)
        b.getArray().copyInto(bArr)
        val result = FloatArray(20)
        for (i in 0 until 20) {
            result[i] = aArr[i] + (bArr[i] - aArr[i]) * factor
        }
        return ColorMatrix(result)
    }

    private fun ColorMatrix.getArray(): FloatArray {
        val result = FloatArray(20)
        getValues(result)
        return result
    }

    private fun applyBlur(bitmap: Bitmap, blur: LayerData.BlurData): Bitmap {
        val radius = blur.intensity.coerceIn(1f, 25f)
        return when (blur.type) {
            LayerData.BlurType.FULL -> stackBlur(bitmap, radius.toInt())
            LayerData.BlurType.RADIAL -> applyRadialBlur(bitmap, blur)
            LayerData.BlurType.LINEAR -> stackBlur(bitmap, radius.toInt()) // Simplified
        }
    }

    private fun applyRadialBlur(bitmap: Bitmap, blur: LayerData.BlurData): Bitmap {
        val blurred = stackBlur(bitmap.copy(Bitmap.Config.ARGB_8888, true), blur.intensity.toInt().coerceIn(1, 25))
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val cx = blur.centerX * bitmap.width
        val cy = blur.centerY * bitmap.height
        val r = blur.radius * Math.max(bitmap.width, bitmap.height)

        // Draw blurred version
        canvas.drawBitmap(blurred, 0f, 0f, null)

        // Draw sharp version with circular mask
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        val shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)

        // Composite: where mask is white, use original; where transparent, use blurred
        val finalResult = blurred.copy(Bitmap.Config.ARGB_8888, true)
        val finalCanvas = Canvas(finalResult)
        finalCanvas.drawBitmap(blurred, 0f, 0f, null)

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

        // Use mask to blend original over blurred
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val maskAlpha = Color.alpha(maskBitmap.getPixel(x, y)) / 255f
                if (maskAlpha > 0f) {
                    val origPixel = bitmap.getPixel(x, y)
                    val blurPixel = blurred.getPixel(x, y)
                    val r2 = (Color.red(origPixel) * maskAlpha + Color.red(blurPixel) * (1 - maskAlpha)).toInt()
                    val g = (Color.green(origPixel) * maskAlpha + Color.green(blurPixel) * (1 - maskAlpha)).toInt()
                    val b2 = (Color.blue(origPixel) * maskAlpha + Color.blue(blurPixel) * (1 - maskAlpha)).toInt()
                    finalResult.setPixel(x, y, Color.argb(255, r2, g, b2))
                }
            }
        }
        maskBitmap.recycle()
        return finalResult
    }

    /**
     * Simple stack blur implementation for API 26+ (no RenderScript needed).
     */
    private fun stackBlur(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return bitmap
        val r = radius.coerceAtMost(25)

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val result = IntArray(w * h)

        // Horizontal pass
        for (y in 0 until h) {
            for (x in 0 until w) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (i in -r..r) {
                    val px = (x + i).coerceIn(0, w - 1)
                    val color = pixels[y * w + px]
                    rSum += Color.red(color)
                    gSum += Color.green(color)
                    bSum += Color.blue(color)
                    count++
                }
                result[y * w + x] = Color.argb(255, rSum / count, gSum / count, bSum / count)
            }
        }

        // Vertical pass
        val finalPixels = IntArray(w * h)
        for (x in 0 until w) {
            for (y in 0 until h) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (i in -r..r) {
                    val py = (y + i).coerceIn(0, h - 1)
                    val color = result[py * w + x]
                    rSum += Color.red(color)
                    gSum += Color.green(color)
                    bSum += Color.blue(color)
                    count++
                }
                finalPixels[y * w + x] = Color.argb(255, rSum / count, gSum / count, bSum / count)
            }
        }

        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        output.setPixels(finalPixels, 0, w, 0, 0, w, h)
        return output
    }

    private fun drawDrawing(canvas: Canvas, width: Int, height: Int, drawing: LayerData.DrawingData) {
        for (drawPath in drawing.paths) {
            if (drawPath.points.size < 2) continue

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = drawPath.color.toInt()
                strokeWidth = drawPath.strokeWidth
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

            canvas.drawPath(path, paint)
        }
    }

    private fun drawText(canvas: Canvas, width: Int, height: Int, text: LayerData.TextData) {
        if (text.text.isEmpty()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = text.color.toInt()
            textSize = text.fontSize * text.scale
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

        // Draw background if set
        if (text.backgroundColor != 0x00000000L) {
            val bgPaint = Paint().apply {
                color = text.backgroundColor.toInt()
            }
            val bounds = android.graphics.Rect()
            paint.getTextBounds(text.text, 0, text.text.length, bounds)
            val padding = 16f
            canvas.drawRoundRect(
                RectF(
                    x + bounds.left - padding,
                    y + bounds.top - padding,
                    x + bounds.right + padding,
                    y + bounds.bottom + padding
                ),
                8f, 8f, bgPaint
            )
        }

        // Draw outline if set
        if (text.outlineWidth > 0f) {
            val outlinePaint = Paint(paint).apply {
                color = text.outlineColor.toInt()
                style = Paint.Style.STROKE
                strokeWidth = text.outlineWidth
            }
            canvas.drawText(text.text, x, y, outlinePaint)
        }

        // Draw text
        canvas.drawText(text.text, x, y, paint)

        canvas.restore()
    }

    private fun drawSticker(canvas: Canvas, width: Int, height: Int, sticker: LayerData.StickerData) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 80f * sticker.scale
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
