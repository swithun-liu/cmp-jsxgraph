/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js ->
 * createReflection / createMirrorElement
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI

internal sealed interface ElementReflectionError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : ElementReflectionError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : ElementReflectionError

    data class UnsupportedSource(
        val elementType: String,
    ) : ElementReflectionError

    data class TransformationFactory(
        val error: TransformationError,
    ) : ElementReflectionError

    data class PointFactory(
        val role: String,
        val error: PointError,
    ) : ElementReflectionError

    data class LineFactory(
        val error: LineError,
    ) : ElementReflectionError

    data class CurveFactory(
        val error: CurveError,
    ) : ElementReflectionError

    data class PolygonFactory(
        val error: PolygonError,
    ) : ElementReflectionError

    data class CircleFactory(
        val error: CircleError,
    ) : ElementReflectionError

    data class EllipseFactory(
        val error: EllipseError,
    ) : ElementReflectionError
}

internal enum class ElementReflectionTransformationType {
    EUCLIDEAN,
    PROJECTIVE,
}

internal data class ElementReflectionPointAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val fixed: Boolean = false,
)

internal data class ElementReflectionAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val fixed: Boolean = true,
    val transformationType: ElementReflectionTransformationType =
        ElementReflectionTransformationType.EUCLIDEAN,
    val point1: ElementReflectionPointAttributes =
        ElementReflectionPointAttributes(),
    val point2: ElementReflectionPointAttributes =
        ElementReflectionPointAttributes(),
    val vertices: ElementReflectionPointAttributes =
        ElementReflectionPointAttributes(),
    val center: ElementReflectionPointAttributes =
        ElementReflectionPointAttributes(),
)

