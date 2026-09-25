package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NlpTest {
    @Test
    fun unconstrainedQuadraticMatchesOfficialEvaluationPath() {
        val variables = doubleArrayOf(3.0, -4.0)

        val result = Nlp.findMinimum(
            calculation = NlpCalculation { _, _, x, _ ->
                GMResult.Ok(
                    (x[0] - 1.0) * (x[0] - 1.0) +
                        (x[1] + 2.0) * (x[1] + 2.0),
                )
            },
            variableCount = 2,
            constraintCount = 0,
            variables = variables,
            initialTrustRegion = 1.0,
            finalTrustRegion = 1.0e-7,
            maximumEvaluations = 400,
        )

        assertEquals(
            NlpStatus.NORMAL,
            assertIs<GMResult.Ok<NlpStatus>>(result).value,
        )
        assertEquals(0.9999998880066555, variables[0], 1.0e-15)
        assertEquals(-1.999999990526853, variables[1], 1.0e-15)
        assertEquals(66, Nlp.lastNumberOfEvaluations)
    }

    @Test
    fun constrainedQuadraticMatchesOfficialEvaluationPath() {
        val variables = doubleArrayOf(2.0, 2.0)

        val result = Nlp.findMinimum(
            calculation = NlpCalculation { _, _, x, constraints ->
                constraints[0] = x[0] + x[1] - 1.0
                GMResult.Ok(x[0] * x[0] + x[1] * x[1])
            },
            variableCount = 2,
            constraintCount = 1,
            variables = variables,
            initialTrustRegion = 1.0,
            finalTrustRegion = 1.0e-7,
            maximumEvaluations = 400,
        )

        assertEquals(
            NlpStatus.NORMAL,
            assertIs<GMResult.Ok<NlpStatus>>(result).value,
        )
        assertEquals(0.49999991923923603, variables[0], 1.0e-15)
        assertEquals(0.5000000807607639, variables[1], 1.0e-15)
        assertEquals(59, Nlp.lastNumberOfEvaluations)
    }

    @Test
    fun evaluationLimitAndCallbackFailureAreExplicit() {
        val limitedVariables = doubleArrayOf(5.0)
        val limited = Nlp.findMinimum(
            calculation = NlpCalculation { _, _, x, _ ->
                GMResult.Ok(x[0] * x[0])
            },
            variableCount = 1,
            constraintCount = 0,
            variables = limitedVariables,
            initialTrustRegion = 1.0,
            finalTrustRegion = 1.0e-8,
            maximumEvaluations = 1,
        )

        assertEquals(
            NlpStatus.MAX_ITERATIONS_REACHED,
            assertIs<GMResult.Ok<NlpStatus>>(limited).value,
        )
        assertEquals(5.0, limitedVariables[0])
        assertEquals(1, Nlp.lastNumberOfEvaluations)

        val failure = Nlp.findMinimum(
            calculation = NlpCalculation { _, _, _, _ ->
                GMResult.Err("rejected")
            },
            variableCount = 1,
            constraintCount = 0,
            variables = doubleArrayOf(1.0),
            initialTrustRegion = 1.0,
            finalTrustRegion = 1.0e-8,
            maximumEvaluations = 10,
        )

        val error = assertIs<GMResult.Err<NlpError<String>>>(failure).error
        assertEquals(
            "rejected",
            assertIs<NlpError.Evaluation<String>>(error).cause,
        )
    }

    @Test
    fun invalidConfigurationReturnsStructuredErrors() {
        val calculation = NlpCalculation<Nothing> { _, _, _, _ ->
            GMResult.Ok(0.0)
        }

        assertIs<GMResult.Err<NlpError.InvalidVariableCount>>(
            Nlp.findMinimum(
                calculation = calculation,
                variableCount = 0,
                constraintCount = 0,
                variables = doubleArrayOf(),
                initialTrustRegion = 1.0,
                finalTrustRegion = 1.0e-8,
                maximumEvaluations = 10,
            ),
        )
        assertIs<GMResult.Err<NlpError.InvalidTrustRegion>>(
            Nlp.findMinimum(
                calculation = calculation,
                variableCount = 1,
                constraintCount = 0,
                variables = doubleArrayOf(0.0),
                initialTrustRegion = 0.0,
                finalTrustRegion = 1.0e-8,
                maximumEvaluations = 10,
            ),
        )
    }
}
