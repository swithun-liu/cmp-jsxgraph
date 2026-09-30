/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/type.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

internal fun interface TypeOwnerCallable<O> {
    fun call(
        owner: O,
        arguments: List<Any?>,
    ): Any?
}

internal class TypeBoundCallable<O>(
    private val function: TypeOwnerCallable<O>,
    private val owner: O,
) {
    operator fun invoke(
        vararg arguments: Any?,
    ): GMResult<Any?, TypeError> =
        try {
            GMResult.Ok(function.call(owner, arguments.toList()))
        } catch (exception: Exception) {
            GMResult.Err(
                TypeError.BoundFunctionFailed(
                    exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }
}

internal fun interface TypeFilterPredicate<T> {
    fun matches(value: T): Boolean
}

internal sealed interface TypeFilterCriterion {
    data class Exact(
        val value: Any?,
    ) : TypeFilterCriterion

    class Predicate(
        val matches: (Any?) -> Boolean,
    ) : TypeFilterCriterion
}

internal interface TypeElementPropertyAccessor<T> {
    fun readProperty(
        element: T,
        property: String,
    ): GMResult<Any?, TypeError>

    fun readVisualProperty(
        element: T,
        property: String,
    ): GMResult<Any?, TypeError>
}

internal object TypeMapElementPropertyAccessor :
    TypeElementPropertyAccessor<Map<String, Any?>> {
    override fun readProperty(
        element: Map<String, Any?>,
        property: String,
    ): GMResult<Any?, TypeError> =
        evaluateProperty(
            value = if (property in element) {
                element[property]
            } else {
                DumpUndefined
            },
            property = property,
            visual = false,
        )

    override fun readVisualProperty(
        element: Map<String, Any?>,
        property: String,
    ): GMResult<Any?, TypeError> {
        val visualProperties = element["visProp"] as? Map<*, *>
            ?: return GMResult.Ok(DumpUndefined)
        val entry = visualProperties.entries.firstOrNull { candidate ->
            candidate.key == property
        } ?: return GMResult.Ok(DumpUndefined)
        return evaluateProperty(
            value = entry.value,
            property = property,
            visual = true,
        )
    }

    private fun evaluateProperty(
        value: Any?,
        property: String,
        visual: Boolean,
    ): GMResult<Any?, TypeError> {
        if (value !is Function<*>) {
            return GMResult.Ok(value)
        }
        if (value !is Function0<*>) {
            return GMResult.Err(
                TypeError.UnsupportedFilterPropertyFunction(
                    property = property,
                    visual = visual,
                    value = value,
                ),
            )
        }
        return try {
            GMResult.Ok(value())
        } catch (exception: Exception) {
            GMResult.Err(
                TypeError.FilterPropertyEvaluationFailed(
                    property = property,
                    visual = visual,
                    message = exception.message
                        ?: exception::class.simpleName.orEmpty(),
                ),
            )
        }
    }
}

/**
 * Explicit callback and property-access adapters for dynamic `JXG.Type`
 * utilities.
 */
internal object TypeFunctional {
    // JSXGraph 1.13.3: src/utils/type.js -> bind.
    fun <O> bind(
        function: TypeOwnerCallable<O>,
        owner: O,
    ): TypeBoundCallable<O> =
        TypeBoundCallable(
            function = function,
            owner = owner,
        )

    // JSXGraph 1.13.3: src/utils/type.js -> filterElements function branch.
    fun <T> filterElements(
        values: List<T>,
        predicate: TypeFilterPredicate<T>,
    ): GMResult<List<T>, TypeError> {
        val result = mutableListOf<T>()
        for ((index, value) in values.withIndex()) {
            val matches = try {
                predicate.matches(value)
            } catch (exception: Exception) {
                return GMResult.Err(
                    TypeError.FilterPredicateFailed(
                        index = index,
                        property = null,
                        message = exception.message
                            ?: exception::class.simpleName.orEmpty(),
                    ),
                )
            }
            if (matches) {
                result += value
            }
        }
        return GMResult.Ok(result)
    }

    // JSXGraph 1.13.3: src/utils/type.js -> filterElements object branch.
    fun <T> filterElements(
        values: List<T>,
        filter: Map<String, TypeFilterCriterion>,
        accessor: TypeElementPropertyAccessor<T>,
    ): GMResult<List<T>, TypeError> {
        val result = mutableListOf<T>()
        val properties = Type.keys(filter)
        for ((index, item) in values.withIndex()) {
            var pass = true
            for (property in properties) {
                val direct = when (
                    val read = accessor.readProperty(item, property)
                ) {
                    is GMResult.Ok -> read.value
                    is GMResult.Err -> return read
                }
                val visual = when (
                    val read = accessor.readVisualProperty(
                        item,
                        property.lowercase(),
                    )
                ) {
                    is GMResult.Ok -> read.value
                    is GMResult.Err -> return read
                }
                pass = when (val criterion = filter.getValue(property)) {
                    is TypeFilterCriterion.Exact ->
                        strictEquals(direct, criterion.value) ||
                            strictEquals(visual, criterion.value)
                    is TypeFilterCriterion.Predicate ->
                        try {
                            criterion.matches(direct) ||
                                criterion.matches(visual)
                        } catch (exception: Exception) {
                            return GMResult.Err(
                                TypeError.FilterPredicateFailed(
                                    index = index,
                                    property = property,
                                    message = exception.message
                                        ?: exception::class.simpleName.orEmpty(),
                                ),
                            )
                        }
                }
                if (!pass) {
                    break
                }
            }
            if (pass) {
                result += item
            }
        }
        return GMResult.Ok(result)
    }

    private fun strictEquals(
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
}
