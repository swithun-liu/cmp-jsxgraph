/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/text3d.js -> Text3D / createText3D
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat

internal sealed interface Text3DError {
    data class InvalidCoordinateCount(
        val count: Int,
    ) : Text3DError

    data class CoordinateEvaluation(
        val coordinateIndex: Int?,
        val error: Point3DDynamicError,
    ) : Text3DError

    data class Registration(
        val error: BoardError,
    ) : Text3DError

    data class ProxyTextFactory(
        val error: TextError,
    ) : Text3DError
}

internal class Text3D private constructor(
    view: View3D,
    private val coordinateSource: Point3DCoordinateSource,
    internal val content: String,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_TEXT3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal var coords: DoubleArray = DoubleArray(4)
        private set
    internal lateinit var text2D: Text
        private set
    internal var coordinateEvaluationError: Text3DError? = null
        private set

    init {
        elType = TEXT_3D_ELEMENT_TYPE
    }

    internal fun X(): Double = coords[1]

    internal fun Y(): Double = coords[2]

    internal fun Z(): Double = coords[3]

    // JSXGraph: src/3d/text3d.js -> initCoords / updateCoords.
    private fun evaluateCoordinates(): GMResult<DoubleArray, Text3DError> =
        when (val source = coordinateSource) {
            is Point3DCoordinateSource.Function -> when (
                val result = source.evaluator.evaluate()
            ) {
                is GMResult.Err -> GMResult.Err(
                    Text3DError.CoordinateEvaluation(
                        coordinateIndex = null,
                        error = result.error,
                    ),
                )
                is GMResult.Ok -> {
                    if (result.value.size != 3) {
                        GMResult.Err(
                            Text3DError.InvalidCoordinateCount(
                                result.value.size,
                            ),
                        )
                    } else {
                        GMResult.Ok(
                            doubleArrayOf(1.0) + result.value,
                        )
                    }
                }
            }
            is Point3DCoordinateSource.Values -> {
                if (source.values.size != 3) {
                    return GMResult.Err(
                        Text3DError.InvalidCoordinateCount(
                            source.values.size,
                        ),
                    )
                }
                val result = DoubleArray(4)
                result[0] = 1.0
                for ((index, value) in source.values.withIndex()) {
                    result[index + 1] = when (value) {
                        is Point3DCoordinateValue.Numeric -> value.value
                        is Point3DCoordinateValue.Dynamic -> when (
                            val evaluated = value.evaluator.evaluate()
                        ) {
                            is GMResult.Ok -> evaluated.value
                            is GMResult.Err -> return GMResult.Err(
                                Text3DError.CoordinateEvaluation(
                                    coordinateIndex = index,
                                    error = evaluated.error,
                                ),
                            )
                        }
                    }
                }
                GMResult.Ok(result)
            }
        }

    override fun update(fromParent: Boolean): GeometryElement {
        when (val result = evaluateCoordinates()) {
            is GMResult.Ok -> {
                coords = result.value
                coordinateEvaluationError = null
                text2D.setPositionDirectly(
                    method = Const.COORDS_BY_USER,
                    coordinates = view.project3DTo2D(coords),
                )
                zIndex = Mat.innerProduct(
                    view.matrix3DRotShift[3],
                    coords,
                )
                text2D.prepareUpdate().update()
            }
            is GMResult.Err -> {
                coordinateEvaluationError = result.error
                coords = DoubleArray(4) { Double.NaN }
            }
        }
        return this
    }

    override fun updateRenderer(): GeometryElement {
        needsUpdate = false
        return this
    }

    internal companion object {
        private const val TEXT_3D_ID_PREFIX = "text3d"
        private const val TEXT_3D_ELEMENT_TYPE = "text3d"

        // JSXGraph: src/3d/text3d.js -> createText3D.
        internal fun create(
            view: View3D,
            coordinateSource: Point3DCoordinateSource,
            content: String,
            dependencies: Iterable<GeometryElement> = emptyList(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Text3D, Text3DError> {
            val text = Text3D(
                view = view,
                coordinateSource = coordinateSource,
                content = content,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            when (val result = text.evaluateCoordinates()) {
                is GMResult.Ok -> text.coords = result.value
                is GMResult.Err -> return result
            }
            when (
                val registration =
                    view.board.setId(text, TEXT_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Text3DError.Registration(registration.error),
                )
            }
            text.registerInView()
            val proxy = when (
                val result = Text.create(
                    board = view.board,
                    coordinates = view.project3DTo2D(text.coords),
                    content = content,
                    name = "",
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(text)
                    return GMResult.Err(
                        Text3DError.ProxyTextFactory(result.error),
                    )
                }
            }
            proxy.dump = false
            proxy.setParents(listOf(text))
            text.text2D = proxy
            text.element2D = proxy
            text.addChild(proxy)
            for (dependency in dependencies.distinct()) {
                dependency.addChild(text)
            }
            text.prepareUpdate().update()
            return GMResult.Ok(text)
        }
    }
}
