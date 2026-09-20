/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/renderer/abstract.js -> getArrowHeadData /
 * getPositionArrowHead, src/renderer/canvas.js -> drawArrows.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.swithun.jsxgraph.core.JsxGraphArrowHead
import kotlin.math.abs
import kotlin.math.hypot

internal data class LineRenderGeometry(
    val strokeStart: Offset,
    val strokeEnd: Offset,
    val firstArrow: ArrowHeadGeometry?,
    val lastArrow: ArrowHeadGeometry?,
)

internal data class ArrowHeadGeometry(
    val type: Int,
    val points: List<Offset>,
    val bezierDegree: Int,
    val filled: Boolean,
)

private data class ArrowHeadData(
    val offset: Float,
    val minimumLengthContribution: Float,
)

internal fun lineRenderGeometry(
    point1: Offset,
    point2: Offset,
    straightFirst: Boolean,
    straightLast: Boolean,
    viewportSize: Size,
    strokeWidth: Float,
    firstArrow: JsxGraphArrowHead?,
    lastArrow: JsxGraphArrowHead?,
): LineRenderGeometry? {
    val delta = point2 - point1
    val sourceLength = hypot(delta.x, delta.y)
    if (
        sourceLength == 0.0f ||
        !sourceLength.isFinite() ||
        !strokeWidth.isFinite() ||
        strokeWidth < 0.0f
    ) {
        return null
    }
    val hasArrow = firstArrow != null || lastArrow != null
    val (displayStart, displayEnd) = displayLineEndpoints(
        point1 = point1,
        point2 = point2,
        straightFirst = straightFirst,
        straightLast = straightLast,
        viewportSize = viewportSize,
        inset = if (hasArrow) ARROW_VIEWPORT_INSET else null,
    )
    val displayDelta = displayEnd - displayStart
    val displayLength = hypot(displayDelta.x, displayDelta.y)
    if (displayLength == 0.0f || !displayLength.isFinite()) {
        return null
    }
    val direction = displayDelta / displayLength
    val firstData = firstArrow?.data(strokeWidth)
    val lastData = lastArrow?.data(strokeWidth)
    val minimumLength =
        ARROW_MINIMUM_LENGTH +
            (firstData?.minimumLengthContribution ?: 0.0f) +
            (lastData?.minimumLengthContribution ?: 0.0f)
    val shorten = displayLength >= minimumLength
    val strokeStart =
        if (shorten && firstData != null) {
            displayStart + direction * firstData.offset
        } else {
            displayStart
        }
    val strokeEnd =
        if (shorten && lastData != null) {
            displayEnd - direction * lastData.offset
        } else {
            displayEnd
        }
    return LineRenderGeometry(
        strokeStart = strokeStart,
        strokeEnd = strokeEnd,
        firstArrow = firstArrow?.let {
            arrowHeadGeometry(
                arrow = it,
                anchor = displayStart,
                direction = direction,
                strokeWidth = strokeWidth,
                isFirst = true,
            )
        },
        lastArrow = lastArrow?.let {
            arrowHeadGeometry(
                arrow = it,
                anchor = displayEnd,
                direction = direction,
                strokeWidth = strokeWidth,
                isFirst = false,
            )
        },
    )
}

private fun displayLineEndpoints(
    point1: Offset,
    point2: Offset,
    straightFirst: Boolean,
    straightLast: Boolean,
    viewportSize: Size,
    inset: Float?,
): Pair<Offset, Offset> {
    if (!straightFirst && !straightLast) {
        return point1 to point2
    }
    if (inset != null) {
        lineViewportIntersections(
            point1 = point1,
            point2 = point2,
            viewportSize = viewportSize,
            inset = inset,
        )?.let { intersections ->
            return (
                if (straightFirst) intersections.first else point1
                ) to (
                if (straightLast) intersections.second else point2
                )
        }
    }
    val delta = point2 - point1
    val length = hypot(delta.x, delta.y)
    val extension = delta / length *
        (hypot(viewportSize.width, viewportSize.height) * 2.0f)
    return (
        if (straightFirst) point1 - extension else point1
        ) to (
        if (straightLast) point2 + extension else point2
        )
}

