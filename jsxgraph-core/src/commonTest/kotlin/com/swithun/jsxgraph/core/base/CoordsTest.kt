package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.utils.EventHandler
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoordsTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun userCoordinatesAreConvertedToScreenCoordinates() {
        val coordinates = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, -3.0),
            board = board,
        )

        assertContentEquals(doubleArrayOf(1.0, 2.0, -3.0), coordinates.usrCoords)
        assertContentEquals(doubleArrayOf(1.0, 350.0, 320.0), coordinates.scrCoords)
    }

    @Test
    fun homogeneousUserCoordinatesAreNormalizedBeforeConversion() {
        val coordinates = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 4.0, 6.0),
            board = board,
        )

        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0), coordinates.usrCoords)
        assertContentEquals(doubleArrayOf(1.0, 350.0, 80.0), coordinates.scrCoords)
    }

    @Test
    fun screenCoordinatesAreConvertedToUserCoordinates() {
        val coordinates = Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = doubleArrayOf(350.0, 80.0),
            board = board,
        )

        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0), coordinates.usrCoords)
        assertTrue(coordinates.scrCoords[0].isNaN())
        assertEquals(350.0, coordinates.scrCoords[1])
        assertEquals(80.0, coordinates.scrCoords[2])
    }

    @Test
    fun roundedConversionMatchesJavaScriptMathRoundAtNegativeHalf() {
        val zeroOriginBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val coordinates = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 0.0),
            board = zeroOriginBoard,
        )

        coordinates.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-0.5, -0.5),
            doRound = true,
        )

        assertEquals((-0.0).toBits(), coordinates.scrCoords[1].toBits())
        assertEquals(1.0, coordinates.scrCoords[2])
    }

    @Test
    fun userDistanceIsInfiniteWhenHomogeneousWeightsDiffer() {
        val finite = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, 1.0),
            board = board,
        )
        val pointAtInfinity = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 1.0, 1.0),
            board = board,
        )

        assertEquals(
            Double.POSITIVE_INFINITY,
            finite.distance(Const.COORDS_BY_USER, pointAtInfinity),
        )
    }

    @Test
    fun updateEventReceivesPreviousCoordinatesAndBlocksReentry() {
        val coordinates = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, 2.0),
            board = board,
        )
        var eventCount = 0
        var previousUserCoordinates: DoubleArray? = null
        var previousScreenCoordinates: DoubleArray? = null
        val handler = EventHandler { _, arguments ->
            eventCount += 1
            previousUserCoordinates = arguments.getOrNull(0) as? DoubleArray
            previousScreenCoordinates = arguments.getOrNull(1) as? DoubleArray
            coordinates.setCoordinates(
                coordType = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(3.0, 4.0),
            )
        }
        coordinates.on(Coords.UPDATE_EVENT, handler)

        coordinates.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 3.0),
        )

        assertEquals(1, eventCount)
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 2.0),
            assertNotNull(previousUserCoordinates),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 300.0, 120.0),
            assertNotNull(previousScreenCoordinates),
        )
        assertContentEquals(doubleArrayOf(1.0, 3.0, 4.0), coordinates.usrCoords)
    }

    @Test
    fun copyUsesJavaScriptSliceOffsetSemantics() {
        val coordinates = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 3.0),
            board = board,
        )

        assertContentEquals(
            doubleArrayOf(2.0, 3.0),
            coordinates.copy(CoordinateArray.USER, offset = 1),
        )
        assertContentEquals(
            doubleArrayOf(3.0),
            coordinates.copy(CoordinateArray.USER, offset = -1),
        )
        assertContentEquals(
            doubleArrayOf(),
            coordinates.copy(CoordinateArray.USER, offset = 20),
        )
    }

    @Test
    fun isRealMatchesUpstreamNaNAndHomogeneousWeightChecks() {
        val real = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 3.0),
            board = board,
        )
        val notReal = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 1.0, 1.0),
            board = board,
        )

        assertTrue(real.isReal())
        assertTrue(!notReal.isReal())
    }
}
