/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/type.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Transformation

/**
 * Typed and map-backed adapters for the model predicates in `JXG.Type`.
 */
internal object TypePredicates {
    // JSXGraph 1.13.3: src/utils/type.js -> isBoard.
    fun isBoard(value: Any?): Boolean {
        if (value is Board) {
            return true
        }
        val properties = value as? Map<*, *> ?: return false
        return Type.isNumber(properties.property("BOARD_MODE_NONE")) &&
            Type.isObject(properties.property("objects")) &&
            Type.isObject(properties.property("jc")) &&
            Type.isFunction(properties.property("update")) &&
            isTruthy(properties.property("containerObj")) &&
            Type.isString(properties.property("id"))
    }

    // JSXGraph 1.13.3: src/utils/type.js -> isId.
    fun isId(
        board: Board,
        value: Any?,
    ): Boolean =
        value is String && board.elementById(value) != null

    // JSXGraph 1.13.3: src/utils/type.js -> isName.
    fun isName(
        board: Board,
        value: Any?,
    ): Boolean =
        value is String && board.elementByName(value) != null

    // JSXGraph 1.13.3: src/utils/type.js -> isGroup.
    fun isGroup(
        board: Board,
        value: Any?,
    ): Boolean =
        value is String && board.groupById(value) != null

    // JSXGraph 1.13.3: src/utils/type.js -> isPoint.
    fun isPoint(value: Any?): Boolean =
        when (value) {
            is GeometryElement ->
                value.elementClass == Const.OBJECT_CLASS_POINT
            is Map<*, *> ->
                numericPropertyEquals(
                    properties = value,
                    property = "elementClass",
                    expected = Const.OBJECT_CLASS_POINT,
                )
            else -> false
        }

    // JSXGraph 1.13.3: src/utils/type.js -> isPoint3D.
    fun isPoint3D(value: Any?): Boolean =
        when (value) {
            is GeometryElement ->
                value.type == Const.OBJECT_TYPE_POINT3D
            is Map<*, *> ->
                numericPropertyEquals(
                    properties = value,
                    property = "type",
                    expected = Const.OBJECT_TYPE_POINT3D,
                )
            else -> false
        }

    // JSXGraph 1.13.3: src/utils/type.js -> isPointType.
    fun isPointType(
        board: Board,
        value: Any?,
    ): GMResult<Boolean, TypeError> =
        isPointType(
            board = board,
            value = value,
            minimumArraySize = 0,
            dimension = 2,
            pointPredicate = ::isPoint,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> isPointType3D.
    fun isPointType3D(
        board: Board,
        value: Any?,
    ): GMResult<Boolean, TypeError> =
        isPointType(
            board = board,
            value = value,
            minimumArraySize = 3,
            dimension = 3,
            pointPredicate = ::isPoint3D,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> isTransformationOrArray.
    fun isTransformationOrArray(value: Any?): Boolean {
        var current = value
        val visitedArrays = mutableListOf<Any>()
        var depth = 0
        while (current != null) {
            val array = arrayValues(current)
            if (array != null) {
                if (
                    array.isEmpty() ||
                    visitedArrays.any { visited -> visited === current } ||
                    depth >= MAX_TRANSFORMATION_ARRAY_DEPTH
                ) {
                    return false
                }
                visitedArrays += current
                current = array.first()
                depth += 1
                continue
            }
            return when (current) {
                is Transformation -> true
                is GeometryElement ->
                    current.type == Const.OBJECT_TYPE_TRANSFORMATION
                is Map<*, *> ->
                    numericPropertyEquals(
                        properties = current,
                        property = "type",
                        expected = Const.OBJECT_TYPE_TRANSFORMATION,
                    )
                else -> false
            }
        }
        return false
    }

    private fun isPointType(
        board: Board,
        value: Any?,
        minimumArraySize: Int,
        dimension: Int,
        pointPredicate: (Any?) -> Boolean,
    ): GMResult<Boolean, TypeError> {
        val directArray = arrayValues(value)
        if (
            directArray != null &&
            (minimumArraySize == 0 || directArray.size >= minimumArraySize)
        ) {
            return GMResult.Ok(true)
        }
        if (value is Function<*>) {
            if (value !is Function0<*>) {
                return GMResult.Err(
                    TypeError.UnsupportedPointTypeFunction(
                        dimension = dimension,
                        value = value,
                    ),
                )
            }
            val evaluated = try {
                value()
            } catch (exception: Exception) {
                return GMResult.Err(
                    TypeError.PointTypeEvaluationFailed(
                        dimension = dimension,
                        message = exception.message
                            ?: exception::class.simpleName.orEmpty(),
                    ),
                )
            }
            val evaluatedArray = arrayValues(evaluated)
            if (
                evaluatedArray != null &&
                evaluatedArray.size >= dimension
            ) {
                return GMResult.Ok(true)
            }
        }
        val selected = when (value) {
            is String -> board.select(value)
            is GeometryElement -> board.select(value)
            else -> null
        }
        return GMResult.Ok(pointPredicate(selected))
    }

    private fun numericPropertyEquals(
        properties: Map<*, *>,
        property: String,
        expected: Int,
    ): Boolean =
        (properties.property(property) as? Number)
            ?.toDouble() == expected.toDouble()

    private fun Map<*, *>.property(name: String): Any? =
        entries.firstOrNull { entry -> entry.key == name }
            ?.value
            ?: DumpUndefined

    private fun arrayValues(value: Any?): List<Any?>? =
        when (value) {
            is List<*> -> value
            is Array<*> -> value.asList()
            is BooleanArray -> value.map { item -> item }
            is ByteArray -> value.map { item -> item }
            is ShortArray -> value.map { item -> item }
            is IntArray -> value.map { item -> item }
            is LongArray -> value.map { item -> item }
            is FloatArray -> value.map { item -> item }
            is DoubleArray -> value.map { item -> item }
            else -> null
        }

    private fun isTruthy(value: Any?): Boolean =
        when (value) {
            null, DumpUndefined -> false
            is Boolean -> value
            is Number ->
                value.toDouble() != 0.0 && !value.toDouble().isNaN()
            is String -> value.isNotEmpty()
            else -> true
        }

    private const val MAX_TRANSFORMATION_ARRAY_DEPTH = 64
}
