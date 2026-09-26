/*
 * Kotlin translation support for JSXGraph.
 * Upstream: src/base/board.js -> create,
 * src/base/element.js -> visual properties,
 * src/base/point.js, src/base/line.js, src/base/circle.js,
 * src/base/curve.js, src/base/polygon.js, src/base/text.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.createBoxPlotGeometry
import com.swithun.jsxgraph.core.base.formatTicksLabel
import com.swithun.jsxgraph.core.math.Geometry
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Platform-independent output of the translated Board and element factories.
 *
 * Compose consumes this model without exposing the still-incomplete mutable
 * Board implementation as public API.
 */
data class JsxGraphScene(
    val boundingBox: JsxGraphBoundingBox,
    val axis: Boolean,
    val grid: Boolean,
    val keepAspectRatio: Boolean,
    val elements: List<JsxGraphSceneElement>,
)

data class JsxGraphBoundingBox(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
)

data class JsxGraphPoint2D(
    val x: Double,
    val y: Double,
)

data class JsxGraphArrowHead(
    val type: Int,
    val size: Double,
    val highlightSize: Double?,
)

data class JsxGraphColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
) {
    companion object {
        val Transparent = JsxGraphColor(0, 0, 0, 0)
    }
}

/**
 * Platform-independent fill-gradient parameters.
 *
 * JSXGraph 1.13.3: src/options.js -> gradient*;
 * src/renderer/svg.js -> updateGradient, updateGradientAngle,
 * updateGradientCircle.
 */
sealed interface JsxGraphFillGradient {
    val secondColor: JsxGraphColor
    val secondOpacity: Double
    val startOffset: Double
    val endOffset: Double

    data class Linear(
        override val secondColor: JsxGraphColor,
        override val secondOpacity: Double,
        override val startOffset: Double,
        override val endOffset: Double,
        val angle: Double,
    ) : JsxGraphFillGradient

    data class Radial(
        override val secondColor: JsxGraphColor,
        override val secondOpacity: Double,
        override val startOffset: Double,
        override val endOffset: Double,
        val centerX: Double,
        val centerY: Double,
        val radius: Double,
        val focalX: Double,
        val focalY: Double,
        val focalRadius: Double,
    ) : JsxGraphFillGradient
}

data class JsxGraphElementStyle(
    val visible: Boolean,
    val strokeColor: JsxGraphColor,
    val fillColor: JsxGraphColor,
    val strokeWidth: Double,
    val strokeOpacity: Double,
    val fillOpacity: Double,
    // JSXGraph 1.13.3: src/options.js -> Options.layer.
    val layer: Int = 0,
    // JSXGraph 1.13.3: src/renderer/abstract.js -> dashArray;
    // src/renderer/canvas.js -> _stroke.
    // Values are resolved CSS-pixel dash and gap lengths.
    val strokeDashPattern: List<Double> = emptyList(),
    val fillGradient: JsxGraphFillGradient? = null,
)

/**
 * Viewport-dependent geometry for a three-point Angle using `radius: auto`.
 */
data class JsxGraphAutoRadiusAngle(
    val first: JsxGraphPoint2D,
    val vertex: JsxGraphPoint2D,
    val third: JsxGraphPoint2D,
    val sign: Double,
) {
    // JSXGraph: src/element/sector.js -> autoRadius /
    // createAngle -> updateDataArraySector.
    fun resolvePoints(
        cssPixelsPerUnitX: Double,
    ): List<JsxGraphPoint2D?> {
        val firstDistance = hypot(
            first.x - vertex.x,
            first.y - vertex.y,
        )
        val radius = max(
            20.0 / cssPixelsPerUnitX,
            min(
                firstDistance * 0.3333,
                50.0 / cssPixelsPerUnitX,
            ),
        )
        val firstAtRadius = doubleArrayOf(
            1.0,
            vertex.x + (first.x - vertex.x) * radius / firstDistance,
            vertex.y + (first.y - vertex.y) * radius / firstDistance,
        )
        val thirdAtRadius = doubleArrayOf(
            1.0,
            vertex.x + (third.x - vertex.x) * radius / firstDistance,
            vertex.y + (third.y - vertex.y) * radius / firstDistance,
        )
        val arc = Geometry.bezierArc(
            first = firstAtRadius,
            center = doubleArrayOf(1.0, vertex.x, vertex.y),
            third = thirdAtRadius,
            withLegs = true,
            sign = sign,
        )
        return arc.xCoordinates.indices.map { index ->
            JsxGraphPoint2D(
                x = arc.xCoordinates[index],
                y = arc.yCoordinates[index],
            )
        }
    }
}

