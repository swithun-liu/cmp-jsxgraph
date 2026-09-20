package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.ClipError
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeCallable
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IntersectionTest {
    @Test
    fun lineLineIntersectionMatchesOfficialIdentityDependenciesAndUpdates() {
        val board = board()
        val horizontalStart = point(board, -6.0, 0.0, "horizontalStart")
        val horizontalEnd = point(board, 6.0, 0.0, "horizontalEnd")
        val verticalStart = point(board, 1.0, -5.0, "verticalStart")
        val verticalEnd = point(board, 1.0, 5.0, "verticalEnd")
        val horizontal = line(
            board,
            horizontalStart,
            horizontalEnd,
            "horizontal",
        )
        val vertical = line(
            board,
            verticalStart,
            verticalEnd,
            "vertical",
        )
        val output = intersection(
            IntersectionPoint.create(
                board = board,
                first = horizontal,
                second = vertical,
                id = "output",
                name = "",
            ),
        )

        assertEquals("intersection", output.elType)
        assertEquals(Const.OBJECT_TYPE_INTERSECTION, output.type)
        assertEquals(Const.OBJECT_TYPE_POINT, output.originalType)
        assertEquals(Const.OBJECT_CLASS_POINT, output.elementClass)
        assertEquals(listOf("horizontal", "vertical"), output.parents)
        assertFalse(output.isDraggable)
        assertTrue(output.isReal)
        assertCoordinates(doubleArrayOf(1.0, 1.0, 0.0), output.Coords(true))
        assertSame(output, horizontal.childElements[output.id])
        assertSame(output, vertical.childElements[output.id])

        verticalStart.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-5.0, -5.0),
        )
        verticalEnd.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-5.0, 5.0),
        )
        board.update()

        assertCoordinates(doubleArrayOf(1.0, -5.0, 0.0), output.Coords(true))
    }

    @Test
    fun lineCircleAndCircleCircleIndicesMatchOfficialOrdering() {
        val board = board()
        val horizontal = line(
            board,
            point(board, -6.0, 0.0),
            point(board, 6.0, 0.0),
        )
        val firstCircle = circle(
            board,
            point(board, 0.0, 0.0),
            radius = 3.0,
        )
        val secondCenter = point(board, 3.0, 0.0)
        val secondCircle = circle(
            board,
            secondCenter,
            radius = 2.5,
        )

        val lineCircle0 = intersection(
            IntersectionPoint.create(
                board,
                horizontal,
                firstCircle,
                firstIndex = IntersectionIndexSource.Number(0.0),
            ),
        )
        val circleLine1 = intersection(
            IntersectionPoint.create(
                board,
                firstCircle,
                horizontal,
                firstIndex = IntersectionIndexSource.Number(1.0),
            ),
        )
        val circleCircle0 = intersection(
            IntersectionPoint.create(
                board,
                firstCircle,
                secondCircle,
                firstIndex = IntersectionIndexSource.Number(0.0),
            ),
        )
        val circleCircle1 = intersection(
            IntersectionPoint.create(
                board,
                firstCircle,
                secondCircle,
                firstIndex = IntersectionIndexSource.Number(1.0),
            ),
        )

        assertCoordinates(
            doubleArrayOf(1.0, 3.0, 0.0),
            lineCircle0.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(1.0, -3.0, 0.0),
            circleLine1.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(
                1.0,
                1.9583333333333337,
                -2.272648357215773,
            ),
            circleCircle0.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(
                1.0,
                1.9583333333333337,
                2.272648357215773,
            ),
            circleCircle1.Coords(true),
        )

        secondCenter.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(8.0, 0.0),
        )
        board.update()

        assertFalse(circleCircle0.isReal)
        assertFalse(circleCircle1.isReal)
        assertCoordinates(
            doubleArrayOf(0.0, 0.0, 0.0),
            circleCircle0.Coords(true),
        )
    }

    @Test
    fun segmentRespectsAlwaysIntersectAndParallelLinesRemainIdeal() {
        val board = board()
        val segment = line(
            board,
            point(board, -1.0, 2.0),
            point(board, 1.0, 2.0),
        ).configureVisibleRange(
            straightFirst = false,
            straightLast = false,
        )
        val circle = circle(
            board,
            point(board, 0.0, 0.0),
            radius = 3.0,
        )
        val extended = intersection(
            IntersectionPoint.create(
                board = board,
                first = segment,
                second = circle,
                alwaysIntersect = true,
            ),
        )
        val clipped = intersection(
            IntersectionPoint.create(
                board = board,
                first = segment,
                second = circle,
                alwaysIntersect = false,
            ),
        )
        assertCoordinates(
            doubleArrayOf(1.0, 2.23606797749979, 2.0),
            extended.Coords(true),
        )
        assertFalse(clipped.isReal)
        assertTrue(clipped.X().isNaN())
        assertTrue(clipped.Y().isNaN())
        assertEquals(0.0, clipped.Z())

        val parallelFirst = line(
            board,
            point(board, -2.0, 4.0),
            point(board, 2.0, 4.0),
        )
        val parallelSecond = line(
            board,
            point(board, -2.0, 6.0),
            point(board, 2.0, 6.0),
        )
        val ideal = intersection(
            IntersectionPoint.create(
                board,
                parallelFirst,
                parallelSecond,
            ),
        )
        assertFalse(ideal.isReal)
        assertCoordinates(
            doubleArrayOf(0.0, -2.0, 0.0),
            ideal.Coords(true),
        )
    }

    @Test
    fun otherIntersectionExcludesPointsWithoutAddingChildDependencies() {
        val board = board()
        val horizontal = line(
            board,
            point(board, -6.0, 0.0),
            point(board, 6.0, 0.0),
            "horizontal",
        )
        val firstCircle = circle(
            board,
            point(board, 0.0, 0.0),
            radius = 3.0,
            id = "firstCircle",
        )
        val first = intersection(
            IntersectionPoint.create(
                board,
                horizontal,
                firstCircle,
                firstIndex = IntersectionIndexSource.Number(0.0),
                id = "first",
            ),
        )
        val other = otherIntersection(
            OtherIntersectionPoint.create(
                board = board,
                first = horizontal,
                second = firstCircle,
                excludedPoints = listOf(first),
                id = "other",
            ),
        )

        assertEquals("otherintersection", other.elType)
        assertEquals(
            listOf("horizontal", "firstCircle", "first"),
            other.parents,
        )
        assertCoordinates(doubleArrayOf(1.0, -3.0, 0.0), other.Coords(true))
        assertSame(other, horizontal.childElements[other.id])
        assertSame(other, firstCircle.childElements[other.id])
        assertFalse(other.id in first.childElements)

        board.removeObject(other)

        assertEquals(null, board.elementById(other.id))
        assertFalse(other.id in horizontal.childElements)
        assertFalse(other.id in firstCircle.childElements)
        assertSame(first, board.elementById(first.id))
    }

    @Test
    fun factoriesReportStructuredParentAndRegistrationFailures() {
        val board = board()
        val first = point(board, -1.0, 0.0, "taken")
        val second = point(board, 1.0, 0.0)
        val line = line(board, first, second)
        val foreignBoard = board("foreign")
        val foreignCircle = circle(
            foreignBoard,
            point(foreignBoard, 0.0, 0.0),
            radius = 1.0,
        )

        val mismatch = assertIs<
            GMResult.Err<IntersectionError.ParentBoardMismatch>,
            >(
            IntersectionPoint.create(
                board,
                line,
                foreignCircle,
            ),
        )
        assertEquals(
            IntersectionError.ParentBoardMismatch(1),
            mismatch.error,
        )
        val duplicate = assertIs<
            GMResult.Err<IntersectionError.Registration>,
            >(
            IntersectionPoint.create(
                board,
                line,
                line,
                id = "taken",
            ),
        )
        assertEquals(
            IntersectionError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            duplicate.error,
        )
        val twoLines = assertIs<
            GMResult.Err<IntersectionError.UnsupportedParentTypes>,
            >(
            OtherIntersectionPoint.create(
                board,
                line,
                line,
                excludedPoints = listOf(first),
            ),
        )
        assertEquals(
            IntersectionError.UnsupportedParentTypes("line", "line"),
            twoLines.error,
        )
    }

    @Test
    fun curveLineAndContinuousFunctionIndicesMatchOfficial() {
        val board = board()
        val horizontal = line(
            board,
            point(board, -7.0, 0.0),
            point(board, 7.0, 0.0),
        )
        val zigzag = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-4.0, -2.0, 0.0, 2.0, 4.0),
                dataY = doubleArrayOf(3.0, -1.0, 3.0, -1.0, 3.0),
            ),
        )
        val expected = listOf(-2.5, -1.5, 1.5, 2.5)
        for ((index, x) in expected.withIndex()) {
            val output = intersection(
                IntersectionPoint.create(
                    board = board,
                    first = zigzag,
                    second = horizontal,
                    firstIndex =
                        IntersectionIndexSource.Number(index.toDouble()),
                ),
            )
            assertCoordinates(
                doubleArrayOf(1.0, x, 0.0),
                output.Coords(true),
            )
        }
        for (index in listOf(4.0, -1.0, 1.5)) {
            val output = intersection(
                IntersectionPoint.create(
                    board = board,
                    first = horizontal,
                    second = zigzag,
                    firstIndex = IntersectionIndexSource.Number(index),
                ),
            )
            assertCoordinates(
                doubleArrayOf(0.0, Double.NaN, Double.NaN),
                output.Coords(true),
            )
        }

        val functionGraph = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x * x - 1",
                minimumSource = "-3",
                maximumSource = "3",
            ),
        )
        val first = intersection(
            IntersectionPoint.create(
                board = board,
                first = functionGraph,
                second = horizontal,
                firstIndex = IntersectionIndexSource.Number(0.0),
            ),
        )
        val second = intersection(
            IntersectionPoint.create(
                board = board,
                first = functionGraph,
                second = horizontal,
                firstIndex = IntersectionIndexSource.Number(1.0),
            ),
        )
        assertCoordinates(
            doubleArrayOf(1.0, -0.9999996642907703, -6.714183468092827e-7),
            first.Coords(true),
            tolerance = 2.0e-5,
        )
        assertCoordinates(
            doubleArrayOf(1.0, 0.9999996632643912, -6.734711043288044e-7),
            second.Coords(true),
            tolerance = 2.0e-5,
        )
    }

    @Test
    fun secondIndexFunctionRemainsUnevaluatedAcrossDispatchBranches() {
        val board = board()
        var callCount = 0
        val location = JessieCodeAstLocation(1, 0, 1, 1)
        val unusedFunction = JessieCodeRuntimeValue.FunctionValue(
            name = "unusedSecondIndex",
            callable = JessieCodeCallable { _, callLocation ->
                callCount += 1
                GMResult.Err(
                    JessieCodeRuntimeError.BuiltInInvocationFailure(
                        functionName = "unusedSecondIndex",
                        reason = "must remain unevaluated",
                        location = callLocation,
                    ),
                )
            },
        )
        val secondIndex = IntersectionIndexSource.Function(
            value = unusedFunction,
            location = location,
        )
        val horizontal = line(
            board,
            point(board, -7.0, 0.0),
            point(board, 7.0, 0.0),
        )
        val analytic = intersection(
            IntersectionPoint.create(
                board = board,
                first = horizontal,
                second = circle(
                    board,
                    point(board, 0.0, 0.0),
                    radius = 3.0,
                ),
                secondIndex = secondIndex,
            ),
        )
        val zigzag = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-4.0, -2.0, 0.0, 2.0, 4.0),
                dataY = doubleArrayOf(3.0, -1.0, 3.0, -1.0, 3.0),
            ),
        )
        val discrete = intersection(
            IntersectionPoint.create(
                board = board,
                first = zigzag,
                second = horizontal,
                secondIndex = secondIndex,
            ),
        )
        val parabola = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x * x - 1",
                minimumSource = "-3",
                maximumSource = "3",
            ),
        )
        val zero = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "0",
                minimumSource = "-3",
                maximumSource = "3",
            ),
        )
        val continuous = intersection(
            IntersectionPoint.create(
                board = board,
                first = parabola,
                second = zero,
                secondIndex = secondIndex,
            ),
        )
        val legacyFractional = intersection(
            IntersectionPoint.create(
                board = board,
                first = parabola,
                second = zero,
                firstIndex = IntersectionIndexSource.Number(0.5),
                secondIndex = secondIndex,
            ),
        )

        board.update()

        assertEquals(0, callCount)
        assertCoordinates(
            doubleArrayOf(1.0, 3.0, 0.0),
            analytic.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(1.0, -2.5, 0.0),
            discrete.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(1.0, -1.000000000095069, 0.0),
            continuous.Coords(true),
            tolerance = 2.0e-5,
        )
        assertFalse(legacyFractional.isReal)
    }

    @Test
    fun curveCurveArcSectorAndArcLineDispatchMatchOfficial() {
        val board = board()
        val center = point(board, 0.0, 0.0)
        val radiusPoint = point(board, 3.0, 0.0)
        val anglePoint = point(board, 0.0, 3.0)
        val arc = arc(
            Arc.create(
                board = board,
                center = center,
                radiuspoint = radiusPoint,
                anglepoint = anglePoint,
            ),
        )
        val sector = sector(
            Sector.create(
                board = board,
                center = center,
                radiuspoint = radiusPoint,
                anglepoint = anglePoint,
            ),
        )
        val horizontalCurve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-7.0, 7.0),
                dataY = doubleArrayOf(1.0, 1.0),
            ),
        )
        val arcCurve = intersection(
            IntersectionPoint.create(
                board = board,
                first = arc,
                second = horizontalCurve,
            ),
        )
        val sectorCurve = intersection(
            IntersectionPoint.create(
                board = board,
                first = sector,
                second = horizontalCurve,
            ),
        )
        assertCoordinates(
            doubleArrayOf(1.0, 2.8283770326345152, 1.0),
            arcCurve.Coords(true),
            tolerance = 1.0e-12,
        )
        assertCoordinates(
            doubleArrayOf(1.0, 2.8283770326345152, 1.0),
            sectorCurve.Coords(true),
            tolerance = 1.0e-12,
        )

        val vertical = line(
            board,
            point(board, 2.0, -5.0),
            point(board, 2.0, 5.0),
        )
        val clippedArcSecond = intersection(
            IntersectionPoint.create(
                board = board,
                first = arc,
                second = vertical,
                firstIndex = IntersectionIndexSource.Number(1.0),
                alwaysIntersect = false,
            ),
        )
        val reversedArcSecond = intersection(
            IntersectionPoint.create(
                board = board,
                first = vertical,
                second = arc,
                firstIndex = IntersectionIndexSource.Number(1.0),
                alwaysIntersect = false,
            ),
        )
        val sectorExtended = intersection(
            IntersectionPoint.create(
                board = board,
                first = sector,
                second = vertical,
            ),
        )
        val sectorClipped = intersection(
            IntersectionPoint.create(
                board = board,
                first = sector,
                second = vertical,
                alwaysIntersect = false,
            ),
        )
        assertCoordinates(
            doubleArrayOf(0.0, Double.NaN, Double.NaN),
            clippedArcSecond.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(1.0, 2.0, -2.23606797749979),
            reversedArcSecond.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(0.0, 0.0, -1.0),
            sectorExtended.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(0.0, Double.NaN, Double.NaN),
            sectorClipped.Coords(true),
        )

        anglePoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, -3.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(0.0, Double.NaN, Double.NaN),
            clippedArcSecond.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(0.0, 0.0, -1.0),
            sectorClipped.Coords(true),
        )
    }

    @Test
    fun polygonLineAndOtherCurveIntersectionsMatchOfficial() {
        val board = board()
        val horizontal = line(
            board,
            point(board, -7.0, 0.0),
            point(board, 7.0, 0.0),
        )
        val shortSegment = line(
            board,
            point(board, -1.0, 0.0),
            point(board, 1.0, 0.0),
        ).configureVisibleRange(
            straightFirst = false,
            straightLast = false,
        )
        val polygon = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(
                    point(board, -2.0, -2.0),
                    point(board, 2.0, -2.0),
                    point(board, 2.0, 2.0),
                    point(board, -2.0, 2.0),
                ),
            ),
        )
        val firstBorder = intersection(
            IntersectionPoint.create(
                board = board,
                first = polygon,
                second = horizontal,
            ),
        )
        val secondBorder = intersection(
            IntersectionPoint.create(
                board = board,
                first = horizontal,
                second = polygon,
                firstIndex = IntersectionIndexSource.Number(1.0),
            ),
        )
        val clipped = intersection(
            IntersectionPoint.create(
                board = board,
                first = polygon,
                second = shortSegment,
                alwaysIntersect = false,
            ),
        )
        assertCoordinates(
            doubleArrayOf(1.0, 2.0, 0.0),
            firstBorder.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(1.0, -2.0, 0.0),
            secondBorder.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(0.0, 0.0, 0.0),
            clipped.Coords(true),
        )

        val zigzag = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-4.0, -2.0, 0.0, 2.0, 4.0),
                dataY = doubleArrayOf(3.0, -1.0, 3.0, -1.0, 3.0),
            ),
        )
        val horizontalCurve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-7.0, 7.0),
                dataY = doubleArrayOf(1.0, 1.0),
            ),
        )
        for ((index, x) in listOf(-3.0, -1.0, 1.0, 3.0).withIndex()) {
            val output = intersection(
                IntersectionPoint.create(
                    board = board,
                    first = zigzag,
                    second = horizontalCurve,
                    firstIndex =
                        IntersectionIndexSource.Number(index.toDouble()),
                ),
            )
            assertCoordinates(
                doubleArrayOf(1.0, x, 1.0),
                output.Coords(true),
            )
        }
        val first = intersection(
            IntersectionPoint.create(
                board = board,
                first = zigzag,
                second = horizontal,
            ),
        )
        val other = otherIntersection(
            OtherIntersectionPoint.create(
                board = board,
                first = horizontal,
                second = zigzag,
                excludedPoints = listOf(first),
            ),
        )
        assertCoordinates(
            doubleArrayOf(1.0, -1.5, 0.0),
            other.Coords(true),
        )

        val circle = circle(
            board = board,
            center = point(board, 0.0, 0.0),
            radius = 2.5,
        )
        val firstCurveCircle = intersection(
            IntersectionPoint.create(
                board = board,
                first = zigzag,
                second = circle,
            ),
        )
        val otherCurveCircle = otherIntersection(
            OtherIntersectionPoint.create(
                board = board,
                first = circle,
                second = zigzag,
                excludedPoints = listOf(firstCurveCircle),
            ),
        )
        assertCoordinates(
            doubleArrayOf(
                1.0,
                0.25652665826380683,
                2.4869466834723863,
            ),
            firstCurveCircle.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(1.0, 2.5, 0.0),
            otherCurveCircle.Coords(true),
        )
    }

    @Test
    fun polygonCirclePathOrderingIndicesAndUpdatesMatchOfficial() {
        val board = board()
        val first = point(board, -2.0, -2.0)
        val second = point(board, 2.0, -2.0)
        val movable = point(board, 2.0, 2.0)
        val fourth = point(board, -2.0, 2.0)
        val polygon = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(first, second, movable, fourth),
            ),
        )
        val circle = circle(
            board = board,
            center = point(board, 0.0, 0.0),
            radius = 2.5,
        )
        val expected = listOf(
            -1.4998405909909862 to -2.0,
            1.4999891609691403 to -2.0,
            2.0 to -1.499885402306805,
            2.0 to 1.4998854023067922,
            1.499989160969136 to 2.0,
            -1.499840590990982 to 2.0,
            -2.0 to 1.4998756368270512,
            -2.0 to -1.4998756368270343,
        )
        val outputs = expected.indices.map { index ->
            intersection(
                IntersectionPoint.create(
                    board = board,
                    first = polygon,
                    second = circle,
                    firstIndex =
                        IntersectionIndexSource.Number(index.toDouble()),
                ),
            )
        }
        for (index in expected.indices) {
            assertCoordinates(
                doubleArrayOf(
                    1.0,
                    expected[index].first,
                    expected[index].second,
                ),
                outputs[index].Coords(true),
            )
        }

        val reverse = intersection(
            IntersectionPoint.create(
                board = board,
                first = circle,
                second = polygon,
            ),
        )
        assertCoordinates(
            doubleArrayOf(1.0, 2.0, 1.4998854023067922),
            reverse.Coords(true),
        )
        val outOfRange = intersection(
            IntersectionPoint.create(
                board = board,
                first = polygon,
                second = circle,
                firstIndex = IntersectionIndexSource.Number(8.0),
            ),
        )
        val negative = intersection(
            IntersectionPoint.create(
                board = board,
                first = polygon,
                second = circle,
                firstIndex = IntersectionIndexSource.Number(-1.0),
            ),
        )
        assertCoordinates(
            doubleArrayOf(0.0, 0.0, 0.0),
            outOfRange.Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(0.0, 0.0, 0.0),
            negative.Coords(true),
        )

        val fractional = assertIs<
            GMResult.Err<IntersectionError.ClipComputation>,
            >(
            IntersectionPoint.create(
                board = board,
                first = polygon,
                second = circle,
                firstIndex = IntersectionIndexSource.Number(0.5),
            ),
        )
        assertEquals(
            IntersectionError.ClipComputation(
                ClipError.InvalidIntersectionIndex(
                    index = 0.5,
                    intersectionCount = 8,
                ),
            ),
            fractional.error,
        )

        val nanFunction = JessieCodeRuntimeValue.FunctionValue(
            name = "nanIndex",
            callable = JessieCodeCallable { _, _ ->
                GMResult.Ok(
                    JessieCodeRuntimeValue.NumberValue(Double.NaN),
                )
            },
        )
        val nanOutput = intersection(
            IntersectionPoint.create(
                board = board,
                first = polygon,
                second = circle,
                firstIndex = IntersectionIndexSource.Function(
                    value = nanFunction,
                    location = JessieCodeAstLocation(1, 0, 1, 1),
                ),
            ),
        )
        assertCoordinates(
            doubleArrayOf(0.0, 0.0, 0.0),
            nanOutput.Coords(true),
        )

        movable.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, 2.0),
        )
        board.update()

        assertCoordinates(
            doubleArrayOf(
                1.0,
                2.2058845193854877,
                -1.176461922458049,
            ),
            outputs[2].Coords(true),
        )
        assertCoordinates(
            doubleArrayOf(1.0, 2.5, 0.0),
            outputs[3].Coords(true),
        )
    }

    @Test
    fun polygonPathVariantsMatchOfficialDegenerateRules() {
        val board = board()
        val polygon = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(
                    point(board, -2.0, -2.0),
                    point(board, 2.0, -2.0),
                    point(board, 2.0, 2.0),
                    point(board, -2.0, 2.0),
                ),
            ),
        )
        val diamond = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(
                    point(board, 0.0, -3.0),
                    point(board, 3.0, 0.0),
                    point(board, 0.0, 3.0),
                    point(board, -3.0, 0.0),
                ),
            ),
        )
        val overlap = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(
                    point(board, 1.0, -2.0),
                    point(board, 4.0, -2.0),
                    point(board, 4.0, 2.0),
                    point(board, 1.0, 2.0),
                ),
            ),
        )
        val sharedCorner = polygon(
            Polygon.create(
                board = board,
                vertices = listOf(
                    point(board, 2.0, 2.0),
                    point(board, 4.0, 2.0),
                    point(board, 4.0, 4.0),
                    point(board, 2.0, 4.0),
                ),
            ),
        )
        val triangle = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-3.0, 0.0, 3.0),
                dataY = doubleArrayOf(-3.0, 3.0, -3.0),
            ),
        )
        val center = point(board, 0.0, 0.0)
        val radiusPoint = point(board, 3.0, 0.0)
        val anglePoint = point(board, 0.0, 3.0)
        val arc = arc(
            Arc.create(
                board = board,
                center = center,
                radiuspoint = radiusPoint,
                anglepoint = anglePoint,
            ),
        )
        val sector = sector(
            Sector.create(
                board = board,
                center = center,
                radiuspoint = radiusPoint,
                anglepoint = anglePoint,
            ),
        )

        assertPathCoordinates(
            board = board,
            first = polygon,
            second = diamond,
            expected = listOf(
                -1.0 to -2.0,
                1.0 to -2.0,
                2.0 to -1.0,
                2.0 to 1.0,
                1.0 to 2.0,
                -1.0 to 2.0,
                -2.0 to 1.0,
                -2.0 to -1.0,
            ),
        )
        assertPathCoordinates(
            board = board,
            first = polygon,
            second = overlap,
            expected = listOf(
                1.0 to -2.0,
                2.0 to -2.0,
                2.0 to 2.0,
                1.0 to 2.0,
            ),
        )
        assertPathCoordinates(
            board = board,
            first = polygon,
            second = sharedCorner,
            expected = listOf(2.0 to 2.0),
        )
        assertPathCoordinates(
            board = board,
            first = polygon,
            second = triangle,
            expected = listOf(
                2.0 to -1.0,
                0.5 to 2.0,
                -0.5 to 2.0,
                -2.0 to -1.0,
            ),
        )
        assertPathCoordinates(
            board = board,
            first = polygon,
            second = arc,
            expected = listOf(
                2.0 to 1.0,
                1.0 to 2.0,
            ),
        )
        assertPathCoordinates(
            board = board,
            first = polygon,
            second = sector,
            expected = listOf(
                2.0 to 0.0,
                1.2246467991473532e-16 to 2.0,
            ),
        )
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
        x: Double,
        y: Double,
        id: String = "",
    ): Point = assertIs<GMResult.Ok<Point>>(
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

    private fun circle(
        board: Board,
        center: Point,
        radius: Double,
        id: String = "",
    ): Circle = assertIs<GMResult.Ok<Circle>>(
        Circle.create(
            board = board,
            center = center,
            radius = radius,
            id = id,
            name = "",
        ),
    ).value

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun arc(
        result: GMResult<Arc, ArcError>,
    ): Arc = assertIs<GMResult.Ok<Arc>>(result).value

    private fun sector(
        result: GMResult<Sector, SectorError>,
    ): Sector = assertIs<GMResult.Ok<Sector>>(result).value

    private fun polygon(
        result: GMResult<Polygon, PolygonError>,
    ): Polygon = assertIs<GMResult.Ok<Polygon>>(result).value

    private fun intersection(
        result: GMResult<IntersectionPoint, IntersectionError>,
    ): IntersectionPoint =
        assertIs<GMResult.Ok<IntersectionPoint>>(result).value

    private fun otherIntersection(
        result: GMResult<OtherIntersectionPoint, IntersectionError>,
    ): OtherIntersectionPoint =
        assertIs<GMResult.Ok<OtherIntersectionPoint>>(result).value

    private fun assertPathCoordinates(
        board: Board,
        first: GeometryElement,
        second: GeometryElement,
        expected: List<Pair<Double, Double>>,
    ) {
        for (index in expected.indices) {
            val output = intersection(
                IntersectionPoint.create(
                    board = board,
                    first = first,
                    second = second,
                    firstIndex =
                        IntersectionIndexSource.Number(index.toDouble()),
                ),
            )
            assertCoordinates(
                doubleArrayOf(
                    1.0,
                    expected[index].first,
                    expected[index].second,
                ),
                output.Coords(true),
            )
        }
        val ideal = intersection(
            IntersectionPoint.create(
                board = board,
                first = first,
                second = second,
                firstIndex =
                    IntersectionIndexSource.Number(
                        expected.size.toDouble(),
                    ),
            ),
        )
        assertCoordinates(
            doubleArrayOf(0.0, 0.0, 0.0),
            ideal.Coords(true),
        )
    }

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
