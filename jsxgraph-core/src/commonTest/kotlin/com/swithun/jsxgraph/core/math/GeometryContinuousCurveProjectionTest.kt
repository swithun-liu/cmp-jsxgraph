package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GeometryContinuousCurveProjectionTest {
    @Test
    fun functionGraphProjectionMatchesOfficialReferences() {
        val parabola = ContinuousCurve2D(
            curve = ParametricCurve2D(
                x = { parameter -> parameter },
                y = { parameter -> parameter * parameter },
            ),
            minimumParameter = -2.0,
            maximumParameter = 2.0,
            type = ContinuousCurveType.FUNCTION_GRAPH,
        )

        assertProjection(
            expectedPoint = point(
                1.9999980874491121,
                3.9999923498001064,
            ),
            expectedParameter = 1.9999980874491121,
            actual = projection(
                Geometry.projectCoordsToCurve(
                    horizontal = 5.0,
                    vertical = 4.0,
                    initialParameter = 2.0,
                    continuousCurve = parabola,
                ),
            ),
        )
        assertProjection(
            expectedPoint = point(
                0.8268865893336592,
                0.6837414316198516,
            ),
            expectedParameter = 0.8268865893336592,
            actual = projection(
                Geometry.projectCoordsToCurve(
                    horizontal = 0.8,
                    vertical = 0.7,
                    initialParameter = 0.0,
                    continuousCurve = parabola,
                ),
            ),
        )
    }

    @Test
    fun parameterProjectionMatchesOfficialCircleAndBoundaryReferences() {
        val circle = ContinuousCurve2D(
            curve = ParametricCurve2D(
                x = { parameter -> cos(parameter) },
                y = { parameter -> sin(parameter) },
            ),
            minimumParameter = 0.0,
            maximumParameter = 2.0 * kotlin.math.PI,
            type = ContinuousCurveType.PARAMETER,
        )
        assertProjection(
            expectedPoint = point(
                0.7525768153749035,
                0.6585044699621775,
            ),
            expectedParameter = 0.7188298163760756,
            actual = projection(
                Geometry.projectCoordsToCurve(
                    horizontal = 0.8,
                    vertical = 0.7,
                    initialParameter = 0.0,
                    continuousCurve = circle,
                ),
            ),
        )

        val boundedLine = ContinuousCurve2D(
            curve = ParametricCurve2D(
                x = { parameter -> parameter },
                y = { 0.0 },
            ),
            minimumParameter = -2.0,
            maximumParameter = 2.0,
            type = ContinuousCurveType.POLAR,
        )
        assertProjection(
            expectedPoint = point(-1.9999970447711366, 0.0),
            expectedParameter = -1.9999970447711366,
            actual = projection(
                Geometry.projectCoordsToCurve(
                    horizontal = -3.0,
                    vertical = 0.0,
                    initialParameter = 0.0,
                    continuousCurve = boundedLine,
                ),
            ),
        )
    }

    @Test
    fun continuousProjectionShrinksAroundUndefinedDomain() {
        val partialParabola = ContinuousCurve2D(
            curve = ParametricCurve2D(
                x = { parameter -> parameter },
                y = { parameter ->
                    if (parameter < 0.0) {
                        Double.NaN
                    } else {
                        parameter * parameter
                    }
                },
            ),
            minimumParameter = -2.0,
            maximumParameter = 2.0,
            type = ContinuousCurveType.PARAMETER,
        )

        assertProjection(
            expectedPoint = point(
                1.000000371978383,
                1.0000007439569043,
            ),
            expectedParameter = 1.000000371978383,
            actual = projection(
                Geometry.projectCoordsToCurve(
                    horizontal = 1.0,
                    vertical = 1.0,
                    initialParameter = 0.0,
                    continuousCurve = partialParabola,
                ),
            ),
        )
    }

    @Test
    fun continuousProjectionReportsInvalidDomains() {
        val curve = ParametricCurve2D(
            x = { parameter -> parameter },
            y = { 0.0 },
        )
        for (
            domain in listOf(
                Double.NaN to 1.0,
                0.0 to Double.POSITIVE_INFINITY,
                2.0 to -2.0,
            )
        ) {
            assertIs<GMResult.Err<GeometryError.InvalidContinuousCurveDomain>>(
                Geometry.projectCoordsToCurve(
                    horizontal = 0.0,
                    vertical = 0.0,
                    initialParameter = 0.0,
                    continuousCurve = ContinuousCurve2D(
                        curve = curve,
                        minimumParameter = domain.first,
                        maximumParameter = domain.second,
                        type = ContinuousCurveType.PARAMETER,
                    ),
                ),
            )
        }
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)

    private fun projection(
        result: GMResult<ProjectionResult, GeometryError>,
    ): ProjectionResult =
        assertIs<GMResult.Ok<ProjectionResult>>(result).value

    private fun assertProjection(
        expectedPoint: DoubleArray,
        expectedParameter: Double,
        actual: ProjectionResult,
    ) {
        assertEquals(expectedPoint.size, actual.point.size)
        for (index in expectedPoint.indices) {
            assertEquals(
                expectedPoint[index],
                actual.point[index],
                absoluteTolerance = 1.0e-8,
            )
        }
        assertEquals(
            expectedParameter,
            actual.parameter,
            absoluteTolerance = 1.0e-8,
        )
    }
}
