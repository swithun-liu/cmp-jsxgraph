package com.swithun.jsxgraph.core.math

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeometryTest {
    @Test
    fun anglesMatchOfficialReferenceValues() {
        assertEquals(
            -PI / 2.0,
            Geometry.angle(
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(0.0, -1.0),
            ),
        )
        assertEquals(
            PI / 2.0,
            Geometry.rad(
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(0.0, 1.0),
            ),
        )
        assertEquals(
            3.0 * PI / 2.0,
            Geometry.rad(
                doubleArrayOf(0.0, 1.0),
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(1.0, 0.0),
            ),
        )
        assertEquals(
            90.0,
            Geometry.trueAngle(
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, 0.0),
                doubleArrayOf(0.0, 1.0),
            ),
        )
    }

    @Test
    fun distancesMatchOfficialReferenceValues() {
        assertEquals(
            5.0,
            Geometry.distance(
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(4.0, 4.0),
                requestedLength = 2,
            ),
        )
        assertEquals(
            sqrt(3.0),
            Geometry.distance(
                doubleArrayOf(0.0, 0.0, 0.0),
                doubleArrayOf(1.0, 1.0, 1.0),
            ),
        )
        assertEquals(
            5.0,
            Geometry.affineDistance(
                doubleArrayOf(1.0, 3.0, 0.0),
                doubleArrayOf(1.0, 0.0, 4.0),
            ),
        )
        assertEquals(
            Double.POSITIVE_INFINITY,
            Geometry.affineDistance(
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(1.0, 0.0, 0.0),
            ),
        )
    }

    @Test
    fun affineRatiosAndOrientationMatchOfficialReferences() {
        assertEquals(
            3.0,
            Geometry.affineRatio(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(1.0, 2.0, 0.0),
                doubleArrayOf(1.0, 6.0, 0.0),
            ),
        )
        assertEquals(
            3.0,
            Geometry.affineRatio(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(1.0, 0.0, 2.0),
                doubleArrayOf(1.0, 0.0, 6.0),
            ),
        )

        val first = doubleArrayOf(1.0, 0.0, 0.0)
        val second = doubleArrayOf(1.0, 1.0, 0.0)
        val third = doubleArrayOf(1.0, 0.0, 1.0)
        assertEquals(0.5, Geometry.signedTriangle(first, second, third))
        assertEquals(1.0, Geometry.det3p(first, second, third))
    }

    @Test
    fun labelQuadrantsAndDirectionsMatchOfficialBehavior() {
        assertEquals("rt", Geometry.calcLabelQuadrant(0.0))
        assertEquals("top", Geometry.calcLabelQuadrant(PI / 2.0))
        assertEquals("lft", Geometry.calcLabelQuadrant(PI))
        assertEquals("bot", Geometry.calcLabelQuadrant(-PI / 2.0))
        assertEquals("lrt", Geometry.calcLabelQuadrant(5.0 * PI / 3.0))

        assertTrue(
            Geometry.isSameDirection(
                start = doubleArrayOf(1.0, 0.0, 0.0),
                point = doubleArrayOf(1.0, 2.0, 3.0),
                visiblePoint = doubleArrayOf(1.0, 4.0, 6.0),
            ),
        )
        assertFalse(
            Geometry.isSameDirection(
                start = doubleArrayOf(1.0, 0.0, 0.0),
                point = doubleArrayOf(1.0, 2.0, 3.0),
                visiblePoint = doubleArrayOf(1.0, -4.0, -6.0),
            ),
        )
        assertTrue(
            Geometry.isSameDir(
                firstStart = doubleArrayOf(1.0, 0.0, 0.0),
                firstEnd = doubleArrayOf(1.0, 2.0, 2.0),
                secondStart = doubleArrayOf(1.0, 4.0, 4.0),
                secondEnd = doubleArrayOf(1.0, 7.0, 7.0),
            ),
        )
    }

    @Test
    fun pointLineAndSegmentDistancesMatchOfficialReferences() {
        assertEquals(
            5.0,
            Geometry.distPointLine(
                point = doubleArrayOf(1.0, 3.0, 5.0),
                line = doubleArrayOf(0.0, 0.0, 1.0),
            ),
        )
        assertEquals(
            2.0,
            Geometry.distPointLine(
                point = doubleArrayOf(1.0, 0.0, 0.0),
                line = doubleArrayOf(-10.0, 3.0, 4.0),
            ),
        )
        assertEquals(
            Double.POSITIVE_INFINITY,
            Geometry.distPointLine(
                point = doubleArrayOf(1.0, 0.0, 0.0),
                line = doubleArrayOf(1.0, 0.0, 0.0),
            ),
        )
        assertEquals(
            sqrt(2.0),
            Geometry.distPointSegment(
                point = doubleArrayOf(1.0, 2.0, 1.0),
                first = doubleArrayOf(1.0, -1.0, 0.0),
                second = doubleArrayOf(1.0, 1.0, 0.0),
            ),
        )
        assertEquals(
            1.0,
            Geometry.distPointSegment(
                point = doubleArrayOf(1.0, 1.0, 1.0),
                first = doubleArrayOf(1.0, 1.0, 0.0),
                second = doubleArrayOf(1.0, 1.0, 0.0),
            ),
        )
        assertEquals(
            0.0,
            Geometry.distPointSegment(
                point = doubleArrayOf(1.0, 0.0, 0.0),
                first = doubleArrayOf(1.0, -1.0, 0.0),
                second = doubleArrayOf(1.0, 2.0, 0.0),
            ),
        )
    }

    @Test
    fun lineAndCircleIntersectionsMatchOfficialReferences() {
        val verticalAxis = line(0.0, 1.0, 0.0)
        val horizontalAxis = line(0.0, 0.0, 1.0)
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0),
            Geometry.meetLineLine(verticalAxis, horizontalAxis),
        )
        assertContentEquals(
            doubleArrayOf(0.0, -0.0, 1.0),
            Geometry.meetLineLine(verticalAxis, line(-1.0, 1.0, 0.0)),
        )

        val circle = circle(0.0, 0.0, 2.0)
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, -0.0),
            Geometry.meetLineCircle(horizontalAxis, circle, 0),
        )
        assertContentEquals(
            doubleArrayOf(1.0, -2.0, 0.0),
            Geometry.meetLineCircle(horizontalAxis, circle, 1),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 0.0),
            Geometry.meetLineCircle(line(-2.0, 1.0, 0.0), circle, 0),
        )
        assertContentEquals(
            doubleArrayOf(0.0, 0.0, 0.0),
            Geometry.meetLineCircle(line(-3.0, 1.0, 0.0), circle, 0),
        )
    }

    @Test
    fun circleIntersectionsAndZeroRadiusCasesMatchOfficialReferences() {
        val first = circle(0.0, 0.0, 2.0)
        val second = circle(2.0, 0.0, 2.0)
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, -sqrt(3.0)),
            Geometry.meetCircleCircle(first, second, 0),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, sqrt(3.0)),
            Geometry.meetCircleCircle(first, second, 1),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 5.0),
            Geometry.meetCircleCircle(
                circle(3.0, 0.0, 5.0),
                circle(3.0, 5.0, 0.0),
                0,
            ),
        )
        assertContentEquals(
            doubleArrayOf(0.0, 0.0, 0.0),
            Geometry.meetCircleCircle(
                circle(0.0, 0.0, 1.0),
                circle(5.0, 5.0, 0.0),
                0,
            ),
        )
    }

    @Test
    fun segmentIntersectionsMatchOfficialReferences() {
        val intersection = Geometry.meetSegmentSegment(
            firstStart = doubleArrayOf(1.0, 0.0, 0.0),
            firstEnd = doubleArrayOf(1.0, 2.0, 2.0),
            secondStart = doubleArrayOf(1.0, 0.0, 2.0),
            secondEnd = doubleArrayOf(1.0, 2.0, 0.0),
        )
        assertContentEquals(doubleArrayOf(1.0, 1.0, 1.0), intersection.point)
        assertEquals(0.5, intersection.firstParameter)
        assertEquals(0.5, intersection.secondParameter)

        val parallel = Geometry.meetSegmentSegment(
            firstStart = doubleArrayOf(1.0, 0.0, 0.0),
            firstEnd = doubleArrayOf(1.0, 2.0, 0.0),
            secondStart = doubleArrayOf(1.0, 0.0, 1.0),
            secondEnd = doubleArrayOf(1.0, 2.0, 1.0),
        )
        assertContentEquals(doubleArrayOf(0.0, -4.0, 0.0), parallel.point)
        assertEquals(Double.POSITIVE_INFINITY, parallel.firstParameter)
        assertEquals(Double.POSITIVE_INFINITY, parallel.secondParameter)
    }

    private fun line(
        constant: Double,
        horizontal: Double,
        vertical: Double,
    ): DoubleArray = doubleArrayOf(
        constant,
        horizontal,
        vertical,
        0.0,
        1.0,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY,
    )

    private fun circle(
        centerX: Double,
        centerY: Double,
        radius: Double,
    ): DoubleArray = Mat.normalize(
        doubleArrayOf(
            centerX * centerX + centerY * centerY - radius * radius,
            -2.0 * centerX,
            -2.0 * centerY,
            1.0,
            2.0 * radius,
            0.0,
            0.0,
            0.0,
        ),
    )
}
