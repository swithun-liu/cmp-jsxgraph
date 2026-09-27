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
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TapemeasureTest {
    @Test
    fun createsOfficialHelperGraphAndTracksEndpointMotion() {
        val board = board()
        val line = tapemeasure(
            Tapemeasure.create(
                board = board,
                startCoordinates = doubleArrayOf(1.0, 2.0),
                endCoordinates = doubleArrayOf(4.0, 6.0),
                attributes = TapemeasureAttributes(
                    id = "tape",
                    name = "dist",
                    point1 = TapemeasurePointAttributes(id = "start"),
                    point2 = TapemeasurePointAttributes(id = "end"),
                    label = TapemeasureElementAttributes(id = "label"),
                    ticks = TapemeasureElementAttributes(id = "ticks"),
                    labelDigits = 3,
                ),
            ),
        )
        board.fullUpdate()
        val definition =
            line.tapemeasureDefinition ?: error("missing definition")
        val label = assertIs<Text>(definition.label)
        val ticks = assertIs<Ticks>(definition.tapeTicks)

        assertEquals("tapemeasure", line.elType)
        assertFalse(line.straightFirst)
        assertFalse(line.straightLast)
        assertEquals(5.0, definition.Value())
        assertEquals(
            listOf("start", "end", "tape", "tapeLabel", "ticks"),
            board.objectsList.map(GeometryElement::id),
        )
        assertEquals(
            mapOf<String, GeometryElement>(
                "point1" to definition.point1,
                "point2" to definition.point2,
            ),
            line.subs,
        )
        assertEquals(
            listOf(definition.point1, definition.point2, ticks),
            line.inherits,
        )
        assertSame(line, label.ancestors[line.id])
        assertSame(label, line.childElements[label.id])
        assertSame(line, ticks.parent)
        assertFalse(definition.point1.dump)
        assertFalse(definition.point2.dump)
        assertFalse(label.dump)
        assertFalse(ticks.dump)
        assertEquals("dist = 5.000", label.plaintext)
        assertEquals(2.5, label.X())
        assertEquals(4.0, label.Y())
        assertContentEquals(doubleArrayOf(10.0, -10.0), label.screenOffset)
        assertEquals(0.1, ticks.attributes.ticksDistance)
        assertEquals(8.0, ticks.attributes.minorHeight)
        assertEquals(16.0, ticks.attributes.majorHeight)
        assertEquals("middle", ticks.attributes.labelAnchorX)
        assertEquals("top", ticks.attributes.labelAnchorY)

        definition.point2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(7.0, 10.0),
        )
        board.fullUpdate()

        assertEquals(10.0, definition.Value())
        assertEquals("dist = 10.000", label.plaintext)
        assertEquals(4.0, label.X())
        assertEquals(6.0, label.Y())

        board.removeObject(line)
        assertTrue(board.objectsList.isEmpty())
    }

    @Test
    fun optionalHelpersAndFailuresPreserveBoardState() {
        val board = board()
        val line = tapemeasure(
            Tapemeasure.create(
                board = board,
                startCoordinates = doubleArrayOf(-2.0, 1.0),
                endCoordinates = doubleArrayOf(2.0, 1.0),
                attributes = TapemeasureAttributes(
                    id = "plain",
                    name = "",
                    withLabel = false,
                    withTicks = false,
                ),
            ),
        )
        val definition =
            line.tapemeasureDefinition ?: error("missing definition")

        assertNull(definition.label)
        assertNull(definition.tapeTicks)
        assertEquals(3, board.numObjects)

        val invalid = assertIs<
            GMResult.Err<TapemeasureError.InvalidCoordinateCount>,
            >(
            Tapemeasure.create(
                board = board,
                startCoordinates = doubleArrayOf(0.0),
                endCoordinates = doubleArrayOf(1.0, 1.0),
            ),
        ).error
        assertEquals("start", invalid.parent)
        assertEquals(3, board.numObjects)

        val duplicate = assertIs<
            GMResult.Err<TapemeasureError.LineFactory>,
            >(
            Tapemeasure.create(
                board = board,
                startCoordinates = doubleArrayOf(0.0, 0.0),
                endCoordinates = doubleArrayOf(1.0, 1.0),
                attributes = TapemeasureAttributes(id = "plain"),
            ),
        ).error
        assertIs<LineError.Registration>(duplicate.error)
        assertEquals(
            listOf("boardP0", "boardP1", "plain"),
            board.objectsList.map(GeometryElement::id),
        )
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        id = "board",
    )

    private fun tapemeasure(
        result: GMResult<Line, TapemeasureError>,
    ): Line = assertIs<GMResult.Ok<Line>>(result).value
}
