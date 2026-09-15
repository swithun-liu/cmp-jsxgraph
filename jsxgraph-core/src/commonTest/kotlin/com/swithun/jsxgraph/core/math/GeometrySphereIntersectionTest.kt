package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GeometrySphereIntersectionTest {
    @Test
    fun planeSphereIntersectionMatchesOfficialReferences() {
        val normal = doubleArrayOf(0.0, 0.0, 0.0, 1.0)
        val crossing = Geometry.meetPlaneSphere(
            planeNormal = normal,
            planeDistance = 1.0,
            sphereCenter = point(0.0, 0.0, 0.0),
            sphereRadius = 2.0,
        )
        assertContentEquals(point(0.0, 0.0, 1.0), crossing.center)
        assertSame(normal, crossing.normal)
        assertEquals(
            1.7320508075688772,
            crossing.radius,
            absoluteTolerance = 1.0e-12,
        )

        val tangent = Geometry.meetPlaneSphere(
            planeNormal = normal,
            planeDistance = 2.0,
            sphereCenter = point(0.0, 0.0, 0.0),
            sphereRadius = 2.0,
        )
        assertContentEquals(point(0.0, 0.0, 2.0), tangent.center)
        assertEquals(0.0, tangent.radius)

        val disjoint = Geometry.meetPlaneSphere(
            planeNormal = normal,
            planeDistance = 3.0,
            sphereCenter = point(0.0, 0.0, 0.0),
            sphereRadius = 2.0,
        )
        assertContentEquals(point(0.0, 0.0, 3.0), disjoint.center)
        assertTrue(disjoint.radius.isNaN())
    }

    @Test
    fun equalSphereIntersectionMatchesOfficialReference() {
        val intersection = Geometry.meetSphereSphere(
            firstCenter = point(0.0, 0.0, 0.0),
            firstRadius = 3.0,
            secondCenter = point(4.0, 0.0, 0.0),
            secondRadius = 3.0,
        )

        assertContentEquals(point(2.0, 0.0, 0.0), intersection.center)
        assertContentEquals(
            doubleArrayOf(0.0, 4.0, 0.0, 0.0),
            intersection.normal,
        )
        assertEquals(
            2.23606797749979,
            intersection.radius,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun unequalAndTangentSphereIntersectionsMatchOfficialReferences() {
        val unequal = Geometry.meetSphereSphere(
            firstCenter = point(0.0, 0.0, 0.0),
            firstRadius = 5.0,
            secondCenter = point(6.0, 0.0, 0.0),
            secondRadius = 3.0,
        )
        assertArrayClose(
            point(4.333333333333333, 0.0, 0.0),
            unequal.center,
        )
        assertContentEquals(
            doubleArrayOf(0.0, 6.0, 0.0, 0.0),
            unequal.normal,
        )
        assertEquals(
            2.494438257849294,
            unequal.radius,
            absoluteTolerance = 1.0e-12,
        )

        val tangent = Geometry.meetSphereSphere(
            firstCenter = point(0.0, 0.0, 0.0),
            firstRadius = 2.0,
            secondCenter = point(3.0, 0.0, 0.0),
            secondRadius = 1.0,
        )
        assertContentEquals(point(2.0, 0.0, 0.0), tangent.center)
        assertContentEquals(
            doubleArrayOf(0.0, 3.0, 0.0, 0.0),
            tangent.normal,
        )
        assertEquals(0.0, tangent.radius)
    }

    @Test
    fun disjointAndConcentricSphereIntersectionsPropagateNaN() {
        val disjoint = Geometry.meetSphereSphere(
            firstCenter = point(0.0, 0.0, 0.0),
            firstRadius = 1.0,
            secondCenter = point(3.0, 0.0, 0.0),
            secondRadius = 1.0,
        )
        assertContentEquals(point(1.5, 0.0, 0.0), disjoint.center)
        assertTrue(disjoint.radius.isNaN())

        for (secondRadius in listOf(1.0, 2.0)) {
            val concentric = Geometry.meetSphereSphere(
                firstCenter = point(0.0, 0.0, 0.0),
                firstRadius = 2.0,
                secondCenter = point(0.0, 0.0, 0.0),
                secondRadius = secondRadius,
            )
            assertEquals(1.0, concentric.center[0])
            assertTrue(concentric.center[1].isNaN())
            assertTrue(concentric.center[2].isNaN())
            assertTrue(concentric.center[3].isNaN())
            assertContentEquals(
                doubleArrayOf(0.0, 0.0, 0.0, 0.0),
                concentric.normal,
            )
            assertTrue(concentric.radius.isNaN())
        }
    }

    @Test
    fun idealSphereCenterPreservesPoint3DDistanceSemantics() {
        val intersection = Geometry.meetSphereSphere(
            firstCenter = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
            firstRadius = 2.0,
            secondCenter = point(3.0, 0.0, 0.0),
            secondRadius = 1.0,
        )

        assertContentEquals(point(2.0, 0.0, 0.0), intersection.center)
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 0.0, 0.0),
            intersection.normal,
        )
        assertTrue(intersection.radius.isNaN())
    }

    private fun point(
        x: Double,
        y: Double,
        z: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y, z)

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
