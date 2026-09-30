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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

    data class NonInvertibleCurveTransformation(
        val curveId: String,
        val chainIndex: Int,
    ) : GliderError

    data class InvalidCoordinateCount(
        val count: Int,
    ) : GliderError

    data class Registration(
        val error: BoardError,
    ) : GliderError
}

/**
 * The Point, Line/Segment/Polygon-border, Circle, Curve-class, Arc, and
 * Sector-backed subset of JXG.Glider.
 *
 * Turtle, attractor, and animation branches remain explicit unsupported
 * creator inputs.
 */
internal open class Glider internal constructor(
    board: Board,
    coordinates: DoubleArray,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    fixed: Boolean,
    internal var slideElement: GeometryElement,
    private val polygonHost: Polygon? = null,
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
        onPolygon = polygonHost != null
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
    // Point, Circle, Line, and Curve/Arc/Sector branches.
    internal fun updateGlider(): GMResult<Glider, GliderError> {
        updateConstraint()
        needsUpdateFromParent = false
        return when (val slide = slideElement) {
            is Circle -> updateCircleGlider(slide)
            is Line -> updateLineGlider(slide)
            is Arc -> updateArcSectorGlider(slide.arcSectorSlideGeometry())
            is Sector ->
                if (slide.type == Const.OBJECT_TYPE_SECTOR) {
                    updateArcSectorGlider(slide.arcSectorSlideGeometry())
                } else {
                    unsupportedSlideObject(slide)
                }
            is Curve -> updateCurveGlider(slide)
            is Point -> updatePointGlider(slide)
            else -> unsupportedSlideObject(slide)
        }
    }

    // JSXGraph: src/base/coordselement.js -> updateGlider,
    // OBJECT_CLASS_CIRCLE branch.
    private fun updateCircleGlider(
        circle: Circle,
    ): GMResult<Glider, GliderError> {
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = Geometry.projectPointToCircle(
                point = coords.usrCoords,
                center = circle.center.coords.usrCoords,
                radius = circle.Radius(),
            ),
            doRound = false,
        )
        position = Geometry.rad(
            doubleArrayOf(
                circle.center.X() + 1.0,
                circle.center.Y(),
            ),
            circle.center.Coords(),
            Coords(),
        ) / (2.0 * PI)
        return GMResult.Ok(this)
    }

    private fun updateLineGlider(line: Line): GMResult<Glider, GliderError> {
        var activeLine = line
        polygonHost?.let { polygon ->
            val borderIndex = polygon.borders.indexOf(activeLine)
            if (borderIndex >= 0 && polygon.borders.isNotEmpty()) {
                val projection = Geometry.projectCoordsToSegment(
                    point = coords.usrCoords,
                    first = activeLine.point1.coords.usrCoords,
                    second = activeLine.point2.coords.usrCoords,
                )
                activeLine = when {
                    projection.parameter < 0.0 ->
                        polygon.borders[
                            (borderIndex - 1 + polygon.borders.size) %
                                polygon.borders.size
                        ]
                    projection.parameter > 1.0 ->
                        polygon.borders[
                            (borderIndex + 1) % polygon.borders.size
                        ]
                    else -> activeLine
                }
                if (activeLine !== slideElement) {
                    slideElement = activeLine
                    slideObject = activeLine
                }
            }
        }

        val first = activeLine.point1.coords.usrCoords
        val second = activeLine.point2.coords.usrCoords
        val distance = activeLine.point1.coords.distance(
            Const.COORDS_BY_USER,
            activeLine.point2.coords,
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
                !activeLine.straightFirst &&
                abs(first[0]) > Mat.eps &&
                newPosition < 0.0
            ) {
                newCoordinates = first
                newPosition = 0.0
            }
            if (
                !activeLine.straightLast &&
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
        val projection =
            if (curve.transformations.isNotEmpty()) {
                projectToTransformedCurve(
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
            } else {
                projectToCurve(
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
            }
        return when (
            val result = projection
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

    // JSXGraph: src/base/coordselement.js -> updateGlider,
    // OBJECT_TYPE_ARC / OBJECT_TYPE_SECTOR branch.
    private fun updateArcSectorGlider(
        slide: ArcSectorSlideGeometry,
    ): GMResult<Glider, GliderError> {
        val newCoordinates = Geometry.projectPointToCircle(
            point = coords.usrCoords,
            center = slide.center.coords.usrCoords,
            radius = slide.radius,
        )
        val angle = Geometry.rad(
            slide.radiusPoint.Coords(),
            slide.center.Coords(),
            Coords(),
        )
        val interval = slide.selectedAngleInterval()
        var newPosition = angle
        if (angle < interval.alpha || angle > interval.beta) {
            newPosition = interval.beta
            if (
                (
                    angle < interval.alpha &&
                        angle > interval.alpha * 0.5
                    ) ||
                (
                    angle > interval.beta &&
                        angle > interval.beta * 0.5 + PI
                    )
            ) {
                newPosition = interval.alpha
            }

            // Upstream calls updateGliderFromParent here before replacing
            // coords/position below. Its lasting effect is that the next
            // parent-driven update is not skipped.
            needsUpdateFromParent = true
        }
        if (abs(interval.delta) > Mat.eps) {
            newPosition /= interval.delta
        }

        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = newCoordinates,
            doRound = false,
        )
        position = newPosition
        return GMResult.Ok(this)
    }

    // JSXGraph: src/base/coordselement.js -> updateGlider,
    // Type.isPoint branch.
    private fun updatePointGlider(
        point: Point,
    ): GMResult<Glider, GliderError> {
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = point.coords.usrCoords,
            doRound = false,
        )
        return GMResult.Ok(this)
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
    // Point, Circle, finite Line, and Curve/Arc/Sector branches.
    internal fun updateGliderFromParent(): GMResult<Glider, GliderError> {
        if (!needsUpdateFromParent) {
            needsUpdateFromParent = true
            return GMResult.Ok(this)
        }
        return when (val slide = slideElement) {
            is Circle -> updateCircleGliderFromParent(slide)
            is Line -> updateLineGliderFromParent(slide)
            is Arc ->
                updateArcSectorGliderFromParent(
                    slide.arcSectorSlideGeometry(),
                )
            is Sector ->
                if (slide.type == Const.OBJECT_TYPE_SECTOR) {
                    updateArcSectorGliderFromParent(
                        slide.arcSectorSlideGeometry(),
                    )
                } else {
                    unsupportedSlideObject(slide)
                }
            is Curve -> updateCurveGliderFromParent(slide)
            is Point -> updatePointGlider(slide)
            else -> unsupportedSlideObject(slide)
        }
    }

    // JSXGraph: src/base/coordselement.js -> updateGliderFromParent,
    // OBJECT_CLASS_CIRCLE branch.
    private fun updateCircleGliderFromParent(
        circle: Circle,
    ): GMResult<Glider, GliderError> {
        val angle = (position ?: 0.0) * 2.0 * PI
        val radius = circle.Radius()
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(
                circle.center.X() + radius * cos(angle),
                circle.center.Y() + radius * sin(angle),
            ),
            doRound = false,
        )
        return GMResult.Ok(this)
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
        if (curve.transformations.isNotEmpty()) {
            val chain = curveTransformationChain(curve)
            when (val result = updateTransformationChain(chain)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val source = chain.last()
            val sourceCoordinates = doubleArrayOf(
                1.0,
                source.X(relativePosition),
                source.Y(relativePosition),
            )
            return when (
                val result = projectToCurve(
                    curve = source,
                    point = sourceCoordinates,
                    initialParameter = relativePosition,
                    rawCoordinates = true,
                )
            ) {
                is GMResult.Ok -> {
                    coords.setCoordinates(
                        coordType = Const.COORDS_BY_USER,
                        coordinates = applyTransformationChain(
                            chain = chain,
                            coordinates = result.value.point,
                        ),
                        doRound = false,
                    )
                    GMResult.Ok(this)
                }
                is GMResult.Err -> result
            }
        }
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

    // JSXGraph: src/base/coordselement.js -> updateGliderFromParent,
    // OBJECT_TYPE_ARC / OBJECT_TYPE_SECTOR branch.
    private fun updateArcSectorGliderFromParent(
        slide: ArcSectorSlideGeometry,
    ): GMResult<Glider, GliderError> {
        val baseAngle = Geometry.rad(
            doubleArrayOf(
                slide.center.X() + 1.0,
                slide.center.Y(),
            ),
            slide.center.Coords(),
            slide.radiusPoint.Coords(),
        )
        val interval = slide.selectedAngleInterval()
        var relativePosition = position ?: 0.0
        var angle = relativePosition * interval.delta
        if (angle < interval.alpha || angle > interval.beta) {
            angle = interval.beta
            if (
                (
                    angle < interval.alpha &&
                        angle > interval.alpha * 0.5
                    ) ||
                (
                    angle > interval.beta &&
                        angle > interval.beta * 0.5 + PI
                    )
            ) {
                angle = interval.alpha
            }
            relativePosition =
                if (abs(interval.delta) > Mat.eps) {
                    angle / interval.delta
                } else {
                    angle
                }
            position = relativePosition
        }

        val absoluteAngle =
            relativePosition * interval.delta + baseAngle
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(
                slide.center.X() + slide.radius * cos(absoluteAngle),
                slide.center.Y() + slide.radius * sin(absoluteAngle),
            ),
            doRound = false,
        )
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/coordselement.js -> updateGlider
    // transformed Curve branch.
    private fun projectToTransformedCurve(
        curve: Curve,
        point: DoubleArray,
        initialParameter: Double,
    ): GMResult<ProjectionResult, GliderError> {
        val chain = curveTransformationChain(curve)
        when (val result = updateTransformationChain(chain)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }

        var sourceCoordinates = point.copyOf()
        for ((index, slide) in chain.withIndex()) {
            val inverse = Mat.inverse(slide.transformMat)
            if (inverse.isEmpty()) {
                return GMResult.Err(
                    GliderError.NonInvertibleCurveTransformation(
                        curveId = slide.id,
                        chainIndex = index,
                    ),
                )
            }
            sourceCoordinates = Mat.matVecMult(
                inverse,
                sourceCoordinates,
            )
        }
        sourceCoordinates = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = sourceCoordinates,
            board = board,
        ).usrCoords.copyOf()

        val source = chain.last()
        return when (
            val result = projectToCurve(
                curve = source,
                point = sourceCoordinates,
                initialParameter = initialParameter,
                rawCoordinates = true,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                ProjectionResult(
                    point = applyTransformationChain(
                        chain = chain,
                        coordinates = result.value.point,
                    ),
                    parameter = result.value.parameter,
                ),
            )
            is GMResult.Err -> result
        }
    }

    private fun curveTransformationChain(curve: Curve): List<Curve> {
        val chain = mutableListOf(curve)
        var source = curve.transformationSource
        while (source != null) {
            chain += source
            source = source.transformationSource
        }
        return chain
    }

    private fun updateTransformationChain(
        chain: List<Curve>,
    ): GMResult<Unit, GliderError> {
        for (slide in chain) {
            when (val result = slide.updateTransformMatrix()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    GliderError.CurveEvaluation(result.error),
                )
            }
        }
        return GMResult.Ok(Unit)
    }

    private fun applyTransformationChain(
        chain: List<Curve>,
        coordinates: DoubleArray,
    ): DoubleArray {
        var transformed = coordinates.copyOf()
        for (slide in chain.asReversed()) {
            transformed = Coords(
                method = Const.COORDS_BY_USER,
                coordinates = Mat.matVecMult(
                    slide.transformMat,
                    transformed,
                ),
                board = board,
            ).usrCoords.copyOf()
        }
        return transformed
    }

    private fun projectToCurve(
        curve: Curve,
        point: DoubleArray,
        initialParameter: Double,
        rawCoordinates: Boolean = false,
    ): GMResult<ProjectionResult, GliderError> {
        curve.evaluationError?.let { error ->
            return GMResult.Err(GliderError.CurveEvaluation(error))
        }
        val result = if (curve.curveType == DATA_CURVE_TYPE) {
            val curvePoints =
                if (rawCoordinates) {
                    curve.dataX?.mapIndexed { index, x ->
                        doubleArrayOf(
                            1.0,
                            x,
                            curve.dataY?.getOrNull(index) ?: Double.NaN,
                        )
                    } ?: curve.points.map {
                        it.usrCoords.copyOf()
                    }
                } else {
                    curve.points.map { it.usrCoords.copyOf() }
                }
            Geometry.projectCoordsToCurve(
                point = point,
                curve = DiscreteCurve2D(
                    points = curvePoints,
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

    private fun unsupportedSlideObject(
        slide: GeometryElement,
    ): GMResult.Err<GliderError.UnsupportedSlideObject> =
        GMResult.Err(
            GliderError.UnsupportedSlideObject(
                slide.elType.ifEmpty { "element" },
            ),
        )

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
            var polygonHost: Polygon? = null
            val effectiveSlideObject =
                if (slideObject is Polygon) {
                    polygonHost = slideObject
                    val pointCoordinates = Coords(
                        method = Const.COORDS_BY_USER,
                        coordinates = coordinates,
                        board = board,
                    ).usrCoords
                    slideObject.borders.minByOrNull { border ->
                        Geometry.distPointLine(
                            pointCoordinates,
                            border.stdform,
                        )
                    } ?: return GMResult.Err(
                        GliderError.UnsupportedSlideObject(
                            slideObject.elType.ifEmpty { "polygon" },
                        ),
                    )
                } else {
                    slideObject
                }
            val supportedSlideObject = when (effectiveSlideObject) {
                is Circle -> effectiveSlideObject
                is Line -> effectiveSlideObject
                is Arc -> effectiveSlideObject
                is Sector -> {
                    if (
                        effectiveSlideObject.type !=
                        Const.OBJECT_TYPE_SECTOR
                    ) {
                        return GMResult.Err(
                            GliderError.UnsupportedSlideObject(
                                effectiveSlideObject.elType.ifEmpty {
                                    "sector"
                                },
                            ),
                        )
                    }
                    effectiveSlideObject
                }
                is Point -> effectiveSlideObject
                is Curve -> effectiveSlideObject
                else -> return GMResult.Err(
                    GliderError.UnsupportedSlideObject(
                        effectiveSlideObject.elType.ifEmpty { "element" },
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
                polygonHost = polygonHost,
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

private data class ArcSectorSlideGeometry(
    val center: Point,
    val radiusPoint: Point,
    val anglePoint: Point,
    val radius: Double,
    val selection: String,
) {
    fun selectedAngleInterval(): SelectedAngleInterval {
        var alpha = 0.0
        var beta = Geometry.rad(
            radiusPoint.Coords(),
            center.Coords(),
            anglePoint.Coords(),
        )
        if (
            (
                selection == Arc.SELECTION_MINOR &&
                    beta > PI
                ) ||
            (
                selection == Arc.SELECTION_MAJOR &&
                    beta < PI
                )
        ) {
            alpha = beta
            beta = 2.0 * PI
        }
        return SelectedAngleInterval(
            alpha = alpha,
            beta = beta,
        )
    }
}

private data class SelectedAngleInterval(
    val alpha: Double,
    val beta: Double,
) {
    val delta: Double
        get() = beta - alpha
}

private fun Arc.arcSectorSlideGeometry(): ArcSectorSlideGeometry =
    ArcSectorSlideGeometry(
        center = center,
        radiusPoint = radiuspoint,
        anglePoint = anglepoint,
        radius = Radius(),
        selection = selection,
    )

private fun Sector.arcSectorSlideGeometry(): ArcSectorSlideGeometry =
    ArcSectorSlideGeometry(
        center = center,
        radiusPoint = radiuspoint,
        anglePoint = anglepoint,
        radius = Radius(),
        selection = selection,
    )
