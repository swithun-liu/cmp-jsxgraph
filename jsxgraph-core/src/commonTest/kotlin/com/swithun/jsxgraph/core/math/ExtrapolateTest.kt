package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExtrapolateTest {
    @Test
    fun defaultsAndSuspendedUpdateFlagMatchOfficialContract() {
        assertEquals(15, Extrapolate.upper)
        assertEquals(1.0e4, Extrapolate.infty)

        val suspendedUpdateFlags = mutableListOf<Boolean>()
        val result = Extrapolate.iteration(
            x0 = 0.0,
            initialStep = 1.0,
            function = ExtrapolateFunction<Nothing> { x, suspendedUpdate ->
                suspendedUpdateFlags += suspendedUpdate
                GMResult.Ok(1.0 + x)
            },
            method = "aitken",
            stepType = 1,
        )

        assertIs<GMResult.Ok<ExtrapolateResult>>(result)
        assertTrue(suspendedUpdateFlags.isNotEmpty())
        assertTrue(suspendedUpdateFlags.all { it })
    }

    @Test
    fun sequenceTransformationsMatchOfficialReferenceValues() {
        val wynnState = mutableListOf<Double>()
        val aitkenState = mutableListOf<Double>()
        val brezinskiState = mutableListOf<Double>()
        val wynnValues = mutableListOf<Double>()
        val aitkenValues = mutableListOf<Double>()
        val brezinskiValues = mutableListOf<Double>()

        for (index in 0..5) {
            val sequenceValue = 1.0 - 2.0.pow(-(index + 1))
            if (index <= 3) {
                wynnValues += assertIs<GMResult.Ok<Double>>(
                    Extrapolate.wynnEps(sequenceValue, index, wynnState),
                ).value
                aitkenValues += assertIs<GMResult.Ok<Double>>(
                    Extrapolate.aitken(sequenceValue, index, aitkenState),
                ).value
            }
            brezinskiValues += assertIs<GMResult.Ok<Double>>(
                Extrapolate.brezinski(sequenceValue, index, brezinskiState),
            ).value
        }

        assertEquals(listOf(0.5, 0.75, 1.0, 1.0), wynnValues)
        assertEquals(listOf(0.5, 0.75, 1.0, 1.0), aitkenValues)
        assertEquals(
            listOf(0.5, 0.75, 0.875, 1.0, 1.0, 1.0),
            brezinskiValues,
        )
    }

    @Test
    fun levinTransformationMatchesOfficialAlternatingHarmonicValues() {
        val numeratorState = mutableListOf<Double>()
        val denominatorState = mutableListOf<Double>()
        val actual = mutableListOf<Double>()
        var partialSum = 0.0

        for (index in 0..7) {
            val sign = if (index % 2 == 0) 1.0 else -1.0
            partialSum += sign / (index + 1)
            actual += assertIs<GMResult.Ok<Double>>(
                Extrapolate.levin(
                    sequenceValue = partialSum,
                    index = index,
                    omega = sign,
                    beta = 1.0,
                    numeratorState = numeratorState,
                    denominatorState = denominatorState,
                ),
            ).value
        }

        val expected = listOf(
            1.0,
            0.75,
            0.6875000000000001,
            0.6936728395061729,
            0.6930852935263371,
            0.6931570788337901,
            0.6931452154206078,
            0.693147626516202,
        )
        for (index in expected.indices) {
            assertEquals(expected[index], actual[index], absoluteTolerance = 1.0e-15)
        }
    }

    @Test
    fun degenerateAndSparseStatePreserveJavaScriptNumericBehavior() {
        val constantState = mutableListOf<Double>()
        Extrapolate.aitken(1.0, 0, constantState)
        Extrapolate.aitken(1.0, 1, constantState)
        val degenerate = assertIs<GMResult.Ok<Double>>(
            Extrapolate.aitken(1.0, 2, constantState),
        )
        assertEquals(1.0e20, degenerate.value)

        val sparseState = mutableListOf<Double>()
        val sparse = assertIs<GMResult.Ok<Double>>(
            Extrapolate.wynnEps(1.0, 2, sparseState),
        )
        assertEquals(3, sparseState.size)
        assertTrue(sparse.value.isNaN())
        assertTrue(sparseState[1].isNaN())
    }

    @Test
    fun extrapolatedIterationsMatchOfficialReferenceValues() {
        val linear = ExtrapolateFunction<Nothing> { x, _ ->
            GMResult.Ok(1.0 + x)
        }

        val wynn = assertIs<GMResult.Ok<ExtrapolateResult>>(
            Extrapolate.iteration(
                x0 = 0.0,
                initialStep = 1.0,
                function = linear,
                method = "wynnEps",
                stepType = 0,
            ),
        ).value
        assertEquals(1.013888895454347, wynn.value, absoluteTolerance = 1.0e-15)
        assertEquals(ExtrapolateClassification.FINITE, wynn.classification)
        assertEquals(0.0, wynn.reliability)

        val aitken = assertIs<GMResult.Ok<ExtrapolateResult>>(
            Extrapolate.iteration(
                x0 = 0.0,
                initialStep = 1.0,
                function = linear,
                method = "aitken",
                stepType = 1,
            ),
        ).value
        assertEquals(1.0, aitken.value, absoluteTolerance = 1.0e-15)
        assertEquals(ExtrapolateClassification.FINITE, aitken.classification)
        assertEquals(0.8, aitken.reliability)

        val brezinski = assertIs<GMResult.Ok<ExtrapolateResult>>(
            Extrapolate.iteration(
                x0 = 0.0,
                initialStep = 1.0,
                function = linear,
                method = "brezinski",
                stepType = 0,
            ),
        ).value
        assertEquals(
            0.9999999999999944,
            brezinski.value,
            absoluteTolerance = 1.0e-15,
        )
        assertEquals(ExtrapolateClassification.FINITE, brezinski.classification)
        assertEquals(0.7333333333333334, brezinski.reliability)
    }

    @Test
    fun levinIterationAndLimitMatchOfficialReferenceValues() {
        val linear = ExtrapolateFunction<Nothing> { x, _ ->
            GMResult.Ok(1.0 + x)
        }
        val finite = assertIs<GMResult.Ok<ExtrapolateResult>>(
            Extrapolate.iterationLevin(
                x0 = 0.0,
                initialStep = 1.0,
                function = linear,
            ),
        ).value
        assertEquals(1.000015951832944, finite.value, absoluteTolerance = 1.0e-15)
        assertEquals(ExtrapolateClassification.FINITE, finite.classification)
        assertEquals(0.0, finite.reliability)

        val reciprocal = assertIs<GMResult.Ok<ExtrapolateResult>>(
            Extrapolate.iterationLevin(
                x0 = 0.0,
                initialStep = 1.0,
                function = ExtrapolateFunction<Nothing> { x, _ ->
                    GMResult.Ok(1.0 / x)
                },
            ),
        ).value
        assertEquals(1.0e20, reciprocal.value)
        assertEquals(
            ExtrapolateClassification.INFINITE,
            reciprocal.classification,
        )
        assertEquals(0.9333333333333333, reciprocal.reliability)

        val logarithm = officialLimit { x -> ln(x) }
        assertEquals(
            -2237.7273428853346,
            logarithm.value,
            absoluteTolerance = 1.0e-9,
        )
        assertEquals(ExtrapolateClassification.FINITE, logarithm.classification)
        assertEquals(0.0, logarithm.reliability)

        val tangent = officialLimit { x -> tan(x - PI * 0.5) }
        assertEquals(
            -11056039.511692017,
            tangent.value,
            absoluteTolerance = 1.0e-6,
        )
        assertEquals(ExtrapolateClassification.INFINITE, tangent.classification)
        assertEquals(0.5333333333333333, tangent.reliability)

        val scaledReciprocal = officialLimit { x -> 4.0 / x }
        assertEquals(
            20000039.99939709,
            scaledReciprocal.value,
            absoluteTolerance = 1.0e-6,
        )
        assertEquals(
            ExtrapolateClassification.INFINITE,
            scaledReciprocal.classification,
        )
        assertEquals(0.9333333333333333, scaledReciprocal.reliability)
    }

    @Test
    fun nanAndFailurePathsAreExplicit() {
        val nanIteration = assertIs<GMResult.Ok<ExtrapolateResult>>(
            Extrapolate.iteration(
                x0 = 0.0,
                initialStep = 1.0,
                function = ExtrapolateFunction<Nothing> { _, _ ->
                    GMResult.Ok(Double.NaN)
                },
                method = "wynnEps",
            ),
        ).value
        assertTrue(nanIteration.value.isNaN())
        assertEquals(ExtrapolateClassification.NAN, nanIteration.classification)
        assertEquals(1.0, nanIteration.reliability)

        val callbackFailure = Extrapolate.limit(
            x0 = 0.0,
            initialStep = 1.0,
            function = ExtrapolateFunction<String> { _, _ ->
                GMResult.Err("rejected")
            },
        )
        val evaluation = assertIs<ExtrapolateError.Evaluation<String>>(
            assertIs<GMResult.Err<ExtrapolateError<String>>>(
                callbackFailure,
            ).error,
        )
        assertEquals("rejected", evaluation.cause)

        val callbackException = Extrapolate.limit(
            x0 = 0.0,
            initialStep = 1.0,
            function = ExtrapolateFunction<Nothing> { _, _ ->
                error("boom")
            },
        )
        assertEquals(
            "boom",
            assertIs<ExtrapolateError.EvaluationException>(
                assertIs<GMResult.Err<ExtrapolateError<Nothing>>>(
                    callbackException,
                ).error,
            ).message,
        )
    }

    @Test
    fun invalidConfigurationReturnsStructuredErrors() {
        assertIs<ExtrapolateError.InvalidIndex>(
            assertIs<GMResult.Err<ExtrapolateError<Nothing>>>(
                Extrapolate.wynnEps(
                    sequenceValue = 1.0,
                    index = -1,
                    state = mutableListOf(),
                ),
            ).error,
        )

        var callbackCount = 0
        val unsupported = Extrapolate.iteration(
            x0 = 0.0,
            initialStep = 1.0,
            function = ExtrapolateFunction<Nothing> { _, _ ->
                callbackCount += 1
                GMResult.Ok(0.0)
            },
            method = "unknown",
        )
        assertIs<ExtrapolateError.UnsupportedMethod>(
            assertIs<GMResult.Err<ExtrapolateError<Nothing>>>(unsupported).error,
        )
        assertEquals(0, callbackCount)

        val originalUpper = Extrapolate.upper
        try {
            Extrapolate.upper = 0
            val invalidUpper = Extrapolate.limit(
                x0 = 0.0,
                initialStep = 1.0,
                function = ExtrapolateFunction<Nothing> { _, _ ->
                    GMResult.Ok(0.0)
                },
            )
            assertIs<ExtrapolateError.InvalidUpper>(
                assertIs<GMResult.Err<ExtrapolateError<Nothing>>>(
                    invalidUpper,
                ).error,
            )
        } finally {
            Extrapolate.upper = originalUpper
        }
    }

    private fun officialLimit(
        function: (Double) -> Double,
    ): ExtrapolateResult =
        assertIs<GMResult.Ok<ExtrapolateResult>>(
            Extrapolate.limit(
                x0 = 1.0e-7,
                initialStep = 0.1,
                function = ExtrapolateFunction<Nothing> { x, _ ->
                    GMResult.Ok(function(x))
                },
            ),
        ).value
}
