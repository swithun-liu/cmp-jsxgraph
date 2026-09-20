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

class EllipseTest {
    @Test
    fun pointEllipseMatchesOfficialCoordinatesMetadataAndUpdates() {
        val board = board("point-ellipse")
        val focus1 = point(board, -3.0, 0.0, "focus1")
        val focus2 = point(board, 3.0, 0.0, "focus2")
        val ellipsePoint = point(board, 0.0, 5.0, "ellipsePoint")
        val ellipse = ellipse(
            Ellipse.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                pointOnEllipse = ellipsePoint,
                id = "ellipse",
                name = "point-ellipse",
                needsRegularUpdate = false,
            ),
        )
        val center = assertIs<Point>(ellipse.center)

        assertEquals("curve", ellipse.elType)
        assertEquals(Const.OBJECT_TYPE_CONIC, ellipse.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, ellipse.elementClass)
        assertEquals("parameter", ellipse.curveType)
        assertEquals("ellipse", ellipse.id)
        assertEquals("point-ellipse", ellipse.name)
        assertFalse(ellipse.needsRegularUpdate)
        assertTrue(ellipse.isEllipse)
        assertTrue(ellipse.isDraggable)
        assertEquals(Curve.DEFAULT_SAMPLE_COUNT, ellipse.numberPoints)
        assertEquals(0.0, ellipse.minX(), absoluteTolerance = TOLERANCE)
        assertEquals(2.0 * PI, ellipse.maxX(), absoluteTolerance = TOLERANCE)
        assertEquals(
            11.661903789690601,
            ellipse.majorAxis(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            5.830951894845299,
            ellipse.X(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(0.0, ellipse.Y(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, center.X(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, center.Y(), absoluteTolerance = TOLERANCE)
        assertEquals(Const.OBJECT_TYPE_CAS, center.type)
        assertEquals("point", center.elType)
        assertFalse(center.isDraggable)
        assertEquals(emptyList(), center.parents)
        assertSame(center, ellipse.midpoint)
        assertSame(center, ellipse.subs["center"])
        assertEquals(listOf(focus1, focus2), ellipse.foci)
        assertSame(ellipsePoint, ellipse.pointOnEllipse)
        assertEquals(
            listOf<GeometryElement>(
                center,
                focus1,
                focus2,
                ellipsePoint,
            ),
            ellipse.inherits,
        )
        assertEquals(
            listOf("focus1", "focus2", "ellipsePoint"),
            ellipse.parents,
        )
        for (parent in listOf(center, focus1, focus2, ellipsePoint)) {
            assertSame(ellipse, parent.childElements[ellipse.id])
        }
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(24.999999999999993, 4.440892098500626e-16, 0.0),
                doubleArrayOf(
                    4.440892098500626e-16,
                    -0.7352941176470589,
                    -0.0,
                ),
                doubleArrayOf(0.0, -0.0, -1.0),
            ),
            actual = ellipse.quadraticform,
        )

        focus1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-4.0, 1.0),
        )
        focus2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, -1.0),
        )
        ellipsePoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 4.0),
        )
        board.fullUpdate()

        assertEquals(-1.0, center.X(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, center.Y(), absoluteTolerance = TOLERANCE)
        assertEquals(
            10.929971408438085,
            ellipse.majorAxis(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            4.184540661677429,
            ellipse.X(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            -1.7281802205591434,
            ellipse.Y(0.0),
            absoluteTolerance = TOLERANCE,
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(
                    19.16741406585186,
                    -0.6986546814666372,
                    -0.10044843951112092,
                ),
                doubleArrayOf(
                    -0.6986546814666372,
                    -0.6986546814666375,
                    -0.10044843951112087,
                ),
                doubleArrayOf(
                    -0.10044843951112092,
                    -0.10044843951112087,
                    -0.9665171868296264,
                ),
            ),
            actual = ellipse.quadraticform,
        )
    }

    @Test
    fun numericAndFunctionMajorAxesPreserveDomainsAndDynamicEvaluation() {
        val numericBoard = board("numeric")
        val numeric = ellipse(
            Ellipse.create(
                board = numericBoard,
                focus1 = point(numericBoard, -2.0, 1.0, "focus1"),
                focus2 = point(numericBoard, 4.0, 1.0, "focus2"),
                majorAxis = 10.0,
                minimum = -0.5 * PI,
                maximum = PI,
                sampleCount = 64,
                id = "numeric",
                name = "",
            ),
        )

        assertEquals(10.0, numeric.majorAxis())
        assertEquals(-0.5 * PI, numeric.minX())
        assertEquals(PI, numeric.maxX())
        assertEquals(64, numeric.numberPoints)
        assertEquals(
            -1.9999999999999998,
            numeric.X(-0.5 * PI),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            -2.2,
            numeric.Y(-0.5 * PI),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(listOf("focus1", "focus2"), numeric.parents)
        assertEquals(3, numeric.inherits.size)
        assertEquals(null, numeric.pointOnEllipse)

        val functionBoard = board("function")
        val majorAxis = MutableNumberFunction(10.0)
        val function = ellipse(
            Ellipse.create(
                board = functionBoard,
                focus1 = point(functionBoard, -3.0, 0.0, "focus1"),
                focus2 = point(functionBoard, 3.0, 0.0, "focus2"),
                majorAxisTerm = majorAxis,
                sampleCount = 32,
                id = "function",
                name = "",
            ),
        )
        assertEquals(10.0, function.majorAxis())
        assertEquals(5.0, function.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(16.0, function.quadraticform[0][0])

        majorAxis.value = 14.0
        functionBoard.fullUpdate()

        assertEquals(14.0, function.majorAxis())
        assertEquals(7.0, function.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(40.0, function.quadraticform[0][0])
    }

    @Test
    fun implicitPointsAndCenterFollowOfficialRemovalRelationships() {
        val outputRemovalBoard = board("output-removal")
        val outputPoints = listOf(
            point(outputRemovalBoard, -3.0, 0.0),
            point(outputRemovalBoard, 3.0, 0.0),
            point(outputRemovalBoard, 0.0, 5.0),
        )
        val output = ellipse(
            Ellipse.create(
                board = outputRemovalBoard,
                focus1 = outputPoints[0],
                focus2 = outputPoints[1],
                pointOnEllipse = outputPoints[2],
                parentlessPoints = outputPoints.toSet(),
                id = "ellipse",
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
            point(parentRemovalBoard, 0.0, 5.0),
        )
        val parentOutput = ellipse(
            Ellipse.create(
                board = parentRemovalBoard,
                focus1 = parentPoints[0],
                focus2 = parentPoints[1],
                pointOnEllipse = parentPoints[2],
                parentlessPoints = parentPoints.toSet(),
                id = "ellipse",
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
        val ellipsePoint = point(board, 0.0, 5.0, "ellipsePoint")
        val extra = point(board, 1.0, 1.0, "extra")

        assertEquals(
            EllipseError.ImplicitPointNotParent("extra"),
            assertIs<GMResult.Err<EllipseError>>(
                Ellipse.create(
                    board = board,
                    focus1 = focus1,
                    focus2 = focus2,
                    pointOnEllipse = ellipsePoint,
                    parentlessPoints = setOf(extra),
                ),
            ).error,
        )

        val otherBoard = board("other")
        val foreign = point(otherBoard, 0.0, 0.0, "foreign")
        assertEquals(
            EllipseError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<EllipseError>>(
                Ellipse.create(
                    board = board,
                    focus1 = focus1,
                    focus2 = foreign,
                    pointOnEllipse = ellipsePoint,
                ),
            ).error,
        )

        val duplicate = point(board, 7.0, 5.0, "duplicate")
        val beforeDuplicate = board.objects.keys.toSet()
        assertEquals(
            EllipseError.DuplicateElementId("duplicate"),
            assertIs<GMResult.Err<EllipseError>>(
                Ellipse.create(
                    board = board,
                    focus1 = focus1,
                    focus2 = focus2,
                    pointOnEllipse = ellipsePoint,
                    id = duplicate.id,
                ),
            ).error,
        )
        assertEquals(beforeDuplicate, board.objects.keys)

        val beforeInvalidSampleCount = board.objects.keys.toSet()
        val invalidSampleCount = assertIs<GMResult.Err<EllipseError>>(
            Ellipse.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                pointOnEllipse = ellipsePoint,
                sampleCount = 0,
            ),
        )
        assertEquals(
            CurveError.InvalidSampleCount(0, Curve.MAX_SAMPLE_COUNT),
            assertIs<EllipseError.CurveCreation>(
                invalidSampleCount.error,
            ).error,
        )
        assertEquals(beforeInvalidSampleCount, board.objects.keys)

        val beforeNonNumeric = board.objects.keys.toSet()
        val nonNumeric = assertIs<GMResult.Err<EllipseError>>(
            Ellipse.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                majorAxisTerm = NonNumericFunction,
            ),
        )
        assertEquals(
            CurveError.NonNumericExpression(
                term = "ellipse.majorAxis",
                actualType = "string",
            ),
            assertIs<EllipseError.CurveCreation>(nonNumeric.error).error,
        )
        assertEquals(beforeNonNumeric, board.objects.keys)
    }

    @Test
    fun degenerateMajorAxesPreserveJavaScriptDoubleArithmetic() {
        val shorter = numericEllipse(4.0)
        assertEquals(2.0, shorter.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(-5.0, shorter.quadraticform[0][0])
        assertEquals(1.25, shorter.quadraticform[1][1])

        val zero = numericEllipse(0.0)
        assertEquals(0.0, zero.X(0.0))
        assertTrue(zero.quadraticform[0].all(Double::isNaN))
        assertTrue(zero.quadraticform[1][1].isInfinite())

        val negative = numericEllipse(-10.0)
        assertEquals(-5.0, negative.X(0.0), absoluteTolerance = TOLERANCE)
        assertEquals(16.0, negative.quadraticform[0][0])

        val nan = numericEllipse(Double.NaN)
        assertTrue(nan.X(0.0).isNaN())
        assertTrue(nan.quadraticform.all { row -> row.all(Double::isNaN) })

        val infinity = numericEllipse(Double.POSITIVE_INFINITY)
        assertTrue(infinity.X(0.0).isNaN())
        assertTrue(infinity.quadraticform[0].all(Double::isNaN))
        assertEquals(-1.0, infinity.quadraticform[1][1])
        assertEquals(-1.0, infinity.quadraticform[2][2])
    }

    @Test
    fun unverifiedConicInteropRemainsStructuredAndAtomic() {
        val board = board("conic-boundaries")
        val focus1 = point(board, -3.0, 0.0, "focus1")
        val focus2 = point(board, 3.0, 0.0, "focus2")
        val ellipsePoint = point(board, 0.0, 5.0, "ellipsePoint")
        val ellipse = ellipse(
            Ellipse.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                pointOnEllipse = ellipsePoint,
                id = "ellipse",
                name = "",
            ),
        )
        val externalPoint = point(board, 7.0, 4.0, "externalPoint")
        val line = line(
            board = board,
            first = point(board, -8.0, 4.0, "lineStart"),
            second = point(board, 8.0, 4.0, "lineEnd"),
            id = "line",
        )
        val objectIds = board.objects.keys.toSet()

        assertEquals(
            TangentError.UnsupportedConic(0),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(board, ellipse, externalPoint),
            ).error,
        )
        assertEquals(
            TangentError.UnsupportedConic(1),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.createPolarLine(board, externalPoint, ellipse),
            ).error,
        )
        assertEquals(
            NormalError.UnsupportedConic(1),
            assertIs<GMResult.Err<NormalError>>(
                Normal.create(board, externalPoint, ellipse),
            ).error,
        )
        assertEquals(
            IntersectionError.UnsupportedConic(1),
            assertIs<GMResult.Err<IntersectionError>>(
                IntersectionPoint.create(board, line, ellipse),
            ).error,
        )
        assertEquals(
            IntersectionError.UnsupportedConic(0),
            assertIs<GMResult.Err<IntersectionError>>(
                OtherIntersectionPoint.create(
                    board = board,
                    first = ellipse,
                    second = line,
                    excludedPoints = listOf(externalPoint),
                ),
            ).error,
        )
        assertEquals(
            PolePointError.UnsupportedParents(listOf("curve", "line")),
            assertIs<GMResult.Err<PolePointError>>(
                PolePoint.create(board, ellipse, line),
            ).error,
        )
        assertEquals(
            TangentToError.UnsupportedConic("curve"),
            assertIs<GMResult.Err<TangentToError>>(
                TangentTo.create(board, ellipse, externalPoint),
            ).error,
        )
        assertEquals(objectIds, board.objects.keys.toSet())
    }

    private fun numericEllipse(majorAxis: Double): Curve {
        val board = board("degenerate-$majorAxis")
        return ellipse(
            Ellipse.create(
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

    private fun line(
        board: Board,
        first: Point,
        second: Point,
        id: String,
    ): Line = assertIs<GMResult.Ok<Line>>(
        Line.create(
            board = board,
            point1 = first,
            point2 = second,
            id = id,
            name = "",
        ),
    ).value

    private fun ellipse(
        result: GMResult<Curve, EllipseError>,
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