/**
 * Viewport-dependent BoxPlot data. Outlier sizes are specified in CSS pixels
 * by JSXGraph, so their user coordinates must be resolved by the renderer.
 */
data class JsxGraphBoxPlot(
    val quantiles: List<Double>,
    val outliers: List<Double>?,
    val axis: Double,
    val width: Double,
    val direction: String,
    val smallWidth: Double,
    val outlierFace: String,
    val outlierSize: Double,
) {
    // JSXGraph 1.13.3: src/base/curve.js ->
    // createBoxPlot.updateDataArray.
    fun resolvePoints(
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): List<JsxGraphPoint2D?> {
        val geometry = createBoxPlotGeometry(
            quantiles = quantiles.toDoubleArray(),
            outliers = outliers?.toDoubleArray(),
            axis = axis,
            width = width,
            direction = direction,
            smallWidth = smallWidth,
            outlierFace = outlierFace,
            outlierSize = outlierSize,
            unitX = cssPixelsPerUnitX,
            unitY = cssPixelsPerUnitY,
        )
        return geometry.xCoordinates.indices.map { index ->
            val x = geometry.xCoordinates[index]
            val y = geometry.yCoordinates[index]
            if (x.isFinite() && y.isFinite()) {
                JsxGraphPoint2D(x, y)
            } else {
                null
            }
        }
    }
}

data class JsxGraphVectorFieldVector(
    val start: JsxGraphPoint2D,
    val end: JsxGraphPoint2D,
)

/**
 * Viewport-dependent VectorField data. JSXGraph specifies arrowhead size in
 * CSS pixels, so the renderer resolves arrowhead coordinates at draw time.
 */
data class JsxGraphVectorField(
    val vectors: List<JsxGraphVectorFieldVector>,
    val arrowEnabled: Boolean,
    val arrowSize: Double,
    val arrowAngle: Double,
) {
    // JSXGraph 1.13.3:
    // src/element/vectorfield.js -> createVectorField.updateDataArray.
    fun resolvePoints(
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): List<JsxGraphPoint2D?> {
        val points = mutableListOf<JsxGraphPoint2D?>()
        val arrowLegX = arrowSize / cssPixelsPerUnitX
        val arrowLegY = arrowSize / cssPixelsPerUnitY
        for (vector in vectors) {
            points.add(vector.start.finiteOrNull())
            points.add(vector.end.finiteOrNull())
            points.add(null)

            val vectorX = vector.end.x - vector.start.x
            val vectorY = vector.end.y - vector.start.y
            if (arrowEnabled && abs(vectorX) + abs(vectorY) > 0.0) {
                val theta = atan2(vectorY, vectorX)
                val firstAngle = theta + arrowAngle
                val secondAngle = theta - arrowAngle
                points.add(
                    JsxGraphPoint2D(
                        x = vector.end.x - cos(firstAngle) * arrowLegX,
                        y = vector.end.y - sin(firstAngle) * arrowLegY,
                    ).finiteOrNull(),
                )
                points.add(vector.end.finiteOrNull())
                points.add(
                    JsxGraphPoint2D(
                        x = vector.end.x - cos(secondAngle) * arrowLegX,
                        y = vector.end.y - sin(secondAngle) * arrowLegY,
                    ).finiteOrNull(),
                )
                points.add(null)
            }
        }
        return points
    }

    private fun JsxGraphPoint2D.finiteOrNull(): JsxGraphPoint2D? =
        takeIf { x.isFinite() && y.isFinite() }
}

data class JsxGraphVectorField3DVector(
    val start: List<Double>,
    val vector: List<Double>,
    val scaledNorm: Double,
)

/**
 * Viewport-dependent VectorField3D data. JSXGraph specifies each arrowhead
 * axis in CSS pixels before projecting the generated 3D points.
 */
