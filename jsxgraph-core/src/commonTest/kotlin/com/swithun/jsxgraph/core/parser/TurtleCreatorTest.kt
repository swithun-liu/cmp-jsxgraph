/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.GliderError
import com.swithun.jsxgraph.core.base.Turtle
import com.swithun.jsxgraph.core.base.TurtleError
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TurtleCreatorTest {
    @Test
    fun creatorSupportsOfficialParentFormsAndPenAttributes() {
        val cases = listOf(
            "turtle()" to doubleArrayOf(0.0, 0.0, 90.0),
            "turtle(1, 2)" to doubleArrayOf(1.0, 2.0, 90.0),
            "turtle(1, 2, 30)" to doubleArrayOf(1.0, 2.0, 30.0),
            "turtle([1, 2])" to doubleArrayOf(1.0, 2.0, 90.0),
            "turtle([1, 2], 30)" to doubleArrayOf(1.0, 2.0, 30.0),
        )

        assertTrue("turtle" in NativeJessieCodeCreators.names)
        for ((index, case) in cases.withIndex()) {
            val board = board("parents-$index")
            val turtle = turtle(
                evaluate(
                    source = case.first +
                        " << id: \"t\", name: \"\", strokeWidth: 3, " +
                        "strokeColor: \"#123456\", " +
                        "highlightStrokeColor: \"#abcdef\", " +
                        "arrow: << visible: false >> >>;",
                    board = board,
                ),
            )

            assertContentEquals(
                case.second.copyOfRange(0, 2),
                turtle.position,
            )
            assertEquals(case.second[2], turtle.direction)
            assertEquals(3.0, turtle.getPenSize())
            assertEquals("#123456", turtle.getPenColor())
            assertEquals("#abcdef", turtle.getHighlightPenColor())
            assertFalse(turtle.arrowVisible)
            assertNull(board.elementById("t"))
        }
    }

    @Test
    fun methodMapAliasesMutateTheOfficialTurtleState() {
        val turtle = turtle(
            evaluate(
                source =
                    """
                    t = turtle([1, 2], 30) << id: "t", name: "" >>;
                    t.fd(4);
                    t.lt(60);
                    t.forward(2);
                    t.pu();
                    t.moveTo([-2, 3]);
                    t.pd();
                    t.penSize(5);
                    t.penColor("#654321");
                    t.highlightPenColor("#fedcba");
                    t.lookTo([0, 0]);
                    t.forward(3);
                    t.push();
                    t.rt(45);
                    t.fd(1);
                    t;
                    """.trimIndent(),
                board = board(),
            ),
        )

        assertContentEquals(
            doubleArrayOf(-0.532015546462498, -0.47673155870445183),
            turtle.position,
        )
        assertEquals(
            -101.30993247402023,
            turtle.direction,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(5.0, turtle.getPenSize())
        assertEquals("#654321", turtle.getPenColor())
        assertEquals("#fedcba", turtle.getHighlightPenColor())
        assertEquals(9.0, turtle.maxX())
    }

    @Test
    fun runtimeFailuresRemainStructured() {
        val pop = runtimeError(
            "t = turtle() << id: \"t\", name: \"\" >>; t.pop();",
            board(),
        )
        val unavailable =
            assertIs<JessieCodeRuntimeError.ElementMethodUnavailable>(pop)
        assertEquals("popTurtle", unavailable.method)
        assertEquals(TurtleError.EmptyStack.toString(), unavailable.reason)

        val invalid = runtimeError(
            "t = turtle() << id: \"t\", name: \"\" >>; t.setPos([1]);",
            board(),
        )
        assertIs<JessieCodeRuntimeError.InvalidArgumentCount>(invalid)
    }

    @Test
    fun turtleGliderMatchesOfficialUnavailablePublicPathWithoutLeakingPoint() {
        val board = board()
        val error = runtimeError(
            source =
                """
                t = turtle([0, 0], 0) << id: "t", name: "" >>;
                t.forward(4);
                glider(1, 2, t) << id: "glider", name: "" >>;
                """.trimIndent(),
            board = board,
        )

        val creator =
            assertIs<JessieCodeRuntimeError.CreatorFailure>(error)
        assertEquals("glider", creator.creatorName)
        assertEquals(
            JessieCodeCreatorError.GliderFactory(
                GliderError.UnsupportedSlideObject("turtle"),
            ),
            creator.error,
        )
        assertNull(board.elementById("t"))
        assertNull(board.elementById("glider"))
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

    private fun runtimeError(
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

    private fun turtle(value: JessieCodeRuntimeValue): Turtle =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun board(id: String = "board"): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = id,
    )
}
