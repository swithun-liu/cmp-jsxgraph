package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardError
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.Polygon
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NativeJessieCodeCreatorsTest {
    @Test
    fun pointCreatorUsesAssignmentNameAndExplicitIdentityAttributes() {
        val implicitBoard = board("implicit")
        val implicit = point(
            evaluate(
                source = "A = point(1, 2); A;",
                board = implicitBoard,
            ),
        )

        assertEquals("A", implicit.name)
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 2.0),
            implicit.coords.usrCoords,
        )
        assertSame(implicit, implicitBoard.select("A"))

        val explicitBoard = board("explicit")
        val explicit = point(
            evaluate(
                source =
                    "A = point(3, 4) << " +
                        "id: \"fixed\", name: \"Named\", " +
                        "needsRegularUpdate: false >>; A;",
                board = explicitBoard,
            ),
        )
        assertEquals("fixed", explicit.id)
        assertEquals("Named", explicit.name)
        assertEquals(false, explicit.needsRegularUpdate)
        assertSame(explicit, explicitBoard.select("fixed"))
        assertSame(explicit, explicitBoard.select("Named"))
    }

    @Test
    fun nestedAssignmentUsesTheInnermostCreatorName() {
        val board = board()

        val point = point(
            evaluate(
                source = "A = B = point(1, 2); A;",
                board = board,
            ),
        )

        assertEquals("B", point.name)
        assertSame(point, board.select("B"))
        assertSame(null, board.select("A"))
    }

    @Test
    fun pointCreatorSupportsMixedJessieCodeCoordinateExpressions() {
        val board = board()

        val point = point(
            evaluate(
                source =
                    "A = point(2, 3); " +
                        "B = point(\"A.X() + 1\", 2); B;",
                board = board,
            ),
        )

        assertEquals("B", point.name)
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 2.0),
            point.coords.usrCoords,
        )
        val driver = assertIs<Point>(board.select("A"))
        assertSame(point, driver.childElements[point.id])
    }

    @Test
    fun lineCreatorSupportsPointsCoordinateArraysAndCoefficients() {
        val pointBoard = board("point-line")
        val pointLine = line(
            evaluate(
                source =
                    "A = point(0, 0); B = point(4, 2); " +
                        "l = line(A, B); l;",
                board = pointBoard,
            ),
        )
        assertEquals("l", pointLine.name)
        assertEquals(
            listOf(
                assertIs<Point>(pointBoard.select("A")).id,
                assertIs<Point>(pointBoard.select("B")).id,
            ),
            pointLine.parents,
        )
        assertEquals(0.5, pointLine.Slope())

        val coordinateBoard = board("coordinate-line")
        val coordinateLine = line(
            evaluate(
                source = "line([0, 0], [2, 3]);",
                board = coordinateBoard,
            ),
        )
        assertEquals(3, coordinateBoard.numObjects)
        assertEquals("", coordinateLine.point1.name)
        assertEquals("", coordinateLine.point2.name)
        assertEquals(1.5, coordinateLine.Slope())

        val coefficientBoard = board("coefficient-line")
        val coefficientLine = line(
            evaluate(
                source = "line(1, -2, 3);",
                board = coefficientBoard,
            ),
        )
        assertEquals(3, coefficientBoard.numObjects)
        val normalization = sqrt(13.0)
        assertEquals(-1.0 / normalization, coefficientLine.stdform[0])
        assertEquals(2.0 / normalization, coefficientLine.stdform[1])
        assertEquals(-3.0 / normalization, coefficientLine.stdform[2])
    }

    @Test
    fun circleCreatorSupportsTranslatedParentOverloads() {
        val board = board()
        val radiusCircle = circle(
            evaluate(
                source =
                    "A = point(1, 2); " +
                        "c = circle(A, -4); c;",
                board = board,
            ),
        )
        assertEquals("c", radiusCircle.name)
        assertEquals(4.0, radiusCircle.Radius())
        assertSame(board.select("A"), radiusCircle.center)

        val pointCircle = circle(
            evaluate(
                source =
                    "A = point(1, 2); B = point(4, 6); " +
                        "c = circle(A, B); c;",
                board = board("point-circle"),
            ),
        )
        assertEquals(5.0, pointCircle.Radius())

        val coordinateCircle = circle(
            evaluate(
                source = "circle([1, 2], 3);",
                board = board("coordinate-circle"),
            ),
        )
        assertEquals("", coordinateCircle.center.name)
        assertEquals(3.0, coordinateCircle.Radius())

        val elementRadiusBoard = board("element-radius-circle")
        val inheritedRadius = circle(
            evaluate(
                source =
                    "A = point(0, 0); B = point(3, 4); " +
                        "l = line(A, B); C = point(2, 2); " +
                        "c = circle(C, l); d = circle(c, A); d;",
                board = elementRadiusBoard,
            ),
        )
        assertEquals(5.0, inheritedRadius.Radius())
        assertSame(elementRadiusBoard.select("A"), inheritedRadius.center)
        val sourceCircle = assertIs<Circle>(
            elementRadiusBoard.select("c"),
        )
        assertSame(sourceCircle, inheritedRadius.circle)
    }

    @Test
    fun curveCreatorsSupportDataParametricAndFunctionGraphParents() {
        val data = curve(
            evaluate(
                source = "curve([-2, 0, 3], [4, 1, -2]);",
                board = board("data-curve"),
            ),
        )
        assertEquals("plot", data.curveType)
        assertEquals(3, data.numberPoints)
        assertContentEquals(
            doubleArrayOf(-2.0, 0.0, 3.0),
            data.points.map { it.usrCoords[1] }.toDoubleArray(),
        )

        val parametric = curve(
            evaluate(
                source =
                    "curve(\"2 * x\", \"x * x\", -1, 1) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 4 >>;",
                board = board("parametric-curve"),
            ),
        )
        assertEquals("parameter", parametric.curveType)
        assertContentEquals(
            doubleArrayOf(-2.0, -1.0, 0.0, 1.0),
            parametric.points.map { it.usrCoords[1] }.toDoubleArray(),
        )

        val functionGraph = curve(
            evaluate(
                source =
                    "functiongraph(\"x * x\", -2, 2) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 4 >>;",
                board = board("function-graph"),
            ),
        )
        assertEquals("functiongraph", functionGraph.curveType)
        assertContentEquals(
            doubleArrayOf(4.0, 1.0, 0.0, 1.0),
            functionGraph.points.map { it.usrCoords[2] }.toDoubleArray(),
        )

        val plot = curve(
            evaluate(
                source =
                    "plot(\"x + 1\", -1, 1) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 2 >>;",
                board = board("plot-alias"),
            ),
        )
        assertEquals("functiongraph", plot.curveType)
        assertContentEquals(
            doubleArrayOf(0.0, 1.0),
            plot.points.map { it.usrCoords[2] }.toDoubleArray(),
        )
    }

    @Test
    fun continuousCurveCreatorRequiresTranslatedSamplingMode() {
        val error = creatorError(
            source = "functiongraph(\"x\", -1, 1);",
            board = board(),
        )

        assertEquals("functiongraph", error.creatorName)
        assertEquals(
            JessieCodeCreatorError.UnsupportedAttributeValue(
                attribute = "doAdvancedPlot",
                actual = "true",
            ),
            error.error,
        )
    }

    @Test
    fun polygonCreatorSupportsCoordinateVerticesAndGeometryMethods() {
        val board = board("polygon")
        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    "P = polygon([0, 0], [4, 0], [0, 3]); " +
                        "[P.Area(), P.L(), Area(P), Perimeter(P), " +
                        "P.vertices.length, P.borders.length, " +
                        "P.BoundingBox()];",
                board = board,
            ),
        ).values
        val polygon = assertIs<Polygon>(board.select("P"))

        assertEquals(4, polygon.vertices.size)
        assertEquals(3, polygon.ownedVertices.size)
        assertEquals(3, polygon.borders.size)
        assertEquals(
            listOf(6.0, 12.0, 6.0, 12.0, 4.0, 3.0),
            values.dropLast(1).map {
                assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
            },
        )
        assertEquals(
            listOf(0.0, 3.0, 4.0, 0.0),
            assertIs<JessieCodeRuntimeValue.ArrayValue>(
                values.last(),
            ).values.map {
                assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
            },
        )
        assertEquals(
            polygon.vertices.dropLast(1).map(Point::id).toSet(),
            polygon.ownedVertices.map(Point::id).toSet(),
        )
    }

    @Test
    fun polygonCreatorRollsBackMaterializedPointsAfterFailure() {
        val board = board("polygon-failure")
        val error = creatorError(
            source = "polygon([0, 0], [\"x +\", 1]);",
            board = board,
        )

        assertEquals("polygon", error.creatorName)
        assertIs<JessieCodeCreatorError.PointFactory>(error.error)
        assertTrue(board.objects.isEmpty())
        assertTrue(board.objectsList.isEmpty())
    }

    @Test
    fun creatorUsesTheBoardSelectedByUse() {
        val first = board("first")
        val second = board("second")

        val created = point(
            evaluate(
                source = "use secondcontainer; A = point(7, 8); A;",
                board = first,
                boardsByContainer = mapOf("secondcontainer" to second),
            ),
        )

        assertSame(second, created.board)
        assertTrue(first.objects.isEmpty())
        assertSame(created, second.select("A"))
    }

    @Test
    fun defaultElementRuntimeExposesCreatedGeometryMethods() {
        val value = evaluate(
            source = "A = point(2, 3); A.X() + A.Y();",
            board = board(),
        )

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(5.0),
            value,
        )
    }

    @Test
    fun creatorFailuresRemainStructuredAndDoNotThrow() {
        val missingBoard = creatorError(
            source = "point(1, 2);",
            board = null,
        )
        assertEquals("point", missingBoard.creatorName)
        assertIs<JessieCodeCreatorError.BoardUnavailable>(
            missingBoard.error,
        )

        val unsupported = creatorError(
            source = "line(1, 2);",
            board = board(),
        )
        assertEquals(
            listOf("number", "number"),
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                unsupported.error,
            ).parentTypes,
        )

        val invalidAttribute = creatorError(
            source = "point(1, 2) << id: 7 >>;",
            board = board(),
        )
        val attributeError =
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidAttribute.error,
            )
        assertEquals("id", attributeError.attribute)
        assertEquals("string", attributeError.expected)
        assertEquals("number", attributeError.actual)

        val duplicateBoard = board("duplicate")
        point(
            evaluate(
                source = "point(1, 2) << id: \"fixed\" >>;",
                board = duplicateBoard,
            ),
        )
        val duplicate = creatorError(
            source = "point(3, 4) << id: \"fixed\" >>;",
            board = duplicateBoard,
        )
        val pointError = assertIs<JessieCodeCreatorError.PointFactory>(
            duplicate.error,
        )
        assertEquals(
            PointError.Registration(
                BoardError.DuplicateElementId("fixed"),
            ),
            pointError.error,
        )
        assertEquals(1, duplicateBoard.numObjects)
    }

    @Test
    fun customCreatorOverridesTheNativeRegistry() {
        val marker = JessieCodeRuntimeValue.StringValue("custom")
        var attributes: JessieCodeRuntimeValue.ObjectValue? = null
        val result = evaluate(
            source = "A = point(1, 2); A;",
            board = board(),
            creators = mapOf(
                "point" to JessieCodeCreator { _, _, value, _ ->
                    attributes = value
                    GMResult.Ok(marker)
                },
            ),
        )

        assertSame(marker, result)
        assertEquals(
            JessieCodeRuntimeValue.StringValue("A"),
            attributes?.properties?.get("name"),
        )
    }

    private fun evaluate(
        source: String,
        board: Board?,
        boardsByContainer: Map<String, Board> = emptyMap(),
        creators: Map<String, JessieCodeCreator> = emptyMap(),
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(
                    creators = creators,
                    board = board,
                    boardsByContainer = boardsByContainer,
                ),
            ),
        ).value
    }

    private fun creatorError(
        source: String,
        board: Board?,
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

    private fun point(value: JessieCodeRuntimeValue): Point =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun line(value: JessieCodeRuntimeValue): Line =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun circle(value: JessieCodeRuntimeValue): Circle =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun curve(value: JessieCodeRuntimeValue): Curve =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun board(id: String = "board"): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = id,
    )
}
