package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ParallelTest {
    @Test
    fun parallelPointMatchesOfficialCoordinatesDependenciesAndUpdates() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0), "a")
        val second = point(board, doubleArrayOf(2.0, 3.0), "b")
        val through = point(board, doubleArrayOf(3.0, -3.0), "c")
        val output = parallelPoint(
            ParallelPoint.create(
                board = board,
                point1 = first,
                point2 = second,
                point3 = through,
                id = "output",
                fixed = true,
            ),
        )

        assertEquals("parallelpoint", output.elType)
        assertEquals(Const.OBJECT_TYPE_CAS, output.type)
        assertEquals(Const.OBJECT_CLASS_POINT, output.elementClass)
        assertEquals(listOf("a", "b", "c"), output.parents)
        assertFalse(output.isDraggable)
        assertTrue(output.isFixed)
        assertCoordinates(
            doubleArrayOf(1.0, 9.0, 1.0),
            output.coords.usrCoords,
        )
        assertSame(output, first.childElements[output.id])
        assertSame(output, second.childElements[output.id])
        assertSame(output, through.childElements[output.id])

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 1.0),
        )
        through.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, -4.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(1.0, 7.0, -5.0),
            output.coords.usrCoords,
        )
    }

    @Test
    fun lineParentParallelUsesDynamicIdealPointAndPreservesParentOrder() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0), "a")
        val second = point(board, doubleArrayOf(2.0, 3.0), "b")
        val sourceLine = line(board, first, second, "baseLine")
        val through = point(board, doubleArrayOf(3.0, -3.0), "c")
        val output = parallelLine(
            ParallelLine.create(
                board = board,
                sourceLine = sourceLine,
                throughPoint = through,
                parentMetadata = listOf(through, sourceLine),
                id = "output",
            ),
        )
        val helper = assertIs<ParallelDirectionPoint>(output.point)

        assertEquals("parallel", output.elType)
        assertEquals(listOf("c", "baseLine"), output.parents)
        assertSame(through, output.point1)
        assertSame(helper, output.point2)
        assertCoordinates(
            doubleArrayOf(
                0.0,
                -0.8320502943378437,
                -0.5547001962252291,
            ),
            helper.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(
                -4.160251471689219,
                0.5547001962252291,
                -0.8320502943378437,
            ),
            output.stdform.copyOf(3),
        )
        assertTrue(helper.isDraggable)
        assertEquals(Const.OBJECT_TYPE_CAS, helper.type)
        assertEquals("point", helper.elType)
        assertTrue(sourceLine.childElements.isEmpty())
        assertSame(output, through.childElements[output.id])
        assertSame(output, helper.childElements[output.id])

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 1.0),
        )
        through.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, -4.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(
                0.0,
                -0.9863939238321437,
                0.1643989873053573,
            ),
            helper.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(
                -3.7811767080232177,
                -0.1643989873053573,
                -0.9863939238321437,
            ),
            output.stdform.copyOf(3),
        )

        board.removeObject(output)

        assertEquals(null, board.elementById(output.id))
        assertSame(helper, board.elementById(helper.id))
        assertSame(sourceLine, board.elementById(sourceLine.id))
        assertSame(through, board.elementById(through.id))
    }

    @Test
    fun threePointParallelUsesFiniteHelperAndLeavesItAfterRemoval() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0), "a")
        val second = point(board, doubleArrayOf(2.0, 3.0), "b")
        val through = point(board, doubleArrayOf(3.0, -3.0), "c")
        val output = parallelLine(
            ParallelLine.create(
                board = board,
                point1 = first,
                point2 = second,
                throughPoint = through,
                id = "output",
            ),
        )
        val helper = assertIs<ParallelPoint>(output.point)

        assertEquals(listOf("a", "b", "c"), output.parents)
        assertSame(through, output.point1)
        assertSame(helper, output.point2)
        assertTrue(helper.isDraggable)
        assertCoordinates(
            doubleArrayOf(1.0, 9.0, 1.0),
            helper.coords.usrCoords,
        )
        assertSame(helper, first.childElements[helper.id])
        assertSame(helper, second.childElements[helper.id])
        assertSame(helper, through.childElements[helper.id])
        assertSame(output, through.childElements[output.id])
        assertSame(output, helper.childElements[output.id])

        board.removeObject(output)

        assertEquals(null, board.elementById(output.id))
        assertSame(helper, board.elementById(helper.id))
        assertSame(first, board.elementById(first.id))
        assertSame(second, board.elementById(second.id))
        assertSame(through, board.elementById(through.id))
    }

    @Test
    fun parallelPointOwnsCoordinateHelpersAndDegenerateLinePropagatesNaN() {
        val helperBoard = board("helper-board")
        val helpers = listOf(
            point(helperBoard, doubleArrayOf(-4.0, -1.0)),
            point(helperBoard, doubleArrayOf(2.0, 3.0)),
            point(helperBoard, doubleArrayOf(3.0, -3.0)),
        )
        val output = parallelPoint(
            ParallelPoint.create(
                board = helperBoard,
                point1 = helpers[0],
                point2 = helpers[1],
                point3 = helpers[2],
                ownedPoints = helpers.toSet(),
            ),
        )
        assertEquals(helpers.map(Point::id), output.parents)
        assertEquals(helpers.map(Point::id).toSet(), output.childElements.keys)

        helperBoard.removeObject(output)

        assertTrue(helperBoard.objects.isEmpty())

        val lineBoard = board("helper-line-board")
        val lineHelpers = listOf(
            point(lineBoard, doubleArrayOf(-4.0, -1.0)),
            point(lineBoard, doubleArrayOf(2.0, 3.0)),
            point(lineBoard, doubleArrayOf(3.0, -3.0)),
        )
        val line = parallelLine(
            ParallelLine.create(
                board = lineBoard,
                point1 = lineHelpers[0],
                point2 = lineHelpers[1],
                throughPoint = lineHelpers[2],
                ownedPoints = lineHelpers.toSet(),
            ),
        )
        val lineHelper = line.point

        lineBoard.removeObject(line)

        assertSame(lineHelper, lineBoard.elementById(lineHelper.id))
        for (helper in lineHelpers) {
            assertSame(helper, lineBoard.elementById(helper.id))
        }
        assertEquals(4, lineBoard.objects.size)

        val degenerateBoard = board("degenerate-board")
        val first = point(
            degenerateBoard,
            doubleArrayOf(1.0, 2.0),
            "sameA",
        )
        val second = point(
            degenerateBoard,
            doubleArrayOf(1.0, 2.0),
            "sameB",
        )
        val through = point(
            degenerateBoard,
            doubleArrayOf(-2.0, 3.0),
            "sameC",
        )
        val degeneratePoint = parallelPoint(
            ParallelPoint.create(
                degenerateBoard,
                first,
                second,
                through,
            ),
        )
        val degenerateLine = parallelLine(
            ParallelLine.create(
                degenerateBoard,
                first,
                second,
                through,
            ),
        )

        assertCoordinates(
            doubleArrayOf(1.0, -2.0, 3.0),
            degeneratePoint.coords.usrCoords,
        )
        assertTrue(degenerateLine.stdform.take(3).all(Double::isNaN))
    }

    @Test
    fun factoriesRejectInvalidParentsAndDuplicateIdsAtomically() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0), "a")
        val second = point(board, doubleArrayOf(2.0, 3.0), "b")
        val sourceLine = line(board, first, second, "baseLine")
        val through = point(board, doubleArrayOf(3.0, -3.0), "taken")
        val originalIds = board.objects.keys.toSet()
        val foreign = point(
            board("foreign"),
            doubleArrayOf(1.0, 1.0),
            "foreign",
        )
        val unregistered = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
            id = "unregistered",
        )

        assertEquals(
            ParallelConstructionError.ParentBoardMismatch(0),
            assertIs<
                GMResult.Err<
                    ParallelConstructionError.ParentBoardMismatch,
                    >,
                >(
                ParallelPoint.create(
                    board,
                    foreign,
                    second,
                    through,
                ),
            ).error,
        )
        assertEquals(
            ParallelConstructionError.ParentNotRegistered(
                1,
                "unregistered",
            ),
            assertIs<
                GMResult.Err<
                    ParallelConstructionError.ParentNotRegistered,
                    >,
                >(
                ParallelLine.create(
                    board,
                    sourceLine,
                    unregistered,
                ),
            ).error,
        )
        assertEquals(
            ParallelConstructionError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            assertIs<
                GMResult.Err<
                    ParallelConstructionError.Registration,
                    >,
                >(
                ParallelLine.create(
                    board,
                    sourceLine,
                    through,
                    id = "taken",
                ),
            ).error,
        )

        assertEquals(originalIds, board.objects.keys)
        assertTrue(sourceLine.childElements.isEmpty())
        assertTrue(through.childElements.isEmpty())
    }

    private fun board(id: String = "board"): Board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
        id = id,
    )

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

    private fun parallelPoint(
        result: GMResult<ParallelPoint, ParallelConstructionError>,
    ): ParallelPoint =
        assertIs<GMResult.Ok<ParallelPoint>>(result).value

    private fun parallelLine(
        result: GMResult<ParallelLine, ParallelConstructionError>,
    ): ParallelLine =
        assertIs<GMResult.Ok<ParallelLine>>(result).value

    private fun assertCoordinates(
        expected: DoubleArray,
        actual: DoubleArray,
        tolerance: Double = 1.0e-12,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            if (expected[index].isNaN()) {
                assertTrue(actual[index].isNaN())
            } else {
                assertTrue(
                    abs(expected[index] - actual[index]) <= tolerance,
                    "coordinate[$index]: expected=${expected[index]}, " +
                        "actual=${actual[index]}",
                )
            }
        }
    }
}
