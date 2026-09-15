package com.swithun.jsxgraph.core.math

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertContentEquals

class GeometryArcTest {
    private val radiusPoint = point(1.0, 0.0)
    private val center = point(0.0, 0.0)
    private val anglePoint = point(0.0, 1.0)
    private val testAngles = doubleArrayOf(
        0.0,
        PI / 4.0,
        PI / 2.0,
        PI,
        3.0 * PI / 2.0,
    )

    @Test
    fun automaticArcSelectionMatchesOfficialOrientations() {
        assertArcMembership(
            selection = ArcSelection.AUTO,
            orientation = ArcOrientation.COUNTERCLOCKWISE,
            expected = booleanArrayOf(true, true, true, false, false),
        )
        assertArcMembership(
            selection = ArcSelection.AUTO,
            orientation = ArcOrientation.CLOCKWISE,
            expected = booleanArrayOf(false, false, true, true, true),
        )
    }

    @Test
    fun minorArcSelectionMatchesOfficialOrientations() {
        val expected = booleanArrayOf(true, true, true, false, false)
        assertArcMembership(
            selection = ArcSelection.MINOR,
            orientation = ArcOrientation.COUNTERCLOCKWISE,
            expected = expected,
        )
        assertArcMembership(
            selection = ArcSelection.MINOR,
            orientation = ArcOrientation.CLOCKWISE,
            expected = expected,
        )
    }

    @Test
    fun majorArcSelectionMatchesOfficialOrientations() {
        val expected = booleanArrayOf(false, false, true, true, true)
        assertArcMembership(
            selection = ArcSelection.MAJOR,
            orientation = ArcOrientation.COUNTERCLOCKWISE,
            expected = expected,
        )
        assertArcMembership(
            selection = ArcSelection.MAJOR,
            orientation = ArcOrientation.CLOCKWISE,
            expected = expected,
        )
    }

    private fun assertArcMembership(
        selection: ArcSelection,
        orientation: ArcOrientation,
        expected: BooleanArray,
    ) {
        val actual = BooleanArray(testAngles.size) { index ->
            val angle = testAngles[index]
            Geometry.coordsOnArc(
                radiusPoint = radiusPoint,
                center = center,
                anglePoint = anglePoint,
                coordinates = point(cos(angle), sin(angle)),
                selection = selection,
                orientation = orientation,
            )
        }
        assertContentEquals(expected, actual)
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(x, y)
}
