package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LineTest {
    @Test
    fun twoPointFactoryRegistersIdentityDependenciesAndOrdinaryMetrics() {
        val board = board()
        val point1 = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(4.0, 2.0))

        val line = line(Line.create(board, point1, point2))

        assertEquals("boardL2", line.id)
        assertEquals("a", line.name)
        assertEquals("line", line.elType)
        assertEquals(Const.OBJECT_TYPE_LINE, line.type)
        assertEquals(Const.OBJECT_TYPE_LINE, line.originalType)
        assertEquals(Const.OBJECT_CLASS_LINE, line.elementClass)
        assertNull(line.baseElement)
        assertTrue(line.isDraggable)
        assertSame(line, board.select(line.id))
        assertSame(line, board.select(line.name))
        assertEquals(listOf(point1.id, point2.id), line.parents)
        assertSame(line, point1.childElements[line.id])
        assertSame(line, point2.childElements[line.id])
        assertSame(point1, line.ancestors[point1.id])
        assertSame(point2, line.ancestors[point2.id])

        assertArrayMatches(
            expected = doubleArrayOf(
                0.0,
                -0.4472135954999579,
                0.8944271909999159,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            actual = line.stdform,
        )
        assertArrayMatches(doubleArrayOf(4.0, 2.0), line.Direction())
        assertEquals(0.5, line.Slope(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, line.getRise(), absoluteTolerance = TOLERANCE)
        assertEquals(
            4.47213595499958,
            line.L(),
            absoluteTolerance = TOLERANCE,
        )
        assertArrayMatches(doubleArrayOf(0.0, 2.0, 4.0, 0.0), line.bounds())
        assertEquals(false, line.isVertical())
    }

    @Test
    fun verticalAndCoincidentLinesMatchOfficialNonFiniteResults() {
        val board = board()
        val vertical = line(
            Line.create(
                board,
                point(board, doubleArrayOf(2.0, -3.0)),
                point(board, doubleArrayOf(2.0, 5.0)),
            ),
        )

        assertArrayMatches(
            doubleArrayOf(
                2.0,
                -1.0,
                0.0,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NaN,
            ),
            vertical.stdform,
        )
        assertArrayMatches(doubleArrayOf(0.0, 8.0), vertical.Direction())
        assertEquals(Double.POSITIVE_INFINITY, vertical.Slope())
        assertEquals(Double.POSITIVE_INFINITY, vertical.getRise())
        assertEquals(8.0, vertical.L())
        assertArrayMatches(doubleArrayOf(2.0, 5.0, 2.0, -3.0), vertical.bounds())
        assertTrue(vertical.isVertical())

        val coincidentPoint = point(board, doubleArrayOf(2.0, 3.0))
        val coincident = line(Line.create(board, coincidentPoint, coincidentPoint))

        assertTrue(coincident.stdform[0].isNaN())
        assertTrue(coincident.stdform[1].isNaN())
        assertTrue(coincident.stdform[2].isNaN())
        assertArrayMatches(doubleArrayOf(0.0, 0.0), coincident.Direction())
        assertEquals(Double.POSITIVE_INFINITY, coincident.Slope())
        assertEquals(Double.POSITIVE_INFINITY, coincident.getRise())
        assertEquals(0.0, coincident.L())
        assertEquals(false, coincident.isVertical())
    }

    @Test
    fun idealPointDirectionBranchesMatchOfficialLineBehavior() {
        val board = board()
        val finite = point(board, doubleArrayOf(2.0, 3.0))
        val ideal = point(board, doubleArrayOf(0.0, 4.0, -5.0))
        val secondIdeal = point(board, doubleArrayOf(0.0, -2.0, 7.0))

        val finiteToIdeal = line(Line.create(board, finite, ideal))
        val idealToFinite = line(Line.create(board, ideal, finite))
        val idealToIdeal = line(Line.create(board, ideal, secondIdeal))

        assertArrayMatches(doubleArrayOf(4.0, -5.0), finiteToIdeal.Direction())
        assertArrayMatches(doubleArrayOf(-4.0, 5.0), idealToFinite.Direction())
        assertArrayMatches(doubleArrayOf(-6.0, 12.0), idealToIdeal.Direction())
        assertEquals(-1.25, finiteToIdeal.Slope(), absoluteTolerance = TOLERANCE)
        assertEquals(5.5, finiteToIdeal.getRise(), absoluteTolerance = TOLERANCE)
        assertTrue(finiteToIdeal.L().isNaN())
        assertArrayMatches(
            doubleArrayOf(2.0, 3.0, 4.0, -5.0),
            finiteToIdeal.bounds(),
        )
    }

    @Test
    fun updateRecomputesStandardFormOnlyWhenRequested() {
        val board = board()
        val point1 = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(4.0, 2.0))
        val line = line(Line.create(board, point1, point2))
        val initialStandardForm = line.stdform.copyOf()

        point2.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 4.0),
        )
        line.needsUpdate = false
        assertSame(line, line.update(fromParent = true))
        assertArrayMatches(initialStandardForm, line.stdform)

        line.needsUpdate = true
        assertSame(line, line.update(fromParent = true))
        assertArrayMatches(
            doubleArrayOf(
                0.0,
                -1.0,
                0.0,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NaN,
            ),
            line.stdform,
        )
    }

    @Test
    fun factoryRejectsForeignAndUnregisteredParents() {
        val board = board()
        val registered = point(board, doubleArrayOf(0.0, 0.0))
        val unregistered = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
            id = "unregistered",
        )
        val foreignBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "foreign",
        )
        val foreign = point(foreignBoard, doubleArrayOf(2.0, 2.0))

        assertEquals(
            LineError.ParentNotRegistered(
                parentIndex = 1,
                id = "unregistered",
            ),
            assertIs<GMResult.Err<LineError.ParentNotRegistered>>(
                Line.create(board, registered, unregistered),
            ).error,
        )
        assertEquals(
            LineError.ParentBoardMismatch(parentIndex = 0),
            assertIs<GMResult.Err<LineError.ParentBoardMismatch>>(
                Line.create(board, foreign, registered),
            ).error,
        )
        assertEquals(1, board.numObjects)
        assertTrue(registered.childElements.isEmpty())
    }

    @Test
    fun duplicateLineIdDoesNotLeakNameOrDependencyEntries() {
        val board = board()
        val point1 = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(4.0, 2.0))
        val first = line(
            Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = "fixed",
                name = "Defined",
            ),
        )

        val duplicate = assertIs<GMResult.Err<LineError.Registration>>(
            Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = "fixed",
                name = "Orphan",
            ),
        )

        assertEquals(
            BoardError.DuplicateElementId("fixed"),
            duplicate.error.error,
        )
        assertSame(first, board.elementById("fixed"))
        assertSame(first, board.elementByName("Defined"))
        assertNull(board.elementByName("Orphan"))
        assertEquals(setOf(first.id), point1.childElements.keys)
        assertEquals(setOf(first.id), point2.childElements.keys)
        assertEquals(3, board.numObjects)
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
        id = "board",
    )

    private fun point(
        board: Board,
        coordinates: DoubleArray,
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(board, coordinates),
    ).value

    private fun line(result: GMResult<Line, LineError>): Line =
        assertIs<GMResult.Ok<Line>>(result).value

    private fun assertArrayMatches(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            when {
                expected[index].isNaN() ->
                    assertTrue(actual[index].isNaN(), "Expected NaN at index $index")

                expected[index].isInfinite() ->
                    assertEquals(expected[index], actual[index], "Mismatch at index $index")

                else -> assertEquals(
                    expected = expected[index],
                    actual = actual[index],
                    absoluteTolerance = TOLERANCE,
                    message = "Mismatch at index $index",
                )
            }
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-14
    }
}
