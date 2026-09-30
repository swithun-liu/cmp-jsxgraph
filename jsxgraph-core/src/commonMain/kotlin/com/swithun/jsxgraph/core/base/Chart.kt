/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/chart.js -> Chart, Legend
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal sealed interface ChartContainerError {
    data class Registration(
        val error: BoardError,
    ) : ChartContainerError
}

/**
 * Non-rendered Chart container. JSXGraph registers this object after all
 * style-specific children but returns the nested child array from its factory.
 */
internal class Chart private constructor(
    board: Board,
    internal val elements: List<GeometryElement>,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    elementClass = Const.OBJECT_CLASS_OTHER,
    needsRegularUpdate = needsRegularUpdate,
) {
    override fun update(fromParent: Boolean): Chart = this

    override fun updateRenderer(): Chart {
        needsUpdate = false
        return this
    }

    internal companion object {
        // JSXGraph 1.13.3: src/base/chart.js -> JXG.Chart constructor.
        internal fun create(
            board: Board,
            elements: List<GeometryElement>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Chart, ChartContainerError> {
            val chart = Chart(
                board = board,
                elements = elements,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (val result = board.setId(chart, "Chart")) {
                is GMResult.Ok -> GMResult.Ok(chart)
                is GMResult.Err -> GMResult.Err(
                    ChartContainerError.Registration(result.error),
                )
            }
        }
    }
}

/**
 * Registered Legend container whose visible output is its child line list.
 */
internal class Legend private constructor(
    board: Board,
    internal val lines: List<Line>,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    elementClass = Const.OBJECT_CLASS_OTHER,
    needsRegularUpdate = needsRegularUpdate,
) {
    override fun update(fromParent: Boolean): Legend = this

    override fun updateRenderer(): Legend {
        needsUpdate = false
        return this
    }

    internal companion object {
        // JSXGraph 1.13.3: src/base/chart.js -> JXG.Legend constructor.
        internal fun create(
            board: Board,
            lines: List<Line>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Legend, ChartContainerError> {
            val legend = Legend(
                board = board,
                lines = lines,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (val result = board.setId(legend, "Leg")) {
                is GMResult.Ok -> {
                    for (line in lines) {
                        legend.addChild(line)
                    }
                    GMResult.Ok(legend)
                }
                is GMResult.Err -> GMResult.Err(
                    ChartContainerError.Registration(result.error),
                )
            }
        }
    }
}
