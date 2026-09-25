package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Line3DTest {
    @Test
    fun twoPointAndDirectionFormsMatchOfficialFixture() {
        val board = createBoard()
        val view = createView(board)
        val numeric = point(view, doubleArrayOf(1.0, 2.0, 2.0), "numeric")
        val functionPoint = point(
            Point3D.create(
                view = view,
                coordinateSource = Point3DCoordinateSource.Function(
                    Point3DArrayEvaluator {
                        GMResult.Ok(doubleArrayOf(2.0, -1.0, 3.0))
                    },
                ),
                id = "functionPoint",
                name = "",
                fixed = true,
            ),
        )
        val mixedPoint = point(
            view,
            doubleArrayOf(-2.0, 3.0, 1.0),
            "mixedPoint",
        )
        val homogeneous = point(
            view,
            doubleArrayOf(2.0, 4.0, 6.0, 8.0),
            "homogeneous",
        )
        val twoPointLine = line(
            Line3D.create(
                view = view,
                point1 = numeric,
                point2 = functionPoint,
                straightFirst = true,
                straightLast = true,
                id = "twoPointLine",
                name = "",
            ),
        )
        val directionLine = line(
            Line3D.create(
                view = view,
                point = mixedPoint,
                directionSource = Line3DDirectionSource.Values(
                    listOf(
                        Line3DCoordinateValue.Numeric(0.0),
                        Line3DCoordinateValue.Numeric(2.0),
                        Line3DCoordinateValue.Numeric(-1.0),
                    ),
                ),
                rangeSource = listOf(
                    Line3DCoordinateValue.Numeric(
                        Double.NEGATIVE_INFINITY,
                    ),
                    Line3DCoordinateValue.Numeric(2.0),
                ),
                id = "directionLine",
                name = "",
            ),
        )
        val copiedDirectionLine = line(
            Line3D.create(
                view = view,
                point = homogeneous,
                directionSource =
                    Line3DDirectionSource.Line(directionLine),
                rangeSource = listOf(
                    Line3DCoordinateValue.Numeric(-2.0),
                    Line3DCoordinateValue.Numeric(
                        Double.POSITIVE_INFINITY,
                    ),
                ),
                id = "copiedDirectionLine",
                name = "",
            ),
        )

        board.fullUpdate()

        assertContentEquals(
            doubleArrayOf(0.0, 1.0, -3.0, 1.0),
            twoPointLine.vec,
        )
        assertVectorMatches(
            doubleArrayOf(
                1.0,
                -0.33333333333333326,
                6.0,
                0.6666666666666667,
            ),
            twoPointLine.endpoints[0].coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 3.0, -4.0, 4.0),
            twoPointLine.endpoints[1].coords,
        )
        assertSame(numeric, twoPointLine.point1)
        assertSame(functionPoint, twoPointLine.point2)
        assertFalse(twoPointLine.line2D.straightFirst)
        assertFalse(twoPointLine.line2D.straightLast)
        assertFalse(twoPointLine.line2D.dump)

        assertContentEquals(
            doubleArrayOf(0.0, 0.0, 2.0, -1.0),
            directionLine.vec,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, -2.0, -4.0, 4.5),
            directionLine.endpoints[0].coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, -2.0, 6.0, -0.5),
            directionLine.endpoints[1].coords,
        )
        assertSame(directionLine.endpoints[0], directionLine.point1)
        assertSame(directionLine.endpoints[1], directionLine.point2)
        assertEquals(listOf("mixedPoint"), directionLine.parents)

        assertContentEquals(directionLine.vec, copiedDirectionLine.vec)
        assertVectorMatches(
            doubleArrayOf(2.0, 4.0, 8.0, 7.0),
            copiedDirectionLine.endpoints[0].coords,
        )
        assertVectorMatches(
            doubleArrayOf(2.0, 4.0, 6.0, 8.0),
            copiedDirectionLine.endpoints[1].coords,
        )
        assertEquals(
            listOf("homogeneous", "directionLine"),
            copiedDirectionLine.parents,
        )
    }

    @Test
    fun dynamicDirectionProjectionAndTransformMatchOfficialFixture() {
        val board = createBoard()
        val view = createView(board)
        var dynamicX = 1.0
        val point = point(
            view,
            doubleArrayOf(-2.0, 3.0, 1.0),
            "point",
        )
        val directionLine = line(
            Line3D.create(
                view = view,
                point = point,
                directionSource = Line3DDirectionSource.Values(
                    listOf(
                        Line3DCoordinateValue.Dynamic(
                            Line3DScalarEvaluator {
                                GMResult.Ok(dynamicX - 1.0)
                            },
                        ),
                        Line3DCoordinateValue.Numeric(2.0),
                        Line3DCoordinateValue.Numeric(-1.0),
                    ),
                ),
                rangeSource = listOf(
                    Line3DCoordinateValue.Numeric(
                        Double.NEGATIVE_INFINITY,
                    ),
                    Line3DCoordinateValue.Numeric(2.0),
                ),
                id = "directionLine",
                name = "",
            ),
        )
        val translation = assertIs<GMResult.Ok<Transformation>>(
            Transformation.create3D(
                "translate",
                doubleArrayOf(2.0, -3.0, 4.0),
            ),
        ).value
        val transformed = line(
            Line3D.create(
                view = view,
                baseLine = directionLine,
                transformations = listOf(translation),
                id = "transformedLine",
                name = "",
            ),
        )

        board.fullUpdate()
        val parameters = doubleArrayOf(0.0)
        assertVectorMatches(
            doubleArrayOf(1.0, -2.0, 2.0, 1.5),
            directionLine.projectCoords(
                coordinates = doubleArrayOf(1.0, 3.0, 2.0),
                parameters = parameters,
            ),
        )
        assertEquals(-0.5, parameters[0], TOLERANCE)
        assertVectorMatches(
            doubleArrayOf(
                1.0,
                -2.0,
                1.6232816487111346,
                1.6883591756444325,
            ),
            directionLine.projectScreenCoords(
                doubleArrayOf(0.25, -0.5),
            ),
        )
        assertEquals(-100000.0, directionLine.evaluatedRange[0])
        assertSame(directionLine, transformed.baseElement)
        assertEquals(
            TransformationType.TRANSLATE,
            transformed.transformations.single().transformationType,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 0.0, -4.0, 7.0),
            transformed.endpoints[0].coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 0.0, 6.0, 2.0),
            transformed.endpoints[1].coords,
        )

        dynamicX = 2.0
        assertIs<GMResult.Ok<Point3D>>(
            point.setPosition(doubleArrayOf(5.0, 3.0, 0.0)),
        )
        board.fullUpdate()

        assertContentEquals(
            doubleArrayOf(0.0, 1.0, 2.0, -1.0),
            directionLine.vec,
        )
        assertEquals(-100000.0, directionLine.evaluatedRange[0])
        assertVectorMatches(
            doubleArrayOf(1.0, 7.0, -4.0, 6.0),
            transformed.endpoints[0].coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 7.0, 6.0, 1.0),
            transformed.endpoints[1].coords,
        )
    }

    @Test
    fun removalCleansOwnedEndpointsProxyAndViewRegistry() {
        val board = createBoard()
        val view = createView(board)
        val point1 = point(view, doubleArrayOf(0.0, 0.0, 0.0), "A")
        val point2 = point(view, doubleArrayOf(1.0, 1.0, 1.0), "B")
        val line = line(
            Line3D.create(
                view = view,
                point1 = point1,
                point2 = point2,
                id = "line",
                name = "",
            ),
        )
        val endpointIds = line.endpoints.flatMap {
            listOf(it.id, it.point2D.id)
        }
        val proxyId = line.line2D.id

        board.removeObject(line)

        assertNull(board.elementById(line.id))
        assertNull(view.select(line.id))
        assertNull(board.elementById(proxyId))
        for (endpointId in endpointIds) {
            assertNull(board.elementById(endpointId))
        }
        assertSame(point1, board.elementById(point1.id))
        assertSame(point2, board.elementById(point2.id))
    }

    @Test
    fun malformedInputsReturnStructuredErrors() {
        val view = createView(createBoard())
        val point = point(view, doubleArrayOf(0.0, 0.0, 0.0), "point")

        assertIs<GMResult.Err<Line3DError.InvalidDirectionCount>>(
            Line3D.create(
                view = view,
                point = point,
                directionSource = Line3DDirectionSource.Values(
                    listOf(
                        Line3DCoordinateValue.Numeric(1.0),
                        Line3DCoordinateValue.Numeric(2.0),
                    ),
                ),
            ),
        )
        assertIs<GMResult.Err<Line3DError.InvalidRangeCount>>(
            Line3D.create(
                view = view,
                point = point,
                directionSource = Line3DDirectionSource.Values(
                    List(3) { Line3DCoordinateValue.Numeric(1.0) },
                ),
                rangeSource = listOf(
                    Line3DCoordinateValue.Numeric(0.0),
                ),
            ),
        )
        assertIs<GMResult.Err<Line3DError.InvalidTransformationCount>>(
            Line3D.create(
                view = view,
                baseLine = line(
                    Line3D.create(
                        view = view,
                        point1 = point,
                        point2 = point(
                            view,
                            doubleArrayOf(1.0, 0.0, 0.0),
                            "other",
                        ),
                    ),
                ),
                transformations = emptyList(),
            ),
        )
        assertTrue(view.select(point.id) != null)
    }

    private fun createBoard(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )

    private fun createView(board: Board): View3D =
        assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(8.0, 7.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 6.0),
                    doubleArrayOf(-3.0, 7.0),
                ),
                projection = "parallel",
                azimuth = 1.0,
                elevation = 0.3,
                bank = 0.0,
                id = "view",
                name = "",
            ),
        ).value

    private fun point(
        view: View3D,
        coordinates: DoubleArray,
        id: String,
    ): Point3D = point(
        Point3D.create(
            view = view,
            coordinates = coordinates,
            id = id,
            name = "",
        ),
    )

    private fun point(
        result: GMResult<Point3D, Point3DError>,
    ): Point3D = assertIs<GMResult.Ok<Point3D>>(result).value

    private fun line(
        result: GMResult<Line3D, Line3DError>,
    ): Line3D = assertIs<GMResult.Ok<Line3D>>(result).value

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
    }
}
