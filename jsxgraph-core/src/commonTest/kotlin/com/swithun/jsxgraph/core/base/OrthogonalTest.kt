package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OrthogonalTest {
    @Test
    fun orthogonalPointFactoriesMatchOfficialParentsAndUpdateBehavior() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0))
        val second = point(board, doubleArrayOf(2.0, 3.0))
        val line = line(board, first, second)
        val source = point(board, doubleArrayOf(3.0, -3.0))

        val projection = orthogonalPoint(
            OrthogonalPoint.create(
                board = board,
                point = source,
                line = line,
                kind = OrthogonalPointKind.ORTHOGONAL_PROJECTION,
                id = "projection",
                name = "Projection",
                fixed = true,
            ),
        )
        val perpendicularPoint = orthogonalPoint(
            OrthogonalPoint.create(
                board = board,
                point = source,
                line = line,
                kind = OrthogonalPointKind.PERPENDICULAR_POINT,
                id = "perpendicularPoint",
            ),
        )

        assertEquals("orthogonalprojection", projection.elType)
        assertEquals(Const.OBJECT_TYPE_CAS, projection.type)
        assertEquals(Const.OBJECT_CLASS_POINT, projection.elementClass)
        assertEquals(listOf(source.id, projection.id), projection.parents)
        assertTrue(projection.isFixed)
        assertFalse(projection.isDraggable)
        assertEquals(
            listOf(source.id, line.id),
            perpendicularPoint.parents,
        )
        assertCoordinates(
            doubleArrayOf(
                1.0,
                -0.07692307692307687,
                1.6153846153846156,
            ),
            projection.coords.usrCoords,
        )
        assertCoordinates(
            projection.coords.usrCoords,
            perpendicularPoint.coords.usrCoords,
        )
        assertSame(projection, source.childElements[projection.id])
        assertSame(projection, line.childElements[projection.id])
        assertSame(
            perpendicularPoint,
            source.childElements[perpendicularPoint.id],
        )
        assertSame(
            perpendicularPoint,
            line.childElements[perpendicularPoint.id],
        )

        source.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 4.0),
        )
        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-5.0, 2.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(1.0, 0.18, 2.74),
            projection.coords.usrCoords,
        )
        assertCoordinates(
            projection.coords.usrCoords,
            perpendicularPoint.coords.usrCoords,
        )
    }

    @Test
    fun perpendicularPointPreservesOfficialOnLineAndEndpointBranches() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0))
        val second = point(board, doubleArrayOf(2.0, 3.0))
        val line = line(board, first, second)
        val onLine = point(board, doubleArrayOf(-1.0, 1.0))

        val projection = orthogonalPoint(
            OrthogonalPoint.create(
                board,
                onLine,
                line,
                OrthogonalPointKind.ORTHOGONAL_PROJECTION,
            ),
        )
        val onLinePerpendicular = orthogonalPoint(
            OrthogonalPoint.create(
                board,
                onLine,
                line,
                OrthogonalPointKind.PERPENDICULAR_POINT,
            ),
        )
        val firstPerpendicular = orthogonalPoint(
            OrthogonalPoint.create(
                board,
                first,
                line,
                OrthogonalPointKind.PERPENDICULAR_POINT,
            ),
        )
        val secondPerpendicular = orthogonalPoint(
            OrthogonalPoint.create(
                board,
                second,
                line,
                OrthogonalPointKind.PERPENDICULAR_POINT,
            ),
        )

        assertCoordinates(
            doubleArrayOf(1.0, -1.0, 1.0),
            projection.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, 1.0, -2.0),
            onLinePerpendicular.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, 0.0, -7.0),
            firstPerpendicular.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, -2.0, 9.0),
            secondPerpendicular.coords.usrCoords,
        )
    }

    @Test
    fun perpendicularLineMatchesOfficialCoefficientConstructionAndOwnership() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0))
        val second = point(board, doubleArrayOf(2.0, 3.0))
        val sourceLine = line(board, first, second)
        val source = point(board, doubleArrayOf(3.0, -3.0))
        val perpendicular = perpendicularLine(
            PerpendicularLine.create(
                board = board,
                line = sourceLine,
                point = source,
                id = "perpendicular",
            ),
        )

        assertEquals("perpendicular", perpendicular.elType)
        assertEquals(listOf(sourceLine.id, source.id), perpendicular.parents)
        assertFalse(perpendicular.isDraggable)
        assertCoordinates(
            doubleArrayOf(
                1.0,
                -0.4170927001427658,
                2.125639050214149,
            ),
            perpendicular.point1.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(
                1.0,
                0.13760749608246337,
                1.2935887558763053,
            ),
            perpendicular.point2.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(
                -0.8320502943378439,
                0.8320502943378437,
                0.5547001962252293,
            ),
            perpendicular.stdform.copyOf(3),
        )
        assertEquals(
            setOf(perpendicular.point1.id, perpendicular.point2.id),
            perpendicular.childElements.keys,
        )
        assertSame(
            perpendicular,
            source.childElements[perpendicular.id],
        )
        assertSame(
            perpendicular,
            sourceLine.childElements[perpendicular.id],
        )

        source.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 4.0),
        )
        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-5.0, 2.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(
                1.0,
                0.277157287525381,
                2.0598989873223332,
            ),
            perpendicular.point1.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(
                1.0,
                0.4185786437626905,
                1.0699494936611667,
            ),
            perpendicular.point2.coords.usrCoords,
        )

        val helperIds =
            setOf(perpendicular.point1.id, perpendicular.point2.id)
        board.removeObject(perpendicular)

        assertTrue(helperIds.none(board.objects::containsKey))
        assertSame(source, board.elementById(source.id))
        assertSame(sourceLine, board.elementById(sourceLine.id))
    }

    @Test
    fun perpendicularSegmentPreservesOfficialDynamicEndpointOrder() {
        val board = board()
        val first = point(board, doubleArrayOf(-4.0, -1.0))
        val second = point(board, doubleArrayOf(2.0, 3.0))
        val sourceLine = line(board, first, second)
        val offLine = point(board, doubleArrayOf(3.0, -3.0))
        val onLine = point(board, doubleArrayOf(-1.0, 1.0))

        val offSegment = perpendicularSegment(
            PerpendicularSegmentLine.create(
                board,
                sourceLine,
                offLine,
                id = "offSegment",
            ),
        )
        val onSegment = perpendicularSegment(
            PerpendicularSegmentLine.create(
                board,
                sourceLine,
                onLine,
                id = "onSegment",
            ),
        )
        val firstSegment = perpendicularSegment(
            PerpendicularSegmentLine.create(
                board,
                sourceLine,
                first,
                id = "firstSegment",
            ),
        )
        val secondSegment = perpendicularSegment(
            PerpendicularSegmentLine.create(
                board,
                sourceLine,
                second,
                id = "secondSegment",
            ),
        )

        assertEquals(
            listOf(offLine.id, sourceLine.id),
            offSegment.parents,
        )
        assertSame(offSegment.point, offSegment.point1)
        assertSame(offLine, offSegment.point2)
        assertSame(onSegment.point, onSegment.point1)
        assertSame(onLine, onSegment.point2)
        assertSame(firstSegment.point, firstSegment.point1)
        assertSame(first, firstSegment.point2)
        assertSame(second, secondSegment.point1)
        assertSame(secondSegment.point, secondSegment.point2)
        assertCoordinates(
            doubleArrayOf(1.0, -2.0, 9.0),
            secondSegment.point.coords.usrCoords,
        )

        offLine.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 4.0),
        )
        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-5.0, 2.0),
        )
        board.update()

        assertSame(offSegment.point, offSegment.point1)
        assertSame(offLine, offSegment.point2)
        assertCoordinates(
            doubleArrayOf(1.0, 0.18, 2.74),
            offSegment.point1.coords.usrCoords,
        )
        assertCoordinates(
            doubleArrayOf(1.0, 0.0, 4.0),
            offSegment.point2.coords.usrCoords,
        )
    }

    @Test
    fun removalMatchesOfficialExistingAndCoordinateHelperLifetimes() {
        val pointBoard = board("point-output")
        val pointLine = baseLine(pointBoard)
        val ownedPoint = point(pointBoard, doubleArrayOf(3.0, -3.0))
        val projection = orthogonalPoint(
            OrthogonalPoint.create(
                pointBoard,
                ownedPoint,
                pointLine,
                OrthogonalPointKind.ORTHOGONAL_PROJECTION,
                ownsPoint = true,
            ),
        )

        pointBoard.removeObject(projection)

        assertSame(pointLine, pointBoard.elementById(pointLine.id))
        assertEquals(null, pointBoard.elementById(ownedPoint.id))

        val lineBoard = board("line-output")
        val line = baseLine(lineBoard)
        val lineOwnedPoint = point(lineBoard, doubleArrayOf(3.0, -3.0))
        val perpendicular = perpendicularLine(
            PerpendicularLine.create(
                lineBoard,
                line,
                lineOwnedPoint,
                ownsPoint = true,
            ),
        )
        val endpointIds =
            setOf(perpendicular.point1.id, perpendicular.point2.id)

        lineBoard.removeObject(perpendicular)

        assertSame(line, lineBoard.elementById(line.id))
        assertEquals(null, lineBoard.elementById(lineOwnedPoint.id))
        assertTrue(endpointIds.none(lineBoard.objects::containsKey))

        val segmentBoard = board("segment-output")
        val segmentLine = baseLine(segmentBoard)
        val segmentOwnedPoint =
            point(segmentBoard, doubleArrayOf(3.0, -3.0))
        val segment = perpendicularSegment(
            PerpendicularSegmentLine.create(
                segmentBoard,
                segmentLine,
                segmentOwnedPoint,
                ownsSourcePoint = true,
            ),
        )
        val helper = segment.point

        segmentBoard.removeObject(segment)

        assertSame(segmentLine, segmentBoard.elementById(segmentLine.id))
        assertSame(helper, segmentBoard.elementById(helper.id))
        assertSame(
            segmentOwnedPoint,
            segmentBoard.elementById(segmentOwnedPoint.id),
        )
        assertTrue(helper.childElements.containsKey(segmentOwnedPoint.id))
        assertFalse(helper.childElements.containsKey(segment.id))
    }

    @Test
    fun coincidentLineEndpointsPropagateOfficialNanGeometry() {
        val board = board()
        val first = point(board, doubleArrayOf(2.0, 1.0))
        val second = point(board, doubleArrayOf(2.0, 1.0))
        val line = line(board, first, second)
        val source = point(board, doubleArrayOf(-1.0, 4.0))

        val projection = orthogonalPoint(
            OrthogonalPoint.create(
                board,
                source,
                line,
                OrthogonalPointKind.ORTHOGONAL_PROJECTION,
            ),
        )
        val perpendicularPoint = orthogonalPoint(
            OrthogonalPoint.create(
                board,
                source,
                line,
                OrthogonalPointKind.PERPENDICULAR_POINT,
            ),
        )
        val perpendicular = perpendicularLine(
            PerpendicularLine.create(board, line, source),
        )
        val segment = perpendicularSegment(
            PerpendicularSegmentLine.create(board, line, source),
        )

        assertTrue(projection.coords.usrCoords.all(Double::isNaN))
        assertTrue(perpendicularPoint.coords.usrCoords.all(Double::isNaN))
        assertTrue(perpendicular.stdform.take(3).all(Double::isNaN))
        assertTrue(segment.point.coords.usrCoords.all(Double::isNaN))
        assertTrue(segment.stdform.take(3).all(Double::isNaN))
    }

    @Test
    fun factoriesRejectForeignUnregisteredAndDuplicateParentsAtomically() {
        val board = board()
        val first = point(board, doubleArrayOf(0.0, 0.0))
        val second = point(board, doubleArrayOf(2.0, 2.0))
        val line = line(board, first, second)
        val source = point(board, doubleArrayOf(2.0, 0.0))
        val originalIds = board.objects.keys.toSet()
        val foreign = point(board("foreign"), doubleArrayOf(1.0, 1.0))
        val unregistered = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
            id = "unregistered",
        )

        assertEquals(
            OrthogonalConstructionError.ParentBoardMismatch(0),
            assertIs<
                GMResult.Err<
                    OrthogonalConstructionError.ParentBoardMismatch,
                    >,
                >(
                OrthogonalPoint.create(
                    board,
                    foreign,
                    line,
                    OrthogonalPointKind.ORTHOGONAL_PROJECTION,
                ),
            ).error,
        )
        assertEquals(
            OrthogonalConstructionError.ParentNotRegistered(
                1,
                "unregistered",
            ),
            assertIs<
                GMResult.Err<
                    OrthogonalConstructionError.ParentNotRegistered,
                    >,
                >(
                PerpendicularLine.create(board, line, unregistered),
            ).error,
        )
        assertEquals(
            OrthogonalConstructionError.Registration(
                BoardError.DuplicateElementId(source.id),
            ),
            assertIs<
                GMResult.Err<
                    OrthogonalConstructionError.Registration,
                    >,
                >(
                PerpendicularSegmentLine.create(
                    board,
                    line,
                    source,
                    id = source.id,
                ),
            ).error,
        )

        assertEquals(originalIds, board.objects.keys)
        assertTrue(source.childElements.isEmpty())
        assertTrue(line.childElements.isEmpty())
    }

    private fun baseLine(board: Board): Line {
        val first = point(board, doubleArrayOf(-4.0, -1.0))
        val second = point(board, doubleArrayOf(2.0, 3.0))
        return line(board, first, second)
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

    private fun line(
        board: Board,
        first: Point,
        second: Point,
    ): Line = assertIs<GMResult.Ok<Line>>(
        Line.create(board, first, second),
    ).value

    private fun orthogonalPoint(
        result: GMResult<OrthogonalPoint, OrthogonalConstructionError>,
    ): OrthogonalPoint =
        assertIs<GMResult.Ok<OrthogonalPoint>>(result).value

    private fun perpendicularLine(
        result: GMResult<PerpendicularLine, OrthogonalConstructionError>,
    ): PerpendicularLine =
        assertIs<GMResult.Ok<PerpendicularLine>>(result).value

    private fun perpendicularSegment(
        result: GMResult<
            PerpendicularSegmentLine,
            OrthogonalConstructionError,
            >,
    ): PerpendicularSegmentLine =
        assertIs<GMResult.Ok<PerpendicularSegmentLine>>(result).value

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
