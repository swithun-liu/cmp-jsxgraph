package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeometryBezierPathTest {
    private val cubic = DiscreteCurve2D(
        points = listOf(
            point(0.0, 0.0),
            point(1.0, 3.0),
            point(2.0, -1.0),
            point(4.0, 2.0),
            point(5.0, 5.0),
            point(6.0, -2.0),
            point(8.0, 1.0),
        ),
        bezierDegree = 3,
    )
    private val line = DiscreteCurve2D(
        points = listOf(
            point(-1.0, 1.0),
            point(9.0, 1.0),
        ),
        bezierDegree = 1,
    )

    @Test
    fun bezierPathIntersectionsMatchOfficialSegmentTraversal() {
        val expected = listOf(
            point(0.5267170856197547, 1.0),
            point(1.625, 1.0),
            point(3.043260016542931, 1.0),
            point(5.957843131939625, 1.0),
            point(8.0, 1.0),
        )

        for (index in expected.indices) {
            assertPointClose(
                expected[index],
                intersection(
                    Geometry.meetBezierCurveRedBlueSegments(
                        cubic,
                        line,
                        index,
                    ),
                ),
            )
        }
        assertIdealPoint(
            intersection(
                Geometry.meetBezierCurveRedBlueSegments(
                    cubic,
                    line,
                    expected.size,
                ),
            ),
        )
    }

    @Test
    fun bezierPathSwapsLinearAndCubicInputs() {
        assertPointClose(
            point(0.5267170856197547, 1.0),
            intersection(
                Geometry.meetBezierCurveRedBlueSegments(
                    line,
                    cubic,
                    0,
                ),
            ),
        )
    }

    @Test
    fun bezierPathIgnoresSectorLegs() {
        val arc = Geometry.bezierArc(
            first = point(1.0, 0.0),
            center = point(0.0, 0.0),
            third = point(0.0, 1.0),
            withLegs = true,
            sign = 1.0,
        )
        val points = arc.xCoordinates.indices.map { index ->
            point(arc.xCoordinates[index], arc.yCoordinates[index])
        }
        val sector = DiscreteCurve2D(
            points = points,
            bezierDegree = 3,
            isSector = true,
        )
        val plainCurve = sector.copy(isSector = false)
        val horizontal = DiscreteCurve2D(
            points = listOf(point(-1.0, 0.0), point(2.0, 0.0)),
            bezierDegree = 1,
        )

        assertPointClose(
            point(1.0, 0.0),
            intersection(
                Geometry.meetBezierCurveRedBlueSegments(
                    sector,
                    horizontal,
                    0,
                ),
            ),
        )
        assertIdealPoint(
            intersection(
                Geometry.meetBezierCurveRedBlueSegments(
                    sector,
                    horizontal,
                    1,
                ),
            ),
        )
        assertPointClose(
            point(0.0, 0.0),
            intersection(
                Geometry.meetBezierCurveRedBlueSegments(
                    plainCurve,
                    horizontal,
                    1,
                ),
            ),
        )
    }

    @Test
    fun bezierPathReportsInvalidWrapperInputs() {
        assertIs<GMResult.Err<GeometryError.InvalidIntersectionIndex>>(
            Geometry.meetBezierCurveRedBlueSegments(cubic, line, -1),
        )
        assertIs<GMResult.Err<GeometryError.InvalidBezierCurveDegrees>>(
            Geometry.meetBezierCurveRedBlueSegments(
                line,
                line,
                0,
            ),
        )
        assertIdealPoint(
            intersection(
                Geometry.meetBezierCurveRedBlueSegments(
                    DiscreteCurve2D(
                        points = listOf(point(0.0, 0.0)),
                        bezierDegree = 3,
                    ),
                    line,
                    0,
                ),
            ),
        )
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)

    private fun intersection(
        result: GMResult<DoubleArray, GeometryError>,
    ): DoubleArray = assertIs<GMResult.Ok<DoubleArray>>(result).value

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

    private fun assertIdealPoint(point: DoubleArray) {
        assertEquals(0.0, point[0])
        assertTrue(point[1].isNaN())
        assertTrue(point[2].isNaN())
    }
}
