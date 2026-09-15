/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> creator / isCreator,
 * src/base/point.js -> createPoint,
 * src/base/line.js -> createLine,
 * src/base/circle.js -> createCircle,
 * src/base/curve.js -> createCurve / createFunctiongraph
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveError
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.utils.JsNumberFormat

internal sealed interface JessieCodeCreatorError {
    data object BoardUnavailable : JessieCodeCreatorError

    data class UnsupportedParents(
        val parentTypes: List<String>,
    ) : JessieCodeCreatorError

    data class InvalidAttributeType(
        val attribute: String,
        val expected: String,
        val actual: String,
    ) : JessieCodeCreatorError

    data class UnsupportedAttributeValue(
        val attribute: String,
        val actual: String,
    ) : JessieCodeCreatorError

    data class PointFactory(
        val error: PointError,
    ) : JessieCodeCreatorError

    data class LineFactory(
        val error: LineError,
    ) : JessieCodeCreatorError

    data class CircleFactory(
        val error: CircleError,
    ) : JessieCodeCreatorError

    data class CurveFactory(
        val error: CurveError,
    ) : JessieCodeCreatorError
}

/**
 * Native creator subset registered by JSXGraph 1.13.3 through
 * JXG.registerElement. Custom environment creators retain precedence.
 */
