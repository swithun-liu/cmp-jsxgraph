package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TransformationTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun standardNumericTransformsMatchOfficialReferenceValues() {
        /*
         * Values were captured from RELEASE-v1.13.3. The affine fixture uses
         * its documented update formula because that release asks for nine
         * evaluators after requiring exactly four parameters.
         */
        assertTransform(
            transformation = transformation("translate", 2.0, -3.0),
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(2.0, 1.0, 0.0),
                doubleArrayOf(-3.0, 0.0, 1.0),
            ),
            expectedCoordinates = doubleArrayOf(1.0, 5.0, -5.0),
        )
        assertTransform(
            transformation = transformation("scale", 2.0, -4.0),
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 2.0, 0.0),
                doubleArrayOf(0.0, 0.0, -4.0),
            ),
            expectedCoordinates = doubleArrayOf(1.0, 6.0, 8.0),
        )
        assertTransform(
            transformation = transformation(
                "reflect",
                1.0,
                2.0,
                4.0,
                6.0,
            ),
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(-0.64, -0.28, 0.96),
                doubleArrayOf(0.48, 0.96, 0.28),
            ),
            expectedCoordinates = doubleArrayOf(1.0, -3.4, 2.8),
        )
        assertTransform(
            transformation = transformation("rotate", PI / 3.0),
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(
                    0.0,
                    0.5000000000000001,
                    -0.8660254037844386,
                ),
                doubleArrayOf(
                    0.0,
                    0.8660254037844386,
                    0.5000000000000001,
                ),
            ),
            expectedCoordinates = doubleArrayOf(
                1.0,
                3.2320508075688776,
                1.5980762113533158,
            ),
        )
        assertTransform(
            transformation = transformation(
                "rotate",
                PI / 2.0,
                2.0,
                -1.0,
            ),
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(
                    0.9999999999999998,
                    6.123233995736766e-17,
                    -1.0,
                ),
                doubleArrayOf(
                    -3.0,
                    1.0,
                    6.123233995736766e-17,
                ),
            ),
            expectedCoordinates = doubleArrayOf(
                1.0,
                3.0,
                -1.2246467991473532e-16,
            ),
        )
        assertTransform(
            transformation = transformation("shear", 0.5, -2.0),
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.5),
                doubleArrayOf(0.0, -2.0, 1.0),
            ),
            expectedCoordinates = doubleArrayOf(1.0, 2.0, -8.0),
        )
        assertTransform(
            transformation = transformation(
                "affine",
                2.0,
                3.0,
                -1.0,
                4.0,
            ),
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 2.0, 3.0),
                doubleArrayOf(0.0, -1.0, 4.0),
            ),
            expectedCoordinates = doubleArrayOf(1.0, 0.0, -11.0),
        )
        assertTransform(
            transformation = transformation(
                "generic",
                2.0,
                1.0,
                -1.0,
                3.0,
                -2.0,
                4.0,
                5.0,
                6.0,
                -3.0,
            ),
            expectedMatrix = PROJECTIVE_MATRIX,
            expectedCoordinates = doubleArrayOf(7.0, -11.0, 29.0),
        )
    }

    @Test
    fun numericMatrixTransformsMatchOfficialReferenceValues() {
        val affineMatrix = transformation(
            type = "affinematrix",
            matrix = arrayOf(
                doubleArrayOf(2.0, 3.0),
                doubleArrayOf(-1.0, 4.0),
            ),
        )
        assertTransform(
            transformation = affineMatrix,
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 2.0, 3.0),
                doubleArrayOf(0.0, -1.0, 4.0),
            ),
            expectedCoordinates = doubleArrayOf(1.0, 0.0, -11.0),
        )

        val projective = transformation(
            type = "matrix",
            matrix = PROJECTIVE_MATRIX,
        )
        assertTransform(
            transformation = projective,
            expectedMatrix = PROJECTIVE_MATRIX,
            expectedCoordinates = doubleArrayOf(7.0, -11.0, 29.0),
        )
    }

    @Test
    fun dynamicParametersAndMatricesReevaluateLikeOfficialReference() {
        val driver = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 3.0),
                id = "driver",
                name = "driver",
            ),
        )
        var dynamicX = 2.0
        val translation = transformation(
            Transformation.create(
                board = board,
                type = "translate",
                parameters = listOf(
                    TransformationParameter.Dynamic(
                        TransformationDynamicParameter {
                            GMResult.Ok(dynamicX)
                        },
                    ),
                    TransformationParameter.Expression("Y(driver)"),
                ),
            ),
        )
        val projective = transformation(
            Transformation.createMatrix(
                board = board,
                type = "matrix",
                matrix = listOf(
                    listOf(
                        TransformationParameter.Numeric(1.0),
                        TransformationParameter.Numeric(0.0),
                        TransformationParameter.Numeric(0.0),
                    ),
                    listOf(
                        TransformationParameter.Expression("X(driver)"),
                        TransformationParameter.Numeric(1.0),
                        TransformationParameter.Numeric(0.0),
                    ),
                    listOf(
                        TransformationParameter.Numeric(0.0),
                        TransformationParameter.Numeric(0.0),
                        TransformationParameter.Numeric(1.0),
                    ),
                ),
            ),
        )

        assertFalse(translation.isNumericMatrix)
        assertFalse(projective.isNumericMatrix)
        assertNull(translation.clone())
        assertIs<GMResult.Ok<Transformation>>(translation.updateResult())
        assertIs<GMResult.Ok<Transformation>>(projective.updateResult())
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(2.0, 1.0, 0.0),
                doubleArrayOf(3.0, 0.0, 1.0),
            ),
            actual = translation.matrix,
        )
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(2.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0),
            ),
            actual = projective.matrix,
        )

        dynamicX = -1.0
        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-1.0, 0.5),
        )

        assertIs<GMResult.Ok<Transformation>>(translation.updateResult())
        assertIs<GMResult.Ok<Transformation>>(projective.updateResult())
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(-1.0, 1.0, 0.0),
                doubleArrayOf(0.5, 0.0, 1.0),
            ),
            actual = translation.matrix,
        )
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(-1.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0),
            ),
            actual = projective.matrix,
        )
    }

    @Test
    fun pointAndLineBackedTransformsTrackTheirCurrentGeometry() {
        val center = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(1.0, -1.0),
                id = "center",
            ),
        )
        val first = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(-2.0, 1.0),
            ),
        )
        val second = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 3.0),
            ),
        )
        val line = assertIs<GMResult.Ok<Line>>(
            Line.create(board, first, second),
        ).value
        var angle = 3.0
        val rotation = transformation(
            Transformation.createRotation(
                board = board,
                angle = TransformationParameter.Dynamic(
                    TransformationDynamicParameter {
                        GMResult.Ok(angle)
                    },
                ),
                center = center,
            ),
        )
        val lineReflection = Transformation.createReflectionFromLine(line)
        val pointReflection =
            Transformation.createReflectionFromPoints(first, second)

        assertIs<GMResult.Ok<Transformation>>(rotation.updateResult())
        assertIs<GMResult.Ok<Transformation>>(lineReflection.updateResult())
        assertIs<GMResult.Ok<Transformation>>(pointReflection.updateResult())
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(
                    1.8488724885405783,
                    -0.9899924966004454,
                    -0.1411200080598672,
                ),
                doubleArrayOf(
                    -2.1311125046603125,
                    0.1411200080598672,
                    -0.9899924966004454,
                ),
            ),
            actual = rotation.matrix,
        )
        assertMatrixMatches(lineReflection.matrix, pointReflection.matrix)

        angle = 0.5
        center.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 5.0),
        )
        line.update()

        assertIs<GMResult.Ok<Transformation>>(rotation.updateResult())
        assertIs<GMResult.Ok<Transformation>>(lineReflection.updateResult())
        assertIs<GMResult.Ok<Transformation>>(pointReflection.updateResult())
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(
                    1.2036859534276605,
                    0.8775825618903728,
                    -0.479425538604203,
                ),
                doubleArrayOf(
                    -0.7140162009891515,
                    0.479425538604203,
                    0.8775825618903728,
                ),
            ),
            actual = rotation.matrix,
        )
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(-4.0, -1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0),
            ),
            actual = lineReflection.matrix,
        )
        assertMatrixMatches(lineReflection.matrix, pointReflection.matrix)
    }

    @Test
    fun bindAndStaticMeltToMatchOfficialLifecycle() {
        val bound = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 1.0),
                id = "bound",
            ),
        )
        val boundTranslation = transformation("translate", 2.0, -1.0)
        boundTranslation.bindTo(bound)

        board.update()

        assertSame(bound, bound.baseElement)
        assertEquals(listOf(boundTranslation), bound.transformations)
        assertVectorMatches(
            doubleArrayOf(1.0, 3.0, 0.0),
            bound.coords.usrCoords,
        )
        bound.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(10.0, 5.0),
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 8.0, 6.0),
            bound.initialCoords.usrCoords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 10.0, 5.0),
            bound.coords.usrCoords,
        )

        val first = point(Point.create(board, doubleArrayOf(1.0, 2.0)))
        val second = point(Point.create(board, doubleArrayOf(-1.0, 4.0)))
        val translation = transformation("translate", 3.0, -2.0)
        val scale = transformation("scale", 2.0, 4.0)
        assertIs<GMResult.Ok<Unit>>(translation.meltTo(listOf(first, second)))
        assertTrue(first.transformations[0] !== second.transformations[0])
        assertTrue(first.transformations[0] !== translation)
        assertIs<GMResult.Ok<Unit>>(scale.meltTo(listOf(first, second)))

        assertEquals(1, first.transformations.size)
        assertEquals(1, second.transformations.size)
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(6.0, 2.0, 0.0),
                doubleArrayOf(-8.0, 0.0, 4.0),
            ),
            actual = first.transformations[0].matrix,
        )
        assertMatrixMatches(
            first.transformations[0].matrix,
            second.transformations[0].matrix,
        )
    }

    @Test
    fun dynamicEvaluationAndMeltFailuresAreStructured() {
        val compileFailure = assertIs<
            GMResult.Err<TransformationError.ParameterExpressionCompile>
            >(
            Transformation.create(
                board = board,
                type = "translate",
                parameters = listOf(
                    TransformationParameter.Expression("1 +"),
                    TransformationParameter.Numeric(0.0),
                ),
            ),
        )
        assertEquals(0, compileFailure.error.parameterIndex)

        val nonNumeric = transformation(
            Transformation.create(
                board = board,
                type = "translate",
                parameters = listOf(
                    TransformationParameter.Expression("\"x\""),
                    TransformationParameter.Numeric(0.0),
                ),
            ),
        )
        val resultFailure = assertIs<
            GMResult.Err<TransformationError.ParameterExpressionResult>
            >(nonNumeric.updateResult())
        assertEquals(0, resultFailure.error.parameterIndex)
        assertEquals("string", resultFailure.error.actualType)
        assertTrue(nonNumeric.apply(samplePoint()).all(Double::isNaN))

        val rejected = transformation(
            Transformation.create(
                board = board,
                type = "scale",
                parameters = listOf(
                    TransformationParameter.Dynamic(
                        TransformationDynamicParameter {
                            GMResult.Err(
                                TransformationDynamicParameterError.Rejected(
                                    "not available",
                                ),
                            )
                        },
                    ),
                    TransformationParameter.Numeric(1.0),
                ),
            ),
        )
        assertIs<
            GMResult.Err<TransformationError.DynamicParameterEvaluation>
            >(rejected.updateResult())

        val target = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
        )
        assertIs<
            GMResult.Err<TransformationError.DynamicMeltUnsupported>
            >(rejected.meltTo(target))
        assertTrue(target.transformations.isEmpty())
    }

    @Test
    fun reflectionLineAndPointFormsMatchOfficialReferenceValues() {
        val lineReflection = transformation(
            Transformation.createReflectionFromLine(
                doubleArrayOf(-2.0, 1.0, 0.0, 0.0),
            ),
        )
        assertTransform(
            transformation = lineReflection,
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(4.0, -1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0),
            ),
            expectedCoordinates = doubleArrayOf(1.0, 1.0, -2.0),
        )

        val pointReflection = transformation(
            Transformation.createReflectionFromPoints(
                first = doubleArrayOf(1.0, 1.0, 2.0),
                second = doubleArrayOf(1.0, 4.0, 6.0),
            ),
        )
        assertTransform(
            transformation = pointReflection,
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(-0.64, -0.28, 0.96),
                doubleArrayOf(0.48, 0.96, 0.28),
            ),
            expectedCoordinates = doubleArrayOf(1.0, -3.4, 2.8),
        )
    }

    @Test
    fun applyCanUseCurrentOrInitialCoordinates() {
        val point = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 2.0),
        )
        point.coords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, 5.0),
        )
        val translation = transformation("translate", 2.0, -3.0)

        assertVectorMatches(
            expected = doubleArrayOf(1.0, 6.0, 2.0),
            actual = translation.apply(point),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 3.0, -1.0),
            actual = translation.apply(point, self = true),
        )
    }

    @Test
    fun applyOnceMutatesEveryPointAndNormalizesProjectiveCoordinates() {
        val first = Point(
            board = board,
            coordinates = doubleArrayOf(3.0, -2.0),
        )
        val second = Point(
            board = board,
            coordinates = doubleArrayOf(-1.0, 4.0),
        )
        val transformation = transformation(
            type = "matrix",
            matrix = arrayOf(
                doubleArrayOf(2.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0),
            ),
        )

        transformation.applyOnce(listOf(first, second))

        assertVectorMatches(
            expected = doubleArrayOf(1.0, 1.5, -1.0),
            actual = first.coords.usrCoords,
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, -0.5, 2.0),
            actual = second.coords.usrCoords,
        )
    }

    @Test
    fun cloneIsIndependentAndMeltMultipliesFromTheLeft() {
        val translation = transformation("translate", 2.0, -3.0)
        val clone = assertIs<Transformation>(translation.clone())
        val scale = transformation("scale", 4.0, 5.0)

        assertTrue(clone !== translation)
        assertIs<GMResult.Ok<Transformation>>(clone.melt(scale))

        assertTransform(
            transformation = clone,
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(8.0, 4.0, 0.0),
                doubleArrayOf(-15.0, 0.0, 5.0),
            ),
            expectedCoordinates = doubleArrayOf(1.0, 20.0, -25.0),
        )
        assertTransform(
            transformation = translation,
            expectedMatrix = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(2.0, 1.0, 0.0),
                doubleArrayOf(-3.0, 0.0, 1.0),
            ),
            expectedCoordinates = doubleArrayOf(1.0, 5.0, -5.0),
        )
    }

    @Test
    fun degenerateReflectionPropagatesNonFiniteArithmetic() {
        val reflection = transformation(
            "reflect",
            1.0,
            2.0,
            1.0,
            2.0,
        )
        val coordinates = reflection.apply(samplePoint())

        assertVectorMatches(
            expected = doubleArrayOf(1.0, 0.0, 0.0),
            actual = reflection.matrix[0],
        )
        for (row in 1..2) {
            assertTrue(reflection.matrix[row].all(Double::isNaN))
        }
        assertEquals(1.0, coordinates[0])
        assertTrue(coordinates[1].isNaN())
        assertTrue(coordinates[2].isNaN())
    }

    @Test
    fun malformedConstructionReturnsStructuredErrors() {
        assertIs<GMResult.Err<TransformationError.UnsupportedType>>(
            Transformation.create("skew", doubleArrayOf(1.0, 2.0)),
        )
        assertIs<GMResult.Err<TransformationError.InvalidParameterCount>>(
            Transformation.create("translate", doubleArrayOf(1.0)),
        )
        assertIs<GMResult.Err<TransformationError.InvalidParameterCount>>(
            Transformation.create("rotate", doubleArrayOf(1.0, 2.0)),
        )
        assertIs<GMResult.Err<TransformationError.InvalidParameterForm>>(
            Transformation.create("matrix", doubleArrayOf(1.0)),
        )
        assertIs<GMResult.Err<TransformationError.InvalidParameterForm>>(
            Transformation.create(
                type = "scale",
                matrix = arrayOf(
                    doubleArrayOf(1.0, 0.0),
                    doubleArrayOf(0.0, 1.0),
                ),
            ),
        )
        assertIs<GMResult.Err<TransformationError.InvalidMatrixShape>>(
            Transformation.create(
                type = "matrix",
                matrix = arrayOf(
                    doubleArrayOf(1.0, 0.0, 0.0),
                    doubleArrayOf(0.0, 1.0),
                    doubleArrayOf(0.0, 0.0, 1.0),
                ),
            ),
        )
        assertIs<GMResult.Err<TransformationError.InvalidCoordinateCount>>(
            Transformation.createReflectionFromLine(
                doubleArrayOf(1.0, 2.0),
            ),
        )
        assertIs<GMResult.Err<TransformationError.InvalidCoordinateCount>>(
            Transformation.createReflectionFromPoints(
                first = doubleArrayOf(1.0, 2.0),
                second = doubleArrayOf(1.0, 3.0, 4.0),
            ),
        )
    }

    private fun assertTransform(
        transformation: Transformation,
        expectedMatrix: Array<DoubleArray>,
        expectedCoordinates: DoubleArray,
    ) {
        assertMatrixMatches(expectedMatrix, transformation.matrix)
        assertVectorMatches(expectedCoordinates, transformation.apply(samplePoint()))
    }

    private fun samplePoint(): Point =
        Point(
            board = board,
            coordinates = doubleArrayOf(3.0, -2.0),
        )

    private fun point(
        result: GMResult<Point, PointError>,
    ): Point = assertIs<GMResult.Ok<Point>>(result).value

    private fun transformation(
        type: String,
        vararg parameters: Double,
    ): Transformation =
        transformation(
            Transformation.create(type, parameters),
        )

    private fun transformation(
        type: String,
        matrix: Array<DoubleArray>,
    ): Transformation =
        transformation(
            Transformation.create(type, matrix),
        )

    private fun transformation(
        result: GMResult<Transformation, TransformationError>,
    ): Transformation =
        assertIs<GMResult.Ok<Transformation>>(result).value

    private fun assertMatrixMatches(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (row in expected.indices) {
            assertVectorMatches(expected[row], actual[row])
        }
    }

    private fun assertVectorMatches(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected = expected[index],
                actual = actual[index],
                absoluteTolerance = TOLERANCE,
            )
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12

        val PROJECTIVE_MATRIX = arrayOf(
            doubleArrayOf(2.0, 1.0, -1.0),
            doubleArrayOf(3.0, -2.0, 4.0),
            doubleArrayOf(5.0, 6.0, -3.0),
        )
    }
}
