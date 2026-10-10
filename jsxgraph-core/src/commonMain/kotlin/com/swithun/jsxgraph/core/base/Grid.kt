/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/grid.js -> JXG.createGrid
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Andreas Walter, Alfred Wassermann, and contributors.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphGrid2D
import com.swithun.jsxgraph.core.JsxGraphGridDrawZero
import com.swithun.jsxgraph.core.JsxGraphGridFace
import com.swithun.jsxgraph.core.JsxGraphGridForceSquare
import com.swithun.jsxgraph.core.JsxGraphGridLength
import com.swithun.jsxgraph.core.JsxGraphGridMinorElements
import com.swithun.jsxgraph.core.JsxGraphGridPair
import com.swithun.jsxgraph.core.JsxGraphGridResolveError
import com.swithun.jsxgraph.core.JsxGraphGridRole

internal sealed interface GridError {
    data class InvalidParentCount(val count: Int) : GridError

    data class InvalidParent(
        val index: Int,
        val id: String,
        val type: Int,
    ) : GridError

    data class ParentBoardMismatch(
        val index: Int,
        val id: String,
    ) : GridError

    data class ParentNotRegistered(
        val index: Int,
        val id: String,
    ) : GridError

    data class MajorCurveFactory(val error: CurveError) : GridError

    data class MinorCurveFactory(val error: CurveError) : GridError

    data class Geometry(
        val role: JsxGraphGridRole,
        val error: JsxGraphGridResolveError,
    ) : GridError
}

