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
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SlopeTriangleTest {
    @Test
    fun tangentParentCreatesOfficialHelpersAndTracksUpdates() {
        val board = board()
        val start = point(board, -3.0, -1.0, "start")
        val end = point(board, 3.0, 2.0, "end")
        val source = segment(board, start, end, "source")
        val sourceGlider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.5),
                slideObject = source,
                id = "sourceGlider",
                name = "",
            ),
        )
        val tangent = tangent(
            Tangent.create(
                board = board,
                firstParent = source,
                secondParent = sourceGlider,
                id = "tangent",
                name = "",
            ),
        )

        val triangle = slopeTriangle(
            SlopeTriangle.create(
                board = board,
                parents = listOf(tangent),
                attributes = attributes("triangle"),
            ),
        )
        board.fullUpdate()
        val definition = assertIs<SlopeTriangleDefinition>(
            triangle.slopeTriangleDefinition,
        )

        assertEquals("slopetriangle", triangle.elType)
        assertFalse(definition.isPrivateTangent)
        assertSame(tangent, definition.tangent)
        assertSame(sourceGlider, definition.tangentPoint)
        assertEquals(0.5, definition.Slope(), TOLERANCE)
        assertEquals(
            0.4636476090008061,
            definition.getAngle(),
            TOLERANCE,
        )
        assertEquals(
            26.56505117707799,
            angle(definition.getAngle("degrees")),
            TOLERANCE,
        )
        assertEquals(1.0, definition.DeltaX(), TOLERANCE)
        assertEquals(0.5, definition.DeltaY(), TOLERANCE)
        assertContentEquals(
            doubleArrayOf(6.0, 3.0),
            definition.Direction(),
        )

        assertEquals(
            listOf(
                "triangleBase",
                "triangleBaseline",
                "triangleGlider",
                "triangleTop",
                definition.borderVertical.id,
                definition.borderParallel.id,
                definition.borderHorizontal.id,
                "triangle",
                "${definition.borderVertical.id}Label",
            ),
            board.objectsList.drop(5).map(GeometryElement::id),
        )
        assertEquals(
            listOf(
                sourceGlider,
                definition.glider,
                definition.topPoint,
                sourceGlider,
            ),
            triangle.vertices,
        )
        assertSame(triangle.borders[0], definition.borderHorizontal)
        assertSame(triangle.borders[1], definition.borderVertical)
        assertSame(triangle.borders[2], definition.borderParallel)
        assertTrue(definition.glider.isFixed)
        assertEquals("m=0.500!", definition.label.plaintext)
        assertEquals(
            mapOf(
                "glider" to definition.glider,
                "basePoint" to definition.basePoint,
                "baseLine" to definition.baseLine,
                "topPoint" to definition.topPoint,
                "label" to definition.label,
            ),
            triangle.subs,
        )

        definition.glider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-1.5, 0.5),
        )
        board.update(draggedElement = definition.glider)
        assertEquals(-1.5, definition.DeltaX(), TOLERANCE)
        assertEquals(-0.75, definition.DeltaY(), TOLERANCE)
        assertPoint(-1.5, -0.25, definition.topPoint)

        end.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, 5.0),
        )
        board.update()
        assertEquals(1.0, definition.Slope(), TOLERANCE)
        assertPoint(-1.5, -1.0, definition.topPoint)
        assertEquals("m=1.000!", definition.label.plaintext)
    }

    @Test
    fun gliderParentOwnsPrivateTangentAndLinePointReusesLine() {
        val privateBoard = board("private")
        val first = point(privateBoard, -4.0, 2.0, "first")
        val second = point(privateBoard, 4.0, -2.0, "second")
        val source = segment(privateBoard, first, second, "source")
        val sourceGlider = glider(
            Glider.create(
                board = privateBoard,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = source,
                id = "sourceGlider",
                name = "",
            ),
        )
        val privateTriangle = slopeTriangle(
            SlopeTriangle.create(
                board = privateBoard,
                parents = listOf(sourceGlider),
                attributes = attributes("privateTriangle"),
            ),
        )
        val privateDefinition = assertIs<SlopeTriangleDefinition>(
            privateTriangle.slopeTriangleDefinition,
        )

        assertTrue(privateDefinition.isPrivateTangent)
        assertEquals("privateTriangleTangent", privateDefinition.tangent.id)
        assertEquals("tangent", privateDefinition.tangent.elType)
        assertEquals(listOf(sourceGlider.id), privateDefinition.tangent.parents)
        assertEquals(-0.5, privateDefinition.Slope(), TOLERANCE)

        privateBoard.removeObject(privateTriangle)
        assertEquals(
            listOf(first, second, source, sourceGlider),
            privateBoard.objectsList,
        )

        val linePointBoard = board("line-point")
        val lineStart = point(linePointBoard, -2.0, 3.0, "lineStart")
        val lineEnd = point(linePointBoard, 2.0, -3.0, "lineEnd")
        val line = segment(linePointBoard, lineStart, lineEnd, "line")
        val linePoint = glider(
            Glider.create(
                board = linePointBoard,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = line,
                id = "linePoint",
                name = "",
            ),
        )
        val linePointTriangle = slopeTriangle(
            SlopeTriangle.create(
                board = linePointBoard,
                parents = listOf(line, linePoint),
                attributes = attributes("linePointTriangle"),
            ),
        )
        val linePointDefinition = assertIs<SlopeTriangleDefinition>(
            linePointTriangle.slopeTriangleDefinition,
        )

        assertFalse(linePointDefinition.isPrivateTangent)
        assertSame(line, linePointDefinition.tangent)
        assertSame(linePoint, linePointDefinition.tangentPoint)
        assertEquals(-1.5, linePointDefinition.Slope(), TOLERANCE)
        assertEquals(1.0, linePointDefinition.DeltaX(), TOLERANCE)
        assertEquals(-1.5, linePointDefinition.DeltaY(), TOLERANCE)
    }

    @Test
    fun unsupportedParentsAndHelperFailureRollbackAtomically() {
        val board = board()
        val unrelated = point(board, 0.0, 0.0, "unrelated")
        val beforeUnsupported = board.objectsList.toList()

        assertIs<GMResult.Err<SlopeTriangleError.UnsupportedParents>>(
            SlopeTriangle.create(
                board = board,
                parents = listOf(unrelated),
            ),
        )
        assertEquals(beforeUnsupported, board.objectsList)

        val first = point(board, -2.0, 0.0, "first")
        val second = point(board, 2.0, 2.0, "second")
        val source = segment(board, first, second, "source")
        val sourceGlider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 1.0),
                slideObject = source,
                id = "sourceGlider",
                name = "",
            ),
        )
        point(board, 5.0, 5.0, "duplicate")
        val beforeDuplicate = board.objectsList.toList()

        val duplicate = assertIs<
            GMResult.Err<SlopeTriangleError.PointFactory>,
            >(
            SlopeTriangle.create(
                board = board,
                parents = listOf(source, sourceGlider),
                attributes = SlopeTriangleAttributes(
                    id = "triangle",
                    name = "",
                    basePoint = SlopeTriangleElementAttributes(
                        id = "duplicate",
                    ),
                ),
            ),
        ).error
        assertEquals("basepoint", duplicate.role)
        assertIs<PointError.Registration>(duplicate.error)
        assertEquals(beforeDuplicate, board.objectsList)

        val otherBoard = board("other")
        val foreignPoint = point(otherBoard, 0.0, 0.0, "foreign")
        assertEquals(
            SlopeTriangleError.ParentBoardMismatch(parentIndex = 1),
            assertIs<GMResult.Err<SlopeTriangleError.ParentBoardMismatch>>(
                SlopeTriangle.create(
                    board = board,
                    parents = listOf(source, foreignPoint),
                ),
            ).error,
        )
        assertEquals(beforeDuplicate, board.objectsList)
    }

    private fun attributes(id: String): SlopeTriangleAttributes =
        SlopeTriangleAttributes(
            id = id,
            name = "",
            digits = 3,
            prefix = "m=",
            suffix = "!",
            basePoint = SlopeTriangleElementAttributes(
                id = "${id}Base",
                name = "",
            ),
            baseLine = SlopeTriangleElementAttributes(
                id = "${id}Baseline",
                name = "",
            ),
            glider = SlopeTriangleElementAttributes(
                id = "${id}Glider",
                name = "",
                fixed = true,
            ),
            topPoint = SlopeTriangleElementAttributes(
                id = "${id}Top",
                name = "",
            ),
            tangent = SlopeTriangleElementAttributes(
                id = "${id}Tangent",
                name = "",
            ),
        )

    private fun board(id: String = "board"): Board =
        Board(
            originX = 250.0,
            originY = 250.0,
            unitX = 50.0,
            unitY = 50.0,
            id = id,
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

    private fun tangent(
        result: GMResult<Line, TangentError>,
    ): Line = assertIs<GMResult.Ok<Line>>(result).value

    private fun slopeTriangle(
        result: GMResult<Polygon, SlopeTriangleError>,
    ): Polygon = assertIs<GMResult.Ok<Polygon>>(result).value

    private fun angle(
        result: GMResult<Double, LineError>,
    ): Double = assertIs<GMResult.Ok<Double>>(result).value

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
