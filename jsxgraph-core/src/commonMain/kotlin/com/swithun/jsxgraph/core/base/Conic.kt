/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/conic.js ->
 * createEllipse / createHyperbola / createParabola / createConic
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeNumericCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI

internal sealed interface EllipseError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : EllipseError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : EllipseError

    data class ImplicitPointNotParent(
        val id: String,
    ) : EllipseError

    data class DuplicateElementId(
        val id: String,
    ) : EllipseError

    data class CenterCreation(
        val error: PointError,
    ) : EllipseError

    data class CurveCreation(
        val error: CurveError,
    ) : EllipseError
}

internal data class CurveEllipseDefinition(
    val focus1: Point,
    val focus2: Point,
    val pointOnEllipse: Point?,
    val majorAxisTerm: JessieCodeCoordinateFunction?,
    val center: Point,
    val minimum: Double,
    val maximum: Double,
)

internal sealed interface HyperbolaError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : HyperbolaError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : HyperbolaError

    data class ImplicitPointNotParent(
        val id: String,
    ) : HyperbolaError

    data class DuplicateElementId(
        val id: String,
    ) : HyperbolaError

    data class CenterCreation(
        val error: PointError,
    ) : HyperbolaError

    data class CurveCreation(
        val error: CurveError,
    ) : HyperbolaError
}

internal data class CurveHyperbolaDefinition(
    val focus1: Point,
    val focus2: Point,
    val pointOnHyperbola: Point?,
    val majorAxisTerm: JessieCodeCoordinateFunction?,
    val center: Point,
    val minimum: Double,
    val maximum: Double,
)

internal sealed interface ParabolaError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : ParabolaError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : ParabolaError

    data class ImplicitElementNotParent(
        val id: String,
    ) : ParabolaError

    data class DuplicateElementId(
        val id: String,
    ) : ParabolaError

    data class CenterCreation(
        val error: PointError,
    ) : ParabolaError

    data class CurveCreation(
        val error: CurveError,
    ) : ParabolaError
}

internal data class CurveParabolaDefinition(
    val focus: Point,
    val directrix: Line,
    val center: Point,
    val minimum: Double,
    val maximum: Double,
)

internal sealed interface ConicError {
    data class InvalidCoefficientCount(
        val count: Int,
    ) : ConicError

    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : ConicError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : ConicError

    data class ImplicitPointNotParent(
        val id: String,
    ) : ConicError

    data class DuplicateElementId(
        val id: String,
    ) : ConicError

    data class CenterCreation(
        val error: PointError,
    ) : ConicError

    data class CurveCreation(
        val error: CurveError,
    ) : ConicError
}

internal sealed interface CurveConicSource {
    data class Points(
        val points: List<Point>,
    ) : CurveConicSource

    data class Coefficients(
        val terms: List<JessieCodeCoordinateFunction>,
    ) : CurveConicSource
}

internal data class CurveConicDefinition(
    val source: CurveConicSource,
    val minimum: Double,
    val maximum: Double,
    var center: Point? = null,
    var eigenvalues: Array<DoubleArray>? = null,
    var rotationMatrix: Array<DoubleArray> = arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0),
        doubleArrayOf(0.0, 1.0, 0.0),
        doubleArrayOf(0.0, 0.0, 1.0),
    ),
    var c: Double = Double.NaN,
    var a: Double = Double.NaN,
    var b: Double = Double.NaN,
)

internal object Conic {
    private const val COEFFICIENT_COUNT = 6

