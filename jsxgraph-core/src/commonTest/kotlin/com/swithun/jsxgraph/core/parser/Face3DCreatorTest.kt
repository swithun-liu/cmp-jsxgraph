/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardError
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Face3D
import com.swithun.jsxgraph.core.base.Face3DError
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polyhedron3D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Face3DCreatorTest {
    @Test
    fun creatorReusesPolyhedronDefinitionAndBuildsOfficialProxy() {
        val board = board()
        val face = face(
            evaluate(
                source = setupSource() +
                    """
                    selected = face3d(view, solid, 0) <<
                        id: "selected",
                        name: "",
                        fillColor: "#123456",
                        fillOpacity: 0.7,
                        strokeWidth: 3
                    >>;
                    selected;
                    """.trimIndent(),
                board = board,
            ),
        )
        val polyhedron = assertIs<Polyhedron3D>(board.select("solid"))

        assertTrue("face3d" in NativeJessieCodeCreators.names)
        assertEquals(Const.OBJECT_TYPE_FACE3D, face.type)
        assertEquals("face3d", face.elType)
        assertEquals(0, face.faceNumber)
        assertSame(polyhedron.definition, face.polyhedron)
        assertEquals("#123456", face.faceAttributes.fillColor)
        assertEquals(0.7, face.faceAttributes.fillOpacity)
        assertEquals(3.0, face.faceAttributes.strokeWidth)
        assertEquals(4, face.curve2D.numberPoints)
        assertEquals(listOf(face.id), face.curve2D.parents)
        assertSame(face.curve2D, face.element2D)
    }

    @Test
    fun invalidParentsAndFaceNumbersRemainStructuredAndAtomic() {
        val invalidParent = creatorError(
            source = setupSource() +
                """
                point = point3d(view, [0, 0, 0]) <<
                    id: "point", name: ""
                >>;
                face3d(view, point, 0);
                """.trimIndent(),
            board = board("invalid-parent"),
        )
        assertEquals("face3d", invalidParent.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            invalidParent.error,
        )

        val invalidIndexBoard = board("invalid-index")
        val invalidIndex = creatorError(
            source = setupSource() +
                """
                face3d(view, solid, 4) << id: "bad", name: "" >>;
                """.trimIndent(),
            board = invalidIndexBoard,
        )
        assertEquals("face3d", invalidIndex.creatorName)
        assertEquals(
            Face3DError.InvalidFaceNumber(
                faceNumber = 4,
                faceCount = 1,
            ),
            assertIs<JessieCodeCreatorError.Face3DFactory>(
                invalidIndex.error,
            ).error,
        )
        assertNull(invalidIndexBoard.elementById("bad"))

        val duplicateBoard = board("duplicate")
        val duplicate = creatorError(
            source = setupSource() +
                """
                taken = point(8, 8) << id: "taken", name: "" >>;
                face3d(view, solid, 0) << id: "taken", name: "" >>;
                """.trimIndent(),
            board = duplicateBoard,
        )
        assertEquals(
            Face3DError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            assertIs<JessieCodeCreatorError.Face3DFactory>(
                duplicate.error,
            ).error,
        )
        assertIs<Point>(duplicateBoard.elementById("taken"))
        assertTrue(
            duplicateBoard.objects.values.none {
                it is Face3D && it.id == "taken"
            },
        )
    }

    private fun setupSource(): String =
        """
        view = view3d(
            [-5, -4],
            [8, 7],
            [[-5, 5], [-4, 6], [-3, 7]]
        ) <<
            id: "view",
            name: "",
            projection: "parallel",
            axesPosition: "none"
        >>;
        solid = polyhedron3d(
            view,
            [[-2, -1, 0], [2, -1, 0], [2, 3, 0]],
            [[0, 1, 2]]
        ) << id: "solid", name: "" >>;
        """.trimIndent()

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

    private fun face(value: JessieCodeRuntimeValue): Face3D =
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
