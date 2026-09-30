/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeConstantCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MetaPostSplineTest {
    @Test
    fun dynamicPointsAndTensionMatchOfficialControlPoints() {
        val board = board("metapost-dynamic")
        val points = listOf(
            point(board, -3.0, -3.0, "P0"),
            point(board, 0.0, -3.0, "P1"),
            point(board, 4.0, -5.0, "P2"),
            point(board, 6.0, -2.0, "P3"),
        )
        val tension = MutableRuntimeValue(
            JessieCodeRuntimeValue.NumberValue(1.0),
        )
        val closed = MutableRuntimeValue(
            JessieCodeRuntimeValue.BooleanValue(false),
        )
        val spline = curve(
            Curve.createMetaPostSpline(
                board = board,
                points = points,
                controls = CurveMetaPostControlsDefinition(
                    tensionTerm = tension,
                    isClosedTerm = closed,
                    pointControls = emptyList(),
                ),
                id = "metapost",
                name = "",
            ),
        )

        assertTrue(spline.isMetaPostSpline)
        assertEquals("metapostspline", spline.elType)
        assertEquals("plot", spline.curveType)
        assertEquals(3, spline.bezierDegree)
        assertEquals(points.map(Point::id), spline.parents)
        assertTrue(
            points.all { it.childElements[spline.id] === spline },
        )
        assertCoordinates(
            expectedX = doubleArrayOf(
                -3.0,
                -1.9965915203212292,
                -0.9512851323226681,
                0.0,
                1.4302952533432818,
                2.4166437688022535,
                4.0,
                5.264443313090963,
                5.7297941775084,
                6.0,
            ),
            expectedY = doubleArrayOf(
                -3.0,
                -2.8079385084787885,
                -2.6219811391998196,
                -3.0,
                -3.5683664801494444,
                -5.339667473546103,
                -5.0,
                -4.7287469129578135,
                -3.317624516847668,
                -2.0,
            ),
            curve = spline,
        )
        assertEquals(
            -1.4804537447414616,
            spline.X(0.5),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            -2.786219867879478,
            spline.Y(0.5),
            absoluteTolerance = TOLERANCE,
        )

        tension.value = JessieCodeRuntimeValue.NumberValue(2.0)
        points[2].setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(3.0, -1.0),
        )
        board.update()

        assertCoordinates(
            expectedX = doubleArrayOf(
                -3.0,
                -2.495729466545832,
                -0.48273861890393127,
                0.0,
                0.585865098233011,
                2.377846826855823,
                3.0,
                3.538720538932548,
                5.506013303608986,
                6.0,
            ),
            expectedY = doubleArrayOf(
                -3.0,
                -3.0317842126167935,
                -3.157158109645138,
                -3.0,
                -2.809268519774908,
                -1.086838690549428,
                -1.0,
                -0.9248066421705069,
                -1.7830407065386853,
                -2.0,
            ),
            curve = spline,
        )
    }

    @Test
    fun closedAndPointControlledCurvesUseCubicBezierData() {
        val closedBoard = board("metapost-closed")
        val closedPoints = listOf(
            point(closedBoard, -2.0, -1.0, "C0"),
            point(closedBoard, 2.0, -1.0, "C1"),
            point(closedBoard, 1.0, 3.0, "C2"),
        )
        val closed = curve(
            Curve.createMetaPostSpline(
                board = closedBoard,
                points = closedPoints,
                controls = controls(isClosed = true),
                id = "closed",
                name = "",
            ),
        )

        assertEquals(10, closed.numberPoints)
        assertEquals(-2.0, closed.X(3.0))
        assertEquals(-1.0, closed.Y(3.0))
        assertContentEquals(
            doubleArrayOf(
                -2.0,
                -1.0480160634066966,
                0.9419488633326374,
                2.0,
                3.111673617913538,
                2.6138138588268784,
                1.0,
                -1.4124468954863048,
                -3.4925527107164616,
                -2.0,
            ),
            closed.dataX,
        )

        val controlledBoard = board("metapost-controlled")
        val controlledPoints = listOf(
            point(controlledBoard, -3.0, -3.0, "K0"),
            point(controlledBoard, 0.0, -3.0, "K1"),
            point(controlledBoard, 4.0, -5.0, "K2"),
            point(controlledBoard, 6.0, -2.0, "K3"),
        )
        val controlled = curve(
            Curve.createMetaPostSpline(
                board = controlledBoard,
                points = controlledPoints,
                controls = CurveMetaPostControlsDefinition(
                    tensionTerm = number(1.0),
                    isClosedTerm = boolean(false),
                    pointControls = listOf(
                        CurveMetaPostPointControlDefinition(
                            index = 1,
                            type = null,
                            curlTerm = null,
                            directionTerm = constantArray(
                                JessieCodeRuntimeValue.NumberValue(-30.0),
                                JessieCodeRuntimeValue.NumberValue(45.0),
                            ),
                            tensionTerm = constantArray(
                                JessieCodeRuntimeValue.NumberValue(2.0),
                                JessieCodeRuntimeValue.NumberValue(3.0),
                            ),
                        ),
                    ),
                ),
                id = "controlled",
                name = "",
            ),
        )

        assertCoordinates(
            expectedX = doubleArrayOf(
                -3.0,
                -1.974227776419943,
                -0.45072163263964493,
                0.0,
                0.5555440198211982,
                2.583730407853098,
                4.0,
                5.314840653841305,
                5.832552391134155,
                6.0,
            ),
            expectedY = doubleArrayOf(
                -3.0,
                -2.8921869948838457,
                -2.7397757440659136,
                -3.0,
                -2.4444559801788017,
                -5.055013717722158,
                -5.0,
                -4.948926198104421,
                -3.436808042380946,
                -2.0,
            ),
            curve = controlled,
        )
    }

    @Test
    fun invalidDefinitionsReturnStructuredErrorsWithoutRegistration() {
        val board = board("metapost-errors")
        val point = point(board, 0.0, 0.0, "P0")
        assertEquals(
            CurveError.InvalidInterpolationPointCount(
                creator = "metapostspline",
                count = 1,
                minimum = 2,
            ),
            assertIs<GMResult.Err<CurveError>>(
                Curve.createMetaPostSpline(
                    board = board,
                    points = listOf(point),
                    controls = controls(),
                    id = "too-short",
                ),
            ).error,
        )
        assertEquals(null, board.select("too-short"))

        val second = point(board, 1.0, 1.0, "P1")
        val invalidPair = assertIs<GMResult.Err<CurveError>>(
            Curve.createMetaPostSpline(
                board = board,
                points = listOf(point, second),
                controls = CurveMetaPostControlsDefinition(
                    tensionTerm = number(1.0),
                    isClosedTerm = boolean(false),
                    pointControls = listOf(
                        CurveMetaPostPointControlDefinition(
                            index = 0,
                            type = null,
                            curlTerm = null,
                            directionTerm = constantArray(
                                JessieCodeRuntimeValue.NumberValue(20.0),
                            ),
                            tensionTerm = null,
                        ),
                    ),
                ),
                id = "invalid-control",
            ),
        ).error
        assertEquals(
            CurveError.InvalidMetaPostControl(
                control = "metapostspline.controls.0.direction",
                expected = "a number or a two-entry array",
                actualType = "array",
            ),
            invalidPair,
        )
        assertEquals(null, board.select("invalid-control"))
        assertSame(point, board.select("P0"))
        assertSame(second, board.select("P1"))
    }

    private fun controls(
        tension: Double = 1.0,
        isClosed: Boolean = false,
    ): CurveMetaPostControlsDefinition =
        CurveMetaPostControlsDefinition(
            tensionTerm = number(tension),
            isClosedTerm = boolean(isClosed),
            pointControls = emptyList(),
        )

    private fun number(value: Double): JessieCodeCoordinateFunction =
        JessieCodeConstantCoordinateFunction(
            JessieCodeRuntimeValue.NumberValue(value),
        )

    private fun boolean(value: Boolean): JessieCodeCoordinateFunction =
        JessieCodeConstantCoordinateFunction(
            JessieCodeRuntimeValue.BooleanValue(value),
        )

    private fun constantArray(
        vararg values: JessieCodeRuntimeValue,
    ): JessieCodeCoordinateFunction =
        JessieCodeConstantCoordinateFunction(
            JessieCodeRuntimeValue.ArrayValue(values.toList()),
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
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
            ),
        ).value

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun assertCoordinates(
        expectedX: DoubleArray,
        expectedY: DoubleArray,
        curve: Curve,
    ) {
        assertEquals(expectedX.size, curve.numberPoints)
        assertEquals(expectedX.size, expectedY.size)
        val actualX = assertNotNull(curve.dataX)
        val actualY = assertNotNull(curve.dataY)
        for (index in expectedX.indices) {
            assertEquals(
                expectedX[index],
                actualX[index],
                absoluteTolerance = TOLERANCE,
                message = "x[$index]",
            )
            assertEquals(
                expectedY[index],
                actualY[index],
                absoluteTolerance = TOLERANCE,
                message = "y[$index]",
            )
        }
    }

    private class MutableRuntimeValue(
        var value: JessieCodeRuntimeValue,
    ) : JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> = emptyMap()

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(value)
    }

    private companion object {
        const val TOLERANCE = 1.0e-14
    }
}
