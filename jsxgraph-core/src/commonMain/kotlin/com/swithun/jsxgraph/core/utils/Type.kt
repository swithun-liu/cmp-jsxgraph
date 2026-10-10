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
import com.swithun.jsxgraph.core.base.Coords
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Point3D
import com.swithun.jsxgraph.core.base.Point3DArrayEvaluator
import com.swithun.jsxgraph.core.base.Point3DCoordinateSource
import com.swithun.jsxgraph.core.base.Point3DCoordinateValue
import com.swithun.jsxgraph.core.base.Point3DDynamicError
import com.swithun.jsxgraph.core.base.Point3DError
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.base.View3D
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeNumericCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.abs

internal data class ParsedPosition(
    val side: String,
    val pos: String,
)

internal data class TypeCopyLimits(
    val maxDepth: Int = 64,
    val maxValues: Int = 100_000,
)

internal data class TypeMergeLimits(
    val maxDepth: Int = 64,
    val maxValues: Int = 100_000,
)

internal data class TypeEvaluationLimits(
    val maxDepth: Int = 64,
    val maxValues: Int = 100_000,
)

internal data class TypeJsonLimits(
    val maxDepth: Int = 64,
    val maxValues: Int = 100_000,
    val maxOutputLength: Int = 1_000_000,
)

internal sealed interface TypeError {
    data class InvalidCopyLimit(
        val name: String,
        val value: Int,
    ) : TypeError

    data class CopyDepthLimitExceeded(
        val limit: Int,
    ) : TypeError

    data class CopyValueLimitExceeded(
        val limit: Int,
    ) : TypeError

    data object CyclicCopyValue : TypeError

    data class InvalidCopyObjectKey(
        val key: Any?,
    ) : TypeError

    data class UnsupportedCopyValue(
        val value: Any?,
    ) : TypeError

    data class InvalidCopyAttributesResult(
        val value: Any?,
    ) : TypeError

    data class InvalidMergeLimit(
        val name: String,
        val value: Int,
    ) : TypeError

    data class MergeDepthLimitExceeded(
        val limit: Int,
    ) : TypeError

    data class MergeValueLimitExceeded(
        val limit: Int,
    ) : TypeError

    data object CyclicMergeValue : TypeError

    data class InvalidMergeObjectKey(
        val key: Any?,
    ) : TypeError

    data class InvalidMergeTarget(
        val value: Any?,
    ) : TypeError

    data class InvalidMergeProperty(
        val property: String,
        val target: Any?,
    ) : TypeError

    data class MergeMutationFailed(
        val property: String,
        val message: String,
    ) : TypeError

    data class UnsupportedMergeValue(
        val value: Any?,
    ) : TypeError

    data class InvalidEvaluationLimit(
        val name: String,
        val value: Int,
    ) : TypeError

    data class EvaluationDepthLimitExceeded(
        val limit: Int,
    ) : TypeError

    data class EvaluationValueLimitExceeded(
        val limit: Int,
    ) : TypeError

    data object CyclicEvaluationValue : TypeError

    data class UnsupportedEvaluationFunction(
        val value: Any?,
    ) : TypeError

    data class EvaluationFailed(
        val message: String,
    ) : TypeError

    data class InvalidJsonLimit(
        val name: String,
        val value: Int,
    ) : TypeError

    data class JsonDepthLimitExceeded(
        val limit: Int,
    ) : TypeError

    data class JsonValueLimitExceeded(
        val limit: Int,
    ) : TypeError

    data class JsonOutputLimitExceeded(
        val limit: Int,
    ) : TypeError

    data object CyclicJsonValue : TypeError

    data class InvalidJsonObjectKey(
        val key: Any?,
    ) : TypeError

    data class UnsupportedJsonValue(
        val value: Any?,
    ) : TypeError

    data class CssParseFailed(
        val message: String,
    ) : TypeError

    data class InvalidCssDeclaration(
        val index: Int,
        val declaration: String,
    ) : TypeError

    data class BoundFunctionFailed(
        val message: String,
    ) : TypeError

    data class UnsupportedFilterPropertyFunction(
        val property: String,
        val visual: Boolean,
        val value: Any?,
    ) : TypeError

    data class FilterPropertyEvaluationFailed(
        val property: String,
        val visual: Boolean,
        val message: String,
    ) : TypeError

    data class FilterPredicateFailed(
        val index: Int,
        val property: String?,
        val message: String,
    ) : TypeError

    data class UnsupportedPointTypeFunction(
        val dimension: Int,
        val value: Any?,
    ) : TypeError

    data class PointTypeEvaluationFailed(
        val dimension: Int,
        val message: String,
    ) : TypeError

    data class PointProvisionEvaluationFailed(
        val dimension: Int,
        val parentIndex: Int,
        val message: String,
    ) : TypeError

    data class UnsupportedPointProvisionParent(
        val dimension: Int,
        val parentIndex: Int,
        val value: Any?,
    ) : TypeError

    data class PointProvisionFactoryFailed(
        val parentIndex: Int,
        val error: PointError,
    ) : TypeError

    data class Point3DProvisionFactoryFailed(
        val parentIndex: Int,
        val error: Point3DError,
    ) : TypeError

    data class InvalidMethodMap(
        val owner: String,
        val value: Any?,
    ) : TypeError

    data class InvalidMethodMapKey(
        val owner: String,
        val key: Any?,
    ) : TypeError

    data class InvalidClonePropertyMap(
        val property: String,
        val value: Any?,
    ) : TypeError

    data class InvalidClonePropertyKey(
        val property: String,
        val key: Any?,
    ) : TypeError

    data class ClonePropertyEvaluationFailed(
        val property: String,
        val message: String,
    ) : TypeError

    data class InvalidArrayIndex(
        val index: Int,
        val size: Int,
    ) : TypeError

    data class InvalidDigits(
        val digits: Int,
    ) : TypeError

    data class UnsupportedDuplicateValue(
        val value: Any?,
    ) : TypeError

    data class PixelConversionFailed(
        val message: String,
    ) : TypeError
}

internal sealed interface AutoDigitsValue {
    data class Formatted(
        val value: String,
    ) : AutoDigitsValue

    data class Raw(
        val value: Double,
    ) : AutoDigitsValue
}

internal sealed interface StackExpression {
    data class Scalar(
        val value: String,
    ) : StackExpression

    data class ArrayValue(
        val values: List<String>,
    ) : StackExpression
}

internal sealed interface TypeJsonValue {
    data class Serialized(
        val value: String,
    ) : TypeJsonValue

    data object Undefined : TypeJsonValue
}

/**
 * Pure-KMP adapter for the DOM node types inspected by `isDocumentOrFragment`.
 */
internal fun interface TypeDocumentNode {
    fun nodeType(): Int
}

internal enum class DecimalAdjustment {
    ROUND,
    FLOOR,
    CEIL,
}

