package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GeometryProjectionTest {
    @Test
    fun circleProjectionMatchesOfficialReferences() {
        assertPointClose(
            expected = point(1.2, 1.6),
            actual = Geometry.projectPointToCircle(
                point = point(3.0, 4.0),
                center = point(0.0, 0.0),
                radius = 2.0,
            ),
        )
        assertContentEquals(
            point(4.0, 6.0),
            Geometry.projectPointToCircle(
                point = point(4.0, 6.0),
                center = point(1.0, 2.0),
                radius = 5.0,
            ),
        )
    }

    @Test
    fun circleProjectionPreservesCenterAndIdealPointRules() {
        assertContentEquals(
            point(1.0, 2.0),
            Geometry.projectPointToCircle(
                point = point(1.0, 2.0),
                center = point(1.0, 2.0),
                radius = 5.0,
            ),
        )
        assertContentEquals(
            point(0.0, 0.0),
            Geometry.projectPointToCircle(
                point = doubleArrayOf(0.0, 3.0, 4.0),
                center = point(0.0, 0.0),
                radius = 2.0,
            ),
        )
    }

    @Test
    fun lineProjectionMatchesOfficialStandardFormReferences() {
        assertContentEquals(
            point(3.0, 2.0),
            Geometry.projectPointToLine(
                point = point(3.0, 5.0),
                line = doubleArrayOf(-2.0, 0.0, 1.0),
            ),
        )
        assertContentEquals(
            point(-1.0, 5.0),
            Geometry.projectPointToLine(
                point = point(3.0, 5.0),
                line = doubleArrayOf(1.0, 1.0, 0.0),
            ),
        )
        assertContentEquals(
            point(1.0, 1.0),
            Geometry.projectPointToLine(
                point = point(2.0, 0.0),
                line = doubleArrayOf(0.0, 1.0, -1.0),
            ),
        )
    }

    @Test
    fun lineProjectionDoesNotRequireNormalizedStandardForm() {
        assertContentEquals(
            point(3.0, 2.0),
            Geometry.projectPointToLine(
                point = point(3.0, 5.0),
                line = doubleArrayOf(-20.0, 0.0, 10.0),
            ),
        )
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)

    private fun assertPointClose(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                absoluteTolerance = 1.0e-12,
            )
        }
    }
}
