package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Coords
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NumericsSimplificationTest {
    private val board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
    )

    @Test
    fun ramerDouglasPeuckerMatchesOfficialReference() {
        val points = points(
            0.0 to 0.0,
            1.0 to 0.1,
            2.0 to -0.1,
            3.0 to 4.0,
            4.0 to 4.1,
            5.0 to 4.0,
        )

        assertPointIndexes(
            expected = listOf(0, 2, 3, 5),
            original = points,
            actual = simplifiedValueOf(
                Numerics.RamerDouglasPeucker(points, tolerance = 0.25),
            ),
        )
        assertPointIndexes(
            expected = listOf(0, 1, 2, 3, 4, 5),
            original = points,
            actual = simplifiedValueOf(
                Numerics.RamerDouglasPeucker(points, tolerance = 0.05),
            ),
        )
    }

    @Test
    fun deprecatedRamerDouglasPeukerAliasUsesScreenCoordinates() {
        val points = points(
            0.0 to 0.0,
            1.0 to 0.1,
            2.0 to -0.1,
            3.0 to 4.0,
            4.0 to 4.1,
            5.0 to 4.0,
        )

        assertPointIndexes(
            expected = listOf(0, 2, 3, 5),
            original = points,
            actual = simplifiedValueOf(
                Numerics.RamerDouglasPeuker(points, tolerance = 0.25),
            ),
        )
    }

    @Test
    fun ramerDouglasPeuckerPreservesNaNSeparators() {
        val points = points(
            0.0 to 0.0,
            1.0 to 0.1,
            Double.NaN to Double.NaN,
            2.0 to 2.0,
            3.0 to 2.1,
            4.0 to 2.0,
        )

        assertPointIndexes(
            expected = listOf(0, 1, 2, 3, 5),
            original = points,
            actual = simplifiedValueOf(
                Numerics.RamerDouglasPeucker(points, tolerance = 0.25),
            ),
        )
    }

    @Test
    fun ramerDouglasPeuckerMatchesOfficialShortInputBehavior() {
        val point = point(0.0, 0.0, board)
        val pair = listOf(point, point(1.0, 1.0, board))
        val nanPoint = point(Double.NaN, Double.NaN, board)

        assertEquals(
            emptyList(),
            simplifiedValueOf(
                Numerics.RamerDouglasPeucker(emptyList(), tolerance = 0.1),
            ),
        )
        assertEquals(
            emptyList(),
            simplifiedValueOf(
                Numerics.RamerDouglasPeucker(listOf(point), tolerance = 0.1),
            ),
        )
        assertPointIndexes(
            expected = listOf(0, 1),
            original = pair,
            actual = simplifiedValueOf(
                Numerics.RamerDouglasPeucker(pair, tolerance = 0.1),
            ),
        )
        assertEquals(
            emptyList(),
            simplifiedValueOf(
                Numerics.RamerDouglasPeucker(
                    listOf(nanPoint, nanPoint),
                    tolerance = 0.1,
                ),
            ),
        )
    }

    @Test
    fun ramerDouglasPeuckerSelectsScreenOrUserCoordinates() {
        val anisotropicBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 0.01,
        )
        val points = listOf(
            point(0.0, 0.0, anisotropicBoard),
            point(1.0, 3.0, anisotropicBoard),
            point(2.0, 0.0, anisotropicBoard),
        )

        assertPointIndexes(
            expected = listOf(0, 2),
            original = points,
            actual = simplifiedValueOf(
                Numerics.RamerDouglasPeucker(
                    points = points,
                    tolerance = 0.1,
                    useUserCoordinates = false,
                ),
            ),
        )
        assertPointIndexes(
            expected = listOf(0, 1, 2),
            original = points,
            actual = simplifiedValueOf(
                Numerics.RamerDouglasPeucker(
                    points = points,
                    tolerance = 0.1,
                    useUserCoordinates = true,
                ),
            ),
        )
    }

    @Test
    fun ramerDouglasPeuckerMatchesOfficialDynamicParabolaSelection() {
        val viewport = Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
            keepAspectRatio = true,
            defaultCurveMinimum = -9.6,
            defaultCurveMaximum = 9.6,
        )
        val points = List(128) { index ->
            val x = index * 8.0 / 128.0
            point(
                x = x,
                y =
                    if (x == 4.0) {
                        Double.NaN
                    } else {
                        0.25 * 2.0 * (x - 4.0) * (x - 4.0) - 2.0
                    },
                targetBoard = viewport,
            )
        }

        val simplified = simplifiedValueOf(
            Numerics.RamerDouglasPeucker(
                points = points,
                tolerance = 0.004,
                useUserCoordinates = true,
            ),
        )

        assertEquals(
            listOf(
                0.0,
                0.25,
                0.5,
                0.75,
                1.0,
                1.25,
                1.5,
                1.6875,
                1.9375,
                2.1875,
                2.4375,
                2.5625,
                2.6875,
                2.8125,
                2.9375,
                3.0625,
                3.1875,
                3.3125,
                3.4375,
                3.5625,
                3.6875,
                3.8125,
                3.9375,
                4.0,
                4.0625,
                4.25,
                4.375,
                4.5,
                4.625,
                4.75,
                4.875,
                5.0,
                5.125,
                5.25,
                5.375,
                5.5,
                5.625,
                5.75,
                6.0,
                6.25,
                6.5,
                6.75,
                7.0,
                7.25,
                7.4375,
                7.6875,
                7.9375,
            ),
            simplified.map { coordinate -> coordinate.usrCoords[1] },
        )
    }

    @Test
    fun visvalingamMatchesOfficialReference() {
        val points = points(
            0.0 to 0.0,
            1.0 to 0.05,
            2.0 to 0.0,
            3.0 to 2.0,
            4.0 to 2.1,
            5.0 to 2.0,
            6.0 to 0.0,
        )

        assertPointIndexes(
            expected = listOf(0, 6),
            original = points,
            actual = simplifiedValueOf(
                Numerics.Visvalingam(points, numberOfIntermediatePoints = 0),
            ),
        )
        assertPointIndexes(
            expected = listOf(0, 2, 3, 6),
            original = points,
            actual = simplifiedValueOf(
                Numerics.Visvalingam(points, numberOfIntermediatePoints = 2),
            ),
        )
        assertPointIndexes(
            expected = points.indices.toList(),
            original = points,
            actual = simplifiedValueOf(
                Numerics.Visvalingam(points, numberOfIntermediatePoints = 20),
            ),
        )
    }

    @Test
    fun visvalingamMatchesOfficialTieAndNaNBehavior() {
        val ties = points(
            0.0 to 0.0,
            1.0 to 1.0,
            2.0 to 0.0,
            3.0 to 1.0,
            4.0 to 0.0,
        )
        assertPointIndexes(
            expected = listOf(0, 1, 4),
            original = ties,
            actual = simplifiedValueOf(
                Numerics.Visvalingam(ties, numberOfIntermediatePoints = 1),
            ),
        )

        val nanPoints = points(
            0.0 to 0.0,
            1.0 to 0.1,
            Double.NaN to Double.NaN,
            3.0 to 2.0,
            4.0 to 0.0,
        )
        assertPointIndexes(
            expected = listOf(0, 4),
            original = nanPoints,
            actual = simplifiedValueOf(
                Numerics.Visvalingam(
                    nanPoints,
                    numberOfIntermediatePoints = 5,
                ),
            ),
        )
    }

    @Test
    fun simplificationMatchesOfficialLongCurveReference() {
        val points = List(101) { index ->
            val x = -5.0 + index * 0.1
            point(
                x = x,
                y = sin(x) + 0.08 * sin(7.0 * x),
                targetBoard = board,
            )
        }

        assertPointIndexes(
            expected = listOf(
                0, 8, 12, 16, 21, 30, 38, 47,
                52, 61, 70, 79, 84, 88, 92, 100,
            ),
            original = points,
            actual = simplifiedValueOf(
                Numerics.RamerDouglasPeucker(points, tolerance = 0.08),
            ),
        )
        assertPointIndexes(
            expected = listOf(
                0, 7, 17, 20, 29, 38, 47,
                53, 62, 71, 80, 83, 93, 100,
            ),
            original = points,
            actual = simplifiedValueOf(
                Numerics.Visvalingam(
                    points,
                    numberOfIntermediatePoints = 12,
                ),
            ),
        )
    }

    @Test
    fun simplificationReportsInvalidParameters() {
        assertIs<GMResult.Err<NumericsError.InvalidSimplificationTolerance>>(
            Numerics.RamerDouglasPeucker(emptyList(), tolerance = -1.0),
        )
        assertIs<GMResult.Err<NumericsError.InvalidSimplificationTolerance>>(
            Numerics.RamerDouglasPeucker(emptyList(), tolerance = Double.NaN),
        )
        assertIs<GMResult.Err<NumericsError.InvalidSimplificationTolerance>>(
            Numerics.RamerDouglasPeucker(
                emptyList(),
                tolerance = Double.POSITIVE_INFINITY,
            ),
        )
        assertIs<GMResult.Err<NumericsError.InvalidSimplificationPointCount>>(
            Numerics.Visvalingam(emptyList(), numberOfIntermediatePoints = -1),
        )
    }

    private fun points(vararg values: Pair<Double, Double>): List<Coords> =
        values.map { (x, y) -> point(x, y, board) }

    private fun point(
        x: Double,
        y: Double,
        targetBoard: Board,
    ): Coords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = doubleArrayOf(x, y),
        board = targetBoard,
    )

    private fun simplifiedValueOf(
        result: GMResult<List<Coords>, NumericsError>,
    ): List<Coords> = assertIs<GMResult.Ok<List<Coords>>>(result).value

    private fun assertPointIndexes(
        expected: List<Int>,
        original: List<Coords>,
        actual: List<Coords>,
    ) {
        val actualIndexes = actual.map { selected ->
            original.indexOfFirst { candidate -> candidate === selected }
        }
        assertEquals(expected, actualIndexes)
    }
}