internal object Grid {
    // JSXGraph 1.13.3: src/element/grid.js -> JXG.createGrid.
    fun create(
        board: Board,
        parentAxes: List<Line>,
        definition: JsxGraphGrid2D,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = false,
        minorId: String? = null,
        minorName: String? = null,
    ): GMResult<Curve, GridError> {
        if (parentAxes.size > 2) {
            return GMResult.Err(
                GridError.InvalidParentCount(parentAxes.size),
            )
        }
        val resolvedParentAxes = parentAxes.toMutableList()
        board.defaultAxes?.let { defaultAxes ->
            if (resolvedParentAxes.isEmpty()) {
                resolvedParentAxes += defaultAxes.x
            }
            if (resolvedParentAxes.size == 1) {
                resolvedParentAxes += defaultAxes.y
            }
        }
        resolvedParentAxes.forEachIndexed { index, axis ->
            when {
                axis.type != Const.OBJECT_TYPE_AXIS ->
                    return GMResult.Err(
                        GridError.InvalidParent(
                            index = index,
                            id = axis.id,
                            type = axis.type,
                        ),
                    )
                axis.board !== board ->
                    return GMResult.Err(
                        GridError.ParentBoardMismatch(index, axis.id),
                    )
                board.elementById(axis.id) !== axis ->
                    return GMResult.Err(
                        GridError.ParentNotRegistered(index, axis.id),
                    )
            }
        }

        val resolvedDefinition = definition.copy(
            parentMajorStep = JsxGraphGridPair(
                definition.parentMajorStep.x
                    ?: resolvedParentAxes.getOrNull(0)
                        ?.defaultTicks?.attributes?.ticksDistance,
                definition.parentMajorStep.y
                    ?: resolvedParentAxes.getOrNull(1)
                        ?.defaultTicks?.attributes?.ticksDistance,
            ),
            parentMinorElements = JsxGraphGridPair(
                definition.parentMinorElements.x
                    ?: resolvedParentAxes.getOrNull(0)
                        ?.defaultTicks?.attributes?.minorTicks?.toDouble(),
                definition.parentMinorElements.y
                    ?: resolvedParentAxes.getOrNull(1)
                        ?.defaultTicks?.attributes?.minorTicks?.toDouble(),
            ),
        )
        val majorDefinition =
            resolvedDefinition.copy(role = JsxGraphGridRole.Major)
        val major = when (
            val result = Curve.createData(
                board = board,
                dataX = doubleArrayOf(),
                dataY = doubleArrayOf(),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err ->
                return GMResult.Err(GridError.MajorCurveFactory(result.error))
        }
        major.configureGrid(
            definition = majorDefinition,
            bezierDegree = majorDefinition.bezierDegree(),
        )
        when (val data = majorDefinition.data(board)) {
            is GMResult.Ok -> major.replaceData(data.value)
            is GMResult.Err -> {
                board.removeObject(major)
                return GMResult.Err(
                    GridError.Geometry(
                        role = JsxGraphGridRole.Major,
                        error = data.error,
                    ),
                )
            }
        }

        val resolvedMinorId = minorId
            ?.takeUnless { it == id }
            ?: "${major.id}_minor"
        val resolvedMinorName = minorName
            ?.takeUnless { it == name }
            ?: "${major.name}_minor"
        val minorDefinition =
            resolvedDefinition.copy(role = JsxGraphGridRole.Minor)
        val minor = when (
            val result = Curve.createData(
                board = board,
                dataX = doubleArrayOf(),
                dataY = doubleArrayOf(),
                id = resolvedMinorId,
                name = resolvedMinorName,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(major)
                return GMResult.Err(
                    GridError.MinorCurveFactory(result.error),
                )
            }
        }
        minor.configureGrid(
            definition = minorDefinition,
            bezierDegree = minorDefinition.bezierDegree(),
        )
        when (val data = minorDefinition.data(board)) {
            is GMResult.Ok -> minor.replaceData(data.value)
            is GMResult.Err -> {
                board.removeObject(minor)
                board.removeObject(major)
                return GMResult.Err(
                    GridError.Geometry(
                        role = JsxGraphGridRole.Minor,
                        error = data.error,
                    ),
                )
            }
        }
        minor.dump = false

        major.minorGrid = minor
        minor.majorGrid = major
        major.inherits += minor
        major.setParents(resolvedParentAxes)
        minor.setParents(resolvedParentAxes)
        board.grids += major
        board.grids += minor
        return GMResult.Ok(major)
    }

    // JSXGraph 1.13.3: src/element/grid.js ->
    // majorGrid.updateDataArray / minorGrid.updateDataArray.
    internal fun updateData(
        curve: Curve,
    ): GMResult<CurveDataUpdate, JsxGraphGridResolveError> {
        val current = curve.gridDefinition
            ?: return GMResult.Ok(
                CurveDataUpdate(
                    x = doubleArrayOf(),
                    y = doubleArrayOf(),
                ),
            )
        val candidate =
            when (current.role) {
                JsxGraphGridRole.Major ->
                    liveMajorDefinition(curve, current)
                JsxGraphGridRole.Minor ->
                    liveMinorDefinition(curve, current)
            }
        return when (val result = candidate.data(curve.board)) {
            is GMResult.Ok -> {
                curve.updateGridDefinition(
                    definition = candidate,
                    bezierDegree = candidate.bezierDegree(),
                )
                result
            }
            is GMResult.Err -> result
        }
    }

    private fun liveMajorDefinition(
        curve: Curve,
        current: JsxGraphGrid2D,
    ): JsxGraphGrid2D {
        val majorStepValue = curve.evalVisProp("majorstep")
        var majorStep =
            if (majorStepValue == null) {
                current.majorStep
            } else {
                gridLengthPair(
                    curve = curve,
                    value = majorStepValue,
                    allowAuto = true,
                    numericIsPixels = false,
                ) ?: return current
            }
        val gridX = curve.evalVisProp("gridx")
        if (gridX != null) {
            val parsed = gridLength(
                curve = curve,
                value = gridX,
                allowAuto = false,
                numericIsPixels = false,
            ) ?: return current
            majorStep = majorStep.copy(x = parsed)
        }
        val gridY = curve.evalVisProp("gridy")
        if (gridY != null) {
            val parsed = gridLength(
                curve = curve,
                value = gridY,
                allowAuto = false,
                numericIsPixels = false,
            ) ?: return current
            majorStep = majorStep.copy(y = parsed)
        }
        val forceSquareValue = curve.evalVisProp("forcesquare")
        val forceSquare =
            if (forceSquareValue == null) {
                current.forceSquare
            } else {
                gridForceSquare(curve, forceSquareValue) ?: return current
            }
        val includeBoundariesValue =
            curve.evalVisProp("includeboundaries")
        val includeBoundaries =
            if (includeBoundariesValue == null) {
                current.includeBoundaries
            } else {
                curve.eval(includeBoundariesValue) as? Boolean
                    ?: return current
            }
        val major = liveFace(curve, current.major) ?: return current
        val parentValues = liveParentValues(curve, current)
        return current.copy(
            majorStep = majorStep,
            forceSquare = forceSquare,
            includeBoundaries = includeBoundaries,
            major = major,
            parentMajorStep = parentValues.first,
            parentMinorElements = parentValues.second,
        )
    }

    private fun liveMinorDefinition(
        curve: Curve,
        current: JsxGraphGrid2D,
    ): JsxGraphGrid2D {
        val majorDefinition = curve.majorGrid?.gridDefinition ?: current
        val minorElementsValue = curve.evalVisProp("minorelements")
        val minorElements =
            if (minorElementsValue == null) {
                current.minorElements
            } else {
                gridMinorElementsPair(curve, minorElementsValue)
                    ?: return current
            }
        val includeBoundariesValue =
            curve.evalVisProp("includeboundaries")
        val includeBoundaries =
            if (includeBoundariesValue == null) {
                current.includeBoundaries
            } else {
                curve.eval(includeBoundariesValue) as? Boolean
                    ?: return current
            }
        val minor = liveFace(curve, current.minor) ?: return current
        val parentValues = liveParentValues(curve, current)
        return current.copy(
            majorStep = majorDefinition.majorStep,
            minorElements = minorElements,
            forceSquare = majorDefinition.forceSquare,
            includeBoundaries = includeBoundaries,
            major = majorDefinition.major,
            minor = minor,
            parentMajorStep = parentValues.first,
            parentMinorElements = parentValues.second,
        )
    }

    private fun liveParentValues(
        curve: Curve,
        current: JsxGraphGrid2D,
    ): Pair<JsxGraphGridPair<Double?>, JsxGraphGridPair<Double?>> {
        val axes = curve.parentIds().mapNotNull { id ->
            curve.board.elementById(id) as? Line
        }
        val major = JsxGraphGridPair(
            x = axes.getOrNull(0)?.defaultTicks
                ?.attributes?.ticksDistance
                ?: current.parentMajorStep.x,
            y = axes.getOrNull(1)?.defaultTicks
                ?.attributes?.ticksDistance
                ?: current.parentMajorStep.y,
        )
        val minor = JsxGraphGridPair(
            x = axes.getOrNull(0)?.defaultTicks
                ?.attributes?.minorTicks?.toDouble()
                ?: current.parentMinorElements.x,
            y = axes.getOrNull(1)?.defaultTicks
                ?.attributes?.minorTicks?.toDouble()
                ?: current.parentMinorElements.y,
        )
        return major to minor
    }

    private fun liveFace(
        curve: Curve,
        current: JsxGraphGridFace,
    ): JsxGraphGridFace? {
        val faceValue = curve.evalVisProp("face")
        val face =
            if (faceValue == null) {
                current.face
            } else {
                (curve.eval(faceValue) as? String)
                    ?.lowercase()
                    ?.takeIf { it in GRID_FACES }
                    ?: return null
            }
        val sizeValue = curve.evalVisProp("size")
        val size =
            if (sizeValue == null) {
                current.size
            } else {
                gridLengthPair(
                    curve = curve,
                    value = sizeValue,
                    allowAuto = false,
                    numericIsPixels = true,
                ) ?: return null
            }
        val marginValue = curve.evalVisProp("margin")
        val margin =
            if (marginValue == null) {
                current.margin
            } else {
                finiteDouble(curve, marginValue) ?: return null
            }
        val drawZeroValue = curve.evalVisProp("drawzero")
        val drawZero =
            if (drawZeroValue == null) {
                current.drawZero
            } else {
                gridDrawZero(curve, drawZeroValue) ?: return null
            }
        val polygonVerticesValue =
            curve.evalVisProp("polygonvertices")
        val polygonVertices =
            if (polygonVerticesValue == null) {
                current.polygonVertices
            } else {
                finiteDouble(curve, polygonVerticesValue)
                    ?.takeIf {
                        it >= 1.0 &&
                            it <= 4096.0 &&
                            it.toInt().toDouble() == it
                    }
                    ?.toInt()
                    ?: return null
            }
        return current.copy(
            face = face,
            size = size,
            margin = margin,
            drawZero = drawZero,
            polygonVertices = polygonVertices,
        )
    }

    private fun gridLengthPair(
        curve: Curve,
        value: Any?,
        allowAuto: Boolean,
        numericIsPixels: Boolean,
    ): JsxGraphGridPair<JsxGraphGridLength>? {
        val values = pairValues(value) ?: return null
        val x = gridLength(
            curve = curve,
            value = values.first,
            allowAuto = allowAuto,
            numericIsPixels = numericIsPixels,
        ) ?: return null
        val y = gridLength(
            curve = curve,
            value = values.second,
            allowAuto = allowAuto,
            numericIsPixels = numericIsPixels,
        ) ?: return null
        return JsxGraphGridPair(x, y)
    }

    private fun gridLength(
        curve: Curve,
        value: Any?,
        allowAuto: Boolean,
        numericIsPixels: Boolean,
    ): JsxGraphGridLength? {
        val evaluated = curve.eval(value)
        val parsed = when (evaluated) {
            is Number -> evaluated.toDouble()
                .takeIf(Double::isFinite)
                ?.let {
                    if (numericIsPixels) {
                        JsxGraphGridLength.Pixels(it)
                    } else {
                        JsxGraphGridLength.User(it)
                    }
                }
            is String -> {
                val normalized = evaluated.trim().lowercase()
                if (allowAuto && normalized == "auto") {
                    JsxGraphGridLength.Auto
                } else {
                    val match = GRID_LENGTH_PATTERN.matchEntire(normalized)
                        ?: return null
                    val number = match.groupValues[1].toDoubleOrNull()
                        ?.takeIf(Double::isFinite)
                        ?: return null
                    when (match.groupValues[2]) {
                        "%" -> JsxGraphGridLength.Percent(number)
                        "fr" -> JsxGraphGridLength.Fraction(number)
                        "px" -> JsxGraphGridLength.Pixels(number)
                        else ->
                            if (numericIsPixels) {
                                JsxGraphGridLength.Pixels(number)
                            } else {
                                JsxGraphGridLength.User(number)
                            }
                    }
                }
            }
            else -> null
        }
        return parsed?.takeIf {
            it == JsxGraphGridLength.Auto || it.numericValue() > 0.0
        }
    }

    private fun gridMinorElementsPair(
        curve: Curve,
        value: Any?,
    ): JsxGraphGridPair<JsxGraphGridMinorElements>? {
        val values = pairValues(value) ?: return null
        val x = gridMinorElements(curve, values.first) ?: return null
        val y = gridMinorElements(curve, values.second) ?: return null
        return JsxGraphGridPair(x, y)
    }

    private fun gridMinorElements(
        curve: Curve,
        value: Any?,
    ): JsxGraphGridMinorElements? =
        when (val evaluated = curve.eval(value)) {
            is Number -> evaluated.toDouble()
                .takeIf { it.isFinite() && it >= 0.0 }
                ?.let(JsxGraphGridMinorElements::Fixed)
            is String ->
                if (evaluated.lowercase() == "auto") {
                    JsxGraphGridMinorElements.Auto
                } else {
                    evaluated.toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it >= 0.0 }
                        ?.let(JsxGraphGridMinorElements::Fixed)
                }
            else -> null
        }