private class ProvidedPointCoordinateFunction(
    private val parentIndex: Int,
    private val callback: () -> Any?,
    override val returnsCoordinateArray: Boolean,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies = emptyMap<String, com.swithun.jsxgraph.core.base.GeometryElement>()

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> {
        val value = try {
            callback()
        } catch (exception: Exception) {
            return GMResult.Err(
                JessieCodeRuntimeError.BuiltInInvocationFailure(
                    functionName = "providePoints",
                    reason = exception.message
                        ?: exception::class.simpleName.orEmpty(),
                    location = PROVIDED_POINT_LOCATION,
                ),
            )
        }
        return if (returnsCoordinateArray) {
            val coordinates = pointCoordinateValues(value)
                ?: return GMResult.Err(
                    JessieCodeRuntimeError.BuiltInInvocationFailure(
                        functionName = "providePoints",
                        reason =
                            "Parent $parentIndex did not return a coordinate array",
                        location = PROVIDED_POINT_LOCATION,
                    ),
                )
            val runtimeCoordinates = mutableListOf<JessieCodeRuntimeValue>()
            for (coordinate in coordinates) {
                val number = coordinate as? Number
                    ?: return GMResult.Err(
                        JessieCodeRuntimeError.BuiltInInvocationFailure(
                            functionName = "providePoints",
                            reason =
                                "Parent $parentIndex returned a non-numeric coordinate",
                            location = PROVIDED_POINT_LOCATION,
                        ),
                    )
                runtimeCoordinates +=
                    JessieCodeRuntimeValue.NumberValue(number.toDouble())
            }
            GMResult.Ok(
                JessieCodeRuntimeValue.ArrayValue(runtimeCoordinates),
            )
        } else {
            val number = value as? Number
                ?: return GMResult.Err(
                    JessieCodeRuntimeError.BuiltInInvocationFailure(
                        functionName = "providePoints",
                        reason =
                            "Parent $parentIndex returned a non-numeric coordinate",
                        location = PROVIDED_POINT_LOCATION,
                    ),
                )
            GMResult.Ok(JessieCodeRuntimeValue.NumberValue(number.toDouble()))
        }
    }
}

private val PROVIDED_POINT_LOCATION = JessieCodeAstLocation(
    line = 0,
    column = 0,
    endLine = 0,
    endColumn = 0,
)

private fun pointCoordinateValues(value: Any?): List<Any?>? =
    when (value) {
        is List<*> -> value.map { item -> item }
        is Array<*> -> value.map { item -> item }
        is ByteArray -> value.map { item -> item }
        is ShortArray -> value.map { item -> item }
        is IntArray -> value.map { item -> item }
        is LongArray -> value.map { item -> item }
        is FloatArray -> value.map { item -> item }
        is DoubleArray -> value.map { item -> item }
        else -> null
    }

internal class TypeClonedCallable(
    prototype: Any?,
    additions: Map<String, Any?>,
) : () -> Any? {
    var prototype: Any? = prototype
        private set

    val properties: Map<String, Any?>
        get() = mutableProperties

    private val mutableProperties = linkedMapOf<String, Any?>()

    init {
        for (key in additions.keys.sortedWith(CLONE_PROPERTY_KEY_ORDER)) {
            if (key == PROTOTYPE_PROPERTY_NAME) {
                this.prototype = additions[key]
            } else {
                mutableProperties[key] = additions[key]
            }
        }
    }

    override fun invoke(): Any? = DumpUndefined

    operator fun get(property: String): Any? =
        if (property == PROTOTYPE_PROPERTY_NAME) {
            prototype
        } else {
            mutableProperties[property]
        }

    fun keys(): List<String> = mutableProperties.keys.toList()

    private companion object {
        const val PROTOTYPE_PROPERTY_NAME = "prototype"
        const val MAX_JS_ARRAY_INDEX = 4_294_967_295L

        val CLONE_PROPERTY_KEY_ORDER = Comparator<String> { first, second ->
            val firstIndex = first.toJsArrayIndexOrNull()
            val secondIndex = second.toJsArrayIndexOrNull()
            when {
                firstIndex != null && secondIndex != null ->
                    firstIndex.compareTo(secondIndex)
                firstIndex != null -> -1
                secondIndex != null -> 1
                else -> 0
            }
        }

        fun String.toJsArrayIndexOrNull(): Long? {
            if (isEmpty() || (length > 1 && first() == '0')) {
                return null
            }
            if (any { character -> character !in '0'..'9' }) {
                return null
            }
            val value = toLongOrNull() ?: return null
            return value.takeIf { index ->
                index in 0 until MAX_JS_ARRAY_INDEX
            }
        }
    }
}

/**
 * Source-mapped pure utility subset from `JXG.Type`.
 *
 * Browser, Board, and geometry-element adapters remain in their owning
 * translation slices.
 */
internal object Type {
    // JSXGraph 1.13.3: src/utils/type.js -> isString.
    fun isString(value: Any?): Boolean = value is String

    // JSXGraph 1.13.3: src/utils/type.js -> isNumber.
    fun isNumber(
        value: Any?,
        acceptStringNumber: Boolean = false,
        acceptNaN: Boolean = true,
    ): Boolean {
        var result = value is Number
        if (acceptStringNumber && value is String) {
            result = result || jsNumberToString(jsParseFloat(value)) == value
        }
        if (!acceptNaN && result) {
            result = when (value) {
                is Number -> !value.toDouble().isNaN()
                is String -> !jsParseFloat(value).isNaN()
                else -> false
            }
        }
        return result
    }

    // JSXGraph 1.13.3: src/utils/type.js -> isFunction.
    fun isFunction(value: Any?): Boolean = value is Function<*>

    // JSXGraph 1.13.3: src/utils/type.js -> isBoard.
    fun isBoard(value: Any?): Boolean =
        TypePredicates.isBoard(value)

    // JSXGraph 1.13.3: src/utils/type.js -> isId.
    fun isId(
        board: Board,
        value: Any?,
    ): Boolean =
        TypePredicates.isId(board, value)

    // JSXGraph 1.13.3: src/utils/type.js -> isName.
    fun isName(
        board: Board,
        value: Any?,
    ): Boolean =
        TypePredicates.isName(board, value)

    // JSXGraph 1.13.3: src/utils/type.js -> isGroup.
    fun isGroup(
        board: Board,
        value: Any?,
    ): Boolean =
        TypePredicates.isGroup(board, value)

    // JSXGraph 1.13.3: src/utils/type.js -> isPoint.
    fun isPoint(value: Any?): Boolean =
        TypePredicates.isPoint(value)

    // JSXGraph 1.13.3: src/utils/type.js -> isPoint3D.
    fun isPoint3D(value: Any?): Boolean =
        TypePredicates.isPoint3D(value)

    // JSXGraph 1.13.3: src/utils/type.js -> isDocumentOrFragment.
    // DOM nodes are represented by a portable adapter or a structural map.
    fun isDocumentOrFragment(value: Any?): Boolean {
        if (!isObject(value)) {
            return false
        }
        val nodeType = when (value) {
            is TypeDocumentNode -> value.nodeType()
            is Map<*, *> -> (value["nodeType"] as? Number)?.toInt()
            else -> null
        }
        return nodeType == DOCUMENT_NODE ||
            nodeType == DOCUMENT_FRAGMENT_NODE
    }