    // JSXGraph 1.13.3: src/element/conic.js -> createConic
    internal fun create(
        board: Board,
        point1: Point,
        point2: Point,
        point3: Point,
        point4: Point,
        point5: Point,
        parentlessPoints: Set<Point> = emptySet(),
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, ConicError> {
        val points = listOf(point1, point2, point3, point4, point5)
        for ((index, point) in points.withIndex()) {
            validateParent(board, point, index)?.let {
                return GMResult.Err(it)
            }
        }
        parentlessPoints.firstOrNull { it !in points }?.let {
            return GMResult.Err(
                ConicError.ImplicitPointNotParent(it.id),
            )
        }
        return create(
            board = board,
            definition = CurveConicDefinition(
                source = CurveConicSource.Points(points),
                minimum = 0.0,
                maximum = 2.0 * PI,
            ),
            parentlessPoints = parentlessPoints,
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )
    }

    // JSXGraph 1.13.3: src/element/conic.js -> createConic
    internal fun create(
        board: Board,
        coefficients: DoubleArray,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, ConicError> =
        create(
            board = board,
            coefficientTerms = coefficients.map(
                ::JessieCodeNumericCoordinateFunction,
            ),
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )

    // JSXGraph 1.13.3: src/element/conic.js -> createConic
    internal fun create(
        board: Board,
        coefficientTerms: List<JessieCodeCoordinateFunction>,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, ConicError> {
        if (coefficientTerms.size != COEFFICIENT_COUNT) {
            return GMResult.Err(
                ConicError.InvalidCoefficientCount(
                    coefficientTerms.size,
                ),
            )
        }
        return create(
            board = board,
            definition = CurveConicDefinition(
                source = CurveConicSource.Coefficients(
                    coefficientTerms.toList(),
                ),
                minimum = 0.0,
                maximum = 2.0 * PI,
            ),
            parentlessPoints = emptySet(),
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )
    }

    private fun create(
        board: Board,
        definition: CurveConicDefinition,
        parentlessPoints: Set<Point>,
        sampleCount: Int,
        plotOptions: CurvePlotOptions,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        centerId: String,
        centerName: String?,
        centerNeedsRegularUpdate: Boolean,
        centerFixed: Boolean,
    ): GMResult<Curve, ConicError> {
        if (id.isNotEmpty() && board.elementById(id) != null) {
            return GMResult.Err(ConicError.DuplicateElementId(id))
        }
        if (centerId.isNotEmpty() && board.elementById(centerId) != null) {
            return GMResult.Err(
                ConicError.DuplicateElementId(centerId),
            )
        }

        val curve = when (
            val result = Curve.createConicShell(
                board = board,
                definition = definition,
                sampleCount = sampleCount,
                plotOptions = plotOptions,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    ConicError.CurveCreation(result.error),
                )
            }
        }
        val center = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    ConicCenterCoordinateFunction(curve),
                ),
                id = centerId,
                name = centerName,
                needsRegularUpdate = centerNeedsRegularUpdate,
                fixed = centerFixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(curve)
                return GMResult.Err(
                    ConicError.CenterCreation(result.error),
                )
            }
        }

        definition.center = center
        curve.subs["center"] = center
        curve.inherits += center
        val points = (
            definition.source as?
                CurveConicSource.Points
            )?.points.orEmpty()
        curve.inherits += points
        for (point in points) {
            point.addChild(curve)
        }
        curve.setParents(points.filterNot(parentlessPoints::contains))

        return when (val result = curve.initializeConic()) {
            is GMResult.Ok -> result
            is GMResult.Err -> {
                board.removeObject(curve)
                GMResult.Err(
                    ConicError.CurveCreation(result.error),
                )
            }
        }
    }

    private fun validateParent(
        board: Board,
        point: Point,
        parentIndex: Int,
    ): ConicError? {
        if (point.board !== board) {
            return ConicError.ParentBoardMismatch(parentIndex)
        }
        if (board.elementById(point.id) !== point) {
            return ConicError.ParentNotRegistered(
                parentIndex = parentIndex,
                id = point.id,
            )
        }
        return null
    }
}

internal object Ellipse {
    private const val CENTER_COORDINATE_COUNT = 2