    private fun gridForceSquare(
        curve: Curve,
        value: Any?,
    ): JsxGraphGridForceSquare? =
        when (val evaluated = curve.eval(value)) {
            is Boolean ->
                if (evaluated) {
                    JsxGraphGridForceSquare.Min
                } else {
                    JsxGraphGridForceSquare.None
                }
            is String -> when (evaluated.lowercase()) {
                "min" -> JsxGraphGridForceSquare.Min
                "max" -> JsxGraphGridForceSquare.Max
                else -> null
            }
            else -> null
        }

    private fun gridDrawZero(
        curve: Curve,
        value: Any?,
    ): JsxGraphGridDrawZero? {
        when (val evaluated = curve.eval(value)) {
            is Boolean ->
                return JsxGraphGridDrawZero(
                    origin = evaluated,
                    x = evaluated,
                    y = evaluated,
                )
            is Map<*, *> -> {
                val values = evaluated.entries.associate {
                    (key, child) -> key.toString().lowercase() to child
                }
                fun component(name: String): Boolean? =
                    values[name]?.let(curve::eval) as? Boolean ?: false
                return JsxGraphGridDrawZero(
                    origin = component("origin") ?: return null,
                    x = component("x") ?: return null,
                    y = component("y") ?: return null,
                )
            }
            else -> return null
        }
    }

