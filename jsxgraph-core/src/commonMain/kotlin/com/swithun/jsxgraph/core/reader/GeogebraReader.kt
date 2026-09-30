/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/geogebra.js -> GeogebraReader constructor,
 * writeBoard, checkElement, writeElement, and read.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.ArcError
import com.swithun.jsxgraph.core.base.Axis
import com.swithun.jsxgraph.core.base.AxisError
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.Conic
import com.swithun.jsxgraph.core.base.ConicError
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveError
import com.swithun.jsxgraph.core.base.Ellipse
import com.swithun.jsxgraph.core.base.EllipseError
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.GliderError
import com.swithun.jsxgraph.core.base.IntersectionError
import com.swithun.jsxgraph.core.base.IntersectionIndexSource
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Integral
import com.swithun.jsxgraph.core.base.IntegralAttributes
import com.swithun.jsxgraph.core.base.IntegralBoundary
import com.swithun.jsxgraph.core.base.IntegralError
import com.swithun.jsxgraph.core.base.IntegralLabelAttributes
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.MidpointError
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.Normal
import com.swithun.jsxgraph.core.base.NormalError
import com.swithun.jsxgraph.core.base.OtherIntersectionPoint
import com.swithun.jsxgraph.core.base.OrthogonalConstructionError
import com.swithun.jsxgraph.core.base.ParallelConstructionError
import com.swithun.jsxgraph.core.base.ParallelLine
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.PointReflectionError
import com.swithun.jsxgraph.core.base.PointReflections
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.PolygonError
import com.swithun.jsxgraph.core.base.RegularPolygon
import com.swithun.jsxgraph.core.base.RegularPolygonError
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.SectorError
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.base.SliderAttributes
import com.swithun.jsxgraph.core.base.SliderError
import com.swithun.jsxgraph.core.base.Tangent
import com.swithun.jsxgraph.core.base.TangentError
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.TextError
import com.swithun.jsxgraph.core.base.TriangleCenterConstructionError
import com.swithun.jsxgraph.core.base.Transformation
import com.swithun.jsxgraph.core.base.TransformationDynamicParameter
import com.swithun.jsxgraph.core.base.TransformationError
import com.swithun.jsxgraph.core.base.TransformationParameter
import com.swithun.jsxgraph.core.math.Numerics
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeNumericCoordinateFunction
import com.swithun.jsxgraph.core.utils.XML
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import com.swithun.jsxgraph.core.utils.XmlDocument
import com.swithun.jsxgraph.core.utils.XmlElement
import com.swithun.jsxgraph.core.utils.XmlError
import com.swithun.jsxgraph.core.utils.XmlLimits
import kotlin.math.PI

internal data class GeogebraReaderLimits(
    val preparation: ReaderPreparationLimits = ReaderPreparationLimits(),
    val xml: XmlLimits = XmlLimits(),
    val properties: GeogebraPropertyLimits =
        GeogebraPropertyLimits(),
    val expression: GeogebraExpressionLimits =
        GeogebraExpressionLimits(),
    val maxConstructions: Int = 10_000,
    val maxCommands: Int = 100_000,
    val maxElements: Int = 100_000,
    val maxObjects: Int = 100_000,
)

internal sealed interface GeogebraReaderError : ReaderDomainError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : GeogebraReaderError

    data class PreparationFailed(
        val cause: ReaderPreparationError,
    ) : GeogebraReaderError

    data class XmlParsingFailed(
        val cause: XmlError,
    ) : GeogebraReaderError

    data class MissingDocumentPart(
        val name: String,
    ) : GeogebraReaderError

    data class ConstructionLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GeogebraReaderError

    data class CommandLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GeogebraReaderError

    data class ElementLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GeogebraReaderError

    data class ObjectLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GeogebraReaderError

    data class MalformedCommand(
        val command: String,
        val missing: String,
    ) : GeogebraReaderError

    data class MissingElement(
        val name: String,
    ) : GeogebraReaderError

    data class CyclicElementDependency(
        val names: List<String>,
    ) : GeogebraReaderError

    data class UnsupportedElement(
        val type: String,
        val name: String,
    ) : GeogebraReaderError

    data class UnsupportedCommand(
        val name: String,
    ) : GeogebraReaderError

    data class ArgumentCount(
        val command: String,
        val expected: String,
        val actual: Int,
    ) : GeogebraReaderError

    data class ArgumentType(
        val command: String,
        val index: Int,
        val expected: String,
        val actual: String,
    ) : GeogebraReaderError

    data class InvalidNumber(
        val command: String,
        val index: Int,
        val value: String,
    ) : GeogebraReaderError

    data class PropertyFailure(
        val name: String,
        val cause: GeogebraPropertyError,
    ) : GeogebraReaderError

    data class ExpressionFailure(
        val source: String,
        val cause: GeogebraExpressionError,
    ) : GeogebraReaderError

    data class CreationFailed(
        val name: String,
        val cause: GeogebraCreationError,
    ) : GeogebraReaderError
}

internal sealed interface GeogebraCreationError {
    data class Axis(val cause: AxisError) : GeogebraCreationError

    data class Point(val cause: PointError) : GeogebraCreationError

    data class Line(val cause: LineError) : GeogebraCreationError

    data class Circle(val cause: CircleError) : GeogebraCreationError

    data class Arc(val cause: ArcError) : GeogebraCreationError

    data class Sector(val cause: SectorError) : GeogebraCreationError

    data class Ellipse(val cause: EllipseError) : GeogebraCreationError

    data class Conic(val cause: ConicError) : GeogebraCreationError

    data class Polygon(val cause: PolygonError) : GeogebraCreationError

    data class RegularPolygon(
        val cause: RegularPolygonError,
    ) : GeogebraCreationError

    data class Glider(val cause: GliderError) : GeogebraCreationError

    data class Parallel(
        val cause: ParallelConstructionError,
    ) : GeogebraCreationError

    data class Normal(val cause: NormalError) : GeogebraCreationError

    data class Orthogonal(
        val cause: OrthogonalConstructionError,
    ) : GeogebraCreationError

    data class Intersection(
        val cause: IntersectionError,
    ) : GeogebraCreationError

    data class Midpoint(val cause: MidpointError) : GeogebraCreationError

    data class Slider(val cause: SliderError) : GeogebraCreationError

    data class Curve(val cause: CurveError) : GeogebraCreationError

    data class Transformation(
        val cause: TransformationError,
    ) : GeogebraCreationError

    data class PointReflection(
        val cause: PointReflectionError,
    ) : GeogebraCreationError

    data class Tangent(val cause: TangentError) : GeogebraCreationError

    data class Bisector(
        val cause: TriangleCenterConstructionError,
    ) : GeogebraCreationError

    data class Integral(val cause: IntegralError) : GeogebraCreationError

    data class Text(val cause: TextError) : GeogebraCreationError
}

internal data class GeogebraCommand(
    val name: String,
    val inputs: List<String>,
    val outputs: List<String>,
)

internal data class GeogebraConstruction(
    val elements: Map<String, XmlElement>,
    val expressions: Map<String, XmlElement>,
    val commands: List<GeogebraCommand>,
)

internal data class ParsedGeogebra(
    val document: XmlDocument,
    val format: Double,
    val decimals: Int,
    val constructions: List<GeogebraConstruction>,
)

internal data class DrawnGeogebra(
    val parsed: ParsedGeogebra,
    val values: Map<String, GeogebraReaderValue>,
    val objects: Map<String, GeometryElement>,
)

