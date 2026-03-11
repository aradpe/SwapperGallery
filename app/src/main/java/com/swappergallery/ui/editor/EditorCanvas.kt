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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.swappergallery.data.model.LayerData
import com.swappergallery.ui.editor.tools.DrawToolState

data class DrawingPoint(val x: Float, val y: Float)

@Composable
fun EditorCanvas(
    previewBitmap: Bitmap?,
    activeTool: EditorTool,
    drawState: DrawToolState,
    hasSelectedLayer: Boolean,
    onDrawingComplete: (List<LayerData.DrawPath>) -> Unit,
    onLayerTransform: (panX: Float, panY: Float, zoomDelta: Float, rotationDelta: Float) -> Unit,
    onLayerDragEnd: () -> Unit,
    onLayerTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }

    // Drawing state
    val currentPath = remember { mutableStateListOf<DrawingPoint>() }
    val completedPaths = remember { mutableStateListOf<DrawPathWithStyle>() }

    // Zoom/pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val drawModifier = when (activeTool) {
        EditorTool.DRAW -> Modifier.pointerInput(drawState) {
            detectDragGestures(
                onDragStart = { offset ->
                    currentPath.clear()
                    currentPath.add(
                        DrawingPoint(
                            offset.x / canvasWidth,
                            offset.y / canvasHeight
                        )
                    )
                },
                onDrag = { change, _ ->
                    change.consume()
                    currentPath.add(
                        DrawingPoint(
                            change.position.x / canvasWidth,
                            change.position.y / canvasHeight
                        )
                    )
                },
                onDragEnd = {
                    if (currentPath.size >= 2) {
                        completedPaths.add(
                            DrawPathWithStyle(
                                points = currentPath.toList(),
                                color = drawState.color,
                                strokeWidth = drawState.strokeWidth,
                                alpha = drawState.opacity,
                                isEraser = drawState.isEraser
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
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        val rotation = event.calculateRotation()
                        if (hasSelectedLayer && (pan != Offset.Zero || zoom != 1f || rotation != 0f)) {
                            onLayerTransform(
                                pan.x / canvasWidth,
                                pan.y / canvasHeight,
                                zoom,
                                rotation
                            )
                        }
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (hasSelectedLayer) {
                        onLayerDragEnd()
                    }
                }
            }
            .pointerInput(hasSelectedLayer) {
                detectTapGestures { offset ->
                    onLayerTap(
                        offset.x / canvasWidth,
                        offset.y / canvasHeight
                    )
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
                    onLayerTap(offset.x / canvasWidth, offset.y / canvasHeight)
                }
            }
        else -> Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                onLayerTap(offset.x / canvasWidth, offset.y / canvasHeight)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(drawModifier)
    ) {
        canvasWidth = size.width
        canvasHeight = size.height

        // Draw the preview bitmap
        previewBitmap?.let { bitmap ->
            val imageBitmap = bitmap.asImageBitmap()
            val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val canvasAspect = size.width / size.height

            val (drawWidth, drawHeight) = if (imageAspect > canvasAspect) {
                size.width to (size.width / imageAspect)
            } else {
                (size.height * imageAspect) to size.height
            }

            val drawX = (size.width - drawWidth) / 2f
            val drawY = (size.height - drawHeight) / 2f

            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bitmap.width, bitmap.height),
                dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
                dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
            )
        }

        // Draw in-progress paths (while user is drawing)
        for (pathWithStyle in completedPaths) {
            if (pathWithStyle.points.size < 2) continue
            val path = Path()
            val first = pathWithStyle.points.first()
            path.moveTo(first.x * size.width, first.y * size.height)
            for (i in 1 until pathWithStyle.points.size) {
                val pt = pathWithStyle.points[i]
                path.lineTo(pt.x * size.width, pt.y * size.height)
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
                    width = pathWithStyle.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw current path being drawn
        if (currentPath.size >= 2) {
            val path = Path()
            val first = currentPath.first()
            path.moveTo(first.x * size.width, first.y * size.height)
            for (i in 1 until currentPath.size) {
                val pt = currentPath[i]
                path.lineTo(pt.x * size.width, pt.y * size.height)
            }
            val currentColor = if (drawState.isEraser) {
                Color.White.copy(alpha = 0.5f)
            } else {
                Color(drawState.color.toInt()).copy(alpha = drawState.opacity)
            }
            drawPath(
                path = path,
                color = currentColor,
                style = Stroke(
                    width = drawState.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
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
                    isEraser = pathWithStyle.isEraser
                )
            }
            onDrawingComplete(paths)
            completedPaths.clear()
        }
    }
}

private data class DrawPathWithStyle(
    val points: List<DrawingPoint>,
    val color: Long,
    val strokeWidth: Float,
    val alpha: Float,
    val isEraser: Boolean
)
