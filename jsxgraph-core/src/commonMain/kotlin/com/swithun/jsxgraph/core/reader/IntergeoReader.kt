/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/intergeo.js -> IntergeoReader constructor,
 * readElements, readElement, storePoint, storeLine, storeConic,
 * readConstraints, readConstraint, and readParams.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BisectorLineAttributes
import com.swithun.jsxgraph.core.base.BisectorLines
import com.swithun.jsxgraph.core.base.BisectorLinesError
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.Conic
import com.swithun.jsxgraph.core.base.ConicError
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.GliderError
import com.swithun.jsxgraph.core.base.IntersectionError
import com.swithun.jsxgraph.core.base.IntersectionIndexSource
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.MidpointError
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.OrthogonalConstructionError
import com.swithun.jsxgraph.core.base.OtherIntersectionPoint
import com.swithun.jsxgraph.core.base.ParallelConstructionError
import com.swithun.jsxgraph.core.base.ParallelLine
import com.swithun.jsxgraph.core.base.PerpendicularLine
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.PolygonError
import com.swithun.jsxgraph.core.base.Tangent
import com.swithun.jsxgraph.core.base.TangentError
import com.swithun.jsxgraph.core.base.TriangleCenterConstructionError
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.utils.XML
import com.swithun.jsxgraph.core.utils.XmlDocument
import com.swithun.jsxgraph.core.utils.XmlElement
import com.swithun.jsxgraph.core.utils.XmlError
import com.swithun.jsxgraph.core.utils.XmlLimits
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal data class IntergeoReaderLimits(
    val preparation: ReaderPreparationLimits = ReaderPreparationLimits(),
    val xml: XmlLimits = XmlLimits(),
    val maxElements: Int = 100_000,
    val maxConstraints: Int = 100_000,
    val maxParameters: Int = 1_000_000,
)

internal sealed interface IntergeoReaderError : ReaderDomainError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : IntergeoReaderError

    data class PreparationFailed(
        val cause: ReaderPreparationError,
    ) : IntergeoReaderError

    data class XmlParsingFailed(
        val cause: XmlError,
    ) : IntergeoReaderError

    data class MissingSection(
        val name: String,
    ) : IntergeoReaderError

    data class ElementLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : IntergeoReaderError

    data class ConstraintLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : IntergeoReaderError

    data class ParameterLimitExceeded(
        val limit: Int,
        val requested: Long,
    ) : IntergeoReaderError

    data class MissingElementId(
        val elementType: String,
    ) : IntergeoReaderError

    data class MissingElementContent(
        val elementType: String,
        val elementId: String,
    ) : IntergeoReaderError

    data class MissingTextValue(
        val parentType: String,
        val childType: String,
        val childIndex: Int,
    ) : IntergeoReaderError

    data class InvalidConstraintParameterCount(
        val constraintType: String,
        val minimum: Int,
        val actual: Int,
    ) : IntergeoReaderError

    data class MissingConstraintParameterList(
        val constraintType: String,
        val listName: String,
    ) : IntergeoReaderError

    data class MissingStoredObject(
        val id: String,
    ) : IntergeoReaderError

    data class StoredObjectTypeMismatch(
        val id: String,
        val expected: String,
        val actual: String,
    ) : IntergeoReaderError

    data class InvalidStoredCoordinateCount(
        val id: String,
        val minimum: Int,
        val actual: Int,
    ) : IntergeoReaderError

    data class BoardCreationFailed(
        val id: String,
        val cause: IntergeoCreationError,
    ) : IntergeoReaderError

    data class MissingDisplayElement(
        val reference: String,
    ) : IntergeoReaderError

    data class UnsupportedBoardConstraints(
        val constraintTypes: List<String>,
    ) : IntergeoReaderError
}

internal sealed interface IntergeoCreationError {
    data class Point(val cause: PointError) : IntergeoCreationError

    data class Line(val cause: LineError) : IntergeoCreationError

    data class Circle(val cause: CircleError) : IntergeoCreationError

    data class Conic(val cause: ConicError) : IntergeoCreationError

    data class Midpoint(val cause: MidpointError) : IntergeoCreationError

    data class Intersection(
        val cause: IntersectionError,
    ) : IntergeoCreationError

    data class Glider(val cause: GliderError) : IntergeoCreationError

    data class TriangleCenter(
        val cause: TriangleCenterConstructionError,
    ) : IntergeoCreationError

    data class BisectorLines(
        val cause: BisectorLinesError,
    ) : IntergeoCreationError

    data class Parallel(
        val cause: ParallelConstructionError,
    ) : IntergeoCreationError

    data class Perpendicular(
        val cause: OrthogonalConstructionError,
    ) : IntergeoCreationError

    data class Polygon(
        val cause: PolygonError,
    ) : IntergeoCreationError

    data class Tangent(
        val cause: TangentError,
    ) : IntergeoCreationError
}

internal sealed interface IntergeoReaderDiagnostic {
    data class UnsupportedElement(
        val elementType: String,
        val elementId: String?,
    ) : IntergeoReaderDiagnostic

    data class UnsupportedCoordinateType(
        val elementType: String,
        val elementId: String,
        val coordinateType: String,
    ) : IntergeoReaderDiagnostic

    data class UnsupportedHomogeneousComponent(
        val elementId: String,
        val componentType: String,
    ) : IntergeoReaderDiagnostic

    data class UnsupportedPointCoordinates(
        val elementId: String,
        val valueCount: Int,
    ) : IntergeoReaderDiagnostic

    data class UnsupportedConstraint(
        val constraintType: String,
        val firstParameter: String?,
    ) : IntergeoReaderDiagnostic

    data class UnsupportedBoardConstraint(
        val constraintType: String,
        val firstParameter: String?,
    ) : IntergeoReaderDiagnostic

    data class UnsupportedDisplayProperty(
        val propertyType: String,
    ) : IntergeoReaderDiagnostic
}

internal sealed interface IntergeoStoredObject {
    val id: String
    val coords: List<Double>
    val exists: Boolean
        get() = false
    val i2geoType: String
}

internal data class IntergeoPointObject(
    override val id: String,
    override val coords: List<Double>,
) : IntergeoStoredObject {
    override val i2geoType: String = "point"
}

internal data class IntergeoLineObject(
    override val id: String,
    override val coords: List<Double>,
) : IntergeoStoredObject {
    override val i2geoType: String = "line"
}

internal data class IntergeoConicObject(
    override val id: String,
    override val coords: List<Double>,
) : IntergeoStoredObject {
    override val i2geoType: String = "conic"
}

internal data class IntergeoConstraint(
    val name: String,
    val parameters: List<String>,
    val supportedByUpstreamReader: Boolean,
    val nestedParameters: Map<String, List<String>> = emptyMap(),
)

internal data class IntergeoDisplayStyle(
    val reference: String,
    val attributes: Map<String, Any?>,
)

internal data class IntergeoDisplay(
    val backgroundColor: String? = null,
    val styles: List<IntergeoDisplayStyle> = emptyList(),
)

internal data class ParsedIntergeo(
    val objects: Map<String, IntergeoStoredObject>,
    val constraints: List<IntergeoConstraint>,
    val diagnostics: List<IntergeoReaderDiagnostic>,
    val display: IntergeoDisplay = IntergeoDisplay(),
)

internal data class DrawnIntergeo(
    val parsed: ParsedIntergeo,
    val objects: Map<String, GeometryElement>,
    val diagnostics: List<IntergeoReaderDiagnostic>,
)

private data class IntergeoBoardReadState(
    val originX: Double,
    val originY: Double,
    val unitX: Double,
    val unitY: Double,
    val attributes: Map<String, Any?>,
)

/**
 * Translated Intergeo reader slice.
 *
 * The parse model covers all upstream element and constraint dispatch names.
 * Board creation currently covers direct point/line/conic cleanup and the
 * constraints listed in [BOARD_CONSTRAINTS]. Other upstream constraints stay
 * explicit diagnostics until their complete element contracts are available.
 */
