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
import com.swithun.jsxgraph.core.math.Geometry
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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
    ) : JsxGraphSceneElement
}
