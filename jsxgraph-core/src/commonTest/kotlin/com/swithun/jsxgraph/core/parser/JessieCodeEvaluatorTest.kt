package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.GeometryElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JessieCodeEvaluatorTest {
    @Test
    fun literalsAndStringEscapesMatchOfficialRuntime() {
        assertEquals("null", describe(evaluate("null;")))
        assertEquals("boolean:true", describe(evaluate("true;")))
        assertEquals(
            "string:anb",
            describe(evaluate("\"a\\nb\";")),
        )
        assertEquals("number:NaN", describe(evaluate("NaN;")))
        assertEquals(
            "number:Infinity",
            describe(evaluate("Infinity;")),
        )
        assertEquals("number:0.0", describe(evaluate("")))
    }

    @Test
    fun scalarAndArrayArithmeticMatchesOfficialRuntime() {
        val fixtures = mapOf(
            "1 + 2 * 3;" to "number:7.0",
            "-2^4;" to "number:-16.0",
            "2^-3;" to "number:0.125",
            "-5 % 3;" to "number:1.0",
            "5 % -3;" to "number:-1.0",
            "[1,2]+[3,4,5];" to
                "array:[number:4.0,number:6.0]",
            "[1,\"x\"]+[2,3];" to
                "array:[number:3.0,string:x3]",
            "[1,2]-[3];" to "array:[number:-2.0]",
            "-[1,\"2\"];" to
                "array:[number:-1.0,number:-2.0]",
            "[1,2]*[3,4];" to "number:11.0",
            "2*[3,4];" to
                "array:[number:6.0,number:8.0]",
            "[3,4]*2;" to "number:4.0",
            "[6,8]/2;" to
                "array:[number:3.0,number:4.0]",
            "[-5,5]%3;" to
                "array:[number:1.0,number:2.0]",
            "\"hello\"+2;" to "string:hello2",
            "true+\"x\";" to "string:truex",
            "[1,2]+\"x\";" to "string:1,2x",
        )

        for ((source, expected) in fixtures) {
            assertEquals(expected, describe(evaluate(source)), source)
        }
    }

    @Test
    fun equalityComparisonAndApproximationMatchJavaScriptSemantics() {
        val fixtures = mapOf(
            "null == missing;" to true,
            "\"2\" == 2;" to true,
            "false == 0;" to true,
            "[] == 0;" to true,
            "[1] == 1;" to true,
            "[1,2] == \"1,2\";" to true,
            "NaN == NaN;" to false,
            "\"10\" < 2;" to false,
            "\"10\" < \"2\";" to true,
            "null <= 0;" to true,
            "missing > 0;" to false,
            "-0 < 0;" to false,
            "-0 <= 0;" to true,
            "0 <= -0;" to true,
            "\"0x10\" == 16;" to true,
            "\"+0x10\" == 16;" to false,
            "1 ~= 1.0000005;" to true,
            "1 ~= 1.000002;" to false,
        )

        for ((source, expected) in fixtures) {
            assertEquals(
                JessieCodeRuntimeValue.BooleanValue(expected),
                evaluate(source),
                source,
            )
        }
    }

    @Test
    fun logicalAndConditionalOperatorsShortCircuitAndReturnOperands() {
        val fixtures = mapOf(
            "false || 7;" to "number:7.0",
            "true || missing();" to "boolean:true",
            "true && 8;" to "number:8.0",
            "false && missing();" to "boolean:false",
            "!0;" to "boolean:true",
            "!\"\";" to "boolean:true",
            "![];" to "boolean:false",
            "true ? 4 : missing();" to "number:4.0",
            "false ? missing() : 5;" to "number:5.0",
        )

        for ((source, expected) in fixtures) {
            assertEquals(expected, describe(evaluate(source)), source)
        }
    }

    @Test
    fun objectLiteralsMatchOfficialPropertySemantics() {
        val empty = assertIs<JessieCodeRuntimeValue.ObjectValue>(
            evaluate("<< >>;"),
        )
        assertTrue(empty.properties.isEmpty())

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(2.0),
            evaluate("<< a: 1, a: 2 >>.a;"),
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            evaluate("<< nested: << ok: true >> >>.nested.ok;"),
        )

        val literalKeys =
            assertIs<JessieCodeRuntimeValue.ObjectValue>(
                evaluate("<< \"b\": 2, 7: \"seven\" >>;"),
            )
        assertEquals(
            setOf("[object Object]"),
            literalKeys.properties.keys,
        )
        assertEquals(
            JessieCodeRuntimeValue.StringValue("seven"),
            literalKeys.properties["[object Object]"],
        )

        val evaluationOrder = mutableListOf<Double>()
        val environment = JessieCodeRuntimeEnvironment(
            functions = mapOf(
                "record" to JessieCodeCallable { arguments, _ ->
                    val value = assertIs<
                        JessieCodeRuntimeValue.NumberValue
                        >(arguments.first())
                    evaluationOrder += value.value
                    GMResult.Ok(value)
                },
            ),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(2.0),
            evaluate(
                "<< a: record(1), a: record(2) >>.a;",
                environment,
            ),
        )
        assertEquals(listOf(1.0, 2.0), evaluationOrder)
    }

    @Test
    fun assignmentsAndStatementListsMatchOfficialRuntime() {
        val fixtures = mapOf(
            "a = 1; a + 2;" to "number:3.0",
            "a = b = 3; a + b;" to "number:6.0",
            "a = [1, 2]; a[0] = 7; a[0] + a[1];" to
                "number:9.0",
            "a = << x: 1 >>; a.x = 9; a.x;" to "number:9.0",
            "a = []; a[2] = 7; " +
                "[a.length, a[0], a[1], a[2]];" to
                "array:[number:3.0,undefined,undefined,number:7.0]",
            "a = [1, 2, 3]; a.length = 1; a.length;" to
                "number:1.0",
            "a = []; a[-1] = 4; a[-1];" to "number:4.0",
            "a = []; a[1.2] = 4; a[1.2];" to "undefined",
        )

        for ((source, expected) in fixtures) {
            assertEquals(expected, describe(evaluate(source)), source)
        }
    }

    @Test
    fun assignmentResolvesTargetBeforeEvaluatingValue() {
        val calls = mutableListOf<String>()
        val box = JessieCodeRuntimeValue.ObjectValue(emptyMap())
        val environment = JessieCodeRuntimeEnvironment(
            functions = mapOf(
                "target" to JessieCodeCallable { _, _ ->
                    calls += "target"
                    GMResult.Ok(box)
                },
                "value" to JessieCodeCallable { _, _ ->
                    calls += "value"
                    GMResult.Ok(
                        JessieCodeRuntimeValue.NumberValue(7.0),
                    )
                },
            ),
        )

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(7.0),
            evaluate(
                "target().x = value(); target().x;",
                environment,
            ),
        )
        assertEquals(listOf("target", "value", "target"), calls)
    }

    @Test
    fun ifStatementsAndBlocksMatchOfficialRuntime() {
        val fixtures = mapOf(
            "if (true) 7;" to "number:7.0",
            "if (false) 7;" to "number:0.0",
            "if (false) 7; else 9;" to "number:9.0",
            "if (true) { 1; 2; }" to "number:2.0",
            "if (true) if (false) 1; else 2;" to "number:2.0",
            "a = 1; if (true) { a = 2; } a;" to "number:2.0",
            "if (false) { missing(); } 4;" to "number:4.0",
            ";" to "number:0.0",
        )

        for ((source, expected) in fixtures) {
            assertEquals(expected, describe(evaluate(source)), source)
        }
    }

    @Test
    fun loopStatementsMatchOfficialRuntime() {
        val fixtures = mapOf(
            "a = 0; while (a < 3) a = a + 1; a;" to
                "number:3.0",
            "a = 0; do a = a + 1; while (a < 3); a;" to
                "number:3.0",
            "s = 0; for (i = 0; i < 4; i = i + 1) " +
                "s = s + i; s;" to
                "number:6.0",
            "while (false) missing();" to "number:0.0",
            "for (i = 0; false; missing()) missing();" to
                "number:0.0",
        )

        for ((source, expected) in fixtures) {
            assertEquals(expected, describe(evaluate(source)), source)
        }
    }

    @Test
    fun returnAndDeleteStatementsMatchOfficialRuntime() {
        val returnFixtures = mapOf(
            "return;" to "number:0.0",
            "return 3;" to "number:3.0",
            "return 3; 4;" to "number:4.0",
            "if (true) return 3; 4;" to "number:4.0",
        )
        for ((source, expected) in returnFixtures) {
            assertEquals(expected, describe(evaluate(source)), source)
        }

        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val element = registerElement(
            board = board,
            id = "P1",
            name = "A",
            type = Const.OBJECT_TYPE_POINT,
        )
        assertEquals(
            JessieCodeRuntimeValue.UndefinedValue,
            evaluate(
                source = "delete A",
                environment = JessieCodeRuntimeEnvironment(
                    board = board,
                ),
            ),
        )
        assertEquals(null, board.elementById(element.id))

        val shadowedBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val shadowed = registerElement(
            board = shadowedBoard,
            id = "P2",
            name = "A",
            type = Const.OBJECT_TYPE_POINT,
        )
        evaluate(
            source = "delete A",
            environment = JessieCodeRuntimeEnvironment(
                variables = mapOf(
                    "A" to JessieCodeRuntimeValue.NumberValue(1.0),
                ),
                board = shadowedBoard,
            ),
        )
        assertSame(shadowed, shadowedBoard.elementById(shadowed.id))
    }

    @Test
    fun indexesPropertiesCallsAndMathBuiltInsMatchOfficialRuntime() {
        val add = JessieCodeRuntimeValue.FunctionValue(
            name = "add",
            callable = JessieCodeCallable { arguments, location ->
                val argument = arguments.firstOrNull()
                if (argument !is JessieCodeRuntimeValue.NumberValue) {
                    GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentType(
                            functionName = "add",
                            argumentIndex = 0,
                            expected = "number",
                            actual = argument?.let(::describe)
                                ?: "undefined",
                            location = location,
                        ),
                    )
                } else {
                    GMResult.Ok(
                        JessieCodeRuntimeValue.NumberValue(
                            9.0 + argument.value,
                        ),
                    )
                }
            },
        )
        val environment = JessieCodeRuntimeEnvironment(
            variables = mapOf(
                "box" to JessieCodeRuntimeValue.ObjectValue(
                    mapOf(
                        "value" to
                            JessieCodeRuntimeValue.NumberValue(9.0),
                        "add" to add,
                    ),
                ),
            ),
            functions = mapOf(
                "double" to JessieCodeCallable { arguments, location ->
                    val argument = arguments.firstOrNull()
                    if (argument !is JessieCodeRuntimeValue.NumberValue) {
                        GMResult.Err(
                            JessieCodeRuntimeError.InvalidArgumentType(
                                functionName = "double",
                                argumentIndex = 0,
                                expected = "number",
                                actual = argument?.let(::describe)
                                    ?: "undefined",
                                location = location,
                            ),
                        )
                    } else {
                        GMResult.Ok(
                            JessieCodeRuntimeValue.NumberValue(
                                argument.value * 2.0,
                            ),
                        )
                    }
                },
            ),
        )
        val fixtures = mapOf(
            "[10,20][1];" to "number:20.0",
            "[10,20][1.0000000000005];" to "undefined",
            "[10,20][1.1];" to "undefined",
            "\"abc\"[1];" to "string:b",
            "[1].length;" to "number:1.0",
            "\"abc\".length;" to "number:3.0",
            "double(4);" to "number:8.0",
            "box.value;" to "number:9.0",
            "box.add(2);" to "number:11.0",
            "PI;" to "number:3.141592653589793",
            "EULER;" to "number:2.718281828459045",
            "sin(PI / 2);" to "number:1.0",
            "max(1, 4, -2);" to "number:4.0",
            "pow(2, 5);" to "number:32.0",
        )

        for ((source, expected) in fixtures) {
            assertEquals(
                expected,
                describe(evaluate(source, environment)),
                source,
            )
        }

        val minimum = assertIs<JessieCodeRuntimeValue.NumberValue>(
            evaluate("min(0, -0);"),
        )
        assertEquals((-0.0).toBits(), minimum.value.toBits())
        val maximum = assertIs<JessieCodeRuntimeValue.NumberValue>(
            evaluate("max(-0, 0);"),
        )
        assertEquals(0.0.toBits(), maximum.value.toBits())
    }

    @Test
    fun boardElementsUseTheBoundedRuntimeAdapter() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val point = registerElement(
            board = board,
            id = "P1",
            name = "A",
            type = Const.OBJECT_TYPE_POINT,
        )
        val slider = registerElement(
            board = board,
            id = "P2",
            name = "B",
            type = Const.OBJECT_TYPE_GLIDER,
        )
        val adapter = object : JessieCodeElementRuntime {
            override fun valueOf(
                element: GeometryElement,
                location: JessieCodeAstLocation,
            ): GMResult<
                JessieCodeRuntimeValue,
                JessieCodeRuntimeError,
                > = GMResult.Ok(
                JessieCodeRuntimeValue.NumberValue(
                    if (element === slider) 7.0 else 11.0,
                ),
            )

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
        val environment = JessieCodeRuntimeEnvironment(
            board = board,
            elementRuntime = adapter,
        )

        val direct = assertIs<
            JessieCodeRuntimeValue.ElementReference
            >(evaluate("A;", environment))
        assertSame(point, direct.element)

        val byId = assertIs<
            JessieCodeRuntimeValue.ElementReference
            >(evaluate("\$(\"P1\");", environment))
        assertSame(point, byId.element)

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(7.0),
            evaluate("\$value(\"P2\");", environment),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(3.0),
            evaluate("A.X();", environment),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(8.0),
            evaluate("B + 1;", environment),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(49.0),
            evaluate("B ^ 2;", environment),
        )
    }

    @Test
    fun expectedRuntimeFailuresAreStructured() {
        val nullAddition = evaluateError("null + \"x\";")
        assertIs<JessieCodeRuntimeError.UnsupportedOperation>(
            nullAddition,
        )

        val missingCall = evaluateError("missing();")
        assertEquals(
            "undefined",
            assertIs<JessieCodeRuntimeError.NotCallable>(
                missingCall,
            ).valueType,
        )

        val missingProperty = evaluateError("[1].unknown;")
        assertEquals(
            "unknown",
            assertIs<JessieCodeRuntimeError.UnknownProperty>(
                missingProperty,
            ).property,
        )

        assertIs<JessieCodeRuntimeError.InvalidAssignmentTarget>(
            evaluateError("1 = missing();"),
        )
        assertIs<JessieCodeRuntimeError.AssignmentTargetUnavailable>(
            evaluateError("\"text\".x = 1;"),
        )

        val invalidLimits = evaluatorError(
            source = "1;",
            limits = JessieCodeEvaluatorLimits(
                maxEvaluationSteps = 0,
            ),
        )
        assertIs<JessieCodeRuntimeError.InvalidLimits>(invalidLimits)

        val invalidCollectionLimit = evaluatorError(
            source = "1;",
            limits = JessieCodeEvaluatorLimits(
                maxCollectionSize = 0,
            ),
        )
        assertIs<JessieCodeRuntimeError.InvalidLimits>(
            invalidCollectionLimit,
        )

        val unsafeDepthLimit = evaluatorError(
            source = "1;",
            limits = JessieCodeEvaluatorLimits(
                maxEvaluationDepth = 65,
            ),
        )
        assertIs<JessieCodeRuntimeError.InvalidLimits>(
            unsafeDepthLimit,
        )

        val stepLimit = evaluatorError(
            source = "1 + 2;",
            limits = JessieCodeEvaluatorLimits(
                maxEvaluationSteps = 1,
            ),
        )
        assertIs<
            JessieCodeRuntimeError.EvaluationStepLimitExceeded
            >(stepLimit)

        val objectStepLimit = evaluatorError(
            source = "<< a: 1, b: 2 >>;",
            limits = JessieCodeEvaluatorLimits(
                maxEvaluationSteps = 4,
            ),
        )
        assertIs<
            JessieCodeRuntimeError.EvaluationStepLimitExceeded
            >(objectStepLimit)

        val collectionLimit = evaluatorError(
            source = "a = []; a[5] = 1;",
            limits = JessieCodeEvaluatorLimits(
                maxCollectionSize = 5,
            ),
        )
        assertEquals(
            6L,
            assertIs<
                JessieCodeRuntimeError.CollectionSizeLimitExceeded
                >(collectionLimit).requestedSize,
        )

        val lengthCollectionLimit = evaluatorError(
            source = "a = []; a.length = 4294967295;",
            limits = JessieCodeEvaluatorLimits(
                maxCollectionSize = 5,
            ),
        )
        assertEquals(
            4_294_967_295L,
            assertIs<
                JessieCodeRuntimeError.CollectionSizeLimitExceeded
                >(lengthCollectionLimit).requestedSize,
        )

        assertIs<JessieCodeRuntimeError.AssignmentTargetUnavailable>(
            evaluateError("a = []; a.length = 4294967296;"),
        )

        val loopStepLimit = evaluatorError(
            source = "while (true) ;",
            limits = JessieCodeEvaluatorLimits(
                maxEvaluationSteps = 10,
            ),
        )
        assertIs<
            JessieCodeRuntimeError.EvaluationStepLimitExceeded
            >(loopStepLimit)

        val depthLimit = evaluatorError(
            source = "1 + 2 + 3;",
            limits = JessieCodeEvaluatorLimits(
                maxEvaluationDepth = 2,
            ),
        )
        assertIs<
            JessieCodeRuntimeError.EvaluationDepthLimitExceeded
            >(depthLimit)
    }

    private fun evaluate(
        source: String,
        environment: JessieCodeRuntimeEnvironment =
            JessieCodeRuntimeEnvironment(),
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            JessieCodeEvaluator().evaluate(ast, environment),
        ).value
    }

    private fun evaluateError(
        source: String,
    ): JessieCodeRuntimeError =
        evaluatorError(
            source = source,
            limits = JessieCodeEvaluatorLimits(),
        )

    private fun evaluatorError(
        source: String,
        limits: JessieCodeEvaluatorLimits,
    ): JessieCodeRuntimeError {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Err<JessieCodeRuntimeError>>(
            JessieCodeEvaluator(limits).evaluate(ast),
        ).error
    }

    private fun registerElement(
        board: Board,
        id: String,
        name: String,
        type: Int,
    ): GeometryElement {
        val element = GeometryElement(
            board = board,
            id = id,
            name = name,
            type = type,
            elementClass = Const.OBJECT_CLASS_POINT,
        )
        assertIs<GMResult.Ok<String>>(
            board.setId(element, "P"),
        )
        return element
    }

    private fun describe(value: JessieCodeRuntimeValue): String =
        when (value) {
            JessieCodeRuntimeValue.NullValue -> "null"
            JessieCodeRuntimeValue.UndefinedValue -> "undefined"
            is JessieCodeRuntimeValue.NumberValue -> when {
                value.value.isNaN() -> "number:NaN"
                value.value == Double.POSITIVE_INFINITY ->
                    "number:Infinity"
                value.value == Double.NEGATIVE_INFINITY ->
                    "number:-Infinity"
                else -> "number:${value.value}"
            }
            is JessieCodeRuntimeValue.BooleanValue ->
                "boolean:${value.value}"
            is JessieCodeRuntimeValue.StringValue ->
                "string:${value.value}"
            is JessieCodeRuntimeValue.ArrayValue ->
                value.values.joinToString(
                    prefix = "array:[",
                    postfix = "]",
                    separator = ",",
                    transform = ::describe,
                )
            is JessieCodeRuntimeValue.ObjectValue -> "object"
            is JessieCodeRuntimeValue.FunctionValue ->
                "function:${value.name}"
            is JessieCodeRuntimeValue.BoardReference -> "board"
            is JessieCodeRuntimeValue.ElementReference ->
                "element:${value.element.id}"
        }
}