internal class IntergeoReader(
    private val data: String,
) {
    // JSXGraph 1.13.3: src/reader/intergeo.js -> constructor and read.
    internal fun parse(
        limits: IntergeoReaderLimits = IntergeoReaderLimits(),
    ): GMResult<ParsedIntergeo, IntergeoReaderError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }

        val prepared = when (
            val result = ReaderPreparation.prepareIntergeo(
                source = data,
                limits = limits.preparation,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    IntergeoReaderError.PreparationFailed(result.error),
                )
            }
        }
        val tree = when (
            val result = XML.parse(
                input = prepared,
                limits = limits.xml,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    IntergeoReaderError.XmlParsingFailed(result.error),
                )
            }
        }

        return parseTree(tree, limits)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> read, readConstraints,
    // readConstraint, cleanUp, addPoint, addLine, and addConic.
    internal fun read(
        board: Board,
        limits: IntergeoReaderLimits = IntergeoReaderLimits(),
        failOnUnsupportedBoardConstraints: Boolean = false,
    ): GMResult<DrawnIntergeo, IntergeoReaderError> {
        val parsed = when (val result = parse(limits)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val unsupportedBoardConstraints = parsed.constraints
            .filter {
                it.supportedByUpstreamReader &&
                    it.name !in BOARD_CONSTRAINTS
            }
            .map(IntergeoConstraint::name)
            .distinct()
        if (
            failOnUnsupportedBoardConstraints &&
            unsupportedBoardConstraints.isNotEmpty()
        ) {
            return GMResult.Err(
                IntergeoReaderError.UnsupportedBoardConstraints(
                    unsupportedBoardConstraints,
                ),
            )
        }
        val diagnostics = parsed.diagnostics.toMutableList()
        val objects = linkedMapOf<String, GeometryElement>()
        val existingIds = board.objects.keys.toSet()
        val boardState = IntergeoBoardReadState(
            originX = board.origin.scrCoords[1],
            originY = board.origin.scrCoords[2],
            unitX = board.unitX,
            unitY = board.unitY,
            attributes = board.attributes.toMap(),
        )
        board.setCoordinateSystem(
            originX = INTERGEO_ORIGIN_X,
            originY = INTERGEO_ORIGIN_Y,
            unitX = INTERGEO_UNIT_X,
            unitY = INTERGEO_UNIT_Y,
        )
        board.suspendUpdate()

        for (constraint in parsed.constraints) {
            if (!constraint.supportedByUpstreamReader) {
                continue
            }
            if (constraint.name !in BOARD_CONSTRAINTS) {
                diagnostics +=
                    IntergeoReaderDiagnostic.UnsupportedBoardConstraint(
                        constraintType = constraint.name,
                        firstParameter =
                            constraint.parameters.firstOrNull(),
                    )
                continue
            }
            when (
                val result = applyConstraint(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    rollbackBoardRead(board, existingIds, boardState)
                    return result
                }
            }
        }

        for ((id, stored) in parsed.objects) {
            if (id in objects) {
                continue
            }
            val element = when (
                val result = materializeStoredObject(
                    board = board,
                    stored = stored,
                    explicitId = false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    rollbackBoardRead(board, existingIds, boardState)
                    return result
                }
            }
            objects[id] = element
        }

        when (
            val result = readDisplay(
                board = board,
                display = parsed.display,
                objects = objects,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> {
                rollbackBoardRead(board, existingIds, boardState)
                return result
            }
        }
        board.unsuspendUpdate()
        return GMResult.Ok(
            DrawnIntergeo(
                parsed = parsed,
                objects = objects.toMap(),
                diagnostics = diagnostics.toList(),
            ),
        )
    }

    private fun applyConstraint(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> =
        when (constraint.name) {
            LINE_THROUGH_TWO_POINTS_CONSTRAINT ->
                createLineThroughTwoPoints(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                    straightFirst = true,
                    straightLast = true,
                )
            RAY_FROM_POINT_THROUGH_POINT_CONSTRAINT ->
                createLineThroughTwoPoints(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                    straightFirst = false,
                    straightLast = true,
                )
            LINE_SEGMENT_BY_POINTS_CONSTRAINT ->
                createLineThroughTwoPoints(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                    straightFirst = false,
                    straightLast = false,
                )
            LINE_THROUGH_POINT_CONSTRAINT ->
                createLineThroughPoint(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            LINE_PARALLEL_TO_LINE_THROUGH_POINT_CONSTRAINT,
            RAY_FROM_POINT_AND_VECTOR_CONSTRAINT,
            -> createParallelThroughPoint(
                board = board,
                parsed = parsed,
                objects = objects,
                constraint = constraint,
                isRay =
                    constraint.name ==
                        RAY_FROM_POINT_AND_VECTOR_CONSTRAINT,
            )
            LINE_PERPENDICULAR_TO_LINE_THROUGH_POINT_CONSTRAINT ->
                createPerpendicularThroughPoint(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            ENDPOINTS_OF_LINE_SEGMENT_CONSTRAINT ->
                constrainSegmentEndpoints(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            FREE_LINE_CONSTRAINT ->
                createFreeLine(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            MIDPOINT_OF_TWO_POINTS_CONSTRAINT,
            MIDPOINT_CONSTRAINT,
            -> createMidpointOfTwoPoints(
                board = board,
                parsed = parsed,
                objects = objects,
                constraint = constraint,
            )
            MIDPOINT_OF_LINE_SEGMENT_CONSTRAINT,
            MIDPOINT_LINE_SEGMENT_CONSTRAINT,
            -> createMidpointOfLineSegment(
                board = board,
                parsed = parsed,
                objects = objects,
                constraint = constraint,
            )
            POINT_INTERSECTION_OF_TWO_LINES_CONSTRAINT ->
                createPointIntersectionOfTwoLines(
                    board = board,
                    objects = objects,
                    constraint = constraint,
                )
            CIRCLE_BY_CENTER_AND_POINT_CONSTRAINT ->
                createCircleByCenterAndPoint(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            CENTER_OF_CIRCLE_CONSTRAINT ->
                createCenterOfCircle(
                    board = board,
                    objects = objects,
                    constraint = constraint,
                )
            POINT_ON_LINE_CONSTRAINT,
            POINT_ON_LINE_SEGMENT_CONSTRAINT,
            -> createPointOnLine(
                board = board,
                parsed = parsed,
                objects = objects,
                constraint = constraint,
            )
            POINT_ON_CIRCLE_CONSTRAINT ->
                createPointOnCircle(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            VECTOR_FROM_POINT_TO_POINT_CONSTRAINT ->
                createVectorFromPointToPoint(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            CIRCLE_BY_THREE_POINTS_CONSTRAINT ->
                createCircleByThreePoints(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            ANGULAR_BISECTOR_OF_THREE_POINTS_CONSTRAINT,
            LINE_ANGULAR_BISECTOR_OF_THREE_POINTS_CONSTRAINT,
            -> createAngularBisectorOfThreePoints(
                board = board,
                parsed = parsed,
                objects = objects,
                constraint = constraint,
                isLine =
                    constraint.name ==
                        LINE_ANGULAR_BISECTOR_OF_THREE_POINTS_CONSTRAINT,
            )
            INTERSECTION_POINTS_OF_TWO_CIRCLES_CONSTRAINT,
            INTERSECTION_POINTS_OF_CIRCLE_AND_LINE_CONSTRAINT,
            -> createIntersectionPoints(
                board = board,
                objects = objects,
                constraint = constraint,
            )
            OTHER_INTERSECTION_POINT_OF_TWO_CIRCLES_CONSTRAINT,
            OTHER_INTERSECTION_POINT_OF_CIRCLE_AND_LINE_CONSTRAINT,
            -> createOtherIntersectionPoint(
                board = board,
                objects = objects,
                constraint = constraint,
            )
            ANGULAR_BISECTORS_OF_TWO_LINES_CONSTRAINT,
            LINE_ANGULAR_BISECTORS_OF_TWO_LINES_CONSTRAINT,
            -> createAngularBisectorsOfTwoLines(
                board = board,
                objects = objects,
                constraint = constraint,
            )
            CIRCLE_TANGENT_LINES_BY_POINT_CONSTRAINT ->
                createCircleTangentLinesByPoint(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            POLYGON_BY_VERTICES_CONSTRAINT ->
                createPolygonByVertices(
                    board = board,
                    parsed = parsed,
                    objects = objects,
                    constraint = constraint,
                )
            LOCUS_DEFINED_BY_POINT_CONSTRAINT,
            LOCUS_DEFINED_BY_POINT_ON_LINE_CONSTRAINT,
            LOCUS_DEFINED_BY_POINT_ON_LINE_SEGMENT_CONSTRAINT,
            LOCUS_DEFINED_BY_LINE_THROUGH_POINT_CONSTRAINT,
            LOCUS_DEFINED_BY_POINT_ON_CIRCLE_CONSTRAINT,
            -> enableTrace(
                objects = objects,
                constraint = constraint,
            )
            else -> GMResult.Ok(Unit)
        }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addLineThroughTwoPoints and addLineSegmentByTwoPoints.
     */
    private fun createLineThroughTwoPoints(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
        straightFirst: Boolean,
        straightLast: Boolean,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 3)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val point1 = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2 = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line = when (
            val result = Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Line(result.error),
                )
            }
        }
        line.configureVisibleRange(straightFirst, straightLast)
        if (!straightFirst && !straightLast) {
            line.setAttribute(mapOf("strokecolor" to "black"))
        }
        objects[outputId] = line
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addLineThroughPoint.
    private fun createLineThroughPoint(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 2)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val stored = when (
            val result = storedLine(parsed, outputId)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (stored.coords.size < LINE_COORDINATE_COUNT) {
            return GMResult.Err(
                IntergeoReaderError.InvalidStoredCoordinateCount(
                    id = outputId,
                    minimum = LINE_COORDINATE_COUNT,
                    actual = stored.coords.size,
                ),
            )
        }
        val point = when (
            val result = materializePoint(
                board = board,
                parsed = parsed,
                objects = objects,
                id = constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val coefficients = IntergeoLineThroughPointCoefficients(
            storedCoordinates = stored.coords,
            point = point,
        )
        val first = when (
            val result = createLineCoefficientEndpoint(
                board = board,
                coefficients = coefficients,
                firstEndpoint = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Point(result.error),
                )
            }
        }
        val second = when (
            val result = createLineCoefficientEndpoint(
                board = board,
                coefficients = coefficients,
                firstEndpoint = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(first)
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Point(result.error),
                )
            }
        }
        val line = when (
            val result = Line.create(
                board = board,
                point1 = first,
                point2 = second,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObjects(listOf(first, second))
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Line(result.error),
                )
            }
        }
        line.setAttribute(
            mapOf(
                "strokecolor" to "black",
                "withlabel" to true,
            ),
        )
        objects[outputId] = line
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addLineParallelToLineThroughPoint.
     */
    private fun createParallelThroughPoint(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
        isRay: Boolean,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 3)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val point = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sourceLine = when (
            val result = materializeLine(
                board,
                parsed,
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line = when (
            val result = ParallelLine.create(
                board = board,
                sourceLine = sourceLine,
                throughPoint = point,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Parallel(result.error),
                )
            }
        }
        line.configureVisibleRange(
            straightFirst = !isRay,
            straightLast = true,
        )
        line.setAttribute(mapOf("withlabel" to true))
        objects[outputId] = line
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addLinePerpendicularToLineThroughPoint.
     */
    private fun createPerpendicularThroughPoint(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 3)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val point = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sourceLine = when (
            val result = materializeLine(
                board,
                parsed,
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line = when (
            val result = PerpendicularLine.create(
                board = board,
                line = sourceLine,
                point = point,
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Perpendicular(result.error),
                )
            }
        }
        line.configureVisibleRange(
            straightFirst = true,
            straightLast = true,
        )
        line.setAttribute(mapOf("withlabel" to true))
        objects[outputId] = line
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addEndpointsOfLineSegment.
     */
    private fun constrainSegmentEndpoints(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 3)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val segment = when (
            val result = materializeLine(
                board,
                parsed,
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val endpoints = listOf(segment.point1, segment.point2)
        for (index in endpoints.indices) {
            val outputId = constraint.parameters[index]
            val point = when (
                val result = materializePoint(
                    board,
                    parsed,
                    objects,
                    outputId,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val endpoint = endpoints[index]
            val dependencies = mapOf(segment.id to segment)
            val functions = listOf(
                IntergeoCoordinateFunction(dependencies, endpoint::Z),
                IntergeoCoordinateFunction(dependencies, endpoint::X),
                IntergeoCoordinateFunction(dependencies, endpoint::Y),
            )
            val coordinates = when (
                val result = point.coordinateConstraintResult(functions)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return creationFailure(
                        outputId,
                        IntergeoCreationError.Point(
                            PointError.CoordinateExpressionEvaluation(
                                result.error,
                            ),
                        ),
                    )
                }
            }
            point
                .replaceCoordinateFunctions(functions)
                .applyCoordinateConstraint(coordinates)
            setAttributes(point)
            objects[outputId] = point
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addFreeLine.
    private fun createFreeLine(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 1)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val stored = parsed.objects[outputId]
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(outputId),
            )
        if (stored !is IntergeoLineObject) {
            return GMResult.Err(
                IntergeoReaderError.StoredObjectTypeMismatch(
                    id = outputId,
                    expected = "line",
                    actual = stored.i2geoType,
                ),
            )
        }
        val line = when (
            val result = createStoredLine(
                board = board,
                stored = stored,
                explicitId = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        objects[outputId] = line
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addMidpointOfTwoPoints.
    private fun createMidpointOfTwoPoints(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 3)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val point1 = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2 = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val midpoint = when (
            val result = MidpointPoint.create(
                board = board,
                point1 = point1,
                point2 = point2,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Midpoint(result.error),
                )
            }
        }
        setAttributes(midpoint)
        objects[outputId] = midpoint
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addMidpointOfLineSegment.
    private fun createMidpointOfLineSegment(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 2)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val lineId = constraint.parameters[1]
        val line = when (
            val result = materializeLine(
                board = board,
                parsed = parsed,
                objects = objects,
                id = lineId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val midpoint = when (
            val result = MidpointPoint.create(
                board = board,
                point1 = line.point1,
                point2 = line.point2,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Midpoint(result.error),
                )
            }
        }
        setAttributes(midpoint)
        objects[outputId] = midpoint
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addPointIntersectionOfTwoLines.
     */
    private fun createPointIntersectionOfTwoLines(
        board: Board,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 3)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val first = when (
            val result = resolvedLine(
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = when (
            val result = resolvedLine(
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = IntersectionPoint.create(
                board = board,
                first = first,
                second = second,
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Intersection(result.error),
                )
            }
        }
        setAttributes(point)
        objects[outputId] = point
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addCircleByCenterAndPoint.
     */
    private fun createCircleByCenterAndPoint(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 3)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val center = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val circle = when (
            val result = Circle.create(
                board = board,
                center = center,
                point2 = point,
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Circle(result.error),
                )
            }
        }
        objects[outputId] = circle
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addCenterOfCircle.
    private fun createCenterOfCircle(
        board: Board,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 2)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val circle = when (
            val result = resolvedCircle(
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val dependencies = mapOf(circle.id to circle)
        val point = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    IntergeoCoordinateFunction(dependencies) {
                        circle.center.X()
                    },
                    IntergeoCoordinateFunction(dependencies) {
                        circle.center.Y()
                    },
                ),
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Point(result.error),
                )
            }
        }
        setAttributes(point)
        objects[outputId] = point
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addPointOnLine.
    private fun createPointOnLine(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 2)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val line = when (
            val result = materializeLine(
                board = board,
                parsed = parsed,
                objects = objects,
                id = constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = line,
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Glider(result.error),
                )
            }
        }
        setAttributes(point)
        objects[outputId] = point
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addPointOnCircle.
    private fun createPointOnCircle(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 2)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val stored = parsed.objects[outputId]
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(outputId),
            )
        if (stored !is IntergeoPointObject) {
            return GMResult.Err(
                IntergeoReaderError.StoredObjectTypeMismatch(
                    id = outputId,
                    expected = "point",
                    actual = stored.i2geoType,
                ),
            )
        }
        if (stored.coords.size < HOMOGENEOUS_POINT_COORDINATE_COUNT) {
            return GMResult.Err(
                IntergeoReaderError.InvalidStoredCoordinateCount(
                    id = outputId,
                    minimum = HOMOGENEOUS_POINT_COORDINATE_COUNT,
                    actual = stored.coords.size,
                ),
            )
        }
        val circle = when (
            val result = resolvedCircle(
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        circle.update()
        val point = when (
            val result = Glider.create(
                board = board,
                coordinates = doubleArrayOf(
                    stored.coords[1],
                    stored.coords[2],
                ),
                slideObject = circle,
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Glider(result.error),
                )
            }
        }
        setAttributes(point)
        objects[outputId] = point
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addVectorFromPointToPoint.
     */
    private fun createVectorFromPointToPoint(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 3)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val point1 = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2 = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val vector = when (
            val result = Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Line(result.error),
                )
            }
        }
        vector.configureArrow()
        setAttributes(vector)
        objects[outputId] = vector
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addCircleByThreePoints.
    private fun createCircleByThreePoints(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 4)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val points = mutableListOf<Point>()
        for (parameter in constraint.parameters.subList(1, 4)) {
            when (
                val result = materializePoint(
                    board,
                    parsed,
                    objects,
                    parameter,
                )
            ) {
                is GMResult.Ok -> points += result.value
                is GMResult.Err -> return result
            }
        }
        val circle = when (
            val result = Circle.createCircumcircle(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Circle(result.error),
                )
            }
        }
        objects[outputId] = circle
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addAngularBisectorOfThreePoints.
     */
    private fun createAngularBisectorOfThreePoints(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
        isLine: Boolean,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 4)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val points = mutableListOf<Point>()
        for (parameter in constraint.parameters.subList(1, 4)) {
            when (
                val result = materializePoint(
                    board,
                    parsed,
                    objects,
                    parameter,
                )
            ) {
                is GMResult.Ok -> points += result.value
                is GMResult.Err -> return result
            }
        }
        val bisector = when (
            val result = BisectorLine.create(
                board = board,
                point1 = points[0],
                vertex = points[1],
                point3 = points[2],
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.TriangleCenter(result.error),
                )
            }
        }
        bisector.configureVisibleRange(
            straightFirst = isLine,
            straightLast = true,
        )
        bisector.setAttribute(mapOf("strokecolor" to "#000000"))
        objects[outputId] = bisector
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addIntersectionPointsOfTwoCircles and
     * addIntersectionPointsOfCircleAndLine.
     */
    private fun createIntersectionPoints(
        board: Board,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 4)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val firstParent = when (
            val result = resolvedElement(
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val secondParent = when (
            val result = resolvedElement(
                objects,
                constraint.parameters[3],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        for (outputIndex in 0..1) {
            val outputId = constraint.parameters[outputIndex]
            val point = when (
                val result = IntersectionPoint.create(
                    board = board,
                    first = firstParent,
                    second = secondParent,
                    firstIndex =
                        IntersectionIndexSource.Number(
                            outputIndex.toDouble(),
                        ),
                    id = outputId,
                    name = outputId,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return creationFailure(
                        outputId,
                        IntergeoCreationError.Intersection(
                            result.error,
                        ),
                    )
                }
            }
            setAttributes(point)
            objects[outputId] = point
        }
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addOtherIntersectionPointOfTwoCircles and
     * addOtherIntersectionPointOfCircleAndLine.
     */
    private fun createOtherIntersectionPoint(
        board: Board,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 4)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val outputId = constraint.parameters[0]
        val excluded = when (
            val result = resolvedPoint(
                objects,
                constraint.parameters[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val firstParent = when (
            val result = resolvedElement(
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val secondParent = when (
            val result = resolvedElement(
                objects,
                constraint.parameters[3],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = OtherIntersectionPoint.create(
                board = board,
                first = firstParent,
                second = secondParent,
                excludedPoints = listOf(excluded),
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Intersection(result.error),
                )
            }
        }
        setAttributes(point)
        objects[outputId] = point
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addAngularBisectorsOfTwoLines.
     */
    private fun createAngularBisectorsOfTwoLines(
        board: Board,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 4)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val first = when (
            val result = resolvedLine(
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = when (
            val result = resolvedLine(
                objects,
                constraint.parameters[3],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val firstOutputId = constraint.parameters[0]
        val secondOutputId = constraint.parameters[1]
        val bisectors = when (
            val result = BisectorLines.create(
                board = board,
                first = first,
                second = second,
                line1Attributes = BisectorLineAttributes(
                    id = firstOutputId,
                    name = firstOutputId,
                ),
                line2Attributes = BisectorLineAttributes(
                    id = secondOutputId,
                    name = secondOutputId,
                ),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    firstOutputId,
                    IntergeoCreationError.BisectorLines(result.error),
                )
            }
        }
        val firstOutput = bisectors.line1
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(firstOutputId),
            )
        val secondOutput = bisectors.line2
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(secondOutputId),
            )
        objects[firstOutputId] = firstOutput
        objects[secondOutputId] = secondOutput
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addCircleTangentLinesByPoint.
     */
    private fun createCircleTangentLinesByPoint(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 4)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val circle = when (
            val result = resolvedElement(
                objects,
                constraint.parameters[2],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = materializePoint(
                board,
                parsed,
                objects,
                constraint.parameters[3],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val polar = when (
            val result = Tangent.createPolarLine(
                board = board,
                firstParent = circle,
                secondParent = point,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    constraint.parameters[0],
                    IntergeoCreationError.Tangent(result.error),
                )
            }
        }
        polar.setAttribute(mapOf("visible" to false))
        val intersections = mutableListOf<IntersectionPoint>()
        for (index in 0..1) {
            val intersection = when (
                val result = IntersectionPoint.create(
                    board = board,
                    first = circle,
                    second = polar,
                    firstIndex =
                        IntersectionIndexSource.Number(index.toDouble()),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return creationFailure(
                        constraint.parameters[index],
                        IntergeoCreationError.Intersection(result.error),
                    )
                }
            }
            intersection.setAttribute(mapOf("visible" to false))
            intersections += intersection
        }
        for (index in intersections.indices) {
            val outputId = constraint.parameters[index]
            val tangent = when (
                val result = Tangent.create(
                    board = board,
                    firstParent = intersections[index],
                    secondParent = circle,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return creationFailure(
                        outputId,
                        IntergeoCreationError.Tangent(result.error),
                    )
                }
            }
            objects[outputId] = tangent
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addPolygonByVertices.
    private fun createPolygonByVertices(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 1)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val vertexIds =
            constraint.nestedParameters[LIST_OF_VERTICES_TAG]
                ?: return GMResult.Err(
                    IntergeoReaderError.MissingConstraintParameterList(
                        constraintType = constraint.name,
                        listName = LIST_OF_VERTICES_TAG,
                    ),
                )
        val vertices = mutableListOf<Point>()
        for (id in vertexIds) {
            when (
                val result = materializePoint(
                    board,
                    parsed,
                    objects,
                    id,
                )
            ) {
                is GMResult.Ok -> vertices += result.value
                is GMResult.Err -> return result
            }
        }
        val outputId = constraint.parameters[0]
        val polygon = when (
            val result = Polygon.create(
                board = board,
                vertices = vertices,
                id = outputId,
                name = outputId,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    outputId,
                    IntergeoCreationError.Polygon(result.error),
                )
            }
        }
        polygon.setAttribute(mapOf("withlabel" to true))
        objects[outputId] = polygon
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * addLocusDefinedByPoint, addLocusDefinedByPointOnLine,
     * addLocusDefinedByLineThroughPoint, and
     * addLocusDefinedByPointOnCircle.
     */
    private fun enableTrace(
        objects: MutableMap<String, GeometryElement>,
        constraint: IntergeoConstraint,
    ): GMResult<Unit, IntergeoReaderError> {
        when (val result = requireParameters(constraint, 2)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val tracedId = constraint.parameters[1]
        val element = when (
            val result = resolvedElement(objects, tracedId)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        element.setAttribute(mapOf("trace" to true))
        setAttributes(element)
        objects[tracedId] = element
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> setAttributes.
    private fun setAttributes(element: GeometryElement) {
        element.setAttribute(
            mapOf(
                "strokecolor" to "blue",
                "fillcolor" to "blue",
            ),
        )
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> addPoint.
    private fun setFreePointAttributes(point: Point) {
        point.setAttribute(
            mapOf(
                "strokecolor" to "red",
                "fillcolor" to "red",
            ),
        )
    }

    private fun materializePoint(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        id: String,
    ): GMResult<Point, IntergeoReaderError> {
        val existing = objects[id]
        if (existing != null) {
            return if (existing is Point) {
                GMResult.Ok(existing)
            } else {
                GMResult.Err(
                    IntergeoReaderError.StoredObjectTypeMismatch(
                        id = id,
                        expected = "point",
                        actual = existing.elType,
                    ),
                )
            }
        }
        val stored = parsed.objects[id]
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(id),
            )
        if (stored !is IntergeoPointObject) {
            return GMResult.Err(
                IntergeoReaderError.StoredObjectTypeMismatch(
                    id = id,
                    expected = "point",
                    actual = stored.i2geoType,
                ),
            )
        }
        val point = when (
            val result = Point.create(
                board = board,
                coordinates = stored.coords.toDoubleArray(),
                name = id,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    id,
                    IntergeoCreationError.Point(result.error),
                )
            }
        }
        setFreePointAttributes(point)
        objects[id] = point
        return GMResult.Ok(point)
    }

    private fun materializeLine(
        board: Board,
        parsed: ParsedIntergeo,
        objects: MutableMap<String, GeometryElement>,
        id: String,
    ): GMResult<Line, IntergeoReaderError> {
        val existing = objects[id]
        if (existing != null) {
            return if (existing is Line) {
                GMResult.Ok(existing)
            } else {
                GMResult.Err(
                    IntergeoReaderError.StoredObjectTypeMismatch(
                        id = id,
                        expected = "line",
                        actual = existing.elType,
                    ),
                )
            }
        }
        val stored = parsed.objects[id]
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(id),
            )
        if (stored !is IntergeoLineObject) {
            return GMResult.Err(
                IntergeoReaderError.StoredObjectTypeMismatch(
                    id = id,
                    expected = "line",
                    actual = stored.i2geoType,
                ),
            )
        }
        val line = when (
            val result = createStoredLine(
                board = board,
                stored = stored,
                explicitId = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        objects[id] = line
        return GMResult.Ok(line)
    }

    private fun storedLine(
        parsed: ParsedIntergeo,
        id: String,
    ): GMResult<IntergeoLineObject, IntergeoReaderError> {
        val stored = parsed.objects[id]
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(id),
            )
        return if (stored is IntergeoLineObject) {
            GMResult.Ok(stored)
        } else {
            GMResult.Err(
                IntergeoReaderError.StoredObjectTypeMismatch(
                    id = id,
                    expected = "line",
                    actual = stored.i2geoType,
                ),
            )
        }
    }

    private fun materializeStoredObject(
        board: Board,
        stored: IntergeoStoredObject,
        explicitId: Boolean,
    ): GMResult<GeometryElement, IntergeoReaderError> =
        when (stored) {
            is IntergeoPointObject -> {
                when (
                    val result = Point.create(
                        board = board,
                        coordinates = stored.coords.toDoubleArray(),
                        id = if (explicitId) stored.id else "",
                        name = stored.id,
                    )
                ) {
                    is GMResult.Ok -> {
                        setFreePointAttributes(result.value)
                        result
                    }
                    is GMResult.Err -> creationFailure(
                        stored.id,
                        IntergeoCreationError.Point(result.error),
                    )
                }
            }
            is IntergeoLineObject ->
                createStoredLine(board, stored, explicitId)
            is IntergeoConicObject ->
                createStoredConic(board, stored, explicitId)
        }

    private fun createStoredLine(
        board: Board,
        stored: IntergeoLineObject,
        explicitId: Boolean,
    ): GMResult<Line, IntergeoReaderError> {
        if (stored.coords.size < LINE_COORDINATE_COUNT) {
            return GMResult.Err(
                IntergeoReaderError.InvalidStoredCoordinateCount(
                    id = stored.id,
                    minimum = LINE_COORDINATE_COUNT,
                    actual = stored.coords.size,
                ),
            )
        }
        return when (
            val result = Line.create(
                board = board,
                coefficients = doubleArrayOf(
                    stored.coords[2],
                    stored.coords[0],
                    stored.coords[1],
                ),
                id = if (explicitId) stored.id else "",
                name = stored.id,
            )
        ) {
            is GMResult.Ok -> {
                result.value.setAttribute(
                    mapOf("strokecolor" to "black"),
                )
                result
            }
            is GMResult.Err -> creationFailure(
                stored.id,
                IntergeoCreationError.Line(result.error),
            )
        }
    }

    private fun createStoredConic(
        board: Board,
        stored: IntergeoConicObject,
        explicitId: Boolean,
    ): GMResult<GeometryElement, IntergeoReaderError> {
        if (stored.coords.size < CONIC_MATRIX_COORDINATE_COUNT) {
            return GMResult.Err(
                IntergeoReaderError.InvalidStoredCoordinateCount(
                    id = stored.id,
                    minimum = CONIC_MATRIX_COORDINATE_COUNT,
                    actual = stored.coords.size,
                ),
            )
        }
        return when (
            val result = Conic.create(
                board = board,
                coefficients = doubleArrayOf(
                    stored.coords[0],
                    stored.coords[4],
                    stored.coords[8],
                    stored.coords[1],
                    stored.coords[5],
                    stored.coords[2],
                ),
                id = if (explicitId) stored.id else "",
                name = stored.id,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                stored.id,
                IntergeoCreationError.Conic(result.error),
            )
        }
    }

    private fun resolvedLine(
        objects: Map<String, GeometryElement>,
        id: String,
    ): GMResult<Line, IntergeoReaderError> {
        val element = objects[id]
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(id),
            )
        return if (element is Line) {
            GMResult.Ok(element)
        } else {
            GMResult.Err(
                IntergeoReaderError.StoredObjectTypeMismatch(
                    id = id,
                    expected = "line",
                    actual = element.elType,
                ),
            )
        }
    }

    private fun resolvedCircle(
        objects: Map<String, GeometryElement>,
        id: String,
    ): GMResult<Circle, IntergeoReaderError> {
        val element = objects[id]
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(id),
            )
        return if (element is Circle) {
            GMResult.Ok(element)
        } else {
            GMResult.Err(
                IntergeoReaderError.StoredObjectTypeMismatch(
                    id = id,
                    expected = "circle",
                    actual = element.elType,
                ),
            )
        }
    }

    private fun resolvedPoint(
        objects: Map<String, GeometryElement>,
        id: String,
    ): GMResult<Point, IntergeoReaderError> {
        val element = objects[id]
            ?: return GMResult.Err(
                IntergeoReaderError.MissingStoredObject(id),
            )
        return if (element is Point) {
            GMResult.Ok(element)
        } else {
            GMResult.Err(
                IntergeoReaderError.StoredObjectTypeMismatch(
                    id = id,
                    expected = "point",
                    actual = element.elType,
                ),
            )
        }
    }

    private fun resolvedElement(
        objects: Map<String, GeometryElement>,
        id: String,
    ): GMResult<GeometryElement, IntergeoReaderError> =
        objects[id]?.let { GMResult.Ok(it) }
            ?: GMResult.Err(
                IntergeoReaderError.MissingStoredObject(id),
            )

    private fun requireParameters(
        constraint: IntergeoConstraint,
        minimum: Int,
    ): GMResult<Unit, IntergeoReaderError> =
        if (constraint.parameters.size >= minimum) {
            GMResult.Ok(Unit)
        } else {
            GMResult.Err(
                IntergeoReaderError.InvalidConstraintParameterCount(
                    constraintType = constraint.name,
                    minimum = minimum,
                    actual = constraint.parameters.size,
                ),
            )
        }

    private fun <T> creationFailure(
        id: String,
        cause: IntergeoCreationError,
    ): GMResult<T, IntergeoReaderError> =
        GMResult.Err(
            IntergeoReaderError.BoardCreationFailed(id, cause),
        )

    private fun rollbackBoardRead(
        board: Board,
        existingIds: Set<String>,
        state: IntergeoBoardReadState,
    ) {
        val created = board.objects
            .filterKeys { it !in existingIds }
            .values
            .toList()
            .asReversed()
        board.removeObjects(created)
        board.setCoordinateSystem(
            originX = state.originX,
            originY = state.originY,
            unitX = state.unitX,
            unitY = state.unitY,
        )
        board.attributes.clear()
        board.attributes.putAll(state.attributes)
        board.unsuspendUpdate()
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * readDisplay and readDisplayProperty.
     */
    private fun readDisplay(
        board: Board,
        display: IntergeoDisplay,
        objects: Map<String, GeometryElement>,
    ): GMResult<Unit, IntergeoReaderError> =
        /*
         * In 1.13.3 read() passes the NodeList returned by
         * getElementsByTagName("display"). JXG.isArray(NodeList) is false,
         * so upstream readDisplay returns before readDisplayProperty.
         */
        GMResult.Ok(Unit)

    private fun parseTree(
        tree: XmlDocument,
        limits: IntergeoReaderLimits,
    ): GMResult<ParsedIntergeo, IntergeoReaderError> {
        val elements = tree.getElementsByTagName(ELEMENTS_TAG).firstOrNull()
            ?: return GMResult.Err(
                IntergeoReaderError.MissingSection(ELEMENTS_TAG),
            )
        val constraints = tree
            .getElementsByTagName(CONSTRAINTS_TAG)
            .firstOrNull()
            ?: return GMResult.Err(
                IntergeoReaderError.MissingSection(CONSTRAINTS_TAG),
            )
        val elementNodes = elements.childElements()
        if (elementNodes.size > limits.maxElements) {
            return GMResult.Err(
                IntergeoReaderError.ElementLimitExceeded(
                    limit = limits.maxElements,
                    requested = elementNodes.size,
                ),
            )
        }
        val constraintNodes = constraints.childElements()
        if (constraintNodes.size > limits.maxConstraints) {
            return GMResult.Err(
                IntergeoReaderError.ConstraintLimitExceeded(
                    limit = limits.maxConstraints,
                    requested = constraintNodes.size,
                ),
            )
        }

        val diagnostics = mutableListOf<IntergeoReaderDiagnostic>()
        val objects = linkedMapOf<String, IntergeoStoredObject>()
        for (node in elementNodes) {
            val stored = when (
                val result = readElement(node, diagnostics)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (stored != null) {
                objects[stored.id] = stored
            }
        }

        val parsedConstraints = mutableListOf<IntergeoConstraint>()
        var parameterCount = 0L
        for (node in constraintNodes) {
            val parameterGroups = when (
                val result = readConstraintParameters(node)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            parameterCount +=
                parameterGroups.parameters.size +
                    parameterGroups.nested.values.sumOf(List<String>::size)
            if (parameterCount > limits.maxParameters.toLong()) {
                return GMResult.Err(
                    IntergeoReaderError.ParameterLimitExceeded(
                        limit = limits.maxParameters,
                        requested = parameterCount,
                    ),
                )
            }
            val parameters = parameterGroups.parameters
            val supported = node.nodeName in SUPPORTED_CONSTRAINTS
            if (!supported) {
                diagnostics +=
                    IntergeoReaderDiagnostic.UnsupportedConstraint(
                        constraintType = node.nodeName,
                        firstParameter = parameters.firstOrNull(),
                    )
            }
            parsedConstraints +=
                IntergeoConstraint(
                    name = node.nodeName,
                    parameters = parameters,
                    supportedByUpstreamReader = supported,
                    nestedParameters = parameterGroups.nested,
                )
        }

        return GMResult.Ok(
            ParsedIntergeo(
                objects = objects.toMap(),
                constraints = parsedConstraints.toList(),
                diagnostics = diagnostics.toList(),
                display = IntergeoDisplay(),
            ),
        )
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> readElement.
    private fun readElement(
        node: XmlElement,
        diagnostics: MutableList<IntergeoReaderDiagnostic>,
    ): GMResult<IntergeoStoredObject?, IntergeoReaderError> =
        when (node.nodeName) {
            POINT_TAG -> storePoint(node, diagnostics)
            LINE_TAG,
            LINE_SEGMENT_TAG,
            RAY_TAG,
            VECTOR_TAG,
            -> storeLine(node, diagnostics)
            CIRCLE_TAG,
            CONIC_TAG,
            -> storeConic(node, diagnostics)
            POLYGON_TAG -> GMResult.Ok(null)
            else -> {
                diagnostics +=
                    IntergeoReaderDiagnostic.UnsupportedElement(
                        elementType = node.nodeName,
                        elementId = node.getAttribute(ID_ATTRIBUTE),
                    )
                GMResult.Ok(null)
            }
        }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> storePoint.
    private fun storePoint(
        node: XmlElement,
        diagnostics: MutableList<IntergeoReaderDiagnostic>,
    ): GMResult<IntergeoStoredObject?, IntergeoReaderError> {
        val id = when (val result = readId(node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val coordinateNode = node.childElements().firstOrNull()
            ?: return GMResult.Err(
                IntergeoReaderError.MissingElementContent(
                    elementType = node.nodeName,
                    elementId = id,
                ),
            )

        val parents = when (coordinateNode.nodeName) {
            HOMOGENEOUS_COORDINATES_TAG -> {
                val values = mutableListOf<Double>()
                for ((index, component) in
                    coordinateNode.childElements().withIndex()
                ) {
                    when (component.nodeName) {
                        DOUBLE_TAG -> {
                            val value = when (
                                val result = readText(
                                    parent = coordinateNode,
                                    child = component,
                                    childIndex = index,
                                )
                            ) {
                                is GMResult.Ok -> result.value
                                is GMResult.Err -> return result
                            }
                            values += jsParseFloat(value)
                        }
                        COMPLEX_TAG -> {
                            for ((complexIndex, child) in
                                component.childElements().withIndex()
                            ) {
                                if (child.nodeName == DOUBLE_TAG) {
                                    val value = when (
                                        val result = readText(
                                            parent = component,
                                            child = child,
                                            childIndex = complexIndex,
                                        )
                                    ) {
                                        is GMResult.Ok -> result.value
                                        is GMResult.Err -> return result
                                    }
                                    values += jsParseFloat(value)
                                }
                            }
                        }
                        else -> {
                            diagnostics +=
                                IntergeoReaderDiagnostic
                                    .UnsupportedHomogeneousComponent(
                                        elementId = id,
                                        componentType = component.nodeName,
                                    )
                            return GMResult.Ok(null)
                        }
                    }
                }
                when {
                    values.size == 3 ->
                        listOf(values[2], values[0], values[1])
                    values.size == 6 &&
                        abs(values[1]) < COMPLEX_REAL_EPSILON &&
                        abs(values[3]) < COMPLEX_REAL_EPSILON &&
                        abs(values[5]) < COMPLEX_REAL_EPSILON ->
                        listOf(values[4], values[0], values[2])
                    else -> {
                        diagnostics +=
                            IntergeoReaderDiagnostic
                                .UnsupportedPointCoordinates(
                                    elementId = id,
                                    valueCount = values.size,
                                )
                        return GMResult.Ok(null)
                    }
                }
            }
            EUCLIDEAN_COORDINATES_TAG,
            EUCLIDIAN_COORDINATES_TAG,
            -> {
                val values = when (
                    val result = readDoubleChildren(coordinateNode)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (values.size < 2) {
                    return GMResult.Err(
                        IntergeoReaderError.MissingElementContent(
                            elementType = coordinateNode.nodeName,
                            elementId = id,
                        ),
                    )
                }
                listOf(values[0], values[1])
            }
            POLAR_COORDINATES_TAG -> {
                val values = when (
                    val result = readDoubleChildren(coordinateNode)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (values.size < 2) {
                    return GMResult.Err(
                        IntergeoReaderError.MissingElementContent(
                            elementType = coordinateNode.nodeName,
                            elementId = id,
                        ),
                    )
                }
                listOf(
                    values[0] * cos(values[1]),
                    values[0] * sin(values[1]),
                )
            }
            else -> {
                diagnostics +=
                    IntergeoReaderDiagnostic.UnsupportedCoordinateType(
                        elementType = node.nodeName,
                        elementId = id,
                        coordinateType = coordinateNode.nodeName,
                    )
                return GMResult.Ok(null)
            }
        }
        return GMResult.Ok(IntergeoPointObject(id, parents))
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> storeLine.
    private fun storeLine(
        node: XmlElement,
        diagnostics: MutableList<IntergeoReaderDiagnostic>,
    ): GMResult<IntergeoStoredObject?, IntergeoReaderError> {
        val id = when (val result = readId(node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val coordinateNode = node.childElements().firstOrNull()
            ?: return GMResult.Err(
                IntergeoReaderError.MissingElementContent(
                    elementType = node.nodeName,
                    elementId = id,
                ),
            )
        if (coordinateNode.nodeName != HOMOGENEOUS_COORDINATES_TAG) {
            diagnostics +=
                IntergeoReaderDiagnostic.UnsupportedCoordinateType(
                    elementType = node.nodeName,
                    elementId = id,
                    coordinateType = coordinateNode.nodeName,
                )
            return GMResult.Ok(null)
        }
        val coords = when (val result = readNamedDoubleChildren(coordinateNode)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(IntergeoLineObject(id, coords))
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> storeConic.
    private fun storeConic(
        node: XmlElement,
        diagnostics: MutableList<IntergeoReaderDiagnostic>,
    ): GMResult<IntergeoStoredObject?, IntergeoReaderError> {
        val id = when (val result = readId(node)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val coordinateNode = node.childElements().firstOrNull()
            ?: return GMResult.Err(
                IntergeoReaderError.MissingElementContent(
                    elementType = node.nodeName,
                    elementId = id,
                ),
            )
        if (coordinateNode.nodeName != MATRIX_TAG) {
            diagnostics +=
                IntergeoReaderDiagnostic.UnsupportedCoordinateType(
                    elementType = node.nodeName,
                    elementId = id,
                    coordinateType = coordinateNode.nodeName,
                )
            return GMResult.Ok(null)
        }
        val coords = when (val result = readNamedDoubleChildren(coordinateNode)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(IntergeoConicObject(id, coords))
    }

    // JSXGraph 1.13.3: src/reader/intergeo.js -> readParams.
    private fun readParams(
        node: XmlElement,
    ): GMResult<List<String>, IntergeoReaderError> {
        val parameters = mutableListOf<String>()
        for ((index, child) in node.childElements().withIndex()) {
            val value = when (
                val result = readText(
                    parent = node,
                    child = child,
                    childIndex = index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            parameters += value
        }
        return GMResult.Ok(parameters)
    }

    private data class IntergeoConstraintParameters(
        val parameters: List<String>,
        val nested: Map<String, List<String>>,
    )

    private fun readConstraintParameters(
        node: XmlElement,
    ): GMResult<IntergeoConstraintParameters, IntergeoReaderError> {
        val parameters = mutableListOf<String>()
        val nested = linkedMapOf<String, List<String>>()
        for ((index, child) in node.childElements().withIndex()) {
            if (child.childElements().isEmpty()) {
                val value = when (
                    val result = readText(
                        parent = node,
                        child = child,
                        childIndex = index,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                parameters += value
            } else {
                val values = when (val result = readParams(child)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                nested[child.nodeName] = values
            }
        }
        return GMResult.Ok(
            IntergeoConstraintParameters(
                parameters = parameters,
                nested = nested,
            ),
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * readDisplay and readDisplayProperty.
     */
    private fun parseDisplay(
        tree: XmlDocument,
        diagnostics: MutableList<IntergeoReaderDiagnostic>,
    ): GMResult<IntergeoDisplay, IntergeoReaderError> {
        val displayNode =
            tree.getElementsByTagName(DISPLAY_TAG).firstOrNull()
                ?: return GMResult.Ok(IntergeoDisplay())
        var backgroundColor: String? = null
        val styles = mutableListOf<IntergeoDisplayStyle>()
        for ((index, node) in displayNode.childElements().withIndex()) {
            when (node.nodeName) {
                BACKGROUND_COLOR_TAG -> {
                    backgroundColor = when (
                        val result = readText(
                            parent = displayNode,
                            child = node,
                            childIndex = index,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }
                STYLE_TAG -> {
                    val reference =
                        node.getAttribute(REFERENCE_ATTRIBUTE).orEmpty()
                    val attributes = linkedMapOf<String, Any?>()
                    for ((propertyIndex, property) in
                        node.childElements().withIndex()
                    ) {
                        val value = when (
                            val result = readText(
                                parent = node,
                                child = property,
                                childIndex = propertyIndex,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        val translated = translateDisplayProperty(
                            key = property.nodeName,
                            value = value,
                        )
                        attributes[translated.first] = translated.second
                    }
                    styles += IntergeoDisplayStyle(
                        reference = reference,
                        attributes = attributes,
                    )
                }
                else -> diagnostics +=
                    IntergeoReaderDiagnostic.UnsupportedDisplayProperty(
                        propertyType = node.nodeName,
                    )
            }
        }
        return GMResult.Ok(
            IntergeoDisplay(
                backgroundColor = backgroundColor,
                styles = styles,
            ),
        )
    }

    private fun translateDisplayProperty(
        key: String,
        value: String,
    ): Pair<String, Any?> =
        when (key) {
            STROKE_TAG -> "strokeColor" to value
            STROKE_WIDTH_TAG,
            BORDER_WIDTH_TAG,
            -> "strokeWidth" to jsParseFloat(value)
            FILL_TAG -> "fillColor" to value
            FILL_OPACITY_TAG -> "fillOpacity" to jsParseFloat(value)
            BORDER_OPACITY_TAG -> "strokeOpacity" to jsParseFloat(value)
            POINT_SIZE_TAG -> "size" to jsParseFloat(value)
            LABEL_TAG -> "name" to value
            POINT_STYLE_TAG -> "face" to
                when (value) {
                    "circle",
                    "point",
                    -> "o"
                    "cross" -> "+"
                    "x-mark" -> "x"
                    "square" -> "[]"
                    "triangle" -> "triangleup"
                    else -> value
                }
            else -> key to value
        }

    private fun readId(
        node: XmlElement,
    ): GMResult<String, IntergeoReaderError> {
        val id = node.getAttribute(ID_ATTRIBUTE)
            ?: return GMResult.Err(
                IntergeoReaderError.MissingElementId(node.nodeName),
            )
        return GMResult.Ok(id)
    }

    private fun readDoubleChildren(
        node: XmlElement,
    ): GMResult<List<Double>, IntergeoReaderError> {
        val values = mutableListOf<Double>()
        for ((index, child) in node.childElements().withIndex()) {
            val value = when (
                val result = readText(
                    parent = node,
                    child = child,
                    childIndex = index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            values += jsParseFloat(value)
        }
        return GMResult.Ok(values)
    }

    private fun readNamedDoubleChildren(
        node: XmlElement,
    ): GMResult<List<Double>, IntergeoReaderError> {
        val values = mutableListOf<Double>()
        for ((index, child) in node.childElements().withIndex()) {
            if (child.nodeName == DOUBLE_TAG) {
                val value = when (
                    val result = readText(
                        parent = node,
                        child = child,
                        childIndex = index,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                values += jsParseFloat(value)
            }
        }
        return GMResult.Ok(values)
    }

    private fun readText(
        parent: XmlElement,
        child: XmlElement,
        childIndex: Int,
    ): GMResult<String, IntergeoReaderError> {
        val value = child.firstChild?.data
            ?: return GMResult.Err(
                IntergeoReaderError.MissingTextValue(
                    parentType = parent.nodeName,
                    childType = child.nodeName,
                    childIndex = childIndex,
                ),
            )
        return GMResult.Ok(value)
    }

    private fun validateLimits(
        limits: IntergeoReaderLimits,
    ): IntergeoReaderError.InvalidLimits? {
        val invalid = when {
            limits.maxElements < 0 ->
                "maxElements" to limits.maxElements
            limits.maxConstraints < 0 ->
                "maxConstraints" to limits.maxConstraints
            limits.maxParameters < 0 ->
                "maxParameters" to limits.maxParameters
            else -> null
        }
        return invalid?.let { (name, value) ->
            IntergeoReaderError.InvalidLimits(name, value)
        }
    }

    private fun XmlElement.childElements(): List<XmlElement> =
        childNodes.filterIsInstance<XmlElement>()

    private fun jsParseFloat(value: String): Double {
        val prefix = FLOAT_PREFIX.find(value.trimStart())?.value
            ?: return Double.NaN
        return when (prefix) {
            "Infinity",
            "+Infinity",
            -> Double.POSITIVE_INFINITY
            "-Infinity" -> Double.NEGATIVE_INFINITY
            else -> prefix.toDoubleOrNull() ?: Double.NaN
        }
    }

    private companion object {
        const val ELEMENTS_TAG = "elements"
        const val CONSTRAINTS_TAG = "constraints"
        const val DISPLAY_TAG = "display"
        const val ID_ATTRIBUTE = "id"
        const val REFERENCE_ATTRIBUTE = "ref"
        const val POINT_TAG = "point"
        const val LINE_TAG = "line"
        const val LINE_SEGMENT_TAG = "line_segment"
        const val RAY_TAG = "ray"
        const val VECTOR_TAG = "vector"
        const val CIRCLE_TAG = "circle"
        const val CONIC_TAG = "conic"
        const val POLYGON_TAG = "polygon"
        const val HOMOGENEOUS_COORDINATES_TAG = "homogeneous_coordinates"
        const val EUCLIDEAN_COORDINATES_TAG = "euclidean_coordinates"
        const val EUCLIDIAN_COORDINATES_TAG = "euclidian_coordinates"
        const val POLAR_COORDINATES_TAG = "polar_coordinates"
        const val MATRIX_TAG = "matrix"
        const val DOUBLE_TAG = "double"
        const val COMPLEX_TAG = "complex"
        const val LIST_OF_VERTICES_TAG = "list_of_vertices"
        const val BACKGROUND_COLOR_TAG = "background-color"
        const val STYLE_TAG = "style"
        const val STROKE_TAG = "stroke"
        const val STROKE_WIDTH_TAG = "stroke-width"
        const val BORDER_WIDTH_TAG = "border-width"
        const val FILL_TAG = "fill"
        const val FILL_OPACITY_TAG = "fill-opacity"
        const val BORDER_OPACITY_TAG = "border-opacity"
        const val POINT_SIZE_TAG = "point-size"
        const val LABEL_TAG = "label"
        const val POINT_STYLE_TAG = "point-style"
        const val CONTAINER_BACKGROUND_COLOR_ATTRIBUTE =
            "containerbackgroundcolor"
        const val COMPLEX_REAL_EPSILON = 1e-10
        const val LINE_COORDINATE_COUNT = 3
        const val CONIC_MATRIX_COORDINATE_COUNT = 9
        const val HOMOGENEOUS_POINT_COORDINATE_COUNT = 3
        const val INTERGEO_ORIGIN_X = 400.0
        const val INTERGEO_ORIGIN_Y = 300.0
        const val INTERGEO_UNIT_X = 30.0
        const val INTERGEO_UNIT_Y = 30.0

        const val LINE_THROUGH_TWO_POINTS_CONSTRAINT =
            "line_through_two_points"
        const val RAY_FROM_POINT_THROUGH_POINT_CONSTRAINT =
            "ray_from_point_through_point"
        const val LINE_SEGMENT_BY_POINTS_CONSTRAINT =
            "line_segment_by_points"
        const val LINE_THROUGH_POINT_CONSTRAINT = "line_through_point"
        const val LINE_PARALLEL_TO_LINE_THROUGH_POINT_CONSTRAINT =
            "line_parallel_to_line_through_point"
        const val RAY_FROM_POINT_AND_VECTOR_CONSTRAINT =
            "ray_from_point_and_vector"
        const val LINE_PERPENDICULAR_TO_LINE_THROUGH_POINT_CONSTRAINT =
            "line_perpendicular_to_line_through_point"
        const val ENDPOINTS_OF_LINE_SEGMENT_CONSTRAINT =
            "endpoints_of_line_segment"
        const val FREE_LINE_CONSTRAINT = "free_line"
        const val MIDPOINT_OF_TWO_POINTS_CONSTRAINT =
            "midpoint_of_two_points"
        const val MIDPOINT_CONSTRAINT = "midpoint"
        const val MIDPOINT_OF_LINE_SEGMENT_CONSTRAINT =
            "midpoint_of_line_segment"
        const val MIDPOINT_LINE_SEGMENT_CONSTRAINT =
            "midpoint_line_segment"
        const val POINT_INTERSECTION_OF_TWO_LINES_CONSTRAINT =
            "point_intersection_of_two_lines"
        const val CIRCLE_BY_CENTER_AND_POINT_CONSTRAINT =
            "circle_by_center_and_point"
        const val CENTER_OF_CIRCLE_CONSTRAINT = "center_of_circle"
        const val POINT_ON_LINE_CONSTRAINT = "point_on_line"
        const val POINT_ON_LINE_SEGMENT_CONSTRAINT =
            "point_on_line_segment"
        const val POINT_ON_CIRCLE_CONSTRAINT = "point_on_circle"
        const val VECTOR_FROM_POINT_TO_POINT_CONSTRAINT =
            "vector_from_point_to_point"
        const val CIRCLE_BY_THREE_POINTS_CONSTRAINT =
            "circle_by_three_points"
        const val ANGULAR_BISECTOR_OF_THREE_POINTS_CONSTRAINT =
            "angular_bisector_of_three_points"
        const val LINE_ANGULAR_BISECTOR_OF_THREE_POINTS_CONSTRAINT =
            "line_angular_bisector_of_three_points"
        const val INTERSECTION_POINTS_OF_TWO_CIRCLES_CONSTRAINT =
            "intersection_points_of_two_circles"
        const val INTERSECTION_POINTS_OF_CIRCLE_AND_LINE_CONSTRAINT =
            "intersection_points_of_circle_and_line"
        const val OTHER_INTERSECTION_POINT_OF_TWO_CIRCLES_CONSTRAINT =
            "other_intersection_point_of_two_circles"
        const val OTHER_INTERSECTION_POINT_OF_CIRCLE_AND_LINE_CONSTRAINT =
            "other_intersection_point_of_circle_and_line"
        const val ANGULAR_BISECTORS_OF_TWO_LINES_CONSTRAINT =
            "angular_bisectors_of_two_lines"
        const val LINE_ANGULAR_BISECTORS_OF_TWO_LINES_CONSTRAINT =
            "line_angular_bisectors_of_two_lines"
        const val CIRCLE_TANGENT_LINES_BY_POINT_CONSTRAINT =
            "circle_tangent_lines_by_point"
        const val POLYGON_BY_VERTICES_CONSTRAINT = "polygon_by_vertices"
        const val LOCUS_DEFINED_BY_POINT_CONSTRAINT =
            "locus_defined_by_point"
        const val LOCUS_DEFINED_BY_POINT_ON_LINE_CONSTRAINT =
            "locus_defined_by_point_on_line"
        const val LOCUS_DEFINED_BY_POINT_ON_LINE_SEGMENT_CONSTRAINT =
            "locus_defined_by_point_on_line_segment"
        const val LOCUS_DEFINED_BY_LINE_THROUGH_POINT_CONSTRAINT =
            "locus_defined_by_line_through_point"
        const val LOCUS_DEFINED_BY_POINT_ON_CIRCLE_CONSTRAINT =
            "locus_defined_by_point_on_circle"

        val FLOAT_PREFIX =
            Regex(
                "^[+-]?(?:Infinity|" +
                    "(?:(?:\\d+\\.?\\d*|\\.\\d+)" +
                    "(?:[eE][+-]?\\d+)?))",
            )

        val SUPPORTED_CONSTRAINTS = setOf(
            "line_through_two_points",
            "ray_from_point_through_point",
            "line_through_point",
            "line_parallel_to_line_through_point",
            "ray_from_point_and_vector",
            "line_perpendicular_to_line_through_point",
            "line_segment_by_points",
            "vector_from_point_to_point",
            "endpoints_of_line_segment",
            "free_line",
            "point_on_line",
            "point_on_line_segment",
            "point_on_circle",
            "angular_bisector_of_three_points",
            "angular_bisectors_of_two_lines",
            "line_angular_bisector_of_three_points",
            "line_angular_bisectors_of_two_lines",
            "midpoint_of_two_points",
            "midpoint",
            "midpoint_of_line_segment",
            "midpoint_line_segment",
            "point_intersection_of_two_lines",
            "locus_defined_by_point",
            "locus_defined_by_point_on_line",
            "locus_defined_by_point_on_line_segment",
            "locus_defined_by_line_through_point",
            "locus_defined_by_point_on_circle",
            "circle_by_three_points",
            "circle_by_center_and_point",
            "center_of_circle",
            "intersection_points_of_two_circles",
            "intersection_points_of_circle_and_line",
            "other_intersection_point_of_two_circles",
            "other_intersection_point_of_circle_and_line",
            "circle_tangent_lines_by_point",
            "polygon_by_vertices",
        )

        val BOARD_CONSTRAINTS = setOf(
            LINE_THROUGH_TWO_POINTS_CONSTRAINT,
            RAY_FROM_POINT_THROUGH_POINT_CONSTRAINT,
            LINE_SEGMENT_BY_POINTS_CONSTRAINT,
            LINE_THROUGH_POINT_CONSTRAINT,
            LINE_PARALLEL_TO_LINE_THROUGH_POINT_CONSTRAINT,
            RAY_FROM_POINT_AND_VECTOR_CONSTRAINT,
            LINE_PERPENDICULAR_TO_LINE_THROUGH_POINT_CONSTRAINT,
            ENDPOINTS_OF_LINE_SEGMENT_CONSTRAINT,
            FREE_LINE_CONSTRAINT,
            MIDPOINT_OF_TWO_POINTS_CONSTRAINT,
            MIDPOINT_CONSTRAINT,
            MIDPOINT_OF_LINE_SEGMENT_CONSTRAINT,
            MIDPOINT_LINE_SEGMENT_CONSTRAINT,
            POINT_INTERSECTION_OF_TWO_LINES_CONSTRAINT,
            CIRCLE_BY_CENTER_AND_POINT_CONSTRAINT,
            POINT_ON_LINE_CONSTRAINT,
            POINT_ON_LINE_SEGMENT_CONSTRAINT,
            POINT_ON_CIRCLE_CONSTRAINT,
            VECTOR_FROM_POINT_TO_POINT_CONSTRAINT,
            CIRCLE_BY_THREE_POINTS_CONSTRAINT,
            ANGULAR_BISECTOR_OF_THREE_POINTS_CONSTRAINT,
            LINE_ANGULAR_BISECTOR_OF_THREE_POINTS_CONSTRAINT,
            INTERSECTION_POINTS_OF_TWO_CIRCLES_CONSTRAINT,
            INTERSECTION_POINTS_OF_CIRCLE_AND_LINE_CONSTRAINT,
            OTHER_INTERSECTION_POINT_OF_TWO_CIRCLES_CONSTRAINT,
            OTHER_INTERSECTION_POINT_OF_CIRCLE_AND_LINE_CONSTRAINT,
            ANGULAR_BISECTORS_OF_TWO_LINES_CONSTRAINT,
            LINE_ANGULAR_BISECTORS_OF_TWO_LINES_CONSTRAINT,
            CIRCLE_TANGENT_LINES_BY_POINT_CONSTRAINT,
            POLYGON_BY_VERTICES_CONSTRAINT,
            CENTER_OF_CIRCLE_CONSTRAINT,
            LOCUS_DEFINED_BY_POINT_CONSTRAINT,
            LOCUS_DEFINED_BY_POINT_ON_LINE_CONSTRAINT,
            LOCUS_DEFINED_BY_POINT_ON_LINE_SEGMENT_CONSTRAINT,
            LOCUS_DEFINED_BY_LINE_THROUGH_POINT_CONSTRAINT,
            LOCUS_DEFINED_BY_POINT_ON_CIRCLE_CONSTRAINT,
        )
    }
}

private class IntergeoLineThroughPointCoefficients(
    private val storedCoordinates: List<Double>,
    private val point: Point,
) {
    val dependencies: Map<String, GeometryElement> =
        mapOf(point.id to point)

    fun evaluate(): DoubleArray {
        val xCoefficient = storedCoordinates[0]
        val yCoefficient = storedCoordinates[1]
        val storedConstant = storedCoordinates[2]
        return doubleArrayOf(
            storedConstant -
                xCoefficient * point.X() -
                yCoefficient * point.Y() -
                storedConstant * point.Z(),
            xCoefficient,
            yCoefficient,
        )
    }
}

private fun createLineCoefficientEndpoint(
    board: Board,
    coefficients: IntergeoLineThroughPointCoefficients,
    firstEndpoint: Boolean,
): GMResult<Point, PointError> =
    Point.createConstrained(
        board = board,
        coordinateFunctions = listOf(
            IntergeoLineEndpointFunction(
                coefficients = coefficients,
                firstEndpoint = firstEndpoint,
                coordinateIndex = 0,
            ),
            IntergeoLineEndpointFunction(
                coefficients = coefficients,
                firstEndpoint = firstEndpoint,
                coordinateIndex = 1,
            ),
            IntergeoLineEndpointFunction(
                coefficients = coefficients,
                firstEndpoint = firstEndpoint,
                coordinateIndex = 2,
            ),
        ),
        name = "",
    )

private class IntergeoLineEndpointFunction(
    private val coefficients: IntergeoLineThroughPointCoefficients,
    private val firstEndpoint: Boolean,
    private val coordinateIndex: Int,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> =
        coefficients.dependencies

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val values = coefficients.evaluate()
        val constant = values[0]
        val xCoefficient = values[1]
        val yCoefficient = values[2]
        val homogeneous =
            yCoefficient * yCoefficient +
                xCoefficient * xCoefficient
        val coordinates =
            if (firstEndpoint) {
                doubleArrayOf(
                    homogeneous,
                    yCoefficient -
                        xCoefficient * constant +
                        yCoefficient,
                    -xCoefficient -
                        yCoefficient * constant -
                        xCoefficient,
                )
            } else {
                doubleArrayOf(
                    homogeneous,
                    -xCoefficient * constant + yCoefficient,
                    -yCoefficient * constant - xCoefficient,
                )
            }
        return GMResult.Ok(
            JessieCodeRuntimeValue.NumberValue(
                coordinates[coordinateIndex],
            ),
        )
    }
}

private class IntergeoCoordinateFunction(
    override val dependencies: Map<String, GeometryElement>,
    private val value: () -> Double,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value()))
}

internal object IntergeoReaderFactory :
    JsxGraphReaderFactory<Board> {
    override fun create(
        board: Board,
        source: String,
    ): GMResult<JsxGraphReader, ReaderError> =
        GMResult.Ok(
            JsxGraphReader {
                when (
                    val result = IntergeoReader(source).read(
                        board = board,
                        failOnUnsupportedBoardConstraints = true,
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(Unit)
                    is GMResult.Err -> {
                        GMResult.Err(
                            ReaderError.DomainFailure(result.error),
                        )
                    }
                }
            },
        )
}

internal fun ReaderRegistry<Board>.registerIntergeoReader() {
    registerReader(
        reader = IntergeoReaderFactory,
        extensions = listOf("i2g", "xml", "intergeo"),
    )
}
