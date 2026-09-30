/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.ForeignObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForeignObjectCreatorTest {
    @Test
    fun aliasesCreateExplicitAndIntrinsicForeignObjects() {
        val board = board()
        evaluate(
            source =
                """
                foreignobject("<b>full</b>", [-3, -2], [3, 2]) <<
                    id: "full",
                    name: "",
                    evaluateOnlyOnce: true
                >>;
                fo("<i>intrinsic</i>", [1, 2]) <<
                    id: "alias",
                    name: ""
                >>;
                """.trimIndent(),
            board = board,
        )
        val full = assertIs<ForeignObject>(board.elementById("full"))
        val alias = assertIs<ForeignObject>(board.elementById("alias"))

        assertTrue("foreignobject" in NativeJessieCodeCreators.names)
        assertTrue("fo" in NativeJessieCodeCreators.names)
        assertEquals("<b>full</b>", full.content)
        assertEquals(true, full.evaluateOnlyOnce)
        assertEquals(false, full.needsRegularUpdate)
        assertContentEquals(doubleArrayOf(3.0, 2.0), full.usrSize)
        assertEquals("<i>intrinsic</i>", alias.content)
        assertEquals(false, alias.usesUserSize)
        assertNull(alias.usrSize)
    }

    @Test
    fun dynamicCoordinatesSizesAndSetSizeFollowBoardUpdates() {
        val board = board()
        evaluate(
            source =
                """
                driver = point(-2, -1) <<
                    id: "driver", name: "", withLabel: false
                >>;
                overlay = fo(
                    "<span>dynamic</span>",
                    [
                        function () { return driver.X(); },
                        function () { return driver.Y(); }
                    ],
                    [
                        function () { return driver.X() + 4; },
                        function () { return driver.Y() + 3; }
                    ]
                ) <<
                    id: "overlay",
                    name: "",
                    needsRegularUpdate: true
                >>;
                """.trimIndent(),
            board = board,
        )
        val overlay = assertIs<ForeignObject>(board.elementById("overlay"))
        assertEquals(-2.0, overlay.X())
        assertEquals(-1.0, overlay.Y())
        assertEquals(2.0, overlay.W())
        assertEquals(2.0, overlay.H())

        evaluate(
            source =
                """
                driver.moveTo([1, 2]);
                overlay.setSize(4, 1.5);
                """.trimIndent(),
            board = board,
        )

        assertEquals(1.0, overlay.X())
        assertEquals(2.0, overlay.Y())
        assertEquals(4.0, overlay.W())
        assertEquals(1.5, overlay.H())
    }

    @Test
    fun invalidContentAndSizeReturnStructuredFailuresWithoutRegistration() {
        for (
            source in listOf(
                """
                foreignobject(
                    function () { return "<b>not evaluated</b>"; },
                    [0, 0],
                    [1, 1]
                ) << id: "bad", name: "" >>;
                """.trimIndent(),
                """
                fo("<b>bad size</b>", [0, 0], [1]) <<
                    id: "bad", name: ""
                >>;
                """.trimIndent(),
            )
        ) {
            val board = board()
            val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
                JessieCodeExpressionParser().parse(source),
            ).value
            val error = assertIs<GMResult.Err<JessieCodeRuntimeError>>(
                JessieCodeEvaluator().evaluate(
                    ast,
                    JessieCodeRuntimeEnvironment(board = board),
                ),
            ).error

            assertIs<JessieCodeRuntimeError.CreatorFailure>(error)
            assertTrue(board.objects.isEmpty())
        }
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

    private fun board(): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = "board",
    )
}
