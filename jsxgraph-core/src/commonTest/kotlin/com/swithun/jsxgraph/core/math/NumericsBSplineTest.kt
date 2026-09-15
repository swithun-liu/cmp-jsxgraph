package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumericsBSplineTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun quadraticBSplineMatchesOfficialReferenceValues() {
        val spline = Numerics.bspline(referencePoints(), order = 3)

        assertEquals(0.0, spline.start)
        assertEquals(5.0, spline.end())
        assertPoint(spline, -1.0, 0.0, 0.0)
        assertPoint(spline, 0.0, 0.0, 0.0)
        assertPoint(spline, 0.25, 0.46875, 1.1875)
        assertPoint(spline, 0.5, 0.875, 1.75)
        assertPoint(spline, 1.0, 1.5, 1.0)
        assertPoint(spline, 1.5, 2.125, -0.125)
        assertPoint(spline, 2.0, 3.0, 0.5)
        assertPoint(spline, 2.5, 3.875, 2.0)
        assertPoint(spline, 3.0, 4.5, 3.5)
        assertPoint(spline, 4.0, 7.0, 0.0)
        assertPoint(spline, 6.0, 7.0, 0.0)
        assertPoint(spline, Double.NaN, 0.0, 0.0)
    }

    @Test
    fun linearAndCubicOrdersMatchOfficialReferenceValues() {
        val linear = Numerics.bspline(referencePoints(), order = 2)
        assertPoint(linear, 0.25, 0.25, 0.75)
        assertPoint(linear, 1.5, 1.5, 1.0)
        assertPoint(linear, 2.5, 3.0, 0.5)
        assertPoint(linear, 4.0, 5.0, 5.0)

        val cubic = Numerics.bspline(referencePoints(), order = 4)
        assertPoint(cubic, 0.25, 0.6653645833333333, 1.4140625)
        assertPoint(cubic, 0.5, 1.1979166666666667, 1.5625)
        assertPoint(cubic, 1.0, 2.083333333333333, 0.5)
        assertPoint(cubic, 1.5, 3.0, 0.71875)
        assertPoint(cubic, 2.5, 4.927083333333334, 3.46875)
        assertPoint(cubic, 3.0, 7.0, 0.0)
    }

    @Test
    fun excessiveOrderIsReducedToPointCount() {
        val maximum = Numerics.bspline(referencePoints(), order = 6)
        val excessive = Numerics.bspline(referencePoints(), order = 7)

        assertPoint(maximum, 0.25, 1.3544921875, 1.171875)
        assertPoint(maximum, 0.5, 3.03125, 1.5625)
        assertPoint(maximum, 1.0, 7.0, 0.0)
        assertPoint(excessive, 0.25, 1.3544921875, 1.171875)
        assertPoint(excessive, 0.5, 3.03125, 1.5625)
        assertPoint(excessive, 1.0, 7.0, 0.0)
    }

    @Test
    fun coordinatesRemainDynamicAndSuspendedUpdateDoesNotCache() {
        val points = referencePoints()
        val spline = Numerics.bspline(points, order = 3)

        assertPoint(spline, 1.5, 2.125, -0.125)
        points[2].moveTo(10.0, 20.0)
        assertPoint(
            spline,
            1.5,
            8.125,
            15.625,
            suspendedUpdate = true,
        )
    }

    @Test
    fun emptySingleAndNonPositiveOrdersMatchOfficialNumericBehavior() {
        val empty = Numerics.bspline(emptyList(), order = 3)
        assertTrue(empty.x(0.0).isNaN())
        assertTrue(empty.y(0.0).isNaN())
        assertEquals(-1.0, empty.end())

        val single = Numerics.bspline(points(2.0 to 3.0), order = 3)
        assertTrue(single.x(0.0).isNaN())
        assertTrue(single.y(0.0).isNaN())
        assertEquals(0.0, single.end())

        val zeroOrder = Numerics.bspline(referencePoints(), order = 0)
        assertPoint(zeroOrder, -1.0, 0.0, 0.0)
        assertPoint(zeroOrder, 0.25, 0.0, 0.0)
        assertPoint(zeroOrder, 6.0, 0.0, 0.0)
        assertPoint(zeroOrder, 7.0, 7.0, 0.0)

        val negativeOrder = Numerics.bspline(referencePoints(), order = -1)
        assertPoint(negativeOrder, 2.0, 0.0, 0.0)
        assertPoint(negativeOrder, 8.0, 7.0, 0.0)
    }

    private fun referencePoints(): List<CoordsElement> = points(
        0.0 to 0.0,
        1.0 to 3.0,
        2.0 to -1.0,
        4.0 to 2.0,
        5.0 to 5.0,
        7.0 to 0.0,
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
        spline: BSplineInterpolation,
        parameter: Double,
        expectedX: Double,
        expectedY: Double,
        suspendedUpdate: Boolean = false,
    ) {
        assertEquals(
            expectedX,
            spline.x(parameter, suspendedUpdate),
            absoluteTolerance = 1.0e-13,
        )
        assertEquals(
            expectedY,
            spline.y(parameter, suspendedUpdate),
            absoluteTolerance = 1.0e-13,
        )
    }
}