    private fun pairValues(value: Any?): Pair<Any?, Any?>? =
        when (value) {
            is List<*> -> value.firstOrNull()?.let { first ->
                first to value.getOrElse(1) { first }
            }
            is DoubleArray -> value.firstOrNull()?.let { first ->
                first to value.getOrElse(1) { first }
            }
            is IntArray -> value.firstOrNull()?.let { first ->
                first to value.getOrElse(1) { first }
            }
            else -> value?.let { it to it }
        }

    private fun finiteDouble(
        curve: Curve,
        value: Any?,
    ): Double? =
        when (val evaluated = curve.eval(value)) {
            is Number -> evaluated.toDouble()
            is String -> evaluated.toDoubleOrNull()
            else -> null
        }?.takeIf(Double::isFinite)

    private fun JsxGraphGridLength.numericValue(): Double =
        when (this) {
            JsxGraphGridLength.Auto -> 1.0
            is JsxGraphGridLength.User -> value
            is JsxGraphGridLength.Percent -> value
            is JsxGraphGridLength.Fraction -> value
            is JsxGraphGridLength.Pixels -> value
        }

    private fun JsxGraphGrid2D.data(
        board: Board,
    ): GMResult<CurveDataUpdate, JsxGraphGridResolveError> {
        val boundingBox = board.getBoundingBox()
        return when (
            val resolved = resolve(
                visibleLeft = boundingBox[0],
                visibleTop = boundingBox[1],
                visibleRight = boundingBox[2],
                visibleBottom = boundingBox[3],
                cssPixelsPerUnitX = board.unitX,
                cssPixelsPerUnitY = board.unitY,
            )
        ) {
            is GMResult.Ok ->
                GMResult.Ok(
                    CurveDataUpdate(
                        x = resolved.value.points.map { point ->
                            point?.x ?: Double.NaN
                        }.toDoubleArray(),
                        y = resolved.value.points.map { point ->
                            point?.y ?: Double.NaN
                        }.toDoubleArray(),
                    ),
                )
            is GMResult.Err -> resolved
        }
    }

    private fun Curve.replaceData(data: CurveDataUpdate) {
        replaceData(data.x, data.y)
    }

    private fun JsxGraphGrid2D.bezierDegree(): Int {
        val face =
            if (role == JsxGraphGridRole.Major) major.face else minor.face
        return if (face.lowercase() in setOf("o", "circle")) 3 else 1
    }

    private val GRID_FACES = setOf(
        ".",
        "point",
        "o",
        "circle",
        "regpol",
        "regularpolygon",
        "[]",
        "square",
        "<>",
        "diamond",
        "<<>>",
        "diamond2",
        "x",
        "cross",
        "+",
        "plus",
        "-",
        "minus",
        "|",
        "divide",
        "^",
        "a",
        "triangleup",
        "v",
        "triangledown",
        "<",
        "triangleleft",
        ">",
        "triangleright",
        "line",
    )

    private val GRID_LENGTH_PATTERN =
        Regex(
            """^([+-]?(?:(?:\d+(?:\.\d*)?|\.\d+)""" +
                """(?:[eE][+-]?\d+)?))\s*(%|fr|px)?$""",
        )
}
