/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/grid.js -> JXG.createGrid / updateDataArray
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Andreas Walter, Alfred Wassermann, and contributors.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class JsxGraphGridRole {
    Major,
    Minor,
}

enum class JsxGraphGridForceSquare {
    None,
    Min,
    Max,
}

sealed interface JsxGraphGridLength {
    data object Auto : JsxGraphGridLength

    data class User(val value: Double) : JsxGraphGridLength

    data class Percent(val value: Double) : JsxGraphGridLength

    data class Fraction(val value: Double) : JsxGraphGridLength

    data class Pixels(val value: Double) : JsxGraphGridLength
}

sealed interface JsxGraphGridMinorElements {
    data object Auto : JsxGraphGridMinorElements

    data class Fixed(val value: Double) : JsxGraphGridMinorElements
}

data class JsxGraphGridPair<T>(
    val x: T,
    val y: T,
)

data class JsxGraphGridDrawZero(
    val origin: Boolean,
    val x: Boolean,
    val y: Boolean,
) {
    companion object {
        val All = JsxGraphGridDrawZero(
            origin = true,
            x = true,
            y = true,
        )
    }
}

data class JsxGraphGridFace(
    val face: String,
    val size: JsxGraphGridPair<JsxGraphGridLength>,
    val margin: Double,
    val drawZero: JsxGraphGridDrawZero,
    val polygonVertices: Int,
)

data class JsxGraphResolvedGrid(
    val points: List<JsxGraphPoint2D?>,
    val bezierDegree: Int,
    val lineCap: String,
    val pointStrokeWidth: Double?,
)

sealed interface JsxGraphGridResolveError {
    data class PointLimitExceeded(
        val limit: Int,
        val requestedSize: Long,
    ) : JsxGraphGridResolveError
}

/**
 * Viewport-dependent Grid geometry.
 *
 * JSXGraph stores Grid as two Curve instances, but both `majorStep: auto` and
 * pixel/relative units depend on the final renderer dimensions. Keeping this
 * definition in the scene preserves the upstream updateDataArray timing.
 */
