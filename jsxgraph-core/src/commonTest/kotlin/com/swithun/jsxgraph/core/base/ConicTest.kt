/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeNumericCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConicTest {
    @Test
    fun fivePointConicMatchesOfficialCoordinatesMetadataAndUpdates() {
        val board = board("five-point-conic")
        val points = listOf(
            point(board, 1.0, 5.0, "A"),
            point(board, 1.0, 2.0, "B"),
            point(board, 2.0, 0.0, "C"),
            point(board, 0.0, 0.0, "D"),
            point(board, -1.0, 5.0, "E"),
        )
        val conic = conic(
            Conic.create(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                point4 = points[3],
                point5 = points[4],
                sampleCount = 9,
                id = "conic",
                name = "five-point",
                needsRegularUpdate = false,
                centerId = "center",
                centerName = "",
                centerFixed = true,
            ),
        )
        val center = assertIs<Point>(conic.center)

        assertEquals("curve", conic.elType)
        assertEquals(Const.OBJECT_TYPE_CONIC, conic.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, conic.elementClass)
        assertEquals("parameter", conic.curveType)
        assertEquals("conic", conic.id)
        assertEquals("five-point", conic.name)
        assertFalse(conic.needsRegularUpdate)
        assertTrue(conic.isGenericConic)
        assertFalse(conic.isEllipse)
        assertFalse(conic.isHyperbola)
        assertFalse(conic.isParabola)
        assertEquals(9, conic.numberPoints)
        assertEquals(0.0, conic.minX())
        assertEquals(2.0 * kotlin.math.PI, conic.maxX())
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(0.0, 2400.0, -360.0),
                doubleArrayOf(2400.0, -2400.0, -480.0),
                doubleArrayOf(-360.0, -480.0, 240.0),
            ),
            actual = conic.quadraticform,
        )
        assertSample(
            conic,
            parameter = 0.0,
            expectedX = 0.16217371071583087,
            expectedY = 2.4140697495159498,
        )
        assertSample(
            conic,
            parameter = kotlin.math.PI / 6.0,
            expectedX = 0.021195254066431895,
            expectedY = 2.9422318513568433,
        )
        assertEquals(0.0, center.X())
        assertEquals(0.0, center.Y())
        assertTrue(center.isFixed)
        assertSame(center, conic.midpoint)
        assertSame(center, conic.subs["center"])
        assertEquals(
            listOf<GeometryElement>(center) + points,
            conic.inherits,
        )
        assertEquals(listOf("A", "B", "C", "D", "E"), conic.parents)
        assertSame(center, conic.childElements[center.id])
        assertEquals(emptyMap(), center.childElements)
        for (point in points) {
            assertSame(conic, point.childElements[conic.id])
        }

        board.fullUpdate()

        assertEquals(0.5, center.X(), absoluteTolerance = TOLERANCE)
        assertEquals(2.5, center.Y(), absoluteTolerance = TOLERANCE)

        points[4].setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 4.0),
        )
        board.fullUpdate()

        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(0.0, 2880.0, 96.0),
                doubleArrayOf(2880.0, -2880.0, -1104.0),
                doubleArrayOf(96.0, -1104.0, 288.0),
            ),
            actual = conic.quadraticform,
        )
        assertSample(
            conic,
            parameter = 0.0,
            expectedX = -0.4152349605725331,
            expectedY = 1.7809796332908405,
        )
        assertSample(
            conic,
            parameter = kotlin.math.PI / 2.0,
            expectedX = 2.3150843039815996,
            expectedY = -0.4168467163674919,
        )
        assertEquals(
            0.4566929133858268,
            center.X(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            1.4173228346456692,
            center.Y(),
            absoluteTolerance = TOLERANCE,
        )
    }

    @Test
    fun coefficientConicMatchesOfficialAndEvaluatesDynamicTerms() {
        val numericBoard = board("numeric-conic")
        val numeric = conic(
            Conic.create(
                board = numericBoard,
                coefficients = doubleArrayOf(1.0, 2.0, -4.0, 0.0, 0.0, 0.0),
                sampleCount = 9,
                id = "numeric",
                name = "",
                centerId = "numeric-center",
                centerName = "",
            ),
        )

        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(-4.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 2.0),
            ),
            actual = numeric.quadraticform,
        )
        assertSample(
            numeric,
            parameter = kotlin.math.PI / 6.0,
            expectedX = 1.7320508075688774,
            expectedY = 0.7071067811865474,
        )
        assertEquals(emptyList(), numeric.parents)
        assertEquals(
            listOf<GeometryElement>(assertIs<Point>(numeric.center)),
            numeric.inherits,
        )

        val branchBoard = board("conic-eigenvalue-branches")
        val secondBranch = conic(
            Conic.create(
                board = branchBoard,
                coefficients = doubleArrayOf(
                    1.0,
                    -1.0,
                    -1.0,
                    0.0,
                    0.0,
                    0.0,
                ),
                sampleCount = 9,
                name = "",
            ),
        )
        val thirdBranch = conic(
            Conic.create(
                board = branchBoard,
                coefficients = doubleArrayOf(
                    1.0,
                    -1.0,
                    1.0,
                    0.0,
                    0.0,
                    0.0,
                ),
                sampleCount = 9,
                name = "",
            ),
        )
        assertSample(
            secondBranch,
            parameter = kotlin.math.PI / 6.0,
            expectedX = 1.1547005383792515,
            expectedY = 0.5773502691896256,
        )
        assertSample(
            thirdBranch,
            parameter = kotlin.math.PI / 6.0,
            expectedX = 1.7320508075688776,
            expectedY = 2.0000000000000004,
        )

        val dynamicBoard = board("dynamic-conic")
        val xSquare = MutableNumberFunction(1.0)
        val ySquare = MutableNumberFunction(2.0)
        val constant = MutableNumberFunction(-4.0)
        val xLinear = MutableNumberFunction(0.0)
        val dynamic = conic(
            Conic.create(
                board = dynamicBoard,
                coefficientTerms = listOf(
                    xSquare,
                    ySquare,
                    constant,
                    JessieCodeNumericCoordinateFunction(0.0),
                    xLinear,
                    JessieCodeNumericCoordinateFunction(0.0),
                ),
                sampleCount = 9,
                id = "dynamic",
                name = "",
                centerId = "dynamic-center",
                centerName = "",
            ),
        )

        xSquare.value = 2.0
        ySquare.value = 1.0
        constant.value = -9.0
        xLinear.value = 2.0
        dynamicBoard.fullUpdate()

        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(-9.0, 2.0, 0.0),
                doubleArrayOf(2.0, 2.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0),
            ),
            actual = dynamic.quadraticform,
        )
        assertSample(
            dynamic,
            parameter = 0.0,
            expectedX = -3.3452078799117126,
            expectedY = 0.0,
        )
        assertSample(
            dynamic,
            parameter = kotlin.math.PI / 2.0,
            expectedX = -0.1761749776799063,
            expectedY = -3.105257584355699,
        )
        val center = assertIs<Point>(dynamic.center)
        assertEquals(-1.0, center.X(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, center.Y(), absoluteTolerance = TOLERANCE)
    }

    @Test
    fun implicitPointsAndCenterFollowOfficialOwnershipRelationships() {
        val board = board("implicit-conic")
        val points = listOf(
            point(board, 1.0, 5.0),
            point(board, 1.0, 2.0),
            point(board, 2.0, 0.0),
            point(board, 0.0, 0.0),
            point(board, -1.0, 5.0),
        )
        val conic = conic(
            Conic.create(
                board = board,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                point4 = points[3],
                point5 = points[4],
                parentlessPoints = points.toSet(),
                id = "conic",
                name = "",
            ),
        )
        val center = assertIs<Point>(conic.center)

        assertEquals(emptyList(), conic.parents)
        assertEquals(
            listOf<GeometryElement>(center) + points,
            conic.inherits,
        )
        assertSame(center, conic.childElements[center.id])

        board.removeObject(conic)

        assertEquals(null, board.elementById(conic.id))
        assertEquals(null, board.elementById(center.id))
        for (point in points) {
            assertSame(point, board.elementById(point.id))
        }
    }

    @Test
    fun degenerateAndInvalidInputsAreSafeAndAtomic() {
        val degenerateBoard = board("degenerate-conic")
        val degeneratePoints = List(5) { index ->
            point(
                board = degenerateBoard,
                x = index.toDouble(),
                y = 0.0,
            )
        }
        val degenerate = conic(
            Conic.create(
                board = degenerateBoard,
                point1 = degeneratePoints[0],
                point2 = degeneratePoints[1],
                point3 = degeneratePoints[2],
                point4 = degeneratePoints[3],
                point5 = degeneratePoints[4],
                sampleCount = 9,
                name = "",
            ),
        )
        assertTrue(
            degenerate.quadraticform.all { row ->
                row.all { value -> value == 0.0 }
            },
        )
        assertTrue(degenerate.X(0.0).isNaN())
        assertTrue(degenerate.Y(0.0).isNaN())

        val board = board("invalid-conic")
        val points = List(6) { index ->
            point(
                board = board,
                x = index.toDouble(),
                y = index.toDouble(),
                id = "P$index",
            )
        }
        assertEquals(
            ConicError.InvalidCoefficientCount(5),
            assertIs<GMResult.Err<ConicError>>(
                Conic.create(
                    board = board,
                    coefficientTerms = List(5) {
                        JessieCodeNumericCoordinateFunction(0.0)
                    },
                ),
            ).error,
        )
        assertEquals(
            ConicError.ImplicitPointNotParent("P5"),
            assertIs<GMResult.Err<ConicError>>(
                Conic.create(
                    board = board,
                    point1 = points[0],
                    point2 = points[1],
                    point3 = points[2],
                    point4 = points[3],
                    point5 = points[4],
                    parentlessPoints = setOf(points[5]),
                ),
            ).error,
        )

        val otherBoard = board("other")
        val foreign = point(otherBoard, 0.0, 0.0, "foreign")
        assertEquals(
            ConicError.ParentBoardMismatch(4),
            assertIs<GMResult.Err<ConicError>>(
                Conic.create(
                    board = board,
                    point1 = points[0],
                    point2 = points[1],
                    point3 = points[2],
                    point4 = points[3],
                    point5 = foreign,
                ),
            ).error,
        )

        val beforeFailure = board.objects.keys.toSet()
        val nonNumeric = assertIs<GMResult.Err<ConicError>>(
            Conic.create(
                board = board,
                coefficientTerms = listOf(
                    NonNumericFunction,
                    JessieCodeNumericCoordinateFunction(1.0),
                    JessieCodeNumericCoordinateFunction(-1.0),
                    JessieCodeNumericCoordinateFunction(0.0),
                    JessieCodeNumericCoordinateFunction(0.0),
                    JessieCodeNumericCoordinateFunction(0.0),
                ),
                id = "failed",
                centerId = "failed-center",
            ),
        )
        assertEquals(
            CurveError.NonNumericExpression(
                term = "conic.coefficient[0]",
                actualType = "string",
            ),
            assertIs<ConicError.CurveCreation>(
                nonNumeric.error,
            ).error,
        )
        assertEquals(beforeFailure, board.objects.keys)

        assertEquals(
            ConicError.DuplicateElementId("P0"),
            assertIs<GMResult.Err<ConicError>>(
                Conic.create(
                    board = board,
                    coefficients = doubleArrayOf(
                        1.0,
                        1.0,
                        -1.0,
                        0.0,
                        0.0,
                        0.0,
                    ),
                    id = "P0",
                ),
            ).error,
        )
    }

    private fun assertSample(
        conic: Curve,
        parameter: Double,
        expectedX: Double,
        expectedY: Double,
    ) {
        assertEquals(
            expectedX,
            conic.X(parameter),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            expectedY,
            conic.Y(parameter),
            absoluteTolerance = TOLERANCE,
        )
    }

    private fun assertMatrixEquals(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (rowIndex in expected.indices) {
            assertEquals(expected[rowIndex].size, actual[rowIndex].size)
            for (columnIndex in expected[rowIndex].indices) {
                assertEquals(
                    expected[rowIndex][columnIndex],
                    actual[rowIndex][columnIndex],
                    absoluteTolerance = TOLERANCE,
                    message = "[$rowIndex][$columnIndex]",
                )
            }
        }
    }

    private fun board(id: String): Board = Board(
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
        id: String = "",
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = doubleArrayOf(x, y),
            id = id,
            name = "",
        ),
    ).value

    private fun conic(
        result: GMResult<Curve, ConicError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private class MutableNumberFunction(
        var value: Double,
    ) : JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> = emptyMap()

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))
    }

    private data object NonNumericFunction :
        JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> = emptyMap()

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(
                JessieCodeRuntimeValue.StringValue("not-a-number"),
            )
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
