package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TriangleCentersTest {
    @Test
    fun factoriesMatchOfficialCoordinatesRelationshipsAndUpdates() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0), "a")
        val vertex = point(board, doubleArrayOf(1.0, 4.0), "b")
        val third = point(board, doubleArrayOf(5.0, -2.0), "c")
        val bisector = bisector(
            BisectorLine.create(
                board = board,
                point1 = first,
                vertex = vertex,
                point3 = third,
                id = "angleBisector",
            ),
        )
        val incenter = incenter(
            IncenterPoint.create(
                board = board,
                point1 = first,
                point2 = vertex,
                point3 = third,
                id = "triangleIncenter",
                fixed = true,
            ),
        )
        val incircle = incircle(
            IncircleCircle.create(
                board = board,
                point1 = first,
                point2 = vertex,
                point3 = third,
                id = "triangleIncircle",
            ),
        )

        assertEquals("bisector", bisector.elType)
        assertEquals(Const.OBJECT_TYPE_LINE, bisector.type)
        assertEquals(Const.OBJECT_CLASS_LINE, bisector.elementClass)
        assertEquals(listOf("a", "b", "c"), bisector.parents)
        assertSame(vertex, bisector.point1)
        assertSame(bisector.point, bisector.point2)
        assertTrue(bisector.isDraggable)
        assertFalse(bisector.point.isDraggable)
        assertTrue(bisector.point.isFixed)
        assertTrue(bisector.point.parents.isEmpty())
        assertCoordinates(
            doubleArrayOf(
                1.0,
                0.9014623820333578,
                3.0048666733319296,
            ),
            bisector.point.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(
                -0.6009828548015016,
                0.9951333266680702,
                -0.09853761796664215,
            ),
            bisector.stdform.copyOf(3),
        )
        assertSame(bisector.point, first.childElements[bisector.point.id])
        assertSame(bisector.point, vertex.childElements[bisector.point.id])
        assertSame(bisector.point, third.childElements[bisector.point.id])
        assertSame(bisector, vertex.childElements[bisector.id])
        assertSame(bisector, bisector.point.childElements[bisector.id])

        assertEquals("incenter", incenter.elType)
        assertEquals(Const.OBJECT_TYPE_CAS, incenter.type)
        assertEquals(listOf("a", "b", "c"), incenter.parents)
        assertTrue(incenter.isFixed)
        assertFalse(incenter.isDraggable)
        assertCoordinates(
            doubleArrayOf(
                1.0,
                0.6670070476375293,
                0.6370976762025347,
            ),
            incenter.coords.usrCoords,
        )

        assertEquals("incircle", incircle.elType)
        assertEquals(Const.OBJECT_TYPE_CIRCLE, incircle.type)
        assertEquals(listOf("a", "b", "c"), incircle.parents)
        assertIs<IncenterPoint>(incircle.center)
        assertSame(incircle.center, incircle.midpoint)
        assertEquals(2.142469462922355, incircle.Radius(), TOLERANCE)
        assertTrue(incircle.isDraggable)
        assertSame(incircle, incircle.center.childElements[incircle.id])
        for (source in listOf(first, vertex, third)) {
            assertSame(incenter, source.childElements[incenter.id])
            assertSame(incircle.center, source.childElements[incircle.center.id])
            assertSame(incircle, source.childElements[incircle.id])
        }

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-6.0, 2.0),
        )
        vertex.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        third.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, -4.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(
                1.0,
                -0.42887834669531066,
                2.096637745012613,
            ),
            bisector.point.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(
                1.0,
                -0.9316296783367083,
                1.0376741014781923,
            ),
            incenter.coords.usrCoords,
        )
        assertCoordinates(
            incenter.coords.usrCoords,
            incircle.center.coords.usrCoords,
        )
        assertEquals(
            1.7824673672181919,
            incircle.Radius(),
            TOLERANCE,
        )
    }

    @Test
    fun helperAndCoordinateParentRemovalMatchesOfficialDirection() {
        val bisectorBoard = board("bisector-board")
        val bisectorParents = coordinateParents(bisectorBoard)
        val bisector = bisector(
            BisectorLine.create(
                board = bisectorBoard,
                point1 = bisectorParents[0],
                vertex = bisectorParents[1],
                point3 = bisectorParents[2],
                ownedPoints = bisectorParents.toSet(),
            ),
        )
        val bisectorHelper = bisector.point

        assertEquals(
            bisectorParents.map(Point::id).toSet(),
            bisectorHelper.childElements.keys
                .filterNot { it == bisector.id }
                .toSet(),
        )
        bisectorBoard.removeObject(bisector)

        assertSame(
            bisectorHelper,
            bisectorBoard.elementById(bisectorHelper.id),
        )
        for (parent in bisectorParents) {
            assertSame(parent, bisectorBoard.elementById(parent.id))
        }
        assertEquals(4, bisectorBoard.objects.size)

        bisectorBoard.removeObject(bisectorHelper)

        assertTrue(bisectorBoard.objects.isEmpty())

        val incenterBoard = board("incenter-board")
        val incenterParents = coordinateParents(incenterBoard)
        val incenter = incenter(
            IncenterPoint.create(
                board = incenterBoard,
                point1 = incenterParents[0],
                point2 = incenterParents[1],
                point3 = incenterParents[2],
                ownedPoints = incenterParents.toSet(),
            ),
        )

        incenterBoard.removeObject(incenter)

        assertTrue(incenterBoard.objects.isEmpty())

        val incircleBoard = board("incircle-board")
        val incircleParents = coordinateParents(incircleBoard)
        val incircle = incircle(
            IncircleCircle.create(
                board = incircleBoard,
                point1 = incircleParents[0],
                point2 = incircleParents[1],
                point3 = incircleParents[2],
                ownedPoints = incircleParents.toSet(),
            ),
        )
        val center = incircle.center

        incircleBoard.removeObject(incircle)

        assertSame(center, incircleBoard.elementById(center.id))
        for (parent in incircleParents) {
            assertSame(parent, incircleBoard.elementById(parent.id))
        }
        assertEquals(4, incircleBoard.objects.size)

        incircleBoard.removeObject(center)

        assertTrue(incircleBoard.objects.isEmpty())
    }

    @Test
    fun removingHiddenHelpersRecursivelyRemovesVisibleOutputs() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0), "a")
        val vertex = point(board, doubleArrayOf(1.0, 4.0), "b")
        val third = point(board, doubleArrayOf(5.0, -2.0), "c")
        val bisector = bisector(
            BisectorLine.create(board, first, vertex, third, id = "bisector"),
        )
        val incircle = incircle(
            IncircleCircle.create(
                board,
                first,
                vertex,
                third,
                id = "incircle",
            ),
        )

        board.removeObject(bisector.point)

        assertEquals(null, board.elementById(bisector.id))
        assertEquals(null, board.elementById(bisector.point.id))
        assertSame(incircle, board.elementById(incircle.id))

        board.removeObject(incircle.center)

        assertEquals(null, board.elementById(incircle.id))
        assertEquals(null, board.elementById(incircle.center.id))
        assertSame(first, board.elementById(first.id))
        assertSame(vertex, board.elementById(vertex.id))
        assertSame(third, board.elementById(third.id))
    }

    @Test
    fun degenerateInputsPreserveOfficialNaNAndZeroResults() {
        val coincidentBoard = board("coincident")
        val coincident = List(3) { index ->
            point(
                board = coincidentBoard,
                coordinates = doubleArrayOf(1.0, 2.0),
                id = "point$index",
            )
        }
        val coincidentBisector = bisector(
            BisectorLine.create(
                coincidentBoard,
                coincident[0],
                coincident[1],
                coincident[2],
            ),
        )
        val coincidentIncenter = incenter(
            IncenterPoint.create(
                coincidentBoard,
                coincident[0],
                coincident[1],
                coincident[2],
            ),
        )
        val coincidentIncircle = incircle(
            IncircleCircle.create(
                coincidentBoard,
                coincident[0],
                coincident[1],
                coincident[2],
            ),
        )

        assertCoordinates(
            doubleArrayOf(1.0, 2.0, 2.0),
            coincidentBisector.point.coords.usrCoords,
        )
        assertTrue(coincidentIncenter.X().isNaN())
        assertTrue(coincidentIncenter.Y().isNaN())
        assertTrue(coincidentIncircle.Radius().isNaN())

        val collinearBoard = board("collinear")
        val first = point(collinearBoard, doubleArrayOf(-3.0, 0.0))
        val vertex = point(collinearBoard, doubleArrayOf(0.0, 0.0))
        val third = point(collinearBoard, doubleArrayOf(4.0, 0.0))
        val collinearBisector = bisector(
            BisectorLine.create(collinearBoard, first, vertex, third),
        )
        val collinearIncenter = incenter(
            IncenterPoint.create(collinearBoard, first, vertex, third),
        )
        val collinearIncircle = incircle(
            IncircleCircle.create(collinearBoard, first, vertex, third),
        )

        assertCoordinates(
            doubleArrayOf(
                1.0,
                -1.8369701987210297e-16,
                -1.0,
            ),
            collinearBisector.point.coords.usrCoords,
        )
        assertEquals(0.0, collinearIncenter.X(), TOLERANCE)
        assertEquals(0.0, collinearIncenter.Y(), TOLERANCE)
        assertEquals(0.0, collinearIncircle.Radius(), TOLERANCE)
    }

    @Test
    fun factoriesRejectInvalidParentsAndDuplicateIdsAtomically() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0), "a")
        val vertex = point(board, doubleArrayOf(1.0, 4.0), "b")
        val third = point(board, doubleArrayOf(5.0, -2.0), "taken")
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
            TriangleCenterConstructionError.ParentBoardMismatch(0),
            assertIs<
                GMResult.Err<
                    TriangleCenterConstructionError.ParentBoardMismatch,
                    >,
                >(
                IncenterPoint.create(
                    board,
                    foreign,
                    vertex,
                    third,
                ),
            ).error,
        )
        assertEquals(
            TriangleCenterConstructionError.ParentNotRegistered(
                1,
                "unregistered",
            ),
            assertIs<
                GMResult.Err<
                    TriangleCenterConstructionError.ParentNotRegistered,
                    >,
                >(
                BisectorLine.create(
                    board,
                    first,
                    unregistered,
                    third,
                ),
            ).error,
        )
        assertEquals(
            TriangleCenterConstructionError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            assertIs<
                GMResult.Err<
                    TriangleCenterConstructionError.Registration,
                    >,
                >(
                IncircleCircle.create(
                    board,
                    first,
                    vertex,
                    third,
                    id = "taken",
                ),
            ).error,
        )

        assertEquals(originalIds, board.objects.keys)
        assertTrue(first.childElements.isEmpty())
        assertTrue(vertex.childElements.isEmpty())
        assertTrue(third.childElements.isEmpty())
    }

    private fun board(id: String = "board"): Board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
        id = id,
    )

    private fun coordinateParents(board: Board): List<Point> = listOf(
        point(board, doubleArrayOf(-4.0, -1.0)),
        point(board, doubleArrayOf(1.0, 4.0)),
        point(board, doubleArrayOf(5.0, -2.0)),
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

    private fun bisector(
        result: GMResult<BisectorLine, TriangleCenterConstructionError>,
    ): BisectorLine =
        assertIs<GMResult.Ok<BisectorLine>>(result).value

    private fun incenter(
        result: GMResult<IncenterPoint, TriangleCenterConstructionError>,
    ): IncenterPoint =
        assertIs<GMResult.Ok<IncenterPoint>>(result).value

    private fun incircle(
        result: GMResult<IncircleCircle, TriangleCenterConstructionError>,
    ): IncircleCircle =
        assertIs<GMResult.Ok<IncircleCircle>>(result).value

    private fun assertCoordinates(
        expected: DoubleArray,
        actual: DoubleArray,
        tolerance: Double = TOLERANCE,
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

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
