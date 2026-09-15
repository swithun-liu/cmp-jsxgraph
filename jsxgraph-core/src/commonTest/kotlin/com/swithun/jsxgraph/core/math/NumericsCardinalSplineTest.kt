package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumericsCardinalSplineTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun cubicPolynomialCoefficientsMatchOfficialReference() {
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 8.0, -7.0),
            Numerics.initCubicPoly(
                first = 1.0,
                second = 4.0,
                firstTangent = 2.0,
                secondTangent = -3.0,
            ),
        )
    }

    @Test
    fun uniformCardinalSplineMatchesOfficialReferenceValues() {
        val spline = Numerics.CardinalSpline(
            points = referencePoints(),
            tension = 0.25,
        )

        assertEquals(0.0, spline.start)
        assertEquals(3.0, spline.end())
        assertPoint(spline, -1.0, 0.0, 0.0)
        assertPoint(spline, 0.25, 0.19140625, 0.41796875)
        assertPoint(spline, 1.0, 1.0, 2.0)
        assertPoint(spline, 1.5, 2.0, 2.6875)
        assertPoint(spline, 2.75, 3.80859375, -0.12890625)
        assertPoint(spline, 3.0, 4.0, -1.0)
        assertTrue(spline.x(3.25).isNaN())
        assertTrue(spline.y(3.25).isNaN())
        assertPoint(spline, 4.0, 4.0, -1.0)
        assertTrue(spline.x(Double.NaN).isNaN())
        assertTrue(spline.y(Double.NaN).isNaN())
    }

    @Test
    fun catmullRomUniformAndCentripetalModesMatchOfficialReferenceValues() {
        val uniform = Numerics.CatmullRomSpline(referencePoints())
        assertPoint(uniform, 0.25, 0.2265625, 0.5234375)
        assertPoint(uniform, 1.5, 2.0, 2.875)
        assertPoint(uniform, 2.75, 3.7734375, 0.1171875)

        val centripetal = Numerics.CatmullRomSpline(
            points = referencePoints(),
            type = "centripetal",
        )
        assertPoint(centripetal, 0.25, 0.19140625, 0.41796875)
        assertPoint(
            centripetal,
            1.5,
            2.002242959438152,
            2.6358372836063104,
        )
        assertPoint(
            centripetal,
            2.75,
            3.8200342371409763,
            -0.11518146668430918,
        )
    }

    @Test
    fun dynamicTensionAndPerCoordinateCachesMatchOfficialBehavior() {
        val points = referencePoints()
        var tension = 0.25
        var tensionEvaluationCount = 0
        val spline = Numerics.CardinalSpline(
            points = points,
            tension = {
                tensionEvaluationCount += 1
                tension
            },
            type = "centripetal",
        )

        assertEquals(0.484375, spline.x(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(1, tensionEvaluationCount)
        assertEquals(1.015625, spline.y(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(2, tensionEvaluationCount)
        assertEquals(
            0.802734375,
            spline.x(0.75, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            1.658203125,
            spline.y(0.75, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(2, tensionEvaluationCount)

        points[1].moveTo(2.0, 5.0)
        tension = 0.75
        assertEquals(
            0.484375,
            spline.x(0.5, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            1.015625,
            spline.y(0.5, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            1.0255485928873647,
            spline.x(0.5),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(3, tensionEvaluationCount)
        assertEquals(
            1.015625,
            spline.y(0.5, suspendedUpdate = true),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            2.9620136994363304,
            spline.y(0.5),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(4, tensionEvaluationCount)
    }

    @Test
    fun duplicatePointsUseCentripetalDistanceFallbacks() {
        val spline = Numerics.CardinalSpline(
            points = points(
                1.0 to 2.0,
                1.0 to 2.0,
                3.0 to 4.0,
            ),
            tension = 0.5,
            type = "centripetal",
        )

        assertPoint(
            spline,
            0.25,
            0.9896069370307763,
            1.9896069370307763,
        )
        assertPoint(spline, 1.0, 1.0, 2.0)
        assertPoint(spline, 1.5, 1.9375, 2.9375)
        assertPoint(spline, 3.0, 3.0, 4.0)
    }

    @Test
    fun shortInputsAndSuspendedCachesHaveStableNumericResults() {
        val empty = Numerics.CatmullRomSpline(emptyList())
        assertTrue(empty.x(0.0).isNaN())
        assertTrue(empty.y(0.0).isNaN())
        assertEquals(-1.0, empty.end())

        val single = Numerics.CatmullRomSpline(points(1.0 to 2.0))
        assertTrue(single.x(0.0).isNaN())
        assertTrue(single.y(0.0).isNaN())
        assertEquals(0.0, single.end())

        val suspended = Numerics.CatmullRomSpline(referencePoints())
        assertEquals(0.0, suspended.x(0.0, suspendedUpdate = true))
        assertEquals(1.0, suspended.x(1.0, suspendedUpdate = true))
        assertTrue(suspended.x(0.5, suspendedUpdate = true).isNaN())
        assertTrue(suspended.y(0.5, suspendedUpdate = true).isNaN())
    }

    @Test
    fun unknownParameterizationFallsBackToUniform() {
        val spline = Numerics.CardinalSpline(
            points = points(
                0.0 to 0.0,
                2.0 to 5.0,
                3.0 to 3.0,
                4.0 to -1.0,
            ),
            tension = 0.5,
            type = "CENTRIPETAL",
        )

        assertEquals(1.0625, spline.x(0.5), absoluteTolerance = 1.0e-14)
    }

    private fun referencePoints(): List<CoordsElement> = points(
        0.0 to 0.0,
        1.0 to 2.0,
        3.0 to 3.0,
        4.0 to -1.0,
    )

    private fun points(vararg coordinates: Pair<Double, Double>): List<CoordsElement> =
        coordinates.map { (x, y) ->
            CoordsElement(
                board = board,
                coordinates = doubleArrayOf(x, y),
            )
        }

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
        spline: CardinalSplineInterpolation,
        parameter: Double,
        expectedX: Double,
        expectedY: Double,
    ) {
        assertEquals(
            expectedX,
            spline.x(parameter),
            absoluteTolerance = 1.0e-13,
        )
        assertEquals(
            expectedY,
            spline.y(parameter),
            absoluteTolerance = 1.0e-13,
        )
    }
}
