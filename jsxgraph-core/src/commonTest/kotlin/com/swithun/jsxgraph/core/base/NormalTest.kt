/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NormalTest {
    @Test
    fun lineAndCircleBranchesMatchOfficialIdentityGeometryAndUpdates() {
        val board = board("line-circle")
        val a = point(board, -4.0, -2.0, "A")
        val b = point(board, 3.0, 2.0, "B")
        val sourceLine = line(board, a, b, "sourceLine")
        val linePoint = point(board, -1.0, 4.0, "linePoint")
        val lineNormal = normal(
            Normal.create(
                board = board,
                firstParent = sourceLine,
                secondParent = linePoint,
                id = "lineNormal",
                name = "",
                straightFirst = false,
                straightLast = true,
                pointId = "lineNormalPoint",
                pointName = "",
            ),
        )
        val reverseLineNormal = normal(
            Normal.create(
                board = board,
                firstParent = linePoint,
                secondParent = sourceLine,
                id = "reverseLineNormal",
                name = "",
                straightFirst = true,
                straightLast = false,
                pointId = "reverseLineNormalPoint",
                pointName = "",
            ),
        )

        val center = point(board, 2.0, -1.0, "center")
        val radiusPoint = point(board, 5.0, -1.0, "radiusPoint")
        val sourceCircle = circle(
            board,
            center,
            radiusPoint,
            "sourceCircle",
        )
        val circlePoint = point(board, 4.0, 3.0, "circlePoint")
        val circleNormal = normal(
            Normal.create(
                board = board,
                firstParent = sourceCircle,
                secondParent = circlePoint,
                id = "circleNormal",
                name = "",
            ),
        )
        val reverseCircleNormal = normal(
            Normal.create(
                board = board,
                firstParent = circlePoint,
                secondParent = sourceCircle,
                id = "reverseCircleNormal",
                name = "",
            ),
        )
        board.update()

        for (output in listOf(lineNormal, reverseLineNormal)) {
            assertEquals("normal", output.elType)
            assertEquals(Const.OBJECT_TYPE_LINE, output.type)
            assertEquals(Const.OBJECT_CLASS_LINE, output.elementClass)
            assertFalse(output.constrained)
            assertTrue(output.isDraggable)
            assertSame(linePoint, output.point1)
            val helper = assertIs<Point>(output.normalPoint)
            assertSame(helper, output.point2)
            assertSame(helper, output.subs["point"])
            assertEquals(
                listOf<GeometryElement>(linePoint, helper, helper),
                output.inherits,
            )
            assertTrue(helper.type == Const.OBJECT_TYPE_CAS)
            assertTrue(helper.isDraggable)
            assertFalse(helper.isFixed)
            assertArrayMatches(
                doubleArrayOf(
                    0.0,
                    0.49613893835683387,
                    -0.8682431421244593,
                ),
                helper.coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(
                    -1.116312611302876,
                    0.8682431421244593,
                    0.49613893835683387,
                ),
                output.stdform.copyOfRange(0, 3),
            )
            assertSame(output, sourceLine.childElements[output.id])
            assertSame(output, linePoint.childElements[output.id])
        }
        assertFalse(lineNormal.straightFirst)
        assertTrue(lineNormal.straightLast)
        assertTrue(reverseLineNormal.straightFirst)
        assertFalse(reverseLineNormal.straightLast)
        assertEquals(
            listOf("sourceLine", "linePoint"),
            lineNormal.parents,
        )
        assertEquals(
            listOf("linePoint", "sourceLine"),
            reverseLineNormal.parents,
        )

        for (output in listOf(circleNormal, reverseCircleNormal)) {
            assertEquals("normal", output.elType)
            assertFalse(output.constrained)
            assertTrue(output.isDraggable)
            assertSame(center, output.point1)
            assertSame(circlePoint, output.point2)
            assertEquals(
                listOf<GeometryElement>(center, circlePoint),
                output.inherits,
            )
            assertEquals(null, output.normalPoint)
            assertTrue(output.subs.isEmpty())
            assertArrayMatches(
                doubleArrayOf(
                    2.23606797749979,
                    -0.8944271909999159,
                    0.4472135954999579,
                ),
                output.stdform.copyOfRange(0, 3),
            )
            assertSame(output, sourceCircle.childElements[output.id])
            assertSame(output, circlePoint.childElements[output.id])
        }
        assertEquals(
            listOf("sourceCircle", "circlePoint"),
            circleNormal.parents,
        )
        assertEquals(
            listOf("circlePoint", "sourceCircle"),
            reverseCircleNormal.parents,
        )

        a.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-5.0, 3.0),
        )
        b.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, -4.0),
        )
        linePoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 4.0),
        )
        center.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        circlePoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, -2.0),
        )
        board.update()

        for (output in listOf(lineNormal, reverseLineNormal)) {
            assertArrayMatches(
                doubleArrayOf(0.0, -0.7071067811865476, -0.7071067811865476),
                assertIs<Point>(output.normalPoint).coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(0.0, 0.7071067811865476, -0.7071067811865476),
                output.stdform.copyOfRange(0, 3),
            )
        }
        for (output in listOf(circleNormal, reverseCircleNormal)) {
            assertArrayMatches(
                doubleArrayOf(0.0, 0.7071067811865475, 0.7071067811865475),
                output.stdform.copyOfRange(0, 3),
            )
        }
    }

    @Test
    fun curveBranchesMatchOfficialContinuousAndPlotGeometry() {
        val board = board("curves")
        val functionCurve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "0.5 * x * x - 2",
                minimumSource = "-5",
                maximumSource = "5",
                id = "functionCurve",
                name = "",
            ),
        )
        val functionPoint = point(board, -2.0, 3.0, "functionPoint")
        val functionNormal = curveNormal(
            board,
            functionCurve,
            functionPoint,
            "functionNormal",
        )
        val parameterCurve = curve(
            Curve.createParametric(
                board = board,
                xSource = "2 * cos(x) + 3",
                ySource = "1.5 * sin(x) - 1",
                minimumSource = "0",
                maximumSource = "6.283185307179586",
                id = "parameterCurve",
                name = "",
            ),
        )
        val parameterPoint = point(board, 5.5, 1.25, "parameterPoint")
        val parameterNormal = normal(
            Normal.create(
                board = board,
                firstParent = parameterPoint,
                secondParent = parameterCurve,
                id = "parameterNormal",
                name = "",
                point1Id = "parameterNormalPoint1",
                point1Name = "",
                point2Id = "parameterNormalPoint2",
                point2Name = "",
            ),
        )
        val plotCurve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-4.0, -1.0, 2.0, 5.0),
                dataY = doubleArrayOf(-2.0, 2.0, -1.0, 3.0),
                id = "plotCurve",
                name = "",
            ),
        )
        val plotPoint = point(board, 0.25, 2.5, "plotPoint")
        val plotNormal = curveNormal(
            board,
            plotCurve,
            plotPoint,
            "plotNormal",
            straightFirst = false,
            straightLast = false,
        )
        board.update()

        for (output in listOf(
            functionNormal,
            parameterNormal,
            plotNormal,
        )) {
            assertEquals("normal", output.elType)
            assertEquals(Const.OBJECT_TYPE_LINE, output.type)
            assertTrue(output.constrained)
            assertFalse(output.isDraggable)
            assertEquals(
                listOf<GeometryElement>(output.point1, output.point2),
                output.inherits,
            )
            assertTrue(output.point1.type == Const.OBJECT_TYPE_CAS)
            assertTrue(output.point2.type == Const.OBJECT_TYPE_CAS)
            assertFalse(output.point1.isDraggable)
            assertFalse(output.point2.isDraggable)
        }
        assertEquals(
            listOf("functionCurve", "functionPoint"),
            functionNormal.parents,
        )
        assertEquals(
            listOf("parameterPoint", "parameterCurve"),
            parameterNormal.parents,
        )
        assertFalse(plotNormal.straightFirst)
        assertFalse(plotNormal.straightLast)
        assertArrayMatches(
            doubleArrayOf(
                -3.5777087639996625,
                -0.44721359549995754,
                0.8944271909999161,
            ),
            functionNormal.stdform.copyOfRange(0, 3),
        )
        assertArrayMatches(
            doubleArrayOf(
                -3.450846328109138,
                0.7719108703581399,
                -0.6357307670885051,
            ),
            parameterNormal.stdform.copyOfRange(0, 3),
            tolerance = CURVE_TOLERANCE,
        )
        assertArrayMatches(
            doubleArrayOf(
                -1.5909902576697323,
                -0.707106781186548,
                0.707106781186547,
            ),
            plotNormal.stdform.copyOfRange(0, 3),
        )

        functionPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.5, 4.0),
        )
        parameterPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.5, -1.5),
        )
        plotPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 4.0),
        )
        board.update()

        assertArrayMatches(
            doubleArrayOf(
                4.160251471689216,
                -0.5547001962252295,
                -0.8320502943378435,
            ),
            functionNormal.stdform.copyOfRange(0, 3),
        )
        assertArrayMatches(
            doubleArrayOf(
                1.5805632495909112,
                -0.29051708281480554,
                0.9568698054556719,
            ),
            parameterNormal.stdform.copyOfRange(0, 3),
            tolerance = CURVE_TOLERANCE,
        )
        assertArrayMatches(
            doubleArrayOf(5.4, -0.6, -0.8),
            plotNormal.stdform.copyOfRange(0, 3),
        )
    }

    @Test
    fun directAndParentRemovalRetainHelpersLikeOfficial() {
        for (kind in listOf("line", "circle", "curve")) {
            for (removedParent in listOf("object", "point", "normal")) {
                val board = board("$kind-$removedParent")
                val point = point(board, 3.0, 3.0, "point")
                val source = when (kind) {
                    "line" -> line(
                        board,
                        point(board, -2.0, 0.0, "p1"),
                        point(board, 2.0, 1.0, "p2"),
                        "source",
                    )
                    "circle" -> circle(
                        board,
                        point(board, 0.0, 0.0, "center"),
                        point(board, 2.0, 0.0, "radius"),
                        "source",
                    )
                    else -> curve(
                        Curve.createFunctionGraph(
                            board = board,
                            ySource = "x * x",
                            minimumSource = "-4",
                            maximumSource = "4",
                            id = "source",
                            name = "",
                        ),
                    )
                }
                val output = normal(
                    Normal.create(
                        board = board,
                        firstParent = source,
                        secondParent = point,
                        id = "normal",
                        pointId = "normalPoint",
                        point1Id = "normalPoint1",
                        point2Id = "normalPoint2",
                    ),
                )
                val helpers = listOfNotNull(
                    output.normalPoint,
                    output.point1.takeIf { it !== point && it !== source },
                    output.point2.takeIf { it !== point && it !== source },
                ).distinct()

                board.removeObject(
                    when (removedParent) {
                        "object" -> source
                        "point" -> point
                        else -> output
                    },
                )

                if (removedParent == "point") {
                    assertEquals(null, board.elementById(point.id))
                } else {
                    assertSame(point, board.elementById(point.id))
                }
                assertEquals(
                    null,
                    board.elementById(output.id),
                )
                for (helper in helpers) {
                    assertSame(helper, board.elementById(helper.id))
                }
            }
        }
    }

    @Test
    fun failuresAreStructuredAndFactoryCreationIsAtomic() {
        val board = board("errors")
        val a = point(board, 0.0, 0.0, "A")
        val b = point(board, 1.0, 1.0, "B")
        assertEquals(
            NormalError.UnsupportedParents(listOf("point", "point")),
            assertIs<GMResult.Err<NormalError>>(
                Normal.create(
                    board = board,
                    firstParent = a,
                    secondParent = b,
                ),
            ).error,
        )

        val otherBoard = board("other")
        val foreignPoint = point(otherBoard, 2.0, 3.0, "foreign")
        val sourceLine = line(board, a, b, "source")
        assertEquals(
            NormalError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<NormalError>>(
                Normal.create(
                    board = board,
                    firstParent = sourceLine,
                    secondParent = foreignPoint,
                ),
            ).error,
        )

        val before = board.objects.keys.toSet()
        val duplicate = assertIs<
            GMResult.Err<NormalError.LineCreation>,
            >(
            Normal.create(
                board = board,
                firstParent = sourceLine,
                secondParent = a,
                id = b.id,
                pointId = "temporaryHelper",
            ),
        ).error
        assertEquals(
            LineError.Registration(BoardError.DuplicateElementId(b.id)),
            duplicate.error,
        )
        assertEquals(before, board.objects.keys.toSet())
        assertEquals(null, board.elementById("temporaryHelper"))
    }

    private fun curveNormal(
        board: Board,
        curve: Curve,
        point: Point,
        id: String,
        straightFirst: Boolean = true,
        straightLast: Boolean = true,
    ): Line =
        normal(
            Normal.create(
                board = board,
                firstParent = curve,
                secondParent = point,
                id = id,
                name = "",
                straightFirst = straightFirst,
                straightLast = straightLast,
                point1Id = "${id}Point1",
                point1Name = "",
                point2Id = "${id}Point2",
                point2Name = "",
            ),
        )

    private fun board(id: String): Board =
        Board(
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
        id: String,
    ): Point =
        when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> throw AssertionError(result.error)
        }

    private fun line(
        board: Board,
        point1: Point,
        point2: Point,
        id: String,
    ): Line =
        when (
            val result = Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> throw AssertionError(result.error)
        }

    private fun circle(
        board: Board,
        center: Point,
        radiusPoint: Point,
        id: String,
    ): Circle =
        when (
            val result = Circle.create(
                board = board,
                center = center,
                point2 = radiusPoint,
                id = id,
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> throw AssertionError(result.error)
        }

    private fun curve(result: GMResult<Curve, CurveError>): Curve =
        when (result) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> throw AssertionError(result.error)
        }

    private fun normal(result: GMResult<Line, NormalError>): Line =
        when (result) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> throw AssertionError(result.error)
        }

    private fun assertArrayMatches(
        expected: DoubleArray,
        actual: DoubleArray,
        tolerance: Double = 1.0e-12,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            val expectedValue = expected[index]
            val actualValue = actual[index]
            when {
                expectedValue.isNaN() ->
                    assertTrue(actualValue.isNaN(), "index=$index")
                expectedValue.isInfinite() ->
                    assertEquals(expectedValue, actualValue, "index=$index")
                else ->
                    assertTrue(
                        abs(expectedValue - actualValue) <= tolerance,
                        "index=$index expected=$expectedValue " +
                            "actual=$actualValue",
                    )
            }
        }
    }

    private companion object {
        const val CURVE_TOLERANCE = 2.0e-5
    }
}
