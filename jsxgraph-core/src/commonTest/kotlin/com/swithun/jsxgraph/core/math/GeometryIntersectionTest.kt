package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeometryIntersectionTest {
    @Test
    fun discreteCurveLinePreservesTraversalAndIndexSemantics() {
        val curve = DiscreteCurve2D(
            points = listOf(
                point(-4.0, 3.0),
                point(-2.0, -1.0),
                point(0.0, 3.0),
                point(2.0, -1.0),
                point(4.0, 3.0),
            ),
            bezierDegree = 1,
        )
        val expected = listOf(-2.5, -1.5, 1.5, 2.5)
        for ((index, x) in expected.withIndex()) {
            assertPointClose(
                point(x, 0.0),
                intersection(
                    Geometry.meetCurveLineDiscrete(
                        curve = curve,
                        lineFirst = point(-7.0, 0.0),
                        lineSecond = point(7.0, 0.0),
                        lineStandardForm =
                            doubleArrayOf(0.0, 0.0, 1.0),
                        straightFirst = true,
                        straightLast = true,
                        intersectionIndex = index.toDouble(),
                        testSegment = false,
                    ),
                ),
            )
        }
        for (index in listOf(4.0, -1.0, 1.5)) {
            assertNonReal(
                intersection(
                    Geometry.meetCurveLineDiscrete(
                        curve = curve,
                        lineFirst = point(-7.0, 0.0),
                        lineSecond = point(7.0, 0.0),
                        lineStandardForm =
                            doubleArrayOf(0.0, 0.0, 1.0),
                        straightFirst = true,
                        straightLast = true,
                        intersectionIndex = index,
                        testSegment = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun continuousCurveCurveUsesOfficialGridTraversal() {
        val parabola = ContinuousCurve2D(
            curve = ParametricCurve2D(
                x = { parameter -> parameter },
                y = { parameter -> parameter * parameter - 1.0 },
            ),
            minimumParameter = -3.0,
            maximumParameter = 3.0,
            type = ContinuousCurveType.FUNCTION_GRAPH,
        )
        val horizontal = ContinuousCurve2D(
            curve = ParametricCurve2D(
                x = { parameter -> parameter },
                y = { 0.0 },
            ),
            minimumParameter = -3.0,
            maximumParameter = 3.0,
            type = ContinuousCurveType.PARAMETER,
        )

        assertPointClose(
            point(-1.0, 0.0),
            intersection(
                Geometry.meetCurveCurveContinuous(
                    first = parabola,
                    second = horizontal,
                    intersectionIndex = 0.0,
                    secondInitialParameter = 0.0,
                ),
            ),
            tolerance = 1.0e-4,
        )
        assertPointClose(
            point(1.0, 0.0),
            intersection(
                Geometry.meetCurveCurveContinuous(
                    first = parabola,
                    second = horizontal,
                    intersectionIndex = 1.0,
                    secondInitialParameter = 0.0,
                ),
            ),
            tolerance = 1.0e-4,
        )
        assertNonReal(
            intersection(
                Geometry.meetCurveCurveContinuous(
                    first = parabola,
                    second = horizontal,
                    intersectionIndex = 2.0,
                    secondInitialParameter = 0.0,
                ),
            ),
        )
    }

    @Test
    fun polygonLineMatchesBorderOrderAndSegmentClipping() {
        val borders = listOf(
            point(2.0, -2.0) to point(2.0, 2.0),
            point(2.0, 2.0) to point(-2.0, 2.0),
            point(-2.0, 2.0) to point(-2.0, -2.0),
            point(-2.0, -2.0) to point(2.0, -2.0),
        )
        assertPointClose(
            point(2.0, 0.0),
            Geometry.meetPolygonLine(
                borders = borders,
                lineFirst = point(-7.0, 0.0),
                lineSecond = point(7.0, 0.0),
                intersectionIndex = 0.0,
                testLineSegment = false,
            ),
        )
        assertPointClose(
            point(-2.0, 0.0),
            Geometry.meetPolygonLine(
                borders = borders,
                lineFirst = point(-7.0, 0.0),
                lineSecond = point(7.0, 0.0),
                intersectionIndex = 1.0,
                testLineSegment = false,
            ),
        )
        assertEquals(
            listOf(0.0, 0.0, 0.0),
            Geometry.meetPolygonLine(
                borders = borders,
                lineFirst = point(-1.0, 0.0),
                lineSecond = point(1.0, 0.0),
                intersectionIndex = 0.0,
                testLineSegment = true,
            ).toList(),
        )
    }

    @Test
    fun continuousIntersectionRejectsInvalidDomainStructurally() {
        val invalid = ContinuousCurve2D(
            curve = ParametricCurve2D(
                x = { parameter -> parameter },
                y = { parameter -> parameter },
            ),
            minimumParameter = 1.0,
            maximumParameter = -1.0,
            type = ContinuousCurveType.PARAMETER,
        )
        val valid = invalid.copy(
            minimumParameter = -1.0,
            maximumParameter = 1.0,
        )

        assertIs<
            GMResult.Err<GeometryError.InvalidContinuousCurveDomain>,
            >(
            Geometry.meetCurveCurveContinuous(
                first = invalid,
                second = valid,
                intersectionIndex = 0.0,
                secondInitialParameter = 0.0,
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
        tolerance: Double = 1.0e-12,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertTrue(
                abs(expected[index] - actual[index]) <= tolerance,
                "coordinate[$index]: expected=${expected[index]}, " +
                    "actual=${actual[index]}",
            )
        }
    }

    private fun assertNonReal(actual: DoubleArray) {
        assertEquals(0.0, actual[0])
        assertTrue(actual[1].isNaN())
        assertTrue(actual[2].isNaN())
    }
}
