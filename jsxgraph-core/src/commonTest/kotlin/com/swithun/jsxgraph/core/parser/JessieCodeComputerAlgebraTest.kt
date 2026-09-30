package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt

class JessieCodeComputerAlgebraTest {
    @Test
    fun evaluatesOfficialElementaryDerivativeFixtures() {
        val session = JessieCodeSession()

        assertNumber(
            4.0,
            session.parse(
                "x = 2; D(x^2, x);",
                storeSource = false,
            ),
        )
        assertNumber(
            1.0,
            session.parse(
                "x = 0; D(sin(x), x);",
                storeSource = false,
            ),
        )
        assertNumber(
            0.25,
            session.parse(
                "x = 4; D(sqrt(x), x);",
                storeSource = false,
            ),
        )
        assertNumber(
            0.5,
            session.parse(
                "x = 2; D(log(x), x);",
                storeSource = false,
            ),
        )
        assertNumber(
            12.0,
            session.parse(
                "x = 2; D(pow(x, 3), x);",
                storeSource = false,
            ),
        )
        assertNumber(
            1.0,
            session.parse(
                "x = 0; D(tan(x), x);",
                storeSource = false,
            ),
        )
    }

    @Test
    fun expandsNestedAndMapDerivativesBeforeEvaluation() {
        val session = JessieCodeSession()

        assertNumber(
            12.0,
            session.parse(
                "x = 2; D(D(x^3, x), x);",
                storeSource = false,
            ),
        )
        assertNumber(
            12.0,
            session.parse(
                "f = map (x) -> x^3; h = D(f, x); h(2);",
                storeSource = false,
            ),
        )
        assertNumber(
            1.0,
            session.parse(
                "f = map (x) -> sin(x); h = D(f, x); h(0);",
                storeSource = false,
            ),
        )
        assertNumber(
            12.0,
            session.parse(
                "f = map (x) -> D(x^3, x); h = D(f, x); h(2);",
                storeSource = false,
            ),
        )
        assertNumber(
            12.0,
            session.parse(
                "h = D(x^3, x, 2); h(2);",
                storeSource = false,
            ),
        )
    }

    @Test
    fun evaluatesEveryUpstreamElementaryDerivativeBranch() {
        val cases = listOf(
            Triple("abs(x)", 2.0, 1.0),
            Triple("abs(x)", -2.0, -1.0),
            Triple("cot(x)", PI / 4.0, -2.0),
            Triple("exp(x)", 0.0, 1.0),
            Triple("log2(x)", 2.0, 1.0 / (2.0 * ln(2.0))),
            Triple("log10(x)", 10.0, 1.0 / (10.0 * ln(10.0))),
            Triple("asin(x)", 0.5, 1.0 / sqrt(0.75)),
            Triple("acos(x)", 0.5, -1.0 / sqrt(0.75)),
            Triple("atan(x)", 1.0, 0.5),
            Triple("acot(x)", 1.0, -0.5),
            Triple("sinh(x)", 0.0, 1.0),
            Triple("cosh(x)", 0.0, 0.0),
            Triple("tanh(x)", 0.0, 1.0),
            Triple("asinh(x)", 0.0, 1.0),
            Triple("acosh(x)", 2.0, 1.0 / sqrt(3.0)),
            Triple("atanh(x)", 0.5, 1.0 / 0.75),
        )

        for ((expression, x, expected) in cases) {
            assertNumber(
                expected,
                JessieCodeSession().parse(
                    "x = $x; D($expression, x);",
                    storeSource = false,
                ),
            )
        }
    }

    @Test
    fun appliesUpstreamTrivialAndElementarySimplifications() {
        val session = JessieCodeSession()

        assertNumber(
            6.0,
            session.parse(
                "x = 2; D(x*x + 2*x + 1, x);",
                storeSource = false,
            ),
        )
        assertNumber(
            0.0,
            session.parse("sin(PI);", storeSource = false),
        )
        assertNumber(
            -1.0,
            session.parse("cos(PI);", storeSource = false),
        )
        assertNumber(
            1.0,
            session.parse("exp(0);", storeSource = false),
        )
        assertNumber(
            1.0,
            session.parse("pow(123, 0);", storeSource = false),
        )
    }

    @Test
    fun reportsUnknownElementaryDerivativeWithoutThrowing() {
        val result = JessieCodeSession().parse(
            "x = 2; D(unknown(x), x);",
            storeSource = false,
        )
        val sessionError =
            assertIs<GMResult.Err<JessieCodeSessionError>>(result).error
        val algebraError =
            assertIs<JessieCodeSessionError.ComputerAlgebra>(
                sessionError,
            ).error
        val unknown = assertIs<
            JessieCodeComputerAlgebraError.UnknownElementaryDerivative
            >(algebraError)

        assertEquals("unknown", unknown.functionName)
        assertEquals(1, unknown.location.line)
    }

    @Test
    fun boundsDerivativeOrderAndTransformationSteps() {
        val orderResult = JessieCodeSession(
            computerAlgebraLimits =
                JessieCodeComputerAlgebraLimits(
                    maxDerivativeOrder = 1,
                ),
        ).parse(
            "D(x^3, x, 2);",
            storeSource = false,
        )
        val orderError = assertIs<
            JessieCodeSessionError.ComputerAlgebra
            >(
                assertIs<GMResult.Err<JessieCodeSessionError>>(
                    orderResult,
                ).error,
            ).error
        assertIs<
            JessieCodeComputerAlgebraError
                .DerivativeOrderLimitExceeded
            >(orderError)

        val stepResult = JessieCodeSession(
            computerAlgebraLimits =
                JessieCodeComputerAlgebraLimits(
                    maxTransformationSteps = 1,
                ),
        ).parse("1;", storeSource = false)
        val stepError = assertIs<
            JessieCodeSessionError.ComputerAlgebra
            >(
                assertIs<GMResult.Err<JessieCodeSessionError>>(
                    stepResult,
                ).error,
            ).error
        assertIs<
            JessieCodeComputerAlgebraError
                .TransformationStepLimitExceeded
            >(stepError)
    }

    private fun assertNumber(
        expected: Double,
        result: GMResult<
            JessieCodeRuntimeValue,
            JessieCodeSessionError,
            >,
    ) {
        val value = assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            result,
        ).value
        assertEquals(
            expected,
            assertIs<JessieCodeRuntimeValue.NumberValue>(value).value,
            absoluteTolerance = 1e-12,
        )
    }
}
