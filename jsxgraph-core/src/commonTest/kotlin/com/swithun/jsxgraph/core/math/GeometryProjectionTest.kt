package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class GeometryProjectionTest {
    @Test
    fun circleProjectionMatchesOfficialReferences() {
        assertPointClose(
            expected = point(1.2, 1.6),
            actual = Geometry.projectPointToCircle(
                point = point(3.0, 4.0),
                center = point(0.0, 0.0),
                radius = 2.0,
            ),
        )
        assertContentEquals(
            point(4.0, 6.0),
            Geometry.projectPointToCircle(
                point = point(4.0, 6.0),
                center = point(1.0, 2.0),
                radius = 5.0,
            ),
        )
    }

    @Test
    fun circleProjectionPreservesCenterAndIdealPointRules() {
        assertContentEquals(
            point(1.0, 2.0),
            Geometry.projectPointToCircle(
                point = point(1.0, 2.0),
                center = point(1.0, 2.0),
                radius = 5.0,
            ),
        )
        assertContentEquals(
            point(0.0, 0.0),
            Geometry.projectPointToCircle(
                point = doubleArrayOf(0.0, 3.0, 4.0),
                center = point(0.0, 0.0),
                radius = 2.0,
            ),
        )
    }

    @Test
    fun lineProjectionMatchesOfficialStandardFormReferences() {
        assertContentEquals(
            point(3.0, 2.0),
            Geometry.projectPointToLine(
                point = point(3.0, 5.0),
                line = doubleArrayOf(-2.0, 0.0, 1.0),
            ),
        )
        assertContentEquals(
            point(-1.0, 5.0),
            Geometry.projectPointToLine(
                point = point(3.0, 5.0),
                line = doubleArrayOf(1.0, 1.0, 0.0),
            ),
        )
        assertContentEquals(
            point(1.0, 1.0),
            Geometry.projectPointToLine(
                point = point(2.0, 0.0),
                line = doubleArrayOf(0.0, 1.0, -1.0),
            ),
        )
    }

    @Test
    fun lineProjectionDoesNotRequireNormalizedStandardForm() {
        assertContentEquals(
            point(3.0, 2.0),
            Geometry.projectPointToLine(
                point = point(3.0, 5.0),
                line = doubleArrayOf(-20.0, 0.0, 10.0),
            ),
        )
    }

    @Test
    fun pointAndBoardProjectionMatchOfficialReferences() {
        val destination = point(4.0, -3.0)
        assertSame(
            destination,
            Geometry.projectPointToPoint(
                point = point(20.0, 10.0),
                destination = destination,
            ),
        )
        assertContentEquals(
            point(5.0, 5.0),
            Geometry.projectPointToBoard(
                point = point(8.0, 7.0),
                boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            ),
        )
        assertContentEquals(
            point(2.0, -3.0),
            Geometry.projectPointToBoard(
                point = point(2.0, -3.0),
                boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0),
            ),
        )
    }

    @Test
    fun lineBoardIntersectionsMatchOfficialViewportCases() {
        assertIntersections(
            expectedFirst = doubleArrayOf(1.0, 0.0, 0.0),
            expectedSecond = doubleArrayOf(1.0, 600.0, 600.0),
            actual = Geometry.meetLineBoard(
                line = doubleArrayOf(0.0, 1.0, -1.0),
                canvasWidth = 800.0,
                canvasHeight = 600.0,
            ),
        )
        assertIntersections(
            expectedFirst = doubleArrayOf(1.0, 250.0, 0.0),
            expectedSecond = doubleArrayOf(1.0, 250.0, 600.0),
            actual = Geometry.meetLineBoard(
                line = doubleArrayOf(-250.0, 1.0, 0.0),
                canvasWidth = 800.0,
                canvasHeight = 600.0,
            ),
        )
        assertIntersections(
            expectedFirst = doubleArrayOf(1.0, 0.0, 120.0),
            expectedSecond = doubleArrayOf(1.0, 800.0, 120.0),
            actual = Geometry.meetLineBoard(
                line = doubleArrayOf(-120.0, 0.0, 1.0),
                canvasWidth = 800.0,
                canvasHeight = 600.0,
            ),
        )
    }

    @Test
    fun lineDelimitingPointsMatchOfficialLineRayAndSegmentCases() {
        val boundingBox = doubleArrayOf(-5.0, 5.0, 5.0, -5.0)
        val line = Geometry.calcLineDelimitingPoints(
            lineStandardForm = doubleArrayOf(0.0, 1.0, -1.0),
            firstPoint = point(-1.0, -1.0),
            secondPoint = point(1.0, 1.0),
            straightFirst = true,
            straightLast = true,
            boundingBox = boundingBox,
        )
        assertPointClose(point(-5.0, -5.0), line.first)
        assertPointClose(point(5.0, 5.0), line.second)

        val ray = Geometry.calcLineDelimitingPoints(
            lineStandardForm = doubleArrayOf(0.0, 0.0, 1.0),
            firstPoint = point(0.0, 0.0),
            secondPoint = point(1.0, 0.0),
            straightFirst = false,
            straightLast = true,
            boundingBox = boundingBox,
        )
        assertPointClose(point(0.0, 0.0), ray.first)
        assertPointClose(point(5.0, 0.0), ray.second)

        val segment = Geometry.calcLineDelimitingPoints(
            lineStandardForm = doubleArrayOf(0.0, 0.0, 1.0),
            firstPoint = point(-1.0, 0.0),
            secondPoint = point(1.0, 0.0),
            straightFirst = false,
            straightLast = false,
            boundingBox = boundingBox,
        )
        assertPointClose(point(-1.0, 0.0), segment.first)
        assertPointClose(point(1.0, 0.0), segment.second)
    }

    @Test
    fun screenParametricProjectionMatchesOfficialCobylaResults() {
        val evaluator = Parametric3DEvaluator<String> { parameters ->
            val parameter = parameters[0]
            GMResult.Ok(
                doubleArrayOf(
                    kotlin.math.cos(parameter),
                    kotlin.math.sin(parameter),
                    parameter / kotlin.math.PI,
                ),
            )
        }
        val projector = ParametricScreenProjector { coordinates ->
            doubleArrayOf(1.0, coordinates[1], coordinates[2])
        }
        val parameters = mutableListOf(0.3)
        val projection = assertIs<GMResult.Ok<DoubleArray>>(
            Geometry.projectScreenCoordsToParametric(
                screenCoordinates = doubleArrayOf(0.2, 1.2),
                evaluator = evaluator,
                projector = projector,
                parameters = parameters,
                rangeU = doubleArrayOf(0.0, 2.0 * kotlin.math.PI),
            ),
        )
        assertPoint4Close(
            expected = doubleArrayOf(
                1.0,
                0.16439887466425038,
                0.9863939426056549,
                0.4474315796380962,
            ),
            actual = projection.value,
        )
        assertEquals(
            1.4056477635751194,
            parameters.single(),
            absoluteTolerance = 1.0e-12,
        )

        val cyclicParameters = mutableListOf(6.1)
        val cyclicProjection = assertIs<GMResult.Ok<DoubleArray>>(
            Geometry.projectScreenCoordsToParametric(
                screenCoordinates = doubleArrayOf(1.0, 0.0),
                evaluator = evaluator,
                projector = projector,
                parameters = cyclicParameters,
                rangeU = doubleArrayOf(0.0, 2.0 * kotlin.math.PI),
                cyclic = true,
            ),
        )
        assertPoint4Close(
            expected = doubleArrayOf(
                1.0,
                0.9999999999999936,
                1.1354428153254024e-7,
                3.614226733144325e-8,
            ),
            actual = cyclicProjection.value,
        )
        assertEquals(
            1.1354428153254048e-7,
            cyclicParameters.single(),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun screenParametricProjectionRejectsInvalidInputs() {
        val evaluator = Parametric3DEvaluator<String> {
            GMResult.Ok(doubleArrayOf(0.0, 0.0, 0.0))
        }
        val projector = ParametricScreenProjector {
            doubleArrayOf(1.0, 0.0, 0.0)
        }
        assertIs<
            GMResult.Err<
                ParametricProjectionError.InvalidScreenCoordinateCount
                >
            >(
            Geometry.projectScreenCoordsToParametric(
                screenCoordinates = doubleArrayOf(0.0),
                evaluator = evaluator,
                projector = projector,
                parameters = mutableListOf(0.0),
                rangeU = doubleArrayOf(0.0, 1.0),
            ),
        )
        assertIs<GMResult.Err<ParametricProjectionError.InvalidDimension>>(
            Geometry.projectScreenCoordsToParametric(
                screenCoordinates = doubleArrayOf(0.0, 0.0),
                evaluator = evaluator,
                projector = projector,
                parameters = mutableListOf(),
                rangeU = doubleArrayOf(0.0, 1.0),
            ),
        )
    }

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)

    private fun assertPointClose(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                absoluteTolerance = 1.0e-12,
            )
        }
    }

    private fun assertPoint4Close(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                absoluteTolerance = 1.0e-11,
            )
        }
    }

    private fun assertIntersections(
        expectedFirst: DoubleArray,
        expectedSecond: DoubleArray,
        actual: LineBoardIntersections,
    ) {
        assertPointClose(expectedFirst, actual.first)
        assertPointClose(expectedSecond, actual.second)
    }
}
