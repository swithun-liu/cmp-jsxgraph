/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/slopetriangle.js -> createSlopeTriangle
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.utils.JsNumberFormat

internal sealed interface SlopeTriangleError {
    data class UnsupportedParents(
        val parentTypes: List<String>,
    ) : SlopeTriangleError

    data class MissingTangentPoint(
        val tangentId: String,
    ) : SlopeTriangleError

    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : SlopeTriangleError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : SlopeTriangleError

    data class TangentFactory(
        val error: TangentError,
    ) : SlopeTriangleError

    data class PointFactory(
        val role: String,
        val error: PointError,
    ) : SlopeTriangleError

    data class LineFactory(
        val role: String,
        val error: LineError,
    ) : SlopeTriangleError

    data class GliderFactory(
        val error: GliderError,
    ) : SlopeTriangleError

    data class PolygonFactory(
        val error: PolygonError,
    ) : SlopeTriangleError

    data class TextFactory(
        val error: TextError,
    ) : SlopeTriangleError
}

internal data class SlopeTriangleElementAttributes(
    val id: String = "",
    val name: String? = null,
    val needsRegularUpdate: Boolean = true,
    val fixed: Boolean = false,
)

internal data class SlopeTriangleAttributes(
    val id: String = "",
    val name: String? = null,
    val needsRegularUpdate: Boolean = true,
    val basePoint: SlopeTriangleElementAttributes =
        SlopeTriangleElementAttributes(name = ""),
    val baseLine: SlopeTriangleElementAttributes =
        SlopeTriangleElementAttributes(name = ""),
    val glider: SlopeTriangleElementAttributes =
        SlopeTriangleElementAttributes(fixed = true),
    val topPoint: SlopeTriangleElementAttributes =
        SlopeTriangleElementAttributes(name = ""),
    val tangent: SlopeTriangleElementAttributes =
        SlopeTriangleElementAttributes(name = ""),
    val label: SlopeTriangleElementAttributes =
        SlopeTriangleElementAttributes(),
    val digits: Int = 2,
    val showPrefix: Boolean = true,
    val showSuffix: Boolean = true,
    val prefix: String = "",
    val suffix: String = "",
)

/**
 * State dynamically attached to the Polygon returned by createSlopeTriangle.
 *
 * JSXGraph extends the Polygon instance directly. Kotlin keeps that ownership
 * explicit while preserving the same helper graph and public method values.
 */
internal class SlopeTriangleDefinition(
    internal val polygon: Polygon,
    internal val tangent: Line,
    internal val tangentPoint: Point,
    internal val glider: Glider,
    internal val basePoint: Point,
    internal val baseLine: Line,
    internal val topPoint: Point,
    internal val label: Text,
    internal val borderHorizontal: Line,
    internal val borderVertical: Line,
    internal val borderParallel: Line,
    internal val isPrivateTangent: Boolean,
    private val digits: Int,
    private val showPrefix: Boolean,
    private val showSuffix: Boolean,
    private val prefix: String,
    private val suffix: String,
) {
    internal var labelUpdateError: TextError? = null
        private set

    // JSXGraph: src/element/slopetriangle.js -> priv.Slope.
    @Suppress("FunctionName")
    internal fun Slope(): Double = tangent.Slope()

    // JSXGraph: src/element/slopetriangle.js -> priv.getAngle.
    internal fun getAngle(): Double = tangent.getAngle()

    internal fun getAngle(unit: String): GMResult<Double, LineError> =
        tangent.getAngle(unit)

    // JSXGraph: src/element/slopetriangle.js -> priv.DeltaX.
    @Suppress("FunctionName")
    internal fun DeltaX(): Double = borderHorizontal.Direction()[0]

    // JSXGraph: src/element/slopetriangle.js -> priv.DeltaY.
    @Suppress("FunctionName")
    internal fun DeltaY(): Double = borderVertical.Direction()[1]

    // JSXGraph: src/element/slopetriangle.js -> priv.Direction.
    @Suppress("FunctionName")
    internal fun Direction(): DoubleArray = tangent.Direction()

    // JSXGraph: src/element/slopetriangle.js -> label.setText callback;
    // src/base/line.js -> getLabelAnchor, default midpoint and offset.
    internal fun updateLabel() {
        label.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(
                (glider.X() + topPoint.X()) * 0.5,
                (glider.Y() + topPoint.Y()) * 0.5,
            ),
        )
        val value = JsNumberFormat.fixed(Slope(), digits)
        val text =
            (if (showPrefix) prefix else "") +
                value +
                (if (showSuffix) suffix else "")
        when (val result = label.setText(text)) {
            is GMResult.Ok -> labelUpdateError = null
            is GMResult.Err -> labelUpdateError = result.error
        }
    }

    // JSXGraph: src/element/slopetriangle.js -> removeSlopeTriangle.
    internal fun remove() {
        board.removeObjects(
            buildList {
                add(topPoint)
                add(glider)
                add(baseLine)
                add(basePoint)
                add(label)
                if (isPrivateTangent) {
                    add(tangent)
                }
            },
        )
    }

    private val board: Board
        get() = polygon.board
}

