/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GeogebraExpressionTest {
    @Test
    fun scalarFunctionsImplicitMultiplicationAndDefinitionsEvaluate() {
        val compiled = compile("f(x) = 2x + sin(PI / 2)")
        val result = assertIs<GMResult.Ok<GeogebraExpressionValue.Scalar>>(
            compiled.evaluate(
                board = board(),
                values = emptyMap(),
                variables = mapOf("x" to 3.0),
            ),
        ).value

        assertEquals(7.0, result.value, TOLERANCE)
        assertEquals(
            GeogebraFunctionDefinition(
                parameters = listOf("x", "y"),
                expression = "x + y",
            ),
            CompiledGeogebraExpression.functionDefinition(
                "sum(x, y) = x + y",
            ),
        )
    }

    @Test
    fun multilineFunctionDefinitionPreservesExpression() {
        assertEquals(
            GeogebraFunctionDefinition(
                parameters = listOf("x"),
                expression = "x +\n  1",
            ),
            CompiledGeogebraExpression.functionDefinition(
                "f(x) = x +\n  1",
            ),
        )
    }

    @Test
    fun pointCoordinatesAndCoordinateArithmeticEvaluate() {
        val board = board()
        val point = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 3.0),
                name = "A",
            ),
        ).value
        val values = mapOf(
            "A" to GeogebraReaderValue.Element(point),
        )

        val coordinates = assertIs<
            GMResult.Ok<GeogebraExpressionValue.Coordinates>
            >(
            compile("2 A + (1, -1)").evaluate(board, values),
        ).value
        assertEquals(5.0, coordinates.x, TOLERANCE)
        assertEquals(5.0, coordinates.y, TOLERANCE)
        assertEquals(false, coordinates.vector)

        val scalar = assertIs<
            GMResult.Ok<GeogebraExpressionValue.Scalar>
            >(
            compile("x(A) + y(A)").evaluate(board, values),
        ).value
        assertEquals(5.0, scalar.value, TOLERANCE)
    }

    @Test
    fun invalidInputsReturnStructuredFailures() {
        assertIs<
            GMResult.Err<GeogebraExpressionError.InvalidLimits>
            >(
            CompiledGeogebraExpression.compile(
                source = "1",
                limits = GeogebraExpressionLimits(maxTokens = -1),
            ),
        )
        val missing = compile("missing + 1")
        assertEquals(
            GeogebraExpressionError.MissingReference("missing"),
            assertIs<
                GMResult.Err<GeogebraExpressionError.MissingReference>
                >(
                missing.evaluate(board(), emptyMap()),
            ).error,
        )
        assertIs<
            GMResult.Err<GeogebraExpressionError.UnexpectedToken>
            >(
            CompiledGeogebraExpression.compile("(1 + 2"),
        )
    }

    private fun compile(source: String): CompiledGeogebraExpression =
        assertIs<GMResult.Ok<CompiledGeogebraExpression>>(
            CompiledGeogebraExpression.compile(source),
        ).value

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        canvasWidth = 500.0,
        canvasHeight = 500.0,
    )

    private companion object {
        const val TOLERANCE = 1.0e-10
    }
}
