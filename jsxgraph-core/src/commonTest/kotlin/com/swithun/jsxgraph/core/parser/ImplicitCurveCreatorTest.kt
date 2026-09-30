/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveDataUpdateError
import com.swithun.jsxgraph.core.base.CurveError
import com.swithun.jsxgraph.core.base.CurveImplicitUpdateError
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.math.ImplicitPlotError
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImplicitCurveCreatorTest {
    @Test
    fun stringFunctionAndExplicitDomainUseNumericalDerivatives() {
        val board = board()
        val curve = curve(
            evaluate(
                source =
                    """
                    c = implicitcurve(
                        "x * x + y * y - 1",
                        [-2, 2],
                        [-2, 2]
                    ) <<
                        id: "circle",
                        name: "",
                        maxSteps: 64,
                        hInitial: 0.1,
                        hMax: 0.5
                    >>;
                    c;
                    """.trimIndent(),
                board = board,
            ),
        )

        assertTrue("implicitcurve" in NativeJessieCodeCreators.names)
        assertEquals("implicitcurve", curve.elType)
        assertEquals("plot", curve.curveType)
        assertEquals(129, curve.numberPoints)
        assertEquals(129, curve.dataX?.size)
        assertEquals(129, curve.dataY?.size)
        val firstX = curve.dataX?.first() ?: Double.NaN
        val firstY = curve.dataY?.first() ?: Double.NaN
        assertEquals(
            1.0,
            firstX * firstX + firstY * firstY,
            absoluteTolerance = 1.0e-7,
        )
    }

    @Test
    fun functionParentsSupportExplicitDerivativesAndDomain() {
        val curve = curve(
            evaluate(
                source =
                    """
                    implicitcurve(
                        function (x, y) {
                            return x * x + y * y - 1;
                        },
                        function (x, y) { return 2 * x; },
                        function (x, y) { return 2 * y; },
                        [-2, 2],
                        [-2, 2]
                    ) <<
                        id: "circle",
                        name: "",
                        maxSteps: function () { return 64; },
                        loopDetection: function () { return true; }
                    >>;
                    """.trimIndent(),
                board = board(),
            ),
        )

        assertEquals("implicitcurve", curve.elType)
        assertEquals(129, curve.numberPoints)
        assertEquals(
            0.9941520442251717,
            curve.dataX?.first() ?: Double.NaN,
            absoluteTolerance = 1.0e-10,
        )
    }

    @Test
    fun expressionDependenciesDriveRegularUpdates() {
        val board = board("dynamic")
        val curve = curve(
            evaluate(
                source =
                    """
                    Driver = point(1, 0) << id: "driver", name: "" >>;
                    c = implicitcurve(
                        "x * x + y * y - driver.X()",
                        [-3, 3],
                        [-3, 3]
                    ) <<
                        id: "dynamic-circle",
                        name: "",
                        maxSteps: 64
                    >>;
                    c;
                    """.trimIndent(),
                board = board,
            ),
        )
        val driver = assertIs<Point>(board.select("driver"))
        assertSame(curve, driver.childElements[curve.id])
        val initialMaximum = (curve.dataX ?: doubleArrayOf())
            .filter(Double::isFinite)
            .maxOf { abs(it) }

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, 0.0),
        )
        board.update()

        val updatedMaximum = (curve.dataX ?: doubleArrayOf())
            .filter(Double::isFinite)
            .maxOf { abs(it) }
        assertTrue(initialMaximum < 1.1)
        assertTrue(updatedMaximum > 1.9)
    }

    @Test
    fun pointLimitFailureIsStructuredAndDoesNotRegisterCurve() {
        val board = board(
            id = "limited",
            maximumPointCount = 10,
        )
        val error = creatorError(
            source =
                """
                implicitcurve(
                    "x * x + y * y - 1",
                    [-2, 2],
                    [-2, 2]
                ) << id: "too-many", name: "", maxSteps: 64 >>;
                """.trimIndent(),
            board = board,
        )

        val curveError = assertIs<JessieCodeCreatorError.CurveFactory>(
            error.error,
        ).error
        val updateError = assertIs<CurveError.DataUpdate>(curveError).error
        val implicitError =
            assertIs<CurveDataUpdateError.ImplicitCurve>(updateError).error
        val plotError =
            assertIs<CurveImplicitUpdateError.Plot>(implicitError).error
        assertIs<ImplicitPlotError.PointLimitExceeded>(plotError)
        assertNull(board.elementById("too-many"))
    }

    @Test
    fun invalidParentShapeIsRejectedBeforeRegistration() {
        val board = board("invalid")
        val error = creatorError(
            source =
                """
                implicitcurve(
                    "x * x + y * y - 1",
                    [0],
                    [-2, 2]
                ) << id: "invalid", name: "" >>;
                """.trimIndent(),
            board = board,
        )

        assertEquals("implicitcurve", error.creatorName)
        assertIs<JessieCodeCreatorError.InvalidAttributeType>(
            error.error,
        )
        assertNull(board.elementById("invalid"))
    }

    @Test
    fun nonNumericFunctionResultIsStructuredAndAtomic() {
        val board = board("non-numeric")
        val error = creatorError(
            source =
                """
                implicitcurve(
                    function (x, y) { return "bad"; },
                    [-2, 2],
                    [-2, 2]
                ) <<
                    id: "non-numeric",
                    name: "",
                    resolutionOuter: 0.1
                >>;
                """.trimIndent(),
            board = board,
        )

        val curveError = assertIs<JessieCodeCreatorError.CurveFactory>(
            error.error,
        ).error
        val updateError = assertIs<CurveError.DataUpdate>(curveError).error
        val implicitError =
            assertIs<CurveDataUpdateError.ImplicitCurve>(updateError).error
        assertIs<CurveImplicitUpdateError.InvalidValue>(implicitError)
        assertNull(board.elementById("non-numeric"))
    }

    private fun evaluate(
        source: String,
        board: Board,
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(board = board),
            ),
        ).value
    }

    private fun creatorError(
        source: String,
        board: Board,
    ): JessieCodeRuntimeError.CreatorFailure {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        val error = assertIs<GMResult.Err<JessieCodeRuntimeError>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(board = board),
            ),
        ).error
        return assertIs(error)
    }

    private fun curve(value: JessieCodeRuntimeValue): Curve =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun board(
        id: String = "board",
        maximumPointCount: Int = 10_000,
    ): Board = Board(
        originX = 20.0,
        originY = 20.0,
        unitX = 10.0,
        unitY = 10.0,
        boundingBox = doubleArrayOf(-2.0, 2.0, 2.0, -2.0),
        maxCurvePoints = maximumPointCount,
        id = id,
    )
}
