/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/jsxgraph.js -> initBoard defaultAxes;
 * src/base/board.js -> setAttribute("axis" / "defaultaxes")
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal data class BoardDefaultAxes(
    val x: Line,
    val y: Line,
)

internal sealed interface BoardDefaultAxesError {
    data class PointFactory(
        val axis: String,
        val pointIndex: Int,
        val cause: PointError,
    ) : BoardDefaultAxesError

    data class AxisFactory(
        val axis: String,
        val cause: AxisError,
    ) : BoardDefaultAxesError
}

internal fun createDefaultAxes(
    board: Board,
    axisAttributes: Map<String, Any?> = emptyMap(),
    defaultAxesAttributes: Map<String, Any?> = emptyMap(),
): GMResult<BoardDefaultAxes, BoardDefaultAxesError> {
    val created = mutableListOf<GeometryElement>()

    fun cleanup() {
        for (element in created.asReversed()) {
            board.removeObject(element)
        }
    }

    fun point(
        axis: String,
        index: Int,
        x: Double,
        y: Double,
    ): GMResult<Point, BoardDefaultAxesError> =
        when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                name = "",
            )
        ) {
            is GMResult.Ok -> {
                result.value.dump = false
                created += result.value
                GMResult.Ok(result.value)
            }
            is GMResult.Err -> GMResult.Err(
                BoardDefaultAxesError.PointFactory(
                    axis = axis,
                    pointIndex = index,
                    cause = result.error,
                ),
            )
        }

    fun axis(
        role: String,
        secondCoordinates: DoubleArray,
    ): GMResult<Line, BoardDefaultAxesError> {
        val mergedAttributes = deepMergeAttributes(
            defaultAxisAttributes(role),
            axisAttributes,
            mapValue(defaultAxesAttributes[role]),
        )
        val first = when (
            val result = point(
                axis = role,
                index = 0,
                x = 0.0,
                y = 0.0,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = when (
            val result = point(
                axis = role,
                index = 1,
                x = secondCoordinates[0],
                y = secondCoordinates[1],
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Axis.create(
                board = board,
                point1 = first,
                point2 = second,
                ticksAttributes = defaultTicksAttributes(role),
                id = stringAttribute(mergedAttributes, "id").orEmpty(),
                name = stringAttribute(mergedAttributes, "name") ?: role,
                needsRegularUpdate =
                    booleanAttribute(
                        mergedAttributes,
                        "needsregularupdate",
                    ) ?: false,
                straightFirst =
                    booleanAttribute(
                        mergedAttributes,
                        "straightfirst",
                    ) ?: true,
                straightLast =
                    booleanAttribute(
                        mergedAttributes,
                        "straightlast",
                    ) ?: true,
            )
        ) {
            is GMResult.Ok -> {
                result.value.dump = false
                result.value.setAttribute(mergedAttributes)
                applyPointAttributes(
                    point = first,
                    value = mergedAttributes["point1"],
                )
                applyPointAttributes(
                    point = second,
                    value = mergedAttributes["point2"],
                )
                created += result.value
                GMResult.Ok(result.value)
            }
            is GMResult.Err -> GMResult.Err(
                BoardDefaultAxesError.AxisFactory(
                    axis = role,
                    cause = result.error,
                ),
            )
        }
    }

    val x = when (
        val result = axis(
            role = "x",
            secondCoordinates = doubleArrayOf(1.0, 0.0),
        )
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> {
            cleanup()
            return result
        }
    }
    val y = when (
        val result = axis(
            role = "y",
            secondCoordinates = doubleArrayOf(0.0, 1.0),
        )
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> {
            cleanup()
            return result
        }
    }
    return GMResult.Ok(BoardDefaultAxes(x = x, y = y))
}

// JSXGraph 1.13.3: src/options.js -> Options.board.defaultAxes.
private fun defaultAxisAttributes(role: String): Map<String, Any?> =
    linkedMapOf(
        "name" to role,
        "fixed" to true,
        "visible" to true,
        "needsregularupdate" to false,
        "ticks" to linkedMapOf(
            "label" to linkedMapOf(
                "visible" to "inherit",
                "anchorx" to if (role == "x") "middle" else "right",
                "anchory" to if (role == "x") "top" else "middle",
                "fontsize" to 12.0,
                "offset" to
                    if (role == "x") {
                        doubleArrayOf(0.0, -3.0)
                    } else {
                        doubleArrayOf(-6.0, 0.0)
                    },
            ),
            "tickendings" to
                if (role == "x") {
                    doubleArrayOf(0.0, 1.0)
                } else {
                    doubleArrayOf(1.0, 0.0)
                },
            "majortickendings" to doubleArrayOf(1.0, 1.0),
            "drawzero" to false,
            "visible" to "inherit",
        ),
    )

private fun defaultTicksAttributes(role: String): TicksAttributes =
    defaultAxisTicksAttributes().copy(
        tickEndings =
            if (role == "x") {
                doubleArrayOf(0.0, 1.0)
            } else {
                doubleArrayOf(1.0, 0.0)
            },
        labelOffset =
            if (role == "x") {
                doubleArrayOf(0.0, -3.0)
            } else {
                doubleArrayOf(-6.0, 0.0)
            },
        labelAnchorX = if (role == "x") "middle" else "right",
        labelAnchorY = if (role == "x") "top" else "middle",
    )

private fun applyPointAttributes(
    point: Point,
    value: Any?,
) {
    val attributes = mapValue(value)
    if (attributes.isNotEmpty()) {
        point.setAttribute(attributes)
    }
}

private fun booleanAttribute(
    attributes: Map<String, Any?>,
    name: String,
): Boolean? =
    normalizedAttributes(attributes)[name] as? Boolean

private fun stringAttribute(
    attributes: Map<String, Any?>,
    name: String,
): String? =
    normalizedAttributes(attributes)[name] as? String

private fun normalizedAttributes(
    attributes: Map<String, Any?>,
): Map<String, Any?> =
    attributes.entries.associateTo(linkedMapOf()) { (key, value) ->
        key.filterNot(Char::isWhitespace).lowercase() to value
    }

private fun deepMergeAttributes(
    vararg sources: Map<String, Any?>,
): Map<String, Any?> {
    val result = linkedMapOf<String, Any?>()
    for (source in sources) {
        mergeAttributes(result, source)
    }
    return result
}

private fun mergeAttributes(
    target: MutableMap<String, Any?>,
    source: Map<String, Any?>,
) {
    for ((rawKey, rawValue) in source) {
        val key = rawKey.filterNot(Char::isWhitespace).lowercase()
        val nested = mapValue(rawValue)
        val existing = target[key] as? Map<*, *>
        target[key] =
            if (rawValue is Map<*, *> && existing != null) {
                val merged = mapValue(existing).toMutableMap()
                mergeAttributes(merged, nested)
                merged
            } else {
                copyDefaultAxisAttribute(rawValue)
            }
    }
}

private fun copyDefaultAxisAttribute(value: Any?): Any? =
    when (value) {
        is DoubleArray -> value.copyOf()
        is IntArray -> value.copyOf()
        is LongArray -> value.copyOf()
        is FloatArray -> value.copyOf()
        is BooleanArray -> value.copyOf()
        is Array<*> -> value.copyOf()
        is List<*> -> value.map(::copyDefaultAxisAttribute)
        is Map<*, *> -> mapValue(value).mapValues {
            copyDefaultAxisAttribute(it.value)
        }
        else -> value
    }

private fun mapValue(value: Any?): Map<String, Any?> {
    val source = value as? Map<*, *> ?: return emptyMap()
    val result = linkedMapOf<String, Any?>()
    for ((key, nestedValue) in source) {
        val name = key as? String ?: continue
        result[name] = nestedValue
    }
    return result
}