data class JsxGraphGrid2D(
    val role: JsxGraphGridRole,
    val majorStep: JsxGraphGridPair<JsxGraphGridLength>,
    val minorElements: JsxGraphGridPair<JsxGraphGridMinorElements>,
    val forceSquare: JsxGraphGridForceSquare,
    val includeBoundaries: Boolean,
    val major: JsxGraphGridFace,
    val minor: JsxGraphGridFace,
    val parentMajorStep: JsxGraphGridPair<Double?> =
        JsxGraphGridPair(null, null),
    val parentMinorElements: JsxGraphGridPair<Double?> =
        JsxGraphGridPair(null, null),
    val maximumPointCount: Int = DEFAULT_MAXIMUM_POINT_COUNT,
) {
    fun resolve(
        visibleLeft: Double,
        visibleTop: Double,
        visibleRight: Double,
        visibleBottom: Double,
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): GMResult<JsxGraphResolvedGrid, JsxGraphGridResolveError> {
        if (maximumPointCount < 1) {
            return GMResult.Err(
                JsxGraphGridResolveError.PointLimitExceeded(
                    limit = maximumPointCount,
                    requestedSize = 1L,
                ),
            )
        }
        val boundingBox = doubleArrayOf(
            visibleLeft,
            visibleTop,
            visibleRight,
            visibleBottom,
        )
        val resolvedMajorStep = resolveMajorStep(
            boundingBox = boundingBox,
            unitX = cssPixelsPerUnitX,
            unitY = cssPixelsPerUnitY,
        )
        val majorSize = JsxGraphGridPair(
            x = major.size.x.resolve(
                relativeTo = resolvedMajorStep.x,
                pixelsToUser = 1.0 / cssPixelsPerUnitX,
            ),
            y = major.size.y.resolve(
                relativeTo = resolvedMajorStep.y,
                pixelsToUser = 1.0 / cssPixelsPerUnitY,
            ),
        )
        val majorRadius = JsxGraphGridPair(
            x = majorSize.x * 0.5,
            y = majorSize.y * 0.5,
        )
        val resolvedPoints = if (role == JsxGraphGridRole.Major) {
            resolveMajorPoints(
                boundingBox = boundingBox,
                unitX = cssPixelsPerUnitX,
                unitY = cssPixelsPerUnitY,
                step = resolvedMajorStep,
                radius = majorRadius,
            )
        } else {
            val resolvedMinorElements = JsxGraphGridPair(
                x = minorElements.x.resolve(parentMinorElements.x),
                y = minorElements.y.resolve(parentMinorElements.y),
            )
            val minorStep = JsxGraphGridPair(
                x = resolvedMajorStep.x / (resolvedMinorElements.x + 1.0),
                y = resolvedMajorStep.y / (resolvedMinorElements.y + 1.0),
            )
            val minorSize = JsxGraphGridPair(
                x = minor.size.x.resolve(
                    relativeTo = minorStep.x,
                    pixelsToUser = 1.0 / cssPixelsPerUnitX,
                ),
                y = minor.size.y.resolve(
                    relativeTo = minorStep.y,
                    pixelsToUser = 1.0 / cssPixelsPerUnitY,
                ),
            )
            val minorRadius = JsxGraphGridPair(
                x = minorSize.x * 0.5,
                y = minorSize.y * 0.5,
            )
            when (
                val points = resolveMinorPoints(
                    boundingBox = boundingBox,
                    unitX = cssPixelsPerUnitX,
                    unitY = cssPixelsPerUnitY,
                    majorStep = resolvedMajorStep,
                    minorStep = minorStep,
                    majorRadius = majorRadius,
                    minorRadius = minorRadius,
                )
            ) {
                is GMResult.Ok ->
                    return GMResult.Ok(
                        resolved(
                            points = points.value,
                            face = minor,
                            radius = minorRadius,
                            unitX = cssPixelsPerUnitX,
                            unitY = cssPixelsPerUnitY,
                        ),
                    )
                is GMResult.Err -> return points
            }
        }
        return when (resolvedPoints) {
            is GMResult.Ok ->
                GMResult.Ok(
                    resolved(
                        points = resolvedPoints.value,
                        face = major,
                        radius = majorRadius,
                        unitX = cssPixelsPerUnitX,
                        unitY = cssPixelsPerUnitY,
                    ),
                )
            is GMResult.Err -> resolvedPoints
        }
    }

    private fun resolved(
        points: List<JsxGraphPoint2D?>,
        face: JsxGraphGridFace,
        radius: JsxGraphGridPair<Double>,
        unitX: Double,
        unitY: Double,
    ): JsxGraphResolvedGrid {
        val normalizedFace = face.face.lowercase()
        return JsxGraphResolvedGrid(
            points = points,
            bezierDegree =
                if (normalizedFace == "o" || normalizedFace == "circle") {
                    3
                } else {
                    1
                },
            lineCap =
                when (normalizedFace) {
                    "o", "circle", "[]", "square", "<>", "diamond",
                    "<<>>", "diamond2",
                    -> "square"
                    else -> "round"
                },
            pointStrokeWidth =
                if (normalizedFace == "." || normalizedFace == "point") {
                    radius.x * unitX + radius.y * unitY
                } else {
                    null
                },
        )
    }

    private fun resolveMajorStep(
        boundingBox: DoubleArray,
        unitX: Double,
        unitY: Double,
    ): JsxGraphGridPair<Double> {
        var x = majorStep.x.resolveStep(
            relativeTo = abs(boundingBox[1] - boundingBox[3]),
            pixelsToUser = 1.0 / unitX,
            auto = parentMajorStep.x ?: autoStep(unitX),
        )
        var y = majorStep.y.resolveStep(
            relativeTo = abs(boundingBox[0] - boundingBox[2]),
            pixelsToUser = 1.0 / unitY,
            auto = parentMajorStep.y ?: autoStep(unitY),
        )
        when (forceSquare) {
            JsxGraphGridForceSquare.None -> Unit
            JsxGraphGridForceSquare.Min ->
                if (x * unitX <= y * unitY) {
                    y = x / unitY * unitX
                } else {
                    x = y / unitX * unitY
                }
            JsxGraphGridForceSquare.Max ->
                if (x * unitX <= y * unitY) {
                    x = y / unitX * unitY
                } else {
                    y = x / unitY * unitX
                }
        }
        return JsxGraphGridPair(x, y)
    }

    private fun resolveMajorPoints(
        boundingBox: DoubleArray,
        unitX: Double,
        unitY: Double,
        step: JsxGraphGridPair<Double>,
        radius: JsxGraphGridPair<Double>,
    ): GMResult<List<JsxGraphPoint2D?>, JsxGraphGridResolveError> {
        if (!canDraw(boundingBox, step, inclusiveX = false)) {
            return GMResult.Ok(emptyList())
        }
        val points = GridPointBuffer(maximumPointCount)
        val startX = Mat.roundToStep(boundingBox[0], step.x)
        val startY = Mat.roundToStep(boundingBox[1], step.y)
        val face = major.face.lowercase()
        if (face == "line") {
            var y = startY
            var count = 0
            while (
                y >= boundingBox[3] &&
                count <= MAX_LINES &&
                !points.exceeded
            ) {
                if (
                    shouldDrawMajorLine(
                        value = y,
                        lower = boundingBox[3],
                        upper = boundingBox[1],
                        radius = radius.y,
                        zeroVisible = major.drawZero.origin &&
                            major.drawZero.y,
                    )
                ) {
                    appendPath(
                        points,
                        listOf(
                            JsxGraphPoint2D(
                                boundingBox[0] - major.margin / unitX,
                                y,
                            ),
                            JsxGraphPoint2D(
                                boundingBox[2] + major.margin / unitX,
                                y,
                            ),
                        ),
                    )
                }
                y -= step.y
                count += 1
            }
            var x = startX
            count = 0
            while (
                x <= boundingBox[2] &&
                count <= MAX_LINES &&
                !points.exceeded
            ) {
                if (
                    shouldDrawMajorLine(
                        value = x,
                        lower = boundingBox[0],
                        upper = boundingBox[2],
                        radius = radius.x,
                        zeroVisible = major.drawZero.origin &&
                            major.drawZero.x,
                    )
                ) {
                    appendPath(
                        points,
                        listOf(
                            JsxGraphPoint2D(
                                x,
                                boundingBox[1] + major.margin / unitY,
                            ),
                            JsxGraphPoint2D(
                                x,
                                boundingBox[3] - major.margin / unitY,
                            ),
                        ),
                    )
                }
                x += step.x
                count += 1
            }
            return points.result()
        }

        var y = startY
        var yCount = 0
        while (
            y >= boundingBox[3] &&
            yCount <= MAX_LINES &&
            !points.exceeded
        ) {
            var x = startX
            var xCount = 0
            while (
                x <= boundingBox[2] &&
                xCount <= MAX_LINES &&
                !points.exceeded
            ) {
                val atOrigin = abs(y) < EPSILON && abs(x) < EPSILON
                val onXAxis = abs(y) < EPSILON && abs(x) >= EPSILON
                val onYAxis = abs(x) < EPSILON && abs(y) >= EPSILON
                val excluded =
                    (!major.drawZero.origin && atOrigin) ||
                        (!major.drawZero.x && onXAxis) ||
                        (!major.drawZero.y && onYAxis) ||
                        (
                            !includeBoundaries &&
                                (
                                    x <= boundingBox[0] + radius.x ||
                                        x >= boundingBox[2] - radius.x ||
                                        y <= boundingBox[3] + radius.y ||
                                        y >= boundingBox[1] - radius.y
                                    )
                            )
                if (!excluded) {
                    appendFace(
                        target = points,
                        face = major,
                        x = x,
                        y = y,
                        radiusX = radius.x,
                        radiusY = radius.y,
                        boundingBox = boundingBox,
                        unitX = unitX,
                        unitY = unitY,
                    )
                }
                x += step.x
                xCount += 1
            }
            y -= step.y
            yCount += 1
        }
        return points.result()
    }

    private fun shouldDrawMajorLine(
        value: Double,
        lower: Double,
        upper: Double,
        radius: Double,
        zeroVisible: Boolean,
    ): Boolean =
        (zeroVisible || abs(value) >= EPSILON) &&
            (
                includeBoundaries ||
                    (value > lower + radius && value < upper - radius)
                )

    private fun resolveMinorPoints(
        boundingBox: DoubleArray,
        unitX: Double,
        unitY: Double,
        majorStep: JsxGraphGridPair<Double>,
        minorStep: JsxGraphGridPair<Double>,
        majorRadius: JsxGraphGridPair<Double>,
        minorRadius: JsxGraphGridPair<Double>,
    ): GMResult<List<JsxGraphPoint2D?>, JsxGraphGridResolveError> {
        if (!canDraw(boundingBox, minorStep, inclusiveX = true)) {
            return GMResult.Ok(emptyList())
        }
        val points = GridPointBuffer(maximumPointCount)
        val startX = Mat.roundToStep(boundingBox[0], minorStep.x)
        val startY = Mat.roundToStep(boundingBox[1], minorStep.y)
        val minorFace = minor.face.lowercase()
        val majorFace = major.face.lowercase()

        if (minorFace != "line") {
            var y = startY
            var yCount = 0
            while (
                y >= boundingBox[3] &&
                yCount <= MAX_LINES &&
                !points.exceeded
            ) {
                var x = startX
                var xCount = 0
                while (
                    x <= boundingBox[2] &&
                    xCount <= MAX_LINES &&
                    !points.exceeded
                ) {
                    if (
                        !minorOverlapsMajor(
                            x = x,
                            y = y,
                            majorFace = majorFace,
                            majorStep = majorStep,
                            majorRadius = majorRadius,
                            minorRadius = minorRadius,
                        ) &&
                        (minor.drawZero.y || abs(x) >= EPSILON) &&
                        (minor.drawZero.x || abs(y) >= EPSILON) &&
                        (
                            includeBoundaries ||
                                !minorTouchesBoundary(
                                    x = x,
                                    y = y,
                                    boundingBox = boundingBox,
                                    majorStep = majorStep,
                                    majorRadius = majorRadius,
                                    minorRadius = minorRadius,
                                )
                            )
                    ) {
                        appendFace(
                            target = points,
                            face = minor,
                            x = x,
                            y = y,
                            radiusX = minorRadius.x,
                            radiusY = minorRadius.y,
                            boundingBox = boundingBox,
                            unitX = unitX,
                            unitY = unitY,
                        )
                    }
                    x += minorStep.x
                    xCount += 1
                }
                y -= minorStep.y
                yCount += 1
            }
            return points.result()
        }

        var y = startY
        var count = 0
        while (
            y >= boundingBox[3] &&
            count <= MAX_LINES &&
            !points.exceeded
        ) {
            if (
                !minorLineOverlapsMajor(
                    value = y,
                    majorStep = majorStep.y,
                    majorRadius = majorRadius.y,
                    minorRadius = minorRadius.y,
                    majorFace = majorFace,
                    zeroVisible =
                        major.drawZero.origin &&
                            major.drawZero.x &&
                            major.drawZero.y,
                ) &&
                (minor.drawZero.x || abs(y) >= EPSILON) &&
                (
                    includeBoundaries ||
                        !lineTouchesBoundary(
                            value = y,
                            lower = boundingBox[3],
                            upper = boundingBox[1],
                            majorStep = majorStep.y,
                            majorRadius = majorRadius.y,
                            minorRadius = minorRadius.y,
                        )
                    )
            ) {
                appendPath(
                    points,
                    listOf(
                        JsxGraphPoint2D(
                            boundingBox[0] - minor.margin / unitX,
                            y,
                        ),
                        JsxGraphPoint2D(
                            boundingBox[2] + minor.margin / unitX,
                            y,
                        ),
                    ),
                )
            }
            y -= minorStep.y
            count += 1
        }
        var x = startX
        count = 0
        while (
            x <= boundingBox[2] &&
            count <= MAX_LINES &&
            !points.exceeded
        ) {
            if (
                !minorLineOverlapsMajor(
                    value = x,
                    majorStep = majorStep.x,
                    majorRadius = majorRadius.x,
                    minorRadius = minorRadius.x,
                    majorFace = majorFace,
                    zeroVisible =
                        major.drawZero.origin &&
                            major.drawZero.x &&
                            major.drawZero.y,
                ) &&
                (minor.drawZero.y || abs(x) >= EPSILON) &&
                (
                    includeBoundaries ||
                        !lineTouchesBoundary(
                            value = x,
                            lower = boundingBox[0],
                            upper = boundingBox[2],
                            majorStep = majorStep.x,
                            majorRadius = majorRadius.x,
                            minorRadius = minorRadius.x,
                        )
                    )
            ) {
                appendPath(
                    points,
                    listOf(
                        JsxGraphPoint2D(
                            x,
                            boundingBox[1] + minor.margin / unitY,
                        ),
                        JsxGraphPoint2D(
                            x,
                            boundingBox[3] - minor.margin / unitY,
                        ),
                    ),
                )
            }
            x += minorStep.x
            count += 1
        }
        return points.result()
    }

    private fun minorOverlapsMajor(
        x: Double,
        y: Double,
        majorFace: String,
        majorStep: JsxGraphGridPair<Double>,
        majorRadius: JsxGraphGridPair<Double>,
        minorRadius: JsxGraphGridPair<Double>,
    ): Boolean {
        val xTo = distanceToStep(x, majorStep.x)
        val xFrom = majorStep.x - xTo
        val yTo = distanceToStep(y, majorStep.y)
        val yFrom = majorStep.y - yTo
        val xOverlap =
            xTo - minorRadius.x - majorRadius.x < EPSILON ||
                xFrom - minorRadius.x - majorRadius.x < EPSILON
        val yOverlap =
            yTo - minorRadius.y - majorRadius.y < EPSILON ||
                yFrom - minorRadius.y - majorRadius.y < EPSILON
        if (majorFace == "line") {
            return xOverlap || yOverlap
        }
        if (!xOverlap || !yOverlap) {
            return false
        }
        return (
            major.drawZero.origin ||
                majorRadius.y - abs(y) + minorRadius.y < EPSILON ||
                majorRadius.x - abs(x) + minorRadius.x < EPSILON
            ) &&
            (
                major.drawZero.x ||
                    majorRadius.y - abs(y) + minorRadius.y < EPSILON ||
                    majorRadius.x + abs(x) - minorRadius.x < EPSILON
                ) &&
            (
                major.drawZero.y ||
                    majorRadius.x - abs(x) + minorRadius.x < EPSILON ||
                    majorRadius.y + abs(y) - minorRadius.y < EPSILON
                )
    }

    private fun minorLineOverlapsMajor(
        value: Double,
        majorStep: Double,
        majorRadius: Double,
        minorRadius: Double,
        majorFace: String,
        zeroVisible: Boolean,
    ): Boolean {
        val to = distanceToStep(value, majorStep)
        val from = majorStep - to
        val overlaps =
            to - minorRadius - majorRadius < EPSILON ||
                from - minorRadius - majorRadius < EPSILON
        return overlaps && (
            majorFace == "line" ||
                zeroVisible ||
                majorRadius - abs(value) + minorRadius < EPSILON
            )
    }

    private fun minorTouchesBoundary(
        x: Double,
        y: Double,
        boundingBox: DoubleArray,
        majorStep: JsxGraphGridPair<Double>,
        majorRadius: JsxGraphGridPair<Double>,
        minorRadius: JsxGraphGridPair<Double>,
    ): Boolean =
        pointTouchesBoundary(
            value = x,
            lower = boundingBox[0],
            upper = boundingBox[2],
            majorStep = majorStep.x,
            majorRadius = majorRadius.x,
            minorRadius = minorRadius.x,
        ) ||
            pointTouchesBoundary(
                value = y,
                lower = boundingBox[3],
                upper = boundingBox[1],
                majorStep = majorStep.y,
                majorRadius = majorRadius.y,
                minorRadius = minorRadius.y,
            )

    private fun pointTouchesBoundary(
        value: Double,
        lower: Double,
        upper: Double,
        majorStep: Double,
        majorRadius: Double,
        minorRadius: Double,
    ): Boolean {
        val lowerTo = abs(lower % majorStep)
        val upperTo = abs(upper % majorStep)
        val lowerFrom = majorStep - lowerTo
        val upperFrom = majorStep - upperTo
        return (
            value - minorRadius - lower - majorRadius + lowerFrom <
                EPSILON &&
                lowerFrom - majorRadius < EPSILON
            ) ||
            (
                value - minorRadius - lower - majorRadius - lowerTo <
                    EPSILON &&
                    lowerTo - majorRadius < EPSILON
                ) ||
            (
                -value - minorRadius + upper - majorRadius + upperFrom <
                    EPSILON &&
                    upperFrom - majorRadius < EPSILON
                ) ||
            (
                -value - minorRadius + upper - majorRadius - upperTo <
                    EPSILON &&
                    upperTo - majorRadius < EPSILON
                ) ||
            value - minorRadius - lower < EPSILON ||
            -value - minorRadius + upper < EPSILON
    }

    private fun lineTouchesBoundary(
        value: Double,
        lower: Double,
        upper: Double,
        majorStep: Double,
        majorRadius: Double,
        minorRadius: Double,
    ): Boolean =
        pointTouchesBoundary(
            value = value,
            lower = lower,
            upper = upper,
            majorStep = majorStep,
            majorRadius = majorRadius,
            minorRadius = minorRadius,
        )

    private fun appendFace(
        target: GridPointBuffer,
        face: JsxGraphGridFace,
        x: Double,
        y: Double,
        radiusX: Double,
        radiusY: Double,
        boundingBox: DoubleArray,
        unitX: Double,
        unitY: Double,
    ) {
        when (face.face.lowercase()) {
            ".", "point" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x, y),
                        JsxGraphPoint2D(x, y),
                    ),
                )
            "o", "circle" -> {
                val q = 4.0 * tan(PI / 8.0) / 3.0
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x + radiusX, y),
                        JsxGraphPoint2D(x + radiusX, y + q * radiusY),
                        JsxGraphPoint2D(x + q * radiusX, y + radiusY),
                        JsxGraphPoint2D(x, y + radiusY),
                        JsxGraphPoint2D(x - q * radiusX, y + radiusY),
                        JsxGraphPoint2D(x - radiusX, y + q * radiusY),
                        JsxGraphPoint2D(x - radiusX, y),
                        JsxGraphPoint2D(x - radiusX, y - q * radiusY),
                        JsxGraphPoint2D(x - q * radiusX, y - radiusY),
                        JsxGraphPoint2D(x, y - radiusY),
                        JsxGraphPoint2D(x + q * radiusX, y - radiusY),
                        JsxGraphPoint2D(x + radiusX, y - q * radiusY),
                        JsxGraphPoint2D(x + radiusX, y),
                    ),
                )
            }
            "regpol", "regularpolygon" -> {
                if (!target.canAppendPath(face.polygonVertices.toLong() + 1L)) {
                    return
                }
                val polygon = (0..face.polygonVertices).map { index ->
                    val angle =
                        2.0 * PI * index / face.polygonVertices.toDouble()
                    JsxGraphPoint2D(
                        x - radiusX * sin(angle),
                        y - radiusY * cos(angle),
                    )
                }
                appendPath(target, polygon)
            }
            "[]", "square" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x - radiusX, y + radiusY),
                        JsxGraphPoint2D(x + radiusX, y + radiusY),
                        JsxGraphPoint2D(x + radiusX, y - radiusY),
                        JsxGraphPoint2D(x - radiusX, y - radiusY),
                        JsxGraphPoint2D(x - radiusX, y + radiusY),
                    ),
                )
            "<>", "diamond" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x, y + radiusY),
                        JsxGraphPoint2D(x + radiusX, y),
                        JsxGraphPoint2D(x, y - radiusY),
                        JsxGraphPoint2D(x - radiusX, y),
                        JsxGraphPoint2D(x, y + radiusY),
                    ),
                )
            "<<>>", "diamond2" -> {
                val radiusX2 = radiusX * sqrt(2.0)
                val radiusY2 = radiusY * sqrt(2.0)
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x, y + radiusY2),
                        JsxGraphPoint2D(x + radiusX2, y),
                        JsxGraphPoint2D(x, y - radiusY2),
                        JsxGraphPoint2D(x - radiusX2, y),
                        JsxGraphPoint2D(x, y + radiusY2),
                    ),
                )
            }
            "x", "cross" -> {
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x - radiusX, y + radiusY),
                        JsxGraphPoint2D(x + radiusX, y - radiusY),
                    ),
                )
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x - radiusX, y - radiusY),
                        JsxGraphPoint2D(x + radiusX, y + radiusY),
                    ),
                )
            }
            "+", "plus" -> {
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x - radiusX, y),
                        JsxGraphPoint2D(x + radiusX, y),
                    ),
                )
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x, y - radiusY),
                        JsxGraphPoint2D(x, y + radiusY),
                    ),
                )
            }
            "-", "minus" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x - radiusX, y),
                        JsxGraphPoint2D(x + radiusX, y),
                    ),
                )
            "|", "divide" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x, y - radiusY),
                        JsxGraphPoint2D(x, y + radiusY),
                    ),
                )
            "^", "a", "triangleup" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x - radiusX, y - radiusY),
                        JsxGraphPoint2D(x, y),
                        JsxGraphPoint2D(x + radiusX, y - radiusY),
                    ),
                )
            "v", "triangledown" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x - radiusX, y + radiusY),
                        JsxGraphPoint2D(x, y),
                        JsxGraphPoint2D(x + radiusX, y + radiusY),
                    ),
                )
            "<", "triangleleft" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x + radiusX, y + radiusY),
                        JsxGraphPoint2D(x, y),
                        JsxGraphPoint2D(x + radiusX, y - radiusY),
                    ),
                )
            ">", "triangleright" ->
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(x - radiusX, y + radiusY),
                        JsxGraphPoint2D(x, y),
                        JsxGraphPoint2D(x - radiusX, y - radiusY),
                    ),
                )
            "line" -> {
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(
                            x,
                            boundingBox[1] + face.margin / unitY,
                        ),
                        JsxGraphPoint2D(
                            x,
                            boundingBox[3] - face.margin / unitY,
                        ),
                    ),
                )
                appendPath(
                    target,
                    listOf(
                        JsxGraphPoint2D(
                            boundingBox[0] - face.margin / unitX,
                            y,
                        ),
                        JsxGraphPoint2D(
                            boundingBox[2] + face.margin / unitX,
                            y,
                        ),
                    ),
                )
            }
        }
    }

    private fun appendPath(
        target: GridPointBuffer,
        path: List<JsxGraphPoint2D>,
    ) {
        target.appendPath(path)
    }

    private fun distanceToStep(
        value: Double,
        step: Double,
    ): Double =
        abs(Mat.roundToStep(abs(value), step) - abs(value))

    private fun canDraw(
        boundingBox: DoubleArray,
        step: JsxGraphGridPair<Double>,
        inclusiveX: Boolean,
    ): Boolean {
        val finite =
            boundingBox.all(Double::isFinite) &&
                step.x.isFinite() &&
                step.y.isFinite() &&
                step.x > 0.0 &&
                step.y > 0.0
        if (!finite) {
            return false
        }
        val xFits =
            if (inclusiveX) {
                abs(boundingBox[2]) <= abs(step.x * MAX_LINES)
            } else {
                abs(boundingBox[2]) < abs(step.x * MAX_LINES)
            }
        return xFits &&
            abs(boundingBox[3]) < abs(step.y * MAX_LINES)
    }

    private fun autoStep(unit: Double): Double =
        10.0.pow(floor(log10(50.0 / unit)))

    private fun JsxGraphGridLength.resolveStep(
        relativeTo: Double,
        pixelsToUser: Double,
        auto: Double,
    ): Double =
        if (this === JsxGraphGridLength.Auto) {
            auto
        } else {
            resolve(relativeTo, pixelsToUser)
        }

    private fun JsxGraphGridLength.resolve(
        relativeTo: Double,
        pixelsToUser: Double,
    ): Double =
        when (this) {
            JsxGraphGridLength.Auto -> Double.NaN
            is JsxGraphGridLength.User -> value
            is JsxGraphGridLength.Percent -> value * relativeTo * 0.01
            is JsxGraphGridLength.Fraction -> value * relativeTo
            is JsxGraphGridLength.Pixels -> value * pixelsToUser
        }

    private fun JsxGraphGridMinorElements.resolve(
        parentValue: Double?,
    ): Double =
        when (this) {
            JsxGraphGridMinorElements.Auto -> parentValue ?: 3.0
            is JsxGraphGridMinorElements.Fixed -> value
        }

    private companion object {
        const val MAX_LINES = 5000
        const val EPSILON = 2.220446049250313e-16
        const val DEFAULT_MAXIMUM_POINT_COUNT = 10_000
    }

    private class GridPointBuffer(
        private val maximumPointCount: Int,
    ) {
        private val values = mutableListOf<JsxGraphPoint2D?>()
        private var requestedSize: Long? = null

        val exceeded: Boolean
            get() = requestedSize != null

        fun canAppendPath(pointCount: Long): Boolean {
            val requested = values.size.toLong() + pointCount + 1L
            if (requested > maximumPointCount) {
                requestedSize = requested
                return false
            }
            return true
        }

        fun appendPath(path: List<JsxGraphPoint2D>) {
            if (!canAppendPath(path.size.toLong())) {
                return
            }
            values += path
            values += null
        }

        fun result():
            GMResult<List<JsxGraphPoint2D?>, JsxGraphGridResolveError> =
            requestedSize?.let { requested ->
                GMResult.Err(
                    JsxGraphGridResolveError.PointLimitExceeded(
                        limit = maximumPointCount,
                        requestedSize = requested,
                    ),
                )
            } ?: GMResult.Ok(values)
    }
}
