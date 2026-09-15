package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Geometry3DTest {
    @Test
    fun threePlaneIntersectionMatchesOfficialReferences() {
        assertArrayClose(
            doubleArrayOf(1.0, 0.0, 0.0, 0.0),
            Geometry.meet3Planes(
                firstNormal = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
                firstDistance = 0.0,
                secondNormal = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                secondDistance = 0.0,
                thirdNormal = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                thirdDistance = 0.0,
            ),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 2.0, 3.0),
            Geometry.meet3Planes(
                firstNormal = doubleArrayOf(0.0, 1.0, 1.0, 0.0),
                firstDistance = 3.0,
                secondNormal = doubleArrayOf(0.0, 0.0, 1.0, 1.0),
                secondDistance = 5.0,
                thirdNormal = doubleArrayOf(0.0, 1.0, 0.0, 1.0),
                thirdDistance = 4.0,
            ),
        )
    }

    @Test
    fun singularThreePlaneIntersectionPropagatesUndefinedNumbers() {
        val intersection = Geometry.meet3Planes(
            firstNormal = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
            firstDistance = 1.0,
            secondNormal = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
            secondDistance = 2.0,
            thirdNormal = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
            thirdDistance = 3.0,
        )

        assertEquals(1.0, intersection[0])
        assertTrue(intersection[1].isNaN())
        assertTrue(intersection[2].isNaN())
        assertEquals(Double.NEGATIVE_INFINITY, intersection[3])
    }

    @Test
    fun planeIntersectionDirectionMatchesOfficialReferences() {
        assertArrayClose(
            doubleArrayOf(0.0, 1.0, 0.0, 0.0),
            Geometry.meetPlanePlane(
                firstVector = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                secondVector = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                thirdVector = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                fourthVector = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
            ),
        )
        assertContentEquals(
            doubleArrayOf(0.0, -1.0, -1.0, -1.0),
            Geometry.meetPlanePlane(
                firstVector = doubleArrayOf(0.0, 1.0, 1.0, 0.0),
                secondVector = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
                thirdVector = doubleArrayOf(0.0, 1.0, 0.0, 1.0),
                fourthVector = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
            ),
        )
    }

    @Test
    fun pointToPlaneProjectionMatchesNormalizedAndRawNormalSemantics() {
        assertContentEquals(
            doubleArrayOf(3.0, 4.0, 1.0),
            Geometry.project3DTo3DPlane(
                point = doubleArrayOf(3.0, 4.0, 5.0),
                normal = doubleArrayOf(0.0, 0.0, 1.0),
                foot = doubleArrayOf(0.0, 0.0, 1.0),
            ),
        )
        assertContentEquals(
            doubleArrayOf(3.0, 4.0, -3.0),
            Geometry.project3DTo3DPlane(
                point = doubleArrayOf(3.0, 4.0, 5.0),
                normal = doubleArrayOf(0.0, 0.0, 2.0),
                foot = doubleArrayOf(0.0, 0.0, 1.0),
            ),
        )
        assertContentEquals(
            doubleArrayOf(3.0, 0.0, 5.0),
            Geometry.project3DTo3DPlane(
                point = doubleArrayOf(3.0, 4.0, 5.0),
                normal = doubleArrayOf(0.0, 1.0, 0.0),
            ),
        )
    }

    @Test
    fun planeBoundsMatchOfficialLinearSystemReferences() {
        assertContentEquals(
            doubleArrayOf(-2.0, 8.0, -3.0, 7.0),
            definedBounds(
                Geometry.getPlaneBounds(
                    firstVector = doubleArrayOf(1.0, 0.0, 1.0),
                    secondVector = doubleArrayOf(0.0, 1.0, 0.0),
                    point = doubleArrayOf(2.0, 3.0, 4.0),
                    start = 0.0,
                    end = 10.0,
                ),
            ),
        )
        assertArrayClose(
            doubleArrayOf(-3.2, -0.8, 1.4, 2.6),
            definedBounds(
                Geometry.getPlaneBounds(
                    firstVector = doubleArrayOf(2.0, 1.0, 0.0),
                    secondVector = doubleArrayOf(1.0, 3.0, 0.0),
                    point = doubleArrayOf(4.0, -2.0, 0.0),
                    start = -1.0,
                    end = 5.0,
                ),
            ),
        )
    }

    @Test
    fun planeBoundsPreserveEarlyExitAndReportSingularMatrix() {
        assertNull(
            bounds(
                Geometry.getPlaneBounds(
                    firstVector = doubleArrayOf(1.0, 0.0, 0.0),
                    secondVector = doubleArrayOf(0.0, 1.0, 0.0),
                    point = doubleArrayOf(0.0, 0.0, 0.0),
                    start = -1.0,
                    end = 1.0,
                ),
            ),
        )
        assertIs<GMResult.Err<NumericsError.SingularMatrix>>(
            Geometry.getPlaneBounds(
                firstVector = doubleArrayOf(1.0, 2.0, 1.0),
                secondVector = doubleArrayOf(2.0, 4.0, 0.0),
                point = doubleArrayOf(0.0, 0.0, 0.0),
                start = -1.0,
                end = 1.0,
            ),
        )
    }

    private fun bounds(
        result: GMResult<DoubleArray?, NumericsError>,
    ): DoubleArray? = assertIs<GMResult.Ok<DoubleArray?>>(result).value

    private fun definedBounds(
        result: GMResult<DoubleArray?, NumericsError>,
    ): DoubleArray = assertNotNull(bounds(result))

    private fun assertArrayClose(
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