    // JSXGraph 1.13.3: src/element/conic.js -> createEllipse
    internal fun create(
        board: Board,
        focus1: Point,
        focus2: Point,
        pointOnEllipse: Point,
        parentlessPoints: Set<Point> = emptySet(),
        minimum: Double = 0.0,
        maximum: Double = 2.0 * PI,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, EllipseError> =
        create(
            board = board,
            focus1 = focus1,
            focus2 = focus2,
            pointOnEllipse = pointOnEllipse,
            majorAxisTerm = null,
            parentlessPoints = parentlessPoints,
            minimum = minimum,
            maximum = maximum,
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )

    // JSXGraph 1.13.3: src/element/conic.js -> createEllipse
    internal fun create(
        board: Board,
        focus1: Point,
        focus2: Point,
        majorAxis: Double,
        parentlessPoints: Set<Point> = emptySet(),
        minimum: Double = 0.0,
        maximum: Double = 2.0 * PI,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, EllipseError> =
        create(
            board = board,
            focus1 = focus1,
            focus2 = focus2,
            pointOnEllipse = null,
            majorAxisTerm = JessieCodeNumericCoordinateFunction(majorAxis),
            parentlessPoints = parentlessPoints,
            minimum = minimum,
            maximum = maximum,
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )

    // JSXGraph 1.13.3: src/element/conic.js -> createEllipse
    internal fun create(
        board: Board,
        focus1: Point,
        focus2: Point,
        majorAxisTerm: JessieCodeCoordinateFunction,
        parentlessPoints: Set<Point> = emptySet(),
        minimum: Double = 0.0,
        maximum: Double = 2.0 * PI,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, EllipseError> =
        create(
            board = board,
            focus1 = focus1,
            focus2 = focus2,
            pointOnEllipse = null,
            majorAxisTerm = majorAxisTerm,
            parentlessPoints = parentlessPoints,
            minimum = minimum,
            maximum = maximum,
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )

    private fun create(
        board: Board,
        focus1: Point,
        focus2: Point,
        pointOnEllipse: Point?,
        majorAxisTerm: JessieCodeCoordinateFunction?,
        parentlessPoints: Set<Point>,
        minimum: Double,
        maximum: Double,
        sampleCount: Int,
        plotOptions: CurvePlotOptions,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        centerId: String,
        centerName: String?,
        centerNeedsRegularUpdate: Boolean,
        centerFixed: Boolean,
    ): GMResult<Curve, EllipseError> {
        val pointParents = listOfNotNull(focus1, focus2, pointOnEllipse)
        for ((index, point) in pointParents.withIndex()) {
            validateParent(board, point, index)?.let {
                return GMResult.Err(it)
            }
        }
        parentlessPoints.firstOrNull { it !in pointParents }?.let {
            return GMResult.Err(EllipseError.ImplicitPointNotParent(it.id))
        }
        if (id.isNotEmpty() && board.elementById(id) != null) {
            return GMResult.Err(EllipseError.DuplicateElementId(id))
        }
        if (centerId.isNotEmpty() && board.elementById(centerId) != null) {
            return GMResult.Err(EllipseError.DuplicateElementId(centerId))
        }

        val center = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = List(CENTER_COORDINATE_COUNT) { index ->
                    EllipseCenterCoordinateFunction(
                        focus1 = focus1,
                        focus2 = focus2,
                        coordinateIndex = index,
                    )
                },
                id = centerId,
                name = centerName,
                needsRegularUpdate = centerNeedsRegularUpdate,
                fixed = centerFixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(EllipseError.CenterCreation(result.error))
            }
        }
        val definition = CurveEllipseDefinition(
            focus1 = focus1,
            focus2 = focus2,
            pointOnEllipse = pointOnEllipse,
            majorAxisTerm = majorAxisTerm,
            center = center,
            minimum = minimum,
            maximum = maximum,
        )
        return when (
            val result = Curve.createEllipse(
                board = board,
                definition = definition,
                parentlessPoints = parentlessPoints,
                sampleCount = sampleCount,
                plotOptions = plotOptions,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> {
                board.removeObject(center)
                GMResult.Err(EllipseError.CurveCreation(result.error))
            }
        }
    }

    private fun validateParent(
        board: Board,
        point: Point,
        parentIndex: Int,
    ): EllipseError? {
        if (point.board !== board) {
            return EllipseError.ParentBoardMismatch(parentIndex)
        }
        if (board.elementById(point.id) !== point) {
            return EllipseError.ParentNotRegistered(
                parentIndex = parentIndex,
                id = point.id,
            )
        }
        return null
    }
}

internal object Hyperbola {
    private const val CENTER_COORDINATE_COUNT = 2
    private const val DEFAULT_DOMAIN_FACTOR = 1.0001

    // JSXGraph 1.13.3: src/element/conic.js -> createHyperbola
    internal fun create(
        board: Board,
        focus1: Point,
        focus2: Point,
        pointOnHyperbola: Point,
        parentlessPoints: Set<Point> = emptySet(),
        minimum: Double = -DEFAULT_DOMAIN_FACTOR * PI,
        maximum: Double = DEFAULT_DOMAIN_FACTOR * PI,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, HyperbolaError> =
        create(
            board = board,
            focus1 = focus1,
            focus2 = focus2,
            pointOnHyperbola = pointOnHyperbola,
            majorAxisTerm = null,
            parentlessPoints = parentlessPoints,
            minimum = minimum,
            maximum = maximum,
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )

    // JSXGraph 1.13.3: src/element/conic.js -> createHyperbola
    internal fun create(
        board: Board,
        focus1: Point,
        focus2: Point,
        majorAxis: Double,
        parentlessPoints: Set<Point> = emptySet(),
        minimum: Double = -DEFAULT_DOMAIN_FACTOR * PI,
        maximum: Double = DEFAULT_DOMAIN_FACTOR * PI,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, HyperbolaError> =
        create(
            board = board,
            focus1 = focus1,
            focus2 = focus2,
            pointOnHyperbola = null,
            majorAxisTerm = JessieCodeNumericCoordinateFunction(majorAxis),
            parentlessPoints = parentlessPoints,
            minimum = minimum,
            maximum = maximum,
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )

    // JSXGraph 1.13.3: src/element/conic.js -> createHyperbola
    internal fun create(
        board: Board,
        focus1: Point,
        focus2: Point,
        majorAxisTerm: JessieCodeCoordinateFunction,
        parentlessPoints: Set<Point> = emptySet(),
        minimum: Double = -DEFAULT_DOMAIN_FACTOR * PI,
        maximum: Double = DEFAULT_DOMAIN_FACTOR * PI,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, HyperbolaError> =
        create(
            board = board,
            focus1 = focus1,
            focus2 = focus2,
            pointOnHyperbola = null,
            majorAxisTerm = majorAxisTerm,
            parentlessPoints = parentlessPoints,
            minimum = minimum,
            maximum = maximum,
            sampleCount = sampleCount,
            plotOptions = plotOptions,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            centerId = centerId,
            centerName = centerName,
            centerNeedsRegularUpdate = centerNeedsRegularUpdate,
            centerFixed = centerFixed,
        )

    private fun create(
        board: Board,
        focus1: Point,
        focus2: Point,
        pointOnHyperbola: Point?,
        majorAxisTerm: JessieCodeCoordinateFunction?,
        parentlessPoints: Set<Point>,
        minimum: Double,
        maximum: Double,
        sampleCount: Int,
        plotOptions: CurvePlotOptions,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        centerId: String,
        centerName: String?,
        centerNeedsRegularUpdate: Boolean,
        centerFixed: Boolean,
    ): GMResult<Curve, HyperbolaError> {
        val pointParents = listOfNotNull(
            focus1,
            focus2,
            pointOnHyperbola,
        )
        for ((index, point) in pointParents.withIndex()) {
            validateParent(board, point, index)?.let {
                return GMResult.Err(it)
            }
        }
        parentlessPoints.firstOrNull { it !in pointParents }?.let {
            return GMResult.Err(
                HyperbolaError.ImplicitPointNotParent(it.id),
            )
        }
        if (id.isNotEmpty() && board.elementById(id) != null) {
            return GMResult.Err(HyperbolaError.DuplicateElementId(id))
        }
        if (centerId.isNotEmpty() && board.elementById(centerId) != null) {
            return GMResult.Err(
                HyperbolaError.DuplicateElementId(centerId),
            )
        }

        val center = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = List(CENTER_COORDINATE_COUNT) { index ->
                    HyperbolaCenterCoordinateFunction(
                        focus1 = focus1,
                        focus2 = focus2,
                        coordinateIndex = index,
                    )
                },
                id = centerId,
                name = centerName,
                needsRegularUpdate = centerNeedsRegularUpdate,
                fixed = centerFixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    HyperbolaError.CenterCreation(result.error),
                )
            }
        }
        val definition = CurveHyperbolaDefinition(
            focus1 = focus1,
            focus2 = focus2,
            pointOnHyperbola = pointOnHyperbola,
            majorAxisTerm = majorAxisTerm,
            center = center,
            minimum = minimum,
            maximum = maximum,
        )
        return when (
            val result = Curve.createHyperbola(
                board = board,
                definition = definition,
                parentlessPoints = parentlessPoints,
                sampleCount = sampleCount,
                plotOptions = plotOptions,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> {
                board.removeObject(center)
                GMResult.Err(HyperbolaError.CurveCreation(result.error))
            }
        }
    }

    private fun validateParent(
        board: Board,
        point: Point,
        parentIndex: Int,
    ): HyperbolaError? {
        if (point.board !== board) {
            return HyperbolaError.ParentBoardMismatch(parentIndex)
        }
        if (board.elementById(point.id) !== point) {
            return HyperbolaError.ParentNotRegistered(
                parentIndex = parentIndex,
                id = point.id,
            )
        }
        return null
    }
}

internal object Parabola {
    private const val CENTER_COORDINATE_COUNT = 2

    // JSXGraph 1.13.3: src/element/conic.js -> createParabola
    internal fun create(
        board: Board,
        focus: Point,
        directrix: Line,
        parentlessElements: Set<GeometryElement> = emptySet(),
        minimum: Double = 0.0,
        maximum: Double = 2.0 * PI,
        sampleCount: Int = Curve.DEFAULT_SAMPLE_COUNT,
        plotOptions: CurvePlotOptions = CurvePlotOptions(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        centerId: String = "",
        centerName: String? = "",
        centerNeedsRegularUpdate: Boolean = true,
        centerFixed: Boolean = false,
    ): GMResult<Curve, ParabolaError> {
        val parents = listOf<GeometryElement>(focus, directrix)
        for ((index, parent) in parents.withIndex()) {
            validateParent(board, parent, index)?.let {
                return GMResult.Err(it)
            }
        }
        parentlessElements.firstOrNull { it !in parents }?.let {
            return GMResult.Err(
                ParabolaError.ImplicitElementNotParent(it.id),
            )
        }
        if (id.isNotEmpty() && board.elementById(id) != null) {
            return GMResult.Err(ParabolaError.DuplicateElementId(id))
        }
        if (centerId.isNotEmpty() && board.elementById(centerId) != null) {
            return GMResult.Err(
                ParabolaError.DuplicateElementId(centerId),
            )
        }

        val center = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions =
                    List(CENTER_COORDINATE_COUNT) { index ->
                        ParabolaCenterCoordinateFunction(
                            focus = focus,
                            directrix = directrix,
                            coordinateIndex = index,
                        )
                    },
                id = centerId,
                name = centerName,
                needsRegularUpdate = centerNeedsRegularUpdate,
                fixed = centerFixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    ParabolaError.CenterCreation(result.error),
                )
            }
        }
        val definition = CurveParabolaDefinition(
            focus = focus,
            directrix = directrix,
            center = center,
            minimum = minimum,
            maximum = maximum,
        )
        return when (
            val result = Curve.createParabola(
                board = board,
                definition = definition,
                parentlessElements = parentlessElements,
                sampleCount = sampleCount,
                plotOptions = plotOptions,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> {
                board.removeObject(center)
                GMResult.Err(ParabolaError.CurveCreation(result.error))
            }
        }
    }

    private fun validateParent(
        board: Board,
        element: GeometryElement,
        parentIndex: Int,
    ): ParabolaError? {
        if (element.board !== board) {
            return ParabolaError.ParentBoardMismatch(parentIndex)
        }
        if (board.elementById(element.id) !== element) {
            return ParabolaError.ParentNotRegistered(
                parentIndex = parentIndex,
                id = element.id,
            )
        }
        return null
    }
}

private class EllipseCenterCoordinateFunction(
    private val focus1: Point,
    private val focus2: Point,
    private val coordinateIndex: Int,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val value =
            if (coordinateIndex == 0) {
                (focus1.X() + focus2.X()) * 0.5
            } else {
                (focus1.Y() + focus2.Y()) * 0.5
            }
        return GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))
    }
}

