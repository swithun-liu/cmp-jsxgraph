package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Sphere3DTest {
    @Test
    fun parallelProjectionUsesCircleAndDynamicAbsoluteRadius() {
        val board = createBoard()
        val view = createView(board, projection = "parallel")
        var radius = -2.0
        val center = point(view, doubleArrayOf(0.0, 0.0, 0.0), "center")
        val sphere = sphere(
            Sphere3D.create(
                view = view,
                center = center,
                radiusSource = Line3DCoordinateValue.Dynamic(
                    Line3DScalarEvaluator { GMResult.Ok(radius) },
                ),
                id = "sphere",
                name = "",
            ),
        )

        val proxy = assertIs<Circle>(sphere.element2D)
        assertEquals(Const.OBJECT_TYPE_SPHERE3D, sphere.type)
        assertEquals("sphere3d", sphere.elType)
        assertEquals(Sphere3DMethod.PointRadius, sphere.method)
        assertEquals(2.0, sphere.Radius())
        assertEquals(2.0, proxy.Radius())
        assertEquals(listOf(center.point2D.id), proxy.parents)
        assertTrue(proxy.id in sphere.childElements)
        assertTrue(sphere.id in center.childElements)

        radius = -3.5
        board.fullUpdate()

        assertSame(proxy, sphere.element2D)
        assertEquals(3.5, sphere.Radius())
        assertEquals(3.5, proxy.Radius())
    }

    @Test
    fun twoPointSphereAndProjectCoordsFollowUpstream() {
        val board = createBoard()
        val view = createView(board, projection = "parallel")
        val center = point(view, doubleArrayOf(0.0, 0.0, 0.0), "center")
        val surfacePoint =
            point(view, doubleArrayOf(2.0, 0.0, 0.0), "surfacePoint")
        val sphere = sphere(
            Sphere3D.create(
                view = view,
                center = center,
                point2 = surfacePoint,
                id = "sphere",
                name = "",
            ),
        )
        val parameters = doubleArrayOf(Double.NaN, Double.NaN)
        val projected = assertIs<GMResult.Ok<DoubleArray>>(
            sphere.projectCoords(
                coordinates = doubleArrayOf(4.0, 0.0, 0.0),
                parameters = parameters,
            ),
        ).value

        assertEquals(Sphere3DMethod.TwoPoints, sphere.method)
        assertEquals(2.0, sphere.Radius())
        assertContentEquals(doubleArrayOf(1.0, 2.0, 0.0, 0.0), projected)
        assertEquals(PI / 2.0, parameters[0], absoluteTolerance = 1.0e-12)
        assertEquals(0.0, parameters[1], absoluteTolerance = 1.0e-12)
        assertTrue(sphere.id in surfacePoint.childElements)
    }

    @Test
    fun centralProjectionUsesEllipseAndOwnsAuxiliaryPoints() {
        val board = createBoard()
        val view = createView(board, projection = "central")
        val center = point(view, doubleArrayOf(0.0, 0.0, 0.0), "center")
        val sphere = sphere(
            Sphere3D.create(
                view = view,
                center = center,
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
                sampleCount = 17,
                id = "sphere",
                name = "",
            ),
        )

        val proxy = assertIs<Curve>(sphere.element2D)
        assertTrue(proxy.isEllipse)
        assertEquals(17, proxy.numberPoints)
        assertEquals(3, sphere.aux2D.size)
        assertEquals(3, sphere.points.size)
        assertTrue(sphere.aux2D.all { !it.dump })
        assertTrue(proxy.parents.isEmpty())
        assertTrue(sphere.aux2D.all { it.id in sphere.childElements })
    }

    @Test
    fun runtimeProjectionSwitchRebuildsAndCleansOwnedElements() {
        val board = createBoard()
        val view = createView(
            board = board,
            projection = "parallel",
            needsRegularUpdate = false,
        )
        val center = point(view, doubleArrayOf(0.0, 0.0, 0.0), "center")
        val sphere = sphere(
            Sphere3D.create(
                view = view,
                center = center,
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
                sampleCount = 17,
                id = "sphere",
                name = "",
            ),
        )
        val parallelProxy = assertIs<Circle>(sphere.element2D)
        val parallelProjectionIds = sphere.childElements.keys.toSet()

        assertSame(view, view.setProjection("CENTRAL"))

        val centralProxy = assertIs<Curve>(sphere.element2D)
        val centralProjectionIds = sphere.childElements.keys.toSet()
        assertEquals("central", view.projectionType)
        assertEquals("central", sphere.projectionType)
        assertTrue(centralProxy.isEllipse)
        assertTrue(parallelProxy.id != centralProxy.id)
        assertTrue(
            parallelProjectionIds.all { board.elementById(it) == null },
        )
        assertEquals(3, sphere.aux2D.size)
        assertEquals(3, sphere.points.size)
        assertEquals(
            listOf<GeometryElement>(centralProxy),
            sphere.inherits,
        )
        assertTrue(
            centralProjectionIds.all { board.elementById(it) != null },
        )

        assertSame(view, view.setProjection("parallel"))

        val restoredProxy = assertIs<Circle>(sphere.element2D)
        assertEquals("parallel", view.projectionType)
        assertEquals("parallel", sphere.projectionType)
        assertTrue(centralProxy.id != restoredProxy.id)
        assertTrue(
            centralProjectionIds.all { board.elementById(it) == null },
        )
        assertTrue(sphere.aux2D.isEmpty())
        assertTrue(sphere.points.isEmpty())
        assertEquals(
            listOf<GeometryElement>(restoredProxy),
            sphere.inherits,
        )
        assertEquals(setOf(restoredProxy.id), sphere.childElements.keys)
        assertEquals(
            setOf(restoredProxy.id),
            center.point2D.childElements.keys,
        )
    }

    @Test
    fun dynamicProjectionKeepsLastValidModeOnEvaluationFailure() {
        val board = createBoard()
        var projection = "parallel"
        var rejected = false
        var evaluationCount = 0
        val view = createView(
            board = board,
            projection = "unused",
            projectionSource = View3DProjectionSource.Dynamic(
                View3DProjectionEvaluator {
                    evaluationCount += 1
                    if (rejected) {
                        GMResult.Err(
                            View3DProjectionDynamicError.Rejected(
                                "projection",
                            ),
                        )
                    } else {
                        GMResult.Ok(projection)
                    }
                },
            ),
        )
        assertEquals(1, evaluationCount)
        val center = point(view, doubleArrayOf(0.0, 0.0, 0.0), "center")
        val sphere = sphere(
            Sphere3D.create(
                view = view,
                center = center,
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
                id = "sphere",
                name = "",
            ),
        )
        assertEquals(2, evaluationCount)

        projection = "central"
        board.fullUpdate()
        val centralProxy = assertIs<Curve>(sphere.element2D)
        assertNull(view.evaluationError)
        assertEquals(3, evaluationCount)

        projection = "parallel"
        rejected = true
        board.fullUpdate()

        assertEquals("central", view.projectionType)
        assertSame(centralProxy, sphere.element2D)
        assertIs<View3DError.ProjectionEvaluation>(view.evaluationError)
        assertEquals(4, evaluationCount)

        rejected = false
        board.fullUpdate()

        assertEquals("parallel", view.projectionType)
        assertIs<Circle>(sphere.element2D)
        assertNull(view.evaluationError)
        assertNull(board.elementById(centralProxy.id))
        assertEquals(5, evaluationCount)
    }

    @Test
    fun removalKeepsExternalParentsAndRemovesOwnedParentsAndProxies() {
        val board = createBoard()
        val view = createView(board, projection = "central")
        val ownedCenter =
            point(view, doubleArrayOf(-1.0, 0.0, 0.0), "ownedCenter")
        val externalCenter =
            point(view, doubleArrayOf(1.0, 0.0, 0.0), "externalCenter")
        val owned = sphere(
            Sphere3D.create(
                view = view,
                center = ownedCenter,
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
                ownsCenter = true,
                id = "owned",
                name = "",
            ),
        )
        val external = sphere(
            Sphere3D.create(
                view = view,
                center = externalCenter,
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
                id = "external",
                name = "",
            ),
        )
        val ownedProjectionIds =
            owned.childElements.keys.toList()
        val externalProjectionIds =
            external.childElements.keys.toList()

        board.removeObject(owned)
        board.removeObject(external)

        assertNull(board.elementById(owned.id))
        assertNull(board.elementById(ownedCenter.id))
        assertTrue(ownedProjectionIds.all { board.elementById(it) == null })
        assertNull(board.elementById(external.id))
        assertSame(externalCenter, board.elementById(externalCenter.id))
        assertTrue(
            externalProjectionIds.all { board.elementById(it) == null },
        )
    }

    @Test
    fun malformedInputsReturnStructuredErrors() {
        val view = createView(createBoard(), projection = "parallel")
        val center = point(view, doubleArrayOf(0.0, 0.0, 0.0), "center")

        assertIs<GMResult.Err<Sphere3DError.MissingRadius>>(
            Sphere3D.create(view = view, center = center),
        )
        assertIs<GMResult.Err<Sphere3DError.InvalidSampleCount>>(
            Sphere3D.create(
                view = view,
                center = center,
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
                sampleCount = 0,
            ),
        )
        assertIs<GMResult.Err<Sphere3DError.RadiusEvaluation>>(
            Sphere3D.create(
                view = view,
                center = center,
                radiusSource = Line3DCoordinateValue.Dynamic(
                    Line3DScalarEvaluator {
                        GMResult.Err(
                            Line3DDynamicError.Rejected("radius"),
                        )
                    },
                ),
            ),
        )
        assertIs<GMResult.Err<Sphere3DError.InvalidCoordinateCount>>(
            sphere(
                Sphere3D.create(
                    view = view,
                    center = center,
                    radiusSource = Line3DCoordinateValue.Numeric(1.0),
                ),
            ).projectCoords(
                coordinates = doubleArrayOf(0.0, 0.0),
                parameters = doubleArrayOf(0.0, 0.0),
            ),
        )
    }

    private fun createBoard(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )

    private fun createView(
        board: Board,
        projection: String,
        projectionSource: View3DProjectionSource? = null,
        needsRegularUpdate: Boolean = true,
    ): View3D =
        assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(10.0, 8.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 4.0),
                    doubleArrayOf(-3.0, 5.0),
                ),
                projection = projection,
                projectionSource = projectionSource,
                azimuth = 1.0,
                elevation = 0.3,
                bank = 0.0,
                id = "view",
                name = "",
                needsRegularUpdate = needsRegularUpdate,
            ),
        ).value

    private fun point(
        view: View3D,
        coordinates: DoubleArray,
        id: String,
    ): Point3D =
        assertIs<GMResult.Ok<Point3D>>(
            Point3D.create(
                view = view,
                coordinates = coordinates,
                id = id,
                name = "",
            ),
        ).value

    private fun sphere(
        result: GMResult<Sphere3D, Sphere3DError>,
    ): Sphere3D = assertIs<GMResult.Ok<Sphere3D>>(result).value
}
