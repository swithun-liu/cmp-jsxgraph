package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JessieCodeElementSetPropTest {
    @Test
    fun freePointNumericCoordinatesUseThePositionPath() {
        val board = board()
        val value = array(
            evaluate(
                source =
                    "A = point(1, 2); " +
                        "x = A.X = 4; y = A.Y = -3; [x, y];",
                board = board,
            ),
        )
        val point = assertIs<Point>(board.select("A"))

        assertEquals(
            listOf<JessieCodeRuntimeValue>(
                JessieCodeRuntimeValue.NumberValue(4.0),
                JessieCodeRuntimeValue.NumberValue(-3.0),
            ),
            value.values,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, -3.0),
            point.coords.usrCoords,
        )
        assertTrue(point.isDraggable)
        assertFalse(point.isConstrained)
        assertTrue(point.coordinateFunctions.isEmpty())
    }

    @Test
    fun stringCoordinateConvertsAFreePointToAConstraint() {
        val board = board()
        val point = point(
            evaluate(
                source =
                    "D = point(2, 3); A = point(1, 2); " +
                        "A.X = \"D.X() + 1\"; A;",
                board = board,
            ),
        )
        val driver = assertIs<Point>(board.select("D"))

        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 2.0),
            point.coords.usrCoords,
        )
        assertFalse(point.isDraggable)
        assertTrue(point.isConstrained)
        assertEquals(Const.OBJECT_TYPE_CAS, point.type)
        assertEquals("D.X() + 1", point.Xjc)
        assertEquals("2", point.Yjc)
        assertSame(point, driver.childElements[point.id])

        driver.setPosition(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(5.0, 7.0),
        )
        board.update()

        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 2.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun constrainedNumericCoordinatePreservesTheOtherOrigin() {
        val board = board()
        val point = point(
            evaluate(
                source =
                    "D = point(2, 3); " +
                        "A = point(\"D.X() + 1\", \"D.Y() + 2\"); " +
                        "A.X = 8; A;",
                board = board,
            ),
        )
        val driver = assertIs<Point>(board.select("D"))

        assertContentEquals(
            doubleArrayOf(1.0, 8.0, 5.0),
            point.coords.usrCoords,
        )
        assertEquals("8", point.Xjc)
        assertEquals("D.Y() + 2", point.Yjc)
        assertFalse(point.isDraggable)

        driver.setPosition(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(10.0, 20.0),
        )
        board.update()

        assertContentEquals(
            doubleArrayOf(1.0, 8.0, 22.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun replacingAConstraintUpdatesDependencyOwnership() {
        val board = board()
        val point = point(
            evaluate(
                source =
                    "D = point(2, 3); E = point(5, 6); " +
                        "A = point(\"D.X()\", 2); " +
                        "A.X = \"E.X() + 1\"; A;",
                board = board,
            ),
        )
        val oldDriver = assertIs<Point>(board.select("D"))
        val newDriver = assertIs<Point>(board.select("E"))

        assertNull(oldDriver.childElements[point.id])
        assertSame(point, newDriver.childElements[point.id])
        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 2.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun namesAndRegularUpdateUseTheTranslatedAttributeSubset() {
        val board = board()
        val values = array(
            evaluate(
                source =
                    "A = point(1, 2); " +
                        "A.Name = \"Mapped\"; " +
                        "A.needsRegularUpdate = \"false\"; " +
                        "[A.name, A.Name(), A.needsRegularUpdate];",
                board = board,
            ),
        )
        val point = assertIs<Point>(board.select("Mapped"))

        assertEquals(
            listOf<JessieCodeRuntimeValue>(
                JessieCodeRuntimeValue.StringValue("Mapped"),
                JessieCodeRuntimeValue.StringValue("Mapped"),
                JessieCodeRuntimeValue.BooleanValue(false),
            ),
            values.values,
        )
        assertFalse(point.needsRegularUpdate)
        assertNull(board.select("A"))

        evaluate(
            source =
                "Mapped.needsRegularUpdate = 0; " +
                    "Mapped.name = \"Renamed\";",
            board = board,
        )
        assertSame(point, board.select("Renamed"))
        assertNull(board.select("Mapped"))
        assertTrue(point.needsRegularUpdate)
    }

    @Test
    fun untranslatedVisualPropertiesReturnAStructuredFailure() {
        val board = board()
        val error = evaluateError(
            source = "A = point(1, 2); A.x = 9;",
            board = board,
        )
        val point = assertIs<Point>(board.select("A"))

        assertEquals(
            "x",
            assertIs<
                JessieCodeRuntimeError.ElementPropertyAssignmentUnavailable
                >(error).property,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 2.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun invalidCoordinateValuesAndExpressionsDoNotMutateThePoint() {
        val invalidTypeBoard = board("invalid-type")
        val invalidType = evaluateError(
            source = "A = point(1, 2); A.X = << value: 3 >>;",
            board = invalidTypeBoard,
        )
        val invalidTypePoint = assertIs<Point>(
            invalidTypeBoard.select("A"),
        )
        val typeError = assertIs<
            JessieCodeRuntimeError.InvalidElementPropertyValue
            >(invalidType)
        assertEquals("number or string", typeError.expected)
        assertEquals("object", typeError.actual)
        assertTrue(invalidTypePoint.isDraggable)

        val invalidExpressionBoard = board("invalid-expression")
        val invalidExpression = evaluateError(
            source = "A = point(1, 2); A.X = \"1 +\";",
            board = invalidExpressionBoard,
        )
        val invalidExpressionPoint = assertIs<Point>(
            invalidExpressionBoard.select("A"),
        )
        val constraintError = assertIs<
            JessieCodeRuntimeError.ElementCoordinateConstraintFailure
            >(invalidExpression)
        assertIs<PointError.CoordinateExpressionCompile>(
            constraintError.error,
        )
        assertTrue(invalidExpressionPoint.isDraggable)
        assertTrue(invalidExpressionPoint.coordinateFunctions.isEmpty())
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 2.0),
            invalidExpressionPoint.coords.usrCoords,
        )
    }

    @Test
    fun elementAssignmentsDelegateToTheConfiguredRuntime() {
        val board = board()
        val element = GeometryElement(
            board = board,
            id = "element",
            name = "A",
            type = Const.OBJECT_TYPE_POINT,
            elementClass = Const.OBJECT_CLASS_POINT,
        )
        assertIs<GMResult.Ok<String>>(board.setId(element, "E"))
        var assignedProperty: String? = null
        var assignedValue: JessieCodeRuntimeValue? = null
        val runtime = object : JessieCodeElementRuntime {
            override fun valueOf(
                element: GeometryElement,
                location: JessieCodeAstLocation,
            ): GMResult<
                JessieCodeRuntimeValue,
                JessieCodeRuntimeError,
                > = GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)

            override fun resolveProperty(
                element: GeometryElement,
                property: String,
                location: JessieCodeAstLocation,
            ): GMResult<
                JessieCodeRuntimeValue,
                JessieCodeRuntimeError,
                > = GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)

            override fun assignProperty(
                element: GeometryElement,
                property: String,
                value: JessieCodeRuntimeValue,
                location: JessieCodeAstLocation,
            ): GMResult<Unit, JessieCodeRuntimeError> {
                assignedProperty = property
                assignedValue = value
                return GMResult.Ok(Unit)
            }
        }

        val value = evaluate(
            source = "A.custom = 7;",
            board = board,
            elementRuntime = runtime,
        )

        assertEquals("custom", assignedProperty)
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(7.0),
            assignedValue,
        )
        assertEquals(JessieCodeRuntimeValue.NumberValue(7.0), value)
    }

    private fun evaluate(
        source: String,
        board: Board,
        elementRuntime: JessieCodeElementRuntime =
            CoreGeometryElementRuntime,
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        val result = JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(
                    board = board,
                    elementRuntime = elementRuntime,
                ),
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

    private fun point(value: JessieCodeRuntimeValue): Point =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun array(
        value: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue.ArrayValue = assertIs(value)

    private fun board(id: String = "set-prop"): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = id,
    )
}