    // JSXGraph 1.13.3: src/utils/type.js -> isPointType.
    fun isPointType(
        board: Board,
        value: Any?,
    ): GMResult<Boolean, TypeError> =
        TypePredicates.isPointType(board, value)

    // JSXGraph 1.13.3: src/utils/type.js -> isPointType3D.
    fun isPointType3D(
        board: Board,
        value: Any?,
    ): GMResult<Boolean, TypeError> =
        TypePredicates.isPointType3D(board, value)

    // JSXGraph 1.13.3: src/utils/type.js -> isTransformationOrArray.
    fun isTransformationOrArray(value: Any?): Boolean =
        TypePredicates.isTransformationOrArray(value)

    // JSXGraph 1.13.3: src/utils/type.js -> providePoints.
    fun providePoints(
        board: Board,
        parents: Any?,
        attributes: Map<String, Any?>? = null,
        options: Map<String, Any?> = emptyMap(),
        attrClass: String = "point",
        attrArray: List<String> = emptyList(),
    ): GMResult<List<Point>, TypeError> {
        val parentValues = arrayValues(parents) ?: listOf(parents)
        val points = mutableListOf<Point>()
        for ((index, parent) in parentValues.withIndex()) {
            val point = when (parent) {
                is Point -> parent
                is String -> board.select(parent) as? Point
                else -> null
            }
            if (point != null) {
                points += point
                continue
            }

            val copiedAttributes = when (
                val result = pointProvisionAttributes(
                    attributes = attributes,
                    options = options,
                    attrClass = attrClass,
                    attrArray = attrArray,
                    parentIndex = index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val created = when (
                val result = createProvidedPoint(
                    board = board,
                    parent = parent,
                    attributes = copiedAttributes,
                    parentIndex = index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            created.isNewByProvision = true
            points += created
        }
        return GMResult.Ok(points)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> providePoints3D.
    fun providePoints3D(
        view: View3D,
        parents: Any?,
        attributes: Map<String, Any?>? = null,
        options: Map<String, Any?> = emptyMap(),
        attrClass: String = "point3d",
        attrArray: List<String> = emptyList(),
    ): GMResult<List<Point3D>, TypeError> {
        val parentValues = arrayValues(parents) ?: listOf(parents)
        val points = mutableListOf<Point3D>()
        for ((index, parent) in parentValues.withIndex()) {
            val nestedCoordinates = arrayValues(parent)
            if (
                nestedCoordinates != null &&
                nestedCoordinates.isNotEmpty() &&
                nestedCoordinates.all { coordinate ->
                    val values = arrayValues(coordinate)
                    values != null && values.firstOrNull() is Number
                }
            ) {
                for (coordinates in nestedCoordinates) {
                    val copiedAttributes = when (
                        val result = pointProvisionAttributes(
                            attributes = attributes,
                            options = options,
                            attrClass = attrClass,
                            attrArray = attrArray,
                            parentIndex = index,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    val created = when (
                        val result = createProvidedPoint3D(
                            view = view,
                            parent = coordinates,
                            attributes = copiedAttributes,
                            parentIndex = index,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    created.isNewByProvision = true
                    points += created
                }
                continue
            }

            val point = when (parent) {
                is Point3D -> parent
                is String -> view.select(parent) as? Point3D
                else -> null
            }
            if (point != null) {
                points += point
                continue
            }

            val copiedAttributes = when (
                val result = pointProvisionAttributes(
                    attributes = attributes,
                    options = options,
                    attrClass = attrClass,
                    attrArray = attrArray,
                    parentIndex = index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val created = when (
                val result = createProvidedPoint3D(
                    view = view,
                    parent = parent,
                    attributes = copiedAttributes,
                    parentIndex = index,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            created.isNewByProvision = true
            points += created
        }
        return GMResult.Ok(points)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> isArray.
    fun isArray(value: Any?): Boolean = isArrayValue(value)

    // JSXGraph 1.13.3: src/utils/type.js -> isObject.
    fun isObject(value: Any?): Boolean =
        !isArrayValue(value) &&
            value !== DumpUndefined &&
            (
                value == null ||
                    value !is String &&
                    value !is Number &&
                    value !is Boolean &&
                    value !is Function<*>
                )

    // JSXGraph 1.13.3: src/utils/type.js -> exists.
    fun exists(
        value: Any?,
        checkEmptyString: Boolean = false,
    ): Boolean {
        val result = value != null && value !== DumpUndefined
        return result && (!checkEmptyString || value != "")
    }

    // JSXGraph 1.13.3: src/utils/type.js -> def.
    fun def(
        value: Any?,
        defaultValue: Any?,
    ): Any? = if (exists(value)) value else defaultValue

    // JSXGraph 1.13.3: src/utils/type.js -> isEmpty.
    fun isEmpty(value: Map<*, *>): Boolean = value.isEmpty()

    // JSXGraph 1.13.3: src/utils/type.js -> isEmpty.
    fun isEmpty(value: List<*>): Boolean = value.isEmpty()

    // JSXGraph 1.13.3: src/utils/type.js -> isEmpty.
    fun isEmpty(value: Array<*>): Boolean = value.isEmpty()

    // JSXGraph 1.13.3: src/utils/type.js -> isEmpty.
    fun isEmpty(value: String): Boolean = value.isEmpty()

    // JSXGraph 1.13.3: src/utils/type.js -> str2Bool.
    fun str2Bool(value: Any?): Boolean =
        when (value) {
            null, DumpUndefined -> true
            is Boolean -> value
            is String -> value.lowercase() == "true"
            else -> false
        }

    // JSXGraph 1.13.3: src/utils/type.js -> cssParse.
    fun cssParse(
        value: Any?,
    ): GMResult<Map<String, Any?>, TypeError> =
        TypeCss.parse(value)

    // JSXGraph 1.13.3: src/utils/type.js -> css2js.
    fun css2js(
        value: String,
    ): GMResult<List<CssKeyValuePair>, TypeError> =
        TypeCss.toKeyValuePairs(value)

    // JSXGraph 1.13.3: src/utils/type.js -> cssStringify.
    fun cssStringify(value: Any?): String =
        TypeCss.stringify(value)

    // JSXGraph 1.13.3: src/utils/type.js -> evalSlider;
    // src/base/coordselement.js -> addConstraint closure makeSliderFunction.
    fun evalSlider(value: Any?): Any? =
        if (value is Slider) {
            value.Value()
        } else {
            value
        }

    // JSXGraph 1.13.3: src/utils/type.js -> bind.
    fun <O> bind(
        function: TypeOwnerCallable<O>,
        owner: O,
    ): TypeBoundCallable<O> =
        TypeFunctional.bind(
            function = function,
            owner = owner,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> filterElements function branch.
    fun <T> filterElements(
        values: List<T>,
        predicate: TypeFilterPredicate<T>,
    ): GMResult<List<T>, TypeError> =
        TypeFunctional.filterElements(values, predicate)

    // JSXGraph 1.13.3: src/utils/type.js -> filterElements object branch.
    fun <T> filterElements(
        values: List<T>,
        filter: Map<String, TypeFilterCriterion>,
        accessor: TypeElementPropertyAccessor<T>,
    ): GMResult<List<T>, TypeError> =
        TypeFunctional.filterElements(
            values = values,
            filter = filter,
            accessor = accessor,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> evaluate.
    fun evaluate(
        value: Any?,
        limits: TypeEvaluationLimits = TypeEvaluationLimits(),
    ): GMResult<Any?, TypeError> =
        TypeEvaluate.evaluate(
            value = value,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> uniqueArray.
    fun uniqueArray(values: MutableList<Any?>): List<Any?> {
        if (values.isEmpty()) {
            return emptyList()
        }

        for (index in values.indices) {
            val isArray = isArrayValue(values[index])
            if (!exists(values[index])) {
                values[index] = ""
                continue
            }

            for (comparisonIndex in index + 1 until values.size) {
                if (
                    isArray &&
                    cmpArrayValues(values[index], values[comparisonIndex])
                ) {
                    values[index] = emptyList<Any?>()
                } else if (
                    !isArray &&
                    jsStrictEquals(values[index], values[comparisonIndex])
                ) {
                    values[index] = ""
                }
            }
        }

        return buildList {
            for (value in values) {
                val array = arrayValues(value)
                if (array == null) {
                    if (value != "") {
                        add(value)
                    }
                } else if (array.isNotEmpty()) {
                    add(array.toList())
                }
            }
        }
    }

    // JSXGraph 1.13.3: src/utils/type.js -> toUniqueArrayFloat.
    fun toUniqueArrayFloat(
        values: List<Double>,
        eps: Double,
    ): List<Double> {
        val result = values.sortedWith(JS_NUMBER_COMPARATOR).toMutableList()
        for (index in result.lastIndex downTo 1) {
            if (abs(result[index] - result[index - 1]) < eps) {
                result.removeAt(index)
            }
        }
        return result
    }

    // JSXGraph 1.13.3: src/utils/type.js -> cmpArrays.
    fun cmpArrays(
        first: List<*>,
        second: List<*>,
    ): Boolean = cmpArrayValues(first, second)

    // JSXGraph 1.13.3: src/utils/type.js -> eliminateDuplicates.
    fun eliminateDuplicates(
        values: List<Any?>,
    ): GMResult<List<String>, TypeError> {
        val properties = linkedSetOf<String>()
        for (value in values) {
            val property = when (value) {
                is String -> value
                is Number -> jsNumberToString(value.toDouble())
                else -> {
                    return GMResult.Err(
                        TypeError.UnsupportedDuplicateValue(value),
                    )
                }
            }
            if (property != PROTOTYPE_PROPERTY) {
                properties += property
            }
        }
        return GMResult.Ok(properties.sortedWith(JS_PROPERTY_KEY_ORDER))
    }

    // JSXGraph 1.13.3: src/utils/type.js -> swap.
    fun swap(
        values: MutableList<Any?>,
        firstIndex: Int,
        secondIndex: Int,
    ): GMResult<MutableList<Any?>, TypeError> {
        if (firstIndex !in values.indices) {
            return GMResult.Err(
                TypeError.InvalidArrayIndex(firstIndex, values.size),
            )
        }
        if (secondIndex !in values.indices) {
            return GMResult.Err(
                TypeError.InvalidArrayIndex(secondIndex, values.size),
            )
        }

        val temporary = values[firstIndex]
        values[firstIndex] = values[secondIndex]
        values[secondIndex] = temporary
        return GMResult.Ok(values)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> coordsArrayToMatrix.
    fun coordsArrayToMatrix(
        coordinates: List<Coords>,
        split: Boolean,
    ): List<DoubleArray> =
        if (split) {
            listOf(
                DoubleArray(coordinates.size) { index ->
                    coordinates[index].usrCoords[1]
                },
                DoubleArray(coordinates.size) { index ->
                    coordinates[index].usrCoords[2]
                },
            )
        } else {
            coordinates.map { coordinate ->
                doubleArrayOf(
                    coordinate.usrCoords[1],
                    coordinate.usrCoords[2],
                )
            }
        }

    // JSXGraph 1.13.3: src/utils/type.js -> indexOf.
    fun indexOf(
        values: List<Any?>,
        value: Any?,
    ): Int =
        values.indexOfFirst { candidate ->
            jsStrictEquals(candidate, value)
        }

    // JSXGraph 1.13.3: src/utils/type.js -> indexOf property branch.
    fun indexOf(
        values: List<Map<String, Any?>>,
        value: Any?,
        property: String,
    ): Int =
        values.indexOfFirst { candidate ->
            jsStrictEquals(candidate[property], value)
        }

    // JSXGraph 1.13.3: src/utils/type.js -> isInArray.
    fun isInArray(
        values: List<Any?>,
        value: Any?,
    ): Boolean = indexOf(values, value) > -1

    // JSXGraph 1.13.3: src/utils/type.js -> removeElementFromArray.
    fun removeElementFromArray(
        values: MutableList<Any?>,
        value: Any?,
    ): MutableList<Any?> {
        val index = indexOf(values, value)
        if (index >= 0) {
            values.removeAt(index)
        }
        return values
    }

    // JSXGraph 1.13.3: src/utils/type.js -> keys.
    fun keys(value: Map<String, *>): List<String> =
        value.keys.sortedWith(JS_PROPERTY_KEY_ORDER)

    // JSXGraph 1.13.3: src/utils/type.js -> clone closure cObj.
    fun clone(value: Any?): MutableMap<String, Any?> =
        linkedMapOf("prototype" to value)

    // JSXGraph 1.13.3: src/utils/type.js -> cloneAndCopy.
    fun cloneAndCopy(
        value: Any?,
        additions: Map<String, Any?>,
    ): TypeClonedCallable =
        TypeClonedCallable(
            prototype = value,
            additions = additions,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> toJSON.
    fun toJSON(
        value: Any?,
        noQuote: Boolean = false,
        limits: TypeJsonLimits = TypeJsonLimits(),
    ): GMResult<TypeJsonValue, TypeError> =
        TypeJson.serialize(
            value = value,
            noQuote = noQuote,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> deepCopy.
    fun deepCopy(
        value: Any?,
        secondary: Map<String, Any?>? = null,
        toLower: Boolean = false,
        limits: TypeCopyLimits = TypeCopyLimits(),
    ): GMResult<Any?, TypeError> =
        TypeCopy.deepCopy(
            value = value,
            secondary = secondary,
            toLower = toLower,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> keysToLowerCase.
    fun keysToLowerCase(
        value: Map<String, Any?>,
        limits: TypeCopyLimits = TypeCopyLimits(),
    ): GMResult<Map<String, Any?>, TypeError> =
        TypeCopy.keysToLowerCase(
            value = value,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> merge.
    fun merge(
        target: Any?,
        source: Any?,
        limits: TypeMergeLimits = TypeMergeLimits(),
    ): GMResult<Any?, TypeError> =
        TypeMerge.merge(
            target = target,
            source = source,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> mergeAttr.
    fun mergeAttr(
        attributes: MutableMap<String, Any?>,
        special: Map<String, Any?>,
        toLower: Boolean = true,
        ignoreUndefinedSpecials: Boolean = false,
        limits: TypeMergeLimits = TypeMergeLimits(),
    ): GMResult<Unit, TypeError> =
        TypeMerge.mergeAttr(
            attributes = attributes,
            special = special,
            toLower = toLower,
            ignoreUndefinedSpecials = ignoreUndefinedSpecials,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> copyAttributes.
    fun copyAttributes(
        attributes: Map<String, Any?>?,
        options: Map<String, Any?>,
        path: List<String>,
        limits: TypeAttributeLimits = TypeAttributeLimits(),
    ): GMResult<Map<String, Any?>, TypeError> =
        TypeAttributes.copyAttributes(
            attributes = attributes,
            options = options,
            path = path,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> copyPrototypeMethods.
    fun copyPrototypeMethods(
        subObject: TypePrototypeDescriptor,
        superObject: TypePrototypeDescriptor,
        constructorName: String,
        limits: TypeCopyLimits = TypeCopyLimits(),
    ): GMResult<Unit, TypeError> =
        TypeMethodMaps.copyPrototypeMethods(
            subObject = subObject,
            superObject = superObject,
            constructorName = constructorName,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> copyMethodMap.
    fun copyMethodMap(
        objectClass: TypePrototypeDescriptor,
        extension: Map<String, Any?> = emptyMap(),
        limits: TypeCopyLimits = TypeCopyLimits(),
    ): GMResult<Unit, TypeError> =
        TypeMethodMaps.copyMethodMap(
            objectClass = objectClass,
            extension = extension,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> extendInstanceMethodMap.
    fun extendInstanceMethodMap(
        instance: TypeMethodMapInstance,
        extension: Map<String, Any?>,
        limits: TypeCopyLimits = TypeCopyLimits(),
    ): GMResult<Unit, TypeError> =
        TypeMethodMaps.extendInstanceMethodMap(
            instance = instance,
            extension = extension,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> extendInstanceMethodMap.
    fun extendInstanceMethodMap(
        instance: TypeMethodMapInstance,
        extension: String,
        extensionValue: Any?,
    ): GMResult<Unit, TypeError> =
        TypeMethodMaps.extendInstanceMethodMap(
            instance = instance,
            extension = extension,
            extensionValue = extensionValue,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> getCloneObject.
    fun getCloneObject(
        element: TypeCloneSource,
        limits: TypeCopyLimits = TypeCopyLimits(),
    ): GMResult<TypeCloneObject, TypeError> =
        TypeTraceClone.getCloneObject(
            element = element,
            limits = limits,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> clearVisPropOld.
    fun <T : TypeVisualPropertyCacheOwner> clearVisPropOld(
        element: T,
    ): T =
        TypeTraceClone.clearVisPropOld(element)

    // JSXGraph 1.13.3: src/utils/type.js -> isInObject.
    fun isInObject(
        value: Map<String, Any?>,
        target: Any?,
    ): Boolean =
        value.values.any { candidate ->
            jsStrictEquals(candidate, target)
        }

    // JSXGraph 1.13.3: src/utils/type.js -> concat.
    fun <T> concat(
        destination: MutableList<T>,
        source: List<T>,
    ): MutableList<T> {
        val sourceLength = source.size
        for (index in 0 until sourceLength) {
            destination.add(source[index])
        }
        return destination
    }

    // JSXGraph 1.13.3: src/utils/type.js -> _decimalAdjust.
    fun decimalAdjust(
        type: DecimalAdjustment,
        value: Double,
        exponent: Int = 0,
    ): Double =
        when (type) {
            DecimalAdjustment.ROUND ->
                JsNumberFormat.roundDecimal(value, -exponent)
            DecimalAdjustment.FLOOR ->
                JsNumberFormat.floorDecimal(value, -exponent)
            DecimalAdjustment.CEIL ->
                JsNumberFormat.ceilDecimal(value, -exponent)
        }

    // JSXGraph 1.13.3: src/utils/type.js -> _round10.
    fun round10(
        value: Double,
        exponent: Int = 0,
    ): Double = decimalAdjust(DecimalAdjustment.ROUND, value, exponent)

    // JSXGraph 1.13.3: src/utils/type.js -> _floor10.
    fun floor10(
        value: Double,
        exponent: Int = 0,
    ): Double = decimalAdjust(DecimalAdjustment.FLOOR, value, exponent)

    // JSXGraph 1.13.3: src/utils/type.js -> _ceil10.
    fun ceil10(
        value: Double,
        exponent: Int = 0,
    ): Double = decimalAdjust(DecimalAdjustment.CEIL, value, exponent)

    // JSXGraph 1.13.3: src/utils/type.js -> toFixed.
    fun toFixed(
        value: Double,
        digits: Int,
    ): GMResult<String, TypeError> {
        if (digits !in MIN_FIXED_DIGITS..MAX_FIXED_DIGITS) {
            return GMResult.Err(TypeError.InvalidDigits(digits))
        }
        return GMResult.Ok(toFixedUnchecked(value, digits))
    }

    // JSXGraph 1.13.3: src/utils/type.js -> trunc.
    fun trunc(
        value: Double,
        decimalPlaces: Int = 0,
    ): GMResult<String, TypeError> = toFixed(value, decimalPlaces)

    // JSXGraph 1.13.3: src/utils/type.js -> autoDigits.
    fun autoDigits(value: Double): AutoDigitsValue =
        when (abs(value)) {
            in 0.1..Double.POSITIVE_INFINITY ->
                AutoDigitsValue.Formatted(toFixedUnchecked(value, 2))
            in 0.01..<0.1 ->
                AutoDigitsValue.Formatted(toFixedUnchecked(value, 4))
            in 0.0001..<0.01 ->
                AutoDigitsValue.Formatted(toFixedUnchecked(value, 6))
            else -> AutoDigitsValue.Raw(value)
        }

    // JSXGraph 1.13.3: src/utils/type.js -> parseNumber.
    fun parseNumber(
        value: Any?,
        percentOfWhat: Double,
    ): Double = parseNumberValue(value, percentOfWhat).value

    // JSXGraph 1.13.3: src/utils/type.js -> parseNumber numeric convertPx.
    fun parseNumber(
        value: Any?,
        percentOfWhat: Double,
        convertPx: Double,
    ): Double {
        val parsed = parseNumberValue(value, percentOfWhat)
        return if (parsed.isPixelValue) {
            parsed.value * convertPx
        } else {
            parsed.value
        }
    }

    // JSXGraph 1.13.3: src/utils/type.js -> parseNumber function convertPx.
    fun parseNumber(
        value: Any?,
        percentOfWhat: Double,
        convertPx: (Double) -> Double,
    ): GMResult<Double, TypeError> {
        val parsed = parseNumberValue(value, percentOfWhat)
        if (!parsed.isPixelValue) {
            return GMResult.Ok(parsed.value)
        }
        return try {
            GMResult.Ok(convertPx(parsed.value))
        } catch (exception: Exception) {
            GMResult.Err(
                TypeError.PixelConversionFailed(
                    exception.message ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }
    }

    // JSXGraph 1.13.3: src/utils/type.js -> parsePosition.
    fun parsePosition(value: String): ParsedPosition {
        var side = ""
        var pos = ""
        val normalized = trimJsWhitespace(value)
        if (normalized.isNotEmpty()) {
            for (part in normalized.split(POSITION_SEPARATOR)) {
                if (part == "left" || part == "right") {
                    side = part
                } else {
                    pos = part
                }
            }
        }
        return ParsedPosition(side = side, pos = pos)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> escapeHTML.
    fun escapeHTML(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    // JSXGraph 1.13.3: src/utils/type.js -> unescapeHTML.
    fun unescapeHTML(value: String): String =
        value
            .replace(HTML_TAG, "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

    // JSXGraph 1.13.3: src/utils/type.js -> sanitizeHTML fallback branch.
    fun sanitizeHTML(value: String): String =
        value
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    // JSXGraph 1.13.3: src/utils/type.js -> capitalize.
    fun capitalize(value: String): String =
        value.take(1).uppercase() + value.drop(1).lowercase()

    // JSXGraph 1.13.3: src/utils/type.js -> trimNumber.
    fun trimNumber(value: String): String {
        var result = value.dropWhile { character -> character == '0' }
        result = result.dropLastWhile { character -> character == '0' }

        if (result.lastOrNull() == '.' || result.lastOrNull() == ',') {
            result = result.dropLast(1)
        }
        if (result.firstOrNull() == '.' || result.firstOrNull() == ',') {
            result = "0$result"
        }
        return result
    }

    // JSXGraph 1.13.3: src/utils/type.js -> trim.
    fun trim(value: String): String = trimJsWhitespace(value)

    // JSXGraph 1.13.3: src/utils/type.js -> toFraction.
    fun toFraction(
        value: Double,
        useTeX: Boolean = false,
        order: Double = 0.001,
    ): String {
        val fraction = Mat.decToFraction(value, order)
        if (fraction[1] == 0.0 && fraction[2] == 0.0) {
            return "0"
        }

        return buildString {
            if (fraction[0] < 0.0) {
                append('-')
            }
            if (fraction[2] == 0.0) {
                append(JsNumberFormat.compact(fraction[1]))
            } else if (!(fraction[2] == 1.0 && fraction[3] == 1.0)) {
                if (fraction[1] != 0.0) {
                    append(JsNumberFormat.compact(fraction[1]))
                    append(' ')
                }
                if (useTeX) {
                    append("\\frac{")
                    append(JsNumberFormat.compact(fraction[2]))
                    append("}{")
                    append(JsNumberFormat.compact(fraction[3]))
                    append('}')
                } else {
                    append(JsNumberFormat.compact(fraction[2]))
                    append('/')
                    append(JsNumberFormat.compact(fraction[3]))
                }
            }
        }
    }

    // JSXGraph 1.13.3: src/utils/type.js -> stack2jsxgraph.
    fun stack2jsxgraph(value: String): StackExpression {
        val normalized = trimJsWhitespace(
            value
                .replace("%pi", "PI")
                .replace("%e", "EULER")
                .replace("%phi", "1.618033988749895")
                .replace("%gamma", "0.5772156649015329"),
        )
        return if (
            normalized.startsWith('[') &&
            normalized.endsWith(']')
        ) {
            StackExpression.ArrayValue(
                splitStackArray(
                    normalized.substring(1, normalized.lastIndex),
                ),
            )
        } else {
            StackExpression.Scalar(normalized)
        }
    }

    private fun pointProvisionAttributes(
        attributes: Map<String, Any?>?,
        options: Map<String, Any?>,
        attrClass: String,
        attrArray: List<String>,
        parentIndex: Int,
    ): GMResult<Map<String, Any?>, TypeError> {
        val path = mutableListOf(attrClass)
        if (attrArray.isNotEmpty()) {
            path += attrArray[minOf(parentIndex, attrArray.lastIndex)]
                .lowercase()
        }
        return copyAttributes(
            attributes = attributes,
            options = options,
            path = path,
        )
    }

    private fun createProvidedPoint(
        board: Board,
        parent: Any?,
        attributes: Map<String, Any?>,
        parentIndex: Int,
    ): GMResult<Point, TypeError> {
        val id = attributes["id"] as? String ?: ""
        val name = attributes["name"] as? String
        val fixed = attributes["fixed"] as? Boolean ?: false
        val needsRegularUpdate =
            attributes["needsregularupdate"] as? Boolean ?: true
        val coordinateValues = arrayValues(parent)
        val result = when {
            coordinateValues != null &&
                coordinateValues.size > 1 &&
                coordinateValues.all { it is Number } ->
                Point.create(
                    board = board,
                    coordinates = coordinateValues
                        .map { (it as Number).toDouble() }
                        .toDoubleArray(),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                    fixed = fixed,
                )
            coordinateValues != null &&
                coordinateValues.size > 1 &&
                coordinateValues.all {
                    it is Number || it is String
                } ->
                Point.create(
                    board = board,
                    coordinateExpressions = coordinateValues.map { value ->
                        if (value is Number) {
                            jsNumberToString(value.toDouble())
                        } else {
                            value as String
                        }
                    },
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                    fixed = fixed,
                )
            coordinateValues != null &&
                coordinateValues.size > 1 &&
                coordinateValues.all {
                    it is Number || it is Function0<*>
                } ->
                Point.createConstrained(
                    board = board,
                    coordinateFunctions =
                        coordinateValues.map { coordinate ->
                            if (coordinate is Number) {
                                JessieCodeNumericCoordinateFunction(
                                    coordinate.toDouble(),
                                )
                            } else {
                                val callback = coordinate as Function0<*>
                                ProvidedPointCoordinateFunction(
                                    parentIndex = parentIndex,
                                    callback = { callback.invoke() },
                                    returnsCoordinateArray = false,
                                )
                            }
                        },
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                    fixed = fixed,
                )
            parent is Function0<*> -> {
                when (
                    val evaluated = evaluateProvidedPointArray(
                        callback = parent,
                        dimension = 2,
                        parentIndex = parentIndex,
                    )
                ) {
                    is GMResult.Err -> return evaluated
                    is GMResult.Ok -> Unit
                }
                Point.createConstrained(
                    board = board,
                    coordinateFunctions = listOf(
                        ProvidedPointCoordinateFunction(
                            parentIndex = parentIndex,
                            callback = { parent.invoke() },
                            returnsCoordinateArray = true,
                        ),
                    ),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                    fixed = fixed,
                )
            }
            else -> return GMResult.Err(
                TypeError.UnsupportedPointProvisionParent(
                    dimension = 2,
                    parentIndex = parentIndex,
                    value = parent,
                ),
            )
        }
        return when (result) {
            is GMResult.Ok -> GMResult.Ok(result.value)
            is GMResult.Err -> GMResult.Err(
                TypeError.PointProvisionFactoryFailed(
                    parentIndex = parentIndex,
                    error = result.error,
                ),
            )
        }
    }

    private fun createProvidedPoint3D(
        view: View3D,
        parent: Any?,
        attributes: Map<String, Any?>,
        parentIndex: Int,
    ): GMResult<Point3D, TypeError> {
        val id = attributes["id"] as? String ?: ""
        val name = attributes["name"] as? String
        val fixed = attributes["fixed"] as? Boolean ?: false
        val needsRegularUpdate =
            attributes["needsregularupdate"] as? Boolean ?: true
        val coordinateValues = arrayValues(parent)
        val source = when {
            coordinateValues != null &&
                coordinateValues.all { it is Number } ->
                Point3DCoordinateSource.Values(
                    coordinateValues.map { coordinate ->
                        Point3DCoordinateValue.Numeric(
                            (coordinate as Number).toDouble(),
                        )
                    },
                )
            coordinateValues != null &&
                coordinateValues.all {
                    it is Number || it is Function0<*>
                } ->
                Point3DCoordinateSource.Values(
                    coordinateValues.map { coordinate ->
                        if (coordinate is Number) {
                            Point3DCoordinateValue.Numeric(
                                coordinate.toDouble(),
                            )
                        } else {
                            val callback = coordinate as Function0<*>
                            Point3DCoordinateValue.Dynamic {
                                evaluateProvidedPoint3DScalar(callback)
                            }
                        }
                    },
                )
            parent is Function0<*> -> {
                when (
                    val evaluated = evaluateProvidedPointArray(
                        callback = parent,
                        dimension = 3,
                        parentIndex = parentIndex,
                    )
                ) {
                    is GMResult.Err -> return evaluated
                    is GMResult.Ok -> Unit
                }
                Point3DCoordinateSource.Function(
                    Point3DArrayEvaluator {
                        evaluateProvidedPoint3DArray(parent)
                    },
                )
            }
            else -> return GMResult.Err(
                TypeError.UnsupportedPointProvisionParent(
                    dimension = 3,
                    parentIndex = parentIndex,
                    value = parent,
                ),
            )
        }
        return when (
            val result = Point3D.create(
                view = view,
                coordinateSource = source,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(result.value)
            is GMResult.Err -> GMResult.Err(
                TypeError.Point3DProvisionFactoryFailed(
                    parentIndex = parentIndex,
                    error = result.error,
                ),
            )
        }
    }

    private fun evaluateProvidedPointArray(
        callback: Function0<*>,
        dimension: Int,
        parentIndex: Int,
    ): GMResult<List<Any?>, TypeError> {
        val value = try {
            callback.invoke()
        } catch (exception: Exception) {
            return GMResult.Err(
                TypeError.PointProvisionEvaluationFailed(
                    dimension = dimension,
                    parentIndex = parentIndex,
                    message = exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }
        val coordinates = arrayValues(value)
            ?: return GMResult.Err(
                TypeError.UnsupportedPointProvisionParent(
                    dimension = dimension,
                    parentIndex = parentIndex,
                    value = value,
                ),
            )
        if (coordinates.size <= 1) {
            return GMResult.Err(
                TypeError.UnsupportedPointProvisionParent(
                    dimension = dimension,
                    parentIndex = parentIndex,
                    value = value,
                ),
            )
        }
        return GMResult.Ok(coordinates)
    }

    private fun evaluateProvidedPoint3DScalar(
        callback: Function0<*>,
    ): GMResult<Double, Point3DDynamicError> =
        try {
            val value = callback.invoke()
            if (value is Number) {
                GMResult.Ok(value.toDouble())
            } else {
                GMResult.Err(
                    Point3DDynamicError.Rejected(
                        "Expected numeric coordinate",
                    ),
                )
            }
        } catch (exception: Exception) {
            GMResult.Err(
                Point3DDynamicError.Rejected(
                    exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }

    private fun evaluateProvidedPoint3DArray(
        callback: Function0<*>,
    ): GMResult<DoubleArray, Point3DDynamicError> =
        try {
            val values = arrayValues(callback.invoke())
                ?: return GMResult.Err(
                    Point3DDynamicError.Rejected(
                        "Expected coordinate array",
                    ),
                )
            val coordinates = DoubleArray(values.size)
            for ((index, value) in values.withIndex()) {
                val number = value as? Number
                    ?: return GMResult.Err(
                        Point3DDynamicError.Rejected(
                            "Coordinate $index is not numeric",
                        ),
                    )
                coordinates[index] = number.toDouble()
            }
            GMResult.Ok(coordinates)
        } catch (exception: Exception) {
            GMResult.Err(
                Point3DDynamicError.Rejected(
                    exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }

    private fun cmpArrayValues(
        first: Any?,
        second: Any?,
    ): Boolean {
        if (first === second) {
            return true
        }

        val firstValues = arrayValues(first) ?: return false
        val secondValues = arrayValues(second) ?: return false
        if (firstValues.size != secondValues.size) {
            return false
        }

        for (index in firstValues.indices) {
            val firstValue = firstValues[index]
            val secondValue = secondValues[index]
            if (
                isArrayValue(firstValue) &&
                isArrayValue(secondValue)
            ) {
                if (!cmpArrayValues(firstValue, secondValue)) {
                    return false
                }
            } else if (!jsStrictEquals(firstValue, secondValue)) {
                return false
            }
        }
        return true
    }

    private fun jsStrictEquals(
        first: Any?,
        second: Any?,
    ): Boolean =
        when {
            first === second -> true
            first is Number && second is Number ->
                first.toDouble() == second.toDouble()
            first is String && second is String -> first == second
            first is Boolean && second is Boolean -> first == second
            else -> false
        }

    private fun isArrayValue(value: Any?): Boolean =
        arrayValues(value) != null

    private fun arrayValues(value: Any?): List<Any?>? =
        when (value) {
            is List<*> -> value.map { item -> item }
            is Array<*> -> value.map { item -> item }
            is BooleanArray -> value.map { item -> item }
            is ByteArray -> value.map { item -> item }
            is ShortArray -> value.map { item -> item }
            is IntArray -> value.map { item -> item }
            is LongArray -> value.map { item -> item }
            is FloatArray -> value.map { item -> item }
            is DoubleArray -> value.map { item -> item }
            else -> null
        }

    private fun parseNumberValue(
        value: Any?,
        percentOfWhat: Double,
    ): ParsedNumber {
        if (value is String && '%' in value) {
            val normalized = removeFirstSpacedUnit(value, "%")
            return ParsedNumber(
                value = jsParseFloat(normalized) * percentOfWhat * 0.01,
                isPixelValue = false,
            )
        }
        if (value is String && "fr" in value) {
            val normalized = removeFirstSpacedUnit(value, "fr")
            return ParsedNumber(
                value = jsParseFloat(normalized) * percentOfWhat,
                isPixelValue = false,
            )
        }
        if (value is String && "px" in value) {
            val normalized = removeFirstSpacedUnit(value, "px")
            return ParsedNumber(
                value = jsParseFloat(normalized),
                isPixelValue = true,
            )
        }
        return ParsedNumber(
            value = jsParseFloat(value),
            isPixelValue = false,
        )
    }

    private fun jsParseFloat(value: Any?): Double {
        if (value is Number) {
            return value.toDouble()
        }
        if (value !is String) {
            return Double.NaN
        }

        val normalized = trimJsWhitespace(value)
        val prefix = FLOAT_PREFIX.find(normalized)?.value
            ?: return Double.NaN
        return when (prefix) {
            "Infinity",
            "+Infinity",
            -> Double.POSITIVE_INFINITY
            "-Infinity" -> Double.NEGATIVE_INFINITY
            else -> prefix.toDoubleOrNull() ?: Double.NaN
        }
    }

    private fun toFixedUnchecked(
        value: Double,
        digits: Int,
    ): String =
        JsNumberFormat.fixed(
            value = JsNumberFormat.roundDecimal(value, digits),
            digits = digits,
        )

    private fun jsNumberToString(value: Double): String {
        if (!value.isFinite() || value == 0.0) {
            return JsNumberFormat.compact(value)
        }

        val negative = value < 0.0
        val absoluteValue = abs(value)
        val raw = absoluteValue.toString()
        val exponentSeparator = raw.indexOfAny(charArrayOf('e', 'E'))
        if (exponentSeparator < 0) {
            return (if (negative) "-" else "") +
                raw.removeSuffix(".0")
        }

        val mantissa = raw.substring(0, exponentSeparator)
        val exponent = raw.substring(exponentSeparator + 1).toInt()
        val decimalIndex = mantissa.indexOf('.')
        val integerDigits = if (decimalIndex < 0) {
            mantissa.length
        } else {
            decimalIndex
        }
        val digits = mantissa
            .replace(".", "")
            .trimEnd('0')
            .ifEmpty { "0" }
        val decimalPosition = integerDigits + exponent
        val magnitudeExponent = decimalPosition - 1
        val unsigned = if (
            absoluteValue >= 1.0e21 ||
            absoluteValue < 1.0e-6
        ) {
            buildString {
                append(digits[0])
                if (digits.length > 1) {
                    append('.')
                    append(digits.substring(1))
                }
                append('e')
                if (magnitudeExponent >= 0) {
                    append('+')
                }
                append(magnitudeExponent)
            }
        } else {
            when {
                decimalPosition <= 0 ->
                    "0." + "0".repeat(-decimalPosition) + digits
                decimalPosition >= digits.length ->
                    digits + "0".repeat(decimalPosition - digits.length)
                else ->
                    digits.substring(0, decimalPosition) +
                        "." +
                        digits.substring(decimalPosition)
            }
        }
        return (if (negative) "-" else "") + unsigned
    }

    internal fun formatJsNumber(value: Double): String =
        jsNumberToString(value)

    private fun String.toJsArrayIndexOrNull(): Long? {
        if (isEmpty() || (length > 1 && first() == '0')) {
            return null
        }
        if (any { character -> character !in '0'..'9' }) {
            return null
        }
        val value = toLongOrNull() ?: return null
        return value.takeIf { index -> index in 0 until MAX_JS_ARRAY_INDEX }
    }

    private fun removeFirstSpacedUnit(
        value: String,
        unit: String,
    ): String {
        var index = 0
        while (index < value.length) {
            if (!value[index].isEcmaScriptWhitespace()) {
                index += 1
                continue
            }

            val start = index
            while (
                index < value.length &&
                value[index].isEcmaScriptWhitespace()
            ) {
                index += 1
            }
            if (!value.startsWith(unit, startIndex = index)) {
                continue
            }
            index += unit.length
            if (
                index >= value.length ||
                !value[index].isEcmaScriptWhitespace()
            ) {
                continue
            }
            while (
                index < value.length &&
                value[index].isEcmaScriptWhitespace()
            ) {
                index += 1
            }
            return value.removeRange(start, index)
        }
        return value
    }

    private fun trimJsWhitespace(value: String): String {
        var start = 0
        while (
            start < value.length &&
            value[start].isEcmaScriptWhitespace()
        ) {
            start += 1
        }

        var end = value.length
        while (
            end > start &&
            value[end - 1].isEcmaScriptWhitespace()
        ) {
            end -= 1
        }
        return value.substring(start, end)
    }

    private fun splitStackArray(value: String): List<String> {
        val result = mutableListOf<String>()
        var segmentStart = 0
        for (index in value.indices) {
            if (value[index] != ',') {
                continue
            }

            var delimiterStart = index
            while (
                delimiterStart > segmentStart &&
                value[delimiterStart - 1].isEcmaScriptWhitespace()
            ) {
                delimiterStart -= 1
            }
            result += value.substring(segmentStart, delimiterStart)

            segmentStart = index + 1
            while (
                segmentStart < value.length &&
                value[segmentStart].isEcmaScriptWhitespace()
            ) {
                segmentStart += 1
            }
        }
        result += value.substring(segmentStart)
        return result
    }

    private fun Char.isEcmaScriptWhitespace(): Boolean =
        this in '\u0009'..'\u000D' ||
            this == '\u0020' ||
            this == '\u00A0' ||
            this == '\u1680' ||
            this in '\u2000'..'\u200A' ||
            this == '\u2028' ||
            this == '\u2029' ||
            this == '\u202F' ||
            this == '\u205F' ||
            this == '\u3000' ||
            this == '\uFEFF'

    private val POSITION_SEPARATOR = Regex("[ ,]+")
    private val HTML_TAG = Regex("</?[^>]+>", RegexOption.IGNORE_CASE)
    private val FLOAT_PREFIX =
        Regex(
            "^[+-]?(?:Infinity|" +
                "(?:(?:\\d+\\.?\\d*|\\.\\d+)" +
                "(?:[eE][+-]?\\d+)?))",
        )
    private val JS_NUMBER_COMPARATOR = Comparator<Double> { first, second ->
        val difference = first - second
        when {
            difference.isNaN() || difference == 0.0 -> 0
            difference < 0.0 -> -1
            else -> 1
        }
    }
    private val JS_PROPERTY_KEY_ORDER = Comparator<String> { first, second ->
        val firstIndex = first.toJsArrayIndexOrNull()
        val secondIndex = second.toJsArrayIndexOrNull()
        when {
            firstIndex != null && secondIndex != null ->
                firstIndex.compareTo(secondIndex)
            firstIndex != null -> -1
            secondIndex != null -> 1
            else -> 0
        }
    }

    private data class ParsedNumber(
        val value: Double,
        val isPixelValue: Boolean,
    )

    private const val MIN_FIXED_DIGITS = 0
    private const val MAX_FIXED_DIGITS = 100
    private const val MAX_JS_ARRAY_INDEX = 4_294_967_295L
    private const val PROTOTYPE_PROPERTY = "__proto__"
    private const val DOCUMENT_NODE = 9
    private const val DOCUMENT_FRAGMENT_NODE = 11
}
