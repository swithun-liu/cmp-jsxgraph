package com.swithun.jsxgraph.core.base

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoordsElementTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun coordinateAccessorsMatchOfficialBehavior() {
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, -3.0),
        )

        assertEquals(2.0, element.X())
        assertEquals(-3.0, element.Y())
        assertEquals(1.0, element.Z())
        assertEquals(element.X(), element.XEval())
        assertEquals(element.Y(), element.YEval())
        assertEquals(element.Z(), element.ZEval())
        assertContentEquals(doubleArrayOf(2.0, -3.0), element.Coords())
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, -3.0),
            element.Coords(withZ = true),
        )
    }

    @Test
    fun distanceUsesUserCoordinatesAndRejectsIdealPoints() {
        val first = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
        )
        val second = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(4.0, 5.0),
        )
        val ideal = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(0.0, 1.0, 1.0),
        )

        assertEquals(5.0, first.Dist(second))
        assertEquals(5.0, second.Dist(first))
        assertTrue(first.Dist(ideal).isNaN())
        assertTrue(ideal.Dist(first).isNaN())
    }

    @Test
    fun coordinateCopiesDoNotExposeInternalArrays() {
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, 3.0),
        )

        val coordinates = element.Coords(withZ = true)
        coordinates[1] = 100.0

        assertEquals(2.0, element.X())
        assertEquals(3.0, element.Y())
    }
}
