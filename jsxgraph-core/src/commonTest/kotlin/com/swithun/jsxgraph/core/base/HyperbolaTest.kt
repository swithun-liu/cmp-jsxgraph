/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HyperbolaTest {
    @Test
    fun pointHyperbolaMatchesOfficialCoordinatesMetadataAndUpdates() {
        val board = board("point-hyperbola")
        val focus1 = point(board, -3.0, 0.0, "focus1")
        val focus2 = point(board, 3.0, 0.0, "focus2")
        val hyperbolaPoint = point(board, 5.0, 2.0, "hyperbolaPoint")
        val hyperbola = hyperbola(
            Hyperbola.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                pointOnHyperbola = hyperbolaPoint,
                id = "hyperbola",
                name = "point-hyperbola",
                needsRegularUpdate = false,
            ),
        )
        val center = assertIs<Point>(hyperbola.center)

        assertEquals("curve", hyperbola.elType)
        assertEquals(Const.OBJECT_TYPE_CONIC, hyperbola.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, hyperbola.elementClass)
        assertEquals("parameter", hyperbola.curveType)
        assertEquals("hyperbola", hyperbola.id)
        assertEquals("point-hyperbola", hyperbola.name)
        assertFalse(hyperbola.needsRegularUpdate)
        assertTrue(hyperbola.isHyperbola)
        assertFalse(hyperbola.isEllipse)
        assertTrue(hyperbola.isDraggable)
        assertEquals(Curve.DEFAULT_SAMPLE_COUNT, hyperbola.numberPoints)
        assertEquals(
            -1.0001 * PI,
            hyperbola.minX(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            1.0001 * PI,
            hyperbola.maxX(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            5.417784126489131,
            hyperbola.majorAxis(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            -2.7088920632445657,
            hyperbola.X(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(0.0, hyperbola.Y(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, center.X(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, center.Y(), absoluteTolerance = TOLERANCE)
        assertEquals(Const.OBJECT_TYPE_CAS, center.type)
        assertEquals("point", center.elType)
        assertFalse(center.isDraggable)
        assertEquals(emptyList(), center.parents)
        assertSame(center, hyperbola.midpoint)
        assertSame(center, hyperbola.subs["center"])
        assertEquals(listOf(focus1, focus2), hyperbola.foci)
        assertSame(hyperbolaPoint, hyperbola.pointOnHyperbola)
        assertEquals(
            listOf<GeometryElement>(
                center,
                focus1,
                focus2,
                hyperbolaPoint,
            ),
            hyperbola.inherits,
        )
        assertEquals(
            listOf("focus1", "focus2", "hyperbolaPoint"),
            hyperbola.parents,
        )
        for (parent in listOf(center, focus1, focus2, hyperbolaPoint)) {
            assertSame(hyperbola, parent.childElements[hyperbola.id])
        }
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(-1.6619037896905997, 0.0, 0.0),
                doubleArrayOf(0.0, 0.2264761515876239, -0.0),
                doubleArrayOf(0.0, -0.0, -1.0),
            ),
            actual = hyperbola.quadraticform,
        )

        focus1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-4.0, 1.0),
        )
        focus2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, -1.0),
        )
        hyperbolaPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(5.0, 3.0),
        )
        board.fullUpdate()

        assertEquals(-1.0, center.X(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, center.Y(), absoluteTolerance = TOLERANCE)
        assertEquals(
            4.219544457292887,
            hyperbola.majorAxis(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            -3.0015056760076906,
            hyperbola.X(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            0.6671685586692302,
            hyperbola.Y(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(
                    -4.5269066975029295,
                    1.021954445729289,
                    -0.6739848152430963,
                ),
                doubleArrayOf(
                    1.021954445729289,
                    1.021954445729289,
                    -0.6739848152430963,
                ),
                doubleArrayOf(
                    -0.6739848152430963,
                    -0.6739848152430963,
                    -0.7753383949189679,
                ),
            ),
            actual = hyperbola.quadraticform,
        )
    }

    @Test
    fun numericAndFunctionMajorAxesPreserveDomainsAndDynamicEvaluation() {
        val numericBoard = board("numeric")
        val numeric = hyperbola(
            Hyperbola.create(
                board = numericBoard,
                focus1 = point(numericBoard, -3.0, 1.0, "focus1"),
                focus2 = point(numericBoard, 3.0, 1.0, "focus2"),
                majorAxis = 4.0,
                minimum = -0.5 * PI,
                maximum = 0.5 * PI,
                sampleCount = 64,
                id = "numeric",
                name = "",
            ),
        )

        assertEquals(4.0, numeric.majorAxis())
        assertEquals(-0.5 * PI, numeric.minX())
        assertEquals(0.5 * PI, numeric.maxX())
        assertEquals(64, numeric.numberPoints)
        assertEquals(
            -3.0,
            numeric.X(-0.5 * PI),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            -1.5,
            numeric.Y(-0.5 * PI),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(listOf("focus1", "focus2"), numeric.parents)
        assertEquals(3, numeric.inherits.size)
        assertEquals(null, numeric.pointOnHyperbola)

        val functionBoard = board("function")
        val majorAxis = MutableNumberFunction(4.0)
        val function = hyperbola(
            Hyperbola.create(
                board = functionBoard,
                focus1 = point(functionBoard, -3.0, 0.0, "focus1"),
                focus2 = point(functionBoard, 3.0, 0.0, "focus2"),
                majorAxisTerm = majorAxis,
                sampleCount = 32,
                id = "function",
                name = "",
            ),
        )
        assertEquals(4.0, function.majorAxis())
        assertEquals(-2.0, function.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(-5.0, function.quadraticform[0][0])

        majorAxis.value = 2.0
        functionBoard.fullUpdate()

        assertEquals(2.0, function.majorAxis())
        assertEquals(-1.0, function.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(-8.0, function.quadraticform[0][0])
    }

    @Test
    fun implicitPointsAndCenterFollowOfficialRemovalRelationships() {
        val outputRemovalBoard = board("output-removal")
        val outputPoints = listOf(
            point(outputRemovalBoard, -3.0, 0.0),
            point(outputRemovalBoard, 3.0, 0.0),
            point(outputRemovalBoard, 5.0, 2.0),
        )
        val output = hyperbola(
            Hyperbola.create(
                board = outputRemovalBoard,
                focus1 = outputPoints[0],
                focus2 = outputPoints[1],
                pointOnHyperbola = outputPoints[2],
                parentlessPoints = outputPoints.toSet(),
                id = "hyperbola",
                name = "",
            ),
        )
        val center = assertIs<Point>(output.center)

        assertEquals(emptyList(), output.parents)
        assertEquals(5, outputRemovalBoard.objects.size)
        outputRemovalBoard.removeObject(output)

        assertEquals(4, outputRemovalBoard.objects.size)
        assertSame(center, outputRemovalBoard.elementById(center.id))
        for (point in outputPoints) {
            assertSame(point, outputRemovalBoard.elementById(point.id))
        }

        val parentRemovalBoard = board("parent-removal")
        val parentPoints = listOf(
            point(parentRemovalBoard, -3.0, 0.0),
            point(parentRemovalBoard, 3.0, 0.0),
            point(parentRemovalBoard, 5.0, 2.0),
        )
        val parentOutput = hyperbola(
            Hyperbola.create(
                board = parentRemovalBoard,
                focus1 = parentPoints[0],
                focus2 = parentPoints[1],
                pointOnHyperbola = parentPoints[2],
                parentlessPoints = parentPoints.toSet(),
                id = "hyperbola",
                name = "",
            ),
        )
        val parentCenter = assertIs<Point>(parentOutput.center)

        parentRemovalBoard.removeObject(parentPoints[0])

        assertEquals(null, parentRemovalBoard.elementById(parentPoints[0].id))
        assertEquals(null, parentRemovalBoard.elementById(parentOutput.id))
        assertSame(parentCenter, parentRemovalBoard.elementById(parentCenter.id))
        assertSame(
            parentPoints[1],
            parentRemovalBoard.elementById(parentPoints[1].id),
        )
        assertSame(
            parentPoints[2],
            parentRemovalBoard.elementById(parentPoints[2].id),
        )
    }

    @Test
    fun invalidParentsAndFactoryFailuresAreStructuredAndAtomic() {
        val board = board("failures")
        val focus1 = point(board, -3.0, 0.0, "focus1")
        val focus2 = point(board, 3.0, 0.0, "focus2")
        val hyperbolaPoint = point(board, 5.0, 2.0, "hyperbolaPoint")
        val extra = point(board, 1.0, 1.0, "extra")

        assertEquals(
            HyperbolaError.ImplicitPointNotParent("extra"),
            assertIs<GMResult.Err<HyperbolaError>>(
                Hyperbola.create(
                    board = board,
                    focus1 = focus1,
                    focus2 = focus2,
                    pointOnHyperbola = hyperbolaPoint,
                    parentlessPoints = setOf(extra),
                ),
            ).error,
        )

        val otherBoard = board("other")
        val foreign = point(otherBoard, 0.0, 0.0, "foreign")
        assertEquals(
            HyperbolaError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<HyperbolaError>>(
                Hyperbola.create(
                    board = board,
                    focus1 = focus1,
                    focus2 = foreign,
                    pointOnHyperbola = hyperbolaPoint,
                ),
            ).error,
        )

        val duplicate = point(board, 7.0, 5.0, "duplicate")
        val beforeDuplicate = board.objects.keys.toSet()
        assertEquals(
            HyperbolaError.DuplicateElementId("duplicate"),
            assertIs<GMResult.Err<HyperbolaError>>(
                Hyperbola.create(
                    board = board,
                    focus1 = focus1,
                    focus2 = focus2,
                    pointOnHyperbola = hyperbolaPoint,
                    id = duplicate.id,
                ),
            ).error,
        )
        assertEquals(beforeDuplicate, board.objects.keys)

        val beforeInvalidSampleCount = board.objects.keys.toSet()
        val invalidSampleCount = assertIs<GMResult.Err<HyperbolaError>>(
            Hyperbola.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                pointOnHyperbola = hyperbolaPoint,
                sampleCount = 0,
            ),
        )
        assertEquals(
            CurveError.InvalidSampleCount(0, Curve.MAX_SAMPLE_COUNT),
            assertIs<HyperbolaError.CurveCreation>(
                invalidSampleCount.error,
            ).error,
        )
        assertEquals(beforeInvalidSampleCount, board.objects.keys)

        val beforeNonNumeric = board.objects.keys.toSet()
        val nonNumeric = assertIs<GMResult.Err<HyperbolaError>>(
            Hyperbola.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                majorAxisTerm = NonNumericFunction,
            ),
        )
        assertEquals(
            CurveError.NonNumericExpression(
                term = "hyperbola.majorAxis",
                actualType = "string",
            ),
            assertIs<HyperbolaError.CurveCreation>(nonNumeric.error).error,
        )
        assertEquals(beforeNonNumeric, board.objects.keys)
    }

    @Test
    fun degenerateMajorAxesPreserveJavaScriptDoubleArithmetic() {
        val shorter = numericHyperbola(4.0)
        assertEquals(-2.0, shorter.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(-5.0, shorter.quadraticform[0][0])
        assertEquals(1.25, shorter.quadraticform[1][1])

        val equal = numericHyperbola(6.0)
        assertEquals(-3.0, equal.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, equal.quadraticform[0][0])
        assertEquals(0.0, equal.quadraticform[1][1])

        val zero = numericHyperbola(0.0)
        assertEquals(0.0, zero.X(0.0))
        assertTrue(zero.quadraticform[0].all(Double::isNaN))
        assertTrue(zero.quadraticform[1][1].isInfinite())

        val negative = numericHyperbola(-4.0)
        assertEquals(2.0, negative.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(-5.0, negative.quadraticform[0][0])

        val nan = numericHyperbola(Double.NaN)
        assertTrue(nan.X(0.0).isNaN())
        assertTrue(nan.quadraticform.all { row -> row.all(Double::isNaN) })

        val infinity = numericHyperbola(Double.POSITIVE_INFINITY)
        assertTrue(infinity.X(0.0).isNaN())
        assertTrue(infinity.quadraticform[0].all(Double::isNaN))
        assertEquals(-1.0, infinity.quadraticform[1][1])
        assertEquals(-1.0, infinity.quadraticform[2][2])
    }

    private fun numericHyperbola(majorAxis: Double): Curve {
        val board = board("degenerate-$majorAxis")
        return hyperbola(
            Hyperbola.create(
                board = board,
                focus1 = point(board, -3.0, 0.0),
                focus2 = point(board, 3.0, 0.0),
                majorAxis = majorAxis,
                sampleCount = 8,
                name = "",
            ),
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

    private fun hyperbola(
        result: GMResult<Curve, HyperbolaError>,
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

    private data object NonNumericFunction : JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> = emptyMap()

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(JessieCodeRuntimeValue.StringValue("not-a-number"))
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
