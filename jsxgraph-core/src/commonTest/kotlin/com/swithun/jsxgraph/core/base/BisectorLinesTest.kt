/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BisectorLinesTest {
    @Test
    fun compositionAndLineGeometryMatchOfficialReference() {
        val fixture = fixture("bisector-lines")
        val result = assertIs<GMResult.Ok<BisectorLines>>(
            BisectorLines.create(
                board = fixture.board,
                first = fixture.first,
                second = fixture.second,
                line1Attributes = BisectorLineAttributes(
                    id = "bisectorFirst",
                    name = "first-bisector",
                ),
                line2Attributes = BisectorLineAttributes(
                    id = "bisectorSecond",
                    name = "second-bisector",
                ),
            ),
        ).value
        val firstBisector = requireLine(result.line1)
        val secondBisector = requireLine(result.line2)

        assertEquals("bisectorlines", result.elType)
        assertTrue(result.dump)
        assertNull(result.getParents())
        assertEquals("bisectorlines", result.getType())
        assertEquals(
            listOf<GeometryElement>(firstBisector, secondBisector),
            result.objectsList,
        )
        assertSame(firstBisector, result.elements["bisectorFirst"])
        assertSame(secondBisector, result.elements["bisectorSecond"])
        assertSame(
            firstBisector,
            result.elementsByName["first-bisector"],
        )
        assertSame(
            secondBisector,
            result.elementsByName["second-bisector"],
        )
        assertSame(firstBisector, result.select("bisectorFirst"))
        assertSame(secondBisector, result.select("second-bisector"))
        assertSame(firstBisector, result.subs["line1"])
        assertSame(secondBisector, result.subs["line2"])

        assertBisectorLine(
            line = firstBisector,
            expectedId = "bisectorFirst",
            expectedName = "first-bisector",
            expectedStdform = doubleArrayOf(
                0.25014602495712585,
                0.9981817925692965,
                -0.060275276715630786,
            ),
            sourceIds = listOf("first", "second"),
        )
        assertBisectorLine(
            line = secondBisector,
            expectedId = "bisectorSecond",
            expectedName = "second-bisector",
            expectedStdform = doubleArrayOf(
                1.5502410389027084,
                -0.060275276715630544,
                -0.9981817925692964,
            ),
            sourceIds = listOf("first", "second"),
        )
        assertFalse(firstBisector.id in fixture.first.childElements)
        assertFalse(secondBisector.id in fixture.first.childElements)
        assertFalse(firstBisector.id in fixture.second.childElements)
        assertFalse(secondBisector.id in fixture.second.childElements)

        fixture.a.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, -3.0),
        )
        fixture.d.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, 1.0),
        )
        fixture.board.update()

        assertStdform(
            firstBisector,
            -1.6893769393752578,
            0.9664996487646696,
            0.25666793515702413,
        )
        assertStdform(
            secondBisector,
            1.4913519224874752,
            0.2566679351570242,
            -0.9664996487646695,
        )
    }

    @Test
    fun compositionRemovalLeavesHiddenPointsAndSourcesRegistered() {
        val fixture = fixture("removal")
        val composition = bisectorLines(
            BisectorLines.create(
                board = fixture.board,
                first = fixture.first,
                second = fixture.second,
                line1Attributes = BisectorLineAttributes(
                    id = "firstBisector",
                    name = "",
                ),
                line2Attributes = BisectorLineAttributes(
                    id = "secondBisector",
                    name = "",
                ),
            ),
        )
        val firstBisector = requireLine(composition.line1)
        val secondBisector = requireLine(composition.line2)
        val helperPoints = listOf(
            firstBisector.point1,
            firstBisector.point2,
            secondBisector.point1,
            secondBisector.point2,
        )

        fixture.board.removeObject(composition)

        assertNull(fixture.board.elementById(firstBisector.id))
        assertNull(fixture.board.elementById(secondBisector.id))
        for (source in listOf(
            fixture.a,
            fixture.b,
            fixture.c,
            fixture.d,
            fixture.first,
            fixture.second,
        )) {
            assertSame(source, fixture.board.elementById(source.id))
        }
        for (helper in helperPoints) {
            assertSame(helper, fixture.board.elementById(helper.id))
            assertTrue(helper.childElements.isEmpty())
        }

        assertTrue(composition.remove("line1"))
        assertNull(composition.member("line1"))
        assertSame(firstBisector, composition.subs["line1"])
        assertFalse(composition.remove("line1"))
        assertEquals(
            listOf<GeometryElement>(secondBisector),
            composition.objectsList,
        )
    }

    @Test
    fun duplicateOutputIdRollsBackEveryCreatedObject() {
        val fixture = fixture("duplicate")
        val before = fixture.board.objects.keys.toList()

        val error = assertIs<GMResult.Err<BisectorLinesError>>(
            BisectorLines.create(
                board = fixture.board,
                first = fixture.first,
                second = fixture.second,
                line1Attributes = BisectorLineAttributes(
                    id = "duplicate",
                ),
                line2Attributes = BisectorLineAttributes(
                    id = "duplicate",
                ),
            ),
        ).error

        val outputError = assertIs<BisectorLinesError.OutputLine>(error)
        assertEquals(1, outputError.outputIndex)
        assertEquals(
            LineError.Registration(
                BoardError.DuplicateElementId("duplicate"),
            ),
            outputError.error,
        )
        assertEquals(before, fixture.board.objects.keys.toList())
    }

    @Test
    fun collapsedSourcePropagatesOfficialNanGeometry() {
        val board = board("degenerate")
        val sameA = point(board, 1.0, 1.0, "sameA")
        val sameB = point(board, 1.0, 1.0, "sameB")
        val regularA = point(board, -2.0, 0.0, "regularA")
        val regularB = point(board, 2.0, 0.0, "regularB")
        val collapsed = line(board, sameA, sameB, "collapsed")
        val regular = line(board, regularA, regularB, "regular")
        val result = bisectorLines(
            BisectorLines.create(
                board = board,
                first = collapsed,
                second = regular,
            ),
        )

        for (output in listOfNotNull(result.line1, result.line2)) {
            assertTrue(output.stdform.take(3).all(Double::isNaN))
            assertTrue(output.point1.coords.usrCoords.all(Double::isNaN))
            assertTrue(output.point2.coords.usrCoords.all(Double::isNaN))
        }
    }

    private fun assertBisectorLine(
        line: Line,
        expectedId: String,
        expectedName: String,
        expectedStdform: DoubleArray,
        sourceIds: List<String>,
    ) {
        assertEquals(expectedId, line.id)
        assertEquals(expectedName, line.name)
        assertEquals("line", line.elType)
        assertEquals(Const.OBJECT_TYPE_LINE, line.type)
        assertEquals(Const.OBJECT_CLASS_LINE, line.elementClass)
        assertFalse(line.isDraggable)
        assertFalse(line.dump)
        assertEquals(sourceIds, line.parents)
        assertStdform(
            line,
            expectedStdform[0],
            expectedStdform[1],
            expectedStdform[2],
        )
        for (helper in listOf(line.point1, line.point2)) {
            assertEquals(Const.OBJECT_TYPE_CAS, helper.type)
            assertEquals("", helper.name)
            assertFalse(helper.isDraggable)
            assertTrue(helper.dump)
            assertTrue(helper.parents.isEmpty())
            assertSame(line, helper.childElements[line.id])
        }
    }

    private fun assertStdform(
        line: Line,
        a: Double,
        b: Double,
        c: Double,
    ) {
        assertEquals(a, line.stdform[0], absoluteTolerance = TOLERANCE)
        assertEquals(b, line.stdform[1], absoluteTolerance = TOLERANCE)
        assertEquals(c, line.stdform[2], absoluteTolerance = TOLERANCE)
    }

    private fun fixture(id: String): Fixture {
        val board = board(id)
        val a = point(board, -4.0, -1.0, "a")
        val b = point(board, 2.0, 3.0, "b")
        val c = point(board, -3.0, 4.0, "c")
        val d = point(board, 4.0, -2.0, "d")
        return Fixture(
            board = board,
            a = a,
            b = b,
            c = c,
            d = d,
            first = line(board, a, b, "first"),
            second = line(board, c, d, "second"),
        )
    }

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

    private fun line(
        board: Board,
        first: Point,
        second: Point,
        id: String,
    ): Line =
        assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = board,
                point1 = first,
                point2 = second,
                id = id,
                name = "",
            ),
        ).value

    private fun requireLine(line: Line?): Line =
        assertIs<Line>(line)

    private fun bisectorLines(
        result: GMResult<BisectorLines, BisectorLinesError>,
    ): BisectorLines =
        assertIs<GMResult.Ok<BisectorLines>>(result).value

    private fun board(id: String): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = id,
    )

    private data class Fixture(
        val board: Board,
        val a: Point,
        val b: Point,
        val c: Point,
        val d: Point,
        val first: Line,
        val second: Line,
    )

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
