/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardError
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SketchCurveCreatorTest {
    @Test
    fun creatorMatchesOfficialEmptyCurveAndIgnoresParents() {
        val board = board()
        val curve = curve(
            evaluate(
                source =
                    """
                    sketchcurve(1, "ignored", [2, 3]) <<
                        id: "sketch",
                        name: "freehand"
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )

        assertTrue("sketchcurve" in NativeJessieCodeCreators.names)
        assertEquals("sketchcurve", curve.elType)
        assertEquals("plot", curve.curveType)
        assertEquals(0, curve.numberPoints)
        assertTrue(curve.dataX?.isEmpty() == true)
        assertTrue(curve.dataY?.isEmpty() == true)
        assertTrue(curve.parents.isEmpty())
        assertEquals("freehand", curve.name)
        assertSame(curve, board.elementById("sketch"))

        board.update()

        assertEquals(0, curve.numberPoints)
        assertTrue(curve.points.isEmpty())
    }

    @Test
    fun duplicateIdFailureIsStructuredAndAtomic() {
        val board = board("duplicate")
        evaluate(
            source = "point(0, 0) << id: \"taken\", name: \"\" >>;",
            board = board,
        )

        val error = creatorError(
            source =
                "sketchcurve() << id: \"taken\", name: \"\" >>;",
            board = board,
        )
        val factory = assertIs<JessieCodeCreatorError.CurveFactory>(
            error.error,
        )
        assertEquals(
            CurveError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            factory.error,
        )
        assertEquals(1, board.objects.size)
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

    private fun board(id: String = "board"): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = id,
    )
}
