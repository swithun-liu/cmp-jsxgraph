package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeometryReuleauxTest {
    private val board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
    )

    @Test
    fun reuleauxTriangleMatchesOfficialPeriodicReferences() {
        val interpolation = interpolation(equilateralTriangle())
        val references = listOf(
            Triple(-PI, 1.2679491924311241, 0.7320508075688748),
            Triple(0.0, -1.7320508075688787, -0.9999999999999991),
            Triple(
                PI / 3.0,
                -6.363451432349441e-16,
                -1.4641016151377544,
            ),
            Triple(
                2.0 * PI / 3.0,
                1.732050807568875,
                -1.0000000000000013,
            ),
            Triple(PI, 1.2679491924311241, 0.7320508075688748),
            Triple(
                2.0 * PI,
                -1.7320508075688787,
                -0.9999999999999991,
            ),
        )

        assertEquals(0.0, interpolation.start)
        assertEquals(2.0 * PI, interpolation.end)
        for ((parameter, expectedX, expectedY) in references) {
            assertEquals(
                expectedX,
                interpolation.x(parameter, suspendedUpdate = false),
                absoluteTolerance = 1.0e-12,
            )
            assertEquals(
                expectedY,
                interpolation.y(parameter, suspendedUpdate = true),
                absoluteTolerance = 1.0e-12,
            )
        }
        assertTrue(interpolation.x(Double.NaN).isNaN())
        assertTrue(interpolation.y(Double.POSITIVE_INFINITY).isNaN())
    }

    @Test
    fun reuleauxCoordinatesShareDynamicCache() {
        val points = equilateralTriangle()
        val interpolation = interpolation(points)

        assertTrue(interpolation.x(0.0, suspendedUpdate = true).isNaN())
        assertTrue(interpolation.y(0.0, suspendedUpdate = true).isNaN())

        assertEquals(
            -1.7320508075688787,
            interpolation.x(0.0, suspendedUpdate = false),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -0.9999999999999991,
            interpolation.y(0.0, suspendedUpdate = true),
            absoluteTolerance = 1.0e-12,
        )

        points[1].moveTo(-2.0, -2.0)
        assertEquals(
            -1.7320508075688787,
            interpolation.x(0.0, suspendedUpdate = true),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -2.000000000000001,
            interpolation.x(0.0, suspendedUpdate = false),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -1.9999999999999996,
            interpolation.y(0.0, suspendedUpdate = true),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun reuleauxConstructionReportsInvalidTopology() {
        val points = equilateralTriangle()
        for (vertexCount in listOf(-1, 0, 2, 4)) {
            assertIs<GMResult.Err<GeometryError.InvalidReuleauxVertexCount>>(
                Geometry.reuleauxPolygon(points, vertexCount),
            )
        }
        assertIs<GMResult.Err<GeometryError.InvalidReuleauxPointCount>>(
            Geometry.reuleauxPolygon(points.take(2), vertexCount = 3),
        )
    }

    private fun equilateralTriangle(): List<CoordsElement> = listOf(
        point(0.0, 2.0),
        point(-sqrt(3.0), -1.0),
        point(sqrt(3.0), -1.0),
    )

    private fun point(
        x: Double,
        y: Double,
    ): CoordsElement = CoordsElement(
        board = board,
        coordinates = doubleArrayOf(x, y),
    )

    private fun CoordsElement.moveTo(
        x: Double,
        y: Double,
    ) {
        coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(x, y),
        )
    }

    private fun interpolation(
        points: List<CoordsElement>,
    ): ReuleauxPolygonInterpolation =
        assertIs<GMResult.Ok<ReuleauxPolygonInterpolation>>(
            Geometry.reuleauxPolygon(points, vertexCount = 3),
        ).value
}
