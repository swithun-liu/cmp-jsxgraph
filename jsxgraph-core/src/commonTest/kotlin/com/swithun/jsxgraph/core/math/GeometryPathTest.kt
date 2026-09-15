package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class GeometryPathTest {
    @Test
    fun polylineIntersectionsMatchOfficialTraversalOrder() {
        val red = listOf(
            point(0.0, 0.0),
            point(2.0, 2.0),
            point(4.0, 0.0),
        )
        val blue = listOf(
            point(-1.0, 1.0),
            point(5.0, 1.0),
        )

        assertContentEquals(
            point(1.0, 1.0),
            Geometry.meetCurveRedBlueSegments(red, blue, 0),
        )
        assertContentEquals(
            point(3.0, 1.0),
            Geometry.meetCurveRedBlueSegments(red, blue, 1),
        )
        assertIdealPoint(
            Geometry.meetCurveRedBlueSegments(red, blue, 2),
        )
        assertIdealPoint(
            Geometry.meetCurveRedBlueSegments(red, blue, -1),
        )
    }

    @Test
    fun polylineIntersectionCountsSharedVertexOnce() {
        val red = listOf(
            point(0.0, 0.0),
            point(1.0, 1.0),
            point(2.0, 0.0),
        )
        val blue = listOf(
            point(0.0, 1.0),
            point(1.0, 1.0),
            point(2.0, 1.0),
        )

        assertContentEquals(
            point(1.0, 1.0),
            Geometry.meetCurveRedBlueSegments(red, blue, 0),
        )
        assertIdealPoint(
            Geometry.meetCurveRedBlueSegments(red, blue, 1),
        )
        assertIdealPoint(
            Geometry.meetCurveRedBlueSegments(
                red = listOf(point(0.0, 0.0)),
                blue = blue,
                intersectionIndex = 0,
            ),
        )
    }

    @Test
    fun polygonProjectionMatchesOfficialClosedAndOpenPathReferences() {
        val closedSquare = listOf(
            point(0.0, 0.0),
            point(4.0, 0.0),
            point(4.0, 3.0),
            point(0.0, 3.0),
            point(0.0, 0.0),
        )
        val inside = projection(
            Geometry.projectCoordsToPolygon(
                point = point(2.0, 2.0),
                vertices = closedSquare,
            ),
        )
        assertContentEquals(point(2.0, 3.0), inside)

        val outside = projection(
            Geometry.projectCoordsToPolygon(
                point = point(5.0, 4.0),
                vertices = closedSquare,
            ),
        )
        assertContentEquals(point(4.0, 3.0), outside)
        assertNotSame(closedSquare[2], outside)

        assertContentEquals(
            point(0.0, 0.0),
            projection(
                Geometry.projectCoordsToPolygon(
                    point = point(-1.0, 1.0),
                    vertices = closedSquare.dropLast(1),
                ),
            ),
        )
    }

    @Test
    fun polygonProjectionReportsInvalidOrUndefinedInput() {
        assertIs<
            GMResult.Err<GeometryError.InvalidPolygonPointCount>,
        >(
            Geometry.projectCoordsToPolygon(
                point = point(0.0, 0.0),
                vertices = emptyList(),
            ),
        )
        assertIs<GMResult.Err<GeometryError.PolygonProjectionUnavailable>>(
            Geometry.projectCoordsToPolygon(
                point = point(0.0, 0.0),
                vertices = listOf(
                    point(Double.NaN, 0.0),
                    point(Double.NaN, 1.0),
                ),
            ),
        )
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)

    private fun projection(
        result: GMResult<DoubleArray, GeometryError>,
    ): DoubleArray = assertIs<GMResult.Ok<DoubleArray>>(result).value

    private fun assertIdealPoint(point: DoubleArray) {
        assertEquals(0.0, point[0])
        assertTrue(point[1].isNaN())
        assertTrue(point[2].isNaN())
    }
}