data class JsxGraphVectorField3D(
    val vectors: List<JsxGraphVectorField3DVector>,
    val arrowEnabled: Boolean,
    val arrowSize: Double,
    val arrowAngle: Double,
    val projection: JsxGraphProjection3D,
) {
    // JSXGraph 1.13.3:
    // src/3d/curve3d.js -> createVectorfield3D.updateDataArray.
    fun resolvePoints(
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): List<JsxGraphPoint2D?> {
        val points = mutableListOf<JsxGraphPoint2D?>()
        val arrowLegs = listOf(
            arrowSize / cssPixelsPerUnitX,
            arrowSize / cssPixelsPerUnitY,
            arrowSize / sqrt(
                cssPixelsPerUnitX * cssPixelsPerUnitY,
            ),
        )
        for (entry in vectors) {
            if (entry.start.size != 3 || entry.vector.size != 3) {
                continue
            }
            val end = List(3) { index ->
                entry.start[index] + entry.vector[index]
            }
            points += projection.project(entry.start)
            points += projection.project(end)
            points += null

            if (arrowEnabled) {
                val phi = atan2(entry.vector[1], entry.vector[0])
                val theta = kotlin.math.asin(
                    entry.vector[2] / entry.scaledNorm,
                )
                val theta1 = theta - arrowAngle
                val theta2 = theta + arrowAngle
                points += projection.project(
                    listOf(
                        end[0] -
                            arrowLegs[0] * cos(phi) * cos(theta1),
                        end[1] -
                            arrowLegs[1] * sin(phi) * cos(theta1),
                        end[2] - arrowLegs[2] * sin(theta2),
                    ),
                )
                points += projection.project(end)
                points += projection.project(
                    listOf(
                        end[0] -
                            arrowLegs[0] * cos(phi) * cos(theta2),
                        end[1] -
                            arrowLegs[1] * sin(phi) * cos(theta2),
                        end[2] - arrowLegs[2] * sin(theta1),
                    ),
                )
                points += null
            }
        }
        return points
    }
}

data class JsxGraphProjection3D(
    val matrix3D: List<List<Double>>,
    val central: Boolean,
    val viewPortTransform: List<List<Double>>?,
) {
    fun project(point: List<Double>): JsxGraphPoint2D? {
        if (point.size != 3 || matrix3D.size != 3) {
            return null
        }
        val homogeneous = listOf(1.0, point[0], point[1], point[2])
        val projected = matrix3D.map { row ->
            if (row.size != homogeneous.size) {
                return null
            }
            row.indices.sumOf { index -> row[index] * homogeneous[index] }
        }.toMutableList()
        if (central) {
            if (projected[0] == 0.0) {
                return null
            }
            projected[1] /= projected[0]
            projected[2] /= projected[0]
            projected[0] = 1.0
            val viewport = viewPortTransform ?: return null
            if (viewport.size != 3) {
                return null
            }
            val transformed = viewport.map { row ->
                if (row.size != 3) {
                    return null
                }
                row.indices.sumOf { index -> row[index] * projected[index] }
            }
            return JsxGraphPoint2D(
                x = transformed[1],
                y = transformed[2],
            ).finiteOrNull()
        }
        return JsxGraphPoint2D(
            x = projected[1],
            y = projected[2],
        ).finiteOrNull()
    }

    private fun JsxGraphPoint2D.finiteOrNull(): JsxGraphPoint2D? =
        takeIf { x.isFinite() && y.isFinite() }
}

/**
 * Viewport-dependent geometry for Ticks3D.
 *
 * JSXGraph specifies majorHeight in CSS pixels, so the final 3D endpoints
 * cannot be resolved until the renderer knows its pixel-to-user-unit scale.
 */
data class JsxGraphTicks3D(
    val tickBases3D: List<List<Double>>,
    val direction2: List<Double>,
    val tickEndings: List<Double>,
    val majorHeight: Double,
    val projection: JsxGraphProjection3D,
) {
    fun resolvePoints(
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): List<JsxGraphPoint2D?> {
        if (direction2.size != 3 || tickEndings.size != 2) {
            return emptyList()
        }
        val scale = sqrt(cssPixelsPerUnitX * cssPixelsPerUnitY)
        val height = majorHeight / scale
        val startOffset = height * -tickEndings[0]
        val endOffset = height * tickEndings[1]
        val points = mutableListOf<JsxGraphPoint2D?>()
        for (base in tickBases3D) {
            if (base.size != 3) {
                continue
            }
            points += projection.project(
                List(3) { index ->
                    base[index] + startOffset * direction2[index]
                },
            )
            points += projection.project(
                List(3) { index ->
                    base[index] + endOffset * direction2[index]
                },
            )
            points += null
        }
        return points
    }
}

data class JsxGraphTicks3DLabel(
    val tickBase3D: List<Double>,
    val direction2: List<Double>,
    val positiveEnding: Double,
    val majorHeight: Double,
    val projection: JsxGraphProjection3D,
) {
    fun resolvePosition(
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): JsxGraphPoint2D? {
        if (tickBase3D.size != 3 || direction2.size != 3) {
            return null
        }
        val scale = sqrt(cssPixelsPerUnitX * cssPixelsPerUnitY)
        val offset = majorHeight / scale * positiveEnding * 2.0
        return projection.project(
            List(3) { index ->
                tickBase3D[index] + offset * direction2[index]
            },
        )
    }
}

