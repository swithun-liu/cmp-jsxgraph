/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphDocumentError
import com.swithun.jsxgraph.core.JsxGraphEngine
import com.swithun.jsxgraph.core.base.Board
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class MsectorCompatibilityTest {
    @Test
    fun msectorRemainsUnavailableLikeUpstreamWithoutMutatingTheBoard() {
        assertFalse("msector" in NativeJessieCodeCreators.names)
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "msector",
        )
        evaluate(
            source =
                """
                A = point(3, 2);
                B = point(0, 0);
                C = point(-1, 3);
                """.trimIndent(),
            board = board,
        )
        val objectIds = board.objects.keys.toList()

        val error = evaluateError(
            source = "msector(A, B, C, 0.2);",
            board = board,
        )

        val notCallable = assertIs<JessieCodeRuntimeError.NotCallable>(error)
        assertEquals("undefined", notCallable.valueType)
        assertEquals(objectIds, board.objects.keys.toList())
    }

    @Test
    fun constructionDocumentReportsMsectorAsUnsupported() {
        val result = JsxGraphEngine.parse(
            """
            {
              "schemaVersion": 1,
              "boundingBox": [-5, 5, 5, -5],
              "axis": false,
              "grid": false,
              "keepAspectRatio": true,
              "objects": [
                {
                  "id": "msector",
                  "type": "msector",
                  "parents": [[3, 2], [0, 0], [-1, 3], 0.2]
                }
              ]
            }
            """.trimIndent(),
        )

        val error = assertIs<GMResult.Err<JsxGraphDocumentError>>(result).error
        val unsupported =
            assertIs<JsxGraphDocumentError.UnsupportedElementType>(error)
        assertEquals(0, unsupported.objectIndex)
        assertEquals("msector", unsupported.id)
        assertEquals("msector", unsupported.type)
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

    private fun evaluateError(
        source: String,
        board: Board,
    ): JessieCodeRuntimeError {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Err<JessieCodeRuntimeError>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(board = board),
            ),
        ).error
    }
}
