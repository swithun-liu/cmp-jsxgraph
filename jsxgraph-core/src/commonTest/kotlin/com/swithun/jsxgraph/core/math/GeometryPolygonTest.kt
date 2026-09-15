package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GeometryPolygonTest {
    @Test
    fun vertexSortingMatchesOfficialAngularOrderAndClosure() {
        val first = point(0.0, 0.0)
        val diagonal = point(1.0, 1.0)
        val right = point(1.0, 0.0)
        val top = point(0.0, 1.0)
        val sorted = Geometry.sortVertices(
            listOf(first, diagonal, right, top),
        )

        assertEquals(listOf(first, right, diagonal, top), sorted)

        val closed = Geometry.sortVertices(
            listOf(first, diagonal, right, top, first, first),
        )
        assertEquals(5, closed.size)
        assertSame(first, closed.first())
        assertSame(first, closed.last())

        assertEquals(emptyList(), Geometry.sortVertices(emptyList()))
        assertEquals(listOf(first), Geometry.sortVertices(listOf(first)))
    }

    @Test
    fun signedPolygonMatchesOfficialWindingAndSortingBehavior() {
        val counterClockwise = listOf(
            point(0.0, 0.0),
            point(1.0, 0.0),
            point(1.0, 1.0),
            point(0.0, 1.0),
        )
        val clockwise = listOf(
            point(0.0, 0.0),
            point(0.0, 1.0),
            point(1.0, 1.0),
            point(1.0, 0.0),
        )
        val crossing = listOf(
            point(0.0, 0.0),
            point(2.0, 2.0),
            point(0.0, 2.0),
            point(2.0, 0.0),
        )

        assertEquals(1.0, Geometry.signedPolygon(counterClockwise, sort = true))
        assertEquals(-1.0, Geometry.signedPolygon(clockwise, sort = true))
        assertEquals(0.0, Geometry.signedPolygon(crossing, sort = true))
        assertEquals(4.0, Geometry.signedPolygon(crossing, sort = false))
        assertEquals(0.0, Geometry.signedPolygon(emptyList()))
    }

    @Test
    fun grahamScanMatchesOfficialHullIndexes() {
        val squareWithInterior = listOf(
            point(0.0, 0.0),
            point(2.0, 0.0),
            point(2.0, 2.0),
            point(0.0, 2.0),
            point(1.0, 1.0),
        )
        assertEquals(
            listOf(0, 1, 2, 3),
            Geometry.GrahamScan(squareWithInterior).map(HullPoint::index),
        )

        val concave = listOf(
            point(0.0, 0.0),
            point(2.0, 0.0),
            point(1.0, 1.0),
            point(2.0, 2.0),
            point(0.0, 2.0),
        )
        assertEquals(
            listOf(0, 1, 3, 4),
            Geometry.GrahamScan(concave).map(HullPoint::index),
        )

        val collinear = listOf(
            point(0.0, 0.0),
            point(1.0, 0.0),
            point(2.0, 0.0),
        )
        assertEquals(
            listOf(0, 2),
            Geometry.GrahamScan(collinear).map(HullPoint::index),
        )
    }

    @Test
    fun convexHullPreservesInputCoordinateObjects() {
        val first = point(0.0, 0.0)
        val second = point(2.0, 0.0)
        val third = point(2.0, 2.0)
        val fourth = point(0.0, 2.0)
        val interior = point(1.0, 1.0)
        val hull = Geometry.convexHull(
            listOf(first, second, third, fourth, interior),
        )

        assertEquals(4, hull.size)
        assertSame(first, hull[0])
        assertSame(second, hull[1])
        assertSame(third, hull[2])
        assertSame(fourth, hull[3])
    }

    @Test
    fun aklToussaintBranchPreservesAllFourExtremeCorners() {
        val points = MutableList(1021) { point(5.0, 5.0) }
        points += point(0.0, 0.0)
        points += point(10.0, 0.0)
        points += point(10.0, 10.0)
        points += point(0.0, 10.0)

        val hullCoordinates = Geometry.GrahamScan(points)
            .map { hullPoint ->
                hullPoint.coordinates[1] to hullPoint.coordinates[2]
            }
            .sortedWith(compareBy<Pair<Double, Double>> { it.first }.thenBy { it.second })

        assertEquals(
            listOf(
                0.0 to 0.0,
                0.0 to 10.0,
                10.0 to 0.0,
                10.0 to 10.0,
            ),
            hullCoordinates,
        )
    }

    @Test
    fun convexityMatchesOfficialReferencePolygons() {
        val square = listOf(
            point(0.0, 0.0),
            point(1.0, 0.0),
            point(1.0, 1.0),
            point(0.0, 1.0),
        )
        val concave = listOf(
            point(0.0, 0.0),
            point(2.0, 0.0),
            point(2.0, 2.0),
            point(1.0, 2.0),
            point(1.0, 1.0),
            point(0.0, 1.0),
        )
        val repeated = listOf(
            point(0.0, 0.0),
            point(1.0, 0.0),
            point(1.0, 0.0),
            point(1.0, 1.0),
            point(0.0, 1.0),
        )
        val crossing = listOf(
            point(0.0, 0.0),
            point(2.0, 2.0),
            point(0.0, 2.0),
            point(2.0, 0.0),
        )

        assertTrue(Geometry.isConvex(square))
        assertFalse(Geometry.isConvex(concave))
        assertTrue(Geometry.isConvex(repeated))
        assertFalse(Geometry.isConvex(crossing))
        assertTrue(Geometry.isConvex(emptyList()))
        assertTrue(Geometry.isConvex(listOf(point(0.0, 0.0))))
        assertTrue(
            Geometry.isConvex(
                listOf(point(0.0, 0.0), point(1.0, 1.0)),
            ),
        )
    }

    @Test
    fun homogeneousCoordinatesRemainUnmodified() {
        val points = listOf(
            point(0.0, 0.0),
            point(2.0, 0.0),
            point(0.0, 2.0),
        )
        val before = points.map(DoubleArray::copyOf)

        Geometry.sortVertices(points)
        Geometry.signedPolygon(points)
        Geometry.GrahamScan(points)
        Geometry.isConvex(points)

        for (index in points.indices) {
            assertContentEquals(before[index], points[index])
        }
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)
}
