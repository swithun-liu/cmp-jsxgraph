package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.math.RandomSource
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JessieCodeBuiltInsTest {
    @Test
    fun numericAndAngleBuiltInsMatchOfficialResults() {
        val values = numbers(
            array(
                evaluate(
                    source =
                        "[binomial(5, 2), gcd(84, 30), " +
                            "deg([1, 0], [0, 0], [0, 1]), " +
                            "rad([1, 0], [0, 0], [0, 1])];",
                ),
            ),
        )

        assertContentEquals(
            doubleArrayOf(10.0, 6.0, 90.0, PI / 2.0),
            values,
        )
    }

    @Test
    fun geometryBuiltInsUseTranslatedElementMethods() {
        val values = numbers(
            array(
                evaluate(
                    source =
                        "P = point(1, 2); Q = point(4, 6); " +
                            "l = line(P, Q); c = circle(P, 2); " +
                            "[X(P), Y(P), L(l), Length(l), " +
                            "A(c), Area(c), perimeter(c), Dist(P, Q), " +
                            "Radius(c), Slope(l)];",
                    environment = environment(board()),
                ),
            ),
        )

        assertContentEquals(
            doubleArrayOf(
                1.0,
                2.0,
                5.0,
                5.0,
                4.0 * PI,
                4.0 * PI,
                4.0 * PI,
                5.0,
                2.0,
            ),
            values.copyOfRange(0, 9),
        )
        assertEquals(
            expected = 4.0 / 3.0,
            actual = values[9],
            absoluteTolerance = 1e-15,
        )
    }

    @Test
    fun namesFallBackToIdsAndValueUsesTheElementRuntime() {
        val board = board("builtin-names")
        val names = array(
            evaluate(
                source =
                    "U = point(0, 0); U.name = \"\"; " +
                        "[getName(U), name(U, true)];",
                environment = environment(board),
            ),
        )
        val point = board.objectsList.single()

        assertEquals(
            JessieCodeRuntimeValue.StringValue(""),
            names.values[0],
        )
        assertEquals(
            JessieCodeRuntimeValue.StringValue(point.id),
            names.values[1],
        )

        val valueRuntime = object : JessieCodeElementRuntime {
            override fun valueOf(
                element: GeometryElement,
                location: JessieCodeAstLocation,
            ): GMResult<
                JessieCodeRuntimeValue,
                JessieCodeRuntimeError,
                > = GMResult.Ok(
                JessieCodeRuntimeValue.NumberValue(7.0),
            )

            override fun resolveProperty(
                element: GeometryElement,
                property: String,
                location: JessieCodeAstLocation,
            ): GMResult<
                JessieCodeRuntimeValue,
                JessieCodeRuntimeError,
                > = GMResult.Err(
                JessieCodeRuntimeError.ElementPropertyUnavailable(
                    elementId = element.id,
                    property = property,
                    location = location,
                ),
            )
        }
        val values = array(
            evaluate(
                source =
                    "s = point(0, 0); [V(s), Value(s)];",
                environment = environment(
                    board = board("builtin-values"),
                    elementRuntime = valueRuntime,
                ),
            ),
        )

        assertEquals(
            listOf<JessieCodeRuntimeValue>(
                JessieCodeRuntimeValue.NumberValue(7.0),
                JessieCodeRuntimeValue.NumberValue(7.0),
            ),
            values.values,
        )
    }

    @Test
    fun randintIsInjectableAndIfThenReturnsTheSelectedOperand() {
        var randomCalls = 0
        val values = array(
            evaluate(
                source =
                    "[randint(2, 10, 2), " +
                        "randint(2, 10, null), " +
                        "randint(2, 10, missing), " +
                        "IfThen(0, 1, 2), IfThen(\"x\", 3, 4)];",
                environment = environment(
                    board = board(),
                    randomSource = RandomSource {
                        randomCalls += 1
                        0.5
                    },
                ),
            ),
        )

        assertEquals(3, randomCalls)
        assertEquals(
            listOf<JessieCodeRuntimeValue>(
                JessieCodeRuntimeValue.NumberValue(6.0),
                JessieCodeRuntimeValue.NumberValue(6.0),
                JessieCodeRuntimeValue.NumberValue(6.0),
                JessieCodeRuntimeValue.NumberValue(2.0),
                JessieCodeRuntimeValue.NumberValue(3.0),
            ),
            values.values,
        )
    }

    @Test
    fun evalRecursesThroughArraysAndCallsFunctions() {
        val value = array(
            evaluate(
                source =
                    "f = function () { return 3; }; " +
                        "g = function () { return 4; }; " +
                        "eval([f, [g], 7]);",
            ),
        )

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(3.0),
            value.values[0],
        )
        val nested = array(value.values[1])
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(4.0),
            nested.values.single(),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(7.0),
            value.values[2],
        )
    }

    @Test
    fun evalRejectsCyclesAndExcessiveArrayDepth() {
        val cycle = assertIs<
            JessieCodeRuntimeError.BuiltInInvocationFailure
            >(
            evaluateError(
                source = "a = []; a[0] = a; eval(a);",
            ),
        )
        assertEquals("eval", cycle.functionName)
        assertEquals("cyclic array", cycle.reason)

        var nested: JessieCodeRuntimeValue =
            JessieCodeRuntimeValue.NumberValue(1.0)
        repeat(20) {
            nested = JessieCodeRuntimeValue.ArrayValue(listOf(nested))
        }
        assertIs<JessieCodeRuntimeError.EvaluationDepthLimitExceeded>(
            evaluateError(
                source = "eval(nested);",
                environment = JessieCodeRuntimeEnvironment(
                    variables = mapOf("nested" to nested),
                ),
                limits = JessieCodeEvaluatorLimits(
                    maxEvaluationDepth = 10,
                ),
            ),
        )

        val oversized = JessieCodeRuntimeValue.ArrayValue(
            listOf(
                JessieCodeRuntimeValue.NumberValue(1.0),
                JessieCodeRuntimeValue.NumberValue(2.0),
            ),
        )
        assertIs<JessieCodeRuntimeError.CollectionSizeLimitExceeded>(
            evaluateError(
                source = "eval(oversized);",
                environment = JessieCodeRuntimeEnvironment(
                    variables = mapOf("oversized" to oversized),
                ),
                limits = JessieCodeEvaluatorLimits(
                    maxCollectionSize = 1,
                ),
            ),
        )
    }

    @Test
    fun removeDeletesElementsAndIgnoresOtherValues() {
        val board = board("builtin-remove")
        val value = array(
            evaluate(
                source =
                    "A = point(1, 2); [remove(A), remove(7)];",
                environment = environment(board),
            ),
        )

        assertEquals(0, board.objectsList.size)
        assertNull(board.select("A"))
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            value.values[0],
        )
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            value.values[1],
        )
    }

    @Test
    fun contextDependentMathDefectMatchesOfficialInterpreterBranches() {
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(0.0),
            evaluate("lcm(0, 6);"),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(1.0),
            evaluate("ratpow(-8, 0, 3);"),
        )
        assertTrue(
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                evaluate("ratpow(-8, 1, 0);"),
            ).value.isNaN(),
        )
        assertTrue(
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                evaluate("lcm(\"6\", 0);"),
            ).value.isNaN(),
        )

        for (source in listOf("lcm(21, 6);", "ratpow(-8, 1, 3);")) {
            val error = assertIs<
                JessieCodeRuntimeError.BuiltInInvocationFailure
                >(evaluateError(source))
            assertTrue(
                error.reason.contains("without its required context"),
                source,
            )
        }
    }

    @Test
    fun invalidGeometryArgumentsReturnStructuredFailures() {
        val coordinate = assertIs<
            JessieCodeRuntimeError.InvalidArgumentType
            >(evaluateError("X(1);"))
        assertEquals("X", coordinate.functionName)
        assertEquals("element", coordinate.expected)

        val line = assertIs<JessieCodeRuntimeError.InvalidArgumentType>(
            evaluateError(
                source = "A = point(1, 2); L(A);",
                environment = environment(board()),
            ),
        )
        assertEquals("line", line.expected)

        val circle = assertIs<JessieCodeRuntimeError.InvalidArgumentType>(
            evaluateError(
                source =
                    "A = point(1, 2); B = point(3, 4); " +
                        "l = line(A, B); Area(l);",
                environment = environment(board()),
            ),
        )
        assertEquals("circle", circle.expected)
    }

    private fun evaluate(
        source: String,
        environment: JessieCodeRuntimeEnvironment =
            JessieCodeRuntimeEnvironment(),
        limits: JessieCodeEvaluatorLimits =
            JessieCodeEvaluatorLimits(),
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        val result = JessieCodeEvaluator(limits).evaluate(ast, environment)
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            result,
            result.toString(),
        ).value
    }

    private fun evaluateError(
        source: String,
        environment: JessieCodeRuntimeEnvironment =
            JessieCodeRuntimeEnvironment(),
        limits: JessieCodeEvaluatorLimits =
            JessieCodeEvaluatorLimits(),
    ): JessieCodeRuntimeError {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Err<JessieCodeRuntimeError>>(
            JessieCodeEvaluator(limits).evaluate(ast, environment),
        ).error
    }

    private fun environment(
        board: Board,
        elementRuntime: JessieCodeElementRuntime =
            CoreGeometryElementRuntime,
        randomSource: RandomSource = RandomSource { 0.0 },
    ): JessieCodeRuntimeEnvironment =
        JessieCodeRuntimeEnvironment(
            board = board,
            elementRuntime = elementRuntime,
            randomSource = randomSource,
        )

    private fun numbers(
        value: JessieCodeRuntimeValue.ArrayValue,
    ): DoubleArray = value.values.map {
        assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
    }.toDoubleArray()

    private fun array(
        value: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue.ArrayValue = assertIs(value)

    private fun board(id: String = "builtins"): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = id,
        )
}
