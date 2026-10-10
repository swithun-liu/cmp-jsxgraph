/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DefaultAxesTest {
    @Test
    fun factoryAppliesOfficialRoleDefaultsAndDeepUserOverrides() {
        val board = board()
        val axes = assertIs<GMResult.Ok<BoardDefaultAxes>>(
            createDefaultAxes(
                board = board,
                axisAttributes = mapOf(
                    "strokeColor" to "#112233",
                    "ticks" to mapOf(
                        "minorTicks" to 2,
                        "label" to mapOf("fontSize" to 15.0),
                    ),
                ),
                defaultAxesAttributes = mapOf(
                    "x" to mapOf(
                        "name" to "horizontal",
                        "ticks" to mapOf("ticksDistance" to 3.0),
                    ),
                    "y" to mapOf(
                        "ticks" to mapOf(
                            "minorTicks" to 5,
                            "label" to mapOf("offset" to listOf(-8.0, 1.0)),
                        ),
                    ),
                ),
            ),
        ).value

        assertEquals("horizontal", axes.x.name)
        assertEquals("y", axes.y.name)
        assertEquals("#112233", axes.x.evalVisProp("strokecolor"))
        assertEquals("#112233", axes.y.evalVisProp("strokecolor"))
        assertFalse(axes.x.dump)
        assertFalse(axes.y.dump)
        assertSame(axes.x, board.elementById(axes.x.id))
        assertSame(axes.y, board.elementById(axes.y.id))

        val xTicks = assertNotNull(axes.x.defaultTicks)
        val yTicks = assertNotNull(axes.y.defaultTicks)
        assertEquals(3.0, xTicks.attributes.ticksDistance)
        assertEquals(2, xTicks.attributes.minorTicks)
        assertEquals(5, yTicks.attributes.minorTicks)
        assertContentEquals(
            doubleArrayOf(0.0, 1.0),
            xTicks.attributes.tickEndings,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0),
            yTicks.attributes.tickEndings,
        )
        assertContentEquals(
            doubleArrayOf(0.0, -3.0),
            xTicks.attributes.labelOffset,
        )
        assertContentEquals(
            doubleArrayOf(-8.0, 1.0),
            yTicks.attributes.labelOffset,
        )
        assertEquals("middle", xTicks.attributes.labelAnchorX)
        assertEquals("top", xTicks.attributes.labelAnchorY)
        assertEquals("right", yTicks.attributes.labelAnchorX)
        assertEquals("middle", yTicks.attributes.labelAnchorY)
        assertEquals(15.0, xTicks.attributes.labelFontSize)
        assertEquals(15.0, yTicks.attributes.labelFontSize)
    }

    @Test
    fun boardAxisMutationCreatesAndTogglesRegisteredAxes() {
        val board = board()

        board.setAttribute(mapOf("axis" to true))

        val axes = assertNotNull(board.defaultAxes)
        assertNull(board.defaultAxesError)
        assertSame(axes.x, board.elementById(axes.x.id))
        assertSame(axes.y, board.elementById(axes.y.id))
        assertTrue(axes.x.evalVisProp("visible") as Boolean)
        assertTrue(axes.y.evalVisProp("visible") as Boolean)

        board.setAttribute(mapOf("axis" to false))

        assertFalse(axes.x.evalVisProp("visible") as Boolean)
        assertFalse(axes.y.evalVisProp("visible") as Boolean)
        assertEquals(false, board.attributes["axis"])

        board.setAttribute(mapOf("axis" to true))

        assertSame(axes, board.defaultAxes)
        assertTrue(axes.x.evalVisProp("visible") as Boolean)
        assertTrue(axes.y.evalVisProp("visible") as Boolean)
    }

    @Test
    fun boardDefaultAxesMutationForwardsNestedAxisAndTickAttributes() {
        val board = board()
        board.setAttribute(mapOf("axis" to true))
        val axes = assertNotNull(board.defaultAxes)

        board.setAttribute(
            mapOf(
                "defaultAxes" to mapOf(
                    "x" to mapOf(
                        "strokeColor" to "#aa0000",
                        "ticks" to mapOf("ticksDistance" to 2.5),
                    ),
                    "y" to mapOf(
                        "visible" to false,
                        "ticks" to mapOf("minorTicks" to 7),
                    ),
                ),
            ),
        )

        assertEquals("#aa0000", axes.x.evalVisProp("strokecolor"))
        assertEquals(
            2.5,
            assertNotNull(axes.x.defaultTicks).attributes.ticksDistance,
        )
        assertFalse(axes.y.evalVisProp("visible") as Boolean)
        assertEquals(
            7,
            assertNotNull(axes.y.defaultTicks).attributes.minorTicks,
        )
        assertNull(board.defaultAxesError)
    }

    private fun board(): Board =
        Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            keepAspectRatio = false,
            defaultCurveMinimum = -6.0,
            defaultCurveMaximum = 6.0,
        )
}
