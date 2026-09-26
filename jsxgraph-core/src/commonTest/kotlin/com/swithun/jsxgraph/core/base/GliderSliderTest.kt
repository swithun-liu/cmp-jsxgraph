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

class GliderSliderTest {
    @Test
    fun lineGliderProjectsClampsAndTracksParentMotion() {
        val board = board()
        val start = point(board, -4.0, 1.0, "lineStart")
        val end = point(board, 4.0, 3.0, "lineEnd")
        val segment = segment(board, start, end, "segment")
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 5.0),
                slideObject = segment,
                id = "glider",
                name = "",
            ),
        )

        assertEquals(Const.OBJECT_TYPE_GLIDER, glider.type)
        assertEquals("glider", glider.elType)
        assertPoint(0.7058823529411765, 2.176470588235294, glider)
        assertEquals(
            0.5882352941176471,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )
        assertSame(segment, glider.slideObject)
        assertSame(segment, glider.slideObjects.single())
        assertSame(glider, segment.childElements[glider.id])
        assertEquals(listOf(segment.id), glider.parents)

        glider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(8.0, -3.0),
        )
        board.update(draggedElement = glider)
        assertPoint(4.0, 3.0, glider)
        assertEquals(1.0, glider.position)

        glider.setGliderPosition(0.25)
        assertPoint(4.0, 3.0, glider)
        assertEquals(0.25, glider.position)

        end.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 5.0),
        )
        board.update()
        assertPoint(-2.0, 2.0, glider)
        assertEquals(0.25, glider.position)
    }

    @Test
    fun sliderOwnsHelpersAndMatchesValueLifecycle() {
        val board = board()
        val slider = slider(
            Slider.create(
                board = board,
                startCoordinates = doubleArrayOf(-4.0, -1.0),
                endCoordinates = doubleArrayOf(4.0, 3.0),
                range = doubleArrayOf(-10.0, 3.0, 10.0),
                attributes = SliderAttributes(
                    id = "slider",
                    name = "s",
                    point1 = SliderPointAttributes(id = "sliderStart"),
                    point2 = SliderPointAttributes(id = "sliderEnd"),
                    baseline = SliderElementAttributes(id = "baseline"),
                    highline = SliderElementAttributes(id = "highline"),
                    ticks = SliderElementAttributes(id = "sliderTicks"),
                    label = SliderElementAttributes(id = "sliderLabel"),
                ),
            ),
        )
        board.fullUpdate()

        assertEquals(Const.OBJECT_TYPE_GLIDER, slider.type)
        assertEquals("slider", slider.elType)
        assertPoint(1.2, 1.6, slider)
        assertEquals(
            0.65,
            assertIs<Double>(slider.position),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(3.0, slider.Value(), absoluteTolerance = TOLERANCE)
        assertFalse(slider.point1.dump)
        assertFalse(slider.point2.dump)
        assertFalse(slider.baseline.dump)
        assertFalse(slider.highline.dump)
        assertTrue(slider.highline.needsRegularUpdate)
        assertFalse(assertIs<Ticks>(slider.sliderTicks).dump)
        assertFalse(assertIs<Text>(slider.label).dump)
        assertEquals(
            listOf(
                "sliderStart",
                "sliderEnd",
                "baseline",
                "slider",
                "highline",
                "sliderLabel",
                "sliderTicks",
            ),
            board.objectsList.map(GeometryElement::id),
        )
        assertEquals(
            mapOf(
                "point1" to slider.point1,
                "point2" to slider.point2,
                "baseLine" to slider.baseline,
                "highLine" to slider.highline,
                "ticks" to slider.sliderTicks,
            ),
            slider.subs,
        )
        assertEquals("s = 3.00", assertIs<Text>(slider.label).plaintext)

        slider.setValue(8.0)
        board.fullUpdate()
        assertPoint(3.2, 2.6, slider)
        assertEquals(8.0, slider.Value())
        assertEquals("s = 8.00", assertIs<Text>(slider.label).plaintext)

        slider.setMin(-20.0).setMax(20.0).setValue(-5.0)
        board.fullUpdate()
        assertPoint(-1.0, 0.5, slider)
        assertEquals(-5.0, slider.Value())
        assertEquals("s = -5.00", assertIs<Text>(slider.label).plaintext)

        board.removeObject(slider)
        assertTrue(board.objectsList.isEmpty())
    }

    @Test
    fun sliderAppliesSnapWidthAndSnapValuesLikeUpstream() {
        val board = board()
        val slider = slider(
            Slider.create(
                board = board,
                startCoordinates = doubleArrayOf(-4.0, 0.0),
                endCoordinates = doubleArrayOf(4.0, 0.0),
                range = doubleArrayOf(0.0, 2.6, 10.0),
                attributes = SliderAttributes(
                    id = "slider",
                    name = "",
                    withLabel = false,
                    withTicks = false,
                    snapWidth = 2.0,
                    snapValues = doubleArrayOf(1.0, 7.0, 9.0),
                    snapValueDistance = 0.6,
                ),
            ),
        )
        board.fullUpdate()

        assertPoint(-2.4, 0.0, slider)
        assertEquals(0.2, slider.position)
        assertEquals(2.0, slider.Value())
        assertNull(slider.label)
        assertNull(slider.sliderTicks)

        slider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.44, 0.0),
        )
        board.update(draggedElement = slider)
        assertPoint(1.44, 0.0, slider)
        assertEquals(0.7, slider.position)
        assertEquals(8.0, slider.Value())
    }

    @Test
    fun unsupportedHostAndDuplicateIdsRollbackAtomically() {
        val board = board()
        val unsupported = point(board, 0.0, 0.0, "host")
        val beforeUnsupported = board.objects.keys.toList()

        val hostError = assertIs<GMResult.Err<GliderError.UnsupportedSlideObject>>(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 1.0),
                slideObject = unsupported,
                id = "candidate",
            ),
        ).error
        assertEquals("point", hostError.elementType)
        assertEquals(beforeUnsupported, board.objects.keys.toList())

        point(board, 3.0, 3.0, "collision")
        val beforeSlider = board.objects.keys.toList()
        val duplicate = assertIs<GMResult.Err<SliderError.GliderFactory>>(
            Slider.create(
                board = board,
                startCoordinates = doubleArrayOf(-2.0, 0.0),
                endCoordinates = doubleArrayOf(2.0, 0.0),
                range = doubleArrayOf(0.0, 1.0, 2.0),
                attributes = SliderAttributes(
                    id = "collision",
                    name = "",
                ),
            ),
        ).error
        assertIs<GliderError.Registration>(duplicate.error)
        assertEquals(beforeSlider, board.objects.keys.toList())
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        id = "board",
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
            ),
        ).value

    private fun segment(
        board: Board,
        point1: Point,
        point2: Point,
        id: String,
    ): Line =
        assertIs<GMResult.Ok<Line>>(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = "",
            ),
        ).value

    private fun glider(
        result: GMResult<Glider, GliderError>,
    ): Glider = assertIs<GMResult.Ok<Glider>>(result).value

    private fun slider(
        result: GMResult<Slider, SliderError>,
    ): Slider = assertIs<GMResult.Ok<Slider>>(result).value

    private fun assertPoint(
        x: Double,
        y: Double,
        point: Point,
    ) {
        assertEquals(x, point.X(), absoluteTolerance = TOLERANCE)
        assertEquals(y, point.Y(), absoluteTolerance = TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
