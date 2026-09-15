package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GeometryBezierTest {
    private val curve = listOf(
        doubleArrayOf(0.0, 0.0),
        doubleArrayOf(1.0, 3.0),
        doubleArrayOf(2.0, -1.0),
        doubleArrayOf(4.0, 2.0),
    )

    @Test
    fun bezierSplitMatchesOfficialDeCasteljauReference() {
        val split = Geometry.bezierSplit(curve)

        assertEquals(2, split.size)
        assertNestedContentEquals(
            listOf(
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(0.5, 1.5),
                doubleArrayOf(1.0, 1.25),
                doubleArrayOf(1.625, 1.0),
            ),
            split[0],
        )
        assertNestedContentEquals(
            listOf(
                doubleArrayOf(1.625, 1.0),
                doubleArrayOf(2.25, 0.75),
                doubleArrayOf(3.0, 0.5),
                doubleArrayOf(4.0, 2.0),
            ),
            split[1],
        )
        assertSame(curve[0], split[0][0])
        assertSame(curve[3], split[1][3])
        assertSame(split[0][3], split[1][0])
    }

    @Test
    fun bezierBoundingBoxesAndOverlapMatchOfficialReferences() {
        assertContentEquals(
            doubleArrayOf(0.0, 3.0, 4.0, -1.0),
            Geometry.bezierBoundingBox(curve),
        )
        assertContentEquals(
            doubleArrayOf(-3.0, 5.0, 2.0, -1.0),
            Geometry.bezierBoundingBox(
                listOf(
                    doubleArrayOf(2.0, -1.0),
                    doubleArrayOf(-3.0, 5.0),
                ),
            ),
        )

        val first = doubleArrayOf(0.0, 2.0, 4.0, -1.0)
        assertTrue(
            Geometry.bezierOverlap(
                first,
                doubleArrayOf(4.0, 3.0, 8.0, 1.0),
            ),
        )
        assertFalse(
            Geometry.bezierOverlap(
                first,
                doubleArrayOf(4.1, 3.0, 8.0, 1.0),
            ),
        )
        assertTrue(
            Geometry.bezierOverlap(
                first,
                doubleArrayOf(1.0, -1.0, 2.0, -3.0),
            ),
        )
        assertFalse(
            Geometry.bezierOverlap(
                first,
                doubleArrayOf(1.0, -1.1, 2.0, -3.0),
            ),
        )
    }

    @Test
    fun bezierSubdivisionFindsAndDeduplicatesCrossing() {
        val intersections = Geometry.meetBeziersegmentBeziersegment(
            red = listOf(
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(1.0, 1.0),
                doubleArrayOf(2.0, 2.0),
                doubleArrayOf(3.0, 3.0),
            ),
            blue = listOf(
                doubleArrayOf(0.0, 3.0),
                doubleArrayOf(1.0, 2.0),
                doubleArrayOf(2.0, 1.0),
                doubleArrayOf(3.0, 0.0),
            ),
            testSegment = true,
        )

        assertEquals(1, intersections.size)
        assertIntersection(
            intersection = intersections[0],
            expectedX = 1.5,
            expectedY = 1.5,
            expectedFirstParameter = 0.5,
            expectedSecondParameter = 0.5,
        )
        assertTrue(
            Geometry.meetBeziersegmentBeziersegment(
                red = listOf(
                    doubleArrayOf(0.0, 0.0),
                    doubleArrayOf(1.0, 0.0),
                    doubleArrayOf(2.0, 0.0),
                    doubleArrayOf(3.0, 0.0),
                ),
                blue = listOf(
                    doubleArrayOf(0.0, 2.0),
                    doubleArrayOf(1.0, 2.0),
                    doubleArrayOf(2.0, 2.0),
                    doubleArrayOf(3.0, 2.0),
                ),
                testSegment = true,
            ).isEmpty(),
        )
    }

    @Test
    fun bezierLineSubdivisionMatchesOfficialThreeRootReference() {
        val intersections = Geometry.meetBeziersegmentBeziersegment(
            red = curve,
            blue = listOf(
                doubleArrayOf(0.0, 1.0),
                doubleArrayOf(4.0, 1.0),
            ),
            testSegment = true,
        )

        assertEquals(3, intersections.size)
        assertIntersection(
            intersection = intersections[0],
            expectedX = 0.5267170856197547,
            expectedY = 1.0,
            expectedFirstParameter = 0.17378147191736604,
            expectedSecondParameter = 0.13167927140493868,
        )
        assertIntersection(
            intersection = intersections[1],
            expectedX = 1.625,
            expectedY = 1.0,
            expectedFirstParameter = 0.5,
            expectedSecondParameter = 0.40625,
        )
        assertIntersection(
            intersection = intersections[2],
            expectedX = 3.043260016542931,
            expectedY = 1.0,
            expectedFirstParameter = 0.826218528082634,
            expectedSecondParameter = 0.7608150041357328,
        )
    }

    @Test
    fun lineSubdivisionPreservesOfficialRecursiveSegmentFlagBehavior() {
        val shortSegment = listOf(
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(0.2, 1.0),
        )
        val segmentIntersections = Geometry.meetBeziersegmentBeziersegment(
            red = curve,
            blue = shortSegment,
            testSegment = true,
        )
        val lineIntersections = Geometry.meetBeziersegmentBeziersegment(
            red = curve,
            blue = shortSegment,
            testSegment = false,
        )

        assertEquals(3, segmentIntersections.size)
        assertEquals(3, lineIntersections.size)
        for (index in segmentIntersections.indices) {
            assertIntersection(
                intersection = segmentIntersections[index],
                expectedX = lineIntersections[index].point[1],
                expectedY = lineIntersections[index].point[2],
                expectedFirstParameter =
                    lineIntersections[index].firstParameter,
                expectedSecondParameter =
                    lineIntersections[index].secondParameter,
            )
            assertTrue(segmentIntersections[index].secondParameter > 1.0)
        }

        assertTrue(
            Geometry.meetBeziersegmentBeziersegment(
                red = curve,
                blue = listOf(
                    doubleArrayOf(10.0, 1.0),
                    doubleArrayOf(11.0, 1.0),
                ),
                testSegment = true,
            ).isEmpty(),
        )
    }

    @Test
    fun bezierSegmentEvaluationMatchesOfficialReferencesAndExtrapolation() {
        assertContentEquals(
            doubleArrayOf(1.0, -4.0, -44.0),
            Geometry.bezierSegmentEval(-1.0, curve),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0),
            Geometry.bezierSegmentEval(0.0, curve),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.765625, 1.15625),
            Geometry.bezierSegmentEval(0.25, curve),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 1.625, 1.0),
            Geometry.bezierSegmentEval(0.5, curve),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 2.0),
            Geometry.bezierSegmentEval(1.0, curve),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 14.0, 46.0),
            Geometry.bezierSegmentEval(2.0, curve),
        )
        val undefined = Geometry.bezierSegmentEval(Double.NaN, curve)
        assertEquals(1.0, undefined[0])
        assertTrue(undefined[1].isNaN())
        assertTrue(undefined[2].isNaN())
    }

    @Test
    fun segmentProjectionMatchesOfficialUnclampedParameters() {
        val first = point(0.0, 0.0)
        val second = point(2.0, 0.0)

        val perpendicular = Geometry.projectCoordsToSegment(
            point = point(1.0, 1.0),
            first = first,
            second = second,
        )
        assertContentEquals(point(1.0, 0.0), perpendicular.point)
        assertEquals(0.5, perpendicular.parameter)

        val beyond = Geometry.projectCoordsToSegment(
            point = point(3.0, 0.0),
            first = first,
            second = second,
        )
        assertContentEquals(point(3.0, 0.0), beyond.point)
        assertEquals(1.5, beyond.parameter)

        val before = Geometry.projectCoordsToSegment(
            point = point(-1.0, 0.0),
            first = first,
            second = second,
        )
        assertContentEquals(point(-1.0, 0.0), before.point)
        assertEquals(-0.5, before.parameter)
    }

    @Test
    fun zeroLengthSegmentProjectionPreservesEndpointIdentity() {
        val endpoint = point(1.0, 1.0)
        val projection = Geometry.projectCoordsToSegment(
            point = point(5.0, 5.0),
            first = endpoint,
            second = endpoint,
        )

        assertSame(endpoint, projection.point)
        assertEquals(0.0, projection.parameter)
    }

    @Test
    fun bezierProjectionMatchesOfficialBrentReferences() {
        val parametricCurve = ParametricCurve2D(
            x = { parameter -> Geometry.bezierSegmentEval(parameter, curve)[1] },
            y = { parameter -> Geometry.bezierSegmentEval(parameter, curve)[2] },
        )
        val projection = projection(
            Geometry.projectCoordsToBeziersegment(
                point = point(1.0, 1.0),
                curve = parametricCurve,
                start = 0.0,
            ),
        )

        assertEquals(
            0.3260955959963084,
            projection.parameter,
            absoluteTolerance = 1.0e-9,
        )
        assertEquals(
            1.0129632516076554,
            projection.point[1],
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            1.1872257623370572,
            projection.point[2],
            absoluteTolerance = 1.0e-8,
        )

        val shiftedCurve = ParametricCurve2D(
            x = { parameter -> Geometry.bezierSegmentEval(parameter - 2.0, curve)[1] },
            y = { parameter -> Geometry.bezierSegmentEval(parameter - 2.0, curve)[2] },
        )
        val shiftedProjection = projection(
            Geometry.projectCoordsToBeziersegment(
                point = point(1.0, 1.0),
                curve = shiftedCurve,
                start = 2.0,
            ),
        )
        assertEquals(
            projection.parameter,
            shiftedProjection.parameter,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(projection.point[0], shiftedProjection.point[0])
        assertEquals(
            projection.point[1],
            shiftedProjection.point[1],
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            projection.point[2],
            shiftedProjection.point[2],
            absoluteTolerance = 1.0e-8,
        )
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)

    private fun assertIntersection(
        intersection: SegmentIntersection,
        expectedX: Double,
        expectedY: Double,
        expectedFirstParameter: Double,
        expectedSecondParameter: Double,
    ) {
        assertEquals(1.0, intersection.point[0])
        assertEquals(
            expectedX,
            intersection.point[1],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            expectedY,
            intersection.point[2],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            expectedFirstParameter,
            intersection.firstParameter,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            expectedSecondParameter,
            intersection.secondParameter,
            absoluteTolerance = 1.0e-12,
        )
    }

    private fun assertNestedContentEquals(
        expected: List<DoubleArray>,
        actual: List<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertContentEquals(expected[index], actual[index])
        }
    }

    private fun projection(
        result: GMResult<ProjectionResult, NumericsError>,
    ): ProjectionResult = assertIs<GMResult.Ok<ProjectionResult>>(result).value
}