private fun lineViewportIntersections(
    point1: Offset,
    point2: Offset,
    viewportSize: Size,
    inset: Float,
): Pair<Offset, Offset>? {
    val left = inset
    val top = inset
    val right = viewportSize.width - inset
    val bottom = viewportSize.height - inset
    if (left > right || top > bottom) {
        return null
    }
    val delta = point2 - point1
    val candidates = mutableListOf<Pair<Float, Offset>>()

    fun addCandidate(parameter: Float, point: Offset) {
        if (
            parameter.isFinite() &&
            point.x >= left - VIEWPORT_EPSILON &&
            point.x <= right + VIEWPORT_EPSILON &&
            point.y >= top - VIEWPORT_EPSILON &&
            point.y <= bottom + VIEWPORT_EPSILON &&
            candidates.none {
                abs(it.first - parameter) <= VIEWPORT_EPSILON
            }
        ) {
            candidates += parameter to point
        }
    }

    if (abs(delta.x) > VIEWPORT_EPSILON) {
        val leftParameter = (left - point1.x) / delta.x
        addCandidate(
            leftParameter,
            Offset(left, point1.y + leftParameter * delta.y),
        )
        val rightParameter = (right - point1.x) / delta.x
        addCandidate(
            rightParameter,
            Offset(right, point1.y + rightParameter * delta.y),
        )
    }
    if (abs(delta.y) > VIEWPORT_EPSILON) {
        val topParameter = (top - point1.y) / delta.y
        addCandidate(
            topParameter,
            Offset(point1.x + topParameter * delta.x, top),
        )
        val bottomParameter = (bottom - point1.y) / delta.y
        addCandidate(
            bottomParameter,
            Offset(point1.x + bottomParameter * delta.x, bottom),
        )
    }
    if (candidates.size < 2) {
        return null
    }
    val ordered = candidates.sortedBy(Pair<Float, Offset>::first)
    return ordered.first().second to ordered.last().second
}

private fun JsxGraphArrowHead.data(
    strokeWidth: Float,
): ArrowHeadData {
    val scaledSize = strokeWidth * size.toFloat()
    return when (type) {
        2 -> ArrowHeadData(
            offset = scaledSize * 0.5f,
            minimumLengthContribution = scaledSize,
        )
        3 -> ArrowHeadData(
            offset = scaledSize / 3.0f,
            minimumLengthContribution = strokeWidth,
        )
        4, 5, 6 -> ArrowHeadData(
            offset = scaledSize / 1.5f,
            minimumLengthContribution = scaledSize,
        )
        7 -> ArrowHeadData(
            offset = 0.0f,
            minimumLengthContribution = strokeWidth,
        )
        else -> ArrowHeadData(
            offset = scaledSize,
            minimumLengthContribution = scaledSize,
        )
    }
}

