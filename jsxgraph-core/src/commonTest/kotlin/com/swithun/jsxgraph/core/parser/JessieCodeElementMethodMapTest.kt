package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Point
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class JessieCodeElementMethodMapTest {
    @Test
    fun boundsUsesTheTranslatedElementImplementations() {
        val values = array(
            evaluate(
                source =
                    "A = point(1, 2); B = point(3, 4); " +
                        "l = line(A, B); c = circle(A, 2); " +
                        "[A.Bounds(), l.Bounds(), c.Bounds()];",
                board = board(),
            ),
        )

        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 1.0, 2.0),
            numbers(array(values.values[0])),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 3.0, 2.0),
            numbers(array(values.values[1])),
        )
        assertContentEquals(
            doubleArrayOf(-1.0, 4.0, 3.0, 0.0),
            numbers(array(values.values[2])),
        )
    }

    @Test
    fun addChildReturnsTheOwnerAndUpdatesDependencyMaps() {
        val board = board()
        val value = element(
            evaluate(
                source =
                    "A = point(1, 2); B = point(3, 4); " +
                        "A.addChild(B);",
                board = board,
            ),
        )
        val child = assertIs<Point>(board.select("B"))

        assertSame(board.select("A"), value)
        assertSame(child, value.childElements[child.id])
        assertSame(value, child.ancestors[value.id])
    }

    @Test
    fun setNameEscapesMarkupAndReturnsUndefined() {
        val board = board()
        val value = evaluate(
            source = "A = point(1, 2); A.setName(\"<B>\");",
            board = board,
        )

        assertSame(JessieCodeRuntimeValue.UndefinedValue, value)
        assertNull(board.select("A"))
        assertIs<Point>(board.select("&lt;B&gt;"))
    }

    @Test
    fun moveAndMoveToUseTheImmediateBoardUpdatePath() {
        val board = board()
        val moved = assertIs<JessieCodeRuntimeValue.BoardReference>(
            evaluate(
                source =
                    "A = point(1, 2); " +
                        "D = point(\"A.X() + 1\", \"A.Y() + 2\"); " +
                        "A.move([4, 5]);",
                board = board,
            ),
        )
        val point = assertIs<Point>(board.select("A"))
        val dependent = assertIs<Point>(board.select("D"))

        assertSame(board, moved.board)
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 5.0),
            point.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, 7.0),
            dependent.coords.usrCoords,
        )

        val movedAgain = assertIs<JessieCodeRuntimeValue.BoardReference>(
            evaluate(
                source = "A.moveTo([6, 7], 0);",
                board = board,
            ),
        )
        assertSame(board, movedAgain.board)
        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 7.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun addConstraintReturnsThePointAndTracksDependencies() {
        val board = board()
        val point = assertIs<Point>(
            element(
                evaluate(
                    source =
                        "A = point(1, 2); B = point(3, 4); " +
                            "A.addConstraint([\"B.X() + 1\", 2]);",
                    board = board,
                ),
            ),
        )
        val driver = assertIs<Point>(board.select("B"))

        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 2.0),
            point.coords.usrCoords,
        )
        assertSame(point, driver.childElements[point.id])

        driver.setPosition(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(8.0, 9.0),
        )
        board.update()
        assertContentEquals(
            doubleArrayOf(1.0, 9.0, 2.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun untranslatedAnimationAndInvalidArgumentsAreStructured() {
        val animationBoard = board("animation")
        val animation = evaluateError(
            source = "A = point(1, 2); A.move([4, 5], 100);",
            board = animationBoard,
        )
        val animationError = assertIs<
            JessieCodeRuntimeError.ElementMethodUnavailable
            >(animation)
        assertEquals("moveTo", animationError.method)
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 2.0),
            assertIs<Point>(animationBoard.select("A"))
                .coords.usrCoords,
        )

        val coordinateCount = evaluateError(
            source = "A = point(1, 2); A.move([4]);",
            board = board("coordinate-count"),
        )
        assertEquals(
            "2 or 3",
            assertIs<JessieCodeRuntimeError.InvalidArgumentCount>(
                coordinateCount,
            ).expected,
        )

        val constraintType = evaluateError(
            source =
                "A = point(1, 2); " +
                    "A.addConstraint([1, << value: 2 >>]);",
            board = board("constraint-type"),
        )
        assertEquals(
            "object",
            assertIs<JessieCodeRuntimeError.InvalidArgumentType>(
                constraintType,
            ).actual,
        )
    }

    @Test
    fun methodFailuresDoNotChangeObjectAndArrayAssignment() {
        val values = array(
            evaluate(
                source =
                    "o = << value: 1 >>; a = [2]; " +
                        "o.value = 3; a[0] = 4; [o.value, a[0]];",
                board = board(),
            ),
        )

        assertEquals(
            listOf<JessieCodeRuntimeValue>(
                JessieCodeRuntimeValue.NumberValue(3.0),
                JessieCodeRuntimeValue.NumberValue(4.0),
            ),
            values.values,
        )
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

    private fun numbers(
        value: JessieCodeRuntimeValue.ArrayValue,
    ): DoubleArray = value.values.map {
        assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
    }.toDoubleArray()

    private fun array(
        value: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue.ArrayValue = assertIs(value)

    private fun element(
        value: JessieCodeRuntimeValue,
    ) = assertIs<JessieCodeRuntimeValue.ElementReference>(value).element

    private fun board(id: String = "method-map"): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = id,
    )
}