internal object ElementReflections {
    // JSXGraph 1.13.3: src/element/composition.js ->
    // createReflection, non-Point branches.
    internal fun createReflection(
        board: Board,
        source: GeometryElement,
        line: Line,
        attributes: ElementReflectionAttributes =
            ElementReflectionAttributes(),
    ): GMResult<GeometryElement, ElementReflectionError> {
        validateParent(board, source, 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(board, line, 1)?.let {
            return GMResult.Err(it)
        }
        return create(
            board = board,
            source = source,
            reflector = line,
            transformation = Transformation.createReflectionFromLine(line),
            attributes = attributes,
            elementType = REFLECTION_ELEMENT_TYPE,
        )
    }

    // JSXGraph 1.13.3: src/element/composition.js ->
    // createMirrorElement, non-Point branches.
    internal fun createMirrorElement(
        board: Board,
        source: GeometryElement,
        mirror: Point,
        attributes: ElementReflectionAttributes =
            ElementReflectionAttributes(),
    ): GMResult<GeometryElement, ElementReflectionError> {
        validateParent(board, source, 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(board, mirror, 1)?.let {
            return GMResult.Err(it)
        }
        val transformation = when (
            val result = Transformation.createRotation(
                board = board,
                angle = TransformationParameter.Numeric(PI),
                center = mirror,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                ElementReflectionError.TransformationFactory(result.error),
            )
        }
        return create(
            board = board,
            source = source,
            reflector = mirror,
            transformation = transformation,
            attributes = attributes,
            elementType = MIRROR_ELEMENT_TYPE,
        )
    }

    private fun create(
        board: Board,
        source: GeometryElement,
        reflector: GeometryElement,
        transformation: Transformation,
        attributes: ElementReflectionAttributes,
        elementType: String,
    ): GMResult<GeometryElement, ElementReflectionError> {
        val output = when (source) {
            is Polygon -> createPolygon(
                board = board,
                source = source,
                transformation = transformation,
                attributes = attributes,
            )
            is Line -> createLine(
                board = board,
                source = source,
                transformation = transformation,
                attributes = attributes,
            )
            is Circle ->
                if (
                    attributes.transformationType ==
                    ElementReflectionTransformationType.EUCLIDEAN
                ) {
                    createCircle(
                        board = board,
                        source = source,
                        transformation = transformation,
                        attributes = attributes,
                    )
                } else {
                    createProjectiveCircle(
                        board = board,
                        source = source,
                        transformation = transformation,
                        attributes = attributes,
                    )
                }
            is CurveTransformationSource -> createCurve(
                board = board,
                source = source,
                transformation = transformation,
                attributes = attributes,
            )
            else -> return GMResult.Err(
                ElementReflectionError.UnsupportedSource(
                    source.elType.ifEmpty { "element" },
                ),
            )
        }
        val element = when (output) {
            is GMResult.Ok -> output.value
            is GMResult.Err -> return output
        }
        reflector.addChild(element)
        element.elType = elementType
        element.addParents(listOf(reflector))
        element.setAttribute(mapOf("fixed" to attributes.fixed))
        element.prepareUpdate().update()
        return GMResult.Ok(element)
    }

    private fun createLine(
        board: Board,
        source: Line,
        transformation: Transformation,
        attributes: ElementReflectionAttributes,
    ): GMResult<GeometryElement, ElementReflectionError> {
        val first = when (
            val result = transformedPoint(
                board = board,
                source = source.point1,
                transformation = transformation,
                attributes = attributes.point1,
                role = "point1",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = when (
            val result = transformedPoint(
                board = board,
                source = source.point2,
                transformation = transformation,
                attributes = attributes.point2,
                role = "point2",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(first)
                return result
            }
        }
        return when (
            val result = Line.create(
                board = board,
                point1 = first,
                point2 = second,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(result.value)
            is GMResult.Err -> {
                board.removeObjects(listOf(first, second))
                GMResult.Err(
                    ElementReflectionError.LineFactory(result.error),
                )
            }
        }
    }

    private fun createCurve(
        board: Board,
        source: CurveTransformationSource,
        transformation: Transformation,
        attributes: ElementReflectionAttributes,
    ): GMResult<GeometryElement, ElementReflectionError> =
        when (
            val result = Curve.createTransformed(
                board = board,
                source = source,
                transformations = listOf(transformation),
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(result.value)
            is GMResult.Err -> GMResult.Err(
                ElementReflectionError.CurveFactory(result.error),
            )
        }

    private fun createPolygon(
        board: Board,
        source: Polygon,
        transformation: Transformation,
        attributes: ElementReflectionAttributes,
    ): GMResult<GeometryElement, ElementReflectionError> {
        val sourceVertices =
            if (
                source.vertices.size > 1 &&
                source.vertices.first().id ==
                source.vertices.last().id
            ) {
                source.vertices.dropLast(1)
            } else {
                source.vertices.toList()
            }
        val vertices = mutableListOf<Point>()
        for ((index, sourceVertex) in sourceVertices.withIndex()) {
            when (
                val result = transformedPoint(
                    board = board,
                    source = sourceVertex,
                    transformation = transformation,
                    attributes = attributes.vertices,
                    role = "vertex[$index]",
                )
            ) {
                is GMResult.Ok -> vertices += result.value
                is GMResult.Err -> {
                    board.removeObjects(vertices)
                    return result
                }
            }
        }
        return when (
            val result = Polygon.create(
                board = board,
                vertices = vertices,
                ownedVertices = emptySet(),
                withLines = source.withLines,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(result.value)
            is GMResult.Err -> {
                board.removeObjects(vertices)
                GMResult.Err(
                    ElementReflectionError.PolygonFactory(result.error),
                )
            }
        }
    }

    private fun createCircle(
        board: Board,
        source: Circle,
        transformation: Transformation,
        attributes: ElementReflectionAttributes,
    ): GMResult<GeometryElement, ElementReflectionError> {
        val center = when (
            val result = transformedPoint(
                board = board,
                source = source.center,
                transformation = transformation,
                attributes = attributes.center,
                role = "center",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Circle.create(
                board = board,
                center = center,
                radiusFunction = source::Radius,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(result.value)
            is GMResult.Err -> {
                board.removeObject(center)
                GMResult.Err(
                    ElementReflectionError.CircleFactory(result.error),
                )
            }
        }
    }

    private fun createProjectiveCircle(
        board: Board,
        source: Circle,
        transformation: Transformation,
        attributes: ElementReflectionAttributes,
    ): GMResult<GeometryElement, ElementReflectionError> {
        val majorAxisTerm = object : JessieCodeCoordinateFunction {
            override val origin: String? = null
            override val dependencies: Map<String, GeometryElement> =
                emptyMap()

            override fun evaluate(
                arguments: List<JessieCodeRuntimeValue>,
            ): GMResult<
                JessieCodeRuntimeValue,
                JessieCodeRuntimeError,
                > =
                GMResult.Ok(
                    JessieCodeRuntimeValue.NumberValue(
                        2.0 * source.Radius(),
                    ),
                )
        }
        return when (
            val result = Ellipse.create(
                board = board,
                focus1 = source.center,
                focus2 = source.center,
                majorAxisTerm = majorAxisTerm,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                centerId = attributes.center.id,
                centerName = attributes.center.name,
                centerNeedsRegularUpdate =
                    attributes.center.needsRegularUpdate,
                centerFixed = attributes.center.fixed,
            )
        ) {
            is GMResult.Ok -> {
                result.value.addTransform(transformation)
                GMResult.Ok(result.value)
            }
            is GMResult.Err -> GMResult.Err(
                ElementReflectionError.EllipseFactory(result.error),
            )
        }
    }

    private fun transformedPoint(
        board: Board,
        source: Point,
        transformation: Transformation,
        attributes: ElementReflectionPointAttributes,
        role: String,
    ): GMResult<Point, ElementReflectionError> =
        when (
            val result = Point.create(
                board = board,
                basePoint = source,
                transformations = listOf(transformation),
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                fixed = attributes.fixed,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                ElementReflectionError.PointFactory(
                    role = role,
                    error = result.error,
                ),
            )
        }

    private fun validateParent(
        board: Board,
        element: GeometryElement,
        parentIndex: Int,
    ): ElementReflectionError? =
        when {
            element.board !== board ->
                ElementReflectionError.ParentBoardMismatch(parentIndex)
            board.elementById(element.id) !== element ->
                ElementReflectionError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = element.id,
                )
            else -> null
        }

    private const val REFLECTION_ELEMENT_TYPE = "reflection"
    private const val MIRROR_ELEMENT_TYPE = "mirrorelement"
}
