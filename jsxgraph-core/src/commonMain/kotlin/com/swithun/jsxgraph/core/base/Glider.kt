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
import com.swithun.jsxgraph.core.math.ContinuousCurve2D
import com.swithun.jsxgraph.core.math.ContinuousCurveType
import com.swithun.jsxgraph.core.math.DiscreteCurve2D
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.GeometryError
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.ParametricCurve2D
import com.swithun.jsxgraph.core.math.ProjectionResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.utils.JsMath
import kotlin.math.abs

internal sealed interface GliderError {
    data class UnsupportedSlideObject(
        val elementType: String,
    ) : GliderError

    data object SlideObjectBoardMismatch : GliderError

    data class CurveEvaluation(
        val error: CurveError,
    ) : GliderError

    data class CurveProjection(
        val error: GeometryError,
    ) : GliderError

    data class InvalidCoordinateCount(
        val count: Int,
    ) : GliderError

    data class Registration(
        val error: BoardError,
    ) : GliderError
}

/**
 * The Line/Segment and ordinary Curve-backed subset of JXG.Glider.
 *
 * Circle, Conic, Grid, Polygon, Turtle, Point, transformed-slide, attractor,
 * and animation branches remain explicit unsupported creator inputs.
 */
internal open class Glider internal constructor(
    board: Board,
    coordinates: DoubleArray,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
    internal val slideElement: GeometryElement,
    coordinateConstraint: JessieCodeCoordinateFunction? = null,
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
    coordinateFunctions = listOfNotNull(coordinateConstraint),
) {
    internal var sliderMinimum: Double? = null
    internal var sliderMaximum: Double? = null
    internal var evaluationError: GliderError? = null
        private set

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
        val result = if (coordinateFunctions.isNotEmpty()) {
            updateGlider()
        } else if (fromParent) {
            updateGliderFromParent()
        } else {
            updateGlider()
        }
        evaluationError = when (result) {
            is GMResult.Ok -> null
            is GMResult.Err -> result.error
        }
        updateTransform(fromParent)
        return this
    }

    // JSXGraph: src/base/coordselement.js -> updateGlider,
    // Line and untransformed non-Arc/Sector Curve branches.
    internal fun updateGlider(): GMResult<Glider, GliderError> {
        updateConstraint()
        needsUpdateFromParent = false
        return when (val slide = slideElement) {
            is Line -> updateLineGlider(slide)
            is Curve -> updateCurveGlider(slide)
            else -> GMResult.Err(
                GliderError.UnsupportedSlideObject(
                    slide.elType.ifEmpty { "element" },
                ),
            )
        }
    }

    private fun updateLineGlider(line: Line): GMResult<Glider, GliderError> {
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
        return GMResult.Ok(this)
    }

    private fun updateCurveGlider(
        curve: Curve,
    ): GMResult<Glider, GliderError> {
        return when (
            val result = projectToCurve(
                curve = curve,
                point = coords.usrCoords,
                initialParameter =
                    position ?: if (
                        curve.curveType == FUNCTION_GRAPH_CURVE_TYPE
                    ) {
                        X()
                    } else {
                        0.0
                    },
            )
        ) {
            is GMResult.Ok -> {
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = result.value.point,
                    doRound = false,
                )
                position = result.value.parameter
                GMResult.Ok(this)
            }
            is GMResult.Err -> result
        }
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
    // finite Line and untransformed non-Arc/Sector Curve branches.
    internal fun updateGliderFromParent(): GMResult<Glider, GliderError> {
        if (!needsUpdateFromParent) {
            needsUpdateFromParent = true
            return GMResult.Ok(this)
        }
        return when (val slide = slideElement) {
            is Line -> updateLineGliderFromParent(slide)
            is Curve -> updateCurveGliderFromParent(slide)
            else -> GMResult.Err(
                GliderError.UnsupportedSlideObject(
                    slide.elType.ifEmpty { "element" },
                ),
            )
        }
    }

    private fun updateLineGliderFromParent(
        line: Line,
    ): GMResult<Glider, GliderError> {
        val first = line.point1.coords.usrCoords
        val second = line.point2.coords.usrCoords
        if (
            first.contentEquals(ZERO_COORDINATES) ||
            second.contentEquals(ZERO_COORDINATES)
        ) {
            coords.setCoordinates(Const.COORDS_BY_USER, ZERO_COORDINATES)
            return GMResult.Ok(this)
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
        return GMResult.Ok(this)
    }

    private fun updateCurveGliderFromParent(
        curve: Curve,
    ): GMResult<Glider, GliderError> {
        updateConstraint()
        val relativePosition = position ?: 0.0
        val sourceCoordinates = doubleArrayOf(
            1.0,
            curve.X(relativePosition),
            curve.Y(relativePosition),
        )
        return when (
            val result = projectToCurve(
                curve = curve,
                point = sourceCoordinates,
                initialParameter = relativePosition,
            )
        ) {
            is GMResult.Ok -> {
                coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = result.value.point,
                    doRound = false,
                )
                GMResult.Ok(this)
            }
            is GMResult.Err -> result
        }
    }

    private fun projectToCurve(
        curve: Curve,
        point: DoubleArray,
        initialParameter: Double,
    ): GMResult<ProjectionResult, GliderError> {
        curve.evaluationError?.let { error ->
            return GMResult.Err(GliderError.CurveEvaluation(error))
        }
        val result = if (curve.curveType == DATA_CURVE_TYPE) {
            Geometry.projectCoordsToCurve(
                point = point,
                curve = DiscreteCurve2D(
                    points = curve.points.map { it.usrCoords.copyOf() },
                    bezierDegree = curve.bezierDegree,
                ),
            )
        } else {
            Geometry.projectCoordsToCurve(
                horizontal = point.getOrElse(1) { Double.NaN },
                vertical = point.getOrElse(2) { Double.NaN },
                initialParameter = initialParameter,
                continuousCurve = ContinuousCurve2D(
                    curve = ParametricCurve2D(
                        x = curve::X,
                        y = curve::Y,
                    ),
                    minimumParameter = curve.minX(),
                    maximumParameter = curve.maxX(),
                    type =
                        if (
                            curve.curveType ==
                            FUNCTION_GRAPH_CURVE_TYPE
                        ) {
                            ContinuousCurveType.FUNCTION_GRAPH
                        } else {
                            ContinuousCurveType.PARAMETER
                        },
                ),
            )
        }
        return when (result) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                GliderError.CurveProjection(result.error),
            )
        }
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
        private const val DATA_CURVE_TYPE = "plot"
        private const val FUNCTION_GRAPH_CURVE_TYPE = "functiongraph"
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
            coordinateConstraint: JessieCodeCoordinateFunction? = null,
        ): GMResult<Glider, GliderError> {
            if (coordinates.size !in 2..3) {
                return GMResult.Err(
                    GliderError.InvalidCoordinateCount(coordinates.size),
                )
            }
            val supportedSlideObject = when (slideObject) {
                is Line -> slideObject
                is Curve -> {
                    if (
                        slideObject.type != Const.OBJECT_TYPE_CURVE ||
                        slideObject.transformations.isNotEmpty()
                    ) {
                        return GMResult.Err(
                            GliderError.UnsupportedSlideObject(
                                slideObject.elType.ifEmpty { "curve" },
                            ),
                        )
                    }
                    slideObject
                }
                else -> return GMResult.Err(
                    GliderError.UnsupportedSlideObject(
                        slideObject.elType.ifEmpty { "element" },
                    ),
                )
            }
            if (
                supportedSlideObject.board !== board ||
                board.elementById(supportedSlideObject.id) !==
                supportedSlideObject
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
                slideElement = supportedSlideObject,
                coordinateConstraint = coordinateConstraint,
            )
            return register(glider)
        }

        internal fun <T : Glider> register(
            glider: T,
        ): GMResult<T, GliderError> {
            when (val result = glider.updateGlider()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(result.error)
            }
            glider.needsUpdateFromParent = true
            when (val result = glider.updateGliderFromParent()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(result.error)
            }
            return when (
                val result = glider.board.setId(
                    glider,
                    GLIDER_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> {
                    glider.baseElement = glider
                    glider.slideObject = glider.slideElement
                    glider.slideObjects += glider.slideElement
                    glider.addParents(listOf(glider.slideElement))
                    glider.addParentsFromJCFunctions(
                        glider.coordinateFunctions,
                    )
                    glider.slideElement.addChild(glider)
                    glider.isDraggable =
                        glider.coordinateFunctions.isEmpty()
                    GMResult.Ok(glider)
                }
                is GMResult.Err -> GMResult.Err(
                    GliderError.Registration(result.error),
                )
            }
        }
    }
}
