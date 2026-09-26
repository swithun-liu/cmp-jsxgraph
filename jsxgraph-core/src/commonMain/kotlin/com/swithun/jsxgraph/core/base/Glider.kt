/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/point.js -> createGlider;
 * src/base/coordselement.js -> updateGlider, findClosestSnapValue,
 * updateGliderFromParent, setGliderPosition, makeGlider
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.JsMath
import kotlin.math.abs

internal sealed interface GliderError {
    data class UnsupportedSlideObject(
        val elementType: String,
    ) : GliderError

    data object SlideObjectBoardMismatch : GliderError

    data class InvalidCoordinateCount(
        val count: Int,
    ) : GliderError

    data class Registration(
        val error: BoardError,
    ) : GliderError
}

/**
 * The line/segment-backed subset of JXG.Glider.
 *
 * Circle, Curve, Polygon, Turtle, Point, transformed-slide, attractor, and
 * animation branches remain explicit unsupported creator inputs.
 */
internal open class Glider internal constructor(
    board: Board,
    coordinates: DoubleArray,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
    internal val line: Line,
    internal var snapWidth: Double = -1.0,
    internal var snapValues: DoubleArray = doubleArrayOf(),
    internal var snapValueDistance: Double = 0.0,
) : Point(
    board = board,
    coordinates = coordinates,
    id = id,
    name = name,
    needsRegularUpdate = needsRegularUpdate,
    fixed = fixed,
) {
    internal var sliderMinimum: Double? = null
    internal var sliderMaximum: Double? = null

    init {
        type = Const.OBJECT_TYPE_GLIDER
        elType = GLIDER_ELEMENT_TYPE
    }

    // JSXGraph: src/base/point.js -> update;
    // src/base/coordselement.js -> updateCoords.
    override fun update(fromParent: Boolean): Glider {
        if (!needsUpdate) {
            return this
        }
        updateConstraint()
        if (fromParent) {
            updateGliderFromParent()
        } else {
            updateGlider()
        }
        updateTransform(fromParent)
        return this
    }

    // JSXGraph: src/base/coordselement.js -> updateGlider, Line branch.
    internal fun updateGlider(): Glider {
        needsUpdateFromParent = false
        val first = line.point1.coords.usrCoords
        val second = line.point2.coords.usrCoords
        val distance = line.point1.coords.distance(
            Const.COORDS_BY_USER,
            line.point2.coords,
        )
        var newCoordinates: DoubleArray
        var newPosition: Double

        if (distance < Mat.eps) {
            newCoordinates = first
            newPosition = 0.0
        } else {
            val projection = Geometry.projectCoordsToSegment(
                point = coords.usrCoords,
                first = first,
                second = second,
            )
            newCoordinates = projection.point
            newPosition = projection.parameter

            findClosestSnapValue(newPosition)?.let { index ->
                val minimum = sliderMinimum
                val maximum = sliderMaximum
                if (minimum != null && maximum != null) {
                    newPosition =
                        (snapValues[index] - minimum) / (maximum - minimum)
                }
            } ?: run {
                val minimum = sliderMinimum
                val maximum = sliderMaximum
                if (
                    snapWidth > 0.0 &&
                    minimum != null &&
                    maximum != null &&
                    abs(maximum - minimum) >= Mat.eps
                ) {
                    newPosition = newPosition.coerceIn(0.0, 1.0)
                    val value =
                        JsMath.round(
                            newPosition * (maximum - minimum) / snapWidth,
                        ) * snapWidth + minimum
                    newPosition = (value - minimum) / (maximum - minimum)
                }
            }

            if (
                !line.straightFirst &&
                abs(first[0]) > Mat.eps &&
                newPosition < 0.0
            ) {
                newCoordinates = first
                newPosition = 0.0
            }
            if (
                !line.straightLast &&
                abs(second[0]) > Mat.eps &&
                newPosition > 1.0
            ) {
                newCoordinates = second
                newPosition = 1.0
            }
        }

        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = newCoordinates,
            doRound = distance < Mat.eps ||
                newCoordinates === first ||
                newCoordinates === second,
        )
        position = newPosition
        return this
    }

    // JSXGraph: src/base/coordselement.js -> findClosestSnapValue.
    internal fun findClosestSnapValue(relativePosition: Double): Int? {
        val minimum = sliderMinimum ?: return null
        val maximum = sliderMaximum ?: return null
        if (
            abs(maximum - minimum) < Mat.eps ||
            snapValueDistance <= 0.0
        ) {
            return null
        }
        var distanceLimit = snapValueDistance
        var closestIndex: Int? = null
        for ((index, snapValue) in snapValues.withIndex()) {
            val distance = abs(
                relativePosition * (maximum - minimum) +
                    minimum -
                    snapValue,
            )
            if (distance < distanceLimit) {
                distanceLimit = distance
                closestIndex = index
            }
        }
        return closestIndex
    }

    // JSXGraph: src/base/coordselement.js -> updateGliderFromParent,
    // finite Line branch.
    internal fun updateGliderFromParent(): Glider {
        if (!needsUpdateFromParent) {
            needsUpdateFromParent = true
            return this
        }
        val first = line.point1.coords.usrCoords
        val second = line.point2.coords.usrCoords
        if (
            first.contentEquals(ZERO_COORDINATES) ||
            second.contentEquals(ZERO_COORDINATES)
        ) {
            coords.setCoordinates(
                Const.COORDS_BY_USER,
                ZERO_COORDINATES,
            )
            return this
        }
        val relativePosition = position ?: 0.0
        coords.setCoordinates(
            Const.COORDS_BY_USER,
            doubleArrayOf(
                first[0] + relativePosition * (second[0] - first[0]),
                first[1] + relativePosition * (second[1] - first[1]),
                first[2] + relativePosition * (second[2] - first[2]),
            ),
        )
        return this
    }

    // JSXGraph: src/base/coordselement.js -> setGliderPosition.
    internal fun setGliderPosition(value: Double): Glider {
        position = value
        board.update()
        return this
    }

    internal companion object {
        private const val GLIDER_ID_PREFIX = "P"
        private const val GLIDER_ELEMENT_TYPE = "glider"
        private val ZERO_COORDINATES = doubleArrayOf(0.0, 0.0, 0.0)

        // JSXGraph: src/base/point.js -> createGlider;
        // src/base/coordselement.js -> makeGlider.
        fun create(
            board: Board,
            coordinates: DoubleArray,
            slideObject: GeometryElement,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
        ): GMResult<Glider, GliderError> {
            if (coordinates.size !in 2..3) {
                return GMResult.Err(
                    GliderError.InvalidCoordinateCount(coordinates.size),
                )
            }
            val line = slideObject as? Line
                ?: return GMResult.Err(
                    GliderError.UnsupportedSlideObject(
                        slideObject.elType.ifEmpty { "element" },
                    ),
                )
            if (
                line.board !== board ||
                board.elementById(line.id) !== line
            ) {
                return GMResult.Err(
                    GliderError.SlideObjectBoardMismatch,
                )
            }
            val glider = Glider(
                board = board,
                coordinates = coordinates,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
                line = line,
            )
            return register(glider)
        }

        internal fun <T : Glider> register(
            glider: T,
        ): GMResult<T, GliderError> =
            when (
                val result = glider.board.setId(
                    glider,
                    GLIDER_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> {
                    glider.baseElement = glider
                    glider.slideObject = glider.line
                    glider.slideObjects += glider.line
                    glider.addParents(listOf(glider.line))
                    glider.line.addChild(glider)
                    glider.isDraggable = true
                    glider.updateGlider()
                    glider.needsUpdateFromParent = true
                    glider.updateGliderFromParent()
                    GMResult.Ok(glider)
                }
                is GMResult.Err -> GMResult.Err(
                    GliderError.Registration(result.error),
                )
            }
    }
}
