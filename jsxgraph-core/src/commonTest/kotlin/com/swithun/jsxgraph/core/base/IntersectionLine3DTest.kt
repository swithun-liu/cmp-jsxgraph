package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IntersectionLine3DTest {
    @Test
    fun planeIntersectionCreatesOwnedPointsAndCanonicalParents() {
        val board = createBoard()
        val view = createView(board)
        val horizontal = plane(
            view = view,
            pointCoordinates = doubleArrayOf(0.0, 0.0, 0.0),
            direction1 = direction(1.0, 0.0, 0.0),
            direction2 = direction(0.0, 1.0, 0.0),
            id = "horizontal",
        )
        val vertical = plane(
            view = view,
            pointCoordinates = doubleArrayOf(1.0, 0.0, 0.0),
            direction1 = direction(0.0, 1.0, 0.0),
            direction2 = direction(0.0, 0.0, 1.0),
            id = "vertical",
        )
        val line = intersection(
            IntersectionLine3D.create(
                view = view,
                first = horizontal.plane,
                second = vertical.plane,
                point1Attributes =
                    IntersectionLine3DPointAttributes(
                        id = "intersectionPoint1",
                    ),
                point2Attributes =
                    IntersectionLine3DPointAttributes(
                        id = "intersectionPoint2",
                    ),
                id = "intersection",
                name = "",
            ),
        )

        board.fullUpdate()

        assertEquals(Const.OBJECT_TYPE_INTERSECTION_LINE3D, line.type)
        assertEquals("intersectionline3d", line.elType)
        assertEquals(
            listOf(horizontal.plane.id, vertical.plane.id),
            line.parents,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 4.0, 0.0),
            line.point1.coords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, -4.0, 0.0),
            line.point2.coords,
        )
        assertSame(line.point1, board.elementById("intersectionPoint1"))
        assertSame(line.point2, board.elementById("intersectionPoint2"))
        assertTrue(line.point1.id in line.childElements)
        assertTrue(line.point2.id in line.childElements)
        assertTrue(line.id in horizontal.plane.childElements)
        assertTrue(line.id in vertical.plane.childElements)

        val ownedIds = buildSet {
            add(line.point1.id)
            add(line.point1.point2D.id)
            add(line.point2.id)
            add(line.point2.point2D.id)
            line.endpoints.forEach { endpoint ->
                add(endpoint.id)
                add(endpoint.point2D.id)
            }
            add(line.line2D.id)
        }
        board.removeObject(line)

        assertNull(board.elementById(line.id))
        assertTrue(ownedIds.all { board.elementById(it) == null })
        assertSame(
            horizontal.plane,
            board.elementById(horizontal.plane.id),
        )
        assertSame(vertical.plane, board.elementById(vertical.plane.id))
        assertTrue(line.id !in horizontal.plane.childElements)
        assertTrue(line.id !in vertical.plane.childElements)
    }

    @Test
    fun creationUsesTheOfficialOneTimeIntersectionSnapshot() {
        val board = createBoard()
        val view = createView(board)
        val horizontal = plane(
            view = view,
            pointCoordinates = doubleArrayOf(0.0, 0.0, 0.0),
            direction1 = direction(1.0, 0.0, 0.0),
            direction2 = direction(0.0, 1.0, 0.0),
            id = "horizontal",
        )
        val vertical = plane(
            view = view,
            pointCoordinates = doubleArrayOf(1.0, 0.0, 0.0),
            direction1 = direction(0.0, 1.0, 0.0),
            direction2 = direction(0.0, 0.0, 1.0),
            id = "vertical",
        )
        val line = intersection(
            IntersectionLine3D.create(
                view = view,
                first = horizontal.plane,
                second = vertical.plane,
                id = "intersection",
                name = "",
            ),
        )
        val initialPoint1 = line.point1.coords.copyOf()
        val initialPoint2 = line.point2.coords.copyOf()

        assertIs<GMResult.Ok<Point3D>>(
            vertical.point.setPosition(doubleArrayOf(2.0, 0.0, 0.0)),
        )
        board.fullUpdate()

        assertContentEquals(initialPoint1, line.point1.coords)
        assertContentEquals(initialPoint2, line.point2.coords)
        assertEquals(2.0, vertical.plane.point.X())
    }

    @Test
    fun unsupportedAndParallelParentsFailWithoutCreatingHelpers() {
        val board = createBoard()
        val view = createView(board)
        val first = plane(
            view = view,
            pointCoordinates = doubleArrayOf(0.0, 0.0, 0.0),
            direction1 = direction(1.0, 0.0, 0.0),
            direction2 = direction(0.0, 1.0, 0.0),
            id = "first",
        )
        val parallel = plane(
            view = view,
            pointCoordinates = doubleArrayOf(0.0, 0.0, 1.0),
            direction1 = direction(1.0, 0.0, 0.0),
            direction2 = direction(0.0, 1.0, 0.0),
            id = "parallel",
        )
        val sphereCenter =
            point(view, doubleArrayOf(0.0, 0.0, 0.0), "sphereCenter")
        val sphere = assertIs<GMResult.Ok<Sphere3D>>(
            Sphere3D.create(
                view = view,
                center = sphereCenter,
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
                id = "sphere",
                name = "",
            ),
        ).value
        val objectIds = board.objects.keys.toSet()

        assertIs<GMResult.Err<IntersectionLine3DError.UnsupportedParents>>(
            IntersectionLine3D.create(
                view = view,
                first = first.plane,
                second = sphere,
                id = "unsupported",
            ),
        )
        assertEquals(objectIds, board.objects.keys)

        assertIs<GMResult.Err<IntersectionLine3DError.MissingEndpoint>>(
            IntersectionLine3D.create(
                view = view,
                first = first.plane,
                second = parallel.plane,
                id = "parallelIntersection",
            ),
        )
        assertEquals(objectIds, board.objects.keys)
    }

    @Test
    fun pointAndLineRegistrationFailuresRollbackOwnedHelpers() {
        val board = createBoard()
        val view = createView(board)
        val horizontal = plane(
            view = view,
            pointCoordinates = doubleArrayOf(0.0, 0.0, 0.0),
            direction1 = direction(1.0, 0.0, 0.0),
            direction2 = direction(0.0, 1.0, 0.0),
            id = "horizontal",
        )
        val vertical = plane(
            view = view,
            pointCoordinates = doubleArrayOf(1.0, 0.0, 0.0),
            direction1 = direction(0.0, 1.0, 0.0),
            direction2 = direction(0.0, 0.0, 1.0),
            id = "vertical",
        )
        val existing =
            point(view, doubleArrayOf(0.0, 0.0, 0.0), "duplicate")
        val objectIds = board.objects.keys.toSet()

        val pointError = assertIs<
            GMResult.Err<IntersectionLine3DError.PointFactory>
            >(
            IntersectionLine3D.create(
                view = view,
                first = horizontal.plane,
                second = vertical.plane,
                point2Attributes =
                    IntersectionLine3DPointAttributes(id = existing.id),
                id = "pointFailure",
            ),
        ).error
        assertEquals(1, pointError.endpointIndex)
        assertIs<Point3DError.Registration>(pointError.error)
        assertEquals(objectIds, board.objects.keys)

        val lineError = assertIs<
            GMResult.Err<IntersectionLine3DError.LineFactory>
            >(
            IntersectionLine3D.create(
                view = view,
                first = horizontal.plane,
                second = vertical.plane,
                id = existing.id,
            ),
        ).error
        assertIs<Line3DError.Registration>(lineError.error)
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

    private fun plane(
        view: View3D,
        pointCoordinates: DoubleArray,
        direction1: Plane3DDirectionSource,
        direction2: Plane3DDirectionSource,
        id: String,
    ): PlaneDefinition {
        val point = point(view, pointCoordinates, "${id}Point")
        val plane = assertIs<GMResult.Ok<Plane3D>>(
            Plane3D.create(
                view = view,
                point = point,
                direction1Source = direction1,
                direction2Source = direction2,
                planeType = "wireframe",
                id = id,
                name = "",
            ),
        ).value
        return PlaneDefinition(point, plane)
    }

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

    private fun direction(
        x: Double,
        y: Double,
        z: Double,
    ): Plane3DDirectionSource =
        Plane3DDirectionSource.Values(
            listOf(x, y, z).map(Line3DCoordinateValue::Numeric),
        )

    private fun intersection(
        result: GMResult<Line3D, IntersectionLine3DError>,
    ): Line3D = assertIs<GMResult.Ok<Line3D>>(result).value

    private data class PlaneDefinition(
        val point: Point3D,
        val plane: Plane3D,
    )
}
