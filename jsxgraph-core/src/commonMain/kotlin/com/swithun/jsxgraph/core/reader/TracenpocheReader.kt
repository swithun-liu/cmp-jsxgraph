/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/tracenpoche.js -> parseOptions, parseFigure, read,
 * and the point/line/circle/polygon/text construction functions.
 * Copyright 2011-2013 Emmanuel Ostenne and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Axis
import com.swithun.jsxgraph.core.base.AxisError
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardError
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveError
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.IntersectionError
import com.swithun.jsxgraph.core.base.IntersectionIndexSource
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.MidpointError
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.OrthogonalConstructionError
import com.swithun.jsxgraph.core.base.OrthogonalPoint
import com.swithun.jsxgraph.core.base.OrthogonalPointKind
import com.swithun.jsxgraph.core.base.ParallelConstructionError
import com.swithun.jsxgraph.core.base.ParallelLine
import com.swithun.jsxgraph.core.base.PerpendicularLine
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.PolygonError
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.base.SliderAttributes
import com.swithun.jsxgraph.core.base.SliderError
import com.swithun.jsxgraph.core.base.Tangent
import com.swithun.jsxgraph.core.base.TangentError
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.TextError
import com.swithun.jsxgraph.core.base.Transformation
import com.swithun.jsxgraph.core.base.TransformationDynamicParameter
import com.swithun.jsxgraph.core.base.TransformationError
import com.swithun.jsxgraph.core.base.TransformationParameter
import com.swithun.jsxgraph.core.base.TriangleCenterConstructionError
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal data class TracenpocheReaderLimits(
    val parser: TracenpocheParserLimits = TracenpocheParserLimits(),
    val maxLoopIterations: Int = 100_000,
    val maxObjects: Int = 100_000,
)

internal sealed interface TracenpocheReaderError : ReaderDomainError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : TracenpocheReaderError

    data class ParserFailed(
        val cause: TracenpocheParserError,
    ) : TracenpocheReaderError

    data class UndefinedReference(
        val name: String,
    ) : TracenpocheReaderError

    data class InvalidIndexedReference(
        val name: String,
        val index: Double,
    ) : TracenpocheReaderError

    data class ArgumentCount(
        val function: String,
        val expected: String,
        val actual: Int,
    ) : TracenpocheReaderError

    data class ArgumentType(
        val function: String,
        val index: Int,
        val expected: String,
        val actual: String,
    ) : TracenpocheReaderError

    data class UnsupportedFunction(
        val name: String,
    ) : TracenpocheReaderError

    data class UnsupportedExpression(
        val description: String,
    ) : TracenpocheReaderError

    data class LoopLimitExceeded(
        val limit: Int,
    ) : TracenpocheReaderError

    data class ObjectLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : TracenpocheReaderError

    data class BoardSetupFailed(
        val cause: TracenpocheCreationError,
    ) : TracenpocheReaderError

    data class CreationFailed(
        val function: String,
        val cause: TracenpocheCreationError,
    ) : TracenpocheReaderError
}

internal sealed interface TracenpocheCreationError {
    data class Board(val error: BoardError) : TracenpocheCreationError

    data class Axis(val error: AxisError) : TracenpocheCreationError

    data class Point(val error: PointError) : TracenpocheCreationError

    data class Line(val error: LineError) : TracenpocheCreationError

    data class Circle(val error: CircleError) : TracenpocheCreationError

    data class Polygon(val error: PolygonError) : TracenpocheCreationError

    data class Intersection(
        val error: IntersectionError,
    ) : TracenpocheCreationError

    data class Midpoint(val error: MidpointError) : TracenpocheCreationError

    data class Parallel(
        val error: ParallelConstructionError,
    ) : TracenpocheCreationError

    data class Orthogonal(
        val error: OrthogonalConstructionError,
    ) : TracenpocheCreationError

    data class Bisector(
        val error: TriangleCenterConstructionError,
    ) : TracenpocheCreationError

    data class Curve(val error: CurveError) : TracenpocheCreationError

    data class Slider(val error: SliderError) : TracenpocheCreationError

    data class Tangent(val error: TangentError) : TracenpocheCreationError

    data class Text(val error: TextError) : TracenpocheCreationError

    data class Transformation(
        val error: TransformationError,
    ) : TracenpocheCreationError
}

internal sealed interface TracenpocheValue {
    data class Number(
        val value: Double,
    ) : TracenpocheValue

    data class Text(
        val value: String,
    ) : TracenpocheValue

    data class Flag(
        val value: Boolean,
    ) : TracenpocheValue

    data class Element(
        val value: GeometryElement,
    ) : TracenpocheValue

    data class Transform(
        val value: Transformation,
    ) : TracenpocheValue

    data class Function(
        val expression: TracenpocheExpression,
    ) : TracenpocheValue
}

internal data class ParsedTracenpoche(
    val program: ParsedTracenpocheProgram,
)

internal data class DrawnTracenpoche(
    val parsed: ParsedTracenpoche,
    val values: Map<String, TracenpocheValue>,
    val objects: Map<String, GeometryElement>,
)

