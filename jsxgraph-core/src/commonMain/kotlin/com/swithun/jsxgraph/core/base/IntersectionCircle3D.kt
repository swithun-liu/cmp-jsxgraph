/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/circle3d.js -> createIntersectionCircle3D
 *           src/math/geometry.js -> intersectionFunction3D
 * Copyright 2008-2026 Matthias Ehmann, Aaron Fenyes, Carsten Miller,
 * Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Circle3DIntersection
import com.swithun.jsxgraph.core.math.Geometry

internal sealed interface IntersectionCircle3DError {
    data class UnsupportedParents(
        val firstType: String,
        val secondType: String,
    ) : IntersectionCircle3DError

    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : IntersectionCircle3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : IntersectionCircle3DError

    data class CenterFactory(
        val error: Point3DError,
    ) : IntersectionCircle3DError

    data class CircleFactory(
        val error: Circle3DError,
    ) : IntersectionCircle3DError
}

/**
 * Source-mapped IntersectionCircle3D factory. JSXGraph returns a Circle3D
 * whose center, normal, and radius are supplied by the two parent elements.
 */
internal object IntersectionCircle3D {
    // JSXGraph: src/3d/circle3d.js -> createIntersectionCircle3D.
    internal fun create(
        view: View3D,
        first: GeometryElement3D,
        second: GeometryElement3D,
        sampleCount: Int = Circle3D.DEFAULT_SAMPLE_COUNT,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
    ): GMResult<Circle3D, IntersectionCircle3DError> {
        validateParent(view, first, 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(view, second, 1)?.let {
            return GMResult.Err(it)
        }
        val source = intersectionSource(first, second)
            ?: return GMResult.Err(
                IntersectionCircle3DError.UnsupportedParents(
                    firstType = first.elType,
                    secondType = second.elType,
                ),
            )
        val center = when (
            val result = Point3D.create(
                view = view,
                coordinateSource = Point3DCoordinateSource.Function(
                    Point3DArrayEvaluator {
                        GMResult.Ok(source.evaluate().center)
                    },
                ),
                id = "",
                name = "",
                needsRegularUpdate = needsRegularUpdate,
                fixed = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                IntersectionCircle3DError.CenterFactory(result.error),
            )
        }
        center.dump = false
        val circle = when (
            val result = Circle3D.create(
                view = view,
                center = center,
                normalSource = source.normalSource(),
                radiusSource = Line3DCoordinateValue.Dynamic(
                    Line3DScalarEvaluator {
                        GMResult.Ok(source.evaluate().radius)
                    },
                ),
                ownsCenter = true,
                sampleCount = sampleCount,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                view.board.removeObject(center)
                return GMResult.Err(
                    IntersectionCircle3DError.CircleFactory(result.error),
                )
            }
        }

        first.addChild(circle)
        second.addChild(circle)
        circle.type = Const.OBJECT_TYPE_INTERSECTION_CIRCLE3D
        circle.elType = INTERSECTION_CIRCLE_3D_ELEMENT_TYPE
        circle.setParents(listOf(first, second))
        return GMResult.Ok(circle)
    }

    // JSXGraph: src/math/geometry.js -> intersectionFunction3D.
    private fun intersectionSource(
        first: GeometryElement3D,
        second: GeometryElement3D,
    ): IntersectionSource? =
        when {
            first is Plane3D && second is Sphere3D ->
                IntersectionSource.PlaneSphere(first, second)
            first is Sphere3D && second is Plane3D ->
                IntersectionSource.PlaneSphere(second, first)
            first is Sphere3D && second is Sphere3D ->
                IntersectionSource.SphereSphere(first, second)
            else -> null
        }

    private fun validateParent(
        view: View3D,
        parent: GeometryElement3D,
        parentIndex: Int,
    ): IntersectionCircle3DError? {
        if (parent.view !== view) {
            return IntersectionCircle3DError.ParentViewMismatch(parentIndex)
        }
        if (view.board.elementById(parent.id) !== parent) {
            return IntersectionCircle3DError.ParentNotRegistered(
                parentIndex = parentIndex,
                id = parent.id,
            )
        }
        return null
    }

    private sealed interface IntersectionSource {
        fun evaluate(): Circle3DIntersection

        fun normalSource(): Circle3DNormalSource

        data class PlaneSphere(
            val plane: Plane3D,
            val sphere: Sphere3D,
        ) : IntersectionSource {
            override fun evaluate(): Circle3DIntersection =
                Geometry.meetPlaneSphere(
                    planeNormal = plane.normal,
                    planeDistance = plane.d,
                    sphereCenter = sphere.center.coords,
                    sphereRadius = sphere.Radius(),
                )

            // JSXGraph returns the Plane3D normal array directly here.
            override fun normalSource(): Circle3DNormalSource =
                Circle3DNormalSource.Values(
                    plane.normal.map(Line3DCoordinateValue::Numeric),
                )
        }

        data class SphereSphere(
            val first: Sphere3D,
            val second: Sphere3D,
        ) : IntersectionSource {
            override fun evaluate(): Circle3DIntersection =
                Geometry.meetSphereSphere(
                    firstCenter = first.center.coords,
                    firstRadius = first.Radius(),
                    secondCenter = second.center.coords,
                    secondRadius = second.Radius(),
                )

            override fun normalSource(): Circle3DNormalSource =
                Circle3DNormalSource.Function(
                    Line3DArrayEvaluator {
                        GMResult.Ok(evaluate().normal)
                    },
                )
        }
    }

    private const val INTERSECTION_CIRCLE_3D_ELEMENT_TYPE =
        "intersectioncircle3d"
}
