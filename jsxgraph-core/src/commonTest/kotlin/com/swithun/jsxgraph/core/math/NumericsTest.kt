package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NumericsTest {
    @Test
    fun gaussMatchesOfficialReferenceWithoutMutatingInputs() {
        val matrix = arrayOf(
            doubleArrayOf(2.0, 1.0),
            doubleArrayOf(2.0, 3.0),
        )
        val vector = doubleArrayOf(4.0, 5.0)

        val result = Numerics.Gauss(matrix, vector)

        assertContentEquals(
            doubleArrayOf(1.75, 0.5),
            assertIs<GMResult.Ok<DoubleArray>>(result).value,
        )
        assertMatrixEquals(
            arrayOf(doubleArrayOf(2.0, 1.0), doubleArrayOf(2.0, 3.0)),
            matrix,
        )
        assertContentEquals(doubleArrayOf(4.0, 5.0), vector)
    }

    @Test
    fun gaussReportsInvalidAndSingularSystems() {
        assertIs<GMResult.Err<NumericsError.DimensionMismatch>>(
            Numerics.Gauss(
                inputMatrix = arrayOf(doubleArrayOf(1.0, 2.0)),
                inputVector = doubleArrayOf(1.0),
            ),
        )
        assertIs<GMResult.Err<NumericsError.SingularMatrix>>(
            Numerics.Gauss(
                inputMatrix = arrayOf(
                    doubleArrayOf(1.0, 2.0),
                    doubleArrayOf(2.0, 4.0),
                ),
                inputVector = doubleArrayOf(1.0, 2.0),
            ),
        )
    }

    @Test
    fun backwardSolveMatchesOfficialReference() {
        assertContentEquals(
            doubleArrayOf(0.75, 1.5, 2.0),
            Numerics.backwardSolve(
                rightTriangularMatrix = arrayOf(
                    doubleArrayOf(2.0, 1.0, 3.0),
                    doubleArrayOf(0.0, 4.0, 2.0),
                    doubleArrayOf(0.0, 0.0, 5.0),
                ),
                inputVector = doubleArrayOf(9.0, 10.0, 10.0),
            ),
        )
    }

    @Test
    fun determinantsMatchOfficialReference() {
        assertEquals(
            -2.0,
            Numerics.det(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))),
        )
        assertEquals(
            -306.0,
            Numerics.det(
                arrayOf(
                    doubleArrayOf(6.0, 1.0, 1.0),
                    doubleArrayOf(4.0, -2.0, 5.0),
                    doubleArrayOf(2.0, 8.0, 7.0),
                ),
            ),
        )
        assertEquals(0.0, Numerics.det(emptyArray()))
        assertEquals(
            0.0,
            Numerics.det(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(2.0, 4.0))),
        )
    }

    @Test
    fun jacobiMatchesOfficialReference() {
        val result = Numerics.Jacobi(
            arrayOf(
                doubleArrayOf(4.0, 1.0, 1.0),
                doubleArrayOf(1.0, 3.0, 0.0),
                doubleArrayOf(1.0, 0.0, 2.0),
            ),
        )

        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(4.879385241571819, 0.0, 0.0),
                doubleArrayOf(0.0, 2.65270364466614, 0.0),
                doubleArrayOf(0.0, 0.0, 1.4679111137620446),
            ),
            actual = result.diagonalizedMatrix,
            tolerance = 1e-12,
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(0.8440296287459854, -0.29312841385727234, -0.449098785111287),
                doubleArrayOf(0.44909878511128687, 0.8440296287459854, 0.29312841385727223),
                doubleArrayOf(0.2931284138572723, -0.44909878511128676, 0.8440296287459855),
            ),
            actual = result.eigenvectors,
            tolerance = 1e-12,
        )
    }

    @Test
    fun newtonCotesRulesMatchOfficialReferenceValues() {
        val function: (Double) -> Double = { value -> value * value }
        assertEquals(
            2.666666666666666,
            valueOf(Numerics.NewtonCotes(doubleArrayOf(0.0, 2.0), function)),
            absoluteTolerance = 1e-14,
        )
        assertEquals(
            2.668367346938774,
            valueOf(
                Numerics.NewtonCotes(
                    doubleArrayOf(0.0, 2.0),
                    function,
                    NewtonCotesConfig(28, IntegrationType.TRAPEZ),
                ),
            ),
            absoluteTolerance = 1e-14,
        )
        assertEquals(
            2.6666666666666647,
            valueOf(
                Numerics.NewtonCotes(
                    doubleArrayOf(0.0, 2.0),
                    function,
                    NewtonCotesConfig(28, IntegrationType.SIMPSON),
                ),
            ),
            absoluteTolerance = 1e-14,
        )
        assertIs<GMResult.Err<NumericsError.InvalidIntegrationNodeCount>>(
            Numerics.NewtonCotes(
                doubleArrayOf(0.0, 2.0),
                function,
                NewtonCotesConfig(3, IntegrationType.SIMPSON),
            ),
        )
    }

    @Test
    fun rombergUsesDocumentedDefaultsAndMatchesOfficialConfiguredValue() {
        val result = Numerics.Romberg(
            interval = doubleArrayOf(0.0, 2.0),
            function = { value -> value * value },
        )

        assertEquals(2.6666666666666665, valueOf(result), absoluteTolerance = 1e-14)
    }

    @Test
    fun gaussLegendreOrdersMatchOfficialReferenceValues() {
        val function: (Double) -> Double = { value -> value * value }
        for (order in 2..18) {
            val result = Numerics.GaussLegendre(
                interval = doubleArrayOf(0.0, 2.0),
                function = function,
                requestedOrder = order,
            )
            assertEquals(
                expected = 8.0 / 3.0,
                actual = valueOf(result),
                absoluteTolerance = 1e-14,
                message = "Order $order",
            )
        }
        assertEquals(
            valueOf(Numerics.GaussLegendre(doubleArrayOf(0.0, 2.0), function, 18)),
            valueOf(Numerics.GaussLegendre(doubleArrayOf(0.0, 2.0), function, 20)),
        )
        assertIs<GMResult.Err<NumericsError.InvalidQuadratureOrder>>(
            Numerics.GaussLegendre(doubleArrayOf(0.0, 2.0), function, 1),
        )
    }

    @Test
    fun gaussKronrodRulesMatchOfficialReferenceDetails() {
        val interval = doubleArrayOf(0.0, kotlin.math.PI)
        val function = { value: Double -> kotlin.math.sin(value) }
        val expectedDeviations = doubleArrayOf(
            0.8377822123396637,
            0.8333083422586608,
            0.8405260379684187,
        )
        val results = arrayOf(
            gaussKronrodValueOf(Numerics.GaussKronrod15(interval, function)),
            gaussKronrodValueOf(Numerics.GaussKronrod21(interval, function)),
            gaussKronrodValueOf(Numerics.GaussKronrod31(interval, function)),
        )

        for (index in results.indices) {
            assertEquals(2.0, results[index].value)
            assertEquals(
                2.220446049250313e-14,
                results[index].absoluteError,
                absoluteTolerance = 1e-28,
            )
            assertEquals(2.0, results[index].absoluteResult)
            assertEquals(
                expectedDeviations[index],
                results[index].absoluteDeviation,
                absoluteTolerance = 1e-15,
            )
        }
    }

    @Test
    fun adaptiveGaussKronrodAndIntegralAliasMatchOfficialReference() {
        assertEquals(
            expected = 1.0 / 3.0,
            actual = valueOf(
                Numerics.Qag(
                    interval = doubleArrayOf(0.0, 1.0),
                    function = { value -> value * value },
                ),
            ),
        )
        assertEquals(
            expected = 2.0,
            actual = valueOf(
                Numerics.Qag(
                    interval = doubleArrayOf(0.0, kotlin.math.PI),
                    function = { value -> kotlin.math.sin(value) },
                ),
            ),
        )
        assertEquals(
            expected = 0.13768112771232224,
            actual = valueOf(
                Numerics.Qag(
                    interval = doubleArrayOf(0.0, 100.0),
                    function = { value -> kotlin.math.sin(value) },
                ),
            ),
            absoluteTolerance = 1e-14,
        )
        assertEquals(
            expected = 0.13768112771231206,
            actual = valueOf(
                Numerics.Qag(
                    interval = doubleArrayOf(0.0, 100.0),
                    function = { value -> kotlin.math.sin(value) },
                    config = QagConfig(rule = GaussKronrodRule.THIRTY_ONE),
                ),
            ),
            absoluteTolerance = 1e-14,
        )
        assertEquals(
            expected = 2.6666666666666665,
            actual = valueOf(
                Numerics.I(
                    interval = doubleArrayOf(0.0, 2.0),
                    function = { value -> value * value },
                ),
            ),
        )
        assertEquals(
            expected = Double.NEGATIVE_INFINITY,
            actual = valueOf(
                Numerics.Qag(
                    interval = doubleArrayOf(0.0, 100.0),
                    function = { value -> kotlin.math.sin(value) },
                    config = QagConfig(limit = 1),
                ),
            ),
        )
    }

    @Test
    fun adaptiveGaussKronrodReportsInvalidConfiguration() {
        assertIs<GMResult.Err<NumericsError.InvalidInterval>>(
            Numerics.GaussKronrod15(doubleArrayOf(0.0)) { it },
        )
        assertIs<GMResult.Err<NumericsError.InvalidIntegrationLimit>>(
            Numerics.Qag(
                interval = doubleArrayOf(0.0, 1.0),
                function = { it },
                config = QagConfig(limit = 0),
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidIntegrationLimit>>(
            Numerics.Qag(
                interval = doubleArrayOf(0.0, 1.0),
                function = { it },
                config = QagConfig(limit = 1001),
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidIntegrationTolerance>>(
            Numerics.Qag(
                interval = doubleArrayOf(0.0, 1.0),
                function = { it },
                config = QagConfig(
                    epsilonRelative = 0.0,
                    epsilonAbsolute = 0.0,
                ),
            ),
        )
    }

    @Test
    fun naturalSplineMatchesOfficialReferenceAndSortsInputs() {
        val knots = doubleArrayOf(3.0, 1.0, 2.0, 4.0)
        val values = doubleArrayOf(9.0, 1.0, 4.0, 16.0)

        val secondDerivatives = arrayValueOf(Numerics.splineDef(knots, values))

        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), knots)
        assertContentEquals(doubleArrayOf(1.0, 4.0, 9.0, 16.0), values)
        assertContentEquals(doubleArrayOf(0.0, 2.4, 2.4, 0.0), secondDerivatives)
        assertEquals(
            expected = 6.2,
            actual = valueOf(
                Numerics.splineEval(2.5, knots, values, secondDerivatives),
            ),
        )
        assertContentEquals(
            expected = doubleArrayOf(1.0, 4.0, 9.0, 16.0),
            actual = arrayValueOf(
                Numerics.splineEval(
                    evaluationPoints = doubleArrayOf(1.0, 2.0, 3.0, 4.0),
                    knots = knots,
                    values = values,
                    secondDerivatives = secondDerivatives,
                ),
            ),
        )
    }

    @Test
    fun splineDefinitionPreservesOfficialLengthAndTwoPointBehavior() {
        val twoKnots = doubleArrayOf(2.0, 1.0)
        val twoValues = doubleArrayOf(4.0, 1.0)
        assertContentEquals(
            doubleArrayOf(0.0, 0.0),
            arrayValueOf(Numerics.splineDef(twoKnots, twoValues)),
        )
        assertContentEquals(doubleArrayOf(2.0, 1.0), twoKnots)
        assertContentEquals(doubleArrayOf(4.0, 1.0), twoValues)

        val knots = doubleArrayOf(3.0, 1.0, 2.0, 9.0)
        val values = doubleArrayOf(9.0, 1.0, 4.0)
        assertContentEquals(
            doubleArrayOf(0.0, 3.0, 0.0),
            arrayValueOf(Numerics.splineDef(knots, values)),
        )
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0, 9.0), knots)
        assertContentEquals(doubleArrayOf(1.0, 4.0, 9.0), values)
    }

    @Test
    fun splineReportsInvalidDataAndOutOfDomainValues() {
        assertIs<GMResult.Err<NumericsError.InvalidSplineDefinition>>(
            Numerics.splineDef(doubleArrayOf(1.0), doubleArrayOf(2.0)),
        )
        assertIs<GMResult.Err<NumericsError.InvalidSplineEvaluation>>(
            Numerics.splineEval(
                value = 1.5,
                knots = doubleArrayOf(1.0, 2.0, 3.0),
                values = doubleArrayOf(1.0, 4.0, 9.0),
                secondDerivatives = doubleArrayOf(0.0, 0.0),
            ),
        )
        assertIs<GMResult.Err<NumericsError.SplineValueOutOfDomain>>(
            Numerics.splineEval(
                value = 0.5,
                knots = doubleArrayOf(1.0, 2.0, 3.0),
                values = doubleArrayOf(1.0, 2.0, 3.0),
                secondDerivatives = doubleArrayOf(0.0, 0.0, 0.0),
            ),
        )

        val arrayError = assertIs<GMResult.Err<NumericsError.SplineValueOutOfDomain>>(
            Numerics.splineEval(
                evaluationPoints = doubleArrayOf(1.5, 4.0),
                knots = doubleArrayOf(1.0, 2.0, 3.0),
                values = doubleArrayOf(1.0, 2.0, 3.0),
                secondDerivatives = doubleArrayOf(0.0, 0.0, 0.0),
            ),
        )
        assertEquals(1, arrayError.error.index)
        assertEquals(4.0, arrayError.error.value)
    }

    @Test
    fun generalizedDampedNewtonMatchesOfficialTwoDimensionalReference() {
        val initialValues = doubleArrayOf(1.0, 1.0)
        val result = dampedNewtonValueOf(
            Numerics.generalizedDampedNewton(
                function = { parameters, _ ->
                    doubleArrayOf(
                        parameters[0] * parameters[0] + parameters[1] - 3.0,
                        parameters[0] + parameters[1] * parameters[1] - 3.0,
                    )
                },
                jacobian = { parameters, _ ->
                    arrayOf(
                        doubleArrayOf(2.0 * parameters[0], 1.0),
                        doubleArrayOf(1.0, 2.0 * parameters[1]),
                    )
                },
                dimension = 2,
                initialValues = initialValues,
                damping = 0.85,
                epsilon = 1e-18,
                maxSteps = 40,
            ),
        )

        assertContentEquals(
            expected = doubleArrayOf(1.3027756376239115, 1.3027756376239115),
            actual = result.parameters,
        )
        assertEquals(3.037312195830457e-19, result.squaredResidual)
        assertContentEquals(doubleArrayOf(1.0, 1.0), initialValues)
    }

    @Test
    fun generalizedDampedNewtonMatchesOfficialArbitraryDimensionReference() {
        val function = { parameters: DoubleArray, _: Int ->
            doubleArrayOf(
                parameters[0] - 1.0,
                2.0 * parameters[1] - 4.0,
                3.0 * parameters[2] - 9.0,
            )
        }
        val jacobian = { _: DoubleArray, _: Int ->
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 2.0, 0.0),
                doubleArrayOf(0.0, 0.0, 3.0),
            )
        }
        val result = dampedNewtonValueOf(
            Numerics.generalizedDampedNewton(
                function = function,
                jacobian = jacobian,
                dimension = 3,
                initialValues = doubleArrayOf(0.0, 0.0, 0.0),
                damping = 0.85,
                epsilon = 1e-18,
                maxSteps = 0,
            ),
        )

        assertContentEquals(
            expected = doubleArrayOf(
                0.999999999980538,
                1.999999999961076,
                2.9999999999416143,
            ),
            actual = result.parameters,
        )
        assertEquals(3.7119280338439306e-20, result.squaredResidual)
    }

    @Test
    fun generalizedDampedNewtonReportsMalformedAndSingularSystems() {
        val constantFunction = { _: DoubleArray, dimension: Int ->
            DoubleArray(dimension) { 1.0 }
        }
        val identityJacobian = { _: DoubleArray, dimension: Int ->
            Mat.identity(dimension)
        }

        assertIs<GMResult.Err<NumericsError.InvalidSystemDimension>>(
            Numerics.generalizedDampedNewton(
                function = constantFunction,
                jacobian = identityJacobian,
                dimension = 3,
                initialValues = doubleArrayOf(0.0, 0.0),
                damping = 1.0,
                epsilon = 1e-12,
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidFunctionResult>>(
            Numerics.generalizedDampedNewton(
                function = { _, _ -> doubleArrayOf(1.0) },
                jacobian = identityJacobian,
                dimension = 2,
                initialValues = doubleArrayOf(0.0, 0.0),
                damping = 1.0,
                epsilon = 1e-12,
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidJacobian>>(
            Numerics.generalizedDampedNewton(
                function = constantFunction,
                jacobian = { _, _ -> arrayOf(doubleArrayOf(1.0, 0.0)) },
                dimension = 2,
                initialValues = doubleArrayOf(0.0, 0.0),
                damping = 1.0,
                epsilon = 1e-12,
            ),
        )
        assertIs<GMResult.Err<NumericsError.SingularMatrix>>(
            Numerics.generalizedDampedNewton(
                function = constantFunction,
                jacobian = { _, _ ->
                    arrayOf(
                        doubleArrayOf(1.0, 2.0),
                        doubleArrayOf(2.0, 4.0),
                    )
                },
                dimension = 2,
                initialValues = doubleArrayOf(0.0, 0.0),
                damping = 1.0,
                epsilon = 1e-12,
            ),
        )
    }

    @Test
    fun predefinedRungeKuttaMethodsMatchOfficialReferenceValues() {
        val growth = { _: Double, state: DoubleArray -> doubleArrayOf(state[0]) }
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(1.0),
                doubleArrayOf(1.5),
                doubleArrayOf(2.25),
                doubleArrayOf(3.375),
                doubleArrayOf(5.0625),
            ),
            actual = trajectoryValueOf(
                Numerics.rungeKutta(
                    method = RungeKuttaMethod.EULER,
                    initialValues = doubleArrayOf(1.0),
                    interval = doubleArrayOf(0.0, 2.0),
                    stepCount = 4,
                    function = growth,
                ),
            ),
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(1.0),
                doubleArrayOf(1.625),
                doubleArrayOf(2.640625),
                doubleArrayOf(4.291015625),
                doubleArrayOf(6.972900390625),
            ),
            actual = trajectoryValueOf(
                Numerics.rungeKutta(
                    method = RungeKuttaMethod.HEUN,
                    initialValues = doubleArrayOf(1.0),
                    interval = doubleArrayOf(0.0, 2.0),
                    stepCount = 4,
                    function = growth,
                ),
            ),
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(1.0),
                doubleArrayOf(1.6484375),
                doubleArrayOf(2.71734619140625),
                doubleArrayOf(4.47937536239624),
                doubleArrayOf(7.383970323950052),
            ),
            actual = trajectoryValueOf(
                Numerics.rungeKutta(
                    method = RungeKuttaMethod.RK4,
                    initialValues = doubleArrayOf(1.0),
                    interval = doubleArrayOf(0.0, 2.0),
                    stepCount = 4,
                    function = growth,
                ),
            ),
        )
    }

    @Test
    fun rungeKuttaSupportsSystemsCustomTableausAndOfficialFallback() {
        val initialValues = doubleArrayOf(1.0, 0.0)
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.8776041666666666, -0.47916666666666663),
                doubleArrayOf(0.54058837890625, -0.8410373263888888),
            ),
            actual = trajectoryValueOf(
                Numerics.rungeKutta(
                    methodName = "rk4",
                    initialValues = initialValues,
                    interval = doubleArrayOf(0.0, 1.0),
                    stepCount = 2,
                    function = { _, state -> doubleArrayOf(state[1], -state[0]) },
                ),
            ),
        )
        assertContentEquals(doubleArrayOf(1.0, 0.0), initialValues)

        var evaluationCount = 0
        val customResult = trajectoryValueOf(
            Numerics.rungeKutta(
                tableau = ButcherTableau(
                    stageCount = 1,
                    coefficients = arrayOf(doubleArrayOf(0.0)),
                    weights = doubleArrayOf(1.0),
                    nodes = doubleArrayOf(0.0),
                ),
                initialValues = doubleArrayOf(1.0),
                interval = doubleArrayOf(0.0, 1.0),
                stepCount = 2,
                function = { _, state ->
                    evaluationCount += 1
                    doubleArrayOf(state[0])
                },
            ),
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(1.0),
                doubleArrayOf(1.5),
                doubleArrayOf(2.25),
            ),
            actual = customResult,
        )
        assertEquals(3, evaluationCount)

        assertMatrixEquals(
            expected = customResult,
            actual = trajectoryValueOf(
                Numerics.rungeKutta(
                    methodName = "unknown",
                    initialValues = doubleArrayOf(1.0),
                    interval = doubleArrayOf(0.0, 1.0),
                    stepCount = 2,
                    function = { _, state -> doubleArrayOf(state[0]) },
                ),
            ),
        )
    }

    @Test
    fun rungeKuttaReportsInvalidInputs() {
        val function = { _: Double, state: DoubleArray -> state.copyOf() }
        assertIs<GMResult.Err<NumericsError.InvalidInterval>>(
            Numerics.rungeKutta(
                method = RungeKuttaMethod.EULER,
                initialValues = doubleArrayOf(1.0),
                interval = doubleArrayOf(0.0),
                stepCount = 1,
                function = function,
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidRungeKuttaStepCount>>(
            Numerics.rungeKutta(
                method = RungeKuttaMethod.EULER,
                initialValues = doubleArrayOf(1.0),
                interval = doubleArrayOf(0.0, 1.0),
                stepCount = 0,
                function = function,
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidButcherTableau>>(
            Numerics.rungeKutta(
                tableau = ButcherTableau(
                    stageCount = 2,
                    coefficients = arrayOf(doubleArrayOf(0.0)),
                    weights = doubleArrayOf(1.0, 1.0),
                    nodes = doubleArrayOf(0.0, 1.0),
                ),
                initialValues = doubleArrayOf(1.0),
                interval = doubleArrayOf(0.0, 1.0),
                stepCount = 1,
                function = function,
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidFunctionResult>>(
            Numerics.rungeKutta(
                method = RungeKuttaMethod.EULER,
                initialValues = doubleArrayOf(1.0, 2.0),
                interval = doubleArrayOf(0.0, 1.0),
                stepCount = 1,
                function = { _, _ -> doubleArrayOf(1.0) },
            ),
        )
    }

    @Test
    fun derivativeAndNewtonMatchOfficialReferenceValues() {
        assertEquals(
            expected = 12.00000000021184,
            actual = Numerics.D { value -> value * value * value }(2.0),
            absoluteTolerance = 1e-12,
        )
        assertEquals(
            expected = 1.9999999999868976,
            actual = Numerics.Newton({ value -> value - 2.0 }, 0.0),
            absoluteTolerance = 1e-14,
        )
        assertEquals(
            expected = -2.0000000000000178,
            actual = Numerics.Newton(
                function = { value -> value * value - 4.0 },
                initialValue = 0.0,
                random = RandomSource { 0.25 },
            ),
            absoluteTolerance = 1e-14,
        )
    }

    @Test
    fun bracketAndBrentRootMatchOfficialReferenceValues() {
        assertContentEquals(
            expected = doubleArrayOf(0.0, -2.0, 2.0, 0.0),
            actual = Numerics.findBracket({ value -> value - 2.0 }, 0.0),
        )
        assertEquals(
            expected = 2.0,
            actual = Numerics.fzero({ value -> value - 2.0 }, 0.0),
        )

        val function = { value: Double -> value * value * value - value - 2.0 }
        assertEquals(
            expected = 1.5213796959100443,
            actual = valueOf(Numerics.fzero(function, doubleArrayOf(1.0, 2.0))),
            absoluteTolerance = 1e-14,
        )
    }

    @Test
    fun chandrupatlaAndRootMatchOfficialReferenceValues() {
        val fixedRandom = RandomSource { 0.25 }
        assertEquals(
            expected = 5.0,
            actual = Numerics.chandrupatla(
                function = { value -> (value - 5.0) * (value - 5.0) },
                initialValue = 0.0,
                random = fixedRandom,
            ),
        )

        val function = { value: Double -> value * value * value - value - 2.0 }
        assertEquals(
            expected = 1.5213797122439434,
            actual = valueOf(
                Numerics.chandrupatla(
                    function = function,
                    interval = doubleArrayOf(1.0, 2.0),
                    random = fixedRandom,
                ),
            ),
            absoluteTolerance = 1e-14,
        )
        assertEquals(
            expected = 1.5213797122439434,
            actual = valueOf(
                Numerics.root(
                    function = function,
                    interval = doubleArrayOf(1.0, 2.0),
                    random = fixedRandom,
                ),
            ),
            absoluteTolerance = 1e-14,
        )
        assertEquals(
            expected = 0.9999999999999999,
            actual = valueOf(
                Numerics.root(
                    function = { value -> (value - 1.0) * (value - 1.0) },
                    interval = doubleArrayOf(0.0, 4.0),
                    random = fixedRandom,
                ),
            ),
            absoluteTolerance = 1e-14,
        )
    }

    @Test
    fun domainAndMinimumMatchOfficialReferenceValues() {
        val squareRootDomain = { value: Double -> kotlin.math.sqrt(value) }
        assertContentEquals(
            expected = doubleArrayOf(-0.00020428174445492973, 5.0),
            actual = arrayValueOf(
                Numerics.findDomain(squareRootDomain, doubleArrayOf(-5.0, 5.0)),
            ),
        )
        assertContentEquals(
            expected = doubleArrayOf(0.00020428174562965915, 5.0),
            actual = arrayValueOf(
                Numerics.findDomain(
                    function = squareRootDomain,
                    interval = doubleArrayOf(-5.0, 5.0),
                    outer = false,
                ),
            ),
        )
        assertEquals(
            expected = 1.5,
            actual = valueOf(
                Numerics.fminbr(
                    function = { value -> (value - 1.5) * (value - 1.5) + 2.0 },
                    interval = doubleArrayOf(-4.0, 7.0),
                ),
            ),
        )
        assertEquals(
            expected = 2.0,
            actual = valueOf(
                Numerics.fminbr(
                    function = { value ->
                        if (value < 0.0) Double.NaN else (value - 2.0) * (value - 2.0)
                    },
                    interval = doubleArrayOf(-5.0, 8.0),
                ),
            ),
        )
    }

    @Test
    fun rootAndMinimumIntervalsReportInvalidInput() {
        val invalidInterval = doubleArrayOf(1.0)
        val function = { value: Double -> value }

        assertIs<GMResult.Err<NumericsError.InvalidInterval>>(
            Numerics.findDomain(function, invalidInterval),
        )
        assertIs<GMResult.Err<NumericsError.InvalidInterval>>(
            Numerics.fminbr(function, invalidInterval),
        )
        assertIs<GMResult.Err<NumericsError.InvalidInterval>>(
            Numerics.fzero(function, invalidInterval),
        )
        assertIs<GMResult.Err<NumericsError.InvalidInterval>>(
            Numerics.chandrupatla(function, invalidInterval),
        )
        assertIs<GMResult.Err<NumericsError.InvalidInterval>>(
            Numerics.root(function, invalidInterval),
        )
    }

    private fun valueOf(result: GMResult<Double, NumericsError>): Double =
        assertIs<GMResult.Ok<Double>>(result).value

    private fun arrayValueOf(
        result: GMResult<DoubleArray, NumericsError>,
    ): DoubleArray = assertIs<GMResult.Ok<DoubleArray>>(result).value

    private fun dampedNewtonValueOf(
        result: GMResult<DampedNewtonResult, NumericsError>,
    ): DampedNewtonResult = assertIs<GMResult.Ok<DampedNewtonResult>>(result).value

    private fun trajectoryValueOf(
        result: GMResult<Array<DoubleArray>, NumericsError>,
    ): Array<DoubleArray> = assertIs<GMResult.Ok<Array<DoubleArray>>>(result).value

    private fun gaussKronrodValueOf(
        result: GMResult<GaussKronrodResult, NumericsError>,
    ): GaussKronrodResult = assertIs<GMResult.Ok<GaussKronrodResult>>(result).value

    private fun assertMatrixEquals(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
        tolerance: Double = 0.0,
    ) {
        assertEquals(expected.size, actual.size)
        for (row in expected.indices) {
            assertEquals(expected[row].size, actual[row].size)
            for (column in expected[row].indices) {
                assertEquals(
                    expected = expected[row][column],
                    actual = actual[row][column],
                    absoluteTolerance = tolerance,
                    message = "Mismatch at [$row][$column]",
                )
            }
        }
    }
}