internal class TracenpocheReader(
    private val data: String,
) {
    // JSXGraph 1.13.3: src/reader/tracenpoche.js -> parseFigure.
    internal fun parse(
        limits: TracenpocheReaderLimits = TracenpocheReaderLimits(),
    ): GMResult<ParsedTracenpoche, TracenpocheReaderError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        val start = data.indexOf(FIGURE_MARKER)
        val figure =
            if (start < 0) {
                ""
            } else {
                val contentStart = start + FIGURE_MARKER.length
                val end = data.indexOf('@', startIndex = contentStart)
                    .takeIf { it >= 0 }
                    ?: data.length
                data.substring(contentStart, end)
            }
        return when (
            val result = TracenpocheParser.parse(
                source = figure,
                limits = limits.parser,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                ParsedTracenpoche(result.value),
            )
            is GMResult.Err -> GMResult.Err(
                TracenpocheReaderError.ParserFailed(result.error),
            )
        }
    }

    // JSXGraph 1.13.3: src/reader/tracenpoche.js -> read / parseData.
    internal fun read(
        board: Board,
        limits: TracenpocheReaderLimits = TracenpocheReaderLimits(),
    ): GMResult<DrawnTracenpoche, TracenpocheReaderError> {
        val parsed = when (val result = parse(limits)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val existingIds = board.objects.keys.toSet()
        val existingNames = board.elementsByName.toMap()
        val previousBoundingBox = board.getBoundingBox()
        board.suspendUpdate()
        when (val result = configureBoard(board)) {
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
        val evaluator = Evaluator(
            board = board,
            limits = limits,
            initialObjectCount = existingIds.size,
        )
        for (statement in parsed.program.statements) {
            when (val result = evaluator.evaluate(statement)) {
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
        }
        board.unsuspendUpdate()
        val values = evaluator.values.toMap()
        return GMResult.Ok(
            DrawnTracenpoche(
                parsed = parsed,
                values = values,
                objects = values.mapNotNull { (name, value) ->
                    (value as? TracenpocheValue.Element)
                        ?.value
                        ?.let { name to it }
                }.toMap(),
            ),
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/tracenpoche.js -> parseOptions.
     * The upstream implementation ignores the options section and always
     * installs this viewport and two axes.
     */
    private fun configureBoard(
        board: Board,
    ): GMResult<Unit, TracenpocheReaderError> {
        when (
            val result = board.setBoundingBox(
                bbox = doubleArrayOf(-10.0, 10.0, 10.0, -10.0),
                keepAspectRatio = true,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> {
                return GMResult.Err(
                    TracenpocheReaderError.BoardSetupFailed(
                        TracenpocheCreationError.Board(result.error),
                    ),
                )
            }
        }
        when (
            val result = createAxis(
                board,
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(1.0, 0.0),
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        return createAxis(
            board,
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(0.0, 1.0),
        )
    }

    private fun createAxis(
        board: Board,
        firstCoordinates: DoubleArray,
        secondCoordinates: DoubleArray,
    ): GMResult<Unit, TracenpocheReaderError> {
        val first = when (
            val result = Point.create(
                board = board,
                coordinates = firstCoordinates,
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    TracenpocheReaderError.BoardSetupFailed(
                        TracenpocheCreationError.Point(result.error),
                    ),
                )
            }
        }
        val second = when (
            val result = Point.create(
                board = board,
                coordinates = secondCoordinates,
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    TracenpocheReaderError.BoardSetupFailed(
                        TracenpocheCreationError.Point(result.error),
                    ),
                )
            }
        }
        return when (
            val result = Axis.create(
                board = board,
                point1 = first,
                point2 = second,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(Unit)
            is GMResult.Err -> GMResult.Err(
                TracenpocheReaderError.BoardSetupFailed(
                    TracenpocheCreationError.Axis(result.error),
                ),
            )
        }
    }

    private class Evaluator(
        private val board: Board,
        private val limits: TracenpocheReaderLimits,
        private val initialObjectCount: Int,
    ) {
        val values = linkedMapOf<String, TracenpocheValue>()
        private var loopIterations = 0

        fun evaluate(
            statement: TracenpocheStatement,
        ): GMResult<Unit, TracenpocheReaderError> =
            when (statement) {
                is TracenpocheStatement.Expression -> {
                    when (val result = expression(statement.expression)) {
                        is GMResult.Ok -> objectLimit()
                        is GMResult.Err -> result
                    }
                }
                is TracenpocheStatement.For -> evaluateFor(statement)
            }

        private fun evaluateFor(
            statement: TracenpocheStatement.For,
        ): GMResult<Unit, TracenpocheReaderError> {
            when (val result = assignment(statement.initializer)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            while (true) {
                val current = when (
                    val result = reference(statement.initializer.name, null)
                ) {
                    is GMResult.Ok -> when (val value = result.value) {
                        is TracenpocheValue.Number -> value.value
                        else -> {
                            return argumentType(
                                function = FOR_KEYWORD,
                                index = 0,
                                expected = "number",
                                value = value,
                            )
                        }
                    }
                    is GMResult.Err -> return result
                }
                val end = when (
                    val result = expression(statement.endInclusive)
                ) {
                    is GMResult.Ok -> when (val value = result.value) {
                        is TracenpocheValue.Number -> value.value
                        else -> {
                            return argumentType(
                                function = FOR_KEYWORD,
                                index = 1,
                                expected = "number",
                                value = value,
                            )
                        }
                    }
                    is GMResult.Err -> return result
                }
                if (current > end) {
                    return GMResult.Ok(Unit)
                }
                loopIterations += 1
                if (loopIterations > limits.maxLoopIterations) {
                    return GMResult.Err(
                        TracenpocheReaderError.LoopLimitExceeded(
                            limits.maxLoopIterations,
                        ),
                    )
                }
                for (bodyStatement in statement.body) {
                    when (val result = evaluate(bodyStatement)) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                }
                values[statement.initializer.name] =
                    TracenpocheValue.Number(current + 1.0)
            }
        }

        private fun expression(
            expression: TracenpocheExpression,
            requestedName: String? = null,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> =
            when (expression) {
                is TracenpocheExpression.NumberLiteral ->
                    GMResult.Ok(TracenpocheValue.Number(expression.value))
                is TracenpocheExpression.StringLiteral ->
                    GMResult.Ok(TracenpocheValue.Text(expression.value))
                is TracenpocheExpression.BooleanLiteral ->
                    GMResult.Ok(TracenpocheValue.Flag(expression.value))
                is TracenpocheExpression.Reference ->
                    reference(expression.name, null)
                is TracenpocheExpression.IndexedReference ->
                    reference(expression.name, expression.index)
                is TracenpocheExpression.Unary ->
                    unary(expression)
                is TracenpocheExpression.Binary ->
                    binary(expression)
                is TracenpocheExpression.Assignment ->
                    assignment(expression)
                is TracenpocheExpression.Call ->
                    call(expression, requestedName)
                is TracenpocheExpression.Function ->
                    if (requestedName == null) {
                        GMResult.Ok(
                            TracenpocheValue.Function(
                                expression.expression,
                            ),
                        )
                    } else {
                        createFunctionGraph(
                            expression.expression,
                            requestedName,
                        )
                    }
                is TracenpocheExpression.Conditional ->
                    conditional(expression)
            }

        private fun assignment(
            assignment: TracenpocheExpression.Assignment,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            val name = when (
                val result = indexedName(
                    assignment.name,
                    assignment.index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val value = when (
                val result = expression(
                    expression = assignment.value,
                    requestedName = name,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            values[name] = value
            return GMResult.Ok(value)
        }

        private fun reference(
            name: String,
            indexExpression: TracenpocheExpression?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (indexExpression == null && name == PI_NAME) {
                return GMResult.Ok(TracenpocheValue.Number(PI))
            }
            val resolvedName = when (
                val result = indexedName(name, indexExpression)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return values[resolvedName]?.let { GMResult.Ok(it) }
                ?: GMResult.Err(
                    TracenpocheReaderError.UndefinedReference(
                        resolvedName,
                    ),
                )
        }

        private fun indexedName(
            name: String,
            indexExpression: TracenpocheExpression?,
        ): GMResult<String, TracenpocheReaderError> {
            if (indexExpression == null) {
                return GMResult.Ok(name)
            }
            val index = when (val result = expression(indexExpression)) {
                is GMResult.Ok -> when (val value = result.value) {
                    is TracenpocheValue.Number -> value.value
                    else -> {
                        return argumentType(
                            function = "index",
                            index = 0,
                            expected = "number",
                            value = value,
                        )
                    }
                }
                is GMResult.Err -> return result
            }
            if (!index.isFinite()) {
                return GMResult.Err(
                    TracenpocheReaderError.InvalidIndexedReference(
                        name = name,
                        index = index,
                    ),
                )
            }
            val suffix =
                if (index % 1.0 == 0.0) {
                    index.toLong().toString()
                } else {
                    index.toString()
                }
            return GMResult.Ok(name + suffix)
        }

        private fun unary(
            expression: TracenpocheExpression.Unary,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            val value = when (val result = expression(expression.operand)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val number = (value as? TracenpocheValue.Number)?.value
                ?: return argumentType(
                    function = expression.operator,
                    index = 0,
                    expected = "number",
                    value = value,
                )
            return GMResult.Ok(
                TracenpocheValue.Number(
                    if (expression.operator == "-") -number else number,
                ),
            )
        }

        private fun binary(
            expression: TracenpocheExpression.Binary,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            val left = when (val result = expression(expression.left)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val right = when (val result = expression(expression.right)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (expression.operator == "#") {
                val first = (left as? TracenpocheValue.Element)
                    ?.value as? CoordsElement
                    ?: return argumentType(
                        "#",
                        0,
                        "coordinate element",
                        left,
                    )
                val second = (right as? TracenpocheValue.Element)
                    ?.value as? CoordsElement
                    ?: return argumentType(
                        "#",
                        1,
                        "coordinate element",
                        right,
                    )
                return GMResult.Ok(
                    TracenpocheValue.Number(first.Dist(second)),
                )
            }
            if (expression.operator == "+") {
                if (
                    left is TracenpocheValue.Text ||
                    right is TracenpocheValue.Text
                ) {
                    return GMResult.Ok(
                        TracenpocheValue.Text(
                            valueText(left) + valueText(right),
                        ),
                    )
                }
            }
            if (
                expression.operator == "&&" ||
                expression.operator == "||"
            ) {
                val first = truthy(left)
                val second = truthy(right)
                return GMResult.Ok(
                    TracenpocheValue.Flag(
                        if (expression.operator == "&&") {
                            first && second
                        } else {
                            first || second
                        },
                    ),
                )
            }
            if (expression.operator in COMPARISON_OPERATORS) {
                val first = number(left, expression.operator, 0)
                val second = number(right, expression.operator, 1)
                val leftNumber = when (first) {
                    is GMResult.Ok -> first.value
                    is GMResult.Err -> return first
                }
                val rightNumber = when (second) {
                    is GMResult.Ok -> second.value
                    is GMResult.Err -> return second
                }
                val comparison = when (expression.operator) {
                    "==" -> leftNumber == rightNumber
                    "!=" -> leftNumber != rightNumber
                    "<" -> leftNumber < rightNumber
                    "<=" -> leftNumber <= rightNumber
                    ">" -> leftNumber > rightNumber
                    else -> leftNumber >= rightNumber
                }
                return GMResult.Ok(TracenpocheValue.Flag(comparison))
            }
            val first = number(left, expression.operator, 0)
            val second = number(right, expression.operator, 1)
            val leftNumber = when (first) {
                is GMResult.Ok -> first.value
                is GMResult.Err -> return first
            }
            val rightNumber = when (second) {
                is GMResult.Ok -> second.value
                is GMResult.Err -> return second
            }
            val result = when (expression.operator) {
                "+" -> leftNumber + rightNumber
                "-" -> leftNumber - rightNumber
                "*" -> leftNumber * rightNumber
                "/" -> leftNumber / rightNumber
                "%" -> leftNumber % rightNumber
                "^" -> leftNumber.pow(rightNumber)
                else -> {
                    return GMResult.Err(
                        TracenpocheReaderError.UnsupportedExpression(
                            "binary operator ${expression.operator}",
                        ),
                    )
                }
            }
            return GMResult.Ok(TracenpocheValue.Number(result))
        }

        private fun conditional(
            conditional: TracenpocheExpression.Conditional,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            val condition = when (
                val result = expression(conditional.condition)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return expression(
                if (truthy(condition)) {
                    conditional.whenTrue
                } else {
                    conditional.whenFalse
                },
            )
        }

        private fun call(
            call: TracenpocheExpression.Call,
            requestedName: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (call.callee in NUMERIC_FUNCTIONS) {
                return numericFunction(call)
            }
            val arguments = mutableListOf<TracenpocheValue>()
            for (argument in call.arguments) {
                when (val result = expression(argument)) {
                    is GMResult.Ok -> arguments += result.value
                    is GMResult.Err -> return result
                }
            }
            val name = requestedName
            return when (call.callee) {
                "point" -> createPoint(arguments, name)
                "image" -> createImagePoint(arguments, name)
                "reel",
                "entier",
                -> createSlider(
                    arguments = arguments,
                    name = name,
                    attributes = call.attributes,
                    function = call.callee,
                )
                "segment" -> createLine(
                    arguments,
                    name,
                    segment = true,
                    arrow = false,
                    function = call.callee,
                )
                "droite" -> createLine(
                    arguments,
                    name,
                    segment = false,
                    arrow = false,
                    function = call.callee,
                )
                "vecteur" -> createLine(
                    arguments,
                    name,
                    segment = true,
                    arrow = true,
                    function = call.callee,
                )
                "droiteEQR" -> createCoefficientLine(
                    arguments,
                    name,
                    homogeneous = true,
                )
                "droiteEQ" -> createCoefficientLine(
                    arguments,
                    name,
                    homogeneous = false,
                )
                "milieu" -> createMidpoint(arguments, name)
                "mediatrice" ->
                    createPerpendicularBisector(arguments, name)
                "parallele" -> createParallel(arguments, name)
                "perpendiculaire" ->
                    createPerpendicular(arguments, name)
                "bissectrice" -> createBisector(arguments, name)
                "tangente" -> createTangent(arguments, name)
                "intersection" -> createIntersection(arguments, name)
                "projete" -> createProjection(arguments, name)
                "pointsur" -> createPointOn(arguments, name)
                "barycentre" -> createBarycenter(arguments, name)
                "cercle",
                "cerclerayon",
                -> createCircle(arguments, name, call.callee)
                "polygone" -> createPolygon(arguments, name)
                "texte" -> createText(arguments, name)
                "homothetie" ->
                    createHomothety(arguments, call.callee)
                "reflexion" ->
                    createReflection(arguments, call.callee)
                "rotation" ->
                    createRotation(arguments, call.callee)
                "symetrie" ->
                    createPointSymmetry(arguments, call.callee)
                "translation" ->
                    createTranslation(arguments, call.callee)
                else -> GMResult.Err(
                    TracenpocheReaderError.UnsupportedFunction(
                        call.callee,
                    ),
                )
            }
        }

        private fun createPoint(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount("point", arguments, 2)?.let { return it }
            val x = when (val result = number(arguments[0], "point", 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val y = when (val result = number(arguments[1], "point", 1)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return pointResult(
                function = "point",
                Point.create(
                    board = board,
                    coordinates = doubleArrayOf(x, y),
                    name = name,
                ),
            )
        }

        private fun createImagePoint(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount("image", arguments, 2)?.let { return it }
            return when (val first = arguments[0]) {
                is TracenpocheValue.Number -> {
                    val x = when (
                        val result = number(arguments[1], "image", 1)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    pointResult(
                        function = "image",
                        Point.create(
                            board = board,
                            coordinates = doubleArrayOf(x, first.value),
                            name = name,
                        ),
                    )
                }
                is TracenpocheValue.Transform -> {
                    val basePoint =
                        (arguments[1] as? TracenpocheValue.Element)
                            ?.value as? CoordsElement
                            ?: return argumentType(
                                "image",
                                1,
                                "coordinate element",
                                arguments[1],
                            )
                    pointResult(
                        function = "image",
                        Point.create(
                            board = board,
                            basePoint = basePoint,
                            transformations = listOf(first.value),
                            name = name,
                        ),
                    )
                }
                else -> argumentType(
                    "image",
                    0,
                    "number or transformation",
                    first,
                )
            }
        }

        private fun createSlider(
            arguments: List<TracenpocheValue>,
            name: String?,
            attributes: List<String>,
            function: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount(function, arguments, 4)?.let { return it }
            val values = mutableListOf<Double>()
            for (index in arguments.indices) {
                when (
                    val result = number(arguments[index], function, index)
                ) {
                    is GMResult.Ok -> values += result.value
                    is GMResult.Err -> return result
                }
            }
            return when (
                val result = Slider.create(
                    board = board,
                    startCoordinates = doubleArrayOf(0.0, -2.0),
                    endCoordinates = doubleArrayOf(3.0, -2.0),
                    range = doubleArrayOf(
                        values[1],
                        values[0],
                        values[2],
                    ),
                    attributes = SliderAttributes(
                        name = name,
                        snapWidth = values[3],
                        withLabel = "sansnom" !in attributes,
                    ),
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    function,
                    TracenpocheCreationError.Slider(result.error),
                )
            }
        }

        private fun createLine(
            arguments: List<TracenpocheValue>,
            name: String?,
            segment: Boolean,
            arrow: Boolean,
            function: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount(function, arguments, 2)?.let { return it }
            val first = when (
                val result = point(arguments[0], function, 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = point(arguments[1], function, 1)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val created =
                if (segment || arrow) {
                    Line.createSegment(
                        board = board,
                        point1 = first,
                        point2 = second,
                        name = name,
                    )
                } else {
                    Line.create(
                        board = board,
                        point1 = first,
                        point2 = second,
                        name = name,
                    )
                }
            return when (created) {
                is GMResult.Ok -> {
                    if (arrow) {
                        created.value.configureArrow()
                    }
                    GMResult.Ok(
                        TracenpocheValue.Element(created.value),
                    )
                }
                is GMResult.Err -> creationFailure(
                    function,
                    TracenpocheCreationError.Line(created.error),
                )
            }
        }

        private fun createCoefficientLine(
            arguments: List<TracenpocheValue>,
            name: String?,
            homogeneous: Boolean,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            val function = if (homogeneous) "droiteEQR" else "droiteEQ"
            val count = if (homogeneous) 3 else 2
            requireCount(function, arguments, count)?.let { return it }
            val values = mutableListOf<Double>()
            for (index in arguments.indices) {
                when (
                    val result = number(arguments[index], function, index)
                ) {
                    is GMResult.Ok -> values += result.value
                    is GMResult.Err -> return result
                }
            }
            val coefficients =
                if (homogeneous) {
                    doubleArrayOf(values[2], values[0], values[1])
                } else {
                    doubleArrayOf(1.0, values[0], values[1])
                }
            return when (
                val result = Line.create(
                    board = board,
                    coefficients = coefficients,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    function,
                    TracenpocheCreationError.Line(result.error),
                )
            }
        }

        private fun createMidpoint(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            val points = when (arguments.size) {
                1 -> {
                    val line = when (
                        val result = line(arguments[0], "milieu", 0)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    listOf(line.point1, line.point2)
                }
                2 -> {
                    val first = when (
                        val result = point(arguments[0], "milieu", 0)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val second = when (
                        val result = point(arguments[1], "milieu", 1)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    listOf(first, second)
                }
                else -> {
                    return GMResult.Err(
                        TracenpocheReaderError.ArgumentCount(
                            function = "milieu",
                            expected = "1 or 2",
                            actual = arguments.size,
                        ),
                    )
                }
            }
            return when (
                val result = MidpointPoint.create(
                    board = board,
                    point1 = points[0],
                    point2 = points[1],
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "milieu",
                    TracenpocheCreationError.Midpoint(result.error),
                )
            }
        }

        private fun createParallel(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount("parallele", arguments, 2)?.let { return it }
            val point = when (
                val result = point(arguments[0], "parallele", 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val line = when (
                val result = line(arguments[1], "parallele", 1)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (
                val result = ParallelLine.create(
                    board = board,
                    sourceLine = line,
                    throughPoint = point,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "parallele",
                    TracenpocheCreationError.Parallel(result.error),
                )
            }
        }

        private fun createPerpendicularBisector(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (arguments.size !in 1..2) {
                return GMResult.Err(
                    TracenpocheReaderError.ArgumentCount(
                        function = "mediatrice",
                        expected = "1 or 2",
                        actual = arguments.size,
                    ),
                )
            }
            val sourceLine: Line
            val firstPoint: Point
            val secondPoint: Point
            if (arguments.size == 1) {
                sourceLine = when (
                    val result = line(arguments[0], "mediatrice", 0)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                firstPoint = sourceLine.point1
                secondPoint = sourceLine.point2
            } else {
                firstPoint = when (
                    val result = point(arguments[0], "mediatrice", 0)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                secondPoint = when (
                    val result = point(arguments[1], "mediatrice", 1)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                sourceLine = when (
                    val result = Line.create(
                        board = board,
                        point1 = firstPoint,
                        point2 = secondPoint,
                        name = "",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return creationFailure(
                        "mediatrice",
                        TracenpocheCreationError.Line(result.error),
                    )
                }
            }
            val midpoint = when (
                val result = MidpointPoint.create(
                    board = board,
                    point1 = firstPoint,
                    point2 = secondPoint,
                    name = "",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    "mediatrice",
                    TracenpocheCreationError.Midpoint(result.error),
                )
            }
            return when (
                val result = PerpendicularLine.create(
                    board = board,
                    line = sourceLine,
                    point = midpoint,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "mediatrice",
                    TracenpocheCreationError.Orthogonal(result.error),
                )
            }
        }

        private fun createPerpendicular(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount("perpendiculaire", arguments, 2)
                ?.let { return it }
            val point = when (
                val result =
                    point(arguments[0], "perpendiculaire", 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val line = when (
                val result =
                    line(arguments[1], "perpendiculaire", 1)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (
                val result = PerpendicularLine.create(
                    board = board,
                    line = line,
                    point = point,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "perpendiculaire",
                    TracenpocheCreationError.Orthogonal(result.error),
                )
            }
        }

        private fun createBisector(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount("bissectrice", arguments, 3)?.let { return it }
            val points = mutableListOf<Point>()
            for (index in arguments.indices) {
                when (
                    val result =
                        point(arguments[index], "bissectrice", index)
                ) {
                    is GMResult.Ok -> points += result.value
                    is GMResult.Err -> return result
                }
            }
            return when (
                val result = BisectorLine.create(
                    board = board,
                    point1 = points[0],
                    vertex = points[1],
                    point3 = points[2],
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "bissectrice",
                    TracenpocheCreationError.Bisector(result.error),
                )
            }
        }

        private fun createTangent(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount("tangente", arguments, 2)?.let { return it }
            val curve = (arguments[0] as? TracenpocheValue.Element)
                ?.value as? Curve
                ?: return argumentType(
                    "tangente",
                    0,
                    "curve",
                    arguments[0],
                )
            val parameter = when (val second = arguments[1]) {
                is TracenpocheValue.Number -> {
                    { second.value }
                }
                is TracenpocheValue.Element -> {
                    when (val element = second.value) {
                        is Slider -> {
                            { element.Value() }
                        }
                        is Point -> {
                            { element.X() }
                        }
                        else -> {
                            return argumentType(
                                "tangente",
                                1,
                                "number, slider, or point",
                                second,
                            )
                        }
                    }
                }
                else -> {
                    return argumentType(
                        "tangente",
                        1,
                        "number, slider, or point",
                        second,
                    )
                }
            }
            val tangentPoint = when (
                val result = Point.createConstrained(
                    board = board,
                    coordinateFunctions = listOf(
                        TracenpocheCoordinateFunction(parameter),
                        TracenpocheCoordinateFunction {
                            curve.Y(parameter())
                        },
                    ),
                    name = "",
                    fixed = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return creationFailure(
                    "tangente",
                    TracenpocheCreationError.Point(result.error),
                )
            }
            tangentPoint.position = parameter()
            tangentPoint.slideObject = curve
            return when (
                val result = Tangent.create(
                    board = board,
                    firstParent = curve,
                    secondParent = tangentPoint,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "tangente",
                    TracenpocheCreationError.Tangent(result.error),
                )
            }
        }

        private fun createIntersection(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (arguments.size !in 2..3) {
                return GMResult.Err(
                    TracenpocheReaderError.ArgumentCount(
                        function = "intersection",
                        expected = "2 or 3",
                        actual = arguments.size,
                    ),
                )
            }
            val first = when (
                val result = element(arguments[0], "intersection", 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = element(arguments[1], "intersection", 1)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val index =
                if (arguments.size == 3) {
                    when (
                        val result =
                            number(arguments[2], "intersection", 2)
                    ) {
                        is GMResult.Ok -> result.value - 1.0
                        is GMResult.Err -> return result
                    }
                } else {
                    0.0
                }
            return when (
                val result = IntersectionPoint.create(
                    board = board,
                    first = first,
                    second = second,
                    firstIndex = IntersectionIndexSource.Number(index),
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "intersection",
                    TracenpocheCreationError.Intersection(result.error),
                )
            }
        }

        private fun createProjection(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (arguments.size !in 2..3) {
                return GMResult.Err(
                    TracenpocheReaderError.ArgumentCount(
                        function = "projete",
                        expected = "2 or 3",
                        actual = arguments.size,
                    ),
                )
            }
            val point = when (
                val result = point(arguments[0], "projete", 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val line = when (
                val result = line(arguments[1], "projete", 1)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (arguments.size == 3) {
                val direction = when (
                    val result = line(arguments[2], "projete", 2)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val parallel = when (
                    val result = ParallelLine.create(
                        board = board,
                        sourceLine = direction,
                        throughPoint = point,
                        name = "",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return creationFailure(
                        "projete",
                        TracenpocheCreationError.Parallel(result.error),
                    )
                }
                return when (
                    val result = IntersectionPoint.create(
                        board = board,
                        first = line,
                        second = parallel,
                        firstIndex = IntersectionIndexSource.Number(0.0),
                        name = name,
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(
                        TracenpocheValue.Element(result.value),
                    )
                    is GMResult.Err -> creationFailure(
                        "projete",
                        TracenpocheCreationError.Intersection(result.error),
                    )
                }
            }
            return when (
                val result = OrthogonalPoint.create(
                    board = board,
                    point = point,
                    line = line,
                    kind = OrthogonalPointKind.ORTHOGONAL_PROJECTION,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "projete",
                    TracenpocheCreationError.Orthogonal(result.error),
                )
            }
        }

        private fun createPointOn(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (arguments.size !in 2..3) {
                return GMResult.Err(
                    TracenpocheReaderError.ArgumentCount(
                        function = "pointsur",
                        expected = "2 or 3",
                        actual = arguments.size,
                    ),
                )
            }
            val firstPoint: Point
            val secondPoint: Point
            val parameterValue: TracenpocheValue
            if (arguments.size == 3) {
                firstPoint = when (
                    val result = point(arguments[0], "pointsur", 0)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                secondPoint = when (
                    val result = point(arguments[1], "pointsur", 1)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                parameterValue = arguments[2]
            } else {
                val line = (arguments[0] as? TracenpocheValue.Element)
                    ?.value as? Line
                if (line != null) {
                    firstPoint = line.point1
                    secondPoint = line.point2
                    parameterValue = arguments[1]
                } else {
                    return createPointOnCircle(arguments, name)
                }
            }
            val parameter = when (
                val result = scalarProvider(
                    parameterValue,
                    "pointsur",
                    arguments.lastIndex,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return constrainedPoint(
                function = "pointsur",
                name = name,
                x = {
                    firstPoint.X() +
                        (secondPoint.X() - firstPoint.X()) * parameter()
                },
                y = {
                    firstPoint.Y() +
                        (secondPoint.Y() - firstPoint.Y()) * parameter()
                },
            )
        }

        private fun createPointOnCircle(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            val circle = (arguments[0] as? TracenpocheValue.Element)
                ?.value as? Circle
                ?: return argumentType(
                    "pointsur",
                    0,
                    "line or circle",
                    arguments[0],
                )
            val angle = when (
                val result = scalarProvider(
                    arguments[1],
                    "pointsur",
                    1,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return constrainedPoint(
                function = "pointsur",
                name = name,
                x = {
                    circle.center.X() + circle.Radius() * cos(angle())
                },
                y = {
                    circle.center.Y() + circle.Radius() * sin(angle())
                },
            )
        }

        private fun createBarycenter(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (arguments.isEmpty() || arguments.size % 2 != 0) {
                return GMResult.Err(
                    TracenpocheReaderError.ArgumentCount(
                        function = "barycentre",
                        expected = "point/weight pairs",
                        actual = arguments.size,
                    ),
                )
            }
            val weightedPoints = mutableListOf<Pair<Point, Double>>()
            for (index in arguments.indices step 2) {
                val point = when (
                    val result =
                        point(arguments[index], "barycentre", index)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val weight = when (
                    val result = number(
                        arguments[index + 1],
                        "barycentre",
                        index + 1,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                weightedPoints += point to weight
            }
            val sum = weightedPoints.sumOf { it.second }
            return constrainedPoint(
                function = "barycentre",
                name = name,
                x = {
                    weightedPoints.sumOf { (point, weight) ->
                        point.X() * weight
                    } / sum
                },
                y = {
                    weightedPoints.sumOf { (point, weight) ->
                        point.Y() * weight
                    } / sum
                },
            )
        }

        private fun constrainedPoint(
            function: String,
            name: String?,
            x: () -> Double,
            y: () -> Double,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> =
            pointResult(
                function = function,
                Point.createConstrained(
                    board = board,
                    coordinateFunctions = listOf(
                        TracenpocheCoordinateFunction(x),
                        TracenpocheCoordinateFunction(y),
                    ),
                    name = name,
                ),
            )

        private fun createCircle(
            arguments: List<TracenpocheValue>,
            name: String?,
            function: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount(function, arguments, 2)?.let { return it }
            val center = when (
                val result = point(arguments[0], function, 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val result =
                when (val radius = arguments[1]) {
                    is TracenpocheValue.Element -> {
                        val radiusPoint = radius.value as? Point
                            ?: return argumentType(
                                function,
                                1,
                                "point or number",
                                radius,
                            )
                        Circle.create(
                            board = board,
                            center = center,
                            point2 = radiusPoint,
                            name = name,
                        )
                    }
                    is TracenpocheValue.Number ->
                        Circle.create(
                            board = board,
                            center = center,
                            radius = radius.value,
                            id = "",
                            name = name,
                        )
                    else -> {
                        return argumentType(
                            function,
                            1,
                            "point or number",
                            radius,
                        )
                    }
                }
            return when (result) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    function,
                    TracenpocheCreationError.Circle(result.error),
                )
            }
        }

        private fun createPolygon(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (arguments.size < 3) {
                return GMResult.Err(
                    TracenpocheReaderError.ArgumentCount(
                        function = "polygone",
                        expected = "at least 3",
                        actual = arguments.size,
                    ),
                )
            }
            val points = mutableListOf<Point>()
            for (index in arguments.indices) {
                when (
                    val result = point(arguments[index], "polygone", index)
                ) {
                    is GMResult.Ok -> points += result.value
                    is GMResult.Err -> return result
                }
            }
            return when (
                val result = Polygon.create(
                    board = board,
                    vertices = points,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "polygone",
                    TracenpocheCreationError.Polygon(result.error),
                )
            }
        }

        private fun createText(
            arguments: List<TracenpocheValue>,
            name: String?,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount("texte", arguments, 3)?.let { return it }
            val x = when (val result = number(arguments[0], "texte", 0)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val y = when (val result = number(arguments[1], "texte", 1)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val content = when (
                val result = text(arguments[2], "texte", 2)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (
                val result = Text.create(
                    board = board,
                    coordinates = doubleArrayOf(x, y),
                    content = content,
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "texte",
                    TracenpocheCreationError.Text(result.error),
                )
            }
        }

        private fun createHomothety(
            arguments: List<TracenpocheValue>,
            function: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount(function, arguments, 2)?.let { return it }
            val center = when (
                val result = point(arguments[0], function, 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val factor = when (
                val result = scalarProvider(arguments[1], function, 1)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return transformationResult(
                function = function,
                result = Transformation.create(
                    board = board,
                    type = "generic",
                    parameters = listOf(
                        numericParameter(1.0),
                        numericParameter(0.0),
                        numericParameter(0.0),
                        dynamicParameter {
                            (1.0 - factor()) * center.X()
                        },
                        dynamicParameter(factor),
                        numericParameter(0.0),
                        dynamicParameter {
                            (1.0 - factor()) * center.Y()
                        },
                        numericParameter(0.0),
                        dynamicParameter(factor),
                    ),
                ),
            )
        }

        private fun createPointSymmetry(
            arguments: List<TracenpocheValue>,
            function: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount(function, arguments, 1)?.let { return it }
            val center = when (
                val result = point(arguments[0], function, 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return transformationResult(
                function = function,
                result = Transformation.createRotation(
                    board = board,
                    angle = numericParameter(PI),
                    center = center,
                ),
            )
        }

        private fun createReflection(
            arguments: List<TracenpocheValue>,
            function: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount(function, arguments, 1)?.let { return it }
            val line = when (
                val result = line(arguments[0], function, 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return GMResult.Ok(
                TracenpocheValue.Transform(
                    Transformation.createReflectionFromLine(line),
                ),
            )
        }

        private fun createRotation(
            arguments: List<TracenpocheValue>,
            function: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount(function, arguments, 2)?.let { return it }
            val center = when (
                val result = point(arguments[0], function, 0)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val degrees = when (
                val result = scalarProvider(arguments[1], function, 1)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return transformationResult(
                function = function,
                result = Transformation.createRotation(
                    board = board,
                    angle = dynamicParameter {
                        PI * degrees() / 180.0
                    },
                    center = center,
                ),
            )
        }

        private fun createTranslation(
            arguments: List<TracenpocheValue>,
            function: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            if (arguments.size !in 1..2) {
                return GMResult.Err(
                    TracenpocheReaderError.ArgumentCount(
                        function = function,
                        expected = "1 or 2",
                        actual = arguments.size,
                    ),
                )
            }
            val first: Point
            val second: Point
            if (arguments.size == 1) {
                val vector = when (
                    val result = line(arguments[0], function, 0)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                first = vector.point1
                second = vector.point2
            } else {
                first = when (
                    val result = point(arguments[0], function, 0)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                second = when (
                    val result = point(arguments[1], function, 1)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
            return transformationResult(
                function = function,
                result = Transformation.create(
                    board = board,
                    type = "translate",
                    parameters = listOf(
                        dynamicParameter {
                            second.X() - first.X()
                        },
                        dynamicParameter {
                            second.Y() - first.Y()
                        },
                    ),
                ),
            )
        }

        private fun createFunctionGraph(
            function: TracenpocheExpression,
            name: String,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            val source = when (val result = functionSource(function)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return when (
                val result = Curve.createFunctionGraph(
                    board = board,
                    ySource = source,
                    minimumSource = board.defaultCurveMinimum.toString(),
                    maximumSource = board.defaultCurveMaximum.toString(),
                    name = name,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    "fonction",
                    TracenpocheCreationError.Curve(result.error),
                )
            }
        }

        private fun numericFunction(
            call: TracenpocheExpression.Call,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> {
            requireCount(call.callee, call.arguments, 1)?.let { return it }
            val argument = when (
                val result = expression(call.arguments[0])
            ) {
                is GMResult.Ok -> when (val value = result.value) {
                    is TracenpocheValue.Number -> value.value
                    else -> {
                        return argumentType(
                            call.callee,
                            0,
                            "number",
                            value,
                        )
                    }
                }
                is GMResult.Err -> return result
            }
            val value = when (call.callee) {
                "sin" -> sin(argument)
                "cos" -> cos(argument)
                "tan" -> kotlin.math.tan(argument)
                "abs" -> abs(argument)
                "racine" -> sqrt(argument)
                "carre" -> argument * argument
                else -> {
                    return GMResult.Err(
                        TracenpocheReaderError.UnsupportedFunction(
                            call.callee,
                        ),
                    )
                }
            }
            return GMResult.Ok(TracenpocheValue.Number(value))
        }

        private fun functionSource(
            expression: TracenpocheExpression,
        ): GMResult<String, TracenpocheReaderError> =
            when (expression) {
                is TracenpocheExpression.NumberLiteral ->
                    GMResult.Ok(expression.value.toString())
                is TracenpocheExpression.Reference ->
                    if (
                        expression.name == "x" ||
                        expression.name == PI_NAME
                    ) {
                        GMResult.Ok(expression.name)
                    } else {
                        GMResult.Err(
                            TracenpocheReaderError.UnsupportedExpression(
                                "dynamic function reference " +
                                    expression.name,
                            ),
                        )
                    }
                is TracenpocheExpression.Unary -> {
                    when (val result = functionSource(expression.operand)) {
                        is GMResult.Ok -> GMResult.Ok(
                            "(${expression.operator}${result.value})",
                        )
                        is GMResult.Err -> result
                    }
                }
                is TracenpocheExpression.Binary -> {
                    val left = when (
                        val result = functionSource(expression.left)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val right = when (
                        val result = functionSource(expression.right)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    GMResult.Ok(
                        "($left${expression.operator}$right)",
                    )
                }
                is TracenpocheExpression.Call -> {
                    if (expression.callee !in NUMERIC_FUNCTIONS) {
                        return GMResult.Err(
                            TracenpocheReaderError.UnsupportedExpression(
                                "function call ${expression.callee}",
                            ),
                        )
                    }
                    if (expression.arguments.size != 1) {
                        return GMResult.Err(
                            TracenpocheReaderError.ArgumentCount(
                                function = expression.callee,
                                expected = "1",
                                actual = expression.arguments.size,
                            ),
                        )
                    }
                    val arguments = mutableListOf<String>()
                    for (argument in expression.arguments) {
                        when (val result = functionSource(argument)) {
                            is GMResult.Ok -> arguments += result.value
                            is GMResult.Err -> return result
                        }
                    }
                    val argument = arguments.single()
                    when (expression.callee) {
                        "racine" -> GMResult.Ok("sqrt($argument)")
                        "carre" -> GMResult.Ok(
                            "(($argument)*($argument))",
                        )
                        else -> GMResult.Ok(
                            "${expression.callee}($argument)",
                        )
                    }
                }
                else -> GMResult.Err(
                    TracenpocheReaderError.UnsupportedExpression(
                        "function expression ${expression::class.simpleName}",
                    ),
                )
            }

        private fun pointResult(
            function: String,
            result: GMResult<Point, PointError>,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> =
            when (result) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Element(result.value),
                )
                is GMResult.Err -> creationFailure(
                    function,
                    TracenpocheCreationError.Point(result.error),
                )
            }

        private fun transformationResult(
            function: String,
            result: GMResult<Transformation, TransformationError>,
        ): GMResult<TracenpocheValue, TracenpocheReaderError> =
            when (result) {
                is GMResult.Ok -> GMResult.Ok(
                    TracenpocheValue.Transform(result.value),
                )
                is GMResult.Err -> creationFailure(
                    function,
                    TracenpocheCreationError.Transformation(result.error),
                )
            }

        private fun scalarProvider(
            value: TracenpocheValue,
            function: String,
            index: Int,
        ): GMResult<() -> Double, TracenpocheReaderError> =
            when (value) {
                is TracenpocheValue.Number ->
                    GMResult.Ok({ value.value })
                is TracenpocheValue.Element -> {
                    val slider = value.value as? Slider
                        ?: return argumentType(
                            function,
                            index,
                            "number or slider",
                            value,
                        )
                    GMResult.Ok(slider::Value)
                }
                else -> argumentType(
                    function,
                    index,
                    "number or slider",
                    value,
                )
            }

        private fun numericParameter(
            value: Double,
        ): TransformationParameter =
            TransformationParameter.Numeric(value)

        private fun dynamicParameter(
            value: () -> Double,
        ): TransformationParameter =
            TransformationParameter.Dynamic(
                TransformationDynamicParameter {
                    GMResult.Ok(value())
                },
            )

        private fun objectLimit():
            GMResult<Unit, TracenpocheReaderError> {
            val requested = board.objects.size - initialObjectCount
            return if (requested > limits.maxObjects) {
                GMResult.Err(
                    TracenpocheReaderError.ObjectLimitExceeded(
                        limit = limits.maxObjects,
                        requested = requested,
                    ),
                )
            } else {
                GMResult.Ok(Unit)
            }
        }

        private fun requireCount(
            function: String,
            arguments: List<*>,
            expected: Int,
        ): GMResult.Err<TracenpocheReaderError>? =
            if (arguments.size == expected) {
                null
            } else {
                GMResult.Err(
                    TracenpocheReaderError.ArgumentCount(
                        function = function,
                        expected = expected.toString(),
                        actual = arguments.size,
                    ),
                )
            }

        private fun point(
            value: TracenpocheValue,
            function: String,
            index: Int,
        ): GMResult<Point, TracenpocheReaderError> {
            val point = (value as? TracenpocheValue.Element)
                ?.value as? Point
                ?: return argumentType(
                    function,
                    index,
                    "point",
                    value,
                )
            return GMResult.Ok(point)
        }

        private fun line(
            value: TracenpocheValue,
            function: String,
            index: Int,
        ): GMResult<Line, TracenpocheReaderError> {
            val line = (value as? TracenpocheValue.Element)
                ?.value as? Line
                ?: return argumentType(
                    function,
                    index,
                    "line",
                    value,
                )
            return GMResult.Ok(line)
        }

        private fun element(
            value: TracenpocheValue,
            function: String,
            index: Int,
        ): GMResult<GeometryElement, TracenpocheReaderError> {
            val element = (value as? TracenpocheValue.Element)?.value
                ?: return argumentType(
                    function,
                    index,
                    "geometry element",
                    value,
                )
            return GMResult.Ok(element)
        }

        private fun number(
            value: TracenpocheValue,
            function: String,
            index: Int,
        ): GMResult<Double, TracenpocheReaderError> {
            val number = (value as? TracenpocheValue.Number)?.value
                ?: return argumentType(
                    function,
                    index,
                    "number",
                    value,
                )
            return GMResult.Ok(number)
        }

        private fun text(
            value: TracenpocheValue,
            function: String,
            index: Int,
        ): GMResult<String, TracenpocheReaderError> {
            val text = (value as? TracenpocheValue.Text)?.value
                ?: return argumentType(
                    function,
                    index,
                    "string",
                    value,
                )
            return GMResult.Ok(text)
        }

        private fun <T> argumentType(
            function: String,
            index: Int,
            expected: String,
            value: TracenpocheValue,
        ): GMResult<T, TracenpocheReaderError> =
            GMResult.Err(
                TracenpocheReaderError.ArgumentType(
                    function = function,
                    index = index,
                    expected = expected,
                    actual = value.typeName(),
                ),
            )

        private fun <T> creationFailure(
            function: String,
            cause: TracenpocheCreationError,
        ): GMResult<T, TracenpocheReaderError> =
            GMResult.Err(
                TracenpocheReaderError.CreationFailed(
                    function = function,
                    cause = cause,
                ),
            )
    }

    private fun rollback(
        board: Board,
        existingIds: Set<String>,
        existingNames: Map<String, GeometryElement>,
        previousBoundingBox: DoubleArray,
    ) {
        val created = board.objects
            .filterKeys { it !in existingIds }
            .values
            .toList()
            .asReversed()
        board.removeObjects(created)
        board.elementsByName.clear()
        board.elementsByName.putAll(existingNames)
        board.setBoundingBox(previousBoundingBox)
        board.unsuspendUpdate()
    }

    private fun validateLimits(
        limits: TracenpocheReaderLimits,
    ): TracenpocheReaderError.InvalidLimits? {
        val invalid = when {
            limits.maxLoopIterations < 0 ->
                "maxLoopIterations" to limits.maxLoopIterations
            limits.maxObjects < 0 ->
                "maxObjects" to limits.maxObjects
            else -> null
        }
        return invalid?.let { (name, value) ->
            TracenpocheReaderError.InvalidLimits(name, value)
        }
    }

    private class TracenpocheCoordinateFunction(
        private val value: () -> Double,
    ) : JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> =
            emptyMap()

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(
                JessieCodeRuntimeValue.NumberValue(value()),
            )
    }

    private companion object {
        const val FIGURE_MARKER = "@figure;"
        const val FOR_KEYWORD = "for"
        const val PI_NAME = "pi"

        val NUMERIC_FUNCTIONS = setOf(
            "sin",
            "cos",
            "tan",
            "abs",
            "racine",
            "carre",
        )
        val COMPARISON_OPERATORS = setOf(
            "==",
            "!=",
            "<",
            "<=",
            ">",
            ">=",
        )
    }
}

private fun TracenpocheValue.typeName(): String =
    when (this) {
        is TracenpocheValue.Number -> "number"
        is TracenpocheValue.Text -> "string"
        is TracenpocheValue.Flag -> "boolean"
        is TracenpocheValue.Element ->
            value.elType.ifEmpty { "geometry element" }
        is TracenpocheValue.Transform -> "transformation"
        is TracenpocheValue.Function -> "function"
    }

private fun truthy(value: TracenpocheValue): Boolean =
    when (value) {
        is TracenpocheValue.Number ->
            value.value != 0.0 && !value.value.isNaN()
        is TracenpocheValue.Text -> value.value.isNotEmpty()
        is TracenpocheValue.Flag -> value.value
        is TracenpocheValue.Element -> true
        is TracenpocheValue.Transform -> true
        is TracenpocheValue.Function -> true
    }

private fun valueText(value: TracenpocheValue): String =
    when (value) {
        is TracenpocheValue.Number -> value.value.toString()
        is TracenpocheValue.Text -> value.value
        is TracenpocheValue.Flag -> value.value.toString()
        is TracenpocheValue.Element -> value.value.name
        is TracenpocheValue.Transform -> "transformation"
        is TracenpocheValue.Function -> "function"
    }

internal object TracenpocheReaderFactory :
    JsxGraphReaderFactory<Board> {
    override fun create(
        board: Board,
        source: String,
    ): GMResult<JsxGraphReader, ReaderError> =
        GMResult.Ok(
            JsxGraphReader {
                when (val result = TracenpocheReader(source).read(board)) {
                    is GMResult.Ok -> GMResult.Ok(Unit)
                    is GMResult.Err -> GMResult.Err(
                        ReaderError.DomainFailure(result.error),
                    )
                }
            },
        )
}

internal fun ReaderRegistry<Board>.registerTracenpocheReader() {
    registerReader(
        reader = TracenpocheReaderFactory,
        extensions = listOf("tracenpoche"),
    )
}
