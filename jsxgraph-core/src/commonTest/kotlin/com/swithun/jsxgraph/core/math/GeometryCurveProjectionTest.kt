package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class GeometryCurveProjectionTest {
    @Test
    fun polylineProjectionMatchesOfficialReferences() {
        val curve = DiscreteCurve2D(
            points = listOf(
                point(0.0, 0.0),
                point(2.0, 0.0),
                point(2.0, 2.0),
            ),
            bezierDegree = 1,
        )

        val sideProjection = projection(
            Geometry.projectCoordsToCurve(
                point = point(1.0, 1.5),
                curve = curve,
            ),
        )
        assertPointClose(point(2.0, 1.5), sideProjection.point)
        assertEquals(
            1.75,
            sideProjection.parameter,
            absoluteTolerance = 1.0e-12,
        )

        val endpointProjection = projection(
            Geometry.projectCoordsToCurve(
                point = point(3.0, -1.0),
                curve = curve,
            ),
        )
        assertPointClose(point(2.0, 0.0), endpointProjection.point)
        assertEquals(
            1.0,
            endpointProjection.parameter,
            absoluteTolerance = 1.0e-12,
        )
        assertNotSame(curve.points[1], endpointProjection.point)
    }

    @Test
    fun cubicBezierPathProjectionMatchesOfficialReferences() {
        val curve = DiscreteCurve2D(
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

        val firstSegment = projection(
            Geometry.projectCoordsToCurve(
                point = point(1.0, 1.0),
                curve = curve,
            ),
        )
        assertPointClose(
            point(1.0129632516076554, 1.1872257623370572),
            firstSegment.point,
            tolerance = 1.0e-8,
        )
        assertEquals(
            0.3260955959963084,
            firstSegment.parameter,
            absoluteTolerance = 1.0e-9,
        )

        val secondSegment = projection(
            Geometry.projectCoordsToCurve(
                point = point(6.0, 1.0),
                curve = curve,
            ),
        )
        assertPointClose(
            point(5.971847799547475, 0.9794003866764205),
            secondSegment.point,
            tolerance = 1.0e-8,
        )
        assertEquals(
            3.589126536065923,
            secondSegment.parameter,
            absoluteTolerance = 1.0e-9,
        )
    }

    @Test
    fun discreteCurveProjectionPreservesEmptyAndSinglePointResults() {
        val empty = projection(
            Geometry.projectCoordsToCurve(
                point = point(1.0, 1.0),
                curve = DiscreteCurve2D(
                    points = emptyList(),
                    bezierDegree = 1,
                ),
            ),
        )
        assertPointClose(
            doubleArrayOf(0.0, 1.0, 1.0),
            empty.point,
        )
        assertEquals(0.0, empty.parameter)

        val onlyPoint = point(4.0, 5.0)
        val single = projection(
            Geometry.projectCoordsToCurve(
                point = point(1.0, 1.0),
                curve = DiscreteCurve2D(
                    points = listOf(onlyPoint),
                    bezierDegree = 1,
                ),
            ),
        )
        assertPointClose(onlyPoint, single.point)
        assertNotSame(onlyPoint, single.point)
        assertEquals(0.0, single.parameter)
    }

    @Test
    fun discreteCurveProjectionReportsInvalidTopology() {
        assertIs<GMResult.Err<GeometryError.InvalidDiscreteCurveDegree>>(
            Geometry.projectCoordsToCurve(
                point = point(0.0, 0.0),
                curve = DiscreteCurve2D(
                    points = listOf(point(0.0, 0.0), point(1.0, 1.0)),
                    bezierDegree = 2,
                ),
            ),
        )
        assertIs<GMResult.Err<GeometryError.InvalidDiscreteCurvePointCount>>(
            Geometry.projectCoordsToCurve(
                point = point(0.0, 0.0),
                curve = DiscreteCurve2D(
                    points = listOf(
                        point(0.0, 0.0),
                        point(1.0, 1.0),
                        point(2.0, 0.0),
                    ),
                    bezierDegree = 3,
                ),
            ),
        )
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)

    private fun projection(
        result: GMResult<ProjectionResult, GeometryError>,
    ): ProjectionResult =
        assertIs<GMResult.Ok<ProjectionResult>>(result).value

    private fun assertPointClose(
        expected: DoubleArray,
        actual: DoubleArray,
        tolerance: Double = 1.0e-12,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertTrue(
                kotlin.math.abs(expected[index] - actual[index]) <= tolerance,
                "index $index: expected ${expected[index]}, actual ${actual[index]}",
            )
        }
    }
}
