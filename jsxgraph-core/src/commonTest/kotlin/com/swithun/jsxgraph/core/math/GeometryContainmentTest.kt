package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeometryContainmentTest {
    private val square = listOf(
        point(0.0, 0.0),
        point(2.0, 0.0),
        point(2.0, 2.0),
        point(0.0, 2.0),
    )

    @Test
    fun windingNumberMatchesOfficialInsideOutsideAndBoundaryRules() {
        assertEquals(1, Geometry.windingNumber(point(1.0, 1.0), square))
        assertEquals(0, Geometry.windingNumber(point(3.0, 1.0), square))
        assertEquals(1, Geometry.windingNumber(point(0.0, 0.0), square))
        assertEquals(0, Geometry.windingNumber(point(1.0, 0.0), square))
        assertEquals(0, Geometry.windingNumber(point(2.0, 1.0), square))
        assertEquals(0, Geometry.windingNumber(point(0.0, 1.0), square))
        assertEquals(
            1,
            Geometry.windingNumber(
                point(Double.NaN, 1.0),
                square,
            ),
        )
    }

    @Test
    fun openPathModeOmitsTheClosingEdge() {
        assertEquals(
            0,
            Geometry.windingNumber(
                point(0.0, 1.0),
                square,
                doNotClosePath = false,
            ),
        )
        assertEquals(
            1,
            Geometry.windingNumber(
                point(0.0, 1.0),
                square,
                doNotClosePath = true,
            ),
        )
    }

    @Test
    fun windingNumberSupportsNaNSeparatedHoleSubpaths() {
        val path = listOf(
            point(0.0, 0.0),
            point(4.0, 0.0),
            point(4.0, 4.0),
            point(0.0, 4.0),
            point(0.0, 0.0),
            point(Double.NaN, Double.NaN),
            point(1.0, 1.0),
            point(1.0, 3.0),
            point(3.0, 3.0),
            point(3.0, 1.0),
            point(1.0, 1.0),
        )

        assertEquals(
            1,
            Geometry.windingNumber(
                point(0.5, 0.5),
                path,
                doNotClosePath = true,
            ),
        )
        assertEquals(
            0,
            Geometry.windingNumber(
                point(2.0, 2.0),
                path,
                doNotClosePath = true,
            ),
        )
        assertEquals(
            0,
            Geometry.windingNumber(
                point(5.0, 5.0),
                path,
                doNotClosePath = true,
            ),
        )
    }

    @Test
    fun idealAndUndefinedPathEdgesAreIgnored() {
        val path = listOf(
            point(0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            point(Double.NaN, Double.NaN),
            point(0.0, 2.0),
        )

        assertEquals(0, Geometry.windingNumber(point(1.0, 1.0), path))
        assertEquals(0, Geometry.windingNumber(point(1.0, 1.0), emptyList()))
    }

    @Test
    fun pnpolyMatchesOfficialScreenCoordinateBoundaryBehavior() {
        val closedSquare = square + point(0.0, 0.0)

        assertTrue(Geometry.pnpoly(1.0, 1.0, closedSquare))
        assertFalse(Geometry.pnpoly(3.0, 1.0, closedSquare))
        assertTrue(Geometry.pnpoly(0.0, 0.0, closedSquare))
        assertTrue(Geometry.pnpoly(1.0, 0.0, closedSquare))
        assertFalse(Geometry.pnpoly(2.0, 1.0, closedSquare))
        assertTrue(Geometry.pnpoly(0.0, 1.0, closedSquare))
        assertFalse(Geometry.pnpoly(1.0, 1.0, emptyList()))
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)
}
