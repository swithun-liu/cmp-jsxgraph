package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Transformation3DTest {
    private val board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
    )

    @Test
    fun scalarTransformsMatchOfficialReferenceValues() {
        val translation = transformation3D(
            Transformation.create3D(
                "translate",
                doubleArrayOf(2.0, -3.0, 4.0),
            ),
        )
        assertTrue(translation.is3D)
        assertTrue(translation.isNumericMatrix)
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                doubleArrayOf(2.0, 1.0, 0.0, 0.0),
                doubleArrayOf(-3.0, 0.0, 1.0, 0.0),
                doubleArrayOf(4.0, 0.0, 0.0, 1.0),
            ),
            actual = translation.matrix,
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 4.0, 0.0, 8.0),
            actual = applied(translation),
        )

        val scale = transformation3D(
            Transformation.create3D(
                "scale",
                doubleArrayOf(2.0, 3.0, 4.0, 99.0),
            ),
        )
        assertTrue(scale.isNumericMatrix)
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 4.0, 9.0, 16.0),
            actual = applied(scale),
        )

        val affine = transformation3D(
            Transformation.create3D(
                "affine",
                doubleArrayOf(
                    1.0,
                    2.0,
                    3.0,
                    4.0,
                    5.0,
                    6.0,
                    7.0,
                    8.0,
                    9.0,
                ),
            ),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 20.0, 47.0, 74.0),
            actual = applied(affine),
        )
    }

    @Test
    fun axisAndArbitraryRotationsMatchOfficialReferenceValues() {
        val rotateX = rotation("rotateX", scalar(PI / 2.0))
        val rotateY = rotation("rotateY", scalar(PI / 2.0))
        val rotateZ = rotation("rotateZ", scalar(PI / 2.0))

        assertEquals(TransformationType.ROTATE_X, rotateX.transformationType)
        assertFalse(rotateX.isNumericMatrix)
        assertNull(rotateX.clone())
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 2.0, -4.0, 3.0),
            actual = applied(rotateX),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 4.0, 3.0, -2.0),
            actual = applied(rotateY),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, -3.0, 2.0, 4.0),
            actual = applied(rotateZ),
        )
        val rotateExtraIgnored = rotation(
            type = "rotate",
            scalar(PI / 2.0),
            vector(0.0, 0.0, 1.0),
            vector(10.0, 20.0, 30.0),
            vector(99.0, 99.0, 99.0),
        )
        val rotateZExtraIgnored = rotation(
            type = "rotateZ",
            scalar(PI / 2.0),
            vector(10.0, 20.0, 30.0),
            vector(99.0, 99.0, 99.0),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, -3.0, 2.0, 4.0),
            actual = applied(rotateExtraIgnored),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, -3.0, 2.0, 4.0),
            actual = applied(rotateZExtraIgnored),
        )

        val aroundCenter = rotation(
            type = "rotate",
            scalar(PI / 3.0),
            vector(1.0, 2.0, 3.0),
            vector(2.0, -1.0, 4.0),
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                1.0,
                -0.49174601360336867,
                1.571428571428572,
                5.782962956915409,
            ),
            actual = applied(aroundCenter),
        )

        val homogeneousNormal = rotation(
            type = "rotate",
            scalar(PI / 2.0),
            vector(1.0, 0.0, 0.0, 1.0),
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                1.0,
                -2.1213203435596424,
                1.4142135623730951,
                1.9999999999999996,
            ),
            actual = applied(homogeneousNormal),
        )
    }

    @Test
    fun dynamicScalarVectorAndCenterParametersReevaluate() {
        var offset = 2.0
        var angle = PI / 4.0
        var normal = doubleArrayOf(0.0, 0.0, 2.0)
        var center = doubleArrayOf(1.0, -1.0, 2.0)
        val translation = transformation3D(
            Transformation.create3D(
                board = board,
                type = "translate",
                parameters = listOf(
                    dynamicScalar { offset },
                    scalar(1.0),
                    scalar(-2.0),
                ),
            ),
        )
        val rotation = transformation3D(
            Transformation.create3D(
                board = board,
                type = "rotate",
                parameters = listOf(
                    dynamicScalar { angle },
                    dynamicVector { normal },
                    dynamicVector { center },
                ),
            ),
        )
        val expressionRotation = transformation3D(
            Transformation.create3D(
                board = board,
                type = "rotate",
                parameters = listOf(
                    scalar(PI / 2.0),
                    Transformation3DParameter.VectorExpression("[0, 0, 1]"),
                    Transformation3DParameter.VectorExpression("[1, -1, 2]"),
                ),
            ),
        )

        assertVectorMatches(
            expected = doubleArrayOf(1.0, 4.0, 4.0, 2.0),
            actual = applied(translation),
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                1.0,
                -1.1213203435596422,
                2.5355339059327378,
                4.0,
            ),
            actual = applied(rotation),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, -3.0, 0.0, 4.0),
            actual = applied(expressionRotation),
        )

        offset = -5.0
        angle = PI / 2.0
        normal = doubleArrayOf(0.0, 1.0, 0.0)
        center = doubleArrayOf(-2.0, 3.0, 1.0)

        assertVectorMatches(
            expected = doubleArrayOf(1.0, -3.0, 4.0, 2.0),
            actual = applied(translation),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 1.0, 3.0, -3.0),
            actual = applied(rotation),
        )
    }

    @Test
    fun matrixFormsMatchOfficialAndRemainNonNumeric() {
        var dynamicOffset = 2.0
        val affine = transformation3D(
            Transformation.create3D(
                type = "affinematrix",
                matrix = arrayOf(
                    doubleArrayOf(1.0, 2.0, 3.0),
                    doubleArrayOf(4.0, 5.0, 6.0),
                    doubleArrayOf(7.0, 8.0, 9.0),
                ),
            ),
        )
        val projective = transformation3D(
            Transformation.create3D(
                type = "matrix",
                matrix = MATRIX_4D,
            ),
        )
        val dynamicMatrix = transformation3D(
            Transformation.create3DMatrix(
                board = board,
                type = "matrix",
                matrix = listOf(
                    listOf(
                        numericParameter(1.0),
                        numericParameter(0.0),
                        numericParameter(0.0),
                        numericParameter(0.0),
                    ),
                    listOf(
                        dynamicParameter { dynamicOffset },
                        numericParameter(1.0),
                        numericParameter(0.0),
                        numericParameter(0.0),
                    ),
                    listOf(
                        numericParameter(0.0),
                        numericParameter(0.0),
                        numericParameter(1.0),
                        numericParameter(0.0),
                    ),
                    listOf(
                        numericParameter(0.0),
                        numericParameter(0.0),
                        numericParameter(0.0),
                        numericParameter(1.0),
                    ),
                ),
            ),
        )

        assertFalse(affine.isNumericMatrix)
        assertFalse(projective.isNumericMatrix)
        assertFalse(dynamicMatrix.isNumericMatrix)
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 20.0, 47.0, 74.0),
            actual = applied(affine),
        )
        assertVectorMatches(
            expected = doubleArrayOf(30.0, 70.0, 110.0, 150.0),
            actual = applied(projective),
        )
        assertVectorMatches(
            expected = doubleArrayOf(1.0, 4.0, 3.0, 4.0),
            actual = applied(dynamicMatrix),
        )
        dynamicOffset = -5.0
        assertVectorMatches(
            expected = doubleArrayOf(1.0, -3.0, 3.0, 4.0),
            actual = applied(dynamicMatrix),
        )
    }

    @Test
    fun genericPreservesOfficialEvaluatorDefectAsStructuredFailure() {
        val generic = transformation3D(
            Transformation.create3D(
                type = "generic",
                parameters = DoubleArray(16) { index ->
                    (index + 1).toDouble()
                },
            ),
        )
        val before = generic.matrix.map(DoubleArray::copyOf).toTypedArray()

        assertTrue(generic.isNumericMatrix)
        val error = assertIs<
            GMResult.Err<TransformationError.UpstreamEvaluationDefect>
            >(generic.updateResult()).error
        assertEquals("generic", error.transformationType)
        assertEquals("1.13.3", error.upstreamVersion)
        assertMatrixMatches(before, generic.matrix)
        assertNull(generic.clone())
    }

    @Test
    fun zeroNormalPropagatesOfficialNonFiniteArithmetic() {
        val rotation = rotation(
            type = "rotate",
            scalar(PI / 2.0),
            vector(0.0, 0.0, 0.0),
        )

        assertTrue(applied(rotation).all(Double::isNaN))
        assertTrue(rotation.matrix.all { row -> row.all(Double::isNaN) })
    }

    @Test
    fun malformed3DConstructionReturnsStructuredErrors() {
        assertIs<GMResult.Err<TransformationError.UnsupportedType>>(
            Transformation.create3D(
                "reflect",
                doubleArrayOf(1.0, 2.0, 3.0),
            ),
        )
        assertIs<GMResult.Err<TransformationError.UnsupportedType>>(
            Transformation.create(
                "rotateX",
                doubleArrayOf(PI / 2.0),
            ),
        )
        assertIs<GMResult.Err<TransformationError.InvalidParameterCount>>(
            Transformation.create3D(
                "translate",
                doubleArrayOf(1.0, 2.0),
            ),
        )
        assertIs<GMResult.Err<TransformationError.InvalidParameterForm>>(
            Transformation.create3D(
                board = board,
                type = "rotate",
                parameters = listOf(
                    scalar(PI / 2.0),
                    scalar(1.0),
                ),
            ),
        )
        val malformedNormal = rotation(
            type = "rotate",
            scalar(PI / 2.0),
            vector(0.0, 1.0),
        )
        assertIs<GMResult.Err<TransformationError.InvalidCoordinateCount>>(
            malformedNormal.updateResult(),
        )
        assertIs<GMResult.Err<TransformationError.InvalidMatrixShape>>(
            Transformation.create3D(
                type = "matrix",
                matrix = arrayOf(
                    doubleArrayOf(1.0, 0.0),
                    doubleArrayOf(0.0, 1.0),
                ),
            ),
        )
    }

    private fun rotation(
        type: String,
        vararg parameters: Transformation3DParameter,
    ): Transformation =
        transformation3D(
            Transformation.create3D(
                board = board,
                type = type,
                parameters = parameters.toList(),
            ),
        )

    private fun scalar(value: Double): Transformation3DParameter =
        Transformation3DParameter.Scalar(
            numericParameter(value),
        )

    private fun numericParameter(value: Double): TransformationParameter =
        TransformationParameter.Numeric(value)

    private fun dynamicParameter(
        evaluator: () -> Double,
    ): TransformationParameter =
        TransformationParameter.Dynamic(
            TransformationDynamicParameter {
                GMResult.Ok(evaluator())
            },
        )

    private fun dynamicScalar(
        evaluator: () -> Double,
    ): Transformation3DParameter =
        Transformation3DParameter.Scalar(
            dynamicParameter(evaluator),
        )

    private fun vector(
        vararg values: Double,
    ): Transformation3DParameter =
        Transformation3DParameter.Vector(values)

    private fun dynamicVector(
        evaluator: () -> DoubleArray,
    ): Transformation3DParameter =
        Transformation3DParameter.DynamicVector(
            TransformationDynamicVectorParameter {
                GMResult.Ok(evaluator())
            },
        )

    private fun applied(transformation: Transformation): DoubleArray =
        assertIs<GMResult.Ok<DoubleArray>>(
            transformation.applyResult(SAMPLE),
        ).value

    private fun transformation3D(
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

        val SAMPLE = doubleArrayOf(1.0, 2.0, 3.0, 4.0)

        val MATRIX_4D = arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(5.0, 6.0, 7.0, 8.0),
            doubleArrayOf(9.0, 10.0, 11.0, 12.0),
            doubleArrayOf(13.0, 14.0, 15.0, 16.0),
        )
    }
}
