package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NumericsCurveNewtonTest {
    @Test
    fun generalizedNewtonMatchesOfficialLineIntersection() {
        val result = point(
            Numerics.generalizedNewton(
                firstCurve = curve(x = { it }, y = { it }),
                secondCurve = curve(x = { it }, y = { 2.0 - it }),
                firstInitialParameter = 0.0,
                secondInitialParameter = 0.0,
            ),
        )

        assertEquals(
            0.9999999999967244,
            result[0],
            absoluteTolerance = 1.0e-10,
        )
        assertEquals(
            1.0000000000032756,
            result[1],
            absoluteTolerance = 1.0e-10,
        )
    }

    @Test
    fun generalizedNewtonMatchesOfficialNonlinearReferences() {
        val parabolaLine = point(
            Numerics.generalizedNewton(
                firstCurve = curve(x = { it }, y = { it * it }),
                secondCurve = curve(x = { it }, y = { 2.0 - it }),
                firstInitialParameter = 0.5,
                secondInitialParameter = 1.5,
            ),
        )
        assertEquals(
            1.0000076800197024,
            parabolaLine[0],
            absoluteTolerance = 1.0e-9,
        )
        assertEquals(
            0.9999923199802976,
            parabolaLine[1],
            absoluteTolerance = 1.0e-9,
        )

        val circleLine = point(
            Numerics.generalizedNewton(
                firstCurve = curve(x = { cos(it) }, y = { sin(it) }),
                secondCurve = curve(x = { it }, y = { 0.5 }),
                firstInitialParameter = 0.0,
                secondInitialParameter = 1.0,
            ),
        )
        assertEquals(
            0.8661025443641527,
            circleLine[0],
            absoluteTolerance = 1.0e-9,
        )
        assertEquals(
            0.4998663647875709,
            circleLine[1],
            absoluteTolerance = 1.0e-9,
        )
    }

    @Test
    fun dampedCurveNewtonMatchesOfficialParametersAndResiduals() {
        val lines = damped(
            Numerics.generalizedDampedNewtonCurves(
                firstCurve = curve(x = { it }, y = { it }),
                secondCurve = curve(x = { it }, y = { 2.0 - it }),
                firstInitialParameter = 0.0,
                secondInitialParameter = 0.0,
                damping = 1.0,
                epsilon = 1.0e-18,
            ),
        )
        assertContentEquals(
            doubleArrayOf(
                0.9999999999856222,
                0.9999999999856222,
            ),
            lines.parameters,
        )
        assertEquals(
            8.26888241768891e-22,
            lines.squaredResidual,
            absoluteTolerance = 1.0e-30,
        )

        val circleLine = damped(
            Numerics.generalizedDampedNewtonCurves(
                firstCurve = curve(x = { cos(it) }, y = { sin(it) }),
                secondCurve = curve(x = { it }, y = { 0.5 }),
                firstInitialParameter = 0.0,
                secondInitialParameter = 1.0,
                damping = 0.5,
                epsilon = 1.0e-18,
            ),
        )
        assertEquals(
            0.5235987750254149,
            circleLine.parameters[0],
            absoluteTolerance = 1.0e-10,
        )
        assertEquals(
            0.8660254041935618,
            circleLine.parameters[1],
            absoluteTolerance = 1.0e-10,
        )
        assertEquals(
            2.6119774498433815e-19,
            circleLine.squaredResidual,
            absoluteTolerance = 1.0e-25,
        )
    }

    @Test
    fun parallelCurveJacobiansReturnExplicitSingularErrors() {
        val first = curve(x = { it }, y = { it })
        val second = curve(x = { it + 1.0 }, y = { it + 1.0 })

        assertIs<GMResult.Err<NumericsError.SingularMatrix>>(
            Numerics.generalizedNewton(
                firstCurve = first,
                secondCurve = second,
                firstInitialParameter = 0.0,
                secondInitialParameter = 0.0,
            ),
        )
        assertIs<GMResult.Err<NumericsError.SingularMatrix>>(
            Numerics.generalizedDampedNewtonCurves(
                firstCurve = first,
                secondCurve = second,
                firstInitialParameter = 0.0,
                secondInitialParameter = 0.0,
                damping = 1.0,
                epsilon = 1.0e-18,
            ),
        )
    }

    private fun curve(
        x: (Double) -> Double,
        y: (Double) -> Double,
    ): ParametricCurve2D = ParametricCurve2D(x = x, y = y)

    private fun point(
        result: GMResult<DoubleArray, NumericsError>,
    ): DoubleArray = assertIs<GMResult.Ok<DoubleArray>>(result).value

    private fun damped(
        result: GMResult<DampedNewtonResult, NumericsError>,
    ): DampedNewtonResult = assertIs<GMResult.Ok<DampedNewtonResult>>(result).value
}
