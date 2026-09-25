/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/linspace3d.js -> createIntersectionLine3D
 *           src/math/geometry.js -> intersectionFunction3D
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal data class IntersectionLine3DPointAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val fixed: Boolean = false,
)

internal sealed interface IntersectionLine3DError {
    data class UnsupportedParents(
        val firstType: String,
        val secondType: String,
    ) : IntersectionLine3DError

    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : IntersectionLine3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : IntersectionLine3DError

    data class MissingEndpoint(
        val endpointIndex: Int,
    ) : IntersectionLine3DError

    data class PointFactory(
        val endpointIndex: Int,
        val error: Point3DError,
    ) : IntersectionLine3DError

    data class LineFactory(
        val error: Line3DError,
    ) : IntersectionLine3DError
}

/**
 * Source-mapped IntersectionLine3D factory. JSXGraph 1.13.3 computes the
 * clipped plane intersection once while creating two owned Point3D elements,
 * then creates the visible Line3D from those points.
 */
internal object IntersectionLine3D {
    // JSXGraph: src/3d/linspace3d.js -> createIntersectionLine3D.
    internal fun create(
        view: View3D,
        first: GeometryElement3D,
        second: GeometryElement3D,
        point1Attributes: IntersectionLine3DPointAttributes =
            IntersectionLine3DPointAttributes(),
        point2Attributes: IntersectionLine3DPointAttributes =
            IntersectionLine3DPointAttributes(),
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        fixed: Boolean = true,
    ): GMResult<Line3D, IntersectionLine3DError> {
        validateParent(view, first, 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(view, second, 1)?.let {
            return GMResult.Err(it)
        }
        val firstPlane = first as? Plane3D
            ?: return unsupported(first, second)
        val secondPlane = second as? Plane3D
            ?: return unsupported(first, second)
        val coordinates = view.intersectionPlanePlane(
            firstNormal = firstPlane.normal,
            firstDistance = firstPlane.d,
            firstVector1 = firstPlane.vec1,
            firstVector2 = firstPlane.vec2,
            secondNormal = secondPlane.normal,
            secondDistance = secondPlane.d,
            secondVector1 = secondPlane.vec1,
            secondVector2 = secondPlane.vec2,
        )
        val point1Coordinates = coordinates[0]
            ?: return GMResult.Err(
                IntersectionLine3DError.MissingEndpoint(0),
            )
        val point2Coordinates = coordinates[1]
            ?: return GMResult.Err(
                IntersectionLine3DError.MissingEndpoint(1),
            )
        val point1 = when (
            val result = createPoint(
                view = view,
                coordinates = point1Coordinates,
                attributes = point1Attributes,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                IntersectionLine3DError.PointFactory(
                    endpointIndex = 0,
                    error = result.error,
                ),
            )
        }
        val point2 = when (
            val result = createPoint(
                view = view,
                coordinates = point2Coordinates,
                attributes = point2Attributes,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                view.board.removeObject(point1)
                return GMResult.Err(
                    IntersectionLine3DError.PointFactory(
                        endpointIndex = 1,
                        error = result.error,
                    ),
                )
            }
        }
        val line = when (
            val result = Line3D.create(
                view = view,
                point1 = point1,
                point2 = point2,
                ownsPoint1 = true,
                ownsPoint2 = true,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                IntersectionLine3DError.LineFactory(result.error),
            )
        }

        firstPlane.addChild(line)
        secondPlane.addChild(line)
        line.type = Const.OBJECT_TYPE_INTERSECTION_LINE3D
        line.elType = INTERSECTION_LINE_3D_ELEMENT_TYPE
        line.setParents(listOf(firstPlane, secondPlane))
        return GMResult.Ok(line)
    }

    private fun createPoint(
        view: View3D,
        coordinates: DoubleArray,
        attributes: IntersectionLine3DPointAttributes,
    ): GMResult<Point3D, Point3DError> =
        Point3D.create(
            view = view,
            coordinates = coordinates,
            id = attributes.id,
            name = attributes.name,
            needsRegularUpdate = attributes.needsRegularUpdate,
            fixed = attributes.fixed,
        )

    private fun validateParent(
        view: View3D,
        parent: GeometryElement3D,
        parentIndex: Int,
    ): IntersectionLine3DError? {
        if (parent.view !== view) {
            return IntersectionLine3DError.ParentViewMismatch(parentIndex)
        }
        if (view.board.elementById(parent.id) !== parent) {
            return IntersectionLine3DError.ParentNotRegistered(
                parentIndex = parentIndex,
                id = parent.id,
            )
        }
        return null
    }

    private fun unsupported(
        first: GeometryElement3D,
        second: GeometryElement3D,
    ): GMResult.Err<IntersectionLine3DError.UnsupportedParents> =
        GMResult.Err(
            IntersectionLine3DError.UnsupportedParents(
                firstType = first.elType,
                secondType = second.elType,
            ),
        )

    private const val INTERSECTION_LINE_3D_ELEMENT_TYPE =
        "intersectionline3d"
}
