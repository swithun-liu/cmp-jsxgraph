/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CurveTransformationTest {
    @Test
    fun transformedDataCurveMatchesOfficialMatrixPointsAndLifecycle() {
        val board = board("transformed-data")
        val source = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-1.0, 0.0, 2.0),
                dataY = doubleArrayOf(0.0, 2.0, -1.0),
                id = "dataSource",
                name = "",
            ),
        )
        val scale = transformation("scale", 2.0, -1.0)
        val translate = transformation("translate", 3.0, 4.0)
        val transformed = curve(
            Curve.createTransformed(
                board = board,
                source = source,
                transformations = listOf(scale, translate),
                id = "dataTransformed",
                name = "",
            ),
        )

        assertMatrix(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(3.0, 2.0, 0.0),
                doubleArrayOf(4.0, 0.0, -1.0),
            ),
            actual = transformed.transformMat,
        )
        assertContentEquals(
            doubleArrayOf(-1.0, 0.0, 2.0),
            transformed.dataX,
        )
        assertContentEquals(
            doubleArrayOf(0.0, 2.0, -1.0),
            transformed.dataY,
        )
        assertCurvePoints(
            transformed,
            listOf(
                doubleArrayOf(1.0, 1.0, 4.0),
                doubleArrayOf(1.0, 3.0, 2.0),
                doubleArrayOf(1.0, 7.0, 5.0),
            ),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 2.0),
            transformed.Ft(1.0),
        )
        assertSame(source, transformed.transformationSource)
        assertEquals(listOf(source.id), transformed.parents)
        assertSame(transformed, source.childElements[transformed.id])

        transformed.removeTransform(scale)
        board.update()
        assertCurvePoints(
            transformed,
            listOf(
                doubleArrayOf(1.0, 2.0, 4.0),
                doubleArrayOf(1.0, 3.0, 6.0),
                doubleArrayOf(1.0, 5.0, 3.0),
            ),
        )

        transformed.clearTransforms()
        board.update()
        assertCurvePoints(
            transformed,
            source.points.map { it.usrCoords.copyOf() },
        )

        transformed.addTransform(scale)
        board.update()
        assertCurvePoints(
            transformed,
            listOf(
                doubleArrayOf(1.0, -2.0, 0.0),
                doubleArrayOf(1.0, 0.0, -2.0),
                doubleArrayOf(1.0, 4.0, 1.0),
            ),
        )
    }

    @Test
    fun nestedDynamicTransformedCurvesTrackTheirSourceLikeOfficial() {
        val board = board("transformed-dynamic")
        val driver = point(board, 1.0, 0.0, "driver")
        val source = curve(
            Curve.createParametric(
                board = board,
                xSource = "x",
                ySource = "x * x",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 5,
                id = "continuousSource",
                name = "",
            ),
        )
        val translate = transformation(
            Transformation.create(
                board = board,
                type = "translate",
                parameters = listOf(
                    TransformationParameter.Dynamic(
                        TransformationDynamicParameter {
                            GMResult.Ok(driver.X())
                        },
                    ),
                    TransformationParameter.Numeric(2.0),
                ),
            ),
        )
        val rotate = transformation("rotate", PI * 0.5)
        val first = curve(
            Curve.createTransformed(
                board = board,
                source = source,
                transformations = listOf(translate),
                id = "firstTransform",
                name = "",
            ),
        )
        val nested = curve(
            Curve.createTransformed(
                board = board,
                source = first,
                transformations = listOf(rotate),
                id = "nestedTransform",
                name = "",
            ),
        )

        assertCurvePoints(
            first,
            listOf(
                point(-1.0, 6.0),
                point(-0.2, 3.44),
                point(0.6, 2.16),
                point(1.4, 2.16),
                point(2.2, 3.44),
            ),
        )
        assertCurvePoints(
            nested,
            listOf(
                point(-6.0, -1.0),
                point(-3.44, -0.2),
                point(-2.16, 0.6),
                point(-2.16, 1.4),
                point(-3.44, 2.2),
            ),
        )
        assertCoordinates(
            expected = doubleArrayOf(1.0, -3.44, -0.2),
            actual = nested.Ft(1.0),
        )

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(3.0, 0.0),
        )
        board.update()

        assertCurvePoints(
            first,
            listOf(
                point(1.0, 6.0),
                point(1.8, 3.44),
                point(2.6, 2.16),
                point(3.4, 2.16),
                point(4.2, 3.44),
            ),
        )
        assertCurvePoints(
            nested,
            listOf(
                point(-6.0, 1.0),
                point(-3.44, 1.8),
                point(-2.16, 2.6),
                point(-2.16, 3.4),
                point(-3.44, 4.2),
            ),
        )
        assertCoordinates(
            expected = doubleArrayOf(1.0, -3.44, 1.8),
            actual = nested.Ft(1.0),
        )
    }

    @Test
    fun transformedCurveFailuresAreStructuredAndAtomic() {
        val board = board("transformed-failures")
        val source = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(0.0, 1.0),
                dataY = doubleArrayOf(0.0, 1.0),
                id = "source",
                name = "",
            ),
        )
        val translate = transformation("translate", 1.0, 2.0)

        assertEquals(
            CurveError.EmptyTransformationList,
            assertIs<GMResult.Err<CurveError.EmptyTransformationList>>(
                Curve.createTransformed(
                    board = board,
                    source = source,
                    transformations = emptyList(),
                ),
            ).error,
        )

        val otherBoard = board("transformed-other-board")
        assertEquals(
            CurveError.TransformationSourceBoardMismatch,
            assertIs<
                GMResult.Err<CurveError.TransformationSourceBoardMismatch>
                >(
                Curve.createTransformed(
                    board = otherBoard,
                    source = source,
                    transformations = listOf(translate),
                ),
            ).error,
        )

        val removedBoard = board("transformed-removed-source")
        val removedSource = curve(
            Curve.createData(
                board = removedBoard,
                dataX = doubleArrayOf(0.0),
                dataY = doubleArrayOf(0.0),
                id = "removed",
                name = "",
            ),
        )
        removedBoard.removeObject(removedSource)
        assertEquals(
            CurveError.TransformationSourceNotRegistered("removed"),
            assertIs<GMResult.Err<CurveError.TransformationSourceNotRegistered>>(
                Curve.createTransformed(
                    board = removedBoard,
                    source = removedSource,
                    transformations = listOf(translate),
                ),
            ).error,
        )

        point(board, 4.0, 4.0, "taken")
        val beforeDuplicate = board.objects.keys.toList()
        assertIs<GMResult.Err<CurveError.Registration>>(
            Curve.createTransformed(
                board = board,
                source = source,
                transformations = listOf(translate),
                id = "taken",
                name = "",
            ),
        )
        assertEquals(beforeDuplicate, board.objects.keys.toList())
        assertTrue(source.childElements.isEmpty())

        val rejected = transformation(
            Transformation.create(
                board = board,
                type = "translate",
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
                    TransformationParameter.Numeric(0.0),
                ),
            ),
        )
        val beforeEvaluation = board.objects.keys.toList()
        val failure = assertIs<
            GMResult.Err<CurveError.TransformationEvaluation>
            >(
            Curve.createTransformed(
                board = board,
                source = source,
                transformations = listOf(rejected),
                id = "rejected",
                name = "",
            ),
        )
        assertEquals(0, failure.error.index)
        assertIs<TransformationError.DynamicParameterEvaluation>(
            failure.error.error,
        )
        assertEquals(beforeEvaluation, board.objects.keys.toList())
        assertTrue(source.childElements.isEmpty())
    }

    private fun board(id: String): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = id,
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
            ),
        ).value

    private fun point(
        x: Double,
        y: Double,
    ): DoubleArray = doubleArrayOf(1.0, x, y)

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun transformation(
        type: String,
        vararg parameters: Double,
    ): Transformation = transformation(
        Transformation.create(type, parameters),
    )

    private fun transformation(
        result: GMResult<Transformation, TransformationError>,
    ): Transformation =
        assertIs<GMResult.Ok<Transformation>>(result).value

    private fun assertCurvePoints(
        curve: Curve,
        expected: List<DoubleArray>,
    ) {
        assertEquals(expected.size, curve.numberPoints)
        assertEquals(expected.size, curve.points.size)
        for (index in expected.indices) {
            assertCoordinates(
                expected = expected[index],
                actual = curve.points[index].usrCoords,
            )
        }
    }

    private fun assertMatrix(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertCoordinates(expected[index], actual[index])
        }
    }

    private fun assertCoordinates(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                absoluteTolerance = TOLERANCE,
                message = "coordinate[$index]",
            )
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