sealed interface JsxGraphTicksParent2D {
    data class Line(
        val point1: JsxGraphPoint2D,
        val point2: JsxGraphPoint2D,
        val straightFirst: Boolean,
        val straightLast: Boolean,
        val axis: Boolean,
    ) : JsxGraphTicksParent2D

    data class Curve(
        val locations: List<JsxGraphCurveTickLocation>,
    ) : JsxGraphTicksParent2D
}

data class JsxGraphCurveTickLocation(
    val base: JsxGraphPoint2D,
    val normal: JsxGraphPoint2D,
    val major: Boolean,
    val label: String?,
)

data class JsxGraphTicksLabelStyle(
    val visible: Boolean,
    val color: JsxGraphColor,
    val opacity: Double,
    val fontSize: Double,
    val anchorX: String,
    val anchorY: String,
    val offsetX: Double,
    val offsetY: Double,
)

data class JsxGraphTickPath(
    val points: List<JsxGraphPoint2D>,
    val major: Boolean,
)

data class JsxGraphTickLabel(
    val position: JsxGraphPoint2D,
    val content: String,
)

data class JsxGraphResolvedTicks(
    val paths: List<JsxGraphTickPath>,
    val labels: List<JsxGraphTickLabel>,
)

/**
 * Viewport-dependent geometry for two-dimensional JXG.Ticks.
 *
 * JSXGraph defines tick heights, label offsets, and automatic minimum spacing
 * in CSS pixels. Resolution therefore remains in the scene model until the
 * renderer knows the final viewport.
 */
