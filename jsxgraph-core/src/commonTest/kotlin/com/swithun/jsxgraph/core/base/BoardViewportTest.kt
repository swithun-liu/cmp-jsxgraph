/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.EventHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BoardViewportTest {
    @Test
    fun initBoardExpandsBoundingBoxWhenKeepingAspectRatio() {
        val board = Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
            keepAspectRatio = true,
            defaultCurveMinimum = -9.6,
            defaultCurveMaximum = 9.6,
        )

        assertEquals(31.25, board.unitX)
        assertEquals(31.25, board.unitY)
        assertEquals(250.0, board.origin.scrCoords[1])
        assertEquals(250.0, board.origin.scrCoords[2])
        assertBoundingBox(
            expected = doubleArrayOf(-8.0, 8.0, 8.0, -8.0),
            actual = board.getBoundingBox(),
        )
    }

    @Test
    fun initBoardPreservesRequestedBoxWithoutAspectRatio() {
        val board = Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
            keepAspectRatio = false,
            defaultCurveMinimum = -9.6,
            defaultCurveMaximum = 9.6,
        )

        assertEquals(31.25, board.unitX)
        assertEquals(500.0 / 12.0, board.unitY)
        assertBoundingBox(
            expected = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
            actual = board.getBoundingBox(),
        )
    }

    @Test
    fun zoomAndNavigationPreserveUpstreamViewportMath() {
        val board = board()

        board.zoomIn()
        assertBoundingBox(
            expected = doubleArrayOf(-4.0, 4.0, 4.0, -4.0),
            actual = board.getBoundingBox(),
        )
        assertEquals(1.25, board.zoomX, ABSOLUTE_TOLERANCE)
        assertEquals(1.25, board.zoomY, ABSOLUTE_TOLERANCE)

        board.zoomOut()
        assertBoundingBox(
            expected = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            actual = board.getBoundingBox(),
        )
        board.clickLeftArrow()
        assertBoundingBox(
            expected = doubleArrayOf(-6.0, 5.0, 4.0, -5.0),
            actual = board.getBoundingBox(),
        )
        board.zoom100()
        assertBoundingBox(
            expected = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            actual = board.getBoundingBox(),
        )
    }

    @Test
    fun boardInputCoordinatesLocationsAndFrameRateArePortable() {
        val board = Board(
            originX = 100.0,
            originY = 100.0,
            unitX = 10.0,
            unitY = 10.0,
            canvasWidth = 200.0,
            canvasHeight = 200.0,
            hostAdapter = object : BoardHostAdapter {
                override fun topLeft(): DoubleArray =
                    doubleArrayOf(20.0, 30.0)
            },
        )
        val event = BoardInputEvent(
            screenX = 130.0,
            screenY = 90.0,
            pointerId = 7,
            timestampMilliseconds = 10,
        )

        assertEquals(
            listOf(110.0, 60.0),
            board.getMousePosition(event).toList(),
        )
        assertEquals(
            listOf(1.0, 4.0),
            board.getUsrCoordsOfMouse(event).toList(),
        )
        assertEquals(
            listOf(0, 0),
            board.getLocationOrigin().toList(),
        )
        assertTrue(board.checkFrameRate(event))
        assertFalse(
            board.checkFrameRate(
                event.copy(timestampMilliseconds = 11),
            ),
        )
        assertTrue(
            board.checkFrameRate(
                event.copy(
                    pointerId = 8,
                    timestampMilliseconds = 11,
                ),
            ),
        )
    }

    @Test
    fun boardHooksAttributesAndDependentBoardsFollowUpdateLifecycle() {
        val parent = board()
        val child = board()
        var parentUpdates = 0
        var childUpdates = 0
        val parentHandler = EventHandler { _, _ -> parentUpdates += 1 }
        child.on("update", EventHandler { _, _ -> childUpdates += 1 })
        val hook = parent.addHook(parentHandler)
        parent.addChild(child)
        parent.update()

        assertTrue(parentUpdates >= 2)
        assertTrue(childUpdates >= 2)
        parent.removeHook(hook)
        val previousParentUpdates = parentUpdates
        parent.update()
        assertEquals(previousParentUpdates, parentUpdates)

        parent.setAttribute(
            mapOf(
                "animationDelay" to 20.0,
                "keepAspectRatio" to true,
            ),
        )
        assertEquals(20.0, parent.animationDelay)
        assertTrue(parent.keepAspectRatio)
        parent.removeChild(child)
        assertTrue(parent.dependentBoards.isEmpty())
    }

    @Test
    fun boardResizeHitTestAndPointLocationUseCurrentViewport() {
        val board = board()

        assertTrue(board.hasPoint(0.0, 0.0))
        assertFalse(board.hasPoint(-5.0, 0.0))
        assertEquals(
            listOf(1, 1),
            board.getPointLoc(
                doubleArrayOf(6.0, 6.0),
            ).toList(),
        )

        board.resizeContainer(
            width = 1_000.0,
            height = 250.0,
        )
        assertEquals(1_000.0, board.canvasWidth)
        assertEquals(250.0, board.canvasHeight)
        assertBoundingBox(
            expected = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            actual = board.getBoundingBox(),
        )
    }

    @Test
    fun pointerLifecycleSelectsDragsAndReleasesPoint() {
        val board = board()
        val point = Point(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            id = "point",
        )
        assertEquals(
            "point",
            assertIs<GMResult.Ok<String>>(
                board.setId(point, Const.OBJECT_TYPE_POINT),
            ).value,
        )
        point.setAttribute(
            mapOf(
                "visible" to true,
                "highlight" to true,
                "size" to 8.0,
            ),
        )
        point.updateVisibility()
        var drags = 0
        point.on(
            "drag",
            EventHandler { _, _ -> drags += 1 },
        )

        assertTrue(
            board.pointerDownListener(
                BoardInputEvent(
                    screenX = 250.0,
                    screenY = 250.0,
                    pointerId = 1,
                    timestampMilliseconds = 100,
                ),
            ),
        )
        assertEquals(Board.BOARD_MODE_DRAG, board.mode)
        assertEquals(point, board.activeDragElement)

        board.pointerMoveListener(
            BoardInputEvent(
                screenX = 300.0,
                screenY = 250.0,
                pointerId = 1,
                timestampMilliseconds = 150,
            ),
        )
        assertEquals(1.0, point.X(), ABSOLUTE_TOLERANCE)
        assertEquals(0.0, point.Y(), ABSOLUTE_TOLERANCE)
        assertEquals(1, drags)

        board.pointerUpListener(
            BoardInputEvent(
                screenX = 300.0,
                screenY = 250.0,
                pointerId = 1,
                timestampMilliseconds = 200,
            ),
        )
        assertEquals(Board.BOARD_MODE_NONE, board.mode)
        assertEquals(null, board.activeDragElement)
        assertTrue(board.pointerPositions.isEmpty())
    }

    @Test
    fun selectionAndGestureAdaptersProduceStableGeometry() {
        val board = board()
        board.startSelectionMode()
        board._startSelecting(doubleArrayOf(10.0, 20.0))
        board._moveSelecting(doubleArrayOf(40.0, 60.0))

        val polygon = board._stopSelecting()
        assertEquals(
            listOf(
                listOf(10.0, 20.0),
                listOf(40.0, 20.0),
                listOf(40.0, 60.0),
                listOf(10.0, 60.0),
            ),
            polygon.map(DoubleArray::toList),
        )
        board.stopSelectionMode()
        assertFalse(board.selectingMode)

        assertTrue(
            board.gestureStartListener(
                listOf(
                    BoardInputEvent(200.0, 250.0),
                    BoardInputEvent(300.0, 250.0),
                ),
            ),
        )
        assertTrue(
            board.gestureChangeListener(
                listOf(
                    BoardInputEvent(150.0, 250.0),
                    BoardInputEvent(350.0, 250.0),
                ),
            ),
        )
        assertBoundingBox(
            expected = doubleArrayOf(-2.5, 2.5, 2.5, -2.5),
            actual = board.getBoundingBox(),
        )
    }

    private fun board(): Board =
        Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            keepAspectRatio = false,
            defaultCurveMinimum = -6.0,
            defaultCurveMaximum = 6.0,
        )

    private fun assertBoundingBox(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                ABSOLUTE_TOLERANCE,
            )
        }
    }

    private companion object {
        const val ABSOLUTE_TOLERANCE = 1e-12
    }
}
