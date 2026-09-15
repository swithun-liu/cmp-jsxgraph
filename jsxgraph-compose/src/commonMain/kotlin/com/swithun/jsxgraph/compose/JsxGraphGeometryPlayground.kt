/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
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
        modifier = modifier
            .background(BoardBackground)
            .border(1.dp, Color(0xFFD4DADF)),
    ) {
        val bounds = scene.boundingBox
        val metrics = BoardMetrics(
            width = size.width,
            height = size.height,
            requestedLeft = bounds.left.toFloat(),
            requestedTop = bounds.top.toFloat(),
            requestedRight = bounds.right.toFloat(),
            requestedBottom = bounds.bottom.toFloat(),
            keepAspectRatio = scene.keepAspectRatio,
        )
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

        if (scene.grid) {
            drawGrid(
                metrics = metrics,
                horizontalMajorStep = horizontalMajorStep,
                verticalMajorStep = verticalMajorStep,
            )
        }
        if (scene.axis) {
            drawAxes(
                metrics = metrics,
                horizontalMajorStep = horizontalMajorStep,
                verticalMajorStep = verticalMajorStep,
                textMeasurer = textMeasurer,
                fontFamily = axisFontFamily,
            )
        }
        for (element in scene.elements) {
            if (!element.style.visible) {
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
            }
        }
    }
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
            style = Stroke(width = point.style.strokeWidth.dp.toPx()),
        )
    }
}

private fun DrawScope.drawSceneLine(
    line: JsxGraphSceneElement.Line,
    metrics: BoardMetrics,
) {
    val point1 = metrics.toScreen(line.point1.toOffset())
    val point2 = metrics.toScreen(line.point2.toOffset())
    val delta = point2 - point1
    val length = hypot(delta.x, delta.y)
    if (length == 0.0f || !length.isFinite()) {
        return
    }
    val extension = Offset(
        x = delta.x / length * hypot(size.width, size.height) * 2.0f,
        y = delta.y / length * hypot(size.width, size.height) * 2.0f,
    )
    val start = if (line.straightFirst) point1 - extension else point1
    val end = if (line.straightLast) point2 + extension else point2
    val color = line.style.strokeColor.toComposeColor(
        opacity = line.style.strokeOpacity,
    )
    if (color.alpha > 0.0f && line.style.strokeWidth > 0.0) {
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = line.style.strokeWidth.dp.toPx(),
            cap = StrokeCap.Butt,
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
    val stroke = circle.style.strokeColor.toComposeColor(
        opacity = circle.style.strokeOpacity,
    )
    if (stroke.alpha > 0.0f && circle.style.strokeWidth > 0.0) {
        drawOval(
            color = stroke,
            topLeft = topLeft,
            size = ellipseSize,
            style = Stroke(width = circle.style.strokeWidth.dp.toPx()),
        )
    }
}

// JSXGraph: src/renderer/abstract.js -> updatePathStringPoint / drawCurve.
private fun DrawScope.drawSceneCurve(
    curve: JsxGraphSceneElement.Curve,
    metrics: BoardMetrics,
) {
    if (curve.bezierDegree != 1) {
        return
    }
    val path = Path()
    var startsSubpath = true
    var segmentCount = 0
    for (point in curve.points) {
        if (point == null) {
            startsSubpath = true
            continue
        }
        val screen = metrics.toScreen(point.toOffset())
        if (!screen.x.isFinite() || !screen.y.isFinite()) {
            startsSubpath = true
            continue
        }
        if (startsSubpath) {
            path.moveTo(screen.x, screen.y)
            startsSubpath = false
        } else {
            path.lineTo(screen.x, screen.y)
            segmentCount += 1
        }
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
            ),
        )
    }
}

private fun com.swithun.jsxgraph.core.JsxGraphPoint2D.toOffset(): Offset =
    Offset(x.toFloat(), y.toFloat())

private fun JsxGraphColor.toComposeColor(
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

private data class BoardMetrics(
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
