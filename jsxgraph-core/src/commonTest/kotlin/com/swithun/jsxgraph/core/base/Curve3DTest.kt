package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Curve3DTest {
    @Test
    fun componentCurveSamplesRangeAndProjectsThroughCurveProxy() {
        val board = createBoard()
        val view = createView(board)
        val curve = curve(
            Curve3D.create(
                view = view,
                source = Curve3DSource.Components(
                    x = Curve3DScalarEvaluator { parameter ->
                        GMResult.Ok(cos(parameter))
                    },
                    y = Curve3DScalarEvaluator { parameter ->
                        GMResult.Ok(sin(parameter))
                    },
                    z = Curve3DScalarEvaluator { parameter ->
                        GMResult.Ok(parameter / PI)
                    },
                ),
                rangeSource = range(0.0, 2.0 * PI),
                sampleCount = 5,
                id = "helix",
                name = "",
            ),
        )

        assertEquals("curve3d", curve.elType)
        assertEquals(Const.OBJECT_TYPE_CURVE3D, curve.type)
        assertEquals(5, curve.numberPoints)
        assertArrayClose(
            doubleArrayOf(1.0, 1.0, 0.0, 0.0),
            curve.points.first(),
        )
        assertArrayClose(
            doubleArrayOf(1.0, 1.0, 0.0, 2.0),
            curve.points.last(),
        )
        assertEquals(5, curve.curve2D.numberPoints)
        assertEquals(listOf(curve.id), curve.curve2D.parents)
        assertTrue(!curve.curve2D.dump)
        val projected = view.project3DTo2D(curve.points[2])
        assertArrayClose(
            doubleArrayOf(projected[1], projected[2]),
            curve.curve2D.points[2].usrCoords.copyOfRange(1, 3),
        )
    }

    @Test
    fun vectorFunctionAndDynamicRangeUpdateWithoutRecreatingProxy() {
        val board = createBoard()
        val view = createView(board)
        var end = 2.0
        val curve = curve(
            Curve3D.create(
                view = view,
                source = Curve3DSource.Function(
                    Curve3DArrayEvaluator { parameter ->
                        GMResult.Ok(
                            doubleArrayOf(
                                parameter,
                                parameter * parameter,
                                -parameter,
                            ),
                        )
                    },
                ),
                rangeSource = listOf(
                    Line3DCoordinateValue.Numeric(0.0),
                    Line3DCoordinateValue.Dynamic(
                        Line3DScalarEvaluator {
                            GMResult.Ok(end)
                        },
                    ),
                ),
                sampleCount = 3,
                id = "dynamic",
                name = "",
            ),
        )
        val proxy = curve.curve2D
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 1.0, -1.0),
            curve.points[1],
        )

        end = 4.0
        curve.prepareUpdate().update()

        assertSame(proxy, curve.curve2D)
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 4.0, -2.0),
            curve.points[1],
        )
        assertContentEquals(doubleArrayOf(0.0, 4.0), curve.evaluatedRange)
    }

    @Test
    fun discreteAndTransformedCurvesPreserveUpstreamPointOrder() {
        val board = createBoard()
        val view = createView(board)
        val base = curve(
            Curve3D.create(
                view = view,
                source = Curve3DSource.Arrays(
                    x = doubleArrayOf(-1.0, 0.0, 2.0),
                    y = doubleArrayOf(0.0, 2.0, 0.0),
                    z = doubleArrayOf(1.0, 2.0, 3.0),
                ),
                id = "base",
                name = "",
            ),
        )
        val translation = assertIs<GMResult.Ok<Transformation>>(
            Transformation.create3D(
                type = "translate",
                parameters = doubleArrayOf(3.0, -1.0, 2.0),
            ),
        ).value
        val transformed = curve(
            Curve3D.create(
                view = view,
                baseCurve = base,
                transformations = listOf(translation),
                id = "moved",
                name = "",
            ),
        )

        assertEquals(3, transformed.numberPoints)
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, -1.0, 3.0),
            transformed.points[0],
        )
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, -1.0, 5.0),
            transformed.points[2],
        )
        assertEquals(listOf(base.id), transformed.parents)
        assertTrue(transformed.id in base.descendants)
    }

    @Test
    fun failuresAreStructuredAndRemovalKeepsExternalBase() {
        val board = createBoard()
        val view = createView(board)
        assertIs<GMResult.Err<Curve3DError.InvalidRangeCount>>(
            Curve3D.create(
                view = view,
                source = Curve3DSource.Function(
                    Curve3DArrayEvaluator {
                        GMResult.Ok(doubleArrayOf(0.0, 0.0, 0.0))
                    },
                ),
                rangeSource = range(0.0),
            ),
        )
        assertIs<GMResult.Err<Curve3DError.InvalidSampleCount>>(
            Curve3D.create(
                view = view,
                source = Curve3DSource.Arrays(
                    x = doubleArrayOf(),
                    y = doubleArrayOf(),
                    z = doubleArrayOf(),
                ),
                sampleCount = Curve3D.MAX_SAMPLE_COUNT + 1,
            ),
        )

        val base = curve(
            Curve3D.create(
                view = view,
                source = Curve3DSource.Arrays(
                    x = doubleArrayOf(0.0, 1.0),
                    y = doubleArrayOf(0.0, 1.0),
                    z = doubleArrayOf(0.0, 1.0),
                ),
                id = "base",
                name = "",
            ),
        )
        val translation = assertIs<GMResult.Ok<Transformation>>(
            Transformation.create3D(
                type = "translate",
                parameters = doubleArrayOf(1.0, 0.0, 0.0),
            ),
        ).value
        val transformed = curve(
            Curve3D.create(
                view = view,
                baseCurve = base,
                transformations = listOf(translation),
                id = "moved",
                name = "",
            ),
        )
        val proxyId = transformed.curve2D.id

        board.removeObject(transformed)

        assertNull(board.elementById(transformed.id))
        assertNull(board.elementById(proxyId))
        assertSame(base, board.elementById(base.id))
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

    private fun range(vararg values: Double): List<Line3DCoordinateValue> =
        values.map(Line3DCoordinateValue::Numeric)

    private fun curve(
        result: GMResult<Curve3D, Curve3DError>,
    ): Curve3D = assertIs<GMResult.Ok<Curve3D>>(result).value

    private fun assertArrayClose(
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
}