private class HyperbolaCenterCoordinateFunction(
    private val focus1: Point,
    private val focus2: Point,
    private val coordinateIndex: Int,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val value =
            if (coordinateIndex == 0) {
                (focus1.X() + focus2.X()) * 0.5
            } else {
                (focus1.Y() + focus2.Y()) * 0.5
            }
        return GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))
    }
}

private class ParabolaCenterCoordinateFunction(
    private val focus: Point,
    private val directrix: Line,
    private val coordinateIndex: Int,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()

    // JSXGraph 1.13.3: src/element/conic.js -> createParabola M
    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val projection = Geometry.projectPointToLine(
            point = focus.coords.usrCoords,
            line = directrix.stdform,
        )
        return GMResult.Ok(
            JessieCodeRuntimeValue.NumberValue(
                projection.getOrElse(coordinateIndex + 1) {
                    Double.NaN
                },
            ),
        )
    }
}

private class ConicCenterCoordinateFunction(
    private val curve: Curve,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> =
        mapOf(curve.id to curve)
    override val returnsCoordinateArray: Boolean = true

    // JSXGraph 1.13.3: src/element/conic.js -> createConic midpoint
    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val matrix = curve.quadraticform
        return GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                listOf(
                    JessieCodeRuntimeValue.NumberValue(
                        matrix[1][1] * matrix[2][2] -
                            matrix[1][2] * matrix[1][2],
                    ),
                    JessieCodeRuntimeValue.NumberValue(
                        matrix[1][2] * matrix[0][2] -
                            matrix[2][2] * matrix[0][1],
                    ),
                    JessieCodeRuntimeValue.NumberValue(
                        matrix[0][1] * matrix[1][2] -
                            matrix[1][1] * matrix[0][2],
                    ),
                ),
            ),
        )
    }
}
