/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/cinderella.js -> CinderellaReader.read
 * (FreePoint, Join, Segment, CircleMP, CircleByFixedRadius,
 * CircleByRadius, PointOnCircle, PointOnLine, Mid, CircleBy3,
 * Parallel, Orthogonal, ConicBy5, ConicFoci, ConicFociH,
 * ConicParabolaPL, Poly, Arc, Through, Compass, AngularBisector,
 * Meet, IntersectionConicLine, IntersectionCircleCircle, setOriginX,
 * setOriginY, and setScale branches).
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.ArcError
import com.swithun.jsxgraph.core.base.BisectorLineAttributes
import com.swithun.jsxgraph.core.base.BisectorLines
import com.swithun.jsxgraph.core.base.BisectorLinesError
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.Conic
import com.swithun.jsxgraph.core.base.ConicError
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Ellipse
import com.swithun.jsxgraph.core.base.EllipseError
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.GliderError
import com.swithun.jsxgraph.core.base.Hyperbola
import com.swithun.jsxgraph.core.base.HyperbolaError
import com.swithun.jsxgraph.core.base.IntersectionError
import com.swithun.jsxgraph.core.base.IntersectionIndexSource
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.MidpointError
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.Normal
import com.swithun.jsxgraph.core.base.NormalError
import com.swithun.jsxgraph.core.base.ParallelConstructionError
import com.swithun.jsxgraph.core.base.ParallelLine
import com.swithun.jsxgraph.core.base.Parabola
import com.swithun.jsxgraph.core.base.ParabolaError
import com.swithun.jsxgraph.core.base.OtherIntersectionPoint
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.PolygonError
import kotlin.math.sqrt

internal data class CinderellaReaderLimits(
    val preparation: ReaderPreparationLimits = ReaderPreparationLimits(),
    val properties: CinderellaPropertyLimits =
        CinderellaPropertyLimits(),
    val maxLines: Int = 1_000_000,
    val maxElements: Int = 100_000,
)

internal sealed interface CinderellaReaderError : ReaderDomainError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : CinderellaReaderError

    data class PreparationFailed(
        val cause: ReaderPreparationError,
    ) : CinderellaReaderError

    data class LineLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : CinderellaReaderError

    data class ElementLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : CinderellaReaderError

    data class MissingObjectName(
        val lineIndex: Int,
        val line: String,
    ) : CinderellaReaderError

    data class MalformedDefinition(
        val lineIndex: Int,
        val kind: String,
        val line: String,
    ) : CinderellaReaderError

    data class PropertyParsingFailed(
        val lineIndex: Int,
        val cause: CinderellaPropertyError,
    ) : CinderellaReaderError

    data class UnsupportedDefinitions(
        val lineIndices: List<Int>,
    ) : CinderellaReaderError

    data object MissingScale : CinderellaReaderError

    data class InvalidScale(
        val value: Double,
    ) : CinderellaReaderError

    data class MissingReference(
        val definitionName: String,
        val referenceName: String,
        val expectedType: String,
    ) : CinderellaReaderError

    data class ReferenceTypeMismatch(
        val definitionName: String,
        val referenceName: String,
        val expectedType: String,
        val actualType: String,
    ) : CinderellaReaderError

    data class BoardCreationFailed(
        val definitionName: String,
        val cause: CinderellaCreationError,
    ) : CinderellaReaderError
}

internal sealed interface CinderellaCreationError {
    data class Point(val cause: PointError) : CinderellaCreationError

    data class Line(val cause: LineError) : CinderellaCreationError

    data class Circle(val cause: CircleError) : CinderellaCreationError

    data class Glider(val cause: GliderError) : CinderellaCreationError

    data class Midpoint(val cause: MidpointError) : CinderellaCreationError

    data class Parallel(
        val cause: ParallelConstructionError,
    ) : CinderellaCreationError

    data class Normal(val cause: NormalError) : CinderellaCreationError

    data class Conic(val cause: ConicError) : CinderellaCreationError

    data class Ellipse(val cause: EllipseError) : CinderellaCreationError

    data class Hyperbola(val cause: HyperbolaError) :
        CinderellaCreationError

    data class Parabola(val cause: ParabolaError) :
        CinderellaCreationError

    data class Polygon(val cause: PolygonError) : CinderellaCreationError

    data class Arc(val cause: ArcError) : CinderellaCreationError

    data class BisectorLines(val cause: BisectorLinesError) :
        CinderellaCreationError

    data class Intersection(val cause: IntersectionError) :
        CinderellaCreationError

    data class MissingCompositeOutput(
        val role: String,
    ) : CinderellaCreationError

    data object UpstreamCompassRadiusFunction :
        CinderellaCreationError
}

internal sealed interface CinderellaDefinition {
    val name: String
    val sourceLine: Int
    val outputNames: List<String>
        get() = listOf(name)
}

