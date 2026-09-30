/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Group
import com.swithun.jsxgraph.core.base.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GroupCreatorTest {
    @Test
    fun creatorRegistersNonRenderedGroupAndFiltersFixedMembers() {
        val board = board()
        val value = evaluate(
            source =
                """
                A = point(0, 0) << id: "A", name: "" >>;
                B = point(2, 0) << id: "B", name: "" >>;
                F = point(4, 4) << id: "F", name: "", fixed: true >>;
                group(A, B, F) <<
                    id: "group",
                    name: "named",
                    needsRegularUpdate: false
                >>;
                """.trimIndent(),
            board = board,
        )
        val group = assertIs<Group>(
            assertIs<JessieCodeRuntimeValue.CompositionReference>(
                value,
            ).composition,
        )

        assertTrue("group" in NativeJessieCodeCreators.names)
        assertSame(group, board.groupById("group"))
        assertEquals("named", group.name)
        assertEquals(listOf("A", "B", "F"), group.getParents())
        assertEquals(listOf("A", "B"), group.groupObjects.keys.toList())
        assertEquals(3, board.objects.size)
        assertEquals(1, board.groups.size)
    }

    @Test
    fun groupMethodsDriveRotationScalingAndMembership() {
        val board = board()
        evaluate(
            source =
                """
                A = point(0, 0) << id: "A", name: "" >>;
                B = point(2, 0) << id: "B", name: "" >>;
                C = point(0, 2) << id: "C", name: "" >>;
                g = group(A, B, C) << id: "group", name: "" >>;
                g.setRotationCenter(A).setRotationPoints([B]);
                B.moveTo([0, 2]);
                """.trimIndent(),
            board = board,
        )

        assertPoint(board, "A", 0.0, 0.0)
        assertPoint(board, "B", 0.0, 2.0)
        assertPoint(board, "C", -2.0, 0.0)

        val parentValue = evaluate(
            source = "group.getParents();",
            board = board,
        )
        val parentIds =
            assertIs<JessieCodeRuntimeValue.ArrayValue>(parentValue)
                .values.map {
                    assertIs<JessieCodeRuntimeValue.StringValue>(it).value
                }
        assertEquals(listOf("A", "B", "C"), parentIds)

        evaluate(
            source =
                """
                group.removePoint(C);
                group.addPoint(C);
                group.ungroup();
                """.trimIndent(),
            board = board,
        )
        assertTrue(board.groupById("group")?.groupObjects?.isEmpty() == true)
    }

    @Test
    fun invalidParentFailureIsStructuredAndDoesNotRegisterGroup() {
        val board = board()
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(
                "group(1) << id: \"group\", name: \"\" >>;",
            ),
        ).value
        val error = assertIs<GMResult.Err<JessieCodeRuntimeError>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(board = board),
            ),
        ).error
        val creator = assertIs<JessieCodeRuntimeError.CreatorFailure>(error)

        assertIs<JessieCodeCreatorError.UnsupportedParents>(creator.error)
        assertTrue(board.groups.isEmpty())
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

    private fun assertPoint(
        board: Board,
        id: String,
        x: Double,
        y: Double,
    ) {
        val point = assertIs<Point>(board.elementById(id))
        assertEquals(x, point.X(), 1.0e-12)
        assertEquals(y, point.Y(), 1.0e-12)
    }

    private fun board(): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = "board",
    )
}