internal class GeogebraReader(
    private val data: String,
) {
    // JSXGraph 1.13.3: src/reader/geogebra.js -> constructor.
    internal fun parse(
        limits: GeogebraReaderLimits = GeogebraReaderLimits(),
    ): GMResult<ParsedGeogebra, GeogebraReaderError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val prepared = when (
            val result = ReaderPreparation.prepareGeogebra(
                source = data,
                limits = limits.preparation,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    GeogebraReaderError.PreparationFailed(result.error),
                )
            }
        }
        val document = when (
            val result = XML.parse(prepared, limits.xml)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    GeogebraReaderError.XmlParsingFailed(result.error),
                )
            }
        }
        val root = document.getElementsByTagName(GEOGEBRA_TAG)
            .firstOrNull()
            ?: document.documentElement
                ?.takeIf { it.nodeName == GEOGEBRA_TAG }
            ?: return GMResult.Err(
                GeogebraReaderError.MissingDocumentPart(GEOGEBRA_TAG),
            )
        val constructions = root.getElementsByTagName(CONSTRUCTION_TAG)
        if (constructions.size > limits.maxConstructions) {
            return GMResult.Err(
                GeogebraReaderError.ConstructionLimitExceeded(
                    limits.maxConstructions,
                    constructions.size,
                ),
            )
        }
        val parsedConstructions =
            mutableListOf<GeogebraConstruction>()
        var commandCount = 0
        var elementCount = 0
        for (construction in constructions) {
            val elements = construction
                .getElementsByTagName(ELEMENT_TAG)
                .associateBy {
                    it.getAttribute(LABEL_ATTRIBUTE).orEmpty()
                }
            val expressions = construction
                .getElementsByTagName(EXPRESSION_TAG)
                .associateBy {
                    it.getAttribute(LABEL_ATTRIBUTE).orEmpty()
                }
            val commands = mutableListOf<GeogebraCommand>()
            for (
                command in
                construction.getElementsByTagName(COMMAND_TAG)
            ) {
                val name = command.getAttribute(NAME_ATTRIBUTE)
                    ?.takeIf(String::isNotEmpty)
                    ?: return GMResult.Err(
                        GeogebraReaderError.MalformedCommand(
                            command = "",
                            missing = NAME_ATTRIBUTE,
                        ),
                    )
                val input = command
                    .getElementsByTagName(INPUT_TAG)
                    .firstOrNull()
                    ?: return GMResult.Err(
                        GeogebraReaderError.MalformedCommand(
                            command = name,
                            missing = INPUT_TAG,
                        ),
                    )
                val output = command
                    .getElementsByTagName(OUTPUT_TAG)
                    .firstOrNull()
                    ?: return GMResult.Err(
                        GeogebraReaderError.MalformedCommand(
                            command = name,
                            missing = OUTPUT_TAG,
                        ),
                    )
                commands += GeogebraCommand(
                    name = name.lowercase(),
                    inputs = input.attributes.toList().map { it.value },
                    outputs = output.attributes.toList().map { it.value },
                )
            }
            commandCount += commands.size
            elementCount += elements.size
            if (commandCount > limits.maxCommands) {
                return GMResult.Err(
                    GeogebraReaderError.CommandLimitExceeded(
                        limits.maxCommands,
                        commandCount,
                    ),
                )
            }
            if (elementCount > limits.maxElements) {
                return GMResult.Err(
                    GeogebraReaderError.ElementLimitExceeded(
                        limits.maxElements,
                        elementCount,
                    ),
                )
            }
            parsedConstructions += GeogebraConstruction(
                elements = elements,
                expressions = expressions,
                commands = commands,
            )
        }
        val decimals = root.getElementsByTagName(DECIMALS_TAG)
            .firstOrNull()
            ?.getAttribute(VALUE_ATTRIBUTE)
            ?.let(::jsParseInt)
            ?: 2
        return GMResult.Ok(
            ParsedGeogebra(
                document = document,
                format = root.getAttribute(FORMAT_ATTRIBUTE)
                    ?.toDoubleOrNull()
                    ?: Double.NaN,
                decimals = decimals,
                constructions = parsedConstructions,
            ),
        )
    }

    // JSXGraph 1.13.3: src/reader/geogebra.js -> read.
    internal fun read(
        board: Board,
        limits: GeogebraReaderLimits = GeogebraReaderLimits(),
    ): GMResult<DrawnGeogebra, GeogebraReaderError> {
        val parsed = when (val result = parse(limits)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val existingIds = board.objects.keys.toSet()
        val existingNames = board.elementsByName.toMap()
        val previousBoundingBox = board.getBoundingBox()
        board.suspendUpdate()
        val evaluator = Evaluator(
            board = board,
            parsed = parsed,
            limits = limits,
            initialObjectCount = existingIds.size,
        )
        when (val result = evaluator.read()) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> {
                rollback(
                    board,
                    existingIds,
                    existingNames,
                    previousBoundingBox,
                )
                return result
            }
        }
        board.unsuspendUpdate()
        val values = evaluator.values.toMap()
        return GMResult.Ok(
            DrawnGeogebra(
                parsed = parsed,
                values = values,
                objects = values.mapNotNull { (name, value) ->
                    when (value) {
                        is GeogebraReaderValue.Element ->
                            value.value
                        is GeogebraReaderValue.ElementScalar ->
                            value.element
                        is GeogebraReaderValue.Scalar -> null
                    }?.let { name to it }
                }.toMap(),
            ),
        )
    }

    private class Evaluator(
        private val board: Board,
        private val parsed: ParsedGeogebra,
        private val limits: GeogebraReaderLimits,
        private val initialObjectCount: Int,
    ) {
        val values = linkedMapOf<String, GeogebraReaderValue>()
        private val pending = mutableListOf<String>()

        fun read(): GMResult<Unit, GeogebraReaderError> {
            when (val result = writeBoard()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            for (construction in parsed.constructions) {
                for (command in construction.commands) {
                    if (
                        command.outputs.firstOrNull()?.let(values::containsKey)
                        == true
                    ) {
                        continue
                    }
                    when (
                        val result =
                            writeCommand(construction, command)
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                    when (val result = objectLimit()) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                }
                for ((name, element) in construction.elements) {
                    if (name.isEmpty() || name in values) {
                        continue
                    }
                    when (
                        val result = checkElement(
                            construction,
                            name,
                            element,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                    when (val result = objectLimit()) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                }
            }
            return GMResult.Ok(Unit)
        }

        // JSXGraph 1.13.3: src/reader/geogebra.js -> writeBoard.
        private fun writeBoard():
            GMResult<Unit, GeogebraReaderError> {
            val root = parsed.document.documentElement
                ?: return GMResult.Err(
                    GeogebraReaderError.MissingDocumentPart(
                        GEOGEBRA_TAG,
                    ),
                )
            val view = root.getElementsByTagName(EUCLIDIAN_VIEW_TAG)
                .firstOrNull()
                ?: return GMResult.Err(
                    GeogebraReaderError.MissingDocumentPart(
                        EUCLIDIAN_VIEW_TAG,
                    ),
                )
            val coordinates = view
                .getElementsByTagName(COORD_SYSTEM_TAG)
                .firstOrNull()
                ?: return GMResult.Err(
                    GeogebraReaderError.MissingDocumentPart(
                        COORD_SYSTEM_TAG,
                    ),
                )
            val unitX = coordinates.getAttribute(SCALE_ATTRIBUTE)
                ?.let(::jsParseInt)
                ?.toDouble()
                ?: 1.0
            val unitY = coordinates.getAttribute(Y_SCALE_ATTRIBUTE)
                ?.let(::jsParseInt)
                ?.toDouble()
                ?: unitX
            board.setCoordinateSystem(
                originX = coordinates
                    .getAttribute(X_ZERO_ATTRIBUTE)
                    ?.let(::jsParseInt)
                    ?.toDouble()
                    ?: 0.0,
                originY = coordinates
                    .getAttribute(Y_ZERO_ATTRIBUTE)
                    ?.let(::jsParseInt)
                    ?.toDouble()
                    ?: 0.0,
                unitX = unitX,
                unitY = unitY,
            )
            val settings = view
                .getElementsByTagName(EV_SETTINGS_TAG)
                .firstOrNull()
                ?: return GMResult.Ok(Unit)
            if (settings.getAttribute(AXES_ATTRIBUTE) != null) {
                val xAxis = when (
                    val result = createAxis(
                        doubleArrayOf(0.0, 0.0),
                        doubleArrayOf(1.0, 0.0),
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val yAxis = when (
                    val result = createAxis(
                        doubleArrayOf(0.0, 0.0),
                        doubleArrayOf(0.0, 1.0),
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                values["xAxis"] = GeogebraReaderValue.Element(xAxis)
                values["yAxis"] = GeogebraReaderValue.Element(yAxis)
            }
            return GMResult.Ok(Unit)
        }

        private fun createAxis(
            firstCoordinates: DoubleArray,
            secondCoordinates: DoubleArray,
        ): GMResult<Line, GeogebraReaderError> {
            val first = when (
                val result = Point.create(
                    board,
                    firstCoordinates,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    "axis",
                    GeogebraCreationError.Point(result.error),
                )
            }
            val second = when (
                val result = Point.create(
                    board,
                    secondCoordinates,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    "axis",
                    GeogebraCreationError.Point(result.error),
                )
            }
            return when (
                val result = Axis.create(board, first, second)
            ) {
                is GMResult.Ok -> GMResult.Ok(result.value)
                is GMResult.Err -> creationFailure(
                    "axis",
                    GeogebraCreationError.Axis(result.error),
                )
            }
        }

        private fun writeCommand(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
        ): GMResult<Unit, GeogebraReaderError> {
            if (command.outputs.isEmpty()) {
                return GMResult.Err(
                    GeogebraReaderError.MalformedCommand(
                        command.name,
                        OUTPUT_TAG,
                    ),
                )
            }
            val outputElements = mutableListOf<XmlElement>()
            for (name in command.outputs) {
                val element = construction.elements[name]
                    ?: return GMResult.Err(
                        GeogebraReaderError.MissingElement(name),
                    )
                outputElements += element
            }
            return when (command.name) {
                "point" -> {
                    requireCount(command, 1)?.let { return it }
                    val host = when (
                        val result = resolveElement(
                            construction,
                            command.inputs[0],
                            command.name,
                            0,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    registerSingle(
                        command.outputs[0],
                        createPoint(
                            construction,
                            outputElements[0],
                            host,
                        ),
                    )
                }
                "segment" -> createTwoPointLine(
                    construction,
                    command,
                    outputElements[0],
                    segment = true,
                    arrow = false,
                )
                "line" -> createLineCommand(
                    construction,
                    command,
                    outputElements[0],
                )
                "orthogonalline" -> createNormal(
                    construction,
                    command,
                    outputElements[0],
                )
                "polygon" -> createPolygon(
                    construction,
                    command,
                    outputElements,
                )
                "intersect" -> createIntersections(
                    construction,
                    command,
                    outputElements,
                )
                "distance" -> createDistance(
                    construction,
                    command,
                    outputElements[0],
                )
                "vector" -> createTwoPointLine(
                    construction,
                    command,
                    outputElements[0],
                    segment = true,
                    arrow = true,
                )
                "rotate" -> createRotate(
                    construction,
                    command,
                    outputElements[0],
                )
                "dilate" -> createDilate(
                    construction,
                    command,
                    outputElements[0],
                )
                "translate" -> createTranslate(
                    construction,
                    command,
                    outputElements[0],
                )
                "mirror" -> createMirror(
                    construction,
                    command,
                    outputElements[0],
                )
                "circle" -> createCircle(
                    construction,
                    command,
                    outputElements[0],
                )
                "circlearc" -> createCircleArc(
                    construction,
                    command,
                    outputElements[0],
                )
                "ellipse" -> createEllipse(
                    construction,
                    command,
                    outputElements[0],
                )
                "conic" -> createConic(
                    construction,
                    command,
                    outputElements[0],
                )
                "circlesector" -> createCircleSector(
                    construction,
                    command,
                    outputElements[0],
                )
                "linebisector" -> createLineBisector(
                    construction,
                    command,
                    outputElements[0],
                )
                "ray" -> createRay(
                    construction,
                    command,
                    outputElements[0],
                )
                "tangent" -> createTangent(
                    construction,
                    command,
                    outputElements,
                )
                "circumcirclearc" -> createCircumcircleArc(
                    construction,
                    command,
                    outputElements[0],
                )
                "circumcirclesector" -> createCircumcircleSector(
                    construction,
                    command,
                    outputElements[0],
                )
                "semicircle" -> createSemicircle(
                    construction,
                    command,
                    outputElements[0],
                )
                "angle" -> createAngle(
                    construction,
                    command,
                    outputElements[0],
                )
                "angularbisector" -> createAngularBisector(
                    construction,
                    command,
                    outputElements[0],
                )
                "numeric" -> registerSingle(
                    command.outputs[0],
                    createNumeric(
                        construction,
                        outputElements[0],
                    ),
                )
                "midpoint" -> createMidpoint(
                    construction,
                    command,
                    outputElements[0],
                )
                "center" -> createCenter(
                    construction,
                    command,
                    outputElements[0],
                )
                "function" -> registerSingle(
                    command.outputs[0],
                    createFunction(
                        construction,
                        command,
                        outputElements[0],
                    ),
                )
                "slope" -> createSlope(
                    construction,
                    command,
                    outputElements[0],
                )
                "polar" -> createPolar(
                    construction,
                    command,
                    outputElements[0],
                )
                "root" -> createRoots(
                    construction,
                    command,
                )
                "integral" -> createIntegral(
                    construction,
                    command,
                    outputElements[0],
                )
                else -> GMResult.Err(
                    GeogebraReaderError.UnsupportedCommand(
                        command.name,
                    ),
                )
            }
        }

        private fun checkElement(
            construction: GeogebraConstruction,
            name: String,
            known: XmlElement? = null,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            values[name]?.let { return GMResult.Ok(it) }
            if (name in pending) {
                return GMResult.Err(
                    GeogebraReaderError.CyclicElementDependency(
                        pending + name,
                    ),
                )
            }
            pending += name
            val result = when (
                val element = known ?: construction.elements[name]
            ) {
                null -> createScalar(construction, name, name)
                else -> createStandalone(construction, element)
            }
            pending.removeAt(pending.lastIndex)
            if (result is GMResult.Ok) {
                values[name] = result.value
            }
            return result
        }

        private fun createStandalone(
            construction: GeogebraConstruction,
            element: XmlElement,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val name = element.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val type =
                    element.getAttribute(TYPE_ATTRIBUTE)?.lowercase()
            ) {
                "point" -> createPoint(construction, element, null)
                "segment" -> createFreeLine(element, segment = true)
                "line" -> createFreeLine(element, segment = false)
                "vector" -> createStandaloneVector(
                    construction,
                    element,
                )
                "numeric" -> createNumeric(construction, element)
                "function" -> createFunction(
                    construction = construction,
                    command = null,
                    output = element,
                )
                "text" -> createText(construction, element)
                "conic" -> createStandaloneConic(element)
                else -> GMResult.Err(
                    GeogebraReaderError.UnsupportedElement(
                        type = type.orEmpty(),
                        name = name,
                    ),
                )
            }
        }

        private fun createPoint(
            construction: GeogebraConstruction,
            element: XmlElement,
            host: GeometryElement?,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val name = element.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val coordinates = when (
                val result = coordinates(element, name)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val expression = construction.expressions[name]
                ?.getAttribute(EXPRESSION_ATTRIBUTE)
            if (expression != null && host == null) {
                val compiled = when (
                    val result = compileExpression(expression)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val initial = when (
                    val result = compiled.evaluate(board, values)
                ) {
                    is GMResult.Ok ->
                        result.value as?
                            GeogebraExpressionValue.Coordinates
                            ?: return GMResult.Err(
                                GeogebraReaderError.ExpressionFailure(
                                    expression,
                                    GeogebraExpressionError
                                        .ExpectedCoordinates(
                                            result.value.typeName(),
                                        ),
                                ),
                            )
                    is GMResult.Err -> {
                        return GMResult.Err(
                            GeogebraReaderError.ExpressionFailure(
                                expression,
                                result.error,
                            ),
                        )
                    }
                }
                val result = Point.createConstrained(
                    board = board,
                    coordinateFunctions = listOf(
                        compiled.coordinateFunction(
                            board,
                            values,
                            emptyList(),
                            0,
                        ),
                        compiled.coordinateFunction(
                            board,
                            values,
                            emptyList(),
                            1,
                        ),
                    ),
                    name = name,
                    fixed = isFixed(element),
                )
                return pointResult(name, result).also {
                    if (it is GMResult.Ok) {
                        it.value.let { value ->
                            (value as GeogebraReaderValue.Element)
                                .value
                                .let { created ->
                                    (created as Point).setPositionDirectly(
                                        1,
                                        doubleArrayOf(
                                            initial.x,
                                            initial.y,
                                        ),
                                    )
                                }
                        }
                    }
                }
            }
            val static = doubleArrayOf(
                coordinates.x(),
                coordinates.y(),
            )
            if (host != null) {
                return when (
                    val result = Glider.create(
                        board = board,
                        coordinates = static,
                        slideObject = host,
                        name = name,
                        fixed = isFixed(element),
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(
                        GeogebraReaderValue.Element(result.value),
                    )
                    is GMResult.Err -> creationFailure(
                        name,
                        GeogebraCreationError.Glider(result.error),
                    )
                }
            }
            return pointResult(
                name,
                Point.create(
                    board = board,
                    coordinates = static,
                    name = name,
                    fixed = isFixed(element),
                ),
            )
        }

        private fun createFreeLine(
            element: XmlElement,
            segment: Boolean,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val name = element.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val coordinates = element
                .getElementsByTagName(COORDS_TAG)
                .firstOrNull()
                ?: return GMResult.Err(
                    GeogebraReaderError.MissingDocumentPart(
                        "$name/$COORDS_TAG",
                    ),
                )
            val coefficients = doubleArrayOf(
                parseDouble(
                    coordinates.getAttribute(Z_ATTRIBUTE),
                    name,
                    2,
                ),
                parseDouble(
                    coordinates.getAttribute(X_ATTRIBUTE),
                    name,
                    0,
                ),
                parseDouble(
                    coordinates.getAttribute(Y_ATTRIBUTE),
                    name,
                    1,
                ),
            )
            val result = Line.create(
                board = board,
                coefficients = coefficients,
                name = name,
            )
            return when (result) {
                is GMResult.Ok -> {
                    if (segment) {
                        result.value.configureVisibleRange(
                            straightFirst = false,
                            straightLast = false,
                        )
                    }
                    GMResult.Ok(
                        GeogebraReaderValue.Element(result.value),
                    )
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Line(result.error),
                )
            }
        }

        private fun createTwoPointLine(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
            segment: Boolean,
            arrow: Boolean,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val result =
                if (segment || arrow) {
                    Line.createSegment(board, first, second, name = name)
                } else {
                    Line.create(board, first, second, name = name)
                }
            return when (result) {
                is GMResult.Ok -> {
                    if (arrow) {
                        result.value.configureArrow()
                    }
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Line(result.error),
                )
            }
        }

        private fun createLineCommand(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolveElement(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val result =
                if (first is Point && second is Line) {
                    when (
                        val created = ParallelLine.create(
                            board,
                            sourceLine = second,
                            throughPoint = first,
                            name = name,
                        )
                    ) {
                        is GMResult.Ok -> GMResult.Ok(created.value)
                        is GMResult.Err -> return creationFailure(
                            name,
                            GeogebraCreationError.Parallel(
                                created.error,
                            ),
                        )
                    }
                } else {
                    val point1 = first as? Point
                        ?: return argumentType(
                            command.name,
                            0,
                            "point",
                            first,
                        )
                    val point2 = second as? Point
                        ?: return argumentType(
                            command.name,
                            1,
                            "point or line",
                            second,
                        )
                    Line.create(
                        board,
                        point1,
                        point2,
                        name = name,
                    )
                }
            return when (result) {
                is GMResult.Ok -> {
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Line(result.error),
                )
            }
        }

        private fun createNormal(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolveElement(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Normal.create(
                    board = board,
                    firstParent = first,
                    secondParent = second,
                    name = name,
                )
            ) {
                is GMResult.Ok -> {
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Normal(result.error),
                )
            }
        }

        private fun createPolygon(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            outputs: List<XmlElement>,
        ): GMResult<Unit, GeogebraReaderError> {
            if (command.inputs.size < 3) {
                return argumentCount(
                    command.name,
                    "at least 3",
                    command.inputs.size,
                )
            }
            val name = outputs[0]
                .getAttribute(LABEL_ATTRIBUTE)
                .orEmpty()
            val numericCount = command.inputs[2].toDoubleOrNull()
            val result =
                if (
                    command.inputs.size == 3 &&
                    outputs.size != 4 &&
                    numericCount != null
                ) {
                    val first = when (
                        val value = resolvePoint(
                            construction,
                            command.inputs[0],
                            command.name,
                            0,
                        )
                    ) {
                        is GMResult.Ok -> value.value
                        is GMResult.Err -> return value
                    }
                    val second = when (
                        val value = resolvePoint(
                            construction,
                            command.inputs[1],
                            command.name,
                            1,
                        )
                    ) {
                        is GMResult.Ok -> value.value
                        is GMResult.Err -> return value
                    }
                    when (
                        val created = RegularPolygon.create(
                            board = board,
                            point1 = first,
                            point2 = second,
                            numberOfVertices = numericCount,
                            name = name,
                        )
                    ) {
                        is GMResult.Ok -> GMResult.Ok(created.value)
                        is GMResult.Err -> return creationFailure(
                            name,
                            GeogebraCreationError.RegularPolygon(
                                created.error,
                            ),
                        )
                    }
                } else {
                    val points = mutableListOf<Point>()
                    for ((index, input) in command.inputs.withIndex()) {
                        when (
                            val value = resolvePoint(
                                construction,
                                input,
                                command.name,
                                index,
                            )
                        ) {
                            is GMResult.Ok -> points += value.value
                            is GMResult.Err -> return value
                        }
                    }
                    Polygon.create(
                        board = board,
                        vertices = points,
                        name = name,
                    )
                }
            return when (result) {
                is GMResult.Ok -> {
                    val polygon = result.value
                    register(
                        name,
                        GeogebraReaderValue.Element(polygon),
                    )
                    for (
                        index in
                        1 until minOf(outputs.size, polygon.borders.size + 1)
                    ) {
                        val borderName = outputs[index]
                            .getAttribute(LABEL_ATTRIBUTE)
                            .orEmpty()
                        polygon.borders[index - 1].setName(borderName)
                        register(
                            borderName,
                            GeogebraReaderValue.Element(
                                polygon.borders[index - 1],
                            ),
                        )
                    }
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Polygon(result.error),
                )
            }
        }

        private fun createIntersections(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            outputs: List<XmlElement>,
        ): GMResult<Unit, GeogebraReaderError> {
            if (command.inputs.size !in 2..3) {
                return argumentCount(
                    command = command.name,
                    expected = "2 or 3",
                    actual = command.inputs.size,
                )
            }
            val first = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolveElement(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val firstName = outputs[0]
                .getAttribute(LABEL_ATTRIBUTE)
                .orEmpty()
            val firstIndex =
                if (outputs.size == 1) 0.0 else 1.0
            val point = when (
                val result = IntersectionPoint.create(
                    board = board,
                    first = first,
                    second = second,
                    firstIndex =
                        IntersectionIndexSource.Number(firstIndex),
                    name = firstName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    firstName,
                    GeogebraCreationError.Intersection(result.error),
                )
            }
            register(
                firstName,
                GeogebraReaderValue.Element(point),
            )
            if (outputs.size > 1) {
                val secondName = outputs[1]
                    .getAttribute(LABEL_ATTRIBUTE)
                    .orEmpty()
                val other = when (
                    val result = OtherIntersectionPoint.create(
                        board = board,
                        first = first,
                        second = second,
                        excludedPoints = listOf(point),
                        name = secondName,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return creationFailure(
                        secondName,
                        GeogebraCreationError.Intersection(
                            result.error,
                        ),
                    )
                }
                register(
                    secondName,
                    GeogebraReaderValue.Element(other),
                )
            }
            return GMResult.Ok(Unit)
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "distance".
        private fun createDistance(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val midpoint = when (
                val result = MidpointPoint.create(
                    board = board,
                    point1 = first,
                    point2 = second,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    command.name,
                    GeogebraCreationError.Midpoint(result.error),
                )
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val dependencies = mapOf(
                first.id to first,
                second.id to second,
                midpoint.id to midpoint,
            )
            val text = when (
                val result = Text.createConstrained(
                    board = board,
                    coordinateFunctions = listOf(
                        ReaderCoordinateFunction(dependencies) {
                            midpoint.X()
                        },
                        ReaderCoordinateFunction(dependencies) {
                            midpoint.Y()
                        },
                    ),
                    content = "",
                    name = name,
                    parse = false,
                    digits = parsed.decimals,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Text(result.error),
                )
            }
            val scalar = GeogebraScalarValue {
                GMResult.Ok(first.Dist(second))
            }
            text.setDynamicText(parse = false) {
                GMResult.Ok(
                    "${first.name}${second.name} = " +
                        formatMeasurement(first.Dist(second)),
                )
            }
            register(
                name,
                GeogebraReaderValue.ElementScalar(text, scalar),
            )
            return GMResult.Ok(Unit)
        }

        private fun createRotate(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 3)?.let { return it }
            val source = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val degrees = when (
                val result = resolveScalar(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val center = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[2],
                    command.name,
                    2,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val transformation = when (
                val result = Transformation.createRotation(
                    board = board,
                    angle = dynamicParameter {
                        degrees.evaluateValue() * PI / 180.0
                    },
                    center = center,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Transformation(result.error),
                )
            }
            return registerSingle(
                name,
                pointResult(
                    name,
                    Point.create(
                        board = board,
                        basePoint = source,
                        transformations = listOf(transformation),
                        name = name,
                    ),
                ),
            )
        }

        private fun createDilate(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 3)?.let { return it }
            val source = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val factor = when (
                val result = resolveScalar(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val center = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[2],
                    command.name,
                    2,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val scale = when (
                val result = Transformation.create(
                    board = board,
                    type = "scale",
                    parameters = listOf(
                        dynamicParameter(factor::evaluateValue),
                        dynamicParameter(factor::evaluateValue),
                    ),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Transformation(result.error),
                )
            }
            val translation = when (
                val result = Transformation.create(
                    board = board,
                    type = "translate",
                    parameters = listOf(
                        dynamicParameter {
                            (1.0 - factor.evaluateValue()) * center.X()
                        },
                        dynamicParameter {
                            (1.0 - factor.evaluateValue()) * center.Y()
                        },
                    ),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Transformation(result.error),
                )
            }
            return registerSingle(
                name,
                pointResult(
                    name,
                    Point.create(
                        board = board,
                        basePoint = source,
                        transformations = listOf(scale, translation),
                        name = name,
                    ),
                ),
            )
        }

        private fun createTranslate(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val source = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val vector = when (
                val result = resolveLine(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val transformation = when (
                val result = Transformation.create(
                    board = board,
                    type = "translate",
                    parameters = listOf(
                        dynamicParameter {
                            vector.point2.X() - vector.point1.X()
                        },
                        dynamicParameter {
                            vector.point2.Y() - vector.point1.Y()
                        },
                    ),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Transformation(result.error),
                )
            }
            return registerSingle(
                name,
                pointResult(
                    name,
                    Point.create(
                        board = board,
                        basePoint = source,
                        transformations = listOf(transformation),
                        name = name,
                    ),
                ),
            )
        }

        private fun createMirror(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val source = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val mirror = when (
                val result = resolveElement(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val result = when (mirror) {
                is Point -> PointReflections.createMirrorPoint(
                    board,
                    source,
                    mirror,
                    name = name,
                )
                is Line -> PointReflections.createReflection(
                    board,
                    source,
                    mirror,
                    name = name,
                )
                else -> {
                    return argumentType(
                        command.name,
                        1,
                        "point or line",
                        mirror,
                    )
                }
            }
            return when (result) {
                is GMResult.Ok -> {
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.PointReflection(result.error),
                )
            }
        }

        private fun createCircle(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val center = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val secondValue = when (
                val result = resolveValue(
                    construction,
                    command.inputs[1],
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val result = when (secondValue) {
                is GeogebraReaderValue.Element -> {
                    val point = secondValue.value as? Point
                        ?: return argumentType(
                            command.name,
                            1,
                            "point or scalar",
                            secondValue.value,
                        )
                    Circle.create(board, center, point, name = name)
                }
                is GeogebraReaderValue.ElementScalar -> Circle.create(
                    board = board,
                    center = center,
                    radius = secondValue.scalar.evaluateValue(),
                    id = "",
                    name = name,
                )
                is GeogebraReaderValue.Scalar -> Circle.create(
                    board = board,
                    center = center,
                    radius = secondValue.value.evaluateValue(),
                    id = "",
                    name = name,
                )
            }
            return when (result) {
                is GMResult.Ok -> {
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Circle(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "circlearc".
        private fun createCircleArc(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            val points = when (
                val result = resolvePoints(
                    construction = construction,
                    command = command,
                    expectedCount = 3,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Arc.create(
                    board = board,
                    center = points[0],
                    radiuspoint = points[1],
                    anglepoint = points[2],
                    name = name,
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Arc(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "ellipse".
        private fun createEllipse(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 3)?.let { return it }
            val first = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val third = when (
                val result = resolveValue(
                    construction,
                    command.inputs[2],
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val result = when (third) {
                is GeogebraReaderValue.Element -> {
                    val point = third.value as? Point
                    if (point != null) {
                        Ellipse.create(
                            board = board,
                            focus1 = first,
                            focus2 = second,
                            pointOnEllipse = point,
                            name = name,
                        )
                    } else {
                        val term = when (
                            val scalar = scalarCoordinateFunction(
                                command = command.name,
                                index = 2,
                                value = third,
                                multiplier = 2.0,
                            )
                        ) {
                            is GMResult.Ok -> scalar.value
                            is GMResult.Err -> return scalar
                        }
                        Ellipse.create(
                            board = board,
                            focus1 = first,
                            focus2 = second,
                            majorAxisTerm = term,
                            name = name,
                        )
                    }
                }
                is GeogebraReaderValue.ElementScalar,
                is GeogebraReaderValue.Scalar,
                -> {
                    val term = when (
                        val scalar = scalarCoordinateFunction(
                            command = command.name,
                            index = 2,
                            value = third,
                            multiplier = 2.0,
                        )
                    ) {
                        is GMResult.Ok -> scalar.value
                        is GMResult.Err -> return scalar
                    }
                    Ellipse.create(
                        board = board,
                        focus1 = first,
                        focus2 = second,
                        majorAxisTerm = term,
                        name = name,
                    )
                }
            }
            return when (result) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Ellipse(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "conic".
        private fun createConic(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val result =
                if (command.inputs.size == 5) {
                    val points = when (
                        val resolved = resolvePoints(
                            construction = construction,
                            command = command,
                            expectedCount = 5,
                        )
                    ) {
                        is GMResult.Ok -> resolved.value
                        is GMResult.Err -> return resolved
                    }
                    Conic.create(
                        board = board,
                        point1 = points[0],
                        point2 = points[1],
                        point3 = points[2],
                        point4 = points[3],
                        point5 = points[4],
                        name = name,
                    )
                } else {
                    val coefficients = when (
                        val resolved = conicCoefficients(
                            element = output,
                            command = command.name,
                        )
                    ) {
                        is GMResult.Ok -> resolved.value
                        is GMResult.Err -> return resolved
                    }
                    Conic.create(
                        board = board,
                        coefficients = coefficients,
                        name = name,
                    )
                }
            return when (result) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Conic(result.error),
                )
            }
        }

        private fun createStandaloneConic(
            element: XmlElement,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val name = element.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val coefficients = when (
                val result = conicCoefficients(
                    element = element,
                    command = name,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (
                val result = Conic.create(
                    board = board,
                    coefficients = coefficients,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    GeogebraReaderValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Conic(result.error),
                )
            }
        }

        private fun conicCoefficients(
            element: XmlElement,
            command: String,
        ): GMResult<DoubleArray, GeogebraReaderError> {
            val matrix = element
                .getElementsByTagName(MATRIX_TAG)
                .firstOrNull()
                ?: return GMResult.Err(
                    GeogebraReaderError.MissingDocumentPart(
                        "$command/$MATRIX_TAG",
                    ),
                )
            val coefficients = DoubleArray(matrix.attributes.length)
            for ((index, attribute) in
                matrix.attributes.withIndex()
            ) {
                coefficients[index] =
                    attribute.value.toDoubleOrNull()
                        ?: return GMResult.Err(
                            GeogebraReaderError.InvalidNumber(
                                command = command,
                                index = index,
                                value = attribute.value,
                            ),
                        )
            }
            return GMResult.Ok(coefficients)
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "circlesector".
        private fun createCircleSector(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            val points = when (
                val result = resolvePoints(
                    construction = construction,
                    command = command,
                    expectedCount = 3,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Sector.create(
                    board = board,
                    center = points[0],
                    radiuspoint = points[1],
                    anglepoint = points[2],
                    name = name,
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Sector(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "circumcirclearc".
        private fun createCircumcircleArc(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            val points = when (
                val result = resolvePoints(
                    construction = construction,
                    command = command,
                    expectedCount = 3,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Arc.createCircumcircleArc(
                    board = board,
                    point1 = points[0],
                    point2 = points[1],
                    point3 = points[2],
                    name = name,
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Arc(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "circumcirclesector".
        private fun createCircumcircleSector(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            val points = when (
                val result = resolvePoints(
                    construction = construction,
                    command = command,
                    expectedCount = 3,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Sector.createCircumcircleSector(
                    board = board,
                    point1 = points[0],
                    point2 = points[1],
                    point3 = points[2],
                    name = name,
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Sector(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "semicircle".
        private fun createSemicircle(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            val points = when (
                val result = resolvePoints(
                    construction = construction,
                    command = command,
                    expectedCount = 2,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Arc.createSemicircle(
                    board = board,
                    point1 = points[0],
                    point2 = points[1],
                    name = name,
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Arc(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "angle".
        private fun createAngle(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            val points = when (
                val result = resolvePoints(
                    construction = construction,
                    command = command,
                    expectedCount = 3,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Sector.createAngle(
                    board = board,
                    first = points[0],
                    vertex = points[1],
                    third = points[2],
                    name = name,
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Sector(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "angularbisector".
        private fun createAngularBisector(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            val points = when (
                val result = resolvePoints(
                    construction = construction,
                    command = command,
                    expectedCount = 3,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = BisectorLine.create(
                    board = board,
                    point1 = points[0],
                    vertex = points[1],
                    point3 = points[2],
                    name = name,
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Bisector(result.error),
                )
            }
        }

        private fun createLineBisector(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolveElement(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val sourceLine: Line
            val firstPoint: Point
            val secondPoint: Point
            if (first is Point && second is Point) {
                firstPoint = first
                secondPoint = second
                sourceLine = when (
                    val result = Line.create(
                        board,
                        first,
                        second,
                        name = "",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return creationFailure(
                        command.name,
                        GeogebraCreationError.Line(result.error),
                    )
                }
            } else {
                sourceLine = first as? Line
                    ?: return argumentType(
                        command.name,
                        0,
                        "line or point",
                        first,
                    )
                firstPoint = sourceLine.point1
                secondPoint = sourceLine.point2
            }
            val midpoint = when (
                val result = MidpointPoint.create(
                    board,
                    firstPoint,
                    secondPoint,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    command.name,
                    GeogebraCreationError.Midpoint(result.error),
                )
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result =
                    com.swithun.jsxgraph.core.base.PerpendicularLine
                        .create(
                            board,
                            sourceLine,
                            midpoint,
                            name = name,
                        )
            ) {
                is GMResult.Ok -> {
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Orthogonal(
                        result.error,
                    ),
                )
            }
        }

        private fun createRay(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Line.create(
                    board,
                    first,
                    second,
                    name = name,
                )
            ) {
                is GMResult.Ok -> {
                    result.value.configureVisibleRange(
                        straightFirst = true,
                        straightLast = false,
                    )
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Line(result.error),
                )
            }
        }

        private fun createTangent(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            outputs: List<XmlElement>,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolveElement(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = outputs[0]
                .getAttribute(LABEL_ATTRIBUTE)
                .orEmpty()
            return when (
                val result = Tangent.create(
                    board = board,
                    firstParent = first,
                    secondParent = second,
                    name = name,
                )
            ) {
                is GMResult.Ok -> {
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Tangent(result.error),
                )
            }
        }

        private fun createMidpoint(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolvePoint(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = MidpointPoint.create(
                    board,
                    first,
                    second,
                    name = name,
                )
            ) {
                is GMResult.Ok -> {
                    register(
                        name,
                        GeogebraReaderValue.Element(result.value),
                    )
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Midpoint(result.error),
                )
            }
        }

        private fun createCenter(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 1)?.let { return it }
            val circle = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value as? Circle
                    ?: return argumentType(
                        command.name,
                        0,
                        "circle",
                        result.value,
                    )
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return registerSingle(
                name,
                pointResult(
                    name,
                    Point.createConstrained(
                        board = board,
                        coordinateFunctions = listOf(
                            ReaderCoordinateFunction(
                                dependencies =
                                    mapOf(circle.id to circle),
                            ) {
                                circle.center.X()
                            },
                            ReaderCoordinateFunction(
                                dependencies =
                                    mapOf(circle.id to circle),
                            ) {
                                circle.center.Y()
                            },
                        ),
                        name = name,
                    ),
                ),
            )
        }

        private fun createFunction(
            construction: GeogebraConstruction,
            command: GeogebraCommand?,
            output: XmlElement,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val raw = construction.expressions[name]
                ?.getAttribute(EXPRESSION_ATTRIBUTE)
                ?: command?.inputs?.firstOrNull()
                ?: return GMResult.Err(
                    GeogebraReaderError.MissingDocumentPart(
                        "$name/$EXPRESSION_TAG",
                    ),
                )
            val definition =
                CompiledGeogebraExpression.functionDefinition(raw)
            val compiled = when (
                val result = compileExpression(definition.expression)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val yTerm = compiled.coordinateFunction(
                board = board,
                values = values,
                variableNames = definition.parameters,
                component = null,
            )
            val boundingBox = board.getBoundingBox()
            return when (
                val result = Curve.createFunctionGraph(
                    board = board,
                    yTerm = yTerm,
                    minimumTerm =
                        JessieCodeNumericCoordinateFunction(
                            command?.inputs
                                ?.getOrNull(1)
                                ?.toDoubleOrNull()
                                ?: boundingBox[0],
                        ),
                    maximumTerm =
                        JessieCodeNumericCoordinateFunction(
                            command?.inputs
                                ?.getOrNull(2)
                                ?.toDoubleOrNull()
                                ?: boundingBox[2],
                        ),
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    GeogebraReaderValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Curve(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "slope".
        private fun createSlope(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 1)?.let { return it }
            val line = when (
                val result = resolveLine(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val resolvedProperties = when (
                val result = properties(output, name)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val slopeWidth = resolvedProperties.slopeWidth
                ?.toIntOrNull()
                ?.takeIf { it != 0 }
                ?: 1
            val start = line.glider ?: line.point1
            val startDependencies = mapOf(start.id to start)
            val baseEnd = when (
                val result = Point.createConstrained(
                    board = board,
                    coordinateFunctions = listOf(
                        ReaderCoordinateFunction(startDependencies) {
                            start.X() + slopeWidth
                        },
                        ReaderCoordinateFunction(startDependencies) {
                            start.Y()
                        },
                    ),
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Point(result.error),
                )
            }
            val baseLine = when (
                val result = Line.createSegment(
                    board = board,
                    point1 = start,
                    point2 = baseEnd,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Line(result.error),
                )
            }
            val normal = when (
                val result = Normal.create(
                    board = board,
                    firstParent = baseLine,
                    secondParent = baseEnd,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Normal(result.error),
                )
            }
            val top = when (
                val result = IntersectionPoint.create(
                    board = board,
                    first = line,
                    second = normal,
                    firstIndex = IntersectionIndexSource.Number(0.0),
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Intersection(result.error),
                )
            }
            val midpoint = when (
                val result = MidpointPoint.create(
                    board = board,
                    point1 = baseEnd,
                    point2 = top,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Midpoint(result.error),
                )
            }
            when (
                val result = Polygon.create(
                    board = board,
                    vertices = listOf(start, baseEnd, top),
                    name = "",
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Polygon(result.error),
                )
            }
            val dependencies = mapOf(
                line.id to line,
                midpoint.id to midpoint,
            )
            val text = when (
                val result = Text.createConstrained(
                    board = board,
                    coordinateFunctions = listOf(
                        ReaderCoordinateFunction(dependencies) {
                            midpoint.X()
                        },
                        ReaderCoordinateFunction(dependencies) {
                            midpoint.Y()
                        },
                    ),
                    content = "",
                    name = name,
                    parse = false,
                    digits = parsed.decimals,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Text(result.error),
                )
            }
            val scalar = GeogebraScalarValue {
                GMResult.Ok(line.Slope())
            }
            text.setDynamicText(parse = false) {
                GMResult.Ok(
                    (if (slopeWidth > 1) "$slopeWidth " else "") +
                        "$name = " +
                        formatMeasurement(slopeWidth * line.Slope()),
                )
            }
            register(
                name,
                GeogebraReaderValue.ElementScalar(text, scalar),
            )
            return GMResult.Ok(Unit)
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "polar".
        private fun createPolar(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 2)?.let { return it }
            val first = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = resolveElement(
                    construction,
                    command.inputs[1],
                    command.name,
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Tangent.createPolarLine(
                    board = board,
                    firstParent = first,
                    secondParent = second,
                    name = name,
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Tangent(result.error),
                )
            }
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "root".
        private fun createRoots(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 1)?.let { return it }
            val source = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value as? Curve
                    ?: return argumentType(
                        command.name,
                        0,
                        "curve",
                        result.value,
                    )
                is GMResult.Err -> return result
            }
            val dependencies = mapOf(source.id to source)
            for ((index, name) in command.outputs.withIndex()) {
                val point = when (
                    val result = resolvePoint(
                        construction = construction,
                        raw = name,
                        command = command.name,
                        index = index,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val initialX = point.X()
                point.replaceCoordinateFunctions(
                    listOf(
                        ReaderCoordinateFunction(dependencies) {
                            Numerics.root(source::Y, initialX)
                        },
                        JessieCodeNumericCoordinateFunction(0.0),
                    ),
                )
                point.prepareUpdate().update()
                register(name, GeogebraReaderValue.Element(point))
            }
            return GMResult.Ok(Unit)
        }

        // JSXGraph 1.13.3:
        // src/reader/geogebra.js -> writeElement "integral".
        private fun createIntegral(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            output: XmlElement,
        ): GMResult<Unit, GeogebraReaderError> {
            requireCount(command, 3)?.let { return it }
            val source = when (
                val result = resolveElement(
                    construction,
                    command.inputs[0],
                    command.name,
                    0,
                )
            ) {
                is GMResult.Ok -> result.value as? Curve
                    ?: return argumentType(
                        command.name,
                        0,
                        "curve",
                        result.value,
                    )
                is GMResult.Err -> return result
            }
            val left = when (
                val result = scalarCoordinateFunction(
                    construction = construction,
                    raw = command.inputs[1],
                    command = command.name,
                    index = 1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val right = when (
                val result = scalarCoordinateFunction(
                    construction = construction,
                    raw = command.inputs[2],
                    command = command.name,
                    index = 2,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val name = output.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            return when (
                val result = Integral.create(
                    board = board,
                    interval = IntegralBoundary.Dynamic(left) to
                        IntegralBoundary.Dynamic(right),
                    source = source,
                    attributes = IntegralAttributes(
                        name = name,
                        label = IntegralLabelAttributes(
                            digits = parsed.decimals,
                        ),
                    ),
                )
            ) {
                is GMResult.Ok -> registerElement(name, result.value)
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Integral(result.error),
                )
            }
        }

        private fun createStandaloneVector(
            construction: GeogebraConstruction,
            element: XmlElement,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val name = element.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val coordinates = when (
                val result = coordinates(element, name)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val startPoint = element
                .getElementsByTagName(START_POINT_TAG)
                .firstOrNull()
                ?.getAttribute(EXPRESSION_ATTRIBUTE)
                ?.let { reference ->
                    when (
                        val result = resolvePoint(
                            construction,
                            reference,
                            name,
                            0,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                }
            val first = startPoint ?: when (
                val result = Point.create(
                    board,
                    doubleArrayOf(0.0, 0.0),
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Point(result.error),
                )
            }
            val second = when (
                val result = Point.create(
                    board,
                    doubleArrayOf(
                        coordinates.x(),
                        coordinates.y(),
                    ),
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    name,
                    GeogebraCreationError.Point(result.error),
                )
            }
            return when (
                val result =
                    Line.createSegment(board, first, second, name = name)
            ) {
                is GMResult.Ok -> {
                    result.value.configureArrow()
                    GMResult.Ok(
                        GeogebraReaderValue.Element(result.value),
                    )
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Line(result.error),
                )
            }
        }

        private fun createNumeric(
            construction: GeogebraConstruction,
            element: XmlElement,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val name = element.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val slider = element
                .getElementsByTagName(SLIDER_TAG)
                .firstOrNull()
            if (slider == null) {
                val expression = construction.expressions[name]
                    ?.getAttribute(EXPRESSION_ATTRIBUTE)
                if (expression != null) {
                    return createScalar(
                        construction,
                        name,
                        expression,
                    )
                }
                val value = element
                    .getElementsByTagName(VALUE_TAG)
                    .firstOrNull()
                    ?.getAttribute(VALUE_ATTRIBUTE)
                    ?.toDoubleOrNull()
                    ?: Double.NaN
                return GMResult.Ok(
                    GeogebraReaderValue.Scalar(
                        GeogebraScalarValue {
                            GMResult.Ok(value)
                        },
                    ),
                )
            }
            var x = parseDouble(
                slider.getAttribute(X_ATTRIBUTE),
                name,
                0,
            )
            var y = parseDouble(
                slider.getAttribute(Y_ATTRIBUTE),
                name,
                1,
            )
            var length = parseDouble(
                slider.getAttribute(WIDTH_ATTRIBUTE),
                name,
                2,
            )
            val absolute =
                slider.getAttribute(ABSOLUTE_SCREEN_LOCATION_ATTRIBUTE) ==
                    "true"
            if (absolute) {
                val coordinates =
                    com.swithun.jsxgraph.core.base.Coords(
                        method = 2,
                        coordinates = doubleArrayOf(x, y),
                        board = board,
                    )
                x = coordinates.usrCoords[1]
                y = coordinates.usrCoords[2]
            }
            val horizontal =
                slider.getAttribute(HORIZONTAL_ATTRIBUTE) == "true"
            if (absolute) {
                length /=
                    if (horizontal) board.unitX else board.unitY
            }
            val end = if (horizontal) {
                doubleArrayOf(x + length, y)
            } else {
                doubleArrayOf(x, y + length)
            }
            val start = element
                .getElementsByTagName(VALUE_TAG)
                .firstOrNull()
                ?.getAttribute(VALUE_ATTRIBUTE)
                ?.toDoubleOrNull()
                ?: Double.NaN
            val minimum = parseDouble(
                slider.getAttribute(MIN_ATTRIBUTE),
                name,
                3,
            )
            val maximum = parseDouble(
                slider.getAttribute(MAX_ATTRIBUTE),
                name,
                4,
            )
            val snapWidth = element
                .getElementsByTagName(ANIMATION_TAG)
                .firstOrNull()
                ?.getAttribute(STEP_ATTRIBUTE)
                ?.toDoubleOrNull()
                ?: -1.0
            return when (
                val result = Slider.create(
                    board = board,
                    startCoordinates = doubleArrayOf(x, y),
                    endCoordinates = end,
                    range = doubleArrayOf(minimum, start, maximum),
                    attributes = SliderAttributes(
                        name = name,
                        snapWidth = snapWidth,
                        withTicks = false,
                    ),
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    GeogebraReaderValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Slider(result.error),
                )
            }
        }

        private fun createText(
            construction: GeogebraConstruction,
            element: XmlElement,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val name = element.getAttribute(LABEL_ATTRIBUTE).orEmpty()
            val coordinates = when (
                val result = coordinates(element, name)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val expression = construction.expressions[name]
                ?.getAttribute(EXPRESSION_ATTRIBUTE)
                ?: ""
            val compiled = when (
                val result = compileExpression(expression)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val attributes = properties(element, name)
            val resolved = when (attributes) {
                is GMResult.Ok -> attributes.value
                is GMResult.Err -> return attributes
            }
            val created = when (val source = coordinates.source) {
                is GeogebraCoordinateSource.Static -> Text.create(
                    board = board,
                    coordinates = doubleArrayOf(
                        source.x(),
                        source.y(),
                    ),
                    content = "",
                    name = name,
                    fixed = resolved.fixed == "true",
                    visible = resolved.visible ?: true,
                    strokeColor = resolved.strokeColor,
                    parse = false,
                    digits = parsed.decimals,
                )
                is GeogebraCoordinateSource.PointAnchor -> {
                    val dependencies = mapOf(
                        source.point.id to source.point,
                    )
                    Text.createConstrained(
                        board = board,
                        coordinateFunctions = listOf(
                            ReaderCoordinateFunction(dependencies) {
                                source.x()
                            },
                            ReaderCoordinateFunction(dependencies) {
                                source.y()
                            },
                        ),
                        content = "",
                        name = name,
                        parse = false,
                        digits = parsed.decimals,
                    )
                }
            }
            return when (val result = created) {
                is GMResult.Ok -> {
                    val text = result.value
                    text.setDynamicText(parse = false) {
                        when (
                            val evaluated =
                                compiled.evaluate(board, values)
                        ) {
                            is GMResult.Ok -> GMResult.Ok(
                                displayText(evaluated.value),
                            )
                            is GMResult.Err -> GMResult.Err(
                                TextError.ContentExpressionResult(
                                    expressionIndex = 0,
                                    actualType =
                                        evaluated.error.toString(),
                                ),
                            )
                        }
                    }
                    text.addParentsFromJCFunctions(
                        listOf(
                            compiled.coordinateFunction(
                                board = board,
                                values = values,
                                variableNames = emptyList(),
                                component = null,
                            ),
                        ),
                    )
                    text.prepareUpdate().update()
                    GMResult.Ok(GeogebraReaderValue.Element(text))
                }
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Text(result.error),
                )
            }
        }

        private fun createScalar(
            construction: GeogebraConstruction,
            name: String,
            source: String,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            val actualSource = construction.expressions[name]
                ?.getAttribute(EXPRESSION_ATTRIBUTE)
                ?: source
            val compiled = when (
                val result = compileExpression(actualSource)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                GeogebraReaderValue.Scalar(
                    GeogebraScalarValue {
                        when (
                            val result =
                                compiled.evaluate(board, values)
                        ) {
                            is GMResult.Ok -> {
                                val scalar = result.value as?
                                    GeogebraExpressionValue.Scalar
                                    ?: return@GeogebraScalarValue GMResult.Err(
                                        GeogebraExpressionError
                                            .ExpectedScalar(
                                                result.value.typeName(),
                                            ),
                                    )
                                GMResult.Ok(scalar.value)
                            }
                            is GMResult.Err -> result
                        }
                    },
                ),
            )
        }

        private fun compileExpression(
            source: String,
        ): GMResult<
            CompiledGeogebraExpression,
            GeogebraReaderError,
            > =
            when (
                val result = CompiledGeogebraExpression.compile(
                    source,
                    limits.expression,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(result.value)
                is GMResult.Err -> GMResult.Err(
                    GeogebraReaderError.ExpressionFailure(
                        source,
                        result.error,
                    ),
                )
            }

        private fun coordinates(
            element: XmlElement,
            name: String,
        ): GMResult<
            GeogebraElementCoordinates,
            GeogebraReaderError,
            > =
            when (
                val result =
                    GeogebraProperties.coordinates(board, element)
            ) {
                is GMResult.Ok -> GMResult.Ok(result.value)
                is GMResult.Err -> GMResult.Err(
                    GeogebraReaderError.PropertyFailure(
                        name,
                        result.error,
                    ),
                )
            }

        private fun properties(
            element: XmlElement,
            name: String,
        ): GMResult<
            GeogebraElementAttributes,
            GeogebraReaderError,
            > =
            GMResult.Ok(
                GeogebraProperties.visualProperties(
                    data = element,
                    attributes = GeogebraProperties.colorProperties(
                        data = element,
                        attributes = GeogebraElementAttributes(),
                    ),
                ),
            )

        private fun resolveValue(
            construction: GeogebraConstruction,
            raw: String,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> {
            values[raw]?.let { return GMResult.Ok(it) }
            construction.elements[raw]?.let {
                return checkElement(construction, raw, it)
            }
            raw.toDoubleOrNull()?.let { value ->
                return GMResult.Ok(
                    GeogebraReaderValue.Scalar(
                        GeogebraScalarValue {
                            GMResult.Ok(value)
                        },
                    ),
                )
            }
            return createScalar(construction, raw, raw)
        }

        private fun resolveElement(
            construction: GeogebraConstruction,
            raw: String,
            command: String,
            index: Int,
        ): GMResult<GeometryElement, GeogebraReaderError> {
            val value = when (
                val result = resolveValue(construction, raw)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (value) {
                is GeogebraReaderValue.Element ->
                    GMResult.Ok(value.value)
                is GeogebraReaderValue.ElementScalar ->
                    GMResult.Ok(value.element)
                is GeogebraReaderValue.Scalar ->
                    argumentType(
                        command,
                        index,
                        "geometry element",
                        value,
                    )
            }
        }

        private fun resolvePoint(
            construction: GeogebraConstruction,
            raw: String,
            command: String,
            index: Int,
        ): GMResult<Point, GeogebraReaderError> {
            val element = when (
                val result = resolveElement(
                    construction,
                    raw,
                    command,
                    index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return (element as? Point)?.let { GMResult.Ok(it) }
                ?: argumentType(
                    command,
                    index,
                    "point",
                    element,
                )
        }

        private fun resolvePoints(
            construction: GeogebraConstruction,
            command: GeogebraCommand,
            expectedCount: Int,
        ): GMResult<List<Point>, GeogebraReaderError> {
            requireCount(command, expectedCount)?.let { return it }
            val points = mutableListOf<Point>()
            for ((index, input) in command.inputs.withIndex()) {
                when (
                    val result = resolvePoint(
                        construction = construction,
                        raw = input,
                        command = command.name,
                        index = index,
                    )
                ) {
                    is GMResult.Ok -> points += result.value
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(points)
        }

        private fun resolveLine(
            construction: GeogebraConstruction,
            raw: String,
            command: String,
            index: Int,
        ): GMResult<Line, GeogebraReaderError> {
            val element = when (
                val result = resolveElement(
                    construction,
                    raw,
                    command,
                    index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return (element as? Line)?.let { GMResult.Ok(it) }
                ?: argumentType(
                    command,
                    index,
                    "line",
                    element,
                )
        }

        private fun resolveScalar(
            construction: GeogebraConstruction,
            raw: String,
            command: String,
            index: Int,
        ): GMResult<GeogebraScalarValue, GeogebraReaderError> {
            val value = when (
                val result = resolveValue(construction, raw)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (value) {
                is GeogebraReaderValue.Scalar ->
                    GMResult.Ok(value.value)
                is GeogebraReaderValue.ElementScalar ->
                    GMResult.Ok(value.scalar)
                is GeogebraReaderValue.Element -> {
                    val slider = value.value as? Slider
                        ?: return argumentType(
                            command,
                            index,
                            "scalar",
                            value.value,
                        )
                    GMResult.Ok(
                        GeogebraScalarValue {
                            GMResult.Ok(slider.Value())
                        },
                    )
                }
            }
        }

        private fun scalarCoordinateFunction(
            construction: GeogebraConstruction,
            raw: String,
            command: String,
            index: Int,
            multiplier: Double = 1.0,
        ): GMResult<JessieCodeCoordinateFunction, GeogebraReaderError> {
            val value = when (
                val result = resolveValue(construction, raw)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return scalarCoordinateFunction(
                command = command,
                index = index,
                value = value,
                multiplier = multiplier,
            )
        }

        private fun scalarCoordinateFunction(
            command: String,
            index: Int,
            value: GeogebraReaderValue,
            multiplier: Double = 1.0,
        ): GMResult<JessieCodeCoordinateFunction, GeogebraReaderError> {
            val scalar: GeogebraScalarValue
            val dependencies: Map<String, GeometryElement>
            when (value) {
                is GeogebraReaderValue.Scalar -> {
                    scalar = value.value
                    dependencies = emptyMap()
                }
                is GeogebraReaderValue.ElementScalar -> {
                    scalar = value.scalar
                    dependencies = mapOf(
                        value.element.id to value.element,
                    )
                }
                is GeogebraReaderValue.Element -> {
                    val slider = value.value as? Slider
                        ?: return argumentType(
                            command,
                            index,
                            "scalar",
                            value.value,
                        )
                    scalar = GeogebraScalarValue {
                        GMResult.Ok(slider.Value())
                    }
                    dependencies = mapOf(slider.id to slider)
                }
            }
            return GMResult.Ok(
                ReaderCoordinateFunction(dependencies) {
                    multiplier * scalar.evaluateValue()
                },
            )
        }

        private fun pointResult(
            name: String,
            result: GMResult<Point, PointError>,
        ): GMResult<GeogebraReaderValue, GeogebraReaderError> =
            when (result) {
                is GMResult.Ok -> GMResult.Ok(
                    GeogebraReaderValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    name,
                    GeogebraCreationError.Point(result.error),
                )
            }

        private fun registerSingle(
            name: String,
            result: GMResult<
                GeogebraReaderValue,
                GeogebraReaderError,
                >,
        ): GMResult<Unit, GeogebraReaderError> =
            when (result) {
                is GMResult.Ok -> {
                    register(name, result.value)
                    GMResult.Ok(Unit)
                }
                is GMResult.Err -> result
            }

        private fun register(
            name: String,
            value: GeogebraReaderValue,
        ) {
            values[name] = value
        }

        private fun registerElement(
            name: String,
            element: GeometryElement,
        ): GMResult<Unit, GeogebraReaderError> {
            register(name, GeogebraReaderValue.Element(element))
            return GMResult.Ok(Unit)
        }

        private fun objectLimit():
            GMResult<Unit, GeogebraReaderError> {
            val requested = board.objects.size - initialObjectCount
            return if (requested > limits.maxObjects) {
                GMResult.Err(
                    GeogebraReaderError.ObjectLimitExceeded(
                        limits.maxObjects,
                        requested,
                    ),
                )
            } else {
                GMResult.Ok(Unit)
            }
        }

        private fun requireCount(
            command: GeogebraCommand,
            expected: Int,
        ): GMResult.Err<GeogebraReaderError>? =
            if (command.inputs.size == expected) {
                null
            } else {
                GMResult.Err(
                    GeogebraReaderError.ArgumentCount(
                        command.name,
                        expected.toString(),
                        command.inputs.size,
                    ),
                )
            }

        private fun <T> argumentCount(
            command: String,
            expected: String,
            actual: Int,
        ): GMResult<T, GeogebraReaderError> =
            GMResult.Err(
                GeogebraReaderError.ArgumentCount(
                    command,
                    expected,
                    actual,
                ),
            )

        private fun <T> argumentType(
            command: String,
            index: Int,
            expected: String,
            actual: GeometryElement,
        ): GMResult<T, GeogebraReaderError> =
            GMResult.Err(
                GeogebraReaderError.ArgumentType(
                    command,
                    index,
                    expected,
                    actual.elType.ifEmpty { "geometry element" },
                ),
            )

        private fun <T> argumentType(
            command: String,
            index: Int,
            expected: String,
            actual: GeogebraReaderValue,
        ): GMResult<T, GeogebraReaderError> =
            GMResult.Err(
                GeogebraReaderError.ArgumentType(
                    command,
                    index,
                    expected,
                    when (actual) {
                        is GeogebraReaderValue.Element ->
                            actual.value.elType.ifEmpty {
                                "geometry element"
                            }
                        is GeogebraReaderValue.ElementScalar ->
                            "${actual.element.elType.ifEmpty {
                                "geometry element"
                            }}/scalar"
                        is GeogebraReaderValue.Scalar -> "scalar"
                    },
                ),
            )

        private fun <T> creationFailure(
            name: String,
            cause: GeogebraCreationError,
        ): GMResult<T, GeogebraReaderError> =
            GMResult.Err(
                GeogebraReaderError.CreationFailed(name, cause),
            )

        private fun parseDouble(
            value: String?,
            command: String,
            index: Int,
        ): Double =
            value?.toDoubleOrNull() ?: Double.NaN

        private fun isFixed(element: XmlElement): Boolean =
            element.getElementsByTagName(FIX_TAG)
                .firstOrNull()
                ?.getAttribute(VALUE_ATTRIBUTE) == "true"

        private fun dynamicParameter(
            value: () -> Double,
        ): TransformationParameter =
            TransformationParameter.Dynamic(
                TransformationDynamicParameter {
                    GMResult.Ok(value())
                },
            )

        private fun formatMeasurement(value: Double): String =
            if (value == 0.0) {
                "0"
            } else {
                JsNumberFormat.fixed(value, parsed.decimals)
                    .trimEnd('0')
                    .trimEnd('.')
            }

        private fun displayText(
            value: GeogebraExpressionValue,
        ): String =
            when (value) {
                is GeogebraExpressionValue.Scalar ->
                    formatMeasurement(value.value)
                is GeogebraExpressionValue.Coordinates ->
                    "(${formatMeasurement(value.x)}, " +
                        "${formatMeasurement(value.y)})"
                is GeogebraExpressionValue.Text -> value.value
                is GeogebraExpressionValue.Flag ->
                    value.value.toString()
            }
    }

    private fun rollback(
        board: Board,
        existingIds: Set<String>,
        existingNames: Map<String, GeometryElement>,
        previousBoundingBox: DoubleArray,
    ) {
        board.removeObjects(
            board.objects
                .filterKeys { it !in existingIds }
                .values
                .toList()
                .asReversed(),
        )
        board.elementsByName.clear()
        board.elementsByName.putAll(existingNames)
        board.setBoundingBox(previousBoundingBox)
        board.unsuspendUpdate()
    }

    private fun validateLimits(
        limits: GeogebraReaderLimits,
    ): GeogebraReaderError.InvalidLimits? {
        val invalid = when {
            limits.maxConstructions < 0 ->
                "maxConstructions" to limits.maxConstructions
            limits.maxCommands < 0 ->
                "maxCommands" to limits.maxCommands
            limits.maxElements < 0 ->
                "maxElements" to limits.maxElements
            limits.maxObjects < 0 ->
                "maxObjects" to limits.maxObjects
            else -> null
        }
        return invalid?.let { (name, value) ->
            GeogebraReaderError.InvalidLimits(name, value)
        }
    }

    private companion object {
        const val GEOGEBRA_TAG = "geogebra"
        const val CONSTRUCTION_TAG = "construction"
        const val COMMAND_TAG = "command"
        const val INPUT_TAG = "input"
        const val OUTPUT_TAG = "output"
        const val ELEMENT_TAG = "element"
        const val EXPRESSION_TAG = "expression"
        const val EUCLIDIAN_VIEW_TAG = "euclidianView"
        const val COORD_SYSTEM_TAG = "coordSystem"
        const val EV_SETTINGS_TAG = "evSettings"
        const val DECIMALS_TAG = "decimals"
        const val COORDS_TAG = "coords"
        const val MATRIX_TAG = "matrix"
        const val START_POINT_TAG = "startPoint"
        const val SLIDER_TAG = "slider"
        const val VALUE_TAG = "value"
        const val ANIMATION_TAG = "animation"
        const val FIX_TAG = "fix"
        const val LABEL_ATTRIBUTE = "label"
        const val NAME_ATTRIBUTE = "name"
        const val TYPE_ATTRIBUTE = "type"
        const val FORMAT_ATTRIBUTE = "format"
        const val EXPRESSION_ATTRIBUTE = "exp"
        const val VALUE_ATTRIBUTE = "val"
        const val X_ATTRIBUTE = "x"
        const val Y_ATTRIBUTE = "y"
        const val Z_ATTRIBUTE = "z"
        const val X_ZERO_ATTRIBUTE = "xZero"
        const val Y_ZERO_ATTRIBUTE = "yZero"
        const val SCALE_ATTRIBUTE = "scale"
        const val Y_SCALE_ATTRIBUTE = "yscale"
        const val AXES_ATTRIBUTE = "axes"
        const val WIDTH_ATTRIBUTE = "width"
        const val MIN_ATTRIBUTE = "min"
        const val MAX_ATTRIBUTE = "max"
        const val STEP_ATTRIBUTE = "step"
        const val HORIZONTAL_ATTRIBUTE = "horizontal"
        const val ABSOLUTE_SCREEN_LOCATION_ATTRIBUTE =
            "absoluteScreenLocation"

        fun jsParseInt(value: String): Int? =
            INTEGER_PREFIX.find(value.trimStart())
                ?.value
                ?.toIntOrNull()

        val INTEGER_PREFIX = Regex("^[+-]?\\d+")
    }
}

private class ReaderCoordinateFunction(
    override val dependencies: Map<String, GeometryElement>,
    private val value: () -> Double,
) : com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction {
    override val origin: String? = null

    override fun evaluate(
        arguments: List<
            com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
            >,
    ): GMResult<
        com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue,
        com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError,
        > =
        GMResult.Ok(
            com.swithun.jsxgraph.core.parser
                .JessieCodeRuntimeValue.NumberValue(value()),
        )
}

private fun GeogebraScalarValue.evaluateValue(): Double =
    when (val result = evaluate()) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> Double.NaN
    }

internal object GeogebraReaderFactory :
    JsxGraphReaderFactory<Board> {
    override fun create(
        board: Board,
        source: String,
    ): GMResult<JsxGraphReader, ReaderError> =
        GMResult.Ok(
            JsxGraphReader {
                when (val result = GeogebraReader(source).read(board)) {
                    is GMResult.Ok -> GMResult.Ok(Unit)
                    is GMResult.Err -> GMResult.Err(
                        ReaderError.DomainFailure(result.error),
                    )
                }
            },
        )
}

internal fun ReaderRegistry<Board>.registerGeogebraReader() {
    registerReader(
        reader = GeogebraReaderFactory,
        extensions = listOf("geogebra", "ggb"),
    )
}
