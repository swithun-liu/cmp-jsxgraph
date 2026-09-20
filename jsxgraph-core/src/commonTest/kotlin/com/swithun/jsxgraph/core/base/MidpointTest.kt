package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MidpointTest {
    @Test
    fun factoryRegistersConstrainedPointAndTracksExistingParents() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, 2.0))
        val second = point(board, doubleArrayOf(2.0, -2.0))

        val midpoint = midpoint(
            MidpointPoint.create(
                board = board,
                point1 = first,
                point2 = second,
                id = "middle",
                name = "Middle",
                fixed = true,
            ),
        )

        assertEquals("middle", midpoint.id)
        assertEquals("Middle", midpoint.name)
        assertEquals("midpoint", midpoint.elType)
        assertEquals(Const.OBJECT_TYPE_CAS, midpoint.type)
        assertEquals(Const.OBJECT_TYPE_POINT, midpoint.originalType)
        assertEquals(Const.OBJECT_CLASS_POINT, midpoint.elementClass)
        assertFalse(midpoint.isDraggable)
        assertTrue(midpoint.isFixed)
        assertSame(first, midpoint.point1)
        assertSame(second, midpoint.point2)
        assertTrue(midpoint.ownedPoints.isEmpty())
        assertEquals(listOf(first.id, second.id), midpoint.parents)
        assertSame(midpoint, first.childElements[midpoint.id])
        assertSame(midpoint, second.childElements[midpoint.id])
        assertSame(first, midpoint.ancestors[first.id])
        assertSame(second, midpoint.ancestors[second.id])
        assertContentEquals(
            doubleArrayOf(1.0, -1.0, 0.0),
            midpoint.coords.usrCoords,
        )

        first.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 4.0),
        )
        board.update(draggedElement = first)

        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 1.0),
            midpoint.coords.usrCoords,
        )
    }

    @Test
    fun ownedCoordinateHelpersAreChildrenAndRemovedWithMidpoint() {
        val board = board()
        val first = point(board, doubleArrayOf(-2.0, 4.0))
        val second = point(board, doubleArrayOf(4.0, 2.0))
        val midpoint = midpoint(
            MidpointPoint.create(
                board = board,
                point1 = first,
                point2 = second,
                ownedPoints = setOf(first, second),
            ),
        )

        assertEquals(setOf(first, second), midpoint.ownedPoints)
        assertEquals(setOf(first.id, second.id), midpoint.childElements.keys)
        assertTrue(midpoint.ancestors.isEmpty())
        assertSame(midpoint, first.ancestors[midpoint.id])
        assertSame(midpoint, second.ancestors[midpoint.id])
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 3.0),
            midpoint.coords.usrCoords,
        )

        board.removeObject(midpoint)

        assertTrue(board.objectsList.isEmpty())
        assertTrue(board.objects.isEmpty())
    }

    @Test
    fun existingParentsSurviveMidpointRemoval() {
        val board = board()
        val first = point(board, doubleArrayOf(-2.0, 4.0))
        val second = point(board, doubleArrayOf(4.0, 2.0))
        val midpoint = midpoint(
            MidpointPoint.create(board, first, second),
        )

        board.removeObject(midpoint)

        assertEquals(
            listOf<GeometryElement>(first, second),
            board.objectsList,
        )
        assertTrue(first.childElements.isEmpty())
        assertTrue(second.childElements.isEmpty())
    }

    @Test
    fun updateMatchesOfficialIdealAndPerCoordinateNanBehavior() {
        val idealBoard = board("ideal")
        val ideal = point(idealBoard, doubleArrayOf(0.0, 2.0, 3.0))
        val finite = point(idealBoard, doubleArrayOf(2.0, 4.0))
        val idealMidpoint = midpoint(
            MidpointPoint.create(idealBoard, ideal, finite),
        )

        assertEquals(1.0, idealMidpoint.coords.usrCoords[0])
        assertTrue(idealMidpoint.X().isNaN())
        assertTrue(idealMidpoint.Y().isNaN())

        val nanBoard = board("partial-nan")
        val nanX = point(
            nanBoard,
            doubleArrayOf(Double.NaN, 6.0),
        )
        val regular = point(nanBoard, doubleArrayOf(2.0, 4.0))
        val partial = midpoint(
            MidpointPoint.create(nanBoard, nanX, regular),
        )

        assertTrue(partial.X().isNaN())
        assertEquals(5.0, partial.Y())
    }

    @Test
    fun factoryFailuresDoNotPolluteRegistriesOrDependencies() {
        val board = board()
        val first = point(board, doubleArrayOf(0.0, 0.0))
        val second = point(board, doubleArrayOf(2.0, 2.0))
        val unregistered = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
            id = "unregistered",
        )
        val foreignBoard = board("foreign")
        val foreign = point(foreignBoard, doubleArrayOf(4.0, 4.0))
        val originalIds = board.objects.keys.toSet()

        assertEquals(
            MidpointError.ParentNotRegistered(1, "unregistered"),
            assertIs<GMResult.Err<MidpointError.ParentNotRegistered>>(
                MidpointPoint.create(board, first, unregistered),
            ).error,
        )
        assertEquals(
            MidpointError.ParentBoardMismatch(0),
            assertIs<GMResult.Err<MidpointError.ParentBoardMismatch>>(
                MidpointPoint.create(board, foreign, second),
            ).error,
        )
        assertEquals(
            MidpointError.OwnedPointNotParent(second.id),
            assertIs<GMResult.Err<MidpointError.OwnedPointNotParent>>(
                MidpointPoint.create(
                    board = board,
                    point1 = first,
                    point2 = first,
                    ownedPoints = setOf(second),
                ),
            ).error,
        )
        assertEquals(
            BoardError.DuplicateElementId(first.id),
            assertIs<GMResult.Err<MidpointError.Registration>>(
                MidpointPoint.create(
                    board = board,
                    point1 = first,
                    point2 = second,
                    id = first.id,
                ),
            ).error.error,
        )

        assertEquals(originalIds, board.objects.keys)
        assertTrue(first.childElements.isEmpty())
        assertTrue(second.childElements.isEmpty())
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
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(board, coordinates),
    ).value

    private fun midpoint(
        result: GMResult<MidpointPoint, MidpointError>,
    ): MidpointPoint =
        assertIs<GMResult.Ok<MidpointPoint>>(result).value
}