data class JsxGraphTicks2D(
    val parent: JsxGraphTicksParent2D,
    val fixedTicks: List<Double>?,
    val fixedLabels: List<String?>,
    val anchor: String,
    val drawZero: Boolean,
    val insertTicks: Boolean,
    val minTicksDistance: Double,
    val minorHeight: Double,
    val majorHeight: Double,
    val tickEndings: List<Double>,
    val majorTickEndings: List<Double>,
    val ignoreInfiniteTickEndings: Boolean,
    val minorTicks: Int,
    val ticksPerLabel: Int?,
    val scale: Double,
    val scaleSymbol: String,
    val maxLabelLength: Int,
    val precision: Int,
    val digits: Int,
    val beautifulScientificTickLabels: Boolean,
    val useUnicodeMinus: Boolean,
    val face: String,
    val includeBoundaries: Boolean,
    val type: String,
    val ticksDistance: Double,
    val drawLabels: Boolean,
    val clip: Boolean,
    val labelStyle: JsxGraphTicksLabelStyle,
) {
    // JSXGraph 1.13.3: src/base/ticks.js ->
    // calculateTicksCoordinates / createTickPath / generateLabelData.
    fun resolve(
        visibleLeft: Double,
        visibleTop: Double,
        visibleRight: Double,
        visibleBottom: Double,
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): JsxGraphResolvedTicks {
        if (
            !visibleLeft.isFinite() ||
            !visibleTop.isFinite() ||
            !visibleRight.isFinite() ||
            !visibleBottom.isFinite() ||
            !cssPixelsPerUnitX.isFinite() ||
            !cssPixelsPerUnitY.isFinite() ||
            visibleLeft > visibleRight ||
            visibleBottom > visibleTop ||
            cssPixelsPerUnitX <= TICK_EPSILON ||
            cssPixelsPerUnitY <= TICK_EPSILON
        ) {
            return JsxGraphResolvedTicks(
                paths = emptyList(),
                labels = emptyList(),
            )
        }
        val locations = when (val parent = parent) {
            is JsxGraphTicksParent2D.Line ->
                lineLocations(
                    parent = parent,
                    visibleLeft = visibleLeft,
                    visibleTop = visibleTop,
                    visibleRight = visibleRight,
                    visibleBottom = visibleBottom,
                    cssPixelsPerUnitX = cssPixelsPerUnitX,
                    cssPixelsPerUnitY = cssPixelsPerUnitY,
                )
            is JsxGraphTicksParent2D.Curve -> parent.locations
        }
        val paths = mutableListOf<JsxGraphTickPath>()
        val labels = mutableListOf<JsxGraphTickLabel>()
        for (location in locations.take(MAXIMUM_TICK_COUNT)) {
            tickPath(
                location = location,
                visibleLeft = visibleLeft,
                visibleTop = visibleTop,
                visibleRight = visibleRight,
                visibleBottom = visibleBottom,
                cssPixelsPerUnitX = cssPixelsPerUnitX,
                cssPixelsPerUnitY = cssPixelsPerUnitY,
            )?.let(paths::add)
            val label = location.label
            if (label != null && labelStyle.visible) {
                val position = JsxGraphPoint2D(
                    x = location.base.x +
                        labelStyle.offsetX / cssPixelsPerUnitX,
                    y = location.base.y +
                        labelStyle.offsetY / cssPixelsPerUnitY,
                )
                if (
                    !clip ||
                    (
                        position.x in visibleLeft..visibleRight &&
                            position.y in visibleBottom..visibleTop
                        )
                ) {
                    labels += JsxGraphTickLabel(position, label)
                }
            }
        }
        return JsxGraphResolvedTicks(paths = paths, labels = labels)
    }

    private fun lineLocations(
        parent: JsxGraphTicksParent2D.Line,
        visibleLeft: Double,
        visibleTop: Double,
        visibleRight: Double,
        visibleBottom: Double,
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): List<JsxGraphCurveTickLocation> {
        val deltaX = parent.point2.x - parent.point1.x
        val deltaY = parent.point2.y - parent.point1.y
        val length = hypot(deltaX, deltaY)
        if (!length.isFinite() || length <= TICK_EPSILON) {
            return emptyList()
        }
        val directionX = deltaX / length
        val directionY = deltaY / length
        val anchorParameter = when {
            parent.axis ->
                (
                    -parent.point1.x * deltaX -
                        parent.point1.y * deltaY
                    ) / (length * length)
            anchor == "right" -> 1.0
            anchor == "middle" -> 0.5
            anchor.startsWith(NUMERIC_ANCHOR_PREFIX) ->
                anchor.removePrefix(NUMERIC_ANCHOR_PREFIX)
                    .toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val zero = JsxGraphPoint2D(
            x = parent.point1.x + deltaX * anchorParameter,
            y = parent.point1.y + deltaY * anchorParameter,
        )
        val parameterBounds = visibleLineParameterBounds(
            parent = parent,
            visibleLeft = visibleLeft,
            visibleTop = visibleTop,
            visibleRight = visibleRight,
            visibleBottom = visibleBottom,
        ) ?: return emptyList()
        var lower = (parameterBounds.first - anchorParameter) * length
        var upper = (parameterBounds.second - anchorParameter) * length
        if (!includeBoundaries) {
            val point1Inside =
                parent.point1.x in visibleLeft..visibleRight &&
                    parent.point1.y in visibleBottom..visibleTop
            val point2Inside =
                parent.point2.x in visibleLeft..visibleRight &&
                    parent.point2.y in visibleBottom..visibleTop
            if (!parent.straightFirst && point1Inside) {
                lower += TICK_EPSILON * 10.0
            }
            if (!parent.straightLast && point2Inside) {
                upper -= TICK_EPSILON * 10.0
            }
        }
        if (type == "polar") {
            upper = max(
                hypot(visibleLeft, visibleTop),
                hypot(visibleRight, visibleBottom),
            )
        }
        val positions = fixedTicks
            ?.mapIndexedNotNull { sourceIndex, position ->
                if (
                    position >= lower - TICK_EPSILON &&
                    position <= upper + TICK_EPSILON
                ) {
                    IndexedSceneTickPosition(sourceIndex, position)
                } else {
                    null
                }
            }
            ?: equidistantLinePositions(
                lower = lower,
                upper = upper,
                directionX = directionX,
                directionY = directionY,
                cssPixelsPerUnitX = cssPixelsPerUnitX,
                cssPixelsPerUnitY = cssPixelsPerUnitY,
            ).mapIndexed { sourceIndex, position ->
                IndexedSceneTickPosition(sourceIndex, position)
            }
        val minorDistance = lineMinorDistance(
            visibleDistance = upper - lower,
            directionX = directionX,
            directionY = directionY,
            cssPixelsPerUnitX = cssPixelsPerUnitX,
            cssPixelsPerUnitY = cssPixelsPerUnitY,
        )
        return positions.map { (sourceIndex, position) ->
            val major =
                fixedTicks != null ||
                    isMajor(position, minorDistance)
            val label = when {
                !drawLabels -> null
                fixedTicks != null ->
                    fixedLabels.getOrNull(sourceIndex)
                        ?: formatLabel(position)
                isLabelPosition(position, minorDistance) ->
                    formatLabel(position / scale)
                else -> null
            }
            JsxGraphCurveTickLocation(
                base = JsxGraphPoint2D(
                    x = zero.x + position * directionX,
                    y = zero.y + position * directionY,
                ),
                normal = JsxGraphPoint2D(
                    x = -directionY,
                    y = directionX,
                ),
                major = major,
                label = label,
            )
        }
    }

    private fun equidistantLinePositions(
        lower: Double,
        upper: Double,
        directionX: Double,
        directionY: Double,
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): List<Double> {
        val distance = lineMinorDistance(
            visibleDistance = upper - lower,
            directionX = directionX,
            directionY = directionY,
            cssPixelsPerUnitX = cssPixelsPerUnitX,
            cssPixelsPerUnitY = cssPixelsPerUnitY,
        )
        if (!distance.isFinite() || distance < TICK_EPSILON) {
            return emptyList()
        }
        val requested =
            ceil((upper - lower).coerceAtLeast(0.0) / distance).toLong() + 2L
        if (requested > MAXIMUM_TICK_COUNT) {
            return emptyList()
        }
        val positions = mutableListOf<Double>()
        var position = if (drawZero) 0.0 else distance
        if (position < lower) {
            position = floor((lower - TICK_EPSILON) / distance) * distance
        }
        while (
            position <= upper + TICK_EPSILON &&
            positions.size < MAXIMUM_TICK_COUNT
        ) {
            if (position >= lower - TICK_EPSILON) {
                positions += position
            }
            position += distance
        }
        position = -distance
        if (position > upper) {
            position = ceil((upper + TICK_EPSILON) / -distance) * -distance
        }
        while (
            position >= lower - TICK_EPSILON &&
            positions.size < MAXIMUM_TICK_COUNT
        ) {
            if (position <= upper + TICK_EPSILON) {
                positions += position
            }
            position -= distance
        }
        return positions
    }

    private fun lineMinorDistance(
        visibleDistance: Double,
        directionX: Double,
        directionY: Double,
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): Double {
        val majorDistance =
            if (!insertTicks) {
                ticksDistance
            } else {
                val maximumDistance = visibleDistance / 6.0 / scale
                val screenUnits = hypot(
                    directionX * cssPixelsPerUnitX,
                    directionY * cssPixelsPerUnitY,
                )
                val minimumDistance =
                    minTicksDistance / scale / screenUnits *
                        (minorTicks.toDouble() + 1.0)
                max(
                    nextNiceMinimum(minimumDistance),
                    previousNiceMaximum(maximumDistance),
                )
            }
        return majorDistance * scale / (minorTicks.toDouble() + 1.0)
    }

    private fun tickPath(
        location: JsxGraphCurveTickLocation,
        visibleLeft: Double,
        visibleTop: Double,
        visibleRight: Double,
        visibleBottom: Double,
        cssPixelsPerUnitX: Double,
        cssPixelsPerUnitY: Double,
    ): JsxGraphTickPath? {
        if (location.major && type == "polar") {
            val radius = hypot(location.base.x, location.base.y)
            val maximumRadius = max(
                hypot(visibleLeft, visibleTop),
                hypot(visibleRight, visibleBottom),
            )
            if (!radius.isFinite() || radius >= maximumRadius) {
                return null
            }
            return JsxGraphTickPath(
                points = (0..180).map { index ->
                    val angle = index * kotlin.math.PI / 90.0
                    JsxGraphPoint2D(
                        x = radius * cos(angle),
                        y = radius * sin(angle),
                    )
                },
                major = true,
            )
        }
        val height = if (location.major) majorHeight else minorHeight
        val rawEndings =
            if (location.major) majorTickEndings else tickEndings
        if (rawEndings.size != 2) {
            return null
        }
        val infinite = height < 0.0
        val endings =
            if (infinite && ignoreInfiniteTickEndings) {
                listOf(1.0, 1.0)
            } else {
                rawEndings.map { ending ->
                    if (ending > 0.0) 1.0 else 0.0
                }
            }
        val normalMetricX = location.normal.x * cssPixelsPerUnitX
        val normalMetricY = location.normal.y * cssPixelsPerUnitY
        val normalLength = hypot(normalMetricX, normalMetricY)
        if (!normalLength.isFinite() || normalLength <= TICK_EPSILON) {
            return null
        }
        val unitMetricX = normalMetricX / normalLength
        val unitMetricY = normalMetricY / normalLength
        if (infinite) {
            val directionX = unitMetricX / cssPixelsPerUnitX
            val directionY = unitMetricY / cssPixelsPerUnitY
            val interval = lineIntervalInBox(
                origin = location.base,
                directionX = directionX,
                directionY = directionY,
                left = visibleLeft,
                top = visibleTop,
                right = visibleRight,
                bottom = visibleBottom,
            ) ?: return null
            val first = if (endings[0] > 0.0) interval.second else 0.0
            val second = if (endings[1] > 0.0) interval.first else 0.0
            return JsxGraphTickPath(
                points = listOf(
                    JsxGraphPoint2D(
                        x = location.base.x + directionX * first,
                        y = location.base.y + directionY * first,
                    ),
                    JsxGraphPoint2D(
                        x = location.base.x + directionX * second,
                        y = location.base.y + directionY * second,
                    ),
                ),
                major = location.major,
            )
        }

        val halfHeight = height * 0.5
        val angle = when (face) {
            ">" -> kotlin.math.PI * 0.25
            "<" -> -kotlin.math.PI * 0.25
            else -> 0.0
        }
        val firstX =
            cos(angle) * unitMetricX -
                sin(angle) * unitMetricY
        val firstY =
            sin(angle) * unitMetricX +
                cos(angle) * unitMetricY
        val secondAngle = -angle
        val secondX =
            cos(secondAngle) * unitMetricX -
                sin(secondAngle) * unitMetricY
        val secondY =
            sin(secondAngle) * unitMetricX +
                cos(secondAngle) * unitMetricY
        val points = listOf(
            JsxGraphPoint2D(
                x = location.base.x +
                    firstX * halfHeight * endings[0] /
                    cssPixelsPerUnitX,
                y = location.base.y +
                    firstY * halfHeight * endings[0] /
                    cssPixelsPerUnitY,
            ),
            location.base,
            JsxGraphPoint2D(
                x = location.base.x -
                    secondX * halfHeight * endings[1] /
                    cssPixelsPerUnitX,
                y = location.base.y -
                    secondY * halfHeight * endings[1] /
                    cssPixelsPerUnitY,
            ),
        )
        if (
            clip &&
            points.take(2).none {
                it.x in visibleLeft..visibleRight &&
                    it.y in visibleBottom..visibleTop
            }
        ) {
            return null
        }
        return JsxGraphTickPath(points, location.major)
    }

    private fun visibleLineParameterBounds(
        parent: JsxGraphTicksParent2D.Line,
        visibleLeft: Double,
        visibleTop: Double,
        visibleRight: Double,
        visibleBottom: Double,
    ): Pair<Double, Double>? {
        val directionX = parent.point2.x - parent.point1.x
        val directionY = parent.point2.y - parent.point1.y
        val interval = lineIntervalInBox(
            origin = parent.point1,
            directionX = directionX,
            directionY = directionY,
            left = visibleLeft,
            top = visibleTop,
            right = visibleRight,
            bottom = visibleBottom,
        ) ?: return null
        val lower = max(
            interval.first,
            if (parent.straightFirst) {
                Double.NEGATIVE_INFINITY
            } else {
                0.0
            },
        )
        val upper = min(
            interval.second,
            if (parent.straightLast) {
                Double.POSITIVE_INFINITY
            } else {
                1.0
            },
        )
        return if (lower <= upper) lower to upper else null
    }

    private fun lineIntervalInBox(
        origin: JsxGraphPoint2D,
        directionX: Double,
        directionY: Double,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
    ): Pair<Double, Double>? {
        var lower = Double.NEGATIVE_INFINITY
        var upper = Double.POSITIVE_INFINITY

        fun constrain(
            coordinate: Double,
            direction: Double,
            minimum: Double,
            maximum: Double,
        ): Boolean {
            if (abs(direction) <= TICK_EPSILON) {
                return coordinate in minimum..maximum
            }
            val first = (minimum - coordinate) / direction
            val second = (maximum - coordinate) / direction
            lower = max(lower, min(first, second))
            upper = min(upper, max(first, second))
            return lower <= upper
        }

        if (!constrain(origin.x, directionX, left, right)) {
            return null
        }
        if (!constrain(origin.y, directionY, bottom, top)) {
            return null
        }
        return lower to upper
    }

    private fun isMajor(
        position: Double,
        minorDistance: Double,
    ): Boolean =
        round(position / minorDistance).toLong() %
            (minorTicks.toLong() + 1L) == 0L

    private fun isLabelPosition(
        position: Double,
        minorDistance: Double,
    ): Boolean {
        val interval = ticksPerLabel?.toLong()
            ?: (minorTicks.toLong() + 1L)
        return round(position / minorDistance).toLong() %
            interval == 0L
    }

    private fun formatLabel(value: Double): String =
        formatTicksLabel(
            value = value,
            maxLabelLength = maxLabelLength,
            precision = precision,
            digits = digits,
            scaleSymbol = scaleSymbol,
            beautifulScientificTickLabels =
                beautifulScientificTickLabels,
            useUnicodeMinus = useUnicodeMinus,
        )

    private fun nextNiceMinimum(value: Double): Double {
        var delta = 10.0.pow(floor(log10(value)))
        if (2.0 * delta >= value) {
            delta *= 2.0
        } else if (5.0 * delta >= value) {
            delta *= 5.0
        }
        return delta
    }

    private fun previousNiceMaximum(value: Double): Double {
        var delta = 10.0.pow(floor(log10(value)))
        if (5.0 * delta < value) {
            delta *= 5.0
        } else if (2.0 * delta < value) {
            delta *= 2.0
        }
        return delta
    }

    companion object {
        const val NUMERIC_ANCHOR_PREFIX: String = "fraction:"
        private const val MAXIMUM_TICK_COUNT = 2048
        private const val TICK_EPSILON = 2.220446049250313e-16
    }
}

private data class IndexedSceneTickPosition(
    val sourceIndex: Int,
    val position: Double,
)

sealed interface JsxGraphSceneElement {
    val id: String
    val name: String
    val style: JsxGraphElementStyle

    data class Point(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val coordinates: JsxGraphPoint2D,
        val size: Double,
        val face: String,
        val draggable: Boolean = false,
        val isReal: Boolean = true,
    ) : JsxGraphSceneElement

    data class Line(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val point1: JsxGraphPoint2D,
        val point2: JsxGraphPoint2D,
        val straightFirst: Boolean,
        val straightLast: Boolean,
        val firstArrow: JsxGraphArrowHead?,
        val lastArrow: JsxGraphArrowHead?,
    ) : JsxGraphSceneElement

    data class Ticks(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val definition: JsxGraphTicks2D,
    ) : JsxGraphSceneElement

    data class Circle(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val center: JsxGraphPoint2D,
        val radius: Double,
    ) : JsxGraphSceneElement

    data class Curve(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val points: List<JsxGraphPoint2D?>,
        val bezierDegree: Int,
        val lineCap: String,
        val autoRadiusAngle: JsxGraphAutoRadiusAngle? = null,
        val boxPlot: JsxGraphBoxPlot? = null,
        val vectorField: JsxGraphVectorField? = null,
        val vectorField3D: JsxGraphVectorField3D? = null,
        val ticks3D: JsxGraphTicks3D? = null,
    ) : JsxGraphSceneElement {
        fun resolvePoints(
            cssPixelsPerUnitX: Double,
            cssPixelsPerUnitY: Double = cssPixelsPerUnitX,
        ): List<JsxGraphPoint2D?> =
            autoRadiusAngle?.resolvePoints(cssPixelsPerUnitX)
                ?: boxPlot?.resolvePoints(
                    cssPixelsPerUnitX = cssPixelsPerUnitX,
                    cssPixelsPerUnitY = cssPixelsPerUnitY,
                )
                ?: vectorField?.resolvePoints(
                    cssPixelsPerUnitX = cssPixelsPerUnitX,
                    cssPixelsPerUnitY = cssPixelsPerUnitY,
                )
                ?: vectorField3D?.resolvePoints(
                    cssPixelsPerUnitX = cssPixelsPerUnitX,
                    cssPixelsPerUnitY = cssPixelsPerUnitY,
                )
                ?: ticks3D?.resolvePoints(
                    cssPixelsPerUnitX = cssPixelsPerUnitX,
                    cssPixelsPerUnitY = cssPixelsPerUnitY,
                )
                ?: points
    }

    data class Polygon(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val vertices: List<JsxGraphPoint2D>,
        val implicitVertices: List<Point>,
        val borderStyle: JsxGraphElementStyle,
        val withLines: Boolean,
        val isClosed: Boolean = true,
    ) : JsxGraphSceneElement

    data class Text(
        override val id: String,
        override val name: String,
        override val style: JsxGraphElementStyle,
        val coordinates: JsxGraphPoint2D,
        val content: String,
        val fontSize: Double,
        val anchorX: String,
        val anchorY: String,
        val ticks3DLabel: JsxGraphTicks3DLabel? = null,
    ) : JsxGraphSceneElement
}
