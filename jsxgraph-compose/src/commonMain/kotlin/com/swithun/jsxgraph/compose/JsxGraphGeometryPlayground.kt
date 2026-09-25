/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swithun.jsxgraph.compose.generated.resources.Res
import com.swithun.jsxgraph.compose.generated.resources.arimo_regular
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphElementStyle
import com.swithun.jsxgraph.core.JsxGraphFillGradient
import com.swithun.jsxgraph.core.JsxGraphInteractionError
import com.swithun.jsxgraph.core.JsxGraphInteractionState
import com.swithun.jsxgraph.core.JsxGraphJessieCodeSession
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import com.swithun.jsxgraph.core.JsxGraphSession
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.Font

private val Background = Color(0xFFF3F5F7)
private val BoardBackground = Color(0xFFFCFDFE)
private val GridColor = Color(0xFFC0C0C0)
private val AxisColor = Color(0xFF666666)
private val SineColor = Color(0xFF246BCE)
private val ParabolaColor = Color(0xFFD9553F)
private val CircleColor = Color(0xFF16877A)
private val ControlColor = Color(0xFFE0A11A)
private val InkColor = Color(0xFF1D252C)
private val MutedColor = Color(0xFF66717A)

data class GeometryPlaygroundScene(
    val fixedPoint: Offset,
    val controlPoint: Offset,
    val circleCenter: Offset,
    val circleRadius: Float,
    val sineAmplitude: Float,
    val sineFrequency: Float,
    val parabolaQuadratic: Float,
    val parabolaConstant: Float,
) {
    companion object {
        val Default = GeometryPlaygroundScene(
            fixedPoint = Offset(-4.0f, -2.0f),
            controlPoint = Offset(3.2f, 2.1f),
            circleCenter = Offset(0.5f, 0.6f),
            circleRadius = 2.35f,
            sineAmplitude = 2.0f,
            sineFrequency = 0.8f,
            parabolaQuadratic = 0.16f,
            parabolaConstant = -2.5f,
        )
    }
}

@Composable
fun JsxGraphGeometryPlayground(
    initialScene: GeometryPlaygroundScene = GeometryPlaygroundScene.Default,
) {
    var controlPoint by remember(initialScene) {
        mutableStateOf(initialScene.controlPoint)
    }
    val intersections = remember(initialScene, controlPoint) {
        calculateIntersections(
            fixedPoint = initialScene.fixedPoint,
            controlPoint = controlPoint,
            circleCenter = initialScene.circleCenter,
            circleRadius = initialScene.circleRadius,
        )
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = SineColor,
            surface = BoardBackground,
            background = Background,
            onSurface = InkColor,
            onBackground = InkColor,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CMP JSXGraph",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkColor,
                        )
                        Text(
                            text = "Geometry playground",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor,
                        )
                    }
                    IconButton(onClick = { controlPoint = initialScene.controlPoint }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset point",
                            tint = InkColor,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFD4DADF),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .background(BoardBackground, RoundedCornerShape(8.dp)),
                ) {
                    GeometryCanvas(
                        modifier = Modifier.fillMaxSize(),
                        fixedPoint = initialScene.fixedPoint,
                        controlPoint = controlPoint,
                        circleCenter = initialScene.circleCenter,
                        circleRadius = initialScene.circleRadius,
                        sineAmplitude = initialScene.sineAmplitude,
                        sineFrequency = initialScene.sineFrequency,
                        parabolaQuadratic = initialScene.parabolaQuadratic,
                        parabolaConstant = initialScene.parabolaConstant,
                        intersections = intersections,
                        interactive = true,
                        onControlPointChange = { controlPoint = it },
                    )
                }

                GeometryReadout(
                    controlPoint = controlPoint,
                    intersectionCount = intersections.size,
                )
            }
        }
    }
}

@Composable
fun JsxGraphGeometryPreview(
    scene: GeometryPlaygroundScene,
    modifier: Modifier = Modifier,
) {
    val intersections = remember(scene) {
        calculateIntersections(
            fixedPoint = scene.fixedPoint,
            controlPoint = scene.controlPoint,
            circleCenter = scene.circleCenter,
            circleRadius = scene.circleRadius,
        )
    }
    Box(
        modifier = modifier
            .background(BoardBackground)
            .border(1.dp, Color(0xFFD4DADF)),
    ) {
        GeometryCanvas(
            modifier = Modifier.fillMaxSize(),
            fixedPoint = scene.fixedPoint,
            controlPoint = scene.controlPoint,
            circleCenter = scene.circleCenter,
            circleRadius = scene.circleRadius,
            sineAmplitude = scene.sineAmplitude,
            sineFrequency = scene.sineFrequency,
            parabolaQuadratic = scene.parabolaQuadratic,
            parabolaConstant = scene.parabolaConstant,
            intersections = intersections,
            interactive = false,
            onControlPointChange = {},
        )
    }
}