internal object NativeJessieCodeCreators {
    private val creators = mapOf(
        "point" to JessieCodeCreator { board, parents, attributes, location ->
            createPoint(board, parents, attributes, location)
        },
        "line" to JessieCodeCreator { board, parents, attributes, location ->
            createLine(board, parents, attributes, location)
        },
        "circle" to JessieCodeCreator { board, parents, attributes, location ->
            createCircle(board, parents, attributes, location)
        },
        "curve" to JessieCodeCreator { board, parents, attributes, location ->
            createCurve(board, parents, attributes, location)
        },
        "functiongraph" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createFunctionGraph(
                board,
                parents,
                attributes,
                location,
                creatorName = "functiongraph",
            )
        },
        "plot" to JessieCodeCreator { board, parents, attributes, location ->
            createFunctionGraph(
                board,
                parents,
                attributes,
                location,
                creatorName = "plot",
            )
        },
    )

    internal fun creator(name: String): JessieCodeCreator? = creators[name]

    private fun createPoint(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure("point", JessieCodeCreatorError.BoardUnavailable, location)
        val identity = when (
            val result = creatorAttributes("point", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            parents.any {
                it !is JessieCodeRuntimeValue.NumberValue &&
                    it !is JessieCodeRuntimeValue.StringValue
            }
        ) {
            return unsupported("point", parents, location)
        }
        val result = createPointFromCoordinates(
            board = resolvedBoard,
            coordinates = parents,
            attributes = identity,
        )
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "point",
                error = JessieCodeCreatorError.PointFactory(result.error),
                location = location,
            )
        }
    }

    private fun createLine(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure("line", JessieCodeCreatorError.BoardUnavailable, location)
        val identity = when (
            val result = creatorAttributes("line", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val points = when {
            parents.size == 2 -> {
                val first = pointParent(resolvedBoard, parents[0])
                    ?: return unsupported("line", parents, location)
                val second = pointParent(resolvedBoard, parents[1])
                    ?: return unsupported("line", parents, location)
                val point1 = when (
                    val result = materializePoint(resolvedBoard, first)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return failure(
                        "line",
                        JessieCodeCreatorError.PointFactory(result.error),
                        location,
                    )
                }
                val point2 = when (
                    val result = materializePoint(resolvedBoard, second)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return failure(
                        "line",
                        JessieCodeCreatorError.PointFactory(result.error),
                        location,
                    )
                }
                point1 to point2
            }
            parents.size == 3 &&
                parents.all { it is JessieCodeRuntimeValue.NumberValue } -> {
                when (
                    val result = coefficientPoints(resolvedBoard, parents)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return failure(
                        "line",
                        JessieCodeCreatorError.PointFactory(result.error),
                        location,
                    )
                }
            }
            else -> return unsupported("line", parents, location)
        }

        return when (
            val result = Line.create(
                board = resolvedBoard,
                point1 = points.first,
                point2 = points.second,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "line",
                error = JessieCodeCreatorError.LineFactory(result.error),
                location = location,
            )
        }
    }

    private fun createCircle(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure("circle", JessieCodeCreatorError.BoardUnavailable, location)
        val identity = when (
            val result = creatorAttributes("circle", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (parents.size != 2) {
            return unsupported("circle", parents, location)
        }

        val firstPoint = pointParent(resolvedBoard, parents[0])
        val secondPoint = pointParent(resolvedBoard, parents[1])
        val firstElement = resolveElement(resolvedBoard, parents[0])
        val secondElement = resolveElement(resolvedBoard, parents[1])
        val firstRadius = radiusParent(parents[0])
        val secondRadius = radiusParent(parents[1])

        val result = when {
            firstPoint != null && secondPoint != null -> {
                val center = when (
                    val point = materializePoint(resolvedBoard, firstPoint)
                ) {
                    is GMResult.Ok -> point.value
                    is GMResult.Err -> return failure(
                        "circle",
                        JessieCodeCreatorError.PointFactory(point.error),
                        location,
                    )
                }
                val point2 = when (
                    val point = materializePoint(resolvedBoard, secondPoint)
                ) {
                    is GMResult.Ok -> point.value
                    is GMResult.Err -> return failure(
                        "circle",
                        JessieCodeCreatorError.PointFactory(point.error),
                        location,
                    )
                }
                Circle.create(
                    board = resolvedBoard,
                    center = center,
                    point2 = point2,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                )
            }
            firstPoint != null && secondRadius != null -> {
                val center = when (
                    val point = materializePoint(resolvedBoard, firstPoint)
                ) {
                    is GMResult.Ok -> point.value
                    is GMResult.Err -> return failure(
                        "circle",
                        JessieCodeCreatorError.PointFactory(point.error),
                        location,
                    )
                }
                createRadiusCircle(resolvedBoard, center, secondRadius, identity)
            }
            firstRadius != null && secondPoint != null -> {
                val center = when (
                    val point = materializePoint(resolvedBoard, secondPoint)
                ) {
                    is GMResult.Ok -> point.value
                    is GMResult.Err -> return failure(
                        "circle",
                        JessieCodeCreatorError.PointFactory(point.error),
                        location,
                    )
                }
                createRadiusCircle(resolvedBoard, center, firstRadius, identity)
            }
            firstPoint != null && secondElement is Line -> {
                return createElementRadiusCircle(
                    resolvedBoard,
                    firstPoint,
                    secondElement,
                    identity,
                    location,
                )
            }
            firstPoint != null && secondElement is Circle -> {
                return createElementRadiusCircle(
                    resolvedBoard,
                    firstPoint,
                    secondElement,
                    identity,
                    location,
                )
            }
            firstElement is Line && secondPoint != null -> {
                return createElementRadiusCircle(
                    resolvedBoard,
                    secondPoint,
                    firstElement,
                    identity,
                    location,
                )
            }
            firstElement is Circle && secondPoint != null -> {
                return createElementRadiusCircle(
                    resolvedBoard,
                    secondPoint,
                    firstElement,
                    identity,
                    location,
                )
            }
            else -> return unsupported("circle", parents, location)
        }

        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "circle",
                error = JessieCodeCreatorError.CircleFactory(result.error),
                location = location,
            )
        }
    }

    private fun createCurve(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                "curve",
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes("curve", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            parents.size == 2 &&
            parents[0] is JessieCodeRuntimeValue.ArrayValue &&
            parents[1] is JessieCodeRuntimeValue.ArrayValue
        ) {
            val dataX = numericArray(parents[0]) ?: return unsupported(
                "curve",
                parents,
                location,
            )
            val dataY = numericArray(parents[1]) ?: return unsupported(
                "curve",
                parents,
                location,
            )
            return curveResult(
                creatorName = "curve",
                location = location,
                result = Curve.createData(
                    board = resolvedBoard,
                    dataX = dataX,
                    dataY = dataY,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                ),
            )
        }
        if (parents.size != 4) {
            return unsupported("curve", parents, location)
        }
        val sources = parents.map(::curveTermSource)
        if (sources.any { it == null }) {
            return unsupported("curve", parents, location)
        }
        val sampleCount = when (
            val result = curveSampleCount("curve", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = "curve",
            location = location,
            result = Curve.createParametric(
                board = resolvedBoard,
                xSource = sources[0] ?: "",
                ySource = sources[1] ?: "",
                minimumSource = sources[2] ?: "",
                maximumSource = sources[3] ?: "",
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    private fun createFunctionGraph(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val sources = parents.map(::curveTermSource)
        if (sources.any { it == null }) {
            return unsupported(creatorName, parents, location)
        }
        val sampleCount = when (
            val result = curveSampleCount(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createFunctionGraph(
                board = resolvedBoard,
                ySource = sources[0] ?: "",
                minimumSource = sources[1] ?: "",
                maximumSource = sources[2] ?: "",
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    private fun curveSampleCount(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<Int, JessieCodeRuntimeError> {
        val advanced = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "doadvancedplot",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (advanced) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "doAdvancedPlot",
                    actual = "true",
                ),
                location = location,
            )
        }
        return integerAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "numberpointshigh",
            default = Curve.DEFAULT_SAMPLE_COUNT,
            minimum = 1,
            maximum = Curve.MAX_SAMPLE_COUNT,
            location = location,
        )
    }

    private fun curveResult(
        creatorName: String,
        location: JessieCodeAstLocation,
        result: GMResult<Curve, CurveError>,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.CurveFactory(result.error),
                location = location,
            )
        }

    private fun numericArray(
        value: JessieCodeRuntimeValue,
    ): DoubleArray? {
        val values = (value as? JessieCodeRuntimeValue.ArrayValue)?.values
            ?: return null
        val result = DoubleArray(values.size)
        for ((index, item) in values.withIndex()) {
            result[index] = (
                item as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return null
        }
        return result
    }

    private fun curveTermSource(
        value: JessieCodeRuntimeValue,
    ): String? =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                JsNumberFormat.compact(value.value)
            is JessieCodeRuntimeValue.StringValue -> value.value
            else -> null
        }

    private fun createElementRadiusCircle(
        board: Board,
        centerParent: PointParent,
        radiusElement: GeometryElement,
        attributes: CreatorAttributes,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val center = when (val result = materializePoint(board, centerParent)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return failure(
                creatorName = "circle",
                error = JessieCodeCreatorError.PointFactory(result.error),
                location = location,
            )
        }
        val result = when (radiusElement) {
            is Line -> Circle.create(
                board = board,
                center = center,
                radiusLine = radiusElement,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            is Circle -> Circle.create(
                board = board,
                center = center,
                radiusCircle = radiusElement,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            else -> return failure(
                creatorName = "circle",
                error = JessieCodeCreatorError.UnsupportedParents(
                    listOf(radiusElement.elType),
                ),
                location = location,
            )
        }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "circle",
                error = JessieCodeCreatorError.CircleFactory(result.error),
                location = location,
            )
        }
    }

    private fun createRadiusCircle(
        board: Board,
        center: Point,
        radius: RadiusParent,
        attributes: CreatorAttributes,
    ): GMResult<Circle, CircleError> =
        when (radius) {
            is RadiusParent.Number -> Circle.create(
                board = board,
                center = center,
                radius = radius.value,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            is RadiusParent.Expression -> Circle.create(
                board = board,
                center = center,
                radiusExpression = radius.source,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        }

    private fun coefficientPoints(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
    ): GMResult<Pair<Point, Point>, PointError> {
        val coefficients = parents.mapNotNull {
            (it as? JessieCodeRuntimeValue.NumberValue)?.value
        }
        if (coefficients.size != parents.size) {
            return GMResult.Err(PointError.InvalidCoordinateCount(parents.size))
        }
        val a = coefficients[0]
        val b = coefficients[1]
        val c = coefficients[2]
        val homogeneous = c * c + b * b
        val point1 = when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(
                    homogeneous,
                    c - b * a + c,
                    -b - c * a - b,
                ),
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2 = when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(
                    homogeneous,
                    -b * a + c,
                    -c * a - b,
                ),
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(point1)
                return result
            }
        }
        return GMResult.Ok(point1 to point2)
    }

    private fun pointParent(
        board: Board,
        value: JessieCodeRuntimeValue,
    ): PointParent? {
        val selected = resolveElement(board, value)
        if (selected is Point) {
            return PointParent.Existing(selected)
        }
        val coordinates = (
            value as? JessieCodeRuntimeValue.ArrayValue
        )?.values ?: return null
        if (
            coordinates.size < 2 ||
            coordinates.any {
                it !is JessieCodeRuntimeValue.NumberValue &&
                    it !is JessieCodeRuntimeValue.StringValue
            }
        ) {
            return null
        }
        return PointParent.Coordinates(coordinates.toList())
    }

    private fun materializePoint(
        board: Board,
        parent: PointParent,
    ): GMResult<Point, PointError> =
        when (parent) {
            is PointParent.Existing -> GMResult.Ok(parent.point)
            is PointParent.Coordinates -> createPointFromCoordinates(
                board = board,
                coordinates = parent.values,
                attributes = CreatorAttributes(
                    id = "",
                    name = "",
                    needsRegularUpdate = true,
                ),
            )
        }

    private fun createPointFromCoordinates(
        board: Board,
        coordinates: List<JessieCodeRuntimeValue>,
        attributes: CreatorAttributes,
    ): GMResult<Point, PointError> {
        val numericCoordinates = coordinates.mapNotNull {
            (it as? JessieCodeRuntimeValue.NumberValue)?.value
        }
        return if (numericCoordinates.size == coordinates.size) {
            Point.create(
                board = board,
                coordinates = numericCoordinates.toDoubleArray(),
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        } else if (
            coordinates.all {
                it is JessieCodeRuntimeValue.NumberValue ||
                    it is JessieCodeRuntimeValue.StringValue
            }
        ) {
            Point.create(
                board = board,
                coordinateExpressions = coordinates.map {
                    when (it) {
                        is JessieCodeRuntimeValue.NumberValue ->
                            JsNumberFormat.compact(it.value)
                        is JessieCodeRuntimeValue.StringValue -> it.value
                        else -> ""
                    }
                },
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        } else {
            GMResult.Err(
                PointError.InvalidCoordinateCount(coordinates.size),
            )
        }
    }

    private fun radiusParent(
        value: JessieCodeRuntimeValue,
    ): RadiusParent? =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                RadiusParent.Number(value.value)
            is JessieCodeRuntimeValue.StringValue ->
                RadiusParent.Expression(value.value)
            else -> null
        }

    private fun resolveElement(
        board: Board,
        value: JessieCodeRuntimeValue,
    ): GeometryElement? =
        when (value) {
            is JessieCodeRuntimeValue.ElementReference -> value.element
            is JessieCodeRuntimeValue.StringValue -> board.select(value.value)
            else -> null
        }

    private fun creatorAttributes(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<CreatorAttributes, JessieCodeRuntimeError> {
        val id = when (
            val result = stringAttribute(
                creatorName,
                attributes,
                "id",
                default = "",
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val name = when (
            val result = nullableStringAttribute(
                creatorName,
                attributes,
                "name",
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val needsRegularUpdate = when (
            val result = booleanAttribute(
                creatorName,
                attributes,
                "needsregularupdate",
                default = true,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            CreatorAttributes(
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            ),
        )
    }

    private fun stringAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: String,
        location: JessieCodeAstLocation,
    ): GMResult<String, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        return if (value is JessieCodeRuntimeValue.StringValue) {
            GMResult.Ok(value.value)
        } else {
            invalidAttribute(
                creatorName,
                name,
                "string",
                value,
                location,
            )
        }
    }

    private fun nullableStringAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        location: JessieCodeAstLocation,
    ): GMResult<String?, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(null)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(null)
        }
        return if (value is JessieCodeRuntimeValue.StringValue) {
            GMResult.Ok(value.value)
        } else {
            invalidAttribute(
                creatorName,
                name,
                "string",
                value,
                location,
            )
        }
    }

    private fun booleanAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Boolean,
        location: JessieCodeAstLocation,
    ): GMResult<Boolean, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        return if (value is JessieCodeRuntimeValue.BooleanValue) {
            GMResult.Ok(value.value)
        } else {
            invalidAttribute(
                creatorName,
                name,
                "boolean",
                value,
                location,
            )
        }
    }

    private fun integerAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Int,
        minimum: Int,
        maximum: Int,
        location: JessieCodeAstLocation,
    ): GMResult<Int, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        val number = (value as? JessieCodeRuntimeValue.NumberValue)?.value
            ?: return invalidAttribute(
                creatorName,
                name,
                "integer",
                value,
                location,
            )
        val integer = number.toInt()
        if (
            !number.isFinite() ||
            integer.toDouble() != number ||
            integer !in minimum..maximum
        ) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = name,
                    actual = number.toString(),
                ),
                location = location,
            )
        }
        return GMResult.Ok(integer)
    }

    private fun <T> invalidAttribute(
        creatorName: String,
        attribute: String,
        expected: String,
        actual: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<T, JessieCodeRuntimeError> =
        failure(
            creatorName = creatorName,
            error = JessieCodeCreatorError.InvalidAttributeType(
                attribute = attribute,
                expected = expected,
                actual = typeName(actual),
            ),
            location = location,
        )

    private fun unsupported(
        creatorName: String,
        parents: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        failure(
            creatorName = creatorName,
            error = JessieCodeCreatorError.UnsupportedParents(
                parentTypes = parents.map(::typeName),
            ),
            location = location,
        )

    private fun element(element: GeometryElement): CreatorResult =
        GMResult.Ok(JessieCodeRuntimeValue.ElementReference(element))

    private fun <T> failure(
        creatorName: String,
        error: JessieCodeCreatorError,
        location: JessieCodeAstLocation,
    ): GMResult<T, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.CreatorFailure(
                creatorName = creatorName,
                error = error,
                location = location,
            ),
        )

    private fun typeName(value: JessieCodeRuntimeValue): String =
        when (value) {
            JessieCodeRuntimeValue.UndefinedValue -> "undefined"
            JessieCodeRuntimeValue.NullValue -> "null"
            is JessieCodeRuntimeValue.NumberValue -> "number"
            is JessieCodeRuntimeValue.BooleanValue -> "boolean"
            is JessieCodeRuntimeValue.StringValue -> "string"
            is JessieCodeRuntimeValue.ArrayValue -> "array"
            is JessieCodeRuntimeValue.ObjectValue -> "object"
            is JessieCodeRuntimeValue.FunctionValue -> "function"
            is JessieCodeRuntimeValue.BoardReference -> "board"
            is JessieCodeRuntimeValue.ElementReference ->
                value.element.elType.ifEmpty { "element" }
        }

    private data class CreatorAttributes(
        val id: String,
        val name: String?,
        val needsRegularUpdate: Boolean,
    )

    private sealed interface PointParent {
        data class Existing(
            val point: Point,
        ) : PointParent

        data class Coordinates(
            val values: List<JessieCodeRuntimeValue>,
        ) : PointParent
    }

    private sealed interface RadiusParent {
        data class Number(
            val value: Double,
        ) : RadiusParent

        data class Expression(
            val source: String,
        ) : RadiusParent
    }
}

private typealias CreatorResult =
    GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
