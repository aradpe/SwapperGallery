package com.swappergallery.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.swappergallery.data.model.LayerData
import com.swappergallery.ui.editor.tools.DrawToolState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class DrawingPoint(val x: Float, val y: Float)

@Composable
fun EditorCanvas(
    previewBitmap: Bitmap?,
    activeTool: EditorTool,
    drawState: DrawToolState,
    hasSelectedLayer: Boolean,
    cropData: LayerData.CropData? = null,
    onDrawingComplete: (List<LayerData.DrawPath>) -> Unit,
    onLayerTransform: (panX: Float, panY: Float, zoomDelta: Float, rotationDelta: Float) -> Unit,
    onLayerDragEnd: () -> Unit,
    onLayerTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Image bounds within the canvas (updated each frame)
    var imgX by remember { mutableFloatStateOf(0f) }
    var imgY by remember { mutableFloatStateOf(0f) }
    var imgW by remember { mutableFloatStateOf(1f) }
    var imgH by remember { mutableFloatStateOf(1f) }

    // Drawing state
    val currentPath = remember { mutableStateListOf<DrawingPoint>() }
    val completedPaths = remember { mutableStateListOf<DrawPathWithStyle>() }

    // Zoom/pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Convert canvas pixel to normalized image coordinate (0-1)
    fun toImageX(px: Float) = ((px - imgX) / imgW).coerceIn(0f, 1f)
    fun toImageY(py: Float) = ((py - imgY) / imgH).coerceIn(0f, 1f)

    val drawModifier = when (activeTool) {
        EditorTool.DRAW -> Modifier.pointerInput(drawState) {
            detectDragGestures(
                onDragStart = { offset ->
                    currentPath.clear()
                    currentPath.add(DrawingPoint(toImageX(offset.x), toImageY(offset.y)))
                },
                onDrag = { change, _ ->
                    change.consume()
                    val newPoint = DrawingPoint(toImageX(change.position.x), toImageY(change.position.y))
                    if (drawState.shapeType == LayerData.ShapeType.FREEHAND) {
                        currentPath.add(newPoint)
                    } else {
                        // For shapes, keep only start and current end point
                        if (currentPath.size >= 2) {
                            currentPath[1] = newPoint
                        } else {
                            currentPath.add(newPoint)
                        }
                    }
                },
                onDragEnd = {
                    if (currentPath.size >= 2) {
                        completedPaths.add(
                            DrawPathWithStyle(
                                points = currentPath.toList(),
                                color = drawState.color,
                                strokeWidth = drawState.strokeWidth,
                                alpha = drawState.opacity,
                                isEraser = drawState.isEraser,
                                shapeType = drawState.shapeType
                            )
                        )
                    }
                    currentPath.clear()
                }
            )
        }
        // Text & Sticker: drag to move, pinch to resize/rotate, tap to select/place
        EditorTool.TEXT, EditorTool.STICKER -> Modifier
            .pointerInput(hasSelectedLayer) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    var hasMoved = false
                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        val rotation = event.calculateRotation()
                        if (hasSelectedLayer && (pan != Offset.Zero || zoom != 1f || rotation != 0f)) {
                            hasMoved = true
                            onLayerTransform(
                                pan.x / imgW,
                                pan.y / imgH,
                                zoom,
                                rotation
                            )
                        }
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (hasMoved && hasSelectedLayer) {
                        onLayerDragEnd()
                    } else if (!hasMoved) {
                        // No drag/pinch occurred — treat as a tap for selection
                        onLayerTap(toImageX(downPos.x), toImageY(downPos.y))
                    }
                }
            }
        EditorTool.NONE -> Modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onLayerTap(toImageX(offset.x), toImageY(offset.y))
                }
            }
        else -> Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                onLayerTap(toImageX(offset.x), toImageY(offset.y))
            }
        }
    }

    // Reset zoom/pan when switching to an editing tool
    LaunchedEffect(activeTool) {
        if (activeTool != EditorTool.NONE) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(drawModifier)
    ) {
        // Draw the preview bitmap
        previewBitmap?.let { bitmap ->
            val imageBitmap = bitmap.asImageBitmap()
            val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val canvasAspect = size.width / size.height

            val (baseWidth, baseHeight) = if (imageAspect > canvasAspect) {
                size.width to (size.width / imageAspect)
            } else {
                (size.height * imageAspect) to size.height
            }

            // Apply zoom/pan transforms
            val drawWidth = baseWidth * scale
            val drawHeight = baseHeight * scale
            val drawX = (size.width - baseWidth) / 2f + (baseWidth - drawWidth) / 2f + offsetX
            val drawY = (size.height - baseHeight) / 2f + (baseHeight - drawHeight) / 2f + offsetY

            // Update image bounds for coordinate conversion
            imgX = drawX
            imgY = drawY
            imgW = drawWidth
            imgH = drawHeight

            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bitmap.width, bitmap.height),
                dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
                dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
            )
        }

        // Draw crop overlay when crop tool is active
        if (activeTool == EditorTool.CROP && cropData != null) {
            drawCropOverlay(cropData, imgX, imgY, imgW, imgH)
        }

        // Scale stroke width to match ImageCompositor output (which uses minOf(w,h)/500)
        val strokeScale = (minOf(imgW, imgH) / 500f).coerceAtLeast(1f)

        // Draw completed paths (positioned relative to image area)
        for (pathWithStyle in completedPaths) {
            if (pathWithStyle.points.size < 2) continue

            if (pathWithStyle.shapeType != LayerData.ShapeType.FREEHAND) {
                val shapeColor = Color(pathWithStyle.color.toInt()).copy(alpha = pathWithStyle.alpha)
                drawShapeOnCanvas(
                    pathWithStyle.points.first(), pathWithStyle.points.last(),
                    pathWithStyle.shapeType, shapeColor,
                    pathWithStyle.strokeWidth * strokeScale,
                    imgX, imgY, imgW, imgH
                )
                continue
            }

            val path = Path()
            val first = pathWithStyle.points.first()
            path.moveTo(imgX + first.x * imgW, imgY + first.y * imgH)
            for (i in 1 until pathWithStyle.points.size) {
                val pt = pathWithStyle.points[i]
                path.lineTo(imgX + pt.x * imgW, imgY + pt.y * imgH)
            }
            val pathColor = if (pathWithStyle.isEraser) {
                Color.White.copy(alpha = 0.5f)
            } else {
                Color(pathWithStyle.color.toInt()).copy(alpha = pathWithStyle.alpha)
            }
            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(
                    width = pathWithStyle.strokeWidth * strokeScale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw current path/shape being drawn
        if (currentPath.size >= 2) {
            val currentColor = if (drawState.isEraser) {
                Color.White.copy(alpha = 0.5f)
            } else {
                Color(drawState.color.toInt()).copy(alpha = drawState.opacity)
            }
            val sw = drawState.strokeWidth * strokeScale

            if (drawState.shapeType != LayerData.ShapeType.FREEHAND) {
                drawShapeOnCanvas(
                    currentPath.first(), currentPath.last(),
                    drawState.shapeType, currentColor, sw,
                    imgX, imgY, imgW, imgH
                )
            } else {
                val path = Path()
                val first = currentPath.first()
                path.moveTo(imgX + first.x * imgW, imgY + first.y * imgH)
                for (i in 1 until currentPath.size) {
                    val pt = currentPath[i]
                    path.lineTo(imgX + pt.x * imgW, imgY + pt.y * imgH)
                }
                drawPath(
                    path = path,
                    color = currentColor,
                    style = Stroke(
                        width = sw,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }

    // When switching away from draw tool, commit completed paths
    LaunchedEffect(activeTool) {
        if (activeTool != EditorTool.DRAW && completedPaths.isNotEmpty()) {
            val paths = completedPaths.map { pathWithStyle ->
                LayerData.DrawPath(
                    points = pathWithStyle.points.map { LayerData.PathPoint(it.x, it.y) },
                    color = pathWithStyle.color,
                    strokeWidth = pathWithStyle.strokeWidth,
                    alpha = pathWithStyle.alpha,
                    isEraser = pathWithStyle.isEraser,
                    shapeType = pathWithStyle.shapeType
                )
            }
            onDrawingComplete(paths)
            completedPaths.clear()
        }
    }
}

/** Draw a shape (line, arrow, rect, circle) on the Compose canvas. */
private fun DrawScope.drawShapeOnCanvas(
    start: DrawingPoint,
    end: DrawingPoint,
    shapeType: LayerData.ShapeType,
    color: Color,
    strokeWidth: Float,
    imgX: Float, imgY: Float, imgW: Float, imgH: Float
) {
    val x1 = imgX + start.x * imgW
    val y1 = imgY + start.y * imgH
    val x2 = imgX + end.x * imgW
    val y2 = imgY + end.y * imgH
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (shapeType) {
        LayerData.ShapeType.LINE -> {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
        LayerData.ShapeType.ARROW -> {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            val angle = atan2(y2 - y1, x2 - x1)
            val headLen = strokeWidth * 4f
            val headAngle = 0.45f
            drawLine(color, Offset(x2, y2),
                Offset(x2 - headLen * cos(angle - headAngle), y2 - headLen * sin(angle - headAngle)),
                strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawLine(color, Offset(x2, y2),
                Offset(x2 - headLen * cos(angle + headAngle), y2 - headLen * sin(angle + headAngle)),
                strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
        LayerData.ShapeType.RECTANGLE -> {
            drawRect(color,
                topLeft = Offset(minOf(x1, x2), minOf(y1, y2)),
                size = Size(abs(x2 - x1), abs(y2 - y1)),
                style = stroke)
        }
        LayerData.ShapeType.CIRCLE -> {
            drawOval(color,
                topLeft = Offset(minOf(x1, x2), minOf(y1, y2)),
                size = Size(abs(x2 - x1), abs(y2 - y1)),
                style = stroke)
        }
        else -> {}
    }
}

/** Draw crop overlay: darken areas outside crop, show grid and corner handles. */
private fun DrawScope.drawCropOverlay(
    crop: LayerData.CropData,
    imgX: Float, imgY: Float, imgW: Float, imgH: Float
) {
    val cropLeft = imgX + crop.left * imgW
    val cropTop = imgY + crop.top * imgH
    val cropRight = imgX + crop.right * imgW
    val cropBottom = imgY + crop.bottom * imgH
    val overlayColor = Color.Black.copy(alpha = 0.5f)

    // Darken areas outside crop region
    // Top
    drawRect(overlayColor, topLeft = Offset(imgX, imgY),
        size = Size(imgW, cropTop - imgY))
    // Bottom
    drawRect(overlayColor, topLeft = Offset(imgX, cropBottom),
        size = Size(imgW, imgY + imgH - cropBottom))
    // Left (between top and bottom overlays)
    drawRect(overlayColor, topLeft = Offset(imgX, cropTop),
        size = Size(cropLeft - imgX, cropBottom - cropTop))
    // Right
    drawRect(overlayColor, topLeft = Offset(cropRight, cropTop),
        size = Size(imgX + imgW - cropRight, cropBottom - cropTop))

    // Crop border
    val borderColor = Color.White
    drawRect(borderColor,
        topLeft = Offset(cropLeft, cropTop),
        size = Size(cropRight - cropLeft, cropBottom - cropTop),
        style = Stroke(width = 2f))

    // Rule of thirds grid
    val gridColor = Color.White.copy(alpha = 0.3f)
    val thirdW = (cropRight - cropLeft) / 3f
    val thirdH = (cropBottom - cropTop) / 3f
    for (i in 1..2) {
        drawLine(gridColor, Offset(cropLeft + thirdW * i, cropTop),
            Offset(cropLeft + thirdW * i, cropBottom), strokeWidth = 1f)
        drawLine(gridColor, Offset(cropLeft, cropTop + thirdH * i),
            Offset(cropRight, cropTop + thirdH * i), strokeWidth = 1f)
    }

    // Corner handles
    val handleLen = 20f
    val handleWidth = 4f
    val corners = listOf(
        Offset(cropLeft, cropTop),
        Offset(cropRight, cropTop),
        Offset(cropLeft, cropBottom),
        Offset(cropRight, cropBottom)
    )
    for (corner in corners) {
        val dx = if (corner.x == cropLeft) 1f else -1f
        val dy = if (corner.y == cropTop) 1f else -1f
        drawLine(borderColor, corner,
            Offset(corner.x + handleLen * dx, corner.y), strokeWidth = handleWidth)
        drawLine(borderColor, corner,
            Offset(corner.x, corner.y + handleLen * dy), strokeWidth = handleWidth)
    }
}

private data class DrawPathWithStyle(
    val points: List<DrawingPoint>,
    val color: Long,
    val strokeWidth: Float,
    val alpha: Float,
    val isEraser: Boolean,
    val shapeType: LayerData.ShapeType = LayerData.ShapeType.FREEHAND
)
