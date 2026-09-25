package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IntersectionCircle3DTest {
    @Test
    fun sphereSphereIntersectionTracksParentsAndOwnsCenter() {
        val board = createBoard()
        val view = createView(board)
        val firstCenter =
            point(view, doubleArrayOf(-1.0, 0.0, 0.0), "firstCenter")
        val secondCenter =
            point(view, doubleArrayOf(1.0, 0.0, 0.0), "secondCenter")
        val first = sphere(view, firstCenter, 2.0, "first")
        val second = sphere(view, secondCenter, 2.0, "second")
        val circle = intersection(
            IntersectionCircle3D.create(
                view = view,
                first = first,
                second = second,
                sampleCount = 9,
                id = "intersection",
                name = "",
            ),
        )

        assertEquals(Const.OBJECT_TYPE_INTERSECTION_CIRCLE3D, circle.type)
        assertEquals("intersectioncircle3d", circle.elType)
        assertEquals(listOf(first.id, second.id), circle.parents)
        assertEquals(0.0, circle.center.X(), TOLERANCE)
        assertEquals(sqrt(3.0), circle.Radius(), TOLERANCE)
        assertEquals(9, circle.curve.numberPoints)
        assertTrue(!circle.center.dump)
        assertTrue(circle.center.id in circle.childElements)
        assertTrue(circle.id in first.childElements)
        assertTrue(circle.id in second.childElements)

        assertIs<GMResult.Ok<Point3D>>(
            secondCenter.setPosition(doubleArrayOf(4.0, 0.0, 0.0)),
        )
        board.fullUpdate()

        assertTrue(circle.Radius().isNaN())
        assertTrue(
            circle.curve.points.all { point ->
                point.drop(1).all(Double::isNaN)
            },
        )

        val ownedCenterId = circle.center.id
        val curveId = circle.curve.id
        val proxyId = circle.curve.curve2D.id
        board.removeObject(circle)

        assertNull(board.elementById(circle.id))
        assertNull(board.elementById(ownedCenterId))
        assertNull(board.elementById(curveId))
        assertNull(board.elementById(proxyId))
        assertSame(first, board.elementById(first.id))
        assertSame(second, board.elementById(second.id))
        assertTrue(circle.id !in first.childElements)
        assertTrue(circle.id !in second.childElements)
    }

    @Test
    fun planeSphereIntersectionSupportsBothParentOrders() {
        val board = createBoard()
        val view = createView(board)
        val planePoint =
            point(view, doubleArrayOf(0.0, 0.0, 1.0), "planePoint")
        val plane = plane(
            Plane3D.create(
                view = view,
                point = planePoint,
                direction1Source = direction(1.0, 0.0, 0.0),
                direction2Source = direction(0.0, 1.0, 0.0),
                rangeUSource = range(-3.0, 3.0),
                rangeVSource = range(-3.0, 3.0),
                planeType = "wireframe",
                id = "plane",
                name = "",
            ),
        )
        val sphereCenter =
            point(view, doubleArrayOf(0.0, 0.0, 0.0), "sphereCenter")
        val sphere = sphere(view, sphereCenter, 2.0, "sphere")
        val forward = intersection(
            IntersectionCircle3D.create(
                view = view,
                first = plane,
                second = sphere,
                id = "forward",
                name = "",
            ),
        )
        val reverse = intersection(
            IntersectionCircle3D.create(
                view = view,
                first = sphere,
                second = plane,
                id = "reverse",
                name = "",
            ),
        )

        for (circle in listOf(forward, reverse)) {
            assertArrayClose(
                doubleArrayOf(1.0, 0.0, 0.0, 1.0),
                circle.center.coords,
            )
            assertArrayClose(
                doubleArrayOf(0.0, 0.0, 0.0, 1.0),
                circle.normal,
            )
            assertEquals(sqrt(3.0), circle.Radius(), TOLERANCE)
        }
    }

    @Test
    fun unsupportedParentsFailBeforeCreatingOwnedElements() {
        val board = createBoard()
        val view = createView(board)
        val firstPoint =
            point(view, doubleArrayOf(0.0, 0.0, 0.0), "firstPoint")
        val secondPoint =
            point(view, doubleArrayOf(0.0, 0.0, 1.0), "secondPoint")
        val first = plane(
            Plane3D.create(
                view = view,
                point = firstPoint,
                direction1Source = direction(1.0, 0.0, 0.0),
                direction2Source = direction(0.0, 1.0, 0.0),
                id = "first",
                name = "",
            ),
        )
        val second = plane(
            Plane3D.create(
                view = view,
                point = secondPoint,
                direction1Source = direction(1.0, 0.0, 0.0),
                direction2Source = direction(0.0, 1.0, 0.0),
                id = "second",
                name = "",
            ),
        )
        val objectIds = board.objects.keys.toSet()

        assertIs<GMResult.Err<IntersectionCircle3DError.UnsupportedParents>>(
            IntersectionCircle3D.create(
                view = view,
                first = first,
                second = second,
                id = "unsupported",
                name = "",
            ),
        )
        assertEquals(objectIds, board.objects.keys)
    }

    @Test
    fun circleFactoryFailureRollsBackOwnedCenter() {
        val board = createBoard()
        val view = createView(board)
        val firstCenter =
            point(view, doubleArrayOf(-1.0, 0.0, 0.0), "firstCenter")
        val secondCenter =
            point(view, doubleArrayOf(1.0, 0.0, 0.0), "secondCenter")
        val first = sphere(view, firstCenter, 2.0, "first")
        val second = sphere(view, secondCenter, 2.0, "second")
        val existing = point(view, doubleArrayOf(0.0, 0.0, 0.0), "duplicate")
        val objectIds = board.objects.keys.toSet()

        val error = assertIs<
            GMResult.Err<IntersectionCircle3DError.CircleFactory>
            >(
            IntersectionCircle3D.create(
                view = view,
                first = first,
                second = second,
                id = existing.id,
                name = "",
            ),
        ).error

        assertIs<Circle3DError.Registration>(error.error)
        assertEquals(objectIds, board.objects.keys)
        assertSame(existing, board.elementById(existing.id))
    }

    private fun createBoard(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )

    private fun createView(board: Board): View3D =
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
                projection = "parallel",
                azimuth = 1.0,
                elevation = 0.3,
                bank = 0.0,
                id = "view",
                name = "",
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
        view: View3D,
        center: Point3D,
        radius: Double,
        id: String,
    ): Sphere3D =
        assertIs<GMResult.Ok<Sphere3D>>(
            Sphere3D.create(
                view = view,
                center = center,
                radiusSource = Line3DCoordinateValue.Numeric(radius),
                id = id,
                name = "",
            ),
        ).value

    private fun plane(
        result: GMResult<Plane3D, Plane3DError>,
    ): Plane3D = assertIs<GMResult.Ok<Plane3D>>(result).value

    private fun intersection(
        result: GMResult<Circle3D, IntersectionCircle3DError>,
    ): Circle3D = assertIs<GMResult.Ok<Circle3D>>(result).value

    private fun direction(
        x: Double,
        y: Double,
        z: Double,
    ): Plane3DDirectionSource =
        Plane3DDirectionSource.Values(
            listOf(x, y, z).map(Line3DCoordinateValue::Numeric),
        )

    private fun range(
        minimum: Double,
        maximum: Double,
    ): List<Line3DCoordinateValue> =
        listOf(minimum, maximum).map(Line3DCoordinateValue::Numeric)

    private fun assertArrayClose(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                absoluteTolerance = TOLERANCE,
            )
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
