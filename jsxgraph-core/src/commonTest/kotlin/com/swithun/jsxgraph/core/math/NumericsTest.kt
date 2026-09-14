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
