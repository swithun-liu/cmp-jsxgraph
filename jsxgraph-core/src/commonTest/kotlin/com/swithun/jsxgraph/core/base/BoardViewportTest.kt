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
    fun viewportNavigationMatchesOfficialFixture() {
        val board = navigationBoard()

        assertViewport(
            board = board,
            boundingBox = doubleArrayOf(-5.0, 4.0, 5.0, -4.0),
            origin = doubleArrayOf(1.0, 250.0, 200.0),
            unitX = 50.0,
            unitY = 50.0,
            zoomX = 1.0,
            zoomY = 1.0,
        )

        board.initMoveOrigin(250.0, 200.0)
        board.moveOrigin(300.0, 230.0, diff = true)
        assertViewport(
            board = board,
            boundingBox = doubleArrayOf(-6.0, 4.6, 4.0, -3.4),
            origin = doubleArrayOf(1.0, 300.0, 230.0),
            unitX = 50.0,
            unitY = 50.0,
            zoomX = 1.0,
            zoomY = 1.0,
        )

        assertIs<GMResult.Ok<Board>>(
            board.setBoundingBox(
                bbox = doubleArrayOf(-5.0, 4.0, 5.0, -4.0),
                setZoom = BoardZoomMode.RESET,
            ),
        )
        board.zoomIn(userX = 2.0, userY = 1.0)
        assertViewport(
            board = board,
            boundingBox = doubleArrayOf(-3.6, 3.4, 4.4, -3.0),
            origin = doubleArrayOf(1.0, 225.0, 212.5),
            unitX = 62.5,
            unitY = 62.5,
            zoomX = 1.25,
            zoomY = 1.25,
        )

        board.zoomOut(userX = 2.0, userY = 1.0)
        board.clickLeftArrow()
        assertBoundingBox(
            expected = doubleArrayOf(-6.0, 4.0, 4.0, -4.0),
            actual = board.getBoundingBox(),
        )
        board.clickRightArrow()
        board.clickUpArrow()
        assertBoundingBox(
            expected = doubleArrayOf(-5.0, 3.2, 5.0, -4.8),
            actual = board.getBoundingBox(),
        )
        board.clickDownArrow()
        assertBoundingBox(
            expected = doubleArrayOf(-5.0, 4.0, 5.0, -4.0),
            actual = board.getBoundingBox(),
        )
    }

    @Test
    fun contentZoomAndMaximumBoundingBoxMatchOfficialFixture() {
        val pointsBoard = navigationBoard()
        val first = registerPoint(pointsBoard, "first", -7.0, 3.0)
        val second = registerPoint(pointsBoard, "second", 4.0, -5.0)
        pointsBoard.zoomAllPoints()
        assertViewport(
            board = pointsBoard,
            boundingBox = doubleArrayOf(-8.0, 4.0, 5.0, -6.0),
            origin = doubleArrayOf(
                1.0,
                307.6923076923077,
                160.0,
            ),
            unitX = 38.46153846153846,
            unitY = 40.0,
            zoomX = 0.7692307692307692,
            zoomY = 0.8,
        )

        val elementsBoard = navigationBoard()
        val selectedFirst =
            registerPoint(elementsBoard, "selectedFirst", -2.0, 1.0)
        val selectedSecond =
            registerPoint(elementsBoard, "selectedSecond", 4.0, -3.0)
        elementsBoard.zoomElements(
            listOf(selectedFirst, selectedSecond),
        )
        assertViewport(
            board = elementsBoard,
            boundingBox = doubleArrayOf(-3.5, 3.5, 5.5, -5.5),
            origin = doubleArrayOf(
                1.0,
                194.44444444444446,
                155.55555555555554,
            ),
            unitX = 55.55555555555556,
            unitY = 44.44444444444444,
            zoomX = 1.1111111111111112,
            zoomY = 0.8888888888888888,
        )

        val boundedBoard = navigationBoard().apply {
            maxBoundingBox = doubleArrayOf(-6.0, 5.0, 6.0, -5.0)
        }
        assertIs<GMResult.Ok<Board>>(
            boundedBoard.setBoundingBox(
                bbox = doubleArrayOf(-7.0, 4.0, 5.0, -4.0),
                setZoom = BoardZoomMode.UPDATE,
            ),
        )
        assertViewport(
            board = boundedBoard,
            boundingBox = doubleArrayOf(-5.0, 4.0, 5.0, -4.0),
            origin = doubleArrayOf(1.0, 250.0, 200.0),
            unitX = 50.0,
            unitY = 50.0,
            zoomX = 1.0,
            zoomY = 1.0,
        )
        assertIs<GMResult.Ok<Board>>(
            boundedBoard.setBoundingBox(
                bbox = doubleArrayOf(-4.0, 3.0, 4.0, -3.0),
                setZoom = BoardZoomMode.UPDATE,
            ),
        )
        assertViewport(
            board = boundedBoard,
            boundingBox = doubleArrayOf(-4.0, 3.0, 4.0, -3.0),
            origin = doubleArrayOf(1.0, 250.0, 200.0),
            unitX = 62.5,
            unitY = 66.66666666666667,
            zoomX = 1.25,
            zoomY = 1.3333333333333335,
        )

        assertTrue(first.coords.isReal())
        assertTrue(second.coords.isReal())
    }

    @Test
    fun keyboardNavigationMatchesOfficialFixture() {
        val board = navigationBoard()
        val point = registerPoint(board, "keyboardPoint", 0.0, 0.0)
        board.keyFocusInListener(point)

        assertTrue(
            board.keyDownListener(
                BoardInputEvent(
                    screenX = 0.0,
                    screenY = 0.0,
                    key = "ArrowRight",
                ),
            ),
        )
        assertEquals(0.2, point.X(), ABSOLUTE_TOLERANCE)
        assertEquals(0.0, point.Y(), ABSOLUTE_TOLERANCE)
        assertTrue(
            board.keyDownListener(
                BoardInputEvent(
                    screenX = 0.0,
                    screenY = 0.0,
                    key = "ArrowUp",
                ),
            ),
        )
        assertEquals(0.2, point.X(), ABSOLUTE_TOLERANCE)
        assertEquals(0.2, point.Y(), ABSOLUTE_TOLERANCE)

        assertTrue(
            board.keyDownListener(
                BoardInputEvent(
                    screenX = 0.0,
                    screenY = 0.0,
                    shiftKey = true,
                    key = "ArrowLeft",
                ),
            ),
        )
        assertBoundingBox(
            expected = doubleArrayOf(-6.0, 4.0, 4.0, -4.0),
            actual = board.getBoundingBox(),
        )
        assertTrue(
            board.keyDownListener(
                BoardInputEvent(
                    screenX = 0.0,
                    screenY = 0.0,
                    shiftKey = true,
                    key = "+",
                ),
            ),
        )
        assertViewport(
            board = board,
            boundingBox = doubleArrayOf(-5.0, 3.2, 3.0, -3.2),
            origin = doubleArrayOf(1.0, 312.5, 200.0),
            unitX = 62.5,
            unitY = 62.5,
            zoomX = 1.25,
            zoomY = 1.25,
        )
        assertTrue(
            board.keyDownListener(
                BoardInputEvent(
                    screenX = 0.0,
                    screenY = 0.0,
                    shiftKey = true,
                    key = "o",
                ),
            ),
        )
        assertViewport(
            board = board,
            boundingBox = doubleArrayOf(-5.0, 4.0, 5.0, -4.0),
            origin = doubleArrayOf(1.0, 250.0, 200.0),
            unitX = 50.0,
            unitY = 50.0,
            zoomX = 1.0,
            zoomY = 1.0,
        )
        board.keyFocusOutListener(point)
        assertTrue(board.focusObjects.isEmpty())
    }

    @Test
    fun pointerPanAndWheelZoomHonorDefaultShiftRequirement() {
        val board = navigationBoard()
        val down = BoardInputEvent(
            screenX = 100.0,
            screenY = 100.0,
            pointerId = 1,
            timestampMilliseconds = 100,
        )

        board.pointerDownListener(down)
        assertEquals(Board.BOARD_MODE_NONE, board.mode)
        board.pointerUpListener(
            down.copy(timestampMilliseconds = 150),
        )
        board.pointerDownListener(
            down.copy(
                pointerId = 2,
                shiftKey = true,
                timestampMilliseconds = 200,
            ),
        )
        assertEquals(Board.BOARD_MODE_MOVE_ORIGIN, board.mode)
        board.pointerMoveListener(
            down.copy(
                screenX = 150.0,
                screenY = 120.0,
                pointerId = 2,
                shiftKey = true,
                timestampMilliseconds = 250,
            ),
        )
        assertBoundingBox(
            expected = doubleArrayOf(-6.0, 4.4, 4.0, -3.6),
            actual = board.getBoundingBox(),
        )
        board.pointerUpListener(
            down.copy(
                pointerId = 2,
                shiftKey = true,
                timestampMilliseconds = 300,
            ),
        )

        val beforeWheel = board.getBoundingBox()
        board.mouseWheelListener(
            BoardInputEvent(
                screenX = 250.0,
                screenY = 200.0,
                deltaY = -1.0,
            ),
        )
        assertBoundingBox(beforeWheel, board.getBoundingBox())
        board.mouseWheelListener(
            BoardInputEvent(
                screenX = 250.0,
                screenY = 200.0,
                shiftKey = true,
                deltaY = -1.0,
            ),
        )
        assertEquals(1.25, board.zoomX, ABSOLUTE_TOLERANCE)
        assertEquals(1.25, board.zoomY, ABSOLUTE_TOLERANCE)
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
    fun pointerHoverMatchesHighlightMoveOutAndFocusLifecycle() {
        val board = board()
        val point = registerPoint(
            board = board,
            id = "point",
            x = 0.0,
            y = 0.0,
        )
        point.setAttribute(
            mapOf(
                "highlight" to true,
                "size" to 8.0,
            ),
        )
        val events = mutableListOf<String>()
        for (eventName in listOf("over", "move", "out")) {
            point.on(
                eventName,
                EventHandler { _, _ -> events += eventName },
            )
        }

        assertTrue(
            board.pointerMoveListener(
                BoardInputEvent(
                    screenX = 250.0,
                    screenY = 250.0,
                    timestampMilliseconds = 100,
                ),
            ),
        )
        assertTrue(point.highlighted)
        assertTrue(point.mouseover)
        assertEquals(listOf("point"), board.highlightedObjects.keys.toList())
        assertEquals(listOf("over"), events)

        board.pointerMoveListener(
            BoardInputEvent(
                screenX = 250.0,
                screenY = 250.0,
                timestampMilliseconds = 150,
            ),
        )
        assertTrue(point.highlighted)
        assertEquals(listOf("over", "move"), events)

        board.pointerMoveListener(
            BoardInputEvent(
                screenX = 400.0,
                screenY = 400.0,
                timestampMilliseconds = 200,
            ),
        )
        assertFalse(point.highlighted)
        assertFalse(point.mouseover)
        assertTrue(board.highlightedObjects.isEmpty())
        assertEquals(listOf("over", "move", "out"), events)

        board.pointerMoveListener(
            BoardInputEvent(
                screenX = 250.0,
                screenY = 250.0,
                timestampMilliseconds = 250,
            ),
        )
        board.pointerLeaveListener(
            BoardInputEvent(
                screenX = 250.0,
                screenY = 250.0,
                timestampMilliseconds = 300,
            ),
        )
        assertFalse(point.highlighted)
        assertTrue(board.highlightedObjects.isEmpty())

        board.keyFocusInListener(point)
        board.dehighlightAll()
        assertTrue(point.highlighted)
        assertEquals(listOf("point"), board.highlightedObjects.keys.toList())

        board.keyFocusOutListener(point)
        assertFalse(point.highlighted)
        assertTrue(board.highlightedObjects.isEmpty())
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
                    BoardInputEvent(185.0, 250.0),
                    BoardInputEvent(315.0, 250.0),
                ),
            ),
        )
        assertBoundingBox(
            expected = doubleArrayOf(
                -3.846153846153846,
                5.0,
                3.8461538461538467,
                -5.0,
            ),
            actual = board.getBoundingBox(),
        )
    }

    @Test
    fun twoFingerTransformsMatchOfficialFixture() {
        val matrixBoard = navigationBoard()
        assertBoundingBox(
            expected = doubleArrayOf(
                1.0, 0.0, 0.0,
                1.0, 1.0, -0.5,
                0.0, 0.5, 1.0,
            ),
            actual = matrixBoard.getTwoFingerTransform(
                first = touchTarget(
                    pointerId = 1,
                    previousX = 150.0,
                    previousY = 200.0,
                    x = 200.0,
                    y = 250.0,
                ),
                second = touchTarget(
                    pointerId = 2,
                    previousX = 350.0,
                    previousY = 200.0,
                    x = 400.0,
                    y = 150.0,
                ),
                scalable = true,
                rotatable = true,
            ),
        )
        assertBoundingBox(
            expected = doubleArrayOf(
                1.0, 0.0, 0.0,
                1.0, 0.8944271909999159, 0.0,
                0.0, 0.0, 0.8944271909999159,
            ),
            actual = matrixBoard.getTwoFingerTransform(
                first = touchTarget(
                    pointerId = 1,
                    previousX = 150.0,
                    previousY = 200.0,
                    x = 200.0,
                    y = 250.0,
                ),
                second = touchTarget(
                    pointerId = 2,
                    previousX = 350.0,
                    previousY = 200.0,
                    x = 400.0,
                    y = 150.0,
                ),
                scalable = false,
                rotatable = false,
            ),
        )

        val lineBoard = navigationBoard()
        val lineFirst = registerPoint(lineBoard, "lineFirst", -1.0, 0.0)
        val lineSecond = registerPoint(lineBoard, "lineSecond", 1.0, 0.0)
        val line = assertIs<GMResult.Ok<Line>>(
            Line.create(lineBoard, lineFirst, lineSecond),
        ).value
        line.setAttribute(
            mapOf("scalable" to true, "rotatable" to true),
        )
        lineBoard.twoFingerTouchObject(
            targets = fixtureTouchTargets(),
            element = line,
        )
        assertPoint(lineFirst, 0.0, -0.5)
        assertPoint(lineSecond, 2.0, 0.5)

        val polygonBoard = navigationBoard()
        val polygonVertices = listOf(
            registerPoint(polygonBoard, "polygonFirst", -1.0, -1.0),
            registerPoint(polygonBoard, "polygonSecond", 1.0, -1.0),
            registerPoint(polygonBoard, "polygonThird", 0.0, 1.0),
        )
        val polygon = assertIs<GMResult.Ok<Polygon>>(
            Polygon.create(
                board = polygonBoard,
                vertices = polygonVertices,
            ),
        ).value
        polygon.setAttribute(
            mapOf("scalable" to true, "rotatable" to true),
        )
        polygonBoard.twoFingerTouchObject(
            targets = fixtureTouchTargets(),
            element = polygon,
        )
        assertPoint(polygonVertices[0], 0.5, -1.5)
        assertPoint(polygonVertices[1], 2.5, -0.5)
        assertPoint(polygonVertices[2], 0.5, 1.0)

        val snappedPolygonBoard = navigationBoard()
        val snappedVertices = listOf(
            registerPoint(
                snappedPolygonBoard,
                "snappedFirst",
                -1.0,
                -1.0,
            ),
            registerPoint(
                snappedPolygonBoard,
                "snappedSecond",
                1.0,
                -1.0,
            ),
            registerPoint(
                snappedPolygonBoard,
                "snappedThird",
                0.0,
                1.0,
            ),
        )
        val snappedPolygon = assertIs<GMResult.Ok<Polygon>>(
            Polygon.create(
                board = snappedPolygonBoard,
                vertices = snappedVertices,
            ),
        ).value
        snappedPolygon.setAttribute(
            mapOf("scalable" to true, "rotatable" to true),
        )
        snappedVertices[1].setAttribute(mapOf("snaptogrid" to true))
        snappedPolygonBoard.twoFingerTouchObject(
            targets = fixtureTouchTargets(),
            element = snappedPolygon,
        )
        assertPoint(snappedVertices[0], -1.0, -1.0)
        assertPoint(snappedVertices[1], 1.0, -1.0)
        assertPoint(snappedVertices[2], 0.0, 1.0)

        val circleBoard = navigationBoard()
        val center = registerPoint(circleBoard, "center", 0.0, 0.0)
        val point2 = registerPoint(circleBoard, "point2", 1.0, 0.0)
        val circle = assertIs<GMResult.Ok<Circle>>(
            Circle.create(circleBoard, center, point2),
        ).value
        circle.setAttribute(mapOf("scalable" to true))
        circleBoard.twoFingerTouchCircle(
            targets = fixtureTouchTargets(),
            circle = circle,
            movedPointerId = 2,
        )
        assertPoint(center, 0.5, 0.5)
        assertPoint(point2, 1.75, 0.75)
        assertEquals(
            1.2747548783981961,
            circle.Radius(),
            ABSOLUTE_TOLERANCE,
        )

        val radiusBoard = navigationBoard()
        val radiusCenter =
            registerPoint(radiusBoard, "radiusCenter", 0.0, 0.0)
        val radiusCircle = assertIs<GMResult.Ok<Circle>>(
            Circle.create(radiusBoard, radiusCenter, radius = 1.0),
        ).value
        radiusCircle.setAttribute(mapOf("scalable" to true))
        radiusBoard.twoFingerTouchCircle(
            targets = fixtureTouchTargets(),
            circle = radiusCircle,
            movedPointerId = 2,
        )
        assertPoint(radiusCenter, 0.5, 0.5)
        assertEquals(
            1.2747548783981961,
            radiusCircle.Radius(),
            ABSOLUTE_TOLERANCE,
        )

        val panBoard = navigationBoard().apply {
            panNeedsTwoFingers = true
        }
        assertTrue(
            panBoard.gestureStartListener(
                listOf(
                    BoardInputEvent(200.0, 200.0),
                    BoardInputEvent(300.0, 200.0),
                ),
            ),
        )
        assertTrue(
            panBoard.gestureChangeListener(
                listOf(
                    BoardInputEvent(220.0, 220.0),
                    BoardInputEvent(320.0, 220.0),
                ),
            ),
        )
        assertViewport(
            board = panBoard,
            boundingBox = doubleArrayOf(-5.4, 4.4, 4.6, -3.6),
            origin = doubleArrayOf(1.0, 270.0, 220.0),
            unitX = 50.0,
            unitY = 50.0,
            zoomX = 1.0,
            zoomY = 1.0,
        )
    }

    @Test
    fun pointerListenersRetainObjectOwnershipUntilAllTouchesEnd() {
        val board = navigationBoard()
        val first = registerPoint(board, "first", -1.0, 0.0)
        val second = registerPoint(board, "second", 1.0, 0.0)
        val line = assertIs<GMResult.Ok<Line>>(
            Line.create(board, first, second),
        ).value
        line.setAttribute(
            mapOf(
                "visible" to true,
                "scalable" to true,
                "rotatable" to true,
            ),
        )
        line.updateVisibility()

        touchDown(board, pointerId = 1, x = 150.0, y = 200.0)
        touchDown(board, pointerId = 2, x = 350.0, y = 200.0)
        assertEquals(1, board.touches.size)
        assertEquals(2, board.touches.single().targets.size)

        touchMove(board, pointerId = 1, x = 150.0, y = 200.0, time = 110)
        touchMove(board, pointerId = 2, x = 350.0, y = 200.0, time = 120)
        touchMove(board, pointerId = 1, x = 200.0, y = 250.0, time = 130)
        touchMove(board, pointerId = 2, x = 400.0, y = 150.0, time = 140)
        assertPoint(first, 0.0, -0.5)
        assertPoint(second, 2.0, 0.5)

        touchUp(board, pointerId = 1, x = 200.0, y = 250.0)
        assertEquals(Board.BOARD_MODE_NONE, board.mode)
        assertEquals(1, board.touches.single().targets.size)
        touchUp(board, pointerId = 2, x = 400.0, y = 150.0)
        assertEquals(Board.BOARD_MODE_NONE, board.mode)
        assertTrue(board.touches.isEmpty())
        assertTrue(board.pointerPositions.isEmpty())
    }

    @Test
    fun firstSingleTouchMoveSeedsNonPointDragWithoutJumping() {
        val board = navigationBoard()
        val first = registerPoint(board, "first", -1.0, 0.0)
        val second = registerPoint(board, "second", 1.0, 0.0)
        val line = assertIs<GMResult.Ok<Line>>(
            Line.create(board, first, second),
        ).value
        line.setAttribute(mapOf("visible" to true))
        line.updateVisibility()

        touchDown(board, pointerId = 1, x = 150.0, y = 200.0)
        assertEquals(line, board.touches.single().element)
        touchMove(board, pointerId = 1, x = 160.0, y = 200.0, time = 110)
        assertEquals(
            160.0,
            board.touches.single().targets.single().previousScreenX,
        )
        assertPoint(first, -1.0, 0.0)
        assertPoint(second, 1.0, 0.0)

        touchMove(board, pointerId = 1, x = 170.0, y = 200.0, time = 150)
        assertEquals(
            170.0,
            board.touches.single().targets.single().previousScreenX,
        )
        assertPoint(first, -0.8, 0.0)
        assertPoint(second, 1.2, 0.0)
    }

    private fun board(): Board =
        Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            keepAspectRatio = false,
            defaultCurveMinimum = -6.0,
            defaultCurveMaximum = 6.0,
        )

    private fun navigationBoard(): Board =
        Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-5.0, 4.0, 5.0, -4.0),
            keepAspectRatio = false,
            canvasWidth = 500.0,
            canvasHeight = 400.0,
            defaultCurveMinimum = -6.0,
            defaultCurveMaximum = 6.0,
        )

    private fun registerPoint(
        board: Board,
        id: String,
        x: Double,
        y: Double,
    ): Point {
        val point = Point(
            board = board,
            coordinates = doubleArrayOf(x, y),
            id = id,
        )
        assertIs<GMResult.Ok<String>>(
            board.setId(point, Const.OBJECT_TYPE_POINT),
        )
        point.setAttribute(mapOf("visible" to true))
        point.updateVisibility()
        return point
    }

    private fun fixtureTouchTargets(): List<BoardTouchTarget> =
        listOf(
            touchTarget(
                pointerId = 1,
                previousX = 150.0,
                previousY = 200.0,
                x = 200.0,
                y = 250.0,
            ),
            touchTarget(
                pointerId = 2,
                previousX = 350.0,
                previousY = 200.0,
                x = 400.0,
                y = 150.0,
            ),
        )

    private fun touchTarget(
        pointerId: Long,
        previousX: Double,
        previousY: Double,
        x: Double,
        y: Double,
    ): BoardTouchTarget =
        BoardTouchTarget(
            pointerId = pointerId,
            screenX = x,
            screenY = y,
            previousScreenX = previousX,
            previousScreenY = previousY,
        )

    private fun touchDown(
        board: Board,
        pointerId: Long,
        x: Double,
        y: Double,
    ) {
        board.pointerDownListener(
            BoardInputEvent(
                screenX = x,
                screenY = y,
                pointerId = pointerId,
                timestampMilliseconds = 100,
                inputDevice = "touch",
            ),
        )
    }

    private fun touchMove(
        board: Board,
        pointerId: Long,
        x: Double,
        y: Double,
        time: Long,
    ) {
        board.pointerMoveListener(
            BoardInputEvent(
                screenX = x,
                screenY = y,
                pointerId = pointerId,
                timestampMilliseconds = time,
                inputDevice = "touch",
            ),
        )
    }

    private fun touchUp(
        board: Board,
        pointerId: Long,
        x: Double,
        y: Double,
    ) {
        board.pointerUpListener(
            BoardInputEvent(
                screenX = x,
                screenY = y,
                pointerId = pointerId,
                timestampMilliseconds = 200,
                inputDevice = "touch",
            ),
        )
    }

    private fun assertPoint(
        point: Point,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, point.X(), ABSOLUTE_TOLERANCE)
        assertEquals(y, point.Y(), ABSOLUTE_TOLERANCE)
    }

    private fun assertViewport(
        board: Board,
        boundingBox: DoubleArray,
        origin: DoubleArray,
        unitX: Double,
        unitY: Double,
        zoomX: Double,
        zoomY: Double,
    ) {
        assertBoundingBox(boundingBox, board.getBoundingBox())
        assertBoundingBox(origin, board.origin.scrCoords)
        assertEquals(unitX, board.unitX, ABSOLUTE_TOLERANCE)
        assertEquals(unitY, board.unitY, ABSOLUTE_TOLERANCE)
        assertEquals(zoomX, board.zoomX, ABSOLUTE_TOLERANCE)
        assertEquals(zoomY, board.zoomY, ABSOLUTE_TOLERANCE)
    }

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
