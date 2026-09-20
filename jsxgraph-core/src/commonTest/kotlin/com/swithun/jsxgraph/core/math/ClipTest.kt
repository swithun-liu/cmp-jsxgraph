/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Sector
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClipTest {
    @Test
    fun findIntersectionsUsesSubjectSegmentThenAlphaOrdering() {
        val intersections = Clip.findIntersections(
            subject = path(
                -2.0 to -2.0,
                2.0 to -2.0,
                2.0 to 2.0,
                -2.0 to 2.0,
            ),
            clip = path(
                0.0 to -3.0,
                3.0 to 0.0,
                0.0 to 3.0,
                -3.0 to 0.0,
            ),
        )

        assertCoordinates(
            expected = listOf(
                -1.0 to -2.0,
                1.0 to -2.0,
                2.0 to -1.0,
                2.0 to 1.0,
                1.0 to 2.0,
                -1.0 to 2.0,
                -2.0 to 1.0,
                -2.0 to -1.0,
            ),
            actual = intersections,
        )
        assertEquals(
            listOf(0, 0, 1, 1, 2, 2, 3, 3),
            intersections.map(ClipIntersection::subjectSegmentIndex),
        )
    }

    @Test
    fun findIntersectionsPreservesCollinearOverlapEndpoints() {
        val intersections = Clip.findIntersections(
            subject = path(
                -2.0 to -2.0,
                2.0 to -2.0,
                2.0 to 2.0,
                -2.0 to 2.0,
            ),
            clip = path(
                1.0 to -2.0,
                4.0 to -2.0,
                4.0 to 2.0,
                1.0 to 2.0,
            ),
        )

        assertCoordinates(
            expected = listOf(
                1.0 to -2.0,
                2.0 to -2.0,
                2.0 to 2.0,
                1.0 to 2.0,
            ),
            actual = intersections,
        )
        assertTrue(
            intersections.all {
                it.type == ClipIntersectionType.TOUCHING
            },
        )
    }

    @Test
    fun getPathMatchesOfficialCircleArcAndSectorSampling() {
        val board = Board(
            originX = 250.0,
            originY = 200.0,
            unitX = 50.0,
            unitY = 40.0,
        )
        val center = point(board, 0.0, 0.0)
        val radiusPoint = point(board, 3.0, 0.0)
        val anglePoint = point(board, 0.0, 3.0)
        val circle = assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = board,
                center = center,
                radius = 2.5,
                name = "",
            ),
        ).value
        val arc = assertIs<GMResult.Ok<Arc>>(
            Arc.create(
                board = board,
                center = center,
                radiuspoint = radiusPoint,
                anglepoint = anglePoint,
                name = "",
            ),
        ).value
        val sector = assertIs<GMResult.Ok<Sector>>(
            Sector.create(
                board = board,
                center = center,
                radiuspoint = radiusPoint,
                anglepoint = anglePoint,
                name = "",
            ),
        ).value

        val circlePath = path(Clip.getPath(circle))
        val arcPath = path(Clip.getPath(arc))
        val sectorPath = path(Clip.getPath(sector))

        assertEquals(360, circlePath.size)
        assertPoint(circlePath.first(), 2.5, 0.0)
        assertEquals(91, arcPath.size)
        assertPoint(arcPath.first(), 3.0, 0.0)
        assertPoint(arcPath.last(), 0.0, 3.0)
        assertEquals(93, sectorPath.size)
        assertPoint(sectorPath.first(), 0.0, 0.0)
        assertPoint(sectorPath[1], 3.0, 0.0)
        assertPoint(sectorPath[91], 0.0, 3.0)
        assertPoint(sectorPath.last(), 0.0, 0.0)
    }

    @Test
    fun booleanOperationsMatchOfficialCrossingFixture() {
        val board = board()
        val first = curve(
            board,
            listOf(
                -3.0 to -2.0,
                2.0 to -2.0,
                2.0 to 2.0,
                -3.0 to 2.0,
            ),
        )
        val second = curve(
            board,
            listOf(
                -1.0 to -3.0,
                4.0 to -3.0,
                4.0 to 1.0,
                -1.0 to 1.0,
            ),
        )

        assertBooleanCoordinates(
            operation = ClipBooleanOperation.INTERSECTION,
            first = first,
            second = second,
            expected = listOf(
                -1.0 to -2.0,
                2.0 to -2.0,
                2.0 to 1.0,
                -1.0 to 1.0,
                -1.0 to -2.0,
            ),
        )
        assertBooleanCoordinates(
            operation = ClipBooleanOperation.UNION,
            first = first,
            second = second,
            expected = listOf(
                -1.0 to -2.0,
                -3.0 to -2.0,
                -3.0 to 2.0,
                2.0 to 2.0,
                2.0 to 1.0,
                4.0 to 1.0,
                4.0 to -3.0,
                -1.0 to -3.0,
                -1.0 to -2.0,
            ),
        )
        assertBooleanCoordinates(
            operation = ClipBooleanOperation.DIFFERENCE,
            first = first,
            second = second,
            expected = listOf(
                -1.0 to -2.0,
                -3.0 to -2.0,
                -3.0 to 2.0,
                2.0 to 2.0,
                2.0 to 1.0,
                -1.0 to 1.0,
                -1.0 to -2.0,
            ),
        )
    }

    @Test
    fun booleanOperationsMatchOfficialEmptyIntersectionFixtures() {
        val disjointBoard = board()
        val disjointFirst = curve(
            disjointBoard,
            listOf(
                -5.0 to -2.0,
                -3.0 to -2.0,
                -3.0 to 0.0,
                -5.0 to 0.0,
            ),
        )
        val disjointSecond = curve(
            disjointBoard,
            listOf(
                2.0 to 1.0,
                4.0 to 1.0,
                4.0 to 3.0,
                2.0 to 3.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.INTERSECTION,
            disjointFirst,
            disjointSecond,
            emptyList(),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.UNION,
            disjointFirst,
            disjointSecond,
            listOf(
                -5.0 to -2.0,
                -3.0 to -2.0,
                -3.0 to 0.0,
                -5.0 to 0.0,
                -5.0 to -2.0,
                Double.NaN to Double.NaN,
                2.0 to 1.0,
                4.0 to 1.0,
                4.0 to 3.0,
                2.0 to 3.0,
                2.0 to 1.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.DIFFERENCE,
            disjointFirst,
            disjointSecond,
            listOf(
                -5.0 to -2.0,
                -3.0 to -2.0,
                -3.0 to 0.0,
                -5.0 to 0.0,
                -5.0 to -2.0,
            ),
        )

        val containedBoard = board()
        val outer = curve(
            containedBoard,
            listOf(
                -4.0 to -4.0,
                4.0 to -4.0,
                4.0 to 4.0,
                -4.0 to 4.0,
            ),
        )
        val inner = curve(
            containedBoard,
            listOf(
                -1.0 to -1.0,
                1.0 to -1.0,
                1.0 to 1.0,
                -1.0 to 1.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.INTERSECTION,
            outer,
            inner,
            listOf(
                -1.0 to -1.0,
                1.0 to -1.0,
                1.0 to 1.0,
                -1.0 to 1.0,
                -1.0 to -1.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.UNION,
            outer,
            inner,
            listOf(
                -4.0 to -4.0,
                4.0 to -4.0,
                4.0 to 4.0,
                -4.0 to 4.0,
                -4.0 to -4.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.DIFFERENCE,
            outer,
            inner,
            listOf(
                -4.0 to -4.0,
                -4.0 to 4.0,
                4.0 to 4.0,
                4.0 to -4.0,
                -4.0 to -4.0,
                Double.NaN to Double.NaN,
                -1.0 to -1.0,
                1.0 to -1.0,
                1.0 to 1.0,
                -1.0 to 1.0,
                -1.0 to -1.0,
            ),
        )
    }

    @Test
    fun booleanOperationsMatchOfficialDegenerateAndMultiComponentFixtures() {
        val sharedBoard = board()
        val left = curve(
            sharedBoard,
            listOf(
                -4.0 to -2.0,
                0.0 to -2.0,
                0.0 to 2.0,
                -4.0 to 2.0,
            ),
        )
        val right = curve(
            sharedBoard,
            listOf(
                0.0 to -2.0,
                4.0 to -2.0,
                4.0 to 2.0,
                0.0 to 2.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.INTERSECTION,
            left,
            right,
            listOf(
                0.0 to -2.0,
                0.0 to 2.0,
                0.0 to -2.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.UNION,
            left,
            right,
            listOf(
                0.0 to -2.0,
                -4.0 to -2.0,
                -4.0 to 2.0,
                0.0 to 2.0,
                4.0 to 2.0,
                4.0 to -2.0,
                0.0 to -2.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.DIFFERENCE,
            left,
            right,
            listOf(
                0.0 to -2.0,
                -4.0 to -2.0,
                -4.0 to 2.0,
                0.0 to 2.0,
                0.0 to -2.0,
            ),
        )

        val multiBoard = board()
        val multi = curve(
            multiBoard,
            listOf(
                -5.0 to -2.0,
                -2.0 to -2.0,
                -2.0 to 1.0,
                -5.0 to 1.0,
                Double.NaN to Double.NaN,
                1.0 to -1.0,
                4.0 to -1.0,
                4.0 to 2.0,
                1.0 to 2.0,
            ),
        )
        val window = curve(
            multiBoard,
            listOf(
                -3.0 to -3.0,
                2.0 to -3.0,
                2.0 to 3.0,
                -3.0 to 3.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.INTERSECTION,
            multi,
            window,
            listOf(
                -3.0 to -2.0,
                -2.0 to -2.0,
                -2.0 to 1.0,
                -3.0 to 1.0,
                -3.0 to -2.0,
                Double.NaN to Double.NaN,
                2.0 to -1.0,
                1.0 to -1.0,
                1.0 to 2.0,
                2.0 to 2.0,
                2.0 to -1.0,
            ),
        )
        assertBooleanCoordinates(
            ClipBooleanOperation.DIFFERENCE,
            multi,
            window,
            listOf(
                -3.0 to -2.0,
                -5.0 to -2.0,
                -5.0 to 1.0,
                -3.0 to 1.0,
                -3.0 to -2.0,
                Double.NaN to Double.NaN,
                2.0 to -1.0,
                4.0 to -1.0,
                4.0 to 2.0,
                2.0 to 2.0,
                2.0 to -1.0,
            ),
        )
    }

    private fun board(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )

    private fun curve(
        board: Board,
        coordinates: List<Pair<Double, Double>>,
    ): Curve =
        assertIs<GMResult.Ok<Curve>>(
            Curve.createData(
                board = board,
                dataX =
                    coordinates.map { it.first }.toDoubleArray(),
                dataY =
                    coordinates.map { it.second }.toDoubleArray(),
                name = "",
            ),
        ).value

    private fun assertBooleanCoordinates(
        operation: ClipBooleanOperation,
        first: Curve,
        second: Curve,
        expected: List<Pair<Double, Double>>,
    ) {
        val result = assertIs<GMResult.Ok<ClipBooleanResult>>(
            Clip.booleanOperation(first, second, operation),
        ).value
        assertEquals(expected.size, result.x.size)
        assertEquals(expected.size, result.y.size)
        for (index in expected.indices) {
            assertDouble(expected[index].first, result.x[index], "x[$index]")
            assertDouble(expected[index].second, result.y[index], "y[$index]")
        }
    }

    private fun assertDouble(
        expected: Double,
        actual: Double,
        label: String,
    ) {
        if (expected.isNaN()) {
            assertTrue(actual.isNaN(), "$label: expected NaN, actual=$actual")
        } else {
            assertTrue(
                abs(expected - actual) <= 1.0e-9,
                "$label: expected=$expected, actual=$actual",
            )
        }
    }

    private fun path(
        vararg coordinates: Pair<Double, Double>,
    ): List<ClipPathNode> =
        coordinates.mapIndexed { index, coordinate ->
            ClipPathNode(
                coordinates = doubleArrayOf(
                    1.0,
                    coordinate.first,
                    coordinate.second,
                ),
                position = index,
            )
        }

    private fun path(
        result: GMResult<List<ClipPathNode>, ClipError>,
    ): List<ClipPathNode> =
        assertIs<GMResult.Ok<List<ClipPathNode>>>(result).value

    private fun point(
        board: Board,
        x: Double,
        y: Double,
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = doubleArrayOf(x, y),
            name = "",
        ),
    ).value

    private fun assertCoordinates(
        expected: List<Pair<Double, Double>>,
        actual: List<ClipIntersection>,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertPoint(
                actual = ClipPathNode(
                    coordinates = actual[index].coordinates,
                    position = index,
                ),
                expectedX = expected[index].first,
                expectedY = expected[index].second,
            )
        }
    }

    private fun assertPoint(
        actual: ClipPathNode,
        expectedX: Double,
        expectedY: Double,
    ) {
        assertTrue(
            abs(expectedX - actual.coordinates[1]) <= 1.0e-12,
            "x: expected=$expectedX, actual=${actual.coordinates[1]}",
        )
        assertTrue(
            abs(expectedY - actual.coordinates[2]) <= 1.0e-12,
            "y: expected=$expectedY, actual=${actual.coordinates[2]}",
        )
    }
}
