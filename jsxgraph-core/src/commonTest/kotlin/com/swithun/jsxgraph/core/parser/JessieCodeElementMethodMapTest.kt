package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
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
    fun arcAndSectorMethodsExposeTranslatedMeasurementsAndPoints() {
        val values = array(
            evaluate(
                source =
                    "A = point(-4, -1); B = point(1, 4); " +
                        "C = point(5, -2); " +
                        "a = arc(B, A, C); " +
                        "n = nonreflexangle(A, B, C) << radius: 2 >>; " +
                        "r = reflexangle(A, B, C) << radius: 2 >>; " +
                        "[a.Value(\"radians\"), n.Value(), " +
                        "r.Value(\"degrees\"), n.Radius(), V(n), " +
                        "n.center == B, n.point2 == A, " +
                        "n.Value(\"turn\")];",
                board = board(),
            ),
        )

        assertEquals(
            1.3734007669450157,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[0],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            1.3734007669450157,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[1],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            281.30993247402023,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[2],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(2.0),
            values.values[3],
        )
        assertEquals(values.values[1], values.values[4])
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[5],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[6],
        )
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            values.values[7],
        )
    }

    @Test
    fun ellipseExposesItsMajorAxisAndCenterMetadata() {
        val values = array(
            evaluate(
                source =
                    "F1 = point(-3, 0); F2 = point(3, 0); C = point(0, 5); " +
                        "e = ellipse(F1, F2, C) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "[e.majorAxis(), e.center == e.midpoint, " +
                        "e.subs.center == e.center];",
                board = board(),
            ),
        )

        assertEquals(
            11.661903789690601,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[0],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[1],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[2],
        )
    }

    @Test
    fun hyperbolaExposesItsMajorAxisAndCenterMetadata() {
        val values = array(
            evaluate(
                source =
                    "F1 = point(-3, 0); F2 = point(3, 0); C = point(5, 2); " +
                        "h = hyperbola(F1, F2, C) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "[h.majorAxis(), h.center == h.midpoint, " +
                        "h.subs.center == h.center];",
                board = board(),
            ),
        )

        assertEquals(
            5.417784126489131,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[0],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[1],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[2],
        )
    }

    @Test
    fun tangentToExposesItsPublicPointAndPolarReferences() {
        val board = board("tangent-to-properties")
        val values = array(
            evaluate(
                source =
                    """
                    O = point(0, 0);
                    R = point(3, 0);
                    C = circle(O, R);
                    P = point(5, 1);
                    T = tangentto(C, P) <<
                        id: "tangent",
                        name: "",
                        point1: << id: "tangentPoint1", name: "" >>,
                        point2: << id: "tangentPoint2", name: "" >>,
                        polar: <<
                            id: "polar",
                            name: "",
                            point1: << id: "polarPoint1", name: "" >>,
                            point2: << id: "polarPoint2", name: "" >>
                        >>,
                        point: << id: "intersection", name: "" >>
                    >>;
                    [T.point, T.polar, T.point1, T.point2];
                    """.trimIndent(),
                board = board,
            ),
        )
        val tangent = assertIs<Line>(board.select("tangent"))
        val intersection = assertIs<IntersectionPoint>(
            element(values.values[0]),
        )
        val polar = assertIs<Line>(element(values.values[1]))

        assertSame(tangent.tangentToPoint, intersection)
        assertSame(tangent.tangentToPolar, polar)
        assertSame(tangent.point1, element(values.values[2]))
        assertSame(tangent.point2, element(values.values[3]))

        val plainLinePoint = evaluateError(
            source =
                "A = point(0, 0); B = point(1, 1); " +
                    "L = line(A, B); L.point;",
            board = board("plain-line-point"),
        )
        assertEquals(
            "point",
            assertIs<JessieCodeRuntimeError.ElementPropertyUnavailable>(
                plainLinePoint,
            ).property,
        )
        val plainLinePolar = evaluateError(
            source =
                "A = point(0, 0); B = point(1, 1); " +
                    "L = line(A, B); L.polar;",
            board = board("plain-line-polar"),
        )
        assertEquals(
            "polar",
            assertIs<JessieCodeRuntimeError.ElementPropertyUnavailable>(
                plainLinePolar,
            ).property,
        )
    }

    @Test
    fun riemannSumValueMethodAndValueBuiltinExposeCachedArea() {
        val values = array(
            evaluate(
                source =
                    """
                    f = function (x) { return x * x + 1; };
                    r = riemannsum(f, 3, "left", -1, 2);
                    [r.Value(), V(r)];
                    """.trimIndent(),
                board = board(),
            ),
        )

        assertEquals(
            5.0,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[0],
            ).value,
        )
        assertEquals(
            5.0,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[1],
            ).value,
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
