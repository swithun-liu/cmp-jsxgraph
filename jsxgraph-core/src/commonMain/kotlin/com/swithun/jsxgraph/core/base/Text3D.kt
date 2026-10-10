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
import kotlin.math.abs

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

    data object SlideViewMismatch : Text3DError

    data class SlideNotRegistered(
        val id: String,
    ) : Text3DError

    data class UnsupportedSlide(
        val elementType: String,
    ) : Text3DError

    data class SlideProjection(
        val reason: String,
    ) : Text3DError
}

internal class Text3D private constructor(
    view: View3D,
    private val coordinateSource: Point3DCoordinateSource,
    internal val content: String,
    internal val slide: GeometryElement3D?,
    internal val isFixed: Boolean,
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
    internal val position = mutableListOf<Double>()
    private var projectedCoordinates: DoubleArray? = null

    init {
        elType = TEXT_3D_ELEMENT_TYPE
    }

    // JSXGraph 1.13.3: src/3d/text3d.js -> Text3D.X.
    internal fun X(): Double = coords[1]

    // JSXGraph 1.13.3: src/3d/text3d.js -> Text3D.Y.
    internal fun Y(): Double = coords[2]

    // JSXGraph 1.13.3: src/3d/text3d.js -> Text3D.Z.
    internal fun Z(): Double = coords[3]

    private fun evaluateCoordinates(
        dynamicOnly: Boolean,
    ): GMResult<DoubleArray, Text3DError> =
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
                val result =
                    if (dynamicOnly) coords.copyOf() else DoubleArray(4)
                if (!dynamicOnly) {
                    result[0] = 1.0
                }
                for ((index, value) in source.values.withIndex()) {
                    result[index + 1] = when (value) {
                        is Point3DCoordinateValue.Numeric ->
                            if (dynamicOnly) {
                                result[index + 1]
                            } else {
                                value.value
                            }
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

    // JSXGraph: src/3d/text3d.js -> initCoords.
    private fun initCoords(): GMResult<Text3D, Text3DError> =
        when (val result = evaluateCoordinates(dynamicOnly = false)) {
            is GMResult.Ok -> {
                coords = result.value
                coordinateEvaluationError = null
                GMResult.Ok(this)
            }
            is GMResult.Err -> {
                coordinateEvaluationError = result.error
                coords = DoubleArray(4) { Double.NaN }
                result
            }
        }

    // JSXGraph: src/3d/text3d.js -> updateCoords.
    internal fun updateCoords(): Text3D {
        when (val result = evaluateCoordinates(dynamicOnly = true)) {
            is GMResult.Ok -> {
                coords = result.value
                coordinateEvaluationError = null
            }
            is GMResult.Err -> {
                coordinateEvaluationError = result.error
                coords = DoubleArray(4) { Double.NaN }
            }
        }
        return this
    }

    // JSXGraph: src/3d/text3d.js -> normalizeCoords.
    internal fun normalizeCoords(): Text3D {
        if (abs(coords[0]) > Mat.eps) {
            coords[1] /= coords[0]
            coords[2] /= coords[0]
            coords[3] /= coords[0]
            coords[0] = 1.0
        }
        return this
    }

    // JSXGraph: src/3d/text3d.js -> setPosition.
    internal fun setPosition(
        coordinates: DoubleArray,
        noEvent: Boolean = false,
    ): GMResult<Text3D, Text3DError> {
        if (coordinates.size !in setOf(3, 4)) {
            return GMResult.Err(
                Text3DError.InvalidCoordinateCount(coordinates.size),
            )
        }
        val oldCoordinates = coords.copyOf()
        coords =
            if (coordinates.size == 3) {
                doubleArrayOf(
                    1.0,
                    coordinates[0],
                    coordinates[1],
                    coordinates[2],
                )
            } else {
                coordinates.copyOf()
            }
        normalizeCoords()
        if (!noEvent && !oldCoordinates.contentEquals(coords)) {
            update3DEvent(oldCoordinates)
        }
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/text3d.js -> update.
    override fun update(fromParent: Boolean): GeometryElement {
        val previousProjection = projectedCoordinates
        val currentProjection = text2D.coords.usrCoords
        val proxyMoved =
            text2D.isDraggable &&
                !isFixed &&
                previousProjection != null &&
                !previousProjection.contentEquals(currentProjection)
        if (proxyMoved) {
            val coordinates3D =
                if (view.isVerticalDrag()) {
                    view.project2DTo3DVertical(
                        coordinates2D = currentProjection,
                        baseCoordinates3D = coords,
                    )
                } else {
                    view.project2DTo3DPlane(
                        coordinates2D = currentProjection,
                        normal = doubleArrayOf(1.0, 0.0, 0.0, 1.0),
                        foot = doubleArrayOf(1.0, 0.0, 0.0, coords[3]),
                    )
                }
            if (coordinates3D[0] != 0.0) {
                val (bounded, corrected) =
                    view.project3DToCube(coordinates3D)
                coords = bounded
                if (corrected) {
                    updateProxyCoordinates()
                }
                when (val result = projectToSlide()) {
                    is GMResult.Ok -> {
                        coords = result.value
                        coordinateEvaluationError = null
                        if (slide != null) {
                            updateProxyCoordinates()
                        }
                    }
                    is GMResult.Err -> {
                        coordinateEvaluationError = result.error
                    }
                }
            }
        } else {
            updateCoords()
            if (coordinateEvaluationError == null) {
                when (val result = projectToSlide()) {
                    is GMResult.Ok -> {
                        coords = result.value
                        coordinateEvaluationError = null
                    }
                    is GMResult.Err -> {
                        coordinateEvaluationError = result.error
                    }
                }
            }
            if (coordinateEvaluationError == null) {
                updateProxyCoordinates()
                zIndex = Mat.innerProduct(
                    view.matrix3DRotShift[3],
                    coords,
                )
                text2D.prepareUpdate().update()
            }
        }
        projectedCoordinates = text2D.coords.usrCoords.copyOf()
        return this
    }

    // JSXGraph: src/3d/text3d.js -> updateRenderer.
    override fun updateRenderer(): Text3D {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/text3d.js -> testIfFinite.
    internal fun testIfFinite(): Boolean = abs(coords[0]) > Mat.eps

    // JSXGraph: src/3d/text3d.js -> __evt__update3D.
    internal fun update3DEvent(
        oldCoordinates: DoubleArray,
    ) {
        @Suppress("UNUSED_VARIABLE")
        val upstreamReservedArgument = oldCoordinates
    }

    private fun updateProxyCoordinates() {
        text2D.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = view.project3DTo2D(coords),
        )
    }

    private fun projectToSlide(): GMResult<DoubleArray, Text3DError> {
        val target = slide ?: return GMResult.Ok(coords)
        return when (target) {
            is Circle3D -> {
                when (
                    val result = target.projectCoords(coords, position)
                ) {
                    is GMResult.Ok -> result
                    is GMResult.Err -> slideProjectionError(result.error)
                }
            }
            is Curve3D -> {
                when (
                    val result = target.projectCoords(coords, position)
                ) {
                    is GMResult.Ok -> result
                    is GMResult.Err -> slideProjectionError(result.error)
                }
            }
            is Line3D -> {
                val parameters = positionArray(1)
                val projected = target.projectCoords(
                    coordinates = coords.copyOfRange(1, 4),
                    parameters = parameters,
                )
                savePosition(parameters)
                GMResult.Ok(projected)
            }
            is Plane3D -> {
                val parameters = positionArray(2)
                val projected = target.projectCoords(
                    coordinates = coords,
                    parameters = parameters,
                )
                savePosition(parameters)
                GMResult.Ok(projected)
            }
            is Sphere3D -> {
                val parameters = positionArray(2)
                when (
                    val result = target.projectCoords(
                        coordinates = coords.copyOfRange(1, 4),
                        parameters = parameters,
                    )
                ) {
                    is GMResult.Ok -> {
                        savePosition(parameters)
                        result
                    }
                    is GMResult.Err -> slideProjectionError(result.error)
                }
            }
            is Surface3D -> {
                when (
                    val result = target.projectCoords(coords, position)
                ) {
                    is GMResult.Ok -> result
                    is GMResult.Err -> slideProjectionError(result.error)
                }
            }
            else -> GMResult.Err(
                Text3DError.UnsupportedSlide(target.elType),
            )
        }
    }

    private fun positionArray(minimumSize: Int): DoubleArray =
        DoubleArray(maxOf(position.size, minimumSize)) { index ->
            position.getOrElse(index) { 0.0 }
        }

    private fun savePosition(values: DoubleArray) {
        position.clear()
        position += values.toList()
    }

    private fun slideProjectionError(
        error: Any,
    ): GMResult.Err<Text3DError.SlideProjection> =
        GMResult.Err(
            Text3DError.SlideProjection(error.toString()),
        )

    internal companion object {
        private const val TEXT_3D_ID_PREFIX = "text3d"
        private const val TEXT_3D_ELEMENT_TYPE = "text3d"

        // JSXGraph: src/3d/text3d.js -> createText3D.
        internal fun create(
            view: View3D,
            coordinateSource: Point3DCoordinateSource,
            content: String,
            slide: GeometryElement3D? = null,
            dependencies: Iterable<GeometryElement> = emptyList(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            fixed: Boolean = false,
            attributes: Map<String, Any?> = emptyMap(),
        ): GMResult<Text3D, Text3DError> {
            if (slide != null) {
                if (slide.view !== view) {
                    return GMResult.Err(Text3DError.SlideViewMismatch)
                }
                if (view.select(slide.id) !== slide) {
                    return GMResult.Err(
                        Text3DError.SlideNotRegistered(slide.id),
                    )
                }
            }
            val text = Text3D(
                view = view,
                coordinateSource = coordinateSource,
                content = content,
                slide = slide,
                isFixed = fixed,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            text.setCreationAttributes(attributes)
            when (val result = text.initCoords()) {
                is GMResult.Ok -> Unit
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
                    name = text.name,
                    needsRegularUpdate = needsRegularUpdate,
                    fixed = fixed,
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
            text.configureElement2D(
                proxy = proxy,
                attributes = attributes,
                defaultWithLabel = false,
                createLabel = false,
            )
            proxy.dump = false
            proxy.setParents(listOf(text))
            text.text2D = proxy
            text.element2D = proxy
            text.inherits += proxy
            text.isDraggable = proxy.isDraggable
            text.addChild(proxy)
            for (dependency in (dependencies + listOfNotNull(slide)).distinct()) {
                dependency.addChild(text)
            }
            if (slide != null) {
                text.setParents(listOf(slide))
            }
            text.projectedCoordinates =
                proxy.coords.usrCoords.copyOf()
            text.prepareUpdate().update()
            return GMResult.Ok(text)
        }
    }
}