internal data class CinderellaFreePointDefinition(
    override val name: String,
    val coordinates: List<Double>,
    val properties: CinderellaPointProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaLineDefinition(
    override val name: String,
    val pointNames: List<String>,
    val segment: Boolean,
    val properties: CinderellaLineProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaCircleByPointsDefinition(
    override val name: String,
    val centerName: String,
    val pointName: String,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaCircleByRadiusDefinition(
    override val name: String,
    val centerName: String,
    val radius: Double,
    val fixedRadius: Boolean,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaPointOnCircleDefinition(
    override val name: String,
    val circleName: String,
    val centerOffset: List<Double>,
    val properties: CinderellaPointProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaPointOnLineDefinition(
    override val name: String,
    val lineName: String,
    val coordinates: List<Double>,
    val properties: CinderellaPointProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaMidpointDefinition(
    override val name: String,
    val pointNames: List<String>,
    val properties: CinderellaPointProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaCircumcircleDefinition(
    override val name: String,
    val pointNames: List<String>,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaParallelDefinition(
    override val name: String,
    val lineName: String,
    val pointName: String,
    val properties: CinderellaLineProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaOrthogonalDefinition(
    override val name: String,
    val lineName: String,
    val pointName: String,
    val properties: CinderellaLineProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaConicByFivePointsDefinition(
    override val name: String,
    val pointNames: List<String>,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaConicFociDefinition(
    override val name: String,
    val pointNames: List<String>,
    val hyperbola: Boolean,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaParabolaDefinition(
    override val name: String,
    val focusName: String,
    val directrixName: String,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaPolygonDefinition(
    override val name: String,
    val pointNames: List<String>,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaArcDefinition(
    override val name: String,
    val pointNames: List<String>,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaThroughDefinition(
    override val name: String,
    val pointName: String,
    val offset: List<Double>,
    val properties: CinderellaLineProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaCompassDefinition(
    override val name: String,
    val pointNames: List<String>,
    val properties: CinderellaCircleProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal data class CinderellaAngularBisectorDefinition(
    val firstOutputName: String?,
    val secondOutputName: String?,
    val lineNames: List<String>,
    val firstProperties: CinderellaLineProperties,
    val secondProperties: CinderellaLineProperties?,
    override val sourceLine: Int,
) : CinderellaDefinition {
    override val name: String =
        firstOutputName ?: secondOutputName.orEmpty()
    override val outputNames: List<String> =
        listOfNotNull(firstOutputName, secondOutputName)
}

internal data class CinderellaMeetDefinition(
    override val name: String,
    val lineNames: List<String>,
    val properties: CinderellaPointProperties,
    override val sourceLine: Int,
) : CinderellaDefinition

internal enum class CinderellaIntersectionKind {
    CONIC_LINE,
    CIRCLE_CIRCLE,
}

internal data class CinderellaPairIntersectionDefinition(
    val firstOutputName: String?,
    val secondOutputName: String?,
    val parentNames: List<String>,
    val kind: CinderellaIntersectionKind,
    val firstProperties: CinderellaPointProperties,
    val secondProperties: CinderellaPointProperties?,
    override val sourceLine: Int,
) : CinderellaDefinition {
    override val name: String =
        firstOutputName ?: secondOutputName.orEmpty()
    override val outputNames: List<String> =
        listOfNotNull(firstOutputName, secondOutputName)
}

internal sealed interface CinderellaReaderDiagnostic {
    data class UnsupportedDefinition(
        val lineIndex: Int,
        val line: String,
    ) : CinderellaReaderDiagnostic
}

internal data class ParsedCinderella(
    val definitions: List<CinderellaDefinition>,
    val originX: String?,
    val originY: String?,
    val scale: Double?,
    val diagnostics: List<CinderellaReaderDiagnostic>,
)

internal data class DrawnCinderella(
    val parsed: ParsedCinderella,
    val objects: Map<String, GeometryElement>,
)

internal class CinderellaReader(
    private val data: String,
) {
    // JSXGraph 1.13.3: src/reader/cinderella.js -> read.
    internal fun parse(
        isString: Boolean = false,
        limits: CinderellaReaderLimits = CinderellaReaderLimits(),
    ): GMResult<ParsedCinderella, CinderellaReaderError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val prepared = when (
            val result = ReaderPreparation.prepareCinderella(
                source = data,
                isString = isString,
                limits = limits.preparation,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    CinderellaReaderError.PreparationFailed(result.error),
                )
            }
        }
        val lines = prepared.split('\n')
        if (lines.size > limits.maxLines) {
            return GMResult.Err(
                CinderellaReaderError.LineLimitExceeded(
                    limit = limits.maxLines,
                    requested = lines.size,
                ),
            )
        }

        val definitions = mutableListOf<CinderellaDefinition>()
        val diagnostics = mutableListOf<CinderellaReaderDiagnostic>()
        var originX: String? = null
        var originY: String? = null
        var scale: Double? = null
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val parsed = when {
                FREE_POINT.find(line) != null ->
                    parseFreePoint(lines, index, limits.properties)
                JOIN.find(line) != null ->
                    parseLine(
                        lines = lines,
                        index = index,
                        segment = false,
                        limits = limits.properties,
                    )
                SEGMENT.find(line) != null ->
                    parseLine(
                        lines = lines,
                        index = index,
                        segment = true,
                        limits = limits.properties,
                    )
                CIRCLE_MP.find(line) != null ->
                    parseCircleByPoints(
                        lines,
                        index,
                        limits.properties,
                    )
                CIRCLE_BY_FIXED_RADIUS.find(line) != null ->
                    parseCircleByRadius(
                        lines = lines,
                        index = index,
                        fixedRadius = true,
                        limits = limits.properties,
                    )
                CIRCLE_BY_RADIUS.find(line) != null ->
                    parseCircleByRadius(
                        lines = lines,
                        index = index,
                        fixedRadius = false,
                        limits = limits.properties,
                    )
                POINT_ON_CIRCLE.find(line) != null ->
                    parsePointOnCircle(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                POINT_ON_LINE.find(line) != null ->
                    parsePointOnLine(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                MIDPOINT.find(line) != null ->
                    parseMidpoint(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                CIRCLE_BY_THREE_POINTS.find(line) != null ->
                    parseCircumcircle(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                PARALLEL.find(line) != null ->
                    parseParallel(
                        lines = lines,
                        index = index,
                        orthogonal = false,
                        limits = limits.properties,
                    )
                ORTHOGONAL.find(line) != null ->
                    parseParallel(
                        lines = lines,
                        index = index,
                        orthogonal = true,
                        limits = limits.properties,
                    )
                CONIC_BY_FIVE_POINTS.find(line) != null ->
                    parseConicByFivePoints(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                CONIC_FOCI_H.find(line) != null ->
                    parseConicFoci(
                        lines = lines,
                        index = index,
                        hyperbola = true,
                        limits = limits.properties,
                    )
                CONIC_FOCI.find(line) != null ->
                    parseConicFoci(
                        lines = lines,
                        index = index,
                        hyperbola = false,
                        limits = limits.properties,
                    )
                CONIC_PARABOLA.find(line) != null ->
                    parseParabola(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                POLYGON.find(line) != null ->
                    parsePolygon(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                ARC.find(line) != null ->
                    parseArc(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                THROUGH.find(line) != null ->
                    parseThrough(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                COMPASS.find(line) != null ->
                    parseCompass(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                ANGULAR_BISECTOR.find(line) != null ->
                    parseAngularBisector(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                MEET.find(line) != null ->
                    parseMeet(
                        lines = lines,
                        index = index,
                        limits = limits.properties,
                    )
                INTERSECTION_CONIC_LINE.find(line) != null ->
                    parsePairIntersection(
                        lines = lines,
                        index = index,
                        kind = CinderellaIntersectionKind.CONIC_LINE,
                        limits = limits.properties,
                    )
                INTERSECTION_CIRCLE_CIRCLE.find(line) != null ->
                    parsePairIntersection(
                        lines = lines,
                        index = index,
                        kind = CinderellaIntersectionKind.CIRCLE_CIRCLE,
                        limits = limits.properties,
                    )
                else -> null
            }
            if (parsed != null) {
                val definition = when (parsed) {
                    is GMResult.Ok -> parsed.value
                    is GMResult.Err -> return parsed
                }
                definitions += definition.definition
                if (definitions.size > limits.maxElements) {
                    return GMResult.Err(
                        CinderellaReaderError.ElementLimitExceeded(
                            limit = limits.maxElements,
                            requested = definitions.size,
                        ),
                    )
                }
                index = definition.nextIndex + 1
                continue
            }

            ORIGIN_X.find(line)?.let {
                originX = it.groupValues[1]
            }
            ORIGIN_Y.find(line)?.let {
                originY = it.groupValues[1]
            }
            SCALE.find(line)?.let {
                scale = jsParseFloat(it.groupValues[1]) / SCALE_DIVISOR
            }
            if (
                DEFINITION_MARKER in line &&
                !isPropertyLine(line)
            ) {
                diagnostics +=
                    CinderellaReaderDiagnostic.UnsupportedDefinition(
                        lineIndex = index,
                        line = line,
                    )
            }
            index += 1
        }
        return GMResult.Ok(
            ParsedCinderella(
                definitions = definitions.toList(),
                originX = originX,
                originY = originY,
                scale = scale,
                diagnostics = diagnostics.toList(),
            ),
        )
    }

    // JSXGraph 1.13.3: src/reader/cinderella.js -> read.
    internal fun read(
        board: Board,
        isString: Boolean = false,
        limits: CinderellaReaderLimits = CinderellaReaderLimits(),
        failOnUnsupportedDefinitions: Boolean = false,
    ): GMResult<DrawnCinderella, CinderellaReaderError> {
        val parsed = when (
            val result = parse(
                isString = isString,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            failOnUnsupportedDefinitions &&
            parsed.diagnostics.isNotEmpty()
        ) {
            return GMResult.Err(
                CinderellaReaderError.UnsupportedDefinitions(
                    parsed.diagnostics.map { diagnostic ->
                        when (diagnostic) {
                            is CinderellaReaderDiagnostic
                                .UnsupportedDefinition ->
                                diagnostic.lineIndex
                        }
                    },
                ),
            )
        }
        val scale = parsed.scale
            ?: return GMResult.Err(CinderellaReaderError.MissingScale)
        if (!scale.isFinite()) {
            return GMResult.Err(
                CinderellaReaderError.InvalidScale(scale),
            )
        }

        val existingIds = board.objects.keys.toSet()
        val existingNames = board.elementsByName.toMap()
        val objects = linkedMapOf<String, GeometryElement>()
        board.suspendUpdate()
        for (definition in parsed.definitions) {
            val createdObjects = when (
                val result = createDefinition(
                    board = board,
                    objects = objects,
                    definition = definition,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    rollbackBoardRead(
                        board = board,
                        existingIds = existingIds,
                        existingNames = existingNames,
                    )
                    return result
                }
            }
            objects.putAll(createdObjects)
        }

        applyViewport(board, scale)
        board.unsuspendUpdate()
        return GMResult.Ok(
            DrawnCinderella(
                parsed = parsed,
                objects = objects.toMap(),
            ),
        )
    }

    private fun createDefinition(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaDefinition,
    ): GMResult<Map<String, GeometryElement>, CinderellaReaderError> {
        val created = when (definition) {
            is CinderellaAngularBisectorDefinition ->
                return createAngularBisector(board, objects, definition)
            is CinderellaPairIntersectionDefinition ->
                return createPairIntersection(board, objects, definition)
            is CinderellaMeetDefinition ->
                createMeet(board, objects, definition)
            is CinderellaFreePointDefinition ->
                createFreePoint(board, definition)
            is CinderellaLineDefinition ->
                createLine(board, objects, definition)
            is CinderellaCircleByPointsDefinition ->
                createCircleByPoints(board, objects, definition)
            is CinderellaCircleByRadiusDefinition ->
                createCircleByRadius(board, objects, definition)
            is CinderellaPointOnCircleDefinition ->
                createPointOnCircle(board, objects, definition)
            is CinderellaPointOnLineDefinition ->
                createPointOnLine(board, objects, definition)
            is CinderellaMidpointDefinition ->
                createMidpoint(board, objects, definition)
            is CinderellaCircumcircleDefinition ->
                createCircumcircle(board, objects, definition)
            is CinderellaParallelDefinition ->
                createParallel(board, objects, definition)
            is CinderellaOrthogonalDefinition ->
                createOrthogonal(board, objects, definition)
            is CinderellaConicByFivePointsDefinition ->
                createConicByFivePoints(board, objects, definition)
            is CinderellaConicFociDefinition ->
                createConicFoci(board, objects, definition)
            is CinderellaParabolaDefinition ->
                createParabola(board, objects, definition)
            is CinderellaPolygonDefinition ->
                createPolygon(board, objects, definition)
            is CinderellaArcDefinition ->
                createArc(board, objects, definition)
            is CinderellaThroughDefinition ->
                createThrough(board, objects, definition)
            is CinderellaCompassDefinition ->
                createCompass(board, objects, definition)
        }
        return when (created) {
            is GMResult.Ok -> GMResult.Ok(
                mapOf(definition.name to created.value),
            )
            is GMResult.Err -> created
        }
    }

    private fun createFreePoint(
        board: Board,
        definition: CinderellaFreePointDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> =
        when (
            val result = Point.create(
                board = board,
                coordinates = definition.coordinates.toDoubleArray(),
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Point(result.error),
            )
        }

    private fun createLine(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaLineDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val point1 = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.pointNames[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2 = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.pointNames[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> {
                result.value.configureVisibleRange(
                    straightFirst = !definition.segment,
                    straightLast = !definition.segment,
                )
                result
            }
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Line(result.error),
            )
        }
    }

    private fun createCircleByPoints(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaCircleByPointsDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val center = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.centerName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.pointName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Circle.create(
                board = board,
                center = center,
                point2 = point,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Circle(result.error),
            )
        }
    }

    private fun createCircleByRadius(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaCircleByRadiusDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val center = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.centerName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Circle.create(
                board = board,
                center = center,
                radius = definition.radius,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Circle(result.error),
            )
        }
    }

    private fun createPointOnCircle(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaPointOnCircleDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val circle = when (
            val result = resolveCircle(
                board,
                objects,
                definition.name,
                definition.circleName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        circle.update()
        return when (
            val result = Glider.create(
                board = board,
                coordinates = doubleArrayOf(
                    circle.center.X() + definition.centerOffset[0],
                    circle.center.Y() + definition.centerOffset[1],
                ),
                slideObject = circle,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Glider(result.error),
            )
        }
    }

    private fun createPointOnLine(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaPointOnLineDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val line = when (
            val result = resolveLine(
                board,
                objects,
                definition.name,
                definition.lineName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Glider.create(
                board = board,
                coordinates = definition.coordinates.toDoubleArray(),
                slideObject = line,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Glider(result.error),
            )
        }
    }

    private fun createMidpoint(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaMidpointDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val point1 = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.pointNames[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2 = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.pointNames[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = MidpointPoint.create(
                board = board,
                point1 = point1,
                point2 = point2,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Midpoint(result.error),
            )
        }
    }

    private fun createCircumcircle(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaCircumcircleDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val points = mutableListOf<Point>()
        for (pointName in definition.pointNames) {
            val point = when (
                val result = resolvePoint(
                    board,
                    objects,
                    definition.name,
                    pointName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            points += point
        }
        return when (
            val result = Circle.createCircumcircle(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Circle(result.error),
            )
        }
    }

    private fun createParallel(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaParallelDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val line = when (
            val result = resolveLine(
                board,
                objects,
                definition.name,
                definition.lineName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.pointName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = ParallelLine.create(
                board = board,
                sourceLine = line,
                throughPoint = point,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Parallel(result.error),
            )
        }
    }

    private fun createOrthogonal(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaOrthogonalDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val line = when (
            val result = resolveLine(
                board,
                objects,
                definition.name,
                definition.lineName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.pointName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Normal.create(
                board = board,
                firstParent = line,
                secondParent = point,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Normal(result.error),
            )
        }
    }

    private fun createConicByFivePoints(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaConicByFivePointsDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val points = when (
            val result = resolvePoints(
                board = board,
                objects = objects,
                definitionName = definition.name,
                pointNames = definition.pointNames,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Conic.create(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                point4 = points[3],
                point5 = points[4],
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Conic(result.error),
            )
        }
    }

    private fun createConicFoci(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaConicFociDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val points = when (
            val result = resolvePoints(
                board = board,
                objects = objects,
                definitionName = definition.name,
                pointNames = definition.pointNames,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return if (definition.hyperbola) {
            when (
                val result = Hyperbola.create(
                    board = board,
                    focus1 = points[0],
                    focus2 = points[1],
                    pointOnHyperbola = points[2],
                    name = definition.name,
                )
            ) {
                is GMResult.Ok -> result
                is GMResult.Err -> creationFailure(
                    definition.name,
                    CinderellaCreationError.Hyperbola(result.error),
                )
            }
        } else {
            when (
                val result = Ellipse.create(
                    board = board,
                    focus1 = points[0],
                    focus2 = points[1],
                    pointOnEllipse = points[2],
                    name = definition.name,
                )
            ) {
                is GMResult.Ok -> result
                is GMResult.Err -> creationFailure(
                    definition.name,
                    CinderellaCreationError.Ellipse(result.error),
                )
            }
        }
    }

    private fun resolvePoints(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionName: String,
        pointNames: List<String>,
    ): GMResult<List<Point>, CinderellaReaderError> {
        val points = mutableListOf<Point>()
        for (pointName in pointNames) {
            val point = when (
                val result = resolvePoint(
                    board = board,
                    objects = objects,
                    definitionName = definitionName,
                    referenceName = pointName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            points += point
        }
        return GMResult.Ok(points.toList())
    }

    private fun createParabola(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaParabolaDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val focus = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.focusName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val directrix = when (
            val result = resolveLine(
                board,
                objects,
                definition.name,
                definition.directrixName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Parabola.create(
                board = board,
                focus = focus,
                directrix = directrix,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Parabola(result.error),
            )
        }
    }

    private fun createPolygon(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaPolygonDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val points = when (
            val result = resolvePoints(
                board = board,
                objects = objects,
                definitionName = definition.name,
                pointNames = definition.pointNames,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Polygon.create(
                board = board,
                vertices = points,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Polygon(result.error),
            )
        }
    }

    private fun createArc(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaArcDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val points = when (
            val result = resolvePoints(
                board = board,
                objects = objects,
                definitionName = definition.name,
                pointNames = definition.pointNames,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Arc.createCircumcircleArc(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Arc(result.error),
            )
        }
    }

    private fun createThrough(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaThroughDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val source = when (
            val result = resolvePoint(
                board,
                objects,
                definition.name,
                definition.pointName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val helper = when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(
                    source.X() + definition.offset[0],
                    source.Y() + definition.offset[1],
                ),
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.name,
                    CinderellaCreationError.Point(result.error),
                )
            }
        }
        return when (
            val result = Line.create(
                board = board,
                point1 = source,
                point2 = helper,
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> {
                board.removeObject(helper)
                creationFailure(
                    definition.name,
                    CinderellaCreationError.Line(result.error),
                )
            }
        }
    }

    private fun createCompass(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaCompassDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        when (
            val result = resolvePoints(
                board = board,
                objects = objects,
                definitionName = definition.name,
                pointNames = definition.pointNames,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        // JSXGraph 1.13.3 passes `b.select[1]` into Point.Dist here.
        // Preserve that observable failure without throwing from Kotlin.
        return creationFailure(
            definition.name,
            CinderellaCreationError.UpstreamCompassRadiusFunction,
        )
    }

    private fun createAngularBisector(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaAngularBisectorDefinition,
    ): GMResult<Map<String, GeometryElement>, CinderellaReaderError> {
        val first = when (
            val result = resolveLine(
                board,
                objects,
                definition.name,
                definition.lineNames[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = when (
            val result = resolveLine(
                board,
                objects,
                definition.name,
                definition.lineNames[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val bisectors = when (
            val result = BisectorLines.create(
                board = board,
                first = first,
                second = second,
                line1Attributes = BisectorLineAttributes(
                    name = definition.secondOutputName ?: "",
                ),
                line2Attributes = BisectorLineAttributes(
                    name = definition.firstOutputName ?: "",
                ),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.name,
                    CinderellaCreationError.BisectorLines(result.error),
                )
            }
        }
        val line1 = bisectors.line1
            ?: return creationFailure(
                definition.name,
                CinderellaCreationError.MissingCompositeOutput("line1"),
            )
        val line2 = bisectors.line2
            ?: return creationFailure(
                definition.name,
                CinderellaCreationError.MissingCompositeOutput("line2"),
            )
        val outputs = linkedMapOf<String, GeometryElement>()
        definition.firstOutputName?.let { outputs[it] = line2 }
        definition.secondOutputName?.let { outputs[it] = line1 }
        return GMResult.Ok(outputs)
    }

    private fun createMeet(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaMeetDefinition,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val first = when (
            val result = resolveLine(
                board,
                objects,
                definition.name,
                definition.lineNames[0],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = when (
            val result = resolveLine(
                board,
                objects,
                definition.name,
                definition.lineNames[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = IntersectionPoint.create(
                board = board,
                first = first,
                second = second,
                firstIndex = IntersectionIndexSource.Number(0.0),
                name = definition.name,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> creationFailure(
                definition.name,
                CinderellaCreationError.Intersection(result.error),
            )
        }
    }

    private fun createPairIntersection(
        board: Board,
        objects: Map<String, GeometryElement>,
        definition: CinderellaPairIntersectionDefinition,
    ): GMResult<Map<String, GeometryElement>, CinderellaReaderError> {
        val first = when (definition.kind) {
            CinderellaIntersectionKind.CONIC_LINE -> {
                val element = when (
                    val result = resolveElement(
                        board = board,
                        objects = objects,
                        definitionName = definition.name,
                        referenceName = definition.parentNames[0],
                        expectedType = "conic",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (element !is Circle && element !is Curve) {
                    return referenceTypeMismatch(
                        definitionName = definition.name,
                        referenceName = definition.parentNames[0],
                        expectedType = "conic",
                        actual = element,
                    )
                }
                element
            }
            CinderellaIntersectionKind.CIRCLE_CIRCLE ->
                when (
                    val result = resolveCircle(
                        board,
                        objects,
                        definition.name,
                        definition.parentNames[0],
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
        }
        val second = when (definition.kind) {
            CinderellaIntersectionKind.CONIC_LINE ->
                when (
                    val result = resolveLine(
                        board,
                        objects,
                        definition.name,
                        definition.parentNames[1],
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            CinderellaIntersectionKind.CIRCLE_CIRCLE ->
                when (
                    val result = resolveCircle(
                        board,
                        objects,
                        definition.name,
                        definition.parentNames[1],
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
        }
        val directName =
            definition.secondOutputName ?: definition.firstOutputName ?: ""
        val directIndex = when (definition.kind) {
            CinderellaIntersectionKind.CONIC_LINE ->
                if (definition.secondOutputName == null) 0.0 else 1.0
            CinderellaIntersectionKind.CIRCLE_CIRCLE ->
                if (definition.secondOutputName == null) 1.0 else 0.0
        }
        val direct = when (
            val result = IntersectionPoint.create(
                board = board,
                first = first,
                second = second,
                firstIndex = IntersectionIndexSource.Number(directIndex),
                name = directName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return creationFailure(
                    definition.name,
                    CinderellaCreationError.Intersection(result.error),
                )
            }
        }
        val outputs = linkedMapOf<String, GeometryElement>()
        definition.secondOutputName?.let { outputs[it] = direct }
        if (definition.firstOutputName != null) {
            if (definition.secondOutputName == null) {
                outputs[definition.firstOutputName] = direct
            } else {
                val other = when (
                    val result = OtherIntersectionPoint.create(
                        board = board,
                        first = first,
                        second = second,
                        excludedPoints = listOf(direct),
                        name = definition.firstOutputName,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        return creationFailure(
                            definition.name,
                            CinderellaCreationError.Intersection(
                                result.error,
                            ),
                        )
                    }
                }
                outputs[definition.firstOutputName] = other
            }
        }
        return GMResult.Ok(outputs)
    }

    private fun resolvePoint(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionName: String,
        referenceName: String,
    ): GMResult<Point, CinderellaReaderError> =
        when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionName = definitionName,
                referenceName = referenceName,
                expectedType = "point",
            )
        ) {
            is GMResult.Ok -> {
                val point = result.value
                if (point is Point) {
                    GMResult.Ok(point)
                } else {
                    referenceTypeMismatch(
                        definitionName,
                        referenceName,
                        "point",
                        point,
                    )
                }
            }
            is GMResult.Err -> result
        }

    private fun resolveLine(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionName: String,
        referenceName: String,
    ): GMResult<Line, CinderellaReaderError> =
        when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionName = definitionName,
                referenceName = referenceName,
                expectedType = "line",
            )
        ) {
            is GMResult.Ok -> {
                val line = result.value
                if (line is Line) {
                    GMResult.Ok(line)
                } else {
                    referenceTypeMismatch(
                        definitionName,
                        referenceName,
                        "line",
                        line,
                    )
                }
            }
            is GMResult.Err -> result
        }

    private fun resolveCircle(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionName: String,
        referenceName: String,
    ): GMResult<Circle, CinderellaReaderError> =
        when (
            val result = resolveElement(
                board = board,
                objects = objects,
                definitionName = definitionName,
                referenceName = referenceName,
                expectedType = "circle",
            )
        ) {
            is GMResult.Ok -> {
                val circle = result.value
                if (circle is Circle) {
                    GMResult.Ok(circle)
                } else {
                    referenceTypeMismatch(
                        definitionName,
                        referenceName,
                        "circle",
                        circle,
                    )
                }
            }
            is GMResult.Err -> result
        }

    private fun resolveElement(
        board: Board,
        objects: Map<String, GeometryElement>,
        definitionName: String,
        referenceName: String,
        expectedType: String,
    ): GMResult<GeometryElement, CinderellaReaderError> {
        val element = objects[referenceName] ?: board.select(referenceName)
            ?: return GMResult.Err(
                CinderellaReaderError.MissingReference(
                    definitionName = definitionName,
                    referenceName = referenceName,
                    expectedType = expectedType,
                ),
            )
        return GMResult.Ok(element)
    }

    private fun referenceTypeMismatch(
        definitionName: String,
        referenceName: String,
        expectedType: String,
        actual: GeometryElement,
    ): GMResult.Err<CinderellaReaderError.ReferenceTypeMismatch> =
        GMResult.Err(
            CinderellaReaderError.ReferenceTypeMismatch(
                definitionName = definitionName,
                referenceName = referenceName,
                expectedType = expectedType,
                actualType = actual.elType.ifEmpty { "element" },
            ),
        )

    private fun creationFailure(
        definitionName: String,
        cause: CinderellaCreationError,
    ): GMResult.Err<CinderellaReaderError.BoardCreationFailed> =
        GMResult.Err(
            CinderellaReaderError.BoardCreationFailed(
                definitionName = definitionName,
                cause = cause,
            ),
        )

    private fun rollbackBoardRead(
        board: Board,
        existingIds: Set<String>,
        existingNames: Map<String, GeometryElement>,
    ) {
        val created = board.objects
            .filterKeys { it !in existingIds }
            .values
            .toList()
            .asReversed()
        board.removeObjects(created)
        board.elementsByName.clear()
        board.elementsByName.putAll(existingNames)
        board.unsuspendUpdate()
    }

    private fun applyViewport(
        board: Board,
        scale: Double,
    ) {
        board.zoomX *= scale / CINDERELLA_ZOOM_DIVISOR
        board.zoomY *= scale / CINDERELLA_ZOOM_DIVISOR
        board.setCoordinateSystem(
            originX =
                board.origin.scrCoords[1] *
                    DEFAULT_BOARD_ZOOM_FACTOR -
                    CINDERELLA_ORIGIN_X_OFFSET,
            originY =
                board.origin.scrCoords[2] *
                    DEFAULT_BOARD_ZOOM_FACTOR +
                    CINDERELLA_ORIGIN_Y_OFFSET,
            unitX = board.unitX,
            unitY = board.unitY,
        )
    }

    private fun parseFreePoint(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val coordinateText = FREE_POINT.find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?: return malformed(index, "FreePoint", line)
        val values = coordinateText.split(',').map(::complexRealPart)
        if (values.size < HOMOGENEOUS_COORDINATE_COUNT) {
            return malformed(index, "FreePoint", line)
        }
        val properties = when (
            val result = CinderellaProperties.readPointProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaFreePointDefinition(
                    name = name,
                    coordinates = listOf(
                        values[0] / values[2],
                        -values[1] / values[2],
                    ),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseLine(
        lines: List<String>,
        index: Int,
        segment: Boolean,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val functionName = if (segment) "Segment" else "Join"
        val references = quotedNames(
            functionArguments(line, functionName)
                ?: return malformed(index, functionName, line),
        )
        if (references.size < TWO_POINT_COUNT) {
            return malformed(index, functionName, line)
        }
        val properties = when (
            val result = CinderellaProperties.readLineProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaLineDefinition(
                    name = name,
                    pointNames = references.take(TWO_POINT_COUNT),
                    segment = segment,
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseCircleByPoints(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val references = quotedNames(
            functionArguments(line, "CircleMP")
                ?: return malformed(index, "CircleMP", line),
        )
        if (references.size < TWO_POINT_COUNT) {
            return malformed(index, "CircleMP", line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaCircleByPointsDefinition(
                    name = name,
                    centerName = references[0],
                    pointName = references[1],
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseCircleByRadius(
        lines: List<String>,
        index: Int,
        fixedRadius: Boolean,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val functionName =
            if (fixedRadius) {
                "CircleByFixedRadius"
            } else {
                "CircleByRadius"
            }
        val arguments = functionArguments(line, functionName)
            ?: return malformed(index, functionName, line)
        val centerName = quotedNames(arguments).firstOrNull()
            ?: return malformed(index, functionName, line)
        val radiusText = arguments
            .substringAfter(',', missingDelimiterValue = "")
            .substringBefore("+i*")
        if (radiusText.isEmpty()) {
            return malformed(index, functionName, line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaCircleByRadiusDefinition(
                    name = name,
                    centerName = centerName,
                    radius = sqrt(jsParseFloat(radiusText)),
                    fixedRadius = fixedRadius,
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parsePointOnCircle(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arguments = functionArguments(line, "PointOnCircle")
            ?: return malformed(index, "PointOnCircle", line)
        val circleName = quotedNames(arguments).firstOrNull()
            ?: return malformed(index, "PointOnCircle", line)
        val values = coordinateVector(arguments)
            ?: return malformed(index, "PointOnCircle", line)
        if (values.size < TWO_POINT_COUNT) {
            return malformed(index, "PointOnCircle", line)
        }
        val orientation = if (line.getOrNull(1) == 'n') -1.0 else 1.0
        val properties = when (
            val result = CinderellaProperties.readPointProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaPointOnCircleDefinition(
                    name = name,
                    circleName = circleName,
                    centerOffset = listOf(
                        orientation * values[0],
                        -orientation * values[1],
                    ),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parsePointOnLine(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arguments = functionArguments(line, "PointOnLine")
            ?: return malformed(index, "PointOnLine", line)
        val lineName = quotedNames(arguments).firstOrNull()
            ?: return malformed(index, "PointOnLine", line)
        val values = coordinateVector(arguments)
            ?: return malformed(index, "PointOnLine", line)
        if (values.size < HOMOGENEOUS_COORDINATE_COUNT) {
            return malformed(index, "PointOnLine", line)
        }
        val properties = when (
            val result = CinderellaProperties.readPointProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaPointOnLineDefinition(
                    name = name,
                    lineName = lineName,
                    coordinates = listOf(
                        values[0] / values[2],
                        -values[1] / values[2],
                    ),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseMidpoint(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointNames = quotedNames(
            functionArguments(line, "Mid")
                ?: return malformed(index, "Mid", line),
        )
        if (pointNames.size < TWO_POINT_COUNT) {
            return malformed(index, "Mid", line)
        }
        val properties = when (
            val result = CinderellaProperties.readPointProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaMidpointDefinition(
                    name = name,
                    pointNames = pointNames.take(TWO_POINT_COUNT),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseCircumcircle(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointNames = quotedNames(
            functionArguments(line, "CircleBy3")
                ?: return malformed(index, "CircleBy3", line),
        )
        if (pointNames.size < THREE_POINT_COUNT) {
            return malformed(index, "CircleBy3", line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaCircumcircleDefinition(
                    name = name,
                    pointNames = pointNames.take(THREE_POINT_COUNT),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseParallel(
        lines: List<String>,
        index: Int,
        orthogonal: Boolean,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val functionName = if (orthogonal) "Orthogonal" else "Parallel"
        val references = quotedNames(
            functionArguments(line, functionName)
                ?: return malformed(index, functionName, line),
        )
        if (references.size < TWO_POINT_COUNT) {
            return malformed(index, functionName, line)
        }
        val properties = when (
            val result = CinderellaProperties.readLineProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        val definition = if (orthogonal) {
            CinderellaOrthogonalDefinition(
                name = name,
                lineName = references[0],
                pointName = references[1],
                properties = properties,
                sourceLine = index,
            )
        } else {
            CinderellaParallelDefinition(
                name = name,
                lineName = references[0],
                pointName = references[1],
                properties = properties,
                sourceLine = index,
            )
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = definition,
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseConicByFivePoints(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointNames = quotedNames(
            functionArguments(line, "ConicBy5")
                ?: return malformed(index, "ConicBy5", line),
        )
        if (pointNames.size < FIVE_POINT_COUNT) {
            return malformed(index, "ConicBy5", line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaConicByFivePointsDefinition(
                    name = name,
                    pointNames = pointNames.take(FIVE_POINT_COUNT),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseConicFoci(
        lines: List<String>,
        index: Int,
        hyperbola: Boolean,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val functionName = if (hyperbola) "ConicFociH" else "ConicFoci"
        val pointNames = quotedNames(
            functionArguments(line, functionName)
                ?: return malformed(index, functionName, line),
        )
        if (pointNames.size < THREE_POINT_COUNT) {
            return malformed(index, functionName, line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaConicFociDefinition(
                    name = name,
                    pointNames = pointNames.take(THREE_POINT_COUNT),
                    hyperbola = hyperbola,
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseParabola(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val references = quotedNames(
            functionArguments(line, "ConicParabolaPL")
                ?: return malformed(index, "ConicParabolaPL", line),
        )
        if (references.size < TWO_POINT_COUNT) {
            return malformed(index, "ConicParabolaPL", line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaParabolaDefinition(
                    name = name,
                    focusName = references[0],
                    directrixName = references[1],
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parsePolygon(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointNames = quotedNames(
            functionArguments(line, "Poly")
                ?: return malformed(index, "Poly", line),
        )
        if (pointNames.size < THREE_POINT_COUNT) {
            return malformed(index, "Poly", line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaPolygonDefinition(
                    name = name,
                    pointNames = pointNames,
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseArc(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointNames = quotedNames(
            functionArguments(line, "Arc")
                ?: return malformed(index, "Arc", line),
        )
        if (pointNames.size < THREE_POINT_COUNT) {
            return malformed(index, "Arc", line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaArcDefinition(
                    name = name,
                    pointNames = pointNames.take(THREE_POINT_COUNT),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseThrough(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arguments = functionArguments(line, "Through")
            ?: return malformed(index, "Through", line)
        val pointName = quotedNames(arguments).firstOrNull()
            ?: return malformed(index, "Through", line)
        val values = coordinateVector(arguments)
            ?: return malformed(index, "Through", line)
        if (values.size < TWO_POINT_COUNT) {
            return malformed(index, "Through", line)
        }
        val properties = when (
            val result = CinderellaProperties.readLineProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaThroughDefinition(
                    name = name,
                    pointName = pointName,
                    offset = values.take(TWO_POINT_COUNT),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseCompass(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointNames = quotedNames(
            functionArguments(line, "Compass")
                ?: return malformed(index, "Compass", line),
        )
        if (pointNames.size < THREE_POINT_COUNT) {
            return malformed(index, "Compass", line)
        }
        val properties = when (
            val result = CinderellaProperties.readCircleProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaCompassDefinition(
                    name = name,
                    pointNames = pointNames.take(THREE_POINT_COUNT),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parseAngularBisector(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val outputNames = when (
            val result = pairedOutputNames(
                line = line,
                lineIndex = index,
                kind = "AngularBisector",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val lineNames = quotedNames(
            functionArguments(line, "AngularBisector")
                ?: return malformed(index, "AngularBisector", line),
        )
        if (lineNames.size < TWO_POINT_COUNT) {
            return malformed(index, "AngularBisector", line)
        }
        val firstProperties = when (
            val result = CinderellaProperties.readLineProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        val secondProperties =
            if (outputNames.first != null && outputNames.second != null) {
                when (
                    val result = CinderellaProperties.readLineProperties(
                        dataLines = lines,
                        startIndex = firstProperties.nextIndex,
                        limits = limits,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err ->
                        return propertyFailure(index, result.error)
                }
            } else {
                null
            }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaAngularBisectorDefinition(
                    firstOutputName = outputNames.first,
                    secondOutputName = outputNames.second,
                    lineNames = lineNames.take(TWO_POINT_COUNT),
                    firstProperties = firstProperties,
                    secondProperties = secondProperties,
                    sourceLine = index,
                ),
                // JSXGraph assigns `i = erg[2]` after reading `erg2`.
                nextIndex = firstProperties.nextIndex,
            ),
        )
    }

    private fun parseMeet(
        lines: List<String>,
        index: Int,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val name = when (val result = objectName(line, index)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val lineNames = quotedNames(
            functionArguments(line, "Meet")
                ?: return malformed(index, "Meet", line),
        )
        if (lineNames.size < TWO_POINT_COUNT) {
            return malformed(index, "Meet", line)
        }
        val properties = when (
            val result = CinderellaProperties.readPointProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaMeetDefinition(
                    name = name,
                    lineNames = lineNames.take(TWO_POINT_COUNT),
                    properties = properties,
                    sourceLine = index,
                ),
                nextIndex = properties.nextIndex,
            ),
        )
    }

    private fun parsePairIntersection(
        lines: List<String>,
        index: Int,
        kind: CinderellaIntersectionKind,
        limits: CinderellaPropertyLimits,
    ): GMResult<ParsedDefinition, CinderellaReaderError> {
        val line = lines[index]
        val functionName = when (kind) {
            CinderellaIntersectionKind.CONIC_LINE ->
                "IntersectionConicLine"
            CinderellaIntersectionKind.CIRCLE_CIRCLE ->
                "IntersectionCircleCircle"
        }
        val outputNames = when (
            val result = pairedOutputNames(
                line = line,
                lineIndex = index,
                kind = functionName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val parentNames = quotedNames(
            functionArguments(line, functionName)
                ?: return malformed(index, functionName, line),
        )
        if (parentNames.size < TWO_POINT_COUNT) {
            return malformed(index, functionName, line)
        }
        val firstProperties = when (
            val result = CinderellaProperties.readPointProperties(
                dataLines = lines,
                startIndex = index,
                limits = limits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return propertyFailure(index, result.error)
        }
        val secondProperties =
            if (outputNames.first != null && outputNames.second != null) {
                when (
                    val result = CinderellaProperties.readPointProperties(
                        dataLines = lines,
                        startIndex = firstProperties.nextIndex,
                        limits = limits,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err ->
                        return propertyFailure(index, result.error)
                }
            } else {
                null
            }
        return GMResult.Ok(
            ParsedDefinition(
                definition = CinderellaPairIntersectionDefinition(
                    firstOutputName = outputNames.first,
                    secondOutputName = outputNames.second,
                    parentNames = parentNames.take(TWO_POINT_COUNT),
                    kind = kind,
                    firstProperties = firstProperties,
                    secondProperties = secondProperties,
                    sourceLine = index,
                ),
                // JSXGraph assigns `i = erg[1]` after reading `erg2`.
                nextIndex = firstProperties.nextIndex,
            ),
        )
    }

    private fun pairedOutputNames(
        line: String,
        lineIndex: Int,
        kind: String,
    ): GMResult<Pair<String?, String?>, CinderellaReaderError> {
        val match = PAIRED_OUTPUT_NAMES.find(line)
            ?: return malformed(lineIndex, kind, line)
        return GMResult.Ok(
            match.groups[2]?.value?.takeIf(String::isNotEmpty) to
                match.groups[4]?.value?.takeIf(String::isNotEmpty),
        )
    }

    private fun objectName(
        line: String,
        lineIndex: Int,
    ): GMResult<String, CinderellaReaderError> {
        val name = QUOTED_NAME.find(line)?.groupValues?.getOrNull(1)
            ?: return GMResult.Err(
                CinderellaReaderError.MissingObjectName(
                    lineIndex = lineIndex,
                    line = line,
                ),
            )
        return GMResult.Ok(name)
    }

    private fun functionArguments(
        line: String,
        functionName: String,
    ): String? {
        val start = line.indexOf("$functionName(")
        if (start < 0) {
            return null
        }
        val contentStart = start + functionName.length + 1
        val end = line.indexOf(')', startIndex = contentStart)
        return if (end >= contentStart) {
            line.substring(contentStart, end)
        } else {
            null
        }
    }

    private fun quotedNames(value: String): List<String> =
        QUOTED_NAME.findAll(value).map { it.groupValues[1] }.toList()

    private fun complexRealPart(value: String): Double =
        jsParseFloat(
            value
                .trim()
                .removePrefix("[")
                .substringBefore("+i*"),
        )

    private fun coordinateVector(arguments: String): List<Double>? {
        val start = arguments.indexOf('[')
        val end = arguments.lastIndexOf(']')
        if (start < 0 || end <= start) {
            return null
        }
        return arguments
            .substring(start + 1, end)
            .split(',')
            .map(::complexRealPart)
    }

    private fun isPropertyLine(line: String): Boolean =
        PROPERTY_MARKERS.any(line::contains)

    private fun validateLimits(
        limits: CinderellaReaderLimits,
    ): CinderellaReaderError.InvalidLimits? {
        val invalid = when {
            limits.maxLines < 0 -> "maxLines" to limits.maxLines
            limits.maxElements < 0 -> "maxElements" to limits.maxElements
            else -> null
        }
        return invalid?.let { (name, value) ->
            CinderellaReaderError.InvalidLimits(name, value)
        }
    }

    private fun malformed(
        lineIndex: Int,
        kind: String,
        line: String,
    ): GMResult.Err<CinderellaReaderError.MalformedDefinition> =
        GMResult.Err(
            CinderellaReaderError.MalformedDefinition(
                lineIndex = lineIndex,
                kind = kind,
                line = line,
            ),
        )

    private fun propertyFailure(
        lineIndex: Int,
        cause: CinderellaPropertyError,
    ): GMResult.Err<CinderellaReaderError.PropertyParsingFailed> =
        GMResult.Err(
            CinderellaReaderError.PropertyParsingFailed(
                lineIndex = lineIndex,
                cause = cause,
            ),
        )

    private fun jsParseFloat(value: String): Double {
        val prefix = FLOAT_PREFIX.find(value.trimStart())?.value
            ?: return Double.NaN
        return prefix.toDoubleOrNull() ?: Double.NaN
    }

    private data class ParsedDefinition(
        val definition: CinderellaDefinition,
        val nextIndex: Int,
    )

    private companion object {
        const val HOMOGENEOUS_COORDINATE_COUNT = 3
        const val TWO_POINT_COUNT = 2
        const val THREE_POINT_COUNT = 3
        const val FIVE_POINT_COUNT = 5
        const val SCALE_DIVISOR = 25.0
        const val CINDERELLA_ZOOM_DIVISOR = 2.4
        const val DEFAULT_BOARD_ZOOM_FACTOR = 1.25
        const val CINDERELLA_ORIGIN_X_OFFSET = 150.0
        const val CINDERELLA_ORIGIN_Y_OFFSET = 50.0
        const val DEFINITION_MARKER = ":="

        val FREE_POINT = Regex("""FreePoint\(\[([^]]*)]""")
        val JOIN = Regex("""Join\(.+""")
        val SEGMENT = Regex("""Segment\(.+""")
        val CIRCLE_MP = Regex("""CircleMP.+""")
        val CIRCLE_BY_FIXED_RADIUS =
            Regex("""CircleByFixedRadius.+""")
        val CIRCLE_BY_RADIUS = Regex("""CircleByRadius.+""")
        val POINT_ON_CIRCLE = Regex("""PointOnCircle.+""")
        val POINT_ON_LINE = Regex("""PointOnLine.+""")
        val MIDPOINT = Regex("""Mid\(.+""")
        val CIRCLE_BY_THREE_POINTS = Regex("""CircleBy3\(.+""")
        val PARALLEL = Regex("""Parallel\(.+""")
        val ORTHOGONAL = Regex("""Orthogonal\(.+""")
        val CONIC_BY_FIVE_POINTS = Regex("""ConicBy5\(.+""")
        val CONIC_FOCI = Regex("""ConicFoci\(.+""")
        val CONIC_FOCI_H = Regex("""ConicFociH\(.+""")
        val CONIC_PARABOLA = Regex("""ConicParabolaPL\(.+""")
        val POLYGON = Regex("""Poly\(.+""")
        val ARC = Regex("""Arc\(.+""")
        val THROUGH = Regex("""Through\(.+""")
        val COMPASS = Regex(""":=Compass\(.+""")
        val ANGULAR_BISECTOR = Regex("""AngularBisector\(.+""")
        val MEET = Regex("""Meet\(.+""")
        val INTERSECTION_CONIC_LINE =
            Regex("""IntersectionConicLine\(.+""")
        val INTERSECTION_CIRCLE_CIRCLE =
            Regex("""IntersectionCircleCircle\(.+""")
        val PAIRED_OUTPUT_NAMES = Regex(
            """^\s*\{\s*(null|"([A-Za-z0-9]*)")\s*,""" +
                """\s*(null|"([A-Za-z0-9]*)")\s*,""",
        )
        val QUOTED_NAME = Regex(""""([A-Za-z0-9]*)"""")
        val ORIGIN_X = Regex("""setOriginX\(([0-9.]*)\)""")
        val ORIGIN_Y = Regex("""setOriginY\(([0-9.]*)\)""")
        val SCALE = Regex("""setScale\(([0-9.]*)\)""")
        val FLOAT_PREFIX =
            Regex(
                "^[+-]?(?:(?:\\d+\\.?\\d*|\\.\\d+)" +
                    "(?:[eE][+-]?\\d+)?)",
            )
        val PROPERTY_MARKERS = listOf(
            "setAppearance",
            "setAttribute",
            "noPBorder",
        )
    }
}

internal object CinderellaReaderFactory :
    JsxGraphReaderFactory<Board> {
    override fun create(
        board: Board,
        source: String,
    ): GMResult<JsxGraphReader, ReaderError> =
        GMResult.Ok(
            JsxGraphReader {
                when (
                    val result = CinderellaReader(source).read(
                        board = board,
                        failOnUnsupportedDefinitions = true,
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

internal fun ReaderRegistry<Board>.registerCinderellaReader() {
    registerReader(
        reader = CinderellaReaderFactory,
        extensions = listOf("cdy", "cindy", "cinderella"),
    )
}
