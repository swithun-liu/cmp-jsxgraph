package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JessieCodeExpressionFunctionTest {
    @Test
    fun nameReplacementMatchesOfficialStableIdCalls() {
        val fixture = boardFixture()
        val ast = parse("A + sin(B);")
        val replaced = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeNameReplacer(fixture.board).replace(ast),
        ).value
        val expression = nodeChild(replaced, 1)
        val pointCall = nodeChild(expression, 0)
        val sinCall = nodeChild(expression, 1)
        val sliderCall = nodeList(sinCall, 1).single()

        assertEquals("op_execfun", operationName(pointCall))
        assertEquals("\$", textValue(nodeChild(pointCall, 0)))
        assertEquals("P1", textValue(nodeList(pointCall, 1).single()))
        assertTrue(pointCall.replaced)

        assertEquals("op_execfun", operationName(sliderCall))
        assertEquals("\$value", textValue(nodeChild(sliderCall, 0)))
        assertEquals("P2", textValue(nodeList(sliderCall, 1).single()))
        assertTrue(sliderCall.replaced)
    }

    @Test
    fun nameReplacementTraversesObjectValuesButNotKeys() {
        val fixture = boardFixture()
        val replaced = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeNameReplacer(fixture.board).replace(
                parse("<< A: A >>;"),
            ),
        ).value
        val property = nodeChild(expression(replaced), 0)
        val key = assertIs<JessieCodeAstChild.Text>(
            property.children[0],
        )
        val valueCall = nodeChild(property, 1)

        assertEquals("A", key.value)
        assertEquals("op_execfun", operationName(valueCall))
        assertEquals("\$", textValue(nodeChild(valueCall, 0)))
        assertEquals(
            "P1",
            textValue(nodeList(valueCall, 1).single()),
        )
    }

    @Test
    fun directAssignmentTargetsRemainLocalWhenBoardNamesMatch() {
        val fixture = boardFixture()
        val replaced = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeNameReplacer(fixture.board).replace(
                node = parse("A = B;"),
                forceValueCall = false,
            ),
        ).value
        val assignment = expression(replaced)
        val target = nodeChild(assignment, 0)
        val value = nodeChild(assignment, 1)

        assertEquals(JessieCodeAstNodeType.VARIABLE, target.type)
        assertEquals("A", textValue(target))
        assertEquals("op_execfun", operationName(value))
        assertEquals("P2", textValue(nodeList(value, 1).single()))

        val dependencies = assertIs<
            GMResult.Ok<Map<String, GeometryElement>>
            >(
            JessieCodeDependencyCollector(fixture.board).collect(
                replaced,
            ),
        ).value
        assertEquals(listOf("P2", "P1"), dependencies.keys.toList())
    }

    @Test
    fun idReplacementMatchesOfficialCurrentNameRoundTrip() {
        val fixture = boardFixture()
        val replacer = JessieCodeNameReplacer(fixture.board)
        val stable = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            replacer.replace(parse("A + sin(B);")),
        ).value

        fixture.first.setName("RenamedA")
        fixture.second.setName("RenamedB")
        val restored = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            replacer.replaceIds(stable),
        ).value
        val restoredExpression = expression(restored)
        val point = nodeChild(restoredExpression, 0)
        val slider = nodeList(
            nodeChild(restoredExpression, 1),
            1,
        ).single()

        assertEquals(JessieCodeAstNodeType.VARIABLE, point.type)
        assertEquals("RenamedA", textValue(point))
        assertTrue(point.children.isEmpty())
        assertFalse(point.replaced)
        assertEquals(JessieCodeAstNodeType.VARIABLE, slider.type)
        assertEquals("RenamedB", textValue(slider))
        assertTrue(slider.children.isEmpty())
        assertFalse(slider.replaced)
    }

    @Test
    fun idReplacementKeepsStableCallsForEmptyOrMissingNames() {
        val unnamedFixture = boardFixture()
        val unnamedReplacer = JessieCodeNameReplacer(
            unnamedFixture.board,
        )
        val unnamedStable = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            unnamedReplacer.replace(parse("A;")),
        ).value
        unnamedFixture.first.setName("")

        assertEquals(
            unnamedStable,
            assertIs<GMResult.Ok<JessieCodeAstNode>>(
                unnamedReplacer.replaceIds(unnamedStable),
            ).value,
        )

        val missingFixture = boardFixture()
        val missingReplacer = JessieCodeNameReplacer(
            missingFixture.board,
        )
        val missingStable = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            missingReplacer.replace(parse("A;")),
        ).value
        missingFixture.board.removeObject(missingFixture.first)

        assertEquals(
            missingStable,
            assertIs<GMResult.Ok<JessieCodeAstNode>>(
                missingReplacer.replaceIds(missingStable),
            ).value,
        )
    }

    @Test
    fun idReplacementPreservesOfficialReverseChildTraversal() {
        val fixture = boardFixture()
        val stable = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeNameReplacer(fixture.board).replace(
                parse("A + B;"),
            ),
        ).value
        val rightCall = nodeChild(expression(stable), 1)
        val error = assertIs<
            GMResult.Err<JessieCodeNameReplacementError>
            >(
            JessieCodeNameReplacer(
                board = fixture.board,
                limits = JessieCodeNameReplacementLimits(
                    maxVisitedNodes = 2,
                ),
            ).replaceIds(stable),
        ).error
        val nodeLimit = assertIs<
            JessieCodeNameReplacementError.NodeLimitExceeded
            >(error)

        assertEquals(rightCall.location, nodeLimit.location)
    }

    @Test
    fun idReplacementRejectsMalformedReplacementNodes() {
        val fixture = boardFixture()
        val stable = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeNameReplacer(fixture.board).replace(parse("A;")),
        ).value
        val malformed = expression(stable).copy(children = emptyList())
        val error = assertIs<
            GMResult.Err<JessieCodeNameReplacementError>
            >(
            JessieCodeNameReplacer(fixture.board).replaceIds(malformed),
        ).error

        assertIs<
            JessieCodeNameReplacementError.InvalidReplacedNode
            >(error)
    }

    @Test
    fun wholeSliderExpressionUsesValueCallUnlessDisabled() {
        val fixture = boardFixture()
        val ast = parse("B;")

        val valueCall = expression(
            assertIs<GMResult.Ok<JessieCodeAstNode>>(
                JessieCodeNameReplacer(fixture.board).replace(ast),
            ).value,
        )
        assertEquals("\$value", textValue(nodeChild(valueCall, 0)))

        val objectCall = expression(
            assertIs<GMResult.Ok<JessieCodeAstNode>>(
                JessieCodeNameReplacer(fixture.board).replace(
                    node = ast,
                    forceValueCall = false,
                ),
            ).value,
        )
        assertEquals("\$", textValue(nodeChild(objectCall, 0)))
    }

    @Test
    fun boundNamesAreNotReplaced() {
        val fixture = boardFixture()
        val replaced = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeNameReplacer(fixture.board).replace(
                node = parse("A + B;"),
                boundNames = setOf("A"),
            ),
        ).value
        val expression = expression(replaced)

        assertEquals(JessieCodeAstNodeType.VARIABLE, nodeChild(expression, 0).type)
        assertEquals("A", textValue(nodeChild(expression, 0)))
        assertEquals(
            "\$",
            textValue(nodeChild(nodeChild(expression, 1), 0)),
        )
    }

    @Test
    fun functionParametersRemainBoundDuringNameReplacement() {
        val fixture = boardFixture()
        val replaced = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeNameReplacer(fixture.board).replace(
                node = parse(
                    "function (A) { return A + B; };",
                ),
            ),
        ).value
        val function = expression(replaced)
        assertEquals(
            listOf("A"),
            assertIs<JessieCodeAstChild.TextList>(
                function.children[0],
            ).value,
        )
        val body = nodeChild(function, 1)
        val statementList = nodeChild(body, 0)
        val returnNode = nodeChild(statementList, 1)
        val addition = nodeChild(returnNode, 0)
        val parameter = nodeChild(addition, 0)
        val boardReference = nodeChild(addition, 1)

        assertEquals(JessieCodeAstNodeType.VARIABLE, parameter.type)
        assertEquals("A", textValue(parameter))
        assertEquals("op_execfun", operationName(boardReference))
        assertEquals(
            "P2",
            textValue(nodeList(boardReference, 1).single()),
        )
    }

    @Test
    fun predefinedConstantsAreNotReplacedBySameNamedElements() {
        val fixture = boardFixture()
        registerElement(
            board = fixture.board,
            id = "P3",
            name = "PI",
            type = Const.OBJECT_TYPE_POINT,
            elType = "point",
        )
        val replaced = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeNameReplacer(fixture.board).replace(
                node = parse("PI + A;"),
            ),
        ).value
        val expression = expression(replaced)

        assertEquals(JessieCodeAstNodeType.VARIABLE, nodeChild(expression, 0).type)
        assertEquals("PI", textValue(nodeChild(expression, 0)))
        assertEquals(
            "\$",
            textValue(nodeChild(nodeChild(expression, 1), 0)),
        )
    }

    @Test
    fun suppliedFunctionsTakePrecedenceOverSameNamedElements() {
        val fixture = boardFixture()
        val function = assertIs<
            GMResult.Ok<JessieCodeExpressionFunction>
            >(
            JessieCodeExpressionFunction.compile(
                source = "A()",
                board = fixture.board,
                functions = mapOf(
                    "A" to JessieCodeCallable { _, _ ->
                        GMResult.Ok(
                            JessieCodeRuntimeValue.NumberValue(9.0),
                        )
                    },
                ),
            ),
        ).value

        assertTrue(function.dependencies.isEmpty())
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(9.0),
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
                function.evaluate(),
            ).value,
        )
    }

    @Test
    fun compiledExpressionBindsArgumentsAndSurvivesElementRename() {
        val fixture = boardFixture()
        val function = compile(
            source = "A.X() + x",
            board = fixture.board,
            variableNames = listOf("x"),
            elementRuntime = elementRuntime(
                point = fixture.first,
                slider = fixture.second,
            ),
        )

        assertEquals("A.X() + x", function.origin)
        assertEquals(listOf("x"), function.variableNames)
        assertEquals(listOf("P1"), function.dependencies.keys.toList())

        fixture.first.setName("Renamed")
        val value = assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            function.evaluate(
                listOf(JessieCodeRuntimeValue.NumberValue(4.0)),
            ),
        ).value
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(7.0),
            value,
        )
    }

    @Test
    fun compiledAssignmentUsesAFunctionLocalEvenWhenBoardNameMatches() {
        val fixture = boardFixture()
        val function = assertIs<
            GMResult.Ok<JessieCodeExpressionFunction>
            >(
            JessieCodeExpressionFunction.compile(
                source = "A = 1",
                board = fixture.board,
            ),
        ).value

        assertEquals(listOf("P1"), function.dependencies.keys.toList())
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(1.0),
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
                function.evaluate(),
            ).value,
        )
    }

    @Test
    fun compiledSliderExpressionUsesValueAndTracksDependency() {
        val fixture = boardFixture()
        val function = compile(
            source = "sin(B)",
            board = fixture.board,
            elementRuntime = elementRuntime(
                point = fixture.first,
                slider = fixture.second,
            ),
        )

        assertEquals(listOf("P2"), function.dependencies.keys.toList())
        val value = assertIs<JessieCodeRuntimeValue.NumberValue>(
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
                function.evaluate(),
            ).value,
        )
        assertEquals(1.0, value.value, absoluteTolerance = 1.0e-15)
    }

    @Test
    fun coreElementRuntimeExposesTranslatedCoordinateMethods() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val first = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 2.0),
                name = "A",
            ),
        ).value
        val second = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(4.0, 6.0),
                name = "B",
            ),
        ).value
        val function = assertIs<
            GMResult.Ok<JessieCodeExpressionFunction>
            >(
            JessieCodeExpressionFunction.compile(
                source = "A.X() + A.Y() + A.Dist(B)",
                board = board,
            ),
        ).value

        assertEquals(
            setOf(first.id, second.id),
            function.dependencies.keys,
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(8.0),
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
                function.evaluate(),
            ).value,
        )
    }

    @Test
    fun coreElementRuntimeExposesTranslatedLineAndCircleMethods() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val first = point(board, 0.0, 0.0, "A")
        val second = point(board, 3.0, 4.0, "B")
        val line = assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = board,
                point1 = first,
                point2 = second,
                name = "l",
            ),
        ).value
        val circle = assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = board,
                center = first,
                radius = 2.0,
                name = "c",
            ),
        ).value
        val function = assertIs<
            GMResult.Ok<JessieCodeExpressionFunction>
            >(
            JessieCodeExpressionFunction.compile(
                source =
                    "l.L() + l.Slope() + c.Radius() + c.Area()",
                board = board,
            ),
        ).value

        assertEquals(setOf(line.id, circle.id), function.dependencies.keys)
        val value = assertIs<JessieCodeRuntimeValue.NumberValue>(
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
                function.evaluate(),
            ).value,
        )
        assertEquals(
            5.0 + 4.0 / 3.0 + 2.0 + 4.0 * PI,
            value.value,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun functionalDependenciesCreateUpdateEdgesButNotGeometricParents() {
        val fixture = boardFixture()
        val function = compile(
            source = "A.X()",
            board = fixture.board,
            elementRuntime = elementRuntime(
                point = fixture.first,
                slider = fixture.second,
            ),
        )
        val dependent = registerElement(
            board = fixture.board,
            id = "D1",
            name = "D",
            type = Const.OBJECT_TYPE_CIRCLE,
            elType = "circle",
        )

        dependent.addParentsFromJCFunctions(listOf(function))

        assertSame(
            dependent,
            fixture.first.childElements[dependent.id],
        )
        assertSame(
            fixture.first,
            dependent.ancestors[fixture.first.id],
        )
        assertTrue(dependent.parents.isEmpty())
    }

    @Test
    fun compileFailuresAreStructured() {
        val fixture = boardFixture()
        val parserFailure = assertIs<
            GMResult.Err<JessieCodeExpressionCompileError>
            >(
            JessieCodeExpressionFunction.compile(
                source = "1 +",
                board = fixture.board,
            ),
        ).error
        assertIs<JessieCodeExpressionCompileError.Parser>(
            parserFailure,
        )

        val multipleStatements = assertIs<
            GMResult.Err<JessieCodeExpressionCompileError>
            >(
            JessieCodeExpressionFunction.compile(
                source = "a = 1; a + 2",
                board = fixture.board,
            ),
        ).error
        assertEquals(
            2,
            assertIs<
                JessieCodeExpressionCompileError.MultipleStatements
                >(multipleStatements).statementCount,
        )

        val controlStatement = assertIs<
            GMResult.Err<JessieCodeExpressionCompileError>
            >(
            JessieCodeExpressionFunction.compile(
                source = "if (true) 1",
                board = fixture.board,
            ),
        ).error
        assertEquals(
            "op_if",
            assertIs<
                JessieCodeExpressionCompileError.UnsupportedStatement
                >(controlStatement).operator,
        )

        val dependencyFailure = assertIs<
            GMResult.Err<JessieCodeExpressionCompileError>
            >(
            JessieCodeExpressionFunction.compile(
                source = "\$(\"missing\")",
                board = fixture.board,
            ),
        ).error
        assertIs<JessieCodeExpressionCompileError.Dependency>(
            dependencyFailure,
        )

        val replacementFailure = assertIs<
            GMResult.Err<JessieCodeExpressionCompileError>
            >(
            JessieCodeExpressionFunction.compile(
                source = "A",
                board = fixture.board,
                nameReplacementLimits =
                    JessieCodeNameReplacementLimits(
                        maxVisitedNodes = 0,
                    ),
            ),
        ).error
        assertIs<
            JessieCodeExpressionCompileError.NameReplacement
            >(replacementFailure)
    }

    private fun compile(
        source: String,
        board: Board,
        variableNames: List<String> = emptyList(),
        elementRuntime: JessieCodeElementRuntime,
    ): JessieCodeExpressionFunction =
        assertIs<GMResult.Ok<JessieCodeExpressionFunction>>(
            JessieCodeExpressionFunction.compile(
                source = source,
                board = board,
                variableNames = variableNames,
                elementRuntime = elementRuntime,
            ),
        ).value

    private fun elementRuntime(
        point: GeometryElement,
        slider: GeometryElement,
    ): JessieCodeElementRuntime =
        object : JessieCodeElementRuntime {
            override fun valueOf(
                element: GeometryElement,
                location: JessieCodeAstLocation,
            ): GMResult<
                JessieCodeRuntimeValue,
                JessieCodeRuntimeError,
                > =
                if (element === slider) {
                    GMResult.Ok(
                        JessieCodeRuntimeValue.NumberValue(PI / 2.0),
                    )
                } else {
                    GMResult.Err(
                        JessieCodeRuntimeError.ElementValueUnavailable(
                            elementId = element.id,
                            location = location,
                        ),
                    )
                }

            override fun resolveProperty(
                element: GeometryElement,
                property: String,
                location: JessieCodeAstLocation,
            ): GMResult<
                JessieCodeRuntimeValue,
                JessieCodeRuntimeError,
                > =
                if (element === point && property == "X") {
                    GMResult.Ok(
                        JessieCodeRuntimeValue.FunctionValue(
                            name = "X",
                            callable = JessieCodeCallable { _, _ ->
                                GMResult.Ok(
                                    JessieCodeRuntimeValue.NumberValue(
                                        3.0,
                                    ),
                                )
                            },
                        ),
                    )
                } else {
                    GMResult.Err(
                        JessieCodeRuntimeError
                            .ElementPropertyUnavailable(
                                elementId = element.id,
                                property = property,
                                location = location,
                            ),
                    )
                }
        }

    private fun parse(source: String): JessieCodeAstNode =
        assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value

    private fun expression(program: JessieCodeAstNode): JessieCodeAstNode =
        nodeChild(program, 1)

    private fun nodeChild(
        node: JessieCodeAstNode,
        index: Int,
    ): JessieCodeAstNode =
        assertIs<JessieCodeAstChild.Node>(
            node.children[index],
        ).value

    private fun nodeList(
        node: JessieCodeAstNode,
        index: Int,
    ): List<JessieCodeAstNode> =
        assertIs<JessieCodeAstChild.NodeList>(
            node.children[index],
        ).value

    private fun operationName(node: JessieCodeAstNode): String =
        textValue(node)

    private fun textValue(node: JessieCodeAstNode): String =
        assertIs<JessieCodeAstValue.Text>(node.value).value

    private fun boardFixture(): BoardFixture {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val first = registerElement(
            board = board,
            id = "P1",
            name = "A",
            type = Const.OBJECT_TYPE_POINT,
            elType = "point",
        )
        val second = registerElement(
            board = board,
            id = "P2",
            name = "B",
            type = Const.OBJECT_TYPE_GLIDER,
            elType = "slider",
        )
        return BoardFixture(board, first, second)
    }

    private fun registerElement(
        board: Board,
        id: String,
        name: String,
        type: Int,
        elType: String,
    ): GeometryElement {
        val element = GeometryElement(
            board = board,
            id = id,
            name = name,
            type = type,
            elementClass = Const.OBJECT_CLASS_POINT,
        )
        element.elType = elType
        assertIs<GMResult.Ok<String>>(board.setId(element, "P"))
        return element
    }

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        name: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                name = name,
            ),
        ).value

    private data class BoardFixture(
        val board: Board,
        val first: GeometryElement,
        val second: GeometryElement,
    )
}