@Composable
fun JsxGraphScenePreview(
    scene: JsxGraphScene,
    modifier: Modifier = Modifier,
    onPointDrag: ((String, JsxGraphPoint2D) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentScene by rememberUpdatedState(scene)
    val currentOnPointDrag by rememberUpdatedState(onPointDrag)
    val axisFontFamily = FontFamily(
        Font(
            resource = Res.font.arimo_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
    )
    Canvas(
        modifier = modifier
            .background(BoardBackground)
            .border(1.dp, Color(0xFFD4DADF))
            .pointerInput(onPointDrag != null) {
                if (currentOnPointDrag == null) {
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val metrics = currentScene.boardMetrics(
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                    )
                    val point = draggablePointAt(
                        scene = currentScene,
                        position = down.position,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        density = density,
                    ) ?: return@awaitEachGesture
                    val dragOffset =
                        metrics.toScreen(point.coordinates.toOffset()) -
                            down.position
                    drag(down.id) { change ->
                        val metrics = currentScene.boardMetrics(
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                        )
                        val user = metrics.toUser(change.position + dragOffset)
                        change.consume()
                        currentOnPointDrag?.invoke(
                            point.id,
                            JsxGraphPoint2D(
                                x = user.x.toDouble(),
                                y = user.y.toDouble(),
                            ),
                        )
                    }
                }
            },
    ) {
        val metrics = scene.boardMetrics(size.width, size.height)
        val horizontalMajorStep = metrics.majorTickDistance(
            visibleDistance = metrics.right - metrics.left,
            density = density,
            pixelsPerUnit = metrics.scaleX,
        )
        val verticalMajorStep = metrics.majorTickDistance(
            visibleDistance = metrics.top - metrics.bottom,
            density = density,
            pixelsPerUnit = metrics.scaleY,
        )

        for (item in scene.renderOrderedItems()) {
            when (item) {
                is JsxGraphSceneRenderItem.Grid ->
                    drawGrid(
                        metrics = metrics,
                        horizontalMajorStep = horizontalMajorStep,
                        verticalMajorStep = verticalMajorStep,
                    )
                is JsxGraphSceneRenderItem.Axis ->
                    drawAxes(
                        metrics = metrics,
                        horizontalMajorStep = horizontalMajorStep,
                        verticalMajorStep = verticalMajorStep,
                        textMeasurer = textMeasurer,
                        fontFamily = axisFontFamily,
                    )
                is JsxGraphSceneRenderItem.Element -> {
                    val element = item.element
                    if (
                        !element.style.visible ||
                        element is JsxGraphSceneElement.Point &&
                        !element.isReal
                    ) {
                        continue
                    }
                    when (element) {
                        is JsxGraphSceneElement.Point ->
                            drawScenePoint(element, metrics)
                        is JsxGraphSceneElement.Line ->
                            drawSceneLine(element, metrics)
                        is JsxGraphSceneElement.Circle ->
                            drawSceneCircle(element, metrics)
                        is JsxGraphSceneElement.Curve ->
                            drawSceneCurve(element, metrics)
                        is JsxGraphSceneElement.Polygon -> Unit
                        is JsxGraphSceneElement.Text ->
                            drawSceneText(
                                text = element,
                                metrics = metrics,
                                textMeasurer = textMeasurer,
                                fontFamily = axisFontFamily,
                            )
                    }
                }
                is JsxGraphSceneRenderItem.PolygonFill ->
                    if (item.polygon.style.visible) {
                        drawScenePolygonFill(item.polygon, metrics)
                    }
                is JsxGraphSceneRenderItem.PolygonBorder ->
                    if (
                        item.polygon.style.visible &&
                        item.polygon.borderStyle.visible
                    ) {
                        drawScenePolygonBorder(item.polygon, metrics)
                    }
                is JsxGraphSceneRenderItem.PolygonVertex ->
                    if (
                        item.polygon.style.visible &&
                        item.vertex.style.visible
                    ) {
                        drawScenePoint(item.vertex, metrics)
                    }
            }
        }
    }
}

@Composable
fun JsxGraphBoard(
    session: JsxGraphSession,
    modifier: Modifier = Modifier,
    onInteractionStateChange: (JsxGraphInteractionState) -> Unit = {},
    onInteractionError: (JsxGraphInteractionError) -> Unit = {},
) {
    var scene by remember(session) {
        mutableStateOf(session.scene)
    }
    val currentOnStateChange by rememberUpdatedState(onInteractionStateChange)
    val currentOnError by rememberUpdatedState(onInteractionError)
    JsxGraphScenePreview(
        scene = scene,
        modifier = modifier,
        onPointDrag = { id, coordinates ->
            when (val result = session.movePoint(id, coordinates)) {
                is com.swithun.jsxgraph.core.GMResult.Ok -> {
                    scene = result.value
                    currentOnStateChange(session.captureInteractionState())
                }
                is com.swithun.jsxgraph.core.GMResult.Err ->
                    currentOnError(result.error)
            }
        },
    )
}

@Composable
fun JsxGraphBoard(
    session: JsxGraphJessieCodeSession,
    modifier: Modifier = Modifier,
    onInteractionError: (JsxGraphInteractionError) -> Unit = {},
) {
    var scene by remember(session) {
        mutableStateOf(session.scene)
    }
    val currentOnError by rememberUpdatedState(onInteractionError)
    JsxGraphScenePreview(
        scene = scene,
        modifier = modifier,
        onPointDrag = { id, coordinates ->
            when (val result = session.movePoint(id, coordinates)) {
                is com.swithun.jsxgraph.core.GMResult.Ok ->
                    scene = result.value
                is com.swithun.jsxgraph.core.GMResult.Err ->
                    currentOnError(result.error)
            }
        },
    )
}

internal fun draggablePointAt(
    scene: JsxGraphScene,
    position: Offset,
    width: Float,
    height: Float,
    density: Float,
): JsxGraphSceneElement.Point? {
    val metrics = scene.boardMetrics(width, height)
    return scene.renderOrderedElements()
        .asReversed()
        .filterIsInstance<JsxGraphSceneElement.Point>()
        .firstOrNull { point ->
            if (
                !point.style.visible ||
                !point.isReal ||
                !point.draggable
            ) {
                return@firstOrNull false
            }
            val center = metrics.toScreen(point.coordinates.toOffset())
            // JSXGraph: src/base/point.js -> hasPoint.
            val radius = max(
                point.size.toFloat() * density +
                    point.style.strokeWidth.toFloat() * density * 0.5f,
                POINT_HIT_PRECISION_DP * density,
            ) + POINT_HIT_PADDING_DP * density
            abs(center.x - position.x) < radius &&
                abs(center.y - position.y) < radius
        }
}

// JSXGraph 1.13.3: src/base/board.js -> updateRendererCanvas / _compareDepth.
// Elements in a higher layer are drawn later. Creation order breaks ties.
internal fun JsxGraphScene.renderOrderedElements(): List<JsxGraphSceneElement> =
    elements
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<JsxGraphSceneElement>>(
                { indexed -> indexed.value.style.layer },
                { indexed -> indexed.index },
            ),
        )
        .map(IndexedValue<JsxGraphSceneElement>::value)

internal sealed interface JsxGraphSceneRenderItem {
    val layer: Int
    val position: Long

    data class Grid(
        override val position: Long,
    ) : JsxGraphSceneRenderItem {
        override val layer: Int = 1
    }

    data class Axis(
        override val position: Long,
    ) : JsxGraphSceneRenderItem {
        override val layer: Int = 2
    }

    data class Element(
        val element: JsxGraphSceneElement,
        override val position: Long,
    ) : JsxGraphSceneRenderItem {
        override val layer: Int = element.style.layer
    }

    data class PolygonFill(
        val polygon: JsxGraphSceneElement.Polygon,
        override val position: Long,
    ) : JsxGraphSceneRenderItem {
        override val layer: Int = polygon.style.layer
    }

    data class PolygonBorder(
        val polygon: JsxGraphSceneElement.Polygon,
        override val position: Long,
    ) : JsxGraphSceneRenderItem {
        override val layer: Int = polygon.borderStyle.layer
    }

    data class PolygonVertex(
        val polygon: JsxGraphSceneElement.Polygon,
        val vertex: JsxGraphSceneElement.Point,
        override val position: Long,
    ) : JsxGraphSceneRenderItem {
        override val layer: Int = vertex.style.layer
    }
}

// JSXGraph 1.13.3: src/base/board.js -> updateRendererCanvas / _compareDepth;
// src/base/polygon.js -> Polygon constructor.
// Polygon-owned Points and borders are Board objects created before the
// Polygon itself, so they participate independently in layer ordering.
internal fun JsxGraphScene.renderOrderedItems():
    List<JsxGraphSceneRenderItem> {
    val items = mutableListOf<JsxGraphSceneRenderItem>()
    var position = 0L
    if (grid) {
        items += JsxGraphSceneRenderItem.Grid(position++)
    }
    if (axis) {
        items += JsxGraphSceneRenderItem.Axis(position++)
    }
    for (element in elements) {
        if (element is JsxGraphSceneElement.Polygon) {
            for (vertex in element.implicitVertices) {
                items += JsxGraphSceneRenderItem.PolygonVertex(
                    polygon = element,
                    vertex = vertex,
                    position = position++,
                )
            }
            if (element.withLines) {
                items += JsxGraphSceneRenderItem.PolygonBorder(
                    polygon = element,
                    position = position++,
                )
            }
            items += JsxGraphSceneRenderItem.PolygonFill(
                polygon = element,
                position = position++,
            )
        } else {
            items += JsxGraphSceneRenderItem.Element(
                element = element,
                position = position++,
            )
        }
    }
    return items.sortedWith(
        compareBy<JsxGraphSceneRenderItem>(
            JsxGraphSceneRenderItem::layer,
            JsxGraphSceneRenderItem::position,
        ),
    )
}

private fun JsxGraphScene.boardMetrics(
    width: Float,
    height: Float,
): BoardMetrics {
    val bounds = boundingBox
    return BoardMetrics(
        width = width,
        height = height,
        requestedLeft = bounds.left.toFloat(),
        requestedTop = bounds.top.toFloat(),
        requestedRight = bounds.right.toFloat(),
        requestedBottom = bounds.bottom.toFloat(),
        keepAspectRatio = keepAspectRatio,
    )
}

private fun DrawScope.drawGrid(
    metrics: BoardMetrics,
    horizontalMajorStep: Float,
    verticalMajorStep: Float,
) {
    gridValues(
        lower = metrics.left,
        upper = metrics.right,
        step = horizontalMajorStep,
    ).forEach { x ->
        val screenX = metrics.toScreen(Offset(x, 0f)).x
        drawLine(
            color = GridColor,
            start = Offset(screenX, 0f),
            end = Offset(screenX, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
    gridValues(
        lower = metrics.bottom,
        upper = metrics.top,
        step = verticalMajorStep,
    ).forEach { y ->
        val screenY = metrics.toScreen(Offset(0f, y)).y
        drawLine(
            color = GridColor,
            start = Offset(0f, screenY),
            end = Offset(size.width, screenY),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private fun DrawScope.drawAxes(
    metrics: BoardMetrics,
    horizontalMajorStep: Float,
    verticalMajorStep: Float,
    textMeasurer: TextMeasurer,
    fontFamily: FontFamily,
) {
    val axisOrigin = metrics.toScreen(Offset.Zero)
    drawLine(
        color = AxisColor,
        start = Offset(0f, axisOrigin.y),
        end = Offset(size.width, axisOrigin.y),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = AxisColor,
        start = Offset(axisOrigin.x, 0f),
        end = Offset(axisOrigin.x, size.height),
        strokeWidth = 1.dp.toPx(),
    )
    drawAxisDecorations(
        metrics = metrics,
        horizontalMajorStep = horizontalMajorStep,
        verticalMajorStep = verticalMajorStep,
        textMeasurer = textMeasurer,
        fontFamily = fontFamily,
    )
}

private fun DrawScope.drawScenePoint(
    point: JsxGraphSceneElement.Point,
    metrics: BoardMetrics,
) {
    val center = metrics.toScreen(point.coordinates.toOffset())
    val radius = point.size.dp.toPx()
    val fill = point.style.fillColor.toComposeColor(
        opacity = point.style.fillOpacity,
    )
    if (fill.alpha > 0.0f) {
        drawCircle(
            color = fill,
            radius = radius,
            center = center,
        )
    }
    val stroke = point.style.strokeColor.toComposeColor(
        opacity = point.style.strokeOpacity,
    )
    if (stroke.alpha > 0.0f && point.style.strokeWidth > 0.0) {
        drawCircle(
            color = stroke,
            radius = radius,
            center = center,
            style = Stroke(
                width = point.style.strokeWidth.dp.toPx(),
                pathEffect = strokeDashPathEffect(point.style),
            ),
        )
    }
}

private fun DrawScope.drawSceneLine(
    line: JsxGraphSceneElement.Line,
    metrics: BoardMetrics,
) {
    val point1 = metrics.toScreen(line.point1.toOffset())
    val point2 = metrics.toScreen(line.point2.toOffset())
    val color = line.style.strokeColor.toComposeColor(
        opacity = line.style.strokeOpacity,
    )
    val strokeWidth = line.style.strokeWidth.dp.toPx()
    val geometry = lineRenderGeometry(
        point1 = point1,
        point2 = point2,
        straightFirst = line.straightFirst,
        straightLast = line.straightLast,
        viewportSize = size,
        strokeWidth = strokeWidth,
        firstArrow = line.firstArrow,
        lastArrow = line.lastArrow,
    ) ?: return
    if (color.alpha > 0.0f && strokeWidth > 0.0f) {
        drawLine(
            color = color,
            start = geometry.strokeStart,
            end = geometry.strokeEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
            pathEffect = strokeDashPathEffect(line.style),
        )
        geometry.firstArrow?.let { arrow ->
            drawArrowHead(
                arrow = arrow,
                color = color,
                strokeWidth = strokeWidth,
            )
        }
        geometry.lastArrow?.let { arrow ->
            drawArrowHead(
                arrow = arrow,
                color = color,
                strokeWidth = strokeWidth,
            )
        }
    }
}

// JSXGraph 1.13.3: src/renderer/canvas.js -> _drawPolygon.
private fun DrawScope.drawArrowHead(
    arrow: ArrowHeadGeometry,
    color: Color,
    strokeWidth: Float,
) {
    val first = arrow.points.firstOrNull() ?: return
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        moveTo(first.x, first.y)
        if (arrow.bezierDegree == 1) {
            for (point in arrow.points.drop(1)) {
                lineTo(point.x, point.y)
            }
        } else {
            var index = 1
            while (index + 2 < arrow.points.size) {
                val control1 = arrow.points[index]
                val control2 = arrow.points[index + 1]
                val end = arrow.points[index + 2]
                cubicTo(
                    control1.x,
                    control1.y,
                    control2.x,
                    control2.y,
                    end.x,
                    end.y,
                )
                index += 3
            }
        }
        if (arrow.filled) {
            close()
        }
    }
    if (arrow.filled) {
        drawPath(path = path, color = color)
    } else {
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Butt,
            ),
        )
    }
}

private fun DrawScope.drawSceneCircle(
    circle: JsxGraphSceneElement.Circle,
    metrics: BoardMetrics,
) {
    val center = metrics.toScreen(circle.center.toOffset())
    val radiusX = circle.radius.toFloat() * metrics.scaleX
    val radiusY = circle.radius.toFloat() * metrics.scaleY
    val topLeft = Offset(center.x - radiusX, center.y - radiusY)
    val ellipseSize = Size(radiusX * 2.0f, radiusY * 2.0f)
    val bounds = Rect(offset = topLeft, size = ellipseSize)
    if (circle.style.fillGradient == null) {
        val fill = circle.style.fillColor.toComposeColor(
            opacity = circle.style.fillOpacity,
        )
        if (fill.alpha > 0.0f) {
            drawOval(
                color = fill,
                topLeft = topLeft,
                size = ellipseSize,
            )
        }
    } else {
        val path = Path().apply {
            addOval(bounds)
        }
        drawSceneFill(
            path = path,
            bounds = bounds,
            style = circle.style,
        )
    }
    val stroke = circle.style.strokeColor.toComposeColor(
        opacity = circle.style.strokeOpacity,
    )
    if (stroke.alpha > 0.0f && circle.style.strokeWidth > 0.0) {
        drawOval(
            color = stroke,
            topLeft = topLeft,
            size = ellipseSize,
            style = Stroke(
                width = circle.style.strokeWidth.dp.toPx(),
                pathEffect = strokeDashPathEffect(circle.style),
            ),
        )
    }
}

// JSXGraph: src/renderer/abstract.js -> updatePathStringPoint / drawCurve.
private fun DrawScope.drawSceneCurve(
    curve: JsxGraphSceneElement.Curve,
    metrics: BoardMetrics,
) {
    val commands = curvePathCommands(
        points = curveScreenPoints(
            curve = curve,
            metrics = metrics,
            density = density,
        ),
        bezierDegree = curve.bezierDegree,
    )
    val path = Path()
    var segmentCount = 0
    for (command in commands) {
        when (command) {
            is CurvePathCommand.MoveTo ->
                path.moveTo(command.point.x, command.point.y)
            is CurvePathCommand.LineTo -> {
                path.lineTo(command.point.x, command.point.y)
                segmentCount += 1
            }
            is CurvePathCommand.CubicTo -> {
                path.cubicTo(
                    x1 = command.control1.x,
                    y1 = command.control1.y,
                    x2 = command.control2.x,
                    y2 = command.control2.y,
                    x3 = command.end.x,
                    y3 = command.end.y,
                )
                segmentCount += 1
            }
        }
    }
    if (segmentCount > 0) {
        drawSceneFill(
            path = path,
            bounds = path.getBounds(),
            style = curve.style,
        )
    }
    val stroke = curve.style.strokeColor.toComposeColor(
        opacity = curve.style.strokeOpacity,
    )
    if (
        segmentCount > 0 &&
        stroke.alpha > 0.0f &&
        curve.style.strokeWidth > 0.0
    ) {
        drawPath(
            path = path,
            color = stroke,
            style = Stroke(
                width = curve.style.strokeWidth.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = strokeDashPathEffect(curve.style),
            ),
        )
    }
}

// JSXGraph 1.13.3: src/renderer/svg.js -> updateGradient,
// updateGradientAngle, updateGradientCircle. Compose has no portable
// two-circle radial shader, so radial contours are rasterized into an
// isolated layer while retaining SVG objectBoundingBox geometry.
private fun DrawScope.drawSceneFill(
    path: Path,
    bounds: Rect,
    style: JsxGraphElementStyle,
) {
    val gradient = style.fillGradient
    if (
        gradient == null ||
        bounds.width <= 0.0f ||
        bounds.height <= 0.0f ||
        !bounds.left.isFinite() ||
        !bounds.top.isFinite() ||
        !bounds.right.isFinite() ||
        !bounds.bottom.isFinite()
    ) {
        val fill = style.fillColor.toComposeColor(style.fillOpacity)
        if (fill.alpha > 0.0f) {
            drawPath(path = path, color = fill)
        }
        return
    }

    val colors = gradientStopColors(style, gradient)
    if (colors.first.alpha <= 0.0f && colors.second.alpha <= 0.0f) {
        return
    }
    when (gradient) {
        is JsxGraphFillGradient.Linear -> {
            val geometry = linearGradientGeometry(
                bounds = bounds,
                angle = gradient.angle,
            )
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    gradient.startOffset.toFloat() to colors.first,
                    gradient.endOffset
                        .coerceAtLeast(gradient.startOffset)
                        .toFloat() to colors.second,
                    start = geometry.start,
                    end = geometry.end,
                ),
            )
        }
        is JsxGraphFillGradient.Radial ->
            drawRadialGradient(
                path = path,
                bounds = bounds,
                style = style,
                gradient = gradient,
                edgeColor = colors.second,
            )
    }
}

private fun DrawScope.drawRadialGradient(
    path: Path,
    bounds: Rect,
    style: JsxGraphElementStyle,
    gradient: JsxGraphFillGradient.Radial,
    edgeColor: Color,
) {
    val steps = radialGradientStepCount(bounds)
    drawContext.canvas.saveLayer(bounds, Paint())
    try {
        drawContext.canvas.clipPath(path)
        drawPath(
            path = path,
            color = edgeColor,
            blendMode = BlendMode.Src,
        )
        for (index in steps downTo 0) {
            val progress = index.toFloat() / steps
            val contour = radialGradientContour(
                bounds = bounds,
                gradient = gradient,
                progress = progress,
            )
            if (contour.radiusX <= 0.0f || contour.radiusY <= 0.0f) {
                continue
            }
            drawOval(
                color = gradientColorAt(
                    style = style,
                    gradient = gradient,
                    position = progress,
                ),
                topLeft = Offset(
                    x = contour.center.x - contour.radiusX,
                    y = contour.center.y - contour.radiusY,
                ),
                size = Size(
                    width = contour.radiusX * 2.0f,
                    height = contour.radiusY * 2.0f,
                ),
                blendMode = BlendMode.Src,
            )
        }
    } finally {
        drawContext.canvas.restore()
    }
}

internal data class LinearGradientGeometry(
    val start: Offset,
    val end: Offset,
)

internal fun linearGradientGeometry(
    bounds: Rect,
    angle: Double,
): LinearGradientGeometry {
    val cosine = cos(angle)
    val sine = sin(angle)
    val factor = 1.0 / max(abs(cosine), abs(sine))
    val startX = if (cosine >= 0.0) 0.0 else -cosine * factor
    val endX = if (cosine >= 0.0) cosine * factor else 0.0
    val startY = if (sine >= 0.0) 0.0 else -sine * factor
    val endY = if (sine >= 0.0) sine * factor else 0.0
    return LinearGradientGeometry(
        start = Offset(
            x = bounds.left + bounds.width * startX.toFloat(),
            y = bounds.top + bounds.height * startY.toFloat(),
        ),
        end = Offset(
            x = bounds.left + bounds.width * endX.toFloat(),
            y = bounds.top + bounds.height * endY.toFloat(),
        ),
    )
}

internal data class RadialGradientContour(
    val center: Offset,
    val radiusX: Float,
    val radiusY: Float,
)

internal fun radialGradientContour(
    bounds: Rect,
    gradient: JsxGraphFillGradient.Radial,
    progress: Float,
): RadialGradientContour {
    val t = progress.coerceIn(0.0f, 1.0f)
    val centerX = gradient.focalX +
        (gradient.centerX - gradient.focalX) * t
    val centerY = gradient.focalY +
        (gradient.centerY - gradient.focalY) * t
    val radius = gradient.focalRadius +
        (gradient.radius - gradient.focalRadius) * t
    return RadialGradientContour(
        center = Offset(
            x = bounds.left + bounds.width * centerX.toFloat(),
            y = bounds.top + bounds.height * centerY.toFloat(),
        ),
        radiusX = bounds.width * radius.toFloat(),
        radiusY = bounds.height * radius.toFloat(),
    )
}

internal fun radialGradientStepCount(bounds: Rect): Int =
    ceil(max(bounds.width, bounds.height).toDouble())
        .toInt()
        .coerceIn(256, 1024)

internal fun gradientStopColors(
    style: JsxGraphElementStyle,
    gradient: JsxGraphFillGradient,
): Pair<Color, Color> =
    style.fillColor.toComposeColor(
        // SVG assigns fillOpacity both to the first stop and to the element.
        opacity = style.fillOpacity * style.fillOpacity,
    ) to gradient.secondColor.toComposeColor(
        opacity = style.fillOpacity * gradient.secondOpacity,
    )

internal fun gradientColorAt(
    style: JsxGraphElementStyle,
    gradient: JsxGraphFillGradient,
    position: Float,
): Color {
    val colors = gradientStopColors(style, gradient)
    val start = gradient.startOffset.toFloat()
    val end = gradient.endOffset
        .coerceAtLeast(gradient.startOffset)
        .toFloat()
    val fraction = when {
        position < start -> 0.0f
        position >= end -> 1.0f
        end == start -> 1.0f
        else -> (position - start) / (end - start)
    }
    return Color(
        red = colors.first.red +
            (colors.second.red - colors.first.red) * fraction,
        green = colors.first.green +
            (colors.second.green - colors.first.green) * fraction,
        blue = colors.first.blue +
            (colors.second.blue - colors.first.blue) * fraction,
        alpha = colors.first.alpha +
            (colors.second.alpha - colors.first.alpha) * fraction,
    )
}

internal fun curveScreenPoints(
    curve: JsxGraphSceneElement.Curve,
    metrics: BoardMetrics,
    density: Float,
): List<Offset?> =
    curve.resolvePoints(
        // JSXGraph board units are CSS pixels, which correspond to Compose dp.
        cssPixelsPerUnitX = metrics.scaleX.toDouble() / density,
        cssPixelsPerUnitY = metrics.scaleY.toDouble() / density,
    ).map { point ->
        point?.let {
            metrics.toScreen(it.toOffset())
        }?.takeIf { screen ->
            screen.x.isFinite() && screen.y.isFinite()
        }
    }

internal sealed interface CurvePathCommand {
    data class MoveTo(
        val point: Offset,
    ) : CurvePathCommand

    data class LineTo(
        val point: Offset,
    ) : CurvePathCommand

    data class CubicTo(
        val control1: Offset,
        val control2: Offset,
        val end: Offset,
    ) : CurvePathCommand
}

// JSXGraph: src/renderer/abstract.js -> updatePathStringPoint.
internal fun curvePathCommands(
    points: List<Offset?>,
    bezierDegree: Int,
): List<CurvePathCommand> {
    if (bezierDegree !in setOf(1, 3)) {
        return emptyList()
    }
    val commands = mutableListOf<CurvePathCommand>()
    val contiguous = mutableListOf<Offset>()

    fun appendSubpath() {
        val start = contiguous.firstOrNull() ?: return
        commands += CurvePathCommand.MoveTo(start)
        if (bezierDegree == 1) {
            contiguous.drop(1).forEach { point ->
                commands += CurvePathCommand.LineTo(point)
            }
        } else {
            var index = 1
            while (index + 2 < contiguous.size) {
                commands += CurvePathCommand.CubicTo(
                    control1 = contiguous[index],
                    control2 = contiguous[index + 1],
                    end = contiguous[index + 2],
                )
                index += 3
            }
        }
        contiguous.clear()
    }

    for (point in points) {
        if (point == null || !point.x.isFinite() || !point.y.isFinite()) {
            appendSubpath()
        } else {
            contiguous += point
        }
    }
    appendSubpath()
    return commands
}

// JSXGraph: src/renderer/abstract.js -> drawPolygon / updatePolygon.
private fun DrawScope.drawScenePolygonFill(
    polygon: JsxGraphSceneElement.Polygon,
    metrics: BoardMetrics,
) {
    // JSXGraph: src/renderer/canvas.js -> updatePolygonPrim. PolygonalChain
    // keeps a closed fill primitive even though its separately rendered
    // Segment border remains open.
    val path = polygonPath(
        polygon = polygon,
        metrics = metrics,
        close = true,
    ) ?: return
    val fill = polygon.style.fillColor.toComposeColor(
        opacity = polygon.style.fillOpacity,
    )
    if (polygon.vertices.size >= 3 && fill.alpha > 0.0f) {
        drawPath(path = path, color = fill)
    }
}

private fun DrawScope.drawScenePolygonBorder(
    polygon: JsxGraphSceneElement.Polygon,
    metrics: BoardMetrics,
) {
    val path = polygonPath(
        polygon = polygon,
        metrics = metrics,
        close = polygon.isClosed,
    ) ?: return
    val stroke = polygon.borderStyle.strokeColor.toComposeColor(
        opacity = polygon.borderStyle.strokeOpacity,
    )
    if (
        polygon.vertices.size >= 2 &&
        stroke.alpha > 0.0f &&
        polygon.borderStyle.strokeWidth > 0.0
    ) {
        drawPath(
            path = path,
            color = stroke,
            style = Stroke(
                width = polygon.borderStyle.strokeWidth.dp.toPx(),
                cap = StrokeCap.Butt,
                join = StrokeJoin.Miter,
                pathEffect = strokeDashPathEffect(polygon.borderStyle),
            ),
        )
    }
}

// JSXGraph 1.13.3: src/renderer/canvas.js -> _stroke. Scene dash
// intervals are CSS pixels, which correspond to Compose dp.
private fun DrawScope.strokeDashPathEffect(
    style: com.swithun.jsxgraph.core.JsxGraphElementStyle,
): PathEffect? {
    if (style.strokeDashPattern.isEmpty()) {
        return null
    }
    val intervals = style.strokeDashPattern.map { length ->
        // Skia requires positive intervals; preserve JSXGraph's zero-length
        // dotted segment with the smallest practical positive value.
        max(length.dp.toPx(), 0.001f)
    }.toFloatArray()
    return PathEffect.dashPathEffect(intervals)
}

private fun polygonPath(
    polygon: JsxGraphSceneElement.Polygon,
    metrics: BoardMetrics,
    close: Boolean,
): Path? {
    val screenVertices = polygon.vertices.map { vertex ->
        metrics.toScreen(vertex.toOffset())
    }
    if (screenVertices.isEmpty()) {
        return null
    }
    return Path().apply {
        fillType = PathFillType.EvenOdd
        moveTo(screenVertices[0].x, screenVertices[0].y)
        for (index in 1 until screenVertices.size) {
            lineTo(
                screenVertices[index].x,
                screenVertices[index].y,
            )
        }
        if (close) {
            close()
        }
    }
}

// JSXGraph: src/renderer/canvas.js -> drawInternalText.
private fun DrawScope.drawSceneText(
    text: JsxGraphSceneElement.Text,
    metrics: BoardMetrics,
    textMeasurer: TextMeasurer,
    fontFamily: FontFamily,
) {
    if (text.content.isEmpty() || text.fontSize <= 0.0) {
        return
    }
    val layout = textMeasurer.measure(
        text = text.content,
        style = TextStyle(
            color = text.style.strokeColor.toComposeColor(
                opacity = text.style.strokeOpacity,
            ),
            fontSize = text.fontSize.toFloat().sp,
            fontFamily = fontFamily,
            letterSpacing = 0.sp,
        ),
    )
    val coordinates =
        text.ticks3DLabel?.resolvePosition(
            cssPixelsPerUnitX = metrics.scaleX.toDouble() / density,
            cssPixelsPerUnitY = metrics.scaleY.toDouble() / density,
        ) ?: text.coordinates
    val anchor = metrics.toScreen(coordinates.toOffset())
    drawText(
        textLayoutResult = layout,
        topLeft = textTopLeft(
            anchor = anchor,
            width = layout.size.width.toFloat(),
            height = layout.size.height.toFloat(),
            anchorX = text.anchorX,
            anchorY = text.anchorY,
        ),
    )
}

internal fun textTopLeft(
    anchor: Offset,
    width: Float,
    height: Float,
    anchorX: String,
    anchorY: String,
): Offset =
    Offset(
        x = when (anchorX) {
            "middle" -> anchor.x - width * 0.5f
            "right" -> anchor.x - width
            else -> anchor.x
        },
        y = when (anchorY) {
            "middle" -> anchor.y - height * 0.5f
            "bottom" -> anchor.y - height
            else -> anchor.y
        },
    )

private fun com.swithun.jsxgraph.core.JsxGraphPoint2D.toOffset(): Offset =
    Offset(x.toFloat(), y.toFloat())

internal fun JsxGraphColor.toComposeColor(
    opacity: Double,
): Color =
    Color(
        red = red / 255.0f,
        green = green / 255.0f,
        blue = blue / 255.0f,
        alpha = alpha / 255.0f * opacity.toFloat(),
    )

@Composable
private fun GeometryCanvas(
    modifier: Modifier,
    fixedPoint: Offset,
    controlPoint: Offset,
    circleCenter: Offset,
    circleRadius: Float,
    sineAmplitude: Float,
    sineFrequency: Float,
    parabolaQuadratic: Float,
    parabolaConstant: Float,
    intersections: List<Offset>,
    interactive: Boolean,
    onControlPointChange: (Offset) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val axisFontFamily = FontFamily(
        Font(
            resource = Res.font.arimo_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
    )
    Canvas(
        modifier = modifier.pointerInput(interactive) {
            if (!interactive) {
                return@pointerInput
            }
            var dragging = false
            detectDragGestures(
                onDragStart = { position ->
                    val metrics = BoardMetrics(size.width.toFloat(), size.height.toFloat())
                    dragging =
                        (position - metrics.toScreen(controlPoint)).getDistance() <= 34.dp.toPx()
                },
                onDragEnd = { dragging = false },
                onDragCancel = { dragging = false },
                onDrag = { change, _ ->
                    if (dragging) {
                        change.consume()
                        val metrics = BoardMetrics(size.width.toFloat(), size.height.toFloat())
                        val point = metrics.toUser(change.position)
                        onControlPointChange(
                            Offset(
                                x = point.x.coerceIn(-5.7f, 5.7f),
                                y = point.y.coerceIn(-4.7f, 4.7f),
                            ),
                        )
                    }
                },
            )
        },
    ) {
        val metrics = BoardMetrics(size.width, size.height)
        val horizontalMajorStep = metrics.majorTickDistance(
            metrics.right - metrics.left,
            density,
        )
        val verticalMajorStep = metrics.majorTickDistance(
            metrics.top - metrics.bottom,
            density,
        )

        gridValues(
            lower = metrics.left,
            upper = metrics.right,
            step = horizontalMajorStep,
        ).forEach { x ->
            val screenX = metrics.toScreen(Offset(x, 0f)).x
            drawLine(
                color = GridColor,
                start = Offset(screenX, 0f),
                end = Offset(screenX, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
        gridValues(
            lower = metrics.bottom,
            upper = metrics.top,
            step = verticalMajorStep,
        ).forEach { y ->
            val screenY = metrics.toScreen(Offset(0f, y)).y
            drawLine(
                color = GridColor,
                start = Offset(0f, screenY),
                end = Offset(size.width, screenY),
                strokeWidth = 1.dp.toPx(),
            )
        }
        val axisOrigin = metrics.toScreen(Offset.Zero)
        drawLine(
            color = AxisColor,
            start = Offset(0f, axisOrigin.y),
            end = Offset(size.width, axisOrigin.y),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = AxisColor,
            start = Offset(axisOrigin.x, 0f),
            end = Offset(axisOrigin.x, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        drawAxisDecorations(
            metrics = metrics,
            horizontalMajorStep = horizontalMajorStep,
            verticalMajorStep = verticalMajorStep,
            textMeasurer = textMeasurer,
            fontFamily = axisFontFamily,
        )

        drawFunction(
            metrics = metrics,
            color = SineColor,
            function = { x -> sineAmplitude * sin(x * sineFrequency) },
        )
        drawFunction(
            metrics = metrics,
            color = ParabolaColor,
            function = { x -> parabolaQuadratic * x * x + parabolaConstant },
        )

        drawCircle(
            color = CircleColor,
            radius = circleRadius * metrics.scale,
            center = metrics.toScreen(circleCenter),
            style = Stroke(width = 2.5.dp.toPx()),
        )

        val direction = controlPoint - fixedPoint
        drawLine(
            color = Color(0xFF49545D),
            start = metrics.toScreen(fixedPoint - direction * 8f),
            end = metrics.toScreen(fixedPoint + direction * 8f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        intersections.forEach { point ->
            drawCircle(
                color = BoardBackground,
                radius = 6.dp.toPx(),
                center = metrics.toScreen(point),
            )
            drawCircle(
                color = CircleColor,
                radius = 6.dp.toPx(),
                center = metrics.toScreen(point),
                style = Stroke(width = 2.5.dp.toPx()),
            )
        }

        drawCircle(
            color = Color(0xFF6F7780),
            radius = 5.dp.toPx(),
            center = metrics.toScreen(fixedPoint),
        )
        drawCircle(
            color = BoardBackground,
            radius = 11.dp.toPx(),
            center = metrics.toScreen(controlPoint),
        )
        drawCircle(
            color = ControlColor,
            radius = 9.dp.toPx(),
            center = metrics.toScreen(controlPoint),
        )
    }
}

@Composable
private fun GeometryReadout(
    controlPoint: Offset,
    intersectionCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "P  (${formatCoordinate(controlPoint.x)}, ${formatCoordinate(controlPoint.y)})",
                style = MaterialTheme.typography.labelLarge,
                color = InkColor,
            )
            Text(
                text = "Intersections  $intersectionCount",
                style = MaterialTheme.typography.labelLarge,
                color = CircleColor,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(SineColor, "2 sin(0.8x)")
            LegendItem(ParabolaColor, "0.16x^2 - 2.5")
            LegendItem(CircleColor, "circle")
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Spacer(
            modifier = Modifier
                .size(9.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedColor,
        )
    }
}

private fun calculateIntersections(
    fixedPoint: Offset,
    controlPoint: Offset,
    circleCenter: Offset,
    circleRadius: Float,
): List<Offset> {
    val line = Mat.normalize(
        doubleArrayOf(
            (fixedPoint.x * controlPoint.y - fixedPoint.y * controlPoint.x).toDouble(),
            (fixedPoint.y - controlPoint.y).toDouble(),
            (controlPoint.x - fixedPoint.x).toDouble(),
            0.0,
            1.0,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
        ),
    )
    val circle = Mat.normalize(
        doubleArrayOf(
            (
                circleCenter.x * circleCenter.x +
                    circleCenter.y * circleCenter.y -
                    circleRadius * circleRadius
            ).toDouble(),
            (-2.0f * circleCenter.x).toDouble(),
            (-2.0f * circleCenter.y).toDouble(),
            1.0,
            (2.0f * circleRadius).toDouble(),
            0.0,
            0.0,
            0.0,
        ),
    )
    return listOf(
        Geometry.meetLineCircle(line, circle, 0),
        Geometry.meetLineCircle(line, circle, 1),
    ).mapNotNull { coordinates ->
        if (
            coordinates[0] == 0.0 ||
            !coordinates[1].isFinite() ||
            !coordinates[2].isFinite()
        ) {
            null
        } else {
            Offset(coordinates[1].toFloat(), coordinates[2].toFloat())
        }
    }
}

// JSXGraph: src/options.js -> board.defaultAxes and axis.ticks defaults.
private fun DrawScope.drawAxisDecorations(
    metrics: BoardMetrics,
    horizontalMajorStep: Float,
    verticalMajorStep: Float,
    textMeasurer: TextMeasurer,
    fontFamily: FontFamily,
) {
    val axisOrigin = metrics.toScreen(Offset.Zero)
    val tickColor = AxisColor.copy(alpha = 0.25f)
    val textStyle = TextStyle(
        color = Color.Black,
        fontSize = 12.sp,
        fontFamily = fontFamily,
        letterSpacing = 0.sp,
    )

    minorTickValues(
        lower = metrics.left,
        upper = metrics.right,
        majorStep = horizontalMajorStep,
    ).forEach { value ->
        val horizontal = metrics.toScreen(Offset(value, 0f))
        drawLine(
            color = tickColor,
            start = Offset(horizontal.x, axisOrigin.y),
            end = Offset(horizontal.x, axisOrigin.y + 5.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
        )
    }
    minorTickValues(
        lower = metrics.bottom,
        upper = metrics.top,
        majorStep = verticalMajorStep,
    ).forEach { value ->
        val vertical = metrics.toScreen(Offset(0f, value))
        drawLine(
            color = tickColor,
            start = Offset(axisOrigin.x - 5.dp.toPx(), vertical.y),
            end = Offset(axisOrigin.x, vertical.y),
            strokeWidth = 1.dp.toPx(),
        )
    }

    gridValues(
        lower = metrics.left,
        upper = metrics.right,
        step = horizontalMajorStep,
    ).filterNot(::isZero).forEach { value ->
        val horizontal = metrics.toScreen(Offset(value, 0f))
        val horizontalLabel = textMeasurer.measure(formatAxisValue(value), textStyle)
        drawText(
            textLayoutResult = horizontalLabel,
            topLeft = Offset(
                x = horizontal.x - horizontalLabel.size.width * 0.5f,
                y = axisOrigin.y + 3.dp.toPx(),
            ),
        )
    }
    gridValues(
        lower = metrics.bottom,
        upper = metrics.top,
        step = verticalMajorStep,
    ).filterNot(::isZero).forEach { value ->
        val vertical = metrics.toScreen(Offset(0f, value))
        val verticalLabel = textMeasurer.measure(formatAxisValue(value), textStyle)
        drawText(
            textLayoutResult = verticalLabel,
            topLeft = Offset(
                x = axisOrigin.x - verticalLabel.size.width - 6.dp.toPx(),
                y = vertical.y - verticalLabel.size.height * 0.5f,
            ),
        )
    }

    val arrowSize = 8.dp.toPx()
    val horizontalArrow = Path().apply {
        moveTo(size.width - 4.dp.toPx(), axisOrigin.y)
        lineTo(size.width - arrowSize - 4.dp.toPx(), axisOrigin.y - arrowSize * 0.6f)
        lineTo(size.width - arrowSize - 4.dp.toPx(), axisOrigin.y + arrowSize * 0.6f)
        close()
    }
    drawPath(horizontalArrow, AxisColor)
    val verticalArrow = Path().apply {
        moveTo(axisOrigin.x, 4.dp.toPx())
        lineTo(axisOrigin.x - arrowSize * 0.6f, arrowSize + 4.dp.toPx())
        lineTo(axisOrigin.x + arrowSize * 0.6f, arrowSize + 4.dp.toPx())
        close()
    }
    drawPath(verticalArrow, AxisColor)
}

private fun DrawScope.drawFunction(
    metrics: BoardMetrics,
    color: Color,
    function: (Float) -> Float,
) {
    val path = Path()
    val samples = 240
    for (index in 0..samples) {
        val x = -6.0f + 12.0f * index / samples
        val point = metrics.toScreen(Offset(x, function(x)))
        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
    )
}

internal data class BoardMetrics(
    val width: Float,
    val height: Float,
    val requestedLeft: Float = -6.0f,
    val requestedTop: Float = 5.0f,
    val requestedRight: Float = 6.0f,
    val requestedBottom: Float = -5.0f,
    val keepAspectRatio: Boolean = true,
) {
    private val requestedWidth = requestedRight - requestedLeft
    private val requestedHeight = requestedTop - requestedBottom
    private val requestedCenterX = (requestedLeft + requestedRight) * 0.5f
    private val requestedCenterY = (requestedTop + requestedBottom) * 0.5f
    private val uniformScale =
        min(width / requestedWidth, height / requestedHeight)
    val scaleX: Float =
        if (keepAspectRatio) uniformScale else width / requestedWidth
    val scaleY: Float =
        if (keepAspectRatio) uniformScale else height / requestedHeight
    val scale: Float = min(scaleX, scaleY)
    val left: Float = requestedCenterX - width * 0.5f / scaleX
    val right: Float = requestedCenterX + width * 0.5f / scaleX
    val top: Float = requestedCenterY + height * 0.5f / scaleY
    val bottom: Float = requestedCenterY - height * 0.5f / scaleY

    fun toScreen(point: Offset): Offset = Offset(
        x = (point.x - left) * scaleX,
        y = (top - point.y) * scaleY,
    )

    fun toUser(point: Offset): Offset = Offset(
        x = left + point.x / scaleX,
        y = top - point.y / scaleY,
    )

    // JSXGraph: src/base/ticks.js -> getDistanceMajorTicks.
    fun majorTickDistance(
        visibleDistance: Float,
        density: Float,
        pixelsPerUnit: Float = scale,
    ): Float =
        jsxGraphMajorTickDistance(
            visibleDistance = visibleDistance,
            // JSXGraph board units are CSS pixels, which correspond to Compose dp.
            cssPixelsPerUnit = pixelsPerUnit / density,
        )
}

private const val POINT_HIT_PRECISION_DP = 4.0f
private const val POINT_HIT_PADDING_DP = 2.0f

// JSXGraph: src/base/ticks.js -> getDistanceMajorTicks.
internal fun jsxGraphMajorTickDistance(
    visibleDistance: Float,
    cssPixelsPerUnit: Float,
): Float {
    val maximumDistance = visibleDistance.toDouble() / 6.0
    val minimumDistance = 5.0 / cssPixelsPerUnit * 5.0

    var minimumDelta = 10.0.pow(floor(log10(minimumDistance)))
    if (2.0 * minimumDelta >= minimumDistance) {
        minimumDelta *= 2.0
    } else if (5.0 * minimumDelta >= minimumDistance) {
        minimumDelta *= 5.0
    }

    var maximumDelta = 10.0.pow(floor(log10(maximumDistance)))
    if (5.0 * maximumDelta < maximumDistance) {
        maximumDelta *= 5.0
    } else if (2.0 * maximumDelta < maximumDistance) {
        maximumDelta *= 2.0
    }
    return maxOf(minimumDelta, maximumDelta).toFloat()
}

// JSXGraph: src/math/math.js -> roundToStep and src/element/grid.js -> updateDataArray.
private fun gridValues(
    lower: Float,
    upper: Float,
    step: Float,
): List<Float> {
    val first = Mat.roundToStep(
        value = lower.toDouble(),
        step = step.toDouble(),
    ).toFloat()
    val values = mutableListOf<Float>()
    var value = first
    while (value <= upper + 1e-6f) {
        if (value > lower + 1e-6f && value < upper - 1e-6f) {
            values += value
        }
        value += step
    }
    return values
}

private fun minorTickValues(
    lower: Float,
    upper: Float,
    majorStep: Float,
): List<Float> {
    val minorStep = majorStep / 5.0f
    return gridValues(lower, upper, minorStep)
        .filter { value ->
            val majorIndex = value / majorStep
            kotlin.math.abs(majorIndex - majorIndex.roundToInt()) > 1e-5f
        }
}

private fun isZero(value: Float): Boolean = kotlin.math.abs(value) < 1e-6f

private fun formatAxisValue(value: Float): String {
    val rounded = value.roundToInt()
    return if (kotlin.math.abs(value - rounded) < 1e-6f) {
        rounded.toString()
    } else {
        value.toString()
    }
}

private fun formatCoordinate(value: Float): String =
    (value * 10.0f).roundToInt().div(10.0f).toString()