private fun arrowHeadGeometry(
    arrow: JsxGraphArrowHead,
    anchor: Offset,
    direction: Offset,
    strokeWidth: Float,
    isFirst: Boolean,
): ArrowHeadGeometry {
    val scaledSize = strokeWidth * arrow.size.toFloat()
    val (localPoints, degree) = when (arrow.type) {
        2 -> listOf(
            Offset(-scaledSize, -scaledSize * 0.5f),
            Offset.Zero,
            Offset(-scaledSize, scaledSize * 0.5f),
            Offset(-scaledSize * 0.5f, 0.0f),
        ) to 1
        3 -> listOf(
            Offset(-scaledSize / 3.0f, -scaledSize * 0.5f),
            Offset(0.0f, -scaledSize * 0.5f),
            Offset(0.0f, scaledSize * 0.5f),
            Offset(-scaledSize / 3.0f, scaledSize * 0.5f),
        ) to 1
        4 -> cubicArrowPoints(
            strokeWidth = strokeWidth,
            size = arrow.size.toFloat(),
            centerY = 3.31f,
            source = TYPE_FOUR_POINTS,
        ) to 3
        5 -> cubicArrowPoints(
            strokeWidth = strokeWidth,
            size = arrow.size.toFloat(),
            centerY = 3.28f,
            source = TYPE_FIVE_POINTS,
        ) to 3
        6 -> cubicArrowPoints(
            strokeWidth = strokeWidth,
            size = arrow.size.toFloat(),
            centerY = 2.84f,
            source = TYPE_SIX_POINTS,
        ) to 3
        7 -> cubicArrowPoints(
            strokeWidth = strokeWidth,
            size = 10.0f,
            centerY = 5.2f,
            source = TYPE_SEVEN_POINTS,
        ) to 3
        else -> listOf(
            Offset(-scaledSize, -scaledSize * 0.5f),
            Offset.Zero,
            Offset(-scaledSize, scaledSize * 0.5f),
        ) to 1
    }
    val oriented = localPoints.map { point ->
        val local =
            if (isFirst) {
                Offset(-point.x, point.y)
            } else {
                point
            }
        anchor + Offset(
            x = local.x * direction.x - local.y * direction.y,
            y = local.x * direction.y + local.y * direction.x,
        )
    }
    return ArrowHeadGeometry(
        type = arrow.type,
        points = oriented,
        bezierDegree = degree,
        filled = arrow.type != 7,
    )
}

private fun cubicArrowPoints(
    strokeWidth: Float,
    size: Float,
    centerY: Float,
    source: List<Offset>,
): List<Offset> {
    val scale = strokeWidth * size / 10.0f
    return source.map { point ->
        Offset(
            x = point.x * scale - 10.0f * scale,
            y = point.y * scale - centerY * scale,
        )
    }
}

private const val ARROW_VIEWPORT_INSET = 4.0f
private const val ARROW_MINIMUM_LENGTH = 1.0e-15f
private const val VIEWPORT_EPSILON = 1.0e-4f

private val TYPE_FOUR_POINTS = listOf(
    Offset(10.0f, 3.31f),
    Offset(6.47f, 3.84f),
    Offset(2.87f, 4.5f),
    Offset(0.0f, 6.63f),
    Offset(0.67f, 5.52f),
    Offset(1.33f, 4.42f),
    Offset(2.0f, 3.31f),
    Offset(1.33f, 2.21f),
    Offset(0.67f, 1.1f),
    Offset(0.0f, 0.0f),
    Offset(2.87f, 2.13f),
    Offset(6.47f, 2.79f),
    Offset(10.0f, 3.31f),
)

private val TYPE_FIVE_POINTS = listOf(
    Offset(10.0f, 3.28f),
    Offset(6.61f, 4.19f),
    Offset(3.19f, 5.07f),
    Offset(0.0f, 6.55f),
    Offset(0.62f, 5.56f),
    Offset(1.0f, 4.44f),
    Offset(1.0f, 3.28f),
    Offset(1.0f, 2.11f),
    Offset(0.62f, 0.99f),
    Offset(0.0f, 0.0f),
    Offset(3.19f, 1.49f),
    Offset(6.61f, 2.37f),
    Offset(10.0f, 3.28f),
)

private val TYPE_SIX_POINTS = listOf(
    Offset(10.0f, 2.84f),
    Offset(6.61f, 3.59f),
    Offset(3.21f, 4.35f),
    Offset(0.0f, 5.68f),
    Offset(0.33f, 4.73f),
    Offset(0.67f, 3.78f),
    Offset(1.0f, 2.84f),
    Offset(0.67f, 1.89f),
    Offset(0.33f, 0.95f),
    Offset(0.0f, 0.0f),
    Offset(3.21f, 1.33f),
    Offset(6.61f, 2.09f),
    Offset(10.0f, 2.84f),
)

private val TYPE_SEVEN_POINTS = listOf(
    Offset(0.0f, 10.39f),
    Offset(2.01f, 6.92f),
    Offset(5.96f, 5.2f),
    Offset(10.0f, 5.2f),
    Offset(5.96f, 5.2f),
    Offset(2.01f, 3.47f),
    Offset(0.0f, 0.0f),
)