internal object SlopeTriangle {
    private const val ELEMENT_TYPE = "slopetriangle"

    // JSXGraph: src/element/slopetriangle.js -> createSlopeTriangle.
    internal fun create(
        board: Board,
        parents: List<GeometryElement>,
        attributes: SlopeTriangleAttributes = SlopeTriangleAttributes(),
    ): GMResult<Polygon, SlopeTriangleError> {
        val resolved = when (
            val result = resolveParents(board, parents, attributes)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val created = mutableListOf<GeometryElement>()
        if (resolved.isPrivateTangent) {
            created += resolved.tangent
        }

        val basePoint = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    SlopeTriangleCoordinateFunction {
                        doubleArrayOf(
                            resolved.tangentPoint.X() + 1.0,
                            resolved.tangentPoint.Y(),
                        )
                    },
                ),
                id = attributes.basePoint.id,
                name = attributes.basePoint.name,
                needsRegularUpdate =
                    attributes.basePoint.needsRegularUpdate,
                fixed = attributes.basePoint.fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(
                    SlopeTriangleError.PointFactory(
                        role = "basepoint",
                        error = result.error,
                    ),
                )
            }
        }
        created += basePoint

        val baseLine = when (
            val result = Line.create(
                board = board,
                point1 = resolved.tangentPoint,
                point2 = basePoint,
                id = attributes.baseLine.id,
                name = attributes.baseLine.name,
                needsRegularUpdate =
                    attributes.baseLine.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(
                    SlopeTriangleError.LineFactory(
                        role = "baseline",
                        error = result.error,
                    ),
                )
            }
        }
        created += baseLine

        val glider = when (
            val result = Glider.create(
                board = board,
                coordinates = doubleArrayOf(
                    resolved.tangentPoint.X() + 1.0,
                    resolved.tangentPoint.Y(),
                ),
                slideObject = baseLine,
                id = attributes.glider.id,
                name = attributes.glider.name,
                needsRegularUpdate =
                    attributes.glider.needsRegularUpdate,
                fixed = attributes.glider.fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(
                    SlopeTriangleError.GliderFactory(result.error),
                )
            }
        }
        created += glider

        val topPoint = when (
            val result = Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    SlopeTriangleCoordinateFunction {
                        doubleArrayOf(
                            glider.X(),
                            glider.Y() +
                                (
                                    glider.X() -
                                        resolved.tangentPoint.X()
                                    ) * resolved.tangent.Slope(),
                        )
                    },
                ),
                id = attributes.topPoint.id,
                name = attributes.topPoint.name,
                needsRegularUpdate =
                    attributes.topPoint.needsRegularUpdate,
                fixed = attributes.topPoint.fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(
                    SlopeTriangleError.PointFactory(
                        role = "toppoint",
                        error = result.error,
                    ),
                )
            }
        }
        created += topPoint

        val polygon = when (
            val result = Polygon.create(
                board = board,
                vertices = listOf(
                    resolved.tangentPoint,
                    glider,
                    topPoint,
                ),
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(
                    SlopeTriangleError.PolygonFactory(result.error),
                )
            }
        }
        created += polygon
        polygon.elType = ELEMENT_TYPE

        val borderHorizontal = polygon.borders[0]
        val borderVertical = polygon.borders[1]
        val borderParallel = polygon.borders[2]
        polygon.borders.forEach { border ->
            border.dump = false
        }

        val label = when (
            val result = Text.create(
                board = board,
                coordinates = doubleArrayOf(
                    (glider.X() + topPoint.X()) * 0.5,
                    (glider.Y() + topPoint.Y()) * 0.5,
                ),
                content = "",
                id = "${borderVertical.id}Label",
                name = attributes.label.name,
                needsRegularUpdate =
                    attributes.label.needsRegularUpdate,
                parse = false,
                digits = attributes.digits,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                rollback(board, created)
                return GMResult.Err(
                    SlopeTriangleError.TextFactory(result.error),
                )
            }
        }
        label.elType = "label"
        label.dump = false
        label.screenOffset = doubleArrayOf(10.0, 0.0)
        label.addParents(listOf(borderVertical))
        borderVertical.addChild(label)

        val definition = SlopeTriangleDefinition(
            polygon = polygon,
            tangent = resolved.tangent,
            tangentPoint = resolved.tangentPoint,
            glider = glider,
            basePoint = basePoint,
            baseLine = baseLine,
            topPoint = topPoint,
            label = label,
            borderHorizontal = borderHorizontal,
            borderVertical = borderVertical,
            borderParallel = borderParallel,
            isPrivateTangent = resolved.isPrivateTangent,
            digits = attributes.digits,
            showPrefix = attributes.showPrefix,
            showSuffix = attributes.showSuffix,
            prefix = attributes.prefix,
            suffix = attributes.suffix,
        )
        polygon.slopeTriangleDefinition = definition
        polygon.subs["glider"] = glider
        polygon.subs["basePoint"] = basePoint
        polygon.subs["baseLine"] = baseLine
        polygon.subs["topPoint"] = topPoint
        polygon.subs["label"] = label
        polygon.inherits += listOf(
            glider,
            basePoint,
            baseLine,
            topPoint,
            label,
        )
        definition.updateLabel()
        return GMResult.Ok(polygon)
    }

    private fun resolveParents(
        board: Board,
        parents: List<GeometryElement>,
        attributes: SlopeTriangleAttributes,
    ): GMResult<ResolvedParents, SlopeTriangleError> {
        for ((index, parent) in parents.withIndex()) {
            validateParent(board, parent, index)?.let {
                return GMResult.Err(it)
            }
        }

        if (parents.size == 1) {
            val parent = parents[0]
            if (parent is Line && parent.type == Const.OBJECT_TYPE_TANGENT) {
                val tangentPoint = parent.glider
                    ?: return GMResult.Err(
                        SlopeTriangleError.MissingTangentPoint(parent.id),
                    )
                return GMResult.Ok(
                    ResolvedParents(
                        tangent = parent,
                        tangentPoint = tangentPoint,
                        isPrivateTangent = false,
                    ),
                )
            }
            if (parent is Glider) {
                val tangent = when (
                    val result = Tangent.create(
                        board = board,
                        firstParent = parent.line,
                        secondParent = parent,
                        id = attributes.tangent.id,
                        name = attributes.tangent.name,
                        needsRegularUpdate =
                            attributes.tangent.needsRegularUpdate,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        SlopeTriangleError.TangentFactory(result.error),
                    )
                }
                tangent.setParents(listOf(parent))
                return GMResult.Ok(
                    ResolvedParents(
                        tangent = tangent,
                        tangentPoint = parent,
                        isPrivateTangent = true,
                    ),
                )
            }
        }
        if (
            parents.size == 2 &&
            parents[0] is Line &&
            parents[1] is Point
        ) {
            return GMResult.Ok(
                ResolvedParents(
                    tangent = parents[0] as Line,
                    tangentPoint = parents[1] as Point,
                    isPrivateTangent = false,
                ),
            )
        }
        return GMResult.Err(
            SlopeTriangleError.UnsupportedParents(
                parents.map { parent ->
                    parent.elType.ifEmpty { "element" }
                },
            ),
        )
    }

    private fun validateParent(
        board: Board,
        parent: GeometryElement,
        parentIndex: Int,
    ): SlopeTriangleError? =
        when {
            parent.board !== board ->
                SlopeTriangleError.ParentBoardMismatch(parentIndex)
            board.elementById(parent.id) !== parent ->
                SlopeTriangleError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = parent.id,
                )
            else -> null
        }

    private fun rollback(
        board: Board,
        created: List<GeometryElement>,
    ) {
        board.removeObjects(created.asReversed())
    }

    private data class ResolvedParents(
        val tangent: Line,
        val tangentPoint: Point,
        val isPrivateTangent: Boolean,
    )
}

// Direct JavaScript coordinate functions intentionally have no JessieCode
// dependency entries in JSXGraph.
private class SlopeTriangleCoordinateFunction(
    private val coordinates: () -> DoubleArray,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()
    override val returnsCoordinateArray: Boolean = true

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                coordinates().map(
                    JessieCodeRuntimeValue::NumberValue,
                ),
            ),
        )
}
