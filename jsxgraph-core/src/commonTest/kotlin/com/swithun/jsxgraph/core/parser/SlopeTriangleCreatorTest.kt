/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.SlopeTriangleDefinition
import com.swithun.jsxgraph.core.base.SlopeTriangleError
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SlopeTriangleCreatorTest {
    @Test
    fun creatorBuildsHelpersAndExposesOfficialMethodMap() {
        val board = board()
        val values = array(
            evaluate(
                source =
                    """
                    A = point(-3, -1) << id: "A", name: "" >>;
                    B = point(3, 2) << id: "B", name: "" >>;
                    source = segment(A, B) << id: "source", name: "" >>;
                    g = glider(0, 0.5, source) <<
                        id: "sourceGlider", name: ""
                    >>;
                    t = tangent(source, g) << id: "tangent", name: "" >>;
                    st = slopetriangle(t) <<
                        id: "triangle", name: "",
                        digits: 3, prefix: "m=", suffix: "!",
                        basepoint: << id: "base", name: "" >>,
                        baseline: << id: "baseline", name: "" >>,
                        glider: << id: "helper", name: "" >>,
                        toppoint: << id: "top", name: "" >>,
                        tangent: << id: "ignoredTangent", name: "" >>
                    >>;
                    [
                        st,
                        st.Value(),
                        st.V(),
                        st.Slope(),
                        st.Angle("degrees"),
                        st.DeltaX(),
                        st.DeltaY(),
                        st.Direction(),
                        st.tangent,
                        st.glider,
                        st.basepoint,
                        st.baseline,
                        st.toppoint,
                        st.borderHorizontal,
                        st.borderVertical,
                        st.borderParallel,
                        st.label,
                        st.subs.basePoint
                    ];
                    """.trimIndent(),
                board = board,
            ),
        )
        val triangle = assertIs<Polygon>(element(values.values[0]))
        val definition = assertIs<SlopeTriangleDefinition>(
            triangle.slopeTriangleDefinition,
        )

        assertTrue("slopetriangle" in NativeJessieCodeCreators.names)
        assertEquals("slopetriangle", triangle.elType)
        assertEquals(0.5, number(values.values[1]))
        assertEquals(0.5, number(values.values[2]))
        assertEquals(0.5, number(values.values[3]))
        assertEquals(
            26.56505117707799,
            number(values.values[4]),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(1.0, number(values.values[5]))
        assertEquals(0.5, number(values.values[6]))
        assertContentEquals(
            doubleArrayOf(6.0, 3.0),
            numbers(array(values.values[7])),
        )
        assertSame(definition.tangent, element(values.values[8]))
        assertSame(definition.glider, element(values.values[9]))
        assertSame(definition.basePoint, element(values.values[10]))
        assertSame(definition.baseLine, element(values.values[11]))
        assertSame(definition.topPoint, element(values.values[12]))
        assertSame(definition.borderHorizontal, element(values.values[13]))
        assertSame(definition.borderVertical, element(values.values[14]))
        assertSame(definition.borderParallel, element(values.values[15]))
        assertSame(definition.label, element(values.values[16]))
        assertSame(definition.basePoint, element(values.values[17]))
        assertEquals("m=0.500!", definition.label.plaintext)
    }

    @Test
    fun gliderParentCreatesPrivateTangentAndDeleteRemovesHelpers() {
        val board = board()
        val triangle = assertIs<Polygon>(
            element(
                evaluate(
                    source =
                        """
                        A = point(-4, 2) << id: "A", name: "" >>;
                        B = point(4, -2) << id: "B", name: "" >>;
                        source = segment(A, B) << id: "source", name: "" >>;
                        g = glider(0, 0, source) <<
                            id: "sourceGlider", name: ""
                        >>;
                        slopetriangle(g) <<
                            id: "triangle", name: "",
                            tangent: << id: "privateTangent", name: "" >>
                        >>;
                        """.trimIndent(),
                    board = board,
                ),
            ),
        )
        val definition = assertIs<SlopeTriangleDefinition>(
            triangle.slopeTriangleDefinition,
        )

        assertTrue(definition.isPrivateTangent)
        assertEquals("privateTangent", definition.tangent.id)
        assertEquals(listOf("sourceGlider"), definition.tangent.parents)

        evaluate("delete triangle;", board)
        assertEquals(
            listOf("A", "B", "source", "sourceGlider"),
            board.objectsList.map(GeometryElement::id),
        )
    }

    @Test
    fun duplicateHelperIdentityRollsBackCreatorState() {
        val board = board()
        evaluate(
            source =
                """
                A = point(-2, 0) << id: "A", name: "" >>;
                B = point(2, 2) << id: "B", name: "" >>;
                source = segment(A, B) << id: "source", name: "" >>;
                g = glider(0, 1, source) <<
                    id: "sourceGlider", name: ""
                >>;
                duplicate = point(5, 5) << id: "duplicate", name: "" >>;
                """.trimIndent(),
            board = board,
        )
        val before = board.objectsList.toList()

        val error = creatorError(
            source =
                """
                slopetriangle("source", "sourceGlider") <<
                    id: "triangle", name: "",
                    basepoint: << id: "duplicate" >>
                >>;
                """.trimIndent(),
            board = board,
        )
        assertEquals("slopetriangle", error.creatorName)
        val factory = assertIs<
            JessieCodeCreatorError.SlopeTriangleFactory,
            >(error.error)
        assertIs<SlopeTriangleError.PointFactory>(factory.error)
        assertEquals(before, board.objectsList)
    }

    private fun evaluate(
        source: String,
        board: Board,
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        val result = JessieCodeEvaluator().evaluate(
            ast,
            JessieCodeRuntimeEnvironment(board = board),
        )
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            result,
            result.toString(),
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

    private fun number(value: JessieCodeRuntimeValue): Double =
        assertIs<JessieCodeRuntimeValue.NumberValue>(value).value

    private fun numbers(
        value: JessieCodeRuntimeValue.ArrayValue,
    ): DoubleArray = value.values.map(::number).toDoubleArray()

    private fun array(
        value: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue.ArrayValue = assertIs(value)

    private fun element(
        value: JessieCodeRuntimeValue,
    ): GeometryElement =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value).element

    private fun board(): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = "board",
    )
}
