/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ParabolaTest {
    @Test
    fun parabolaMatchesOfficialCoordinatesMetadataAndUpdates() {
        val board = board("parabola")
        val directrixPoint1 = point(board, -1.0, 4.0, "line1")
        val directrixPoint2 = point(board, -1.0, -4.0, "line2")
        val directrix = line(
            board,
            directrixPoint1,
            directrixPoint2,
            "directrix",
        )
        val focus = point(board, 1.0, 1.0, "focus")
        val parabola = parabola(
            Parabola.create(
                board = board,
                focus = focus,
                directrix = directrix,
                id = "parabola",
                name = "point-parabola",
                needsRegularUpdate = false,
            ),
        )
        val center = assertIs<Point>(parabola.center)

        assertEquals("curve", parabola.elType)
        assertEquals(Const.OBJECT_TYPE_CONIC, parabola.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, parabola.elementClass)
        assertEquals("parameter", parabola.curveType)
        assertEquals("parabola", parabola.id)
        assertEquals("point-parabola", parabola.name)
        assertFalse(parabola.needsRegularUpdate)
        assertTrue(parabola.isParabola)
        assertFalse(parabola.isEllipse)
        assertFalse(parabola.isHyperbola)
        assertTrue(parabola.isDraggable)
        assertEquals(Curve.DEFAULT_SAMPLE_COUNT, parabola.numberPoints)
        assertEquals(0.0, parabola.minX(), absoluteTolerance = TOLERANCE)
        assertEquals(2.0 * PI, parabola.maxX(), absoluteTolerance = TOLERANCE)
        assertEquals(
            1.0000000000000002,
            parabola.X(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(-1.0, parabola.Y(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(
            0.0,
            parabola.X(-0.5 * PI),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            0.9999999999999999,
            parabola.Y(-0.5 * PI),
            TOLERANCE,
        )
        assertTrue(parabola.X(0.5 * PI).isInfinite())
        assertTrue(parabola.Y(0.5 * PI).isNaN())
        assertEquals(-1.0, center.X(), absoluteTolerance = TOLERANCE)
        assertEquals(1.0, center.Y(), absoluteTolerance = TOLERANCE)
        assertEquals(Const.OBJECT_TYPE_CAS, center.type)
        assertFalse(center.isDraggable)
        assertEquals(emptyList(), center.parents)
        assertSame(center, parabola.midpoint)
        assertSame(center, parabola.subs["center"])
        assertSame(focus, parabola.parabolaFocus)
        assertSame(directrix, parabola.parabolaDirectrix)
        assertEquals(
            listOf<GeometryElement>(center, focus),
            parabola.inherits,
        )
        assertEquals(listOf("focus", "directrix"), parabola.parents)
        for (parent in listOf(center, focus, directrix)) {
            assertSame(parabola, parent.childElements[parabola.id])
        }
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(-1.0, 2.0, 1.0),
                doubleArrayOf(2.0, -0.0, 0.0),
                doubleArrayOf(1.0, 0.0, -1.0),
            ),
            actual = parabola.quadraticform,
        )

        focus.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, 2.0),
        )
        directrixPoint1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 3.0),
        )
        directrixPoint2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, -3.0),
        )
        board.fullUpdate()

        assertEquals(
            -1.2000000000000002,
            center.X(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(0.6, center.Y(), absoluteTolerance = TOLERANCE)
        assertEquals(4.4, parabola.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(
            -2.200000000000001,
            parabola.Y(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(
                    -12.099999999999998,
                    3.8999999999999995,
                    2.3,
                ),
                doubleArrayOf(
                    3.8999999999999995,
                    -0.1,
                    0.3,
                ),
                doubleArrayOf(
                    2.3,
                    0.3,
                    -0.8999999999999999,
                ),
            ),
            actual = parabola.quadraticform,
        )
    }

    @Test
    fun parentOwnershipRemovalAndFailuresAreStructured() {
        val outputBoard = board("output")
        val focus = point(outputBoard, 2.0, 1.0)
        val linePoint1 = point(outputBoard, -1.0, 3.0)
        val linePoint2 = point(outputBoard, -1.0, -3.0)
        val directrix = line(outputBoard, linePoint1, linePoint2)
        val output = parabola(
            Parabola.create(
                board = outputBoard,
                focus = focus,
                directrix = directrix,
                parentlessElements = setOf(focus, directrix),
                id = "parabola",
                name = "",
            ),
        )
        val center = assertIs<Point>(output.center)

        assertEquals(emptyList(), output.parents)
        outputBoard.removeObject(output)
        for (element in listOf(focus, linePoint1, linePoint2, directrix, center)) {
            assertSame(element, outputBoard.elementById(element.id))
        }

        val parentBoard = board("parent-removal")
        val parentFocus = point(parentBoard, 2.0, 1.0, "focus")
        val parentLine = line(
            parentBoard,
            point(parentBoard, -1.0, 3.0),
            point(parentBoard, -1.0, -3.0),
            "directrix",
        )
        val parentParabola = parabola(
            Parabola.create(
                board = parentBoard,
                focus = parentFocus,
                directrix = parentLine,
                id = "parabola",
                name = "",
            ),
        )
        val parentCenter = assertIs<Point>(parentParabola.center)
        parentBoard.removeObject(parentLine)

        assertEquals(null, parentBoard.elementById(parentLine.id))
        assertEquals(null, parentBoard.elementById(parentParabola.id))
        assertSame(parentCenter, parentBoard.elementById(parentCenter.id))
        assertSame(parentFocus, parentBoard.elementById(parentFocus.id))

        val otherBoard = board("other")
        val foreignLine = line(
            otherBoard,
            point(otherBoard, 0.0, 1.0),
            point(otherBoard, 0.0, -1.0),
        )
        assertEquals(
            ParabolaError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<ParabolaError>>(
                Parabola.create(
                    board = parentBoard,
                    focus = parentFocus,
                    directrix = foreignLine,
                ),
            ).error,
        )

        val duplicate = point(parentBoard, 7.0, 5.0, "duplicate")
        val duplicateDirectrix = line(
            parentBoard,
            point(parentBoard, 0.0, 1.0),
            point(parentBoard, 0.0, -1.0),
        )
        val beforeDuplicate = parentBoard.objects.keys.toSet()
        assertEquals(
            ParabolaError.DuplicateElementId(duplicate.id),
            assertIs<GMResult.Err<ParabolaError>>(
                Parabola.create(
                    board = parentBoard,
                    focus = parentFocus,
                    directrix = duplicateDirectrix,
                    id = duplicate.id,
                ),
            ).error,
        )
        assertEquals(beforeDuplicate, parentBoard.objects.keys)

        val beforeInvalidSampleCount = parentBoard.objects.keys.toSet()
        val invalidSampleCount = assertIs<GMResult.Err<ParabolaError>>(
            Parabola.create(
                board = parentBoard,
                focus = parentFocus,
                directrix = duplicateDirectrix,
                sampleCount = 0,
            ),
        )
        assertEquals(
            CurveError.InvalidSampleCount(0, Curve.MAX_SAMPLE_COUNT),
            assertIs<ParabolaError.CurveCreation>(
                invalidSampleCount.error,
            ).error,
        )
        assertEquals(beforeInvalidSampleCount, parentBoard.objects.keys)
    }

    @Test
    fun degenerateAndIdealDirectricesPreserveJavaScriptArithmetic() {
        val onLine = parabola(
            board = board("on-line"),
            focusCoordinates = doubleArrayOf(0.0, 1.0),
            firstCoordinates = doubleArrayOf(0.0, 3.0),
            secondCoordinates = doubleArrayOf(0.0, -3.0),
        )
        assertEquals(0.0, onLine.X(0.0))
        assertEquals(1.0, onLine.Y(0.0))
        assertTrue(onLine.X(0.5 * PI).isNaN())
        assertTrue(onLine.Y(0.5 * PI).isNaN())

        val coincident = parabola(
            board = board("coincident"),
            focusCoordinates = doubleArrayOf(2.0, 1.0),
            firstCoordinates = doubleArrayOf(0.0, 0.0),
            secondCoordinates = doubleArrayOf(0.0, 0.0),
        )
        assertTrue(coincident.X(0.0).isNaN())
        assertTrue(coincident.Y(0.0).isNaN())
        assertTrue(coincident.quadraticform.all { row ->
            row.all(Double::isNaN)
        })

        val idealFirst = parabola(
            board = board("ideal-first"),
            focusCoordinates = doubleArrayOf(2.0, 1.0),
            firstCoordinates = doubleArrayOf(0.0, 1.0, 0.0),
            secondCoordinates = doubleArrayOf(1.0, 0.0, 0.0),
        )
        assertEquals(
            1.0,
            idealFirst.X(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            0.9999999999999999,
            idealFirst.Y(0.0),
            absoluteTolerance = TOLERANCE,
        )

        val idealSecond = parabola(
            board = board("ideal-second"),
            focusCoordinates = doubleArrayOf(2.0, 1.0),
            firstCoordinates = doubleArrayOf(1.0, 0.0, 0.0),
            secondCoordinates = doubleArrayOf(0.0, 1.0, 0.0),
        )
        assertEquals(
            3.0,
            idealSecond.X(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            1.0,
            idealSecond.Y(0.0),
            absoluteTolerance = TOLERANCE,
        )
    }

    private fun parabola(
        board: Board,
        focusCoordinates: DoubleArray,
        firstCoordinates: DoubleArray,
        secondCoordinates: DoubleArray,
    ): Curve {
        val focus = point(board, focusCoordinates)
        val directrix = line(
            board,
            point(board, firstCoordinates),
            point(board, secondCoordinates),
        )
        return parabola(
            Parabola.create(
                board = board,
                focus = focus,
                directrix = directrix,
                sampleCount = 8,
                name = "",
            ),
        )
    }

    private fun assertMatrixEquals(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (rowIndex in expected.indices) {
            assertEquals(expected[rowIndex].size, actual[rowIndex].size)
            for (columnIndex in expected[rowIndex].indices) {
                assertEquals(
                    expected[rowIndex][columnIndex],
                    actual[rowIndex][columnIndex],
                    absoluteTolerance = TOLERANCE,
                    message = "[$rowIndex][$columnIndex]",
                )
            }
        }
    }

    private fun board(id: String): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = id,
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String = "",
    ): Point = point(board, doubleArrayOf(x, y), id)

    private fun point(
        board: Board,
        coordinates: DoubleArray,
        id: String = "",
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = coordinates,
            id = id,
            name = "",
        ),
    ).value

    private fun line(
        board: Board,
        first: Point,
        second: Point,
        id: String = "",
    ): Line = assertIs<GMResult.Ok<Line>>(
        Line.create(
            board = board,
            point1 = first,
            point2 = second,
            id = id,
            name = "",
        ),
    ).value

    private fun parabola(
        result: GMResult<Curve, ParabolaError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
