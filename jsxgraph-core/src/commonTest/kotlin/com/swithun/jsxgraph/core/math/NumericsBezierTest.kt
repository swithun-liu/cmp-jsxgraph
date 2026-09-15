package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumericsBezierTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun cubicBezierSegmentsMatchOfficialReferenceValues() {
        val bezier = Numerics.bezier(referencePoints())

        assertEquals(0.0, bezier.start)
        assertEquals(2.0, bezier.end())
        assertPoint(bezier, -1.0, 0.0, 0.0)
        assertPoint(bezier, 0.0, 0.0, 0.0)
        assertPoint(bezier, 0.25, 0.765625, 1.15625)
        assertPoint(bezier, 0.5, 1.625, 1.0)
        assertPoint(bezier, 0.999, 3.994002999, 1.991020986)
        assertPoint(bezier, 1.0, 4.0, 2.0)
        assertPoint(bezier, 1.5, 6.0, 2.25)
        assertPoint(
            bezier,
            1.999,
            7.996997002000001,
            0.9970179860000004,
        )
        assertPoint(bezier, 2.0, 8.0, 1.0)
        assertPoint(bezier, 3.0, 8.0, 1.0)
        assertTrue(bezier.x(Double.NaN).isNaN())
        assertTrue(bezier.y(Double.NaN).isNaN())
    }

    @Test
    fun controlPointCoordinatesRemainDynamicDuringSuspendedUpdates() {
        val points = referencePoints()
        val bezier = Numerics.bezier(points)

        bezier.x(0.5)
        points[1].moveTo(2.0, 6.0)

        assertEquals(
            2.0,
            bezier.x(0.5, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            2.125,
            bezier.y(0.5, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
    }

    @Test
    fun incompleteTrailingControlPointsAreIgnoredLikeOfficial() {
        val points = referencePoints().toMutableList()
        points += point(100.0, 100.0)
        points += point(200.0, 200.0)
        val bezier = Numerics.bezier(points)

        assertEquals(3.0, bezier.end())
        assertPoint(bezier, 2.0, 8.0, 1.0)
        assertPoint(bezier, 5.0, 8.0, 1.0)
    }

    @Test
    fun shortPointListsPreserveAvailableEndpointBehavior() {
        val empty = Numerics.bezier(emptyList())
        assertTrue(empty.x(-1.0).isNaN())
        assertTrue(empty.y(0.0).isNaN())
        assertEquals(0.0, empty.end())

        for (count in 1..3) {
            val points = List(count) { index ->
                point(index.toDouble(), (index * index).toDouble())
            }
            val bezier = Numerics.bezier(points)
            assertPoint(bezier, -1.0, 0.0, 0.0)
            assertPoint(bezier, 0.0, 0.0, 0.0)
            assertPoint(bezier, 0.5, 0.0, 0.0)
        }

        val complete = Numerics.bezier(
            points(
                0.0 to 0.0,
                1.0 to 1.0,
                2.0 to 4.0,
                3.0 to 9.0,
            ),
        )
        assertPoint(complete, 0.5, 1.5, 3.0)
        assertPoint(complete, 1.0, 3.0, 9.0)
    }

    @Test
    fun suspendedEvaluationUsesSharedLengthCache() {
        val bezier = Numerics.bezier(
            points(
                0.0 to 0.0,
                1.0 to 1.0,
                2.0 to 2.0,
                3.0 to 3.0,
            ),
        )

        assertPoint(bezier, 0.5, 1.5, 1.5, suspendedUpdate = true)
        assertTrue(bezier.x(1.0, suspendedUpdate = true).isNaN())
        assertTrue(bezier.y(1.0, suspendedUpdate = true).isNaN())

        assertEquals(3.0, bezier.x(1.0))
        assertEquals(3.0, bezier.y(1.0, suspendedUpdate = true))
    }

    private fun referencePoints(): List<CoordsElement> = points(
        0.0 to 0.0,
        1.0 to 3.0,
        2.0 to -1.0,
        4.0 to 2.0,
        5.0 to 5.0,
        7.0 to 0.0,
        8.0 to 1.0,
    )

    private fun points(vararg coordinates: Pair<Double, Double>): List<CoordsElement> =
        coordinates.map { (x, y) -> point(x, y) }

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

    private fun assertPoint(
        bezier: BezierInterpolation,
        parameter: Double,
        expectedX: Double,
        expectedY: Double,
        suspendedUpdate: Boolean = false,
    ) {
        assertEquals(
            expectedX,
            bezier.x(parameter, suspendedUpdate),
            absoluteTolerance = 1.0e-13,
        )
        assertEquals(
            expectedY,
            bezier.y(parameter, suspendedUpdate),
            absoluteTolerance = 1.0e-13,
        )
    }
}
