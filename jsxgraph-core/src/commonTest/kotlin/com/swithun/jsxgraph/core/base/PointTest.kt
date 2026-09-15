package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PointTest {
    @Test
    fun freePointFactoryRegistersOfficialIdentityAndCoordinates() {
        val board = board()

        val first = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, -3.0),
            ),
        )
        val second = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 8.0, -4.0),
            ),
        )

        assertEquals("boardP0", first.id)
        assertEquals("A", first.name)
        assertEquals("point", first.elType)
        assertEquals(Const.OBJECT_TYPE_POINT, first.type)
        assertEquals(Const.OBJECT_TYPE_POINT, first.originalType)
        assertEquals(Const.OBJECT_CLASS_POINT, first.elementClass)
        assertSame(first, first.baseElement)
        assertContentEquals(doubleArrayOf(1.0, 2.0, -3.0), first.coords.usrCoords)
        assertContentEquals(doubleArrayOf(1.0, 4.0, -2.0), second.coords.usrCoords)
        assertEquals("boardP1", second.id)
        assertEquals("B", second.name)
        assertSame(first, board.select(first.id))
        assertSame(second, board.select(second.name))
    }

    @Test
    fun explicitIdentityAndDuplicateFailureDoNotLeakRegistryEntries() {
        val board = board()
        val first = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 2.0),
                id = "fixed",
                name = "Named",
            ),
        )

        val duplicate = assertIs<GMResult.Err<PointError.Registration>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(3.0, 4.0),
                id = "fixed",
                name = "Orphan",
            ),
        )

        assertEquals(
            BoardError.DuplicateElementId("fixed"),
            duplicate.error.error,
        )
        assertSame(first, board.elementById("fixed"))
        assertSame(first, board.elementByName("Named"))
        assertNull(board.elementByName("Orphan"))
        assertEquals(1, board.numObjects)
    }

    @Test
    fun tooFewCoordinatesReportAnExplicitFactoryError() {
        val board = board()

        assertEquals(
            PointError.InvalidCoordinateCount(0),
            assertIs<GMResult.Err<PointError.InvalidCoordinateCount>>(
                Point.create(board, doubleArrayOf()),
            ).error,
        )
        assertEquals(
            PointError.InvalidCoordinateCount(1),
            assertIs<GMResult.Err<PointError.InvalidCoordinateCount>>(
                Point.create(board, doubleArrayOf(1.0)),
            ).error,
        )
        assertTrue(board.objects.isEmpty())
        assertEquals(0, board.numObjects)
    }

    @Test
    fun pointUpdateUsesTheFreeCoordinateLifecycle() {
        val calls = mutableListOf<String>()
        val point = RecordingPoint(
            board = board(),
            coordinates = doubleArrayOf(2.0, -3.0),
            calls = calls,
        )

        point.needsUpdate = false
        assertSame(point, point.update(fromParent = false))
        assertTrue(calls.isEmpty())

        point.needsUpdate = true
        assertSame(point, point.update(fromParent = false))
        assertEquals(
            listOf("constraint", "transform:false"),
            calls,
        )
    }

    @Test
    fun pointBoundsRepeatTheAffineCoordinates() {
        val point = Point(
            board = board(),
            coordinates = doubleArrayOf(2.0, -3.0),
        )

        assertContentEquals(
            doubleArrayOf(2.0, -3.0, 2.0, -3.0),
            point.bounds(),
        )
    }

    @Test
    fun pointIncidenceMatchesOfficialToleranceAndIdealPointBehavior() {
        val board = board()
        val origin = point(Point.create(board, doubleArrayOf(0.0, 0.0)))
        val same = point(Point.create(board, doubleArrayOf(0.0, 0.0)))
        val halfEpsilon = point(
            Point.create(board, doubleArrayOf(Mat.eps * 0.5, 0.0)),
        )
        val atEpsilon = point(
            Point.create(board, doubleArrayOf(Mat.eps, 0.0)),
        )
        val ideal = point(
            Point.create(board, doubleArrayOf(0.0, 1.0, 0.0)),
        )

        assertTrue(origin.isOn(same))
        assertTrue(origin.isOn(halfEpsilon))
        assertFalse(origin.isOn(atEpsilon))
        assertTrue(origin.isOn(halfEpsilon, tolerance = 0.0))
        assertTrue(origin.isOn(halfEpsilon, tolerance = Double.NaN))
        assertFalse(origin.isOn(same, tolerance = -1.0))
        assertFalse(origin.isOn(ideal))
        assertFalse(origin.isOn(GeometryElement(board)))
    }

    @Test
    fun lineIncidenceMatchesOfficialStrictDistanceComparison() {
        val board = board()
        val line = line(
            Line.create(
                board,
                point(Point.create(board, doubleArrayOf(-2.0, 0.0))),
                point(Point.create(board, doubleArrayOf(3.0, 0.0))),
            ),
        )
        val onLine = point(Point.create(board, doubleArrayOf(1.0, 0.0)))
        val halfEpsilon = point(
            Point.create(board, doubleArrayOf(1.0, Mat.eps * 0.5)),
        )
        val atEpsilon = point(
            Point.create(board, doubleArrayOf(1.0, Mat.eps)),
        )

        assertTrue(onLine.isOn(line))
        assertTrue(halfEpsilon.isOn(line))
        assertFalse(atEpsilon.isOn(line))
        assertTrue(halfEpsilon.isOn(line, tolerance = 0.0))
        assertTrue(halfEpsilon.isOn(line, tolerance = Double.NaN))
        assertFalse(onLine.isOn(line, tolerance = -1.0))
    }

    @Test
    fun circleIncidenceMatchesOfficialBoundaryBehavior() {
        val board = board()
        val center = point(Point.create(board, doubleArrayOf(0.0, 0.0)))
        val circle = circle(Circle.create(board, center, radius = 2.0))
        val boundary = point(Point.create(board, doubleArrayOf(2.0, 0.0)))
        val outerHalfEpsilon = point(
            Point.create(board, doubleArrayOf(2.0 + Mat.eps * 0.5, 0.0)),
        )
        val outerAtEpsilon = point(
            Point.create(board, doubleArrayOf(2.0 + Mat.eps, 0.0)),
        )

        assertTrue(boundary.isOn(circle))
        assertTrue(outerHalfEpsilon.isOn(circle))
        assertFalse(outerAtEpsilon.isOn(circle))
        assertFalse(center.isOn(circle))
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
        id = "board",
    )

    private fun point(
        result: GMResult<Point, PointError>,
    ): Point = assertIs<GMResult.Ok<Point>>(result).value

    private fun line(
        result: GMResult<Line, LineError>,
    ): Line = assertIs<GMResult.Ok<Line>>(result).value

    private fun circle(
        result: GMResult<Circle, CircleError>,
    ): Circle = assertIs<GMResult.Ok<Circle>>(result).value

    private class RecordingPoint(
        board: Board,
        coordinates: DoubleArray,
        private val calls: MutableList<String>,
    ) : Point(
        board = board,
        coordinates = coordinates,
    ) {
        override fun updateConstraint(): CoordsElement {
            calls += "constraint"
            return this
        }

        override fun updateTransform(fromParent: Boolean): CoordsElement {
            calls += "transform:$fromParent"
            return this
        }
    }
}
